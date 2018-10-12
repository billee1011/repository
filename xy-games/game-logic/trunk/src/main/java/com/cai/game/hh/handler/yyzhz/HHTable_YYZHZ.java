package com.cai.game.hh.handler.yyzhz;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.RedisConstant;
import com.cai.common.constant.game.mj.GameConstants_XiangXiang;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.GameRoundRecord;
import com.cai.common.domain.Player;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.SysParamModel;
import com.cai.common.domain.WeaveItem;
import com.cai.common.util.RandomUtil;
import com.cai.common.util.SpringService;
import com.cai.dictionary.SysParamDict;
import com.cai.game.hh.HHGameLogic.AnalyseItem;
import com.cai.game.hh.HHTable;
import com.cai.redis.service.RedisService;
import com.cai.service.PlayerServiceImpl;

import protobuf.clazz.Protocol.GameEndResponse;
import protobuf.clazz.Protocol.GameStartResponse;
import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomInfo;
import protobuf.clazz.Protocol.RoomPlayerResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class HHTable_YYZHZ extends HHTable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public HHTable_YYZHZ() {
		super();
	}

	@Override
	public int getTablePlayerNumber() {
		if (has_rule(GameConstants_YYZHZ.GAME_RULE_PLAYER_YYZGZ_THREE)) {
			return GameConstants_YYZHZ.GAME_PLAYER - 1;
		}
		return GameConstants_YYZHZ.GAME_PLAYER;
	}

	@Override
	public void on_init_table(int game_type_index, int game_rule_index, int game_round) {

		super.on_init_table(game_type_index, game_rule_index, game_round);

		_handler_dispath_card = new PHZHandlerDispatchCard_YYZHZ();
		_handler_out_card_operate = new PHZHandlerOutCardOperate_YYZHZ();
		_handler_chi_peng = new PHZHandlerChiPeng_YYZHZ();
		_handler_chuli_firstcards = new PHZHandlerChuLiFirstCard_YYZHZ();
		_handler_dispath_firstcards = new PHZHandlerDispatchFirstCard_YYZHZ();
	}

	@Override
	public boolean reset_init_data() {

		if (_cur_round == 0) {
			record_game_room();
		}
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_long_count[i] = 0;
		}
		// 不能吃，碰
		_cannot_chi = new int[this.getTablePlayerNumber()][30];
		_cannot_chi_count = new int[this.getTablePlayerNumber()];
		_cannot_peng = new int[this.getTablePlayerNumber()][30];
		_cannot_peng_count = new int[this.getTablePlayerNumber()];
		_hu_weave_count = new int[this.getTablePlayerNumber()]; //
		_da_pai_count = new int[this.getTablePlayerNumber()];
		_xiao_pai_count = new int[this.getTablePlayerNumber()];
		_hu_weave_items = new WeaveItem[this.getTablePlayerNumber()][7];
		_guo_hu_xt = new int[this.getTablePlayerNumber()];
		_ti_mul_long = new int[this.getTablePlayerNumber()];
		_guo_hu_pai_count = new int[this.getTablePlayerNumber()];
		_ting_card = new boolean[this.getTablePlayerNumber()];
		_tun_shu = new int[this.getTablePlayerNumber()];
		_fan_shu = new int[this.getTablePlayerNumber()];
		_hu_code = new int[this.getTablePlayerNumber()];
		_liu_zi_fen = new int[this.getTablePlayerNumber()];
		_hu_xi = new int[this.getTablePlayerNumber()];
		_hu_pai_max_hu = new int[this.getTablePlayerNumber()];
		_xt_display_an_long = new boolean[this.getTablePlayerNumber()];
		_zong_liu_zi_fen = 0;
		_all_xi = 0;
		_all_lz_fen = 0;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			for (int j = 0; j < 7; j++) {
				_hu_weave_items[i][j] = new WeaveItem();
			}
		}

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_cannot_chi_count[i] = 0;
			_cannot_peng_count[i] = 0;
			_da_pai_count[i] = 0;
			_xiao_pai_count[i] = 0;
			_guo_hu_xt[i] = -1;
			_ti_mul_long[i] = 0;
			_guo_hu_pai_count[i] = 0;
			_ting_card[i] = false;
			_tun_shu[i] = 0;
			_fan_shu[i] = 0;
			_hu_code[i] = GameConstants.WIK_NULL;
			_liu_zi_fen[i] = 0;
			_hu_xi[i] = 0;
			_hu_pai_max_hu[i] = 0;
			_xt_display_an_long[i] = false;
		}
		_guo_hu_pai_cards = new int[this.getTablePlayerNumber()][15];
		_guo_hu_xi = new int[this.getTablePlayerNumber()][15];
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			for (int j = 0; j < 15; j++) {
				_guo_hu_pai_cards[i][j] = 0;
				_guo_hu_xi[i][j] = 0;
			}
		}
		_last_card = 0;
		_last_player = -1;// 上次发牌玩家
		_long_count = new int[this.getTablePlayerNumber()]; // 每个用户有几条龙
		_ti_two_long = new boolean[this.getTablePlayerNumber()];
		_is_xiang_gong = new boolean[this.getTablePlayerNumber()];
		// 设置变量
		_provide_card = GameConstants.INVALID_VALUE;
		_out_card_data = GameConstants.INVALID_VALUE;
		_send_card_data = GameConstants.INVALID_VALUE;

		_provide_player = GameConstants.INVALID_SEAT;
		_out_card_player = GameConstants.INVALID_SEAT;
		_current_player = GameConstants.INVALID_SEAT;

		_send_card_count = 0;
		_out_card_count = 0;
		this._handler = null;

		GRR = new GameRoundRecord(GameConstants_YYZHZ.MAX_WEAVE_YYZHZ, GameConstants_YYZHZ.MAX_YYZHZ_COUNT, GameConstants_YYZHZ.MAX_YYZHZ_INDEX);
		GRR.setRoom(this);
		GRR._start_time = System.currentTimeMillis() / 1000L;
		GRR._game_type_index = _game_type_index;
		GRR._cur_round = _cur_round;

		// 新建
		_playerStatus = new PlayerStatus[this.getTablePlayerNumber()];
		istrustee = new boolean[this.getTablePlayerNumber()];
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_playerStatus[i] = new PlayerStatus(GameConstants_YYZHZ.MAX_YYZHZ_INDEX);
		}

		_cur_round++;

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

	// 游戏开始
	@Override
	protected boolean on_handler_game_start() {
		_game_status = GameConstants_YYZHZ.GS_MJ_PLAY;

		reset_init_data();

		// 庄家选择
		progress_banker_select();

		GRR._banker_player = _cur_banker;
		_current_player = GRR._banker_player;

		// 初始化牌
		int card_count = GameConstants_YYZHZ.CARD_COUNT_PHZ_YYZHZ;
		if (has_rule(GameConstants_YYZHZ.GAME_RULE_PLAYER_YYZGZ_WANG_1)) {
			card_count += 1;
		} else if (has_rule(GameConstants_YYZHZ.GAME_RULE_PLAYER_YYZGZ_WANG_2)) {
			card_count += 2;
		} else if (has_rule(GameConstants_YYZHZ.GAME_RULE_PLAYER_YYZGZ_WANG_3)) {
			card_count += 3;
		} else if (has_rule(GameConstants_YYZHZ.GAME_RULE_PLAYER_YYZGZ_WANG_4)) {
			card_count += 4;

		}
		int[] real_card = new int[card_count];
		for (int i = 0; i < real_card.length; i++) {
			if (i >= GameConstants_YYZHZ.CARD_COUNT_PHZ_YYZHZ) {
				real_card[i] = GameConstants_YYZHZ.YYZHZ_MAGIC_CARD;
			} else {
				real_card[i] = GameConstants_YYZHZ.CARD_DATA_PHZ_DEFAULT[i];
			}
		}

		_repertory_card = new int[card_count];
		shuffle(_repertory_card, real_card);
		// 洗牌结束


		if (DEBUG_CARDS_MODE || BACK_DEBUG_CARDS_MODE)
			test_cards();

		// 添加王牌
		_logic.clean_magic_cards();
		if (card_count > GameConstants_YYZHZ.CARD_COUNT_PHZ_YYZHZ) {
			_logic.add_magic_card_index(_logic.switch_to_card_index_yyzhz(GameConstants_YYZHZ.YYZHZ_MAGIC_CARD));
		}

		int playerCount = getPlayerCount();
		this.GRR._banker_player = this._current_player = this._cur_banker;
		// 游戏开始
		this._game_status = GameConstants_YYZHZ.GS_MJ_PLAY;// 设置状态
		GameStartResponse.Builder gameStartResponse = GameStartResponse.newBuilder();
		gameStartResponse.setBankerPlayer(this.GRR._banker_player);
		gameStartResponse.setCurrentPlayer(this._current_player);
		gameStartResponse.setLeftCardCount(this.GRR._left_card_count);

		int hand_cards[][] = new int[playerCount][GameConstants_YYZHZ.MAX_YYZHZ_COUNT];
		// 发送数据
		for (int i = 0; i < playerCount; i++) {
			int hand_card_count = this._logic.switch_to_cards_data_lai(this.GRR._cards_index[i], hand_cards[i]);
			gameStartResponse.addCardsCount(hand_card_count);
		}

		/*
		 * boolean can_ti = false; int ti_card_count[] = new
		 * int[this.getTablePlayerNumber()]; int ti_card_index[][] = new
		 * int[this.getTablePlayerNumber()][5];
		 * 
		 * for (int i = 0; i < playerCount; i++) { ti_card_count[i] =
		 * this._logic.get_action_ti_Card(this.GRR._cards_index[i],
		 * ti_card_index[i]); if (ti_card_count[i] > 0) can_ti = true; }
		 */
		int FlashTime = 4000;
		int standTime = 1000;
		// 发送数据
		for (int i = 0; i < playerCount; i++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

			// 只发自己的牌
			gameStartResponse.clearCardData();
			for (int j = 0; j < GameConstants_YYZHZ.MAX_YYZHZ_COUNT; j++) {
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
			roomResponse.setCurrentPlayer(this._current_player == GameConstants_YYZHZ.INVALID_SEAT ? this._resume_player : this._current_player);
			roomResponse.setLeftCardCount(this.GRR._left_card_count);
			roomResponse.setGameStatus(this._game_status);
			roomResponse.setLeftCardCount(this.GRR._left_card_count);

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
			this.send_response_to_player(i, roomResponse);
		}
		////////////////////////////////////////////////// 回放
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
		this.load_room_info_data(roomResponse);
		this.load_common_status(roomResponse);
		this.load_player_info_data(roomResponse);
		for (int i = 0; i < playerCount; i++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

			for (int j = 0; j < GameConstants_YYZHZ.MAX_YYZHZ_COUNT; j++) {
				cards.addItem(hand_cards[i][j]);
			}
			gameStartResponse.addCardsData(cards);
		}

		roomResponse.setGameStart(gameStartResponse);

		roomResponse.setLeftCardCount(this.GRR._left_card_count);
		this.GRR.add_room_response(roomResponse);

		// 检测听牌
		for (int i = 0; i < playerCount; i++) {
			this._playerStatus[i]._hu_card_count = get_hh_ting_card_twenty(_playerStatus[i]._hu_cards, GRR._cards_index[i], GRR._weave_items[i],
					GRR._weave_count[i], i, i);
			if (this._playerStatus[i]._hu_card_count > 0) {
				this.operate_chi_hu_cards(i, this._playerStatus[i]._hu_card_count, this._playerStatus[i]._hu_cards);
			}
		}
		this._handler = this._handler_dispath_firstcards;
		this.exe_dispatch_first_card(_current_player, GameConstants_YYZHZ.WIK_NULL, FlashTime + standTime);

		return true;
	}

	/// 洗牌
	@Override
	public void shuffle(int repertory_card[], int mj_cards[]) {
		_all_card_len = repertory_card.length;
		GRR._left_card_count = _all_card_len;

//		_logic.random_card_data(repertory_card, mj_cards);
		int xi_pai_count = 0;
		int rand = (int) RandomUtil.generateRandomNumber(3,6) ;
		while(xi_pai_count < 6 && xi_pai_count < rand)
		{
			if(xi_pai_count == 0 )
				_logic.random_card_data(repertory_card, mj_cards);
			else 
				_logic.random_card_data(repertory_card, repertory_card);
			xi_pai_count ++;
		}

		int send_count;
		int have_send_count = 0;

		int count = getTablePlayerNumber();
		for (int i = 0; i < count; i++) {

			send_count = (GameConstants.MAX_FPHZ_INDEX - 1);

			GRR._left_card_count -= send_count;
			// 一人13张牌,庄家多一张
			_logic.switch_to_cards_index(repertory_card, have_send_count, send_count, GRR._cards_index[i]);
			have_send_count += send_count;
		}
		// 记录初始牌型
		_recordRoomRecord.setBeginArray(Arrays.toString(repertory_card));

	}

	/**
	 * 捉红字胡牌分析
	 * 
	 * @param cards_index
	 * @param weaveItems
	 * @param weaveCount
	 * @param seat_index
	 * @param provider_index
	 * @param cur_card
	 * @param chiHuRight
	 * @param card_type
	 * @param dispatch
	 * @return
	 */
	public int analyse_chi_hu_card(int cards_index[], WeaveItem weaveItems[], int weaveCount, int seat_index, int provider_index, int cur_card,
			ChiHuRight chiHuRight, int card_type, boolean dispatch) {
		// 构造扑克
		int cbCardIndexTemp[] = new int[GameConstants_YYZHZ.MAX_YYZHZ_INDEX];
		for (int i = 0; i < GameConstants_YYZHZ.MAX_YYZHZ_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		// 插入扑克
		if (cur_card != GameConstants_YYZHZ.INVALID_VALUE) {
			int index = _logic.switch_to_card_index_yyzhz(cur_card);
			cbCardIndexTemp[index]++;
		}

		List<AnalyseItem> analyseItemArray = new ArrayList<AnalyseItem>();
		// 分析扑克
		boolean bValue = _logic.analyse_card_yyzhz(cbCardIndexTemp, weaveItems, weaveCount, analyseItemArray, seat_index, provider_index, cur_card);

		if (!bValue) {
			chiHuRight.set_empty();
			return GameConstants_YYZHZ.WIK_NULL;
		}

		// 构造完整牌
		int allCardIndexTemp[] = new int[GameConstants_YYZHZ.MAX_YYZHZ_INDEX];
		for (int i = 0; i < GameConstants_YYZHZ.MAX_YYZHZ_INDEX; i++) {
			allCardIndexTemp[i] = cbCardIndexTemp[i];
		}

		// 还原组合牌
		for (int i = 0; i < weaveCount; i++) {
			getWeaveItemCard(weaveItems[i], allCardIndexTemp);
		}

		// 全部牌组分析
		int hong_pai_count_all = 0;
		int hei_pai_count_all = 0;
		int card_xiao = 0;
		int card_da = 0;
		for (int i = 0; i < GameConstants_YYZHZ.MAX_YYZHZ_INDEX; i++) {
			// 全部牌组
			int num = allCardIndexTemp[i];
			if (num != 0) {
				if (_logic.color_hei_yyzhz(_logic.switch_to_card_data_yyzhz(i))) {
					hei_pai_count_all += num;
				} else {
					hong_pai_count_all += num;
				}
				// 大小判断
				if (i < 10) {
					card_xiao += num;
				} else {
					card_da += num;
				}
			}
		}

		// 两红字不能胡
		if (hong_pai_count_all == 2) {
			chiHuRight.set_empty();
			return GameConstants_YYZHZ.WIK_NULL;
		}

		if (hong_pai_count_all == 1) {
			chiHuRight.opr_or(GameConstants_YYZHZ.CHR_HU_DIAN_HU);
		}

		if (hong_pai_count_all == 10) {
			chiHuRight.opr_or(GameConstants_YYZHZ.CHR_HU_SHI_HONG);
		}

		if (hong_pai_count_all == 14) {
			chiHuRight.opr_or(GameConstants_YYZHZ.CHR_HU_MANG_TANG_HONG);
		}

		if (hei_pai_count_all == 14) {
			chiHuRight.opr_or(GameConstants_YYZHZ.CHR_HU_HEI_HU);
		}
		if (_logic.check_yi_gua_bian(cbCardIndexTemp)) {
			chiHuRight.opr_or(GameConstants_YYZHZ.CHR_HU_YI_GUA_BIAN);
		}

		// 碰碰胡
		for (int i = 0; i < analyseItemArray.size(); i++) {
			// 变量定义
			AnalyseItem analyseItem = analyseItemArray.get(i);
			// 碰碰和
			if (_logic.is_pengpeng_hu_yyzhz(analyseItem)) {
				chiHuRight.opr_or(GameConstants_YYZHZ.CHR_HU_PENG_PENG_HU);
				break;
			}
		}

		// 全大
		if (card_xiao == 14) {
			chiHuRight.opr_or(GameConstants_YYZHZ.CHR_HU_XIAO_YI_SE);
		}

		if (card_da == 14) {
			chiHuRight.opr_or(GameConstants_YYZHZ.CHR_HU_DA_YI_SE);
		}

		if (_logic.is_qi_xiao_dui_yyzhz(allCardIndexTemp) != GameConstants.WIK_NULL) {
			chiHuRight.opr_or(GameConstants_YYZHZ.CHR_HU_QI_XIAO_DUI);
		}
		// 四碰单吊
		if (_logic.is_dan_diao(weaveItems, weaveCount, cards_index, cur_card)) {
			chiHuRight.opr_or(GameConstants_YYZHZ.CHR_HU_DANG_DIAO);
		}
		// 句句红
		for (int i = 0; i < analyseItemArray.size(); i++) {
			// 变量定义
			AnalyseItem analyseItem = analyseItemArray.get(i);
			// 句句红
			if (_logic.is_pengpeng_hu_yyzhz(analyseItem)) {
				chiHuRight.opr_or(GameConstants_YYZHZ.CHR_HU_JU_JU_HONG);
				break;
			}
		}
		chiHuRight.hong_count = hong_pai_count_all;
		return GameConstants_YYZHZ.WIK_CHI_HU;
	}

	/**
	 * 还原组合牌组到cardIndex
	 * 
	 * @param weaveItems
	 * @param card_index
	 */
	public void getWeaveItemCard(WeaveItem weaveItems, int card_index[]) {
		int card_temp[] = new int[4];
		_logic.get_weave_card(weaveItems.weave_kind, weaveItems.center_card, card_temp);

		for (int i = 0; i < card_temp.length; i++) {
			if (card_temp[i] == 0)
				continue;
			card_index[_logic.switch_to_card_index(card_temp[i])]++;
		}
	}

	/**
	 * 算分
	 * 
	 * @param seat_index
	 * @param provide_index
	 * @param operate_card
	 * @param zimo
	 */
	@Override
	public void process_chi_hu_player_score_phz(int seat_index, int provide_index, int operate_card, boolean zimo) {
		GRR._chi_hu_card[seat_index][0] = operate_card;

		GRR._win_order[seat_index] = 1;
		// 引用权位
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];
		countCardType(chr, seat_index);
		/*
		 * // 计算胡息 int all_hu_xi = 0; for (int i = 0; i <
		 * this._hu_weave_count[seat_index]; i++) { all_hu_xi +=
		 * this._hu_weave_items[seat_index][i].hu_xi; } this._hu_xi[seat_index]
		 * = all_hu_xi; int calculate_score = 1 + (all_hu_xi - 15) / 3;
		 */

		int fan = get_chi_hu_action_rank_phz(chr);
		int wFanShu = fan == 0 ? chr.hong_count : fan;// 番数

		// 统计
		if (zimo) {
			// 自摸
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index) {
					continue;
				}
				GRR._lost_fan_shu[i][seat_index] = wFanShu;
			}
		}

		float lChiHuScore = wFanShu * 1;

		////////////////////////////////////////////////////// 自摸 算分
		if (zimo) {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index) {
					continue;
				}

				// 胡牌分
				GRR._game_score[i] -= lChiHuScore;
				GRR._game_score[seat_index] += lChiHuScore;
			}
		}
	}

	@Override
	public boolean on_room_game_finish(int seat_index, int reason) {
		_game_status = GameConstants_XiangXiang.GS_MJ_WAIT;
		int real_reason = reason;

		int count = getTablePlayerNumber();
		if (count == 0) {
			count = this.getTablePlayerNumber();
		}
		for (int i = 0; i < count; i++) {
			_player_ready[i] = 0;
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_GAME_END);
		GameEndResponse.Builder game_end = GameEndResponse.newBuilder();

		roomResponse.setLeftCardCount(0);

		this.load_common_status(roomResponse);
		this.load_room_info_data(roomResponse);

		RoomInfo.Builder room_info = getRoomInfo();
		game_end.setRoomInfo(room_info);
		game_end.setGamePlayerNumber(count);
		game_end.setRoundOverType(0);
		game_end.setRoomOverType(0);
		game_end.setEndTime(System.currentTimeMillis() / 1000L);// 结束时间

		if (GRR != null) {// reason == MJGameConstants_YYZHZ.Game_End_NORMAL ||
							// reason
							// == MJGameConstants_YYZHZ.Game_End_DRAW ||
			// (reason ==MJGameConstants_YYZHZ.Game_End_RELEASE_PLAY &&
			// GRR!=null)
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
			game_end.setGamePlayerNumber(getTablePlayerNumber());
			game_end.setCellScore(GameConstants_YYZHZ.CELL_SCORE);

			game_end.setBankerPlayer(GRR._banker_player);// 专家
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
				// WalkerGeek 注释东西
				// Int32ArrayResponse.Builder pnc =
				// Int32ArrayResponse.newBuilder();
				game_end.addHuResult(GRR._hu_result[i]);

				// 胡的牌
				Int32ArrayResponse.Builder hc = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GameConstants_YYZHZ.MAX_YYZHZ_COUNT; j++) {
					hc.addItem(GRR._chi_hu_card[i][j]);
				}

				game_end.addHuCardData(GRR._chi_hu_card[i][0]);
				game_end.addHuCardArray(hc);
			}

			// 现在权值只有一位
			long rv[] = new long[GameConstants_YYZHZ.MAX_RIGHT_COUNT];

			// 设置胡牌描述
			this.set_result_describe(seat_index);
			/*
			 * if (this.has_rule(GameConstants_YYZHZ.GAME_RULE_DI_HUANG_FAN)) {
			 * if (reason == GameConstants_YYZHZ.Game_End_DRAW) {
			 * this._huang_zhang_count++; } else { this._huang_zhang_count = 0;
			 * } }
			 */

			for (int i = 0; i < count; i++) {
				GRR._card_count[i] = _logic.switch_to_cards_data_lai(GRR._cards_index[i], GRR._cards_data[i]);

				Int32ArrayResponse.Builder cs = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GRR._card_count[i]; j++) {

					cs.addItem(GRR._cards_data[i][j]);
				}
				game_end.addCardsData(cs);// 牌
				// int all_hu_xi = 0;
				// 组合
				WeaveItemResponseArrayResponse.Builder weaveItem_array = WeaveItemResponseArrayResponse.newBuilder();
				if (GRR._weave_count[i] > 0) {
					for (int j = 0; j < GRR._weave_count[i]; j++) {
						WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
						weaveItem_item.setProvidePlayer(GRR._weave_items[i][j].provide_player);
						weaveItem_item.setPublicCard(GRR._weave_items[i][j].public_card);
						weaveItem_item.setWeaveKind(GRR._weave_items[i][j].weave_kind);
						weaveItem_item.setHuXi(GRR._weave_items[i][j].hu_xi);
						// WalkerGeek 胡息
						// all_hu_xi += _hu_weave_items[i][j].hu_xi;
						weaveItem_item.setCenterCard(GRR._weave_items[i][j].center_card);
						weaveItem_array.addWeaveItem(weaveItem_item);
					}
				}
				game_end.addWeaveItemArray(weaveItem_array);

				GRR._chi_hu_rights[i].get_right_data(rv);// 获取权位数值
				game_end.addChiHuRight(rv[0]);

				GRR._start_hu_right[i].get_right_data(rv);// 获取权位数值
				game_end.addStartHuRight(rv[0]);

				game_end.addProvidePlayer(GRR._provider[i]);
				game_end.addGameScore(GRR._game_score[i]);// 放炮的人？
				// game_end.addGangScore(lGangScore[i]);// 杠牌得分
				game_end.addStartHuScore(GRR._start_hu_score[i]);
				game_end.addResultDes(GRR._result_des[i]);

				// 胡牌
				game_end.addWinOrder(GRR._win_order[i]);

				Int32ArrayResponse.Builder lfs = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < count; j++) {
					lfs.addItem(GRR._lost_fan_shu[i][j]);
				}

				game_end.addLostFanShu(lfs);
			}
		}

		boolean end = false;
		if (reason == GameConstants_YYZHZ.Game_End_NORMAL || reason == GameConstants_YYZHZ.Game_End_DRAW) {
			if (_cur_round >= _game_round) {// 局数到了
				end = true;
				game_end.setRoomOverType(1);
				game_end.setPlayerResult(this.process_player_result(reason));
			} else {
				// 确定下局庄家
				// 以后谁胡牌，下局谁做庄。
				// 流局,庄家下一个
				// 通炮,放炮的玩家

			}
		} else if (reason == GameConstants_YYZHZ.Game_End_RELEASE_PLAY || reason == GameConstants_YYZHZ.Game_End_RELEASE_NO_BEGIN
				|| reason == GameConstants_YYZHZ.Game_End_RELEASE_RESULT || reason == GameConstants_YYZHZ.Game_End_RELEASE_PLAY_TIME_OUT
				|| reason == GameConstants_YYZHZ.Game_End_RELEASE_WAIT_TIME_OUT || reason == GameConstants_YYZHZ.Game_End_RELEASE_SYSTEM) {
			end = true;
			real_reason = GameConstants_YYZHZ.Game_End_DRAW;// 刘局
			game_end.setRoomOverType(1);
			game_end.setPlayerResult(this.process_player_result(reason));
		}
		game_end.setEndType(real_reason);

		////////////////////////////////////////////////////////////////////// 得分总的
		roomResponse.setGameEnd(game_end);

		this.send_response_to_room(roomResponse);

		record_game_round(game_end, real_reason);

		// 超时解散
		if (reason == GameConstants_YYZHZ.Game_End_RELEASE_PLAY_TIME_OUT || reason == GameConstants_YYZHZ.Game_End_RELEASE_WAIT_TIME_OUT) {
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

	/**
	 * 岳阳捉红字结算描述
	 */
	@Override
	public void set_result_describe(int seat_index) {
		int l;
		long type = 0;
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			String des = "";

			l = GRR._chi_hu_rights[i].type_count;
			for (int j = 0; j < l; j++) {
				type = GRR._chi_hu_rights[i].type_list[j];
				if (GRR._chi_hu_rights[i].is_valid()) {
					if (type == GameConstants_YYZHZ.CHR_HU_BAN_BAN_HU) {
						des += ",板板胡";
					}
					if (type == GameConstants_YYZHZ.CHR_HU_DIAN_HU) {
						des += ",点胡";
					}
					if (type == GameConstants_YYZHZ.CHR_HU_YI_GUA_BIAN) {
						des += ",一挂匾";
					}
					if (type == GameConstants_YYZHZ.CHR_HU_PENG_PENG_HU) {
						des += ",碰碰胡";
					}
					if (type == GameConstants_YYZHZ.CHR_HU_XIAO_YI_SE) {
						des += ",小一色";
					}
					if (type == GameConstants_YYZHZ.CHR_HU_DA_YI_SE) {
						des += ",大一色";
					}
					if (type == GameConstants_YYZHZ.CHR_HU_HEI_HU) {
						des += ",黑胡";
					}
					if (type == GameConstants_YYZHZ.CHR_HU_SHI_HONG) {
						des += ",十红";
					}
					if (type == GameConstants_YYZHZ.CHR_HU_QI_XIAO_DUI) {
						des += ",七小对";
					}
					if (type == GameConstants_YYZHZ.CHR_HU_MANG_TANG_HONG) {
						des += ",满堂红";
					}
					if (type == GameConstants_YYZHZ.CHR_HU_DANG_DIAO) {
						des += ",四碰单吊";
					}
					if (type == GameConstants_YYZHZ.CHR_HU_JU_JU_HONG) {
						des += ",句句红";
					}
				}
			}

			GRR._result_des[i] = des;
		}
	}

	/**
	 * 大胡分数计算
	 * 
	 * @param chr
	 * @return
	 */
	public int get_chi_hu_action_rank_phz(ChiHuRight chr) {
		int wFanShu = 0;
		if (!(chr.opr_and(GameConstants_YYZHZ.CHR_HU_BAN_BAN_HU)).is_empty()) {
			wFanShu += 10;
		}
		if (!(chr.opr_and(GameConstants_YYZHZ.CHR_HU_DIAN_HU)).is_empty()) {
			wFanShu += 10;
		}
		if (!(chr.opr_and(GameConstants_YYZHZ.CHR_HU_PENG_PENG_HU)).is_empty()) {
			wFanShu += 20;
		}
		if (!(chr.opr_and(GameConstants_YYZHZ.CHR_HU_XIAO_YI_SE)).is_empty()) {
			wFanShu += 20;
		}
		if (!(chr.opr_and(GameConstants_YYZHZ.CHR_HU_DA_YI_SE)).is_empty()) {
			wFanShu += 20;
		}
		if (!(chr.opr_and(GameConstants_YYZHZ.CHR_HU_HEI_HU)).is_empty()) {
			wFanShu += 20;
		}
		if (!(chr.opr_and(GameConstants_YYZHZ.CHR_HU_SHI_HONG)).is_empty()) {
			wFanShu += 20;
		}
		if (!(chr.opr_and(GameConstants_YYZHZ.CHR_HU_QI_XIAO_DUI)).is_empty()) {
			wFanShu += 20;
		}
		if (!(chr.opr_and(GameConstants_YYZHZ.CHR_HU_MANG_TANG_HONG)).is_empty()) {
			wFanShu += 40;
		}
		if (!(chr.opr_and(GameConstants_YYZHZ.CHR_HU_DANG_DIAO)).is_empty()) {
			wFanShu += 40;
		}
		if (!(chr.opr_and(GameConstants_YYZHZ.CHR_HU_JU_JU_HONG)).is_empty()) {
			wFanShu += 10;
		}
		return wFanShu;
	}

	/**
	 * 碰、跑、偎、提的字牌
	 * 
	 * @param weaveItems
	 * @param weaveCount
	 * @return
	 */
	public int calculate_hong_pai_count(WeaveItem weaveItems[], int weaveCount) {
		int count = 0;
		for (int i = 0; i < weaveCount; i++) {
			switch (weaveItems[i].weave_kind) {
			case GameConstants_YYZHZ.WIK_TI_LONG:
			case GameConstants_YYZHZ.WIK_AN_LONG:
			case GameConstants_YYZHZ.WIK_PAO:
				if (_logic.color_hei(weaveItems[i].center_card) == false)
					count += 4;
				break;
			case GameConstants_YYZHZ.WIK_SAO:
			case GameConstants_YYZHZ.WIK_PENG:
			case GameConstants_YYZHZ.WIK_CHOU_SAO:
			case GameConstants_YYZHZ.WIK_KAN:
			case GameConstants_YYZHZ.WIK_WEI:
			case GameConstants_YYZHZ.WIK_XIAO:
			case GameConstants_YYZHZ.WIK_CHOU_XIAO:
			case GameConstants_YYZHZ.WIK_CHOU_WEI:
				if (_logic.color_hei(weaveItems[i].center_card) == false)
					count += 3;
				break;
			}
		}
		return count;

	}

	/**
	 * 碰、跑、偎、提的字牌
	 * 
	 * @param weaveItems
	 * @param weaveCount
	 * @return
	 */
	public boolean chunHeiDui(WeaveItem weaveItems[], int weaveCount) {
		for (int i = 0; i < weaveCount; i++) {
			switch (weaveItems[i].weave_kind) {
			case GameConstants_YYZHZ.WIK_TI_LONG:
			case GameConstants_YYZHZ.WIK_AN_LONG:
			case GameConstants_YYZHZ.WIK_PAO:
			case GameConstants_YYZHZ.WIK_SAO:
			case GameConstants_YYZHZ.WIK_PENG:
			case GameConstants_YYZHZ.WIK_CHOU_SAO:
			case GameConstants_YYZHZ.WIK_KAN:
			case GameConstants_YYZHZ.WIK_WEI:
			case GameConstants_YYZHZ.WIK_XIAO:
			case GameConstants_YYZHZ.WIK_CHOU_XIAO:
			case GameConstants_YYZHZ.WIK_CHOU_WEI:
				if (_logic.color_hei(weaveItems[i].center_card) == false)
					return false;
			case GameConstants_YYZHZ.WIK_XXD:
			case GameConstants_YYZHZ.WIK_DDX:
				return false;
			}
		}
		return true;

	}

	// @Override
	// public String get_game_des() {
	// String des = "";
	//
	// if (has_rule(GameConstants_YYZHZ.GAME_RULE_PLAYER_YYZGZ_THREE)) {
	// des += "三人场" + "\n";
	// }
	// if (has_rule(GameConstants_YYZHZ.GAME_RULE_PLAYER_YYZGZ_WANG_1)) {
	// des += "一张王牌" + "\n";
	// }else if (has_rule( GameConstants_YYZHZ.GAME_RULE_PLAYER_YYZGZ_WANG_2)) {
	// des += "二张王牌" + " ";
	// }else if (has_rule( GameConstants_YYZHZ.GAME_RULE_PLAYER_YYZGZ_WANG_3)) {
	// des += "三张王牌" + " ";
	// }else if (has_rule( GameConstants_YYZHZ.GAME_RULE_PLAYER_YYZGZ_WANG_4)) {
	// des += "四张王牌" + "\n";
	// }else{
	// des +="不带王牌"+ "\n";
	// }
	//
	// return des;
	// }

	@Override
	public boolean handler_player_ready(int seat_index, boolean is_cancel) {
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
		int count = this.getTablePlayerNumber();
		for (int i = 0; i < count; i++) {
			if (this.get_players()[i] == null) {
				_player_ready[i] = 0;
			}

			if (_player_ready[i] == 0) {
				this.refresh_room_redis_data(GameConstants.PROXY_ROOM_PLAYER, nt);
				return false;
			}
		}
		if (playerNumber == 0) {
			playerNumber = count;
		}
		handler_game_start();

		this.refresh_room_redis_data(GameConstants.PROXY_ROOM_UPDATE, nt);
		return false;
	}

	/**
	 * 听牌
	 * 
	 * @param cards
	 * @param cards_index
	 * @param weaveItem
	 * @param cbWeaveCount
	 * @returnt
	 */
	@Override
	public int get_hh_ting_card_twenty(int[] cards, int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int seat_index, int provate_index) {
		// 复制数据
		int cbCardIndexTemp[] = new int[GameConstants_YYZHZ.MAX_YYZHZ_INDEX];
		for (int i = 0; i < GameConstants_YYZHZ.MAX_YYZHZ_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		ChiHuRight chr = new ChiHuRight();

		int count = 0;
		int cbCurrentCard;

		int mj_count = GameConstants_YYZHZ.MAX_YYZHZ_INDEX;

		for (int i = 0; i < mj_count; i++) {
			if (this._logic.is_magic_index(i))
				continue;
			cbCurrentCard = _logic.switch_to_card_data(i);
			chr.set_empty();

			if (GameConstants.WIK_CHI_HU == this.analyse_chi_hu_card(cbCardIndexTemp, weaveItem, cbWeaveCount, seat_index, provate_index,
					cbCurrentCard, chr, GameConstants.HU_CARD_TYPE_ZIMO, true)) {
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
}
