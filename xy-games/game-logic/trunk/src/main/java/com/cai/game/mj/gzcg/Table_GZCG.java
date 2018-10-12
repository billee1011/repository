package com.cai.game.mj.gzcg;

import java.util.Arrays;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.Constants_MJ_GZCG;
import com.cai.common.constant.game.czbg.CZBGConstants;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.Player;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.WeaveItem;
import com.cai.common.util.PBUtil;
import com.cai.common.util.RandomUtil;
import com.cai.game.RoomUtil;
import com.cai.game.mj.AbstractMJTable;
import com.cai.game.mj.MJType;
import com.cai.game.util.AnalyseCardUtil;
import com.cai.game.util.GameUtilConstants;
import com.cai.service.PlayerServiceImpl;
import com.google.common.base.Strings;

import protobuf.clazz.Protocol.GameEndResponse;
import protobuf.clazz.Protocol.GameStartResponse;
import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomInfo;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;
import protobuf.clazz.gzcg.GZCGRsp.EveryJingInfo_GZCG;
import protobuf.clazz.gzcg.GZCGRsp.FanJing_GZCG;
import protobuf.clazz.gzcg.GZCGRsp.GameEndResponse_GZCG;

/**
 * 赣州冲关
 * 
 * @author hexinqi
 *
 */
public class Table_GZCG extends AbstractMJTable {

	private static final long serialVersionUID = -3740668572580145190L;

	public boolean isLast; // 是否最后一张牌
	public int isBian[]; // 是否胡的是边章
	public int canNotHu[]; // 过手胡
	public int canNotGang[]; // 每个玩家当前不能杠的牌[有杠不杠选择碰牌、要过一轮之后才能杠]
	public int outCardRound[]; // 补杠辅助判断
	public boolean isQiangGang; // 是否抢杠
	public boolean isDahu;
	public boolean isDianPao;
	public boolean isLiuJu;
	public ChiHuRight[] chr; // 胡牌类型
	public int jiePao[];
	public int dianPao[];
	public int jing[]; // 上精
	public int xiaJing[];
	public int xiaJingNumber;
	public int jingProvider[]; // 上精提供者

	public int jingScore[];
	public int huPaiScore[];
	public int jiangliScore[];
	public int mingGangScore[];
	public int anGangScore[];
	public int gangJingScore[];
	public int chaoZhuangScore[];
	public int maxFan[]; // 最大胡牌番数
	public ChiHuRight[] maxChr; // 最大胡牌番数时胡的牌型
	public int beginCardCount;
	public int chaoZhuangSeat;
	public int fisrtOut[];

	public int chongGuan[][];
	public boolean isBaWang[][];
	public int jingCount[][][];
	public int jingGetScore[][];
	public int piaoZhengScore[];
	public int piaoFuScore[];
	public int preBanker;

	public Table_GZCG() {
		super(MJType.GAME_TYPE_MJ_GZCG);
	}

	@Override
	public int getTablePlayerNumber() {
		if (playerNumber > 0) {
			return playerNumber;
		} else if (has_rule(Constants_MJ_GZCG.GAME_RULE_PLAYER_NUMBER_TWO)) {
			return 2;
		} else if (has_rule(Constants_MJ_GZCG.GAME_RULE_PLAYER_NUMBER_THREE)) {
			return 3;
		}
		return 4;
	}

	@Override
	protected void onInitTable() {
		_handler_chi_peng = new HandlerChiPeng_GZCG();
		_handler_dispath_card = new HandlerDispatchCard_GZCG();
		_handler_gang = new HandlerGang_GZCG();
		_handler_out_card_operate = new HandlerOutCardOperate_GZCG();
	}

	@Override
	public void on_init_table(int game_type_index, int game_rule_index, int game_round) {
		super.on_init_table(game_type_index, game_rule_index, game_round);
		isBian = new int[this.getTablePlayerNumber()];
		canNotHu = new int[this.getTablePlayerNumber()];
		canNotGang = new int[this.getTablePlayerNumber()];
		outCardRound = new int[this.getTablePlayerNumber()];
		jiePao = new int[this.getTablePlayerNumber()];
		dianPao = new int[this.getTablePlayerNumber()];
		jing = new int[2];
		xiaJing = new int[12];
		this.isLast = false;
		this.isQiangGang = false;
		this.isDahu = false;
		this.isDianPao = false;
		this.isLiuJu = false;
		this.preBanker = -1;

		if (has_rule(Constants_MJ_GZCG.GAME_RULE_DI_LEI)) {
			xiaJingNumber = 1;
		} else if (has_rule(Constants_MJ_GZCG.GAME_RULE_ZUOYOU_FAN)) {
			xiaJingNumber = 3;
		} else {
			xiaJingNumber = 5;
		}

		if (has_rule(Constants_MJ_GZCG.GAME_RULE_PLAYER_NUMBER_TWO)) {
			beginCardCount = Constants_MJ_GZCG.BEGIN_TWO;
		} else if (has_rule(Constants_MJ_GZCG.GAME_RULE_PLAYER_NUMBER_THREE)) {
			beginCardCount = Constants_MJ_GZCG.BEGIN_THREE;
		} else {
			beginCardCount = Constants_MJ_GZCG.BEGIN_FOUR;
		}

		this.jingScore = new int[this.getTablePlayerNumber()];
		this.huPaiScore = new int[this.getTablePlayerNumber()];
		this.jiangliScore = new int[this.getTablePlayerNumber()];
		this.mingGangScore = new int[this.getTablePlayerNumber()];
		this.anGangScore = new int[this.getTablePlayerNumber()];
		this.gangJingScore = new int[this.getTablePlayerNumber()];
		this.chaoZhuangScore = new int[this.getTablePlayerNumber()];
		this.jingProvider = new int[this.getTablePlayerNumber()];
		this.maxFan = new int[this.getTablePlayerNumber()];
		this.fisrtOut = new int[this.getTablePlayerNumber()];
		this.piaoZhengScore = new int[this.getTablePlayerNumber()];
		this.piaoFuScore = new int[this.getTablePlayerNumber()];

		chongGuan = new int[CZBGConstants.CZBG_RULE_PLAYER_6][CZBGConstants.CZBG_RULE_PLAYER_6];// 每一组精是否冲关
		isBaWang = new boolean[CZBGConstants.CZBG_RULE_PLAYER_6][CZBGConstants.CZBG_RULE_PLAYER_6];// 每一组精是否霸王精
		jingCount = new int[CZBGConstants.CZBG_RULE_PLAYER_6][CZBGConstants.CZBG_RULE_PLAYER_6][2];// 每一组精中正、副精的个数
		jingGetScore = new int[CZBGConstants.CZBG_RULE_PLAYER_6][CZBGConstants.CZBG_RULE_PLAYER_6];// 每一组精中得到的分数

		chr = new ChiHuRight[this.getTablePlayerNumber()];
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			chr[i] = new ChiHuRight();
		}
		maxChr = new ChiHuRight[this.getTablePlayerNumber()];
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			maxChr[i] = new ChiHuRight();
		}

		this.setMinPlayerCount(this.getTablePlayerNumber());
	}

	@Override
	public boolean reset_init_data() {
		this.isLast = false;
		this.isQiangGang = false;
		this.isDahu = false;
		this.isDianPao = false;
		this.isLiuJu = false;
		this.chaoZhuangSeat = -1;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			chr[i] = new ChiHuRight();
			maxChr[i] = new ChiHuRight();
		}
		Arrays.fill(isBian, 0);
		Arrays.fill(jiePao, 0);
		Arrays.fill(dianPao, 0);
		Arrays.fill(jing, 0);
		Arrays.fill(xiaJing, 0);
		Arrays.fill(jingScore, 0);
		Arrays.fill(huPaiScore, 0);
		Arrays.fill(jiangliScore, 0);
		Arrays.fill(mingGangScore, 0);
		Arrays.fill(anGangScore, 0);
		Arrays.fill(gangJingScore, 0);
		Arrays.fill(chaoZhuangScore, 0);
		Arrays.fill(jingProvider, -1);
		Arrays.fill(maxFan, 0);
		Arrays.fill(fisrtOut, 0);
		Arrays.fill(canNotHu, 0);
		Arrays.fill(piaoZhengScore, 0);
		Arrays.fill(piaoFuScore, 0);
		chongGuan = new int[CZBGConstants.CZBG_RULE_PLAYER_6][CZBGConstants.CZBG_RULE_PLAYER_6];// 每一组精是否冲关
		isBaWang = new boolean[CZBGConstants.CZBG_RULE_PLAYER_6][CZBGConstants.CZBG_RULE_PLAYER_6];// 每一组精是否霸王精
		jingCount = new int[CZBGConstants.CZBG_RULE_PLAYER_6][CZBGConstants.CZBG_RULE_PLAYER_6][2];// 每一组精中正、副精的个数
		jingGetScore = new int[CZBGConstants.CZBG_RULE_PLAYER_6][CZBGConstants.CZBG_RULE_PLAYER_6];// 每一组精中得到的分数

		this._logic.clean_magic_cards();

		return super.reset_init_data();
	}

	@Override
	protected void init_shuffle() {
		int[] cards = Constants_MJ_GZCG.DEFAULT;
		_repertory_card = new int[cards.length];
		shuffle(_repertory_card, cards);
	}

	@Override
	protected boolean on_game_start() {
		if (DEBUG_CARDS_MODE || BACK_DEBUG_CARDS_MODE) {
			test_cards();
		}

		// 每局开始初始化剩余牌局数
		if (getTablePlayerNumber() == 2) {
			beginCardCount = Constants_MJ_GZCG.BEGIN_TWO;
		} else if (getTablePlayerNumber() == 3) {
			beginCardCount = Constants_MJ_GZCG.BEGIN_THREE;
		} else {
			beginCardCount = Constants_MJ_GZCG.BEGIN_FOUR;
		}

		if (this._cur_round == 1) { // 第一局如果房主在房间内 则房主坐庄
			int banker = RandomUtil.generateRandomNumber(0, (getTablePlayerNumber() - 1));
			/*
			 * for (int i = 0; i < this.getTablePlayerNumber(); i++) { if
			 * (this.getRoom_owner_account_id() ==
			 * this.get_players()[i].getAccount_id()) { banker = i; break; } }
			 */
			GRR._banker_player = banker;
			this._cur_banker = banker;
			_current_player = banker;
		}
		this.chaoZhuangSeat = this._cur_banker;
		this.preBanker = this._cur_banker;

		_game_status = GameConstants.GS_MJ_PLAY;

		GameStartResponse.Builder gameStartResponse = GameStartResponse.newBuilder();
		gameStartResponse.setBankerPlayer(GRR._banker_player);
		gameStartResponse.setCurrentPlayer(_current_player);
		gameStartResponse.setLeftCardCount(GRR._left_card_count);

		int hand_cards[][] = new int[this.getTablePlayerNumber()][GameConstants.MAX_COUNT];

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			int hand_card_count = _logic.switch_to_cards_data_gzcg(GRR._cards_index[i], hand_cards[i]);
			gameStartResponse.addCardsCount(hand_card_count);
		}

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

			gameStartResponse.clearCardData();
			for (int j = 0; j < GameConstants.MAX_COUNT; j++) {
				gameStartResponse.addCardData(hand_cards[i][j]);
			}

			GRR._video_recode.addHandCards(cards);

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			this.load_room_info_data(roomResponse);
			this.load_common_status(roomResponse);
			roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
			roomResponse.setGameStart(gameStartResponse);
			roomResponse.setCurrentPlayer(_current_player == GameConstants.INVALID_SEAT ? _resume_player : _current_player);
			roomResponse.setGameStatus(_game_status);
			roomResponse.setLeftCardCount(GRR._left_card_count);
			this.send_response_to_player(i, roomResponse);
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
		this.load_room_info_data(roomResponse);
		this.load_common_status(roomResponse);
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < GameConstants.MAX_COUNT; j++) {
				cards.addItem(hand_cards[i][j]);
			}
			gameStartResponse.addCardsData(cards);
		}
		roomResponse.setGameStart(gameStartResponse);
		roomResponse.setLeftCardCount(GRR._left_card_count);
		GRR.add_room_response(roomResponse);

		calJing();

		return true;
	}

	/**
	 * 翻精
	 */
	public void calJing() {
		jing[0] = this._repertory_card[this._all_card_len - this.GRR._left_card_count];
		this.GRR._left_card_count--;

		jing[1] = this.getFuJing(jing[0]);

		/*
		 * jing[0] = 0x21; jing[1] = 0x22;
		 */
		if (this.BACK_DEBUG_CARDS_MODE || this.magic_card_decidor != 0) {
			jing[0] = this.magic_card_decidor;
			jing[1] = this.getFuJing(jing[0]);
		}

		for (int i = 0; i < 2; i++) {
			this._logic.add_magic_card_index(this._logic.switch_to_card_index(jing[i]));
		}

		fanJing();
	}

	public int getFuJing(int zhengJing) {
		int cardColor = this._logic.get_card_color(zhengJing);
		int cardValue = this._logic.get_card_value(zhengJing);

		if (cardColor == 3) {
			if (cardValue > 4) { // 中发白
				cardValue++;
				if (cardValue > 7) {
					cardValue = 5;
				}
			} else { // 东南西北
				cardValue++;
				if (cardValue > 4) {
					cardValue = 1;
				}
			}
		} else {
			cardValue++;
			if (cardValue > 9) {
				cardValue = 1;
			}
		}

		return (cardColor << 4) + cardValue;
	}

	public void fanJing() {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(Constants_MJ_GZCG.RESPONSE_GZCG_FAN_JING);

		FanJing_GZCG.Builder fanJing = FanJing_GZCG.newBuilder();
		fanJing.setXiaJing(xiaJingNumber);
		// jing[0] = 0x07;
		// jing[1] = 0x08;
		for (int i = 0; i < 2; i++) {
			fanJing.addZhengJing(jing[i]);
		}

		roomResponse.setCommResponse(PBUtil.toByteString(fanJing));
		RoomUtil.send_response_to_room(this, roomResponse);

		if (this.GRR != null) {
			this.GRR.add_room_response(roomResponse);
		}

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			this.GRR._card_count[i] = this._logic.switch_to_cards_data_gzcg(this.GRR._cards_index[i], this.GRR._cards_data[i]);
			changeCard(this.GRR._cards_data[i], this.GRR._card_count[i]);
			this.operate_player_cards(i, this.GRR._card_count[i], this.GRR._cards_data[i], 0, null);
		}
		// 检测听牌
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_playerStatus[i]._hu_card_count = this.get_ting_card(_playerStatus[i]._hu_cards, GRR._cards_index[i], GRR._weave_items[i],
					GRR._weave_count[i], i);
			if (_playerStatus[i]._hu_card_count > 0) {
				this.operate_chi_hu_cards(i, _playerStatus[i]._hu_card_count, _playerStatus[i]._hu_cards);
			}
		}
		this.exe_dispatch_card(_current_player, GameConstants.WIK_NULL, GameConstants.DELAY_SEND_CARD_DELAY);
	}

	public void changeCard(int cards[], int cardCount) {
		if (cards == null) {
			return;
		}
		for (int j = 0; j < cards.length; j++) {
			if (cards[j] == 0) {
				continue;
			}
			if (cards[j] == jing[0] || cards[j] == jing[1]) {
				cards[j] |= Constants_MJ_GZCG.JING;
			} else {
				cards[j] |= Constants_MJ_GZCG.NORMAL;
			}
		}
	}

	public boolean operate_auto_win_card(int seat_index, boolean isTurnOn) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_SWITCH_AUTO_WIN_CARD);
		roomResponse.setTarget(seat_index);
		roomResponse.setIsXiangGong(isTurnOn); // TODO false 表示隐藏 true 表示显示

		send_response_to_player(seat_index, roomResponse);

		return true;
	}

	@Override
	protected boolean on_handler_game_finish(int seat_index, int reason) {
		int real_reason = reason;

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_player_ready[i] = 0;
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(Constants_MJ_GZCG.RESPONSE_GZCG_GAME_END);
		GameEndResponse.Builder game_end = GameEndResponse.newBuilder();
		GameEndResponse_GZCG.Builder gameEndGzcg = GameEndResponse_GZCG.newBuilder();

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
			gameEndGzcg.setOtherJingNumber(this.xiaJingNumber);
			for (int j = 0; j < 2; j++) {
				gameEndGzcg.addZhengJing(this.jing[j]);
			}
			for (int j = 0; j < this.xiaJingNumber; j++) {
				this.xiaJing[j * 2] = this._repertory_card[this._all_card_len - (this.GRR._left_card_count - j)];
				// WalkerGeek 测试
				// this.xiaJing[j * 2] = 0x19;
				// WalkerGeek 测试
				this.xiaJing[j * 2 + 1] = this.getFuJing(this.xiaJing[j * 2]);
				gameEndGzcg.addOtherJing(this.xiaJing[j * 2]);
				gameEndGzcg.addOtherJing(this.xiaJing[j * 2 + 1]);
			}

			// WalkerGeek 只有正常结束或者流局才结算精分
			if (reason == GameConstants.Game_End_DRAW || reason == GameConstants.Game_End_NORMAL) {
				calScore(this.isLiuJu, seat_index);
			}

			game_end.setRoundOverType(1);
			game_end.setStartTime(GRR._start_time);

			game_end.setGameTypeIndex(GRR._game_type_index);
			roomResponse.setLeftCardCount(GRR._left_card_count);

			for (int k = 0; k < this.getTablePlayerNumber(); k++) {
				EveryJingInfo_GZCG.Builder info = EveryJingInfo_GZCG.newBuilder();
				for (int j = 0; j < this.xiaJingNumber + 1; j++) {
					info.addZhengJingCount(this.jingCount[k][j][0]);
					info.addFuJingCount(this.jingCount[k][j][1]);
					info.addIsBaWangJing(this.isBaWang[j][k]);
					info.addChongGuanScore(this.chongGuan[j][k]);
					info.addEveryJingScore(this.jingGetScore[k][j]);
					info.addJingSocre(this.jingCount[k][j][0] * 2 + this.jingCount[k][j][1]);
				}
				gameEndGzcg.addJingInfo(info);
			}

			GRR._end_type = reason;

			float lGangScore[] = new float[this.getTablePlayerNumber()];
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				if (GRR._end_type != GameConstants.Game_End_DRAW && GRR._end_type != GameConstants.Game_End_RELEASE_PLAY) { // 荒庄荒杠
																															// 中途解散也荒杠
					for (int j = 0; j < GRR._gang_score[i].gang_count; j++) {
						for (int k = 0; k < this.getTablePlayerNumber(); k++) {
							lGangScore[k] += GRR._gang_score[i].scores[j][k];// 杠牌，每个人的分数
						}
					}
				}
				if (GRR._end_type == GameConstants.Game_End_RELEASE_PLAY) {
					this.isLiuJu = true;
				}
				for (int j = 0; j < this.getTablePlayerNumber(); j++) {
					_player_result.lost_fan_shu[i][j] += GRR._lost_fan_shu[i][j];
				}
			}

			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				this.jiangliScore[i] = this.mingGangScore[i] + this.anGangScore[i] + this.gangJingScore[i] + this.chaoZhuangScore[i];
				GRR._game_score[i] = this.jiangliScore[i] + jingScore[i] + huPaiScore[i];
				_player_result.game_score[i] += GRR._game_score[i];

				gameEndGzcg.addJingScore(this.jingScore[i]);
				gameEndGzcg.addHuPaiScore(this.huPaiScore[i]);
				gameEndGzcg.addJiangLiScore(this.jiangliScore[i]);
				gameEndGzcg.addMingGang(this.mingGangScore[i]);
				gameEndGzcg.addAnGang(this.anGangScore[i]);
				gameEndGzcg.addGangJing(this.gangJingScore[i]);
				gameEndGzcg.addChaoZhuang(this.chaoZhuangScore[i]);
				gameEndGzcg.addTotalScore((int) this.GRR._game_score[i]);
				gameEndGzcg.addPiaoZhengJing(this.piaoZhengScore[i]);
				gameEndGzcg.addPiaoFuJing(this.piaoFuScore[i]);
			}

			this.load_player_info_data(roomResponse);

			game_end.setGameRound(_game_round);
			game_end.setCurRound(_cur_round);

			game_end.setCellScore(this.get_di_fen());

			game_end.setBankerPlayer(GRR._banker_player);
			game_end.setLeftCardCount(GRR._left_card_count);

			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
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

			this.set_result_describe(seat_index);
			gameEndGzcg.setResultDesc(seat_index >= 0 ? GRR._result_des[seat_index] : "");
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				GRR._card_count[i] = _logic.switch_to_cards_data_gzcg(GRR._cards_index[i], GRR._cards_data[i]);

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
			}
		} else if ((!is_sys()) && (reason == GameConstants.Game_End_RELEASE_PLAY || reason == GameConstants.Game_End_RELEASE_NO_BEGIN
				|| reason == GameConstants.Game_End_RELEASE_RESULT || reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT || reason == GameConstants.Game_End_RELEASE_SYSTEM)) {
			end = true;
			// real_reason = GameConstants.Game_End_DRAW;
			game_end.setRoomOverType(1);
			game_end.setPlayerResult(this.process_player_result(reason));
		}
		game_end.setEndType(real_reason);
		roomResponse.setGameEnd(game_end);

		gameEndGzcg.setGameEnd(game_end);
		roomResponse.setCommResponse(PBUtil.toByteString(gameEndGzcg));
		this.send_response_to_room(roomResponse);
		game_end.setCommResponse(PBUtil.toByteString(gameEndGzcg));
		record_game_round(game_end);

		if (reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT || reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT) {
			for (int j = 0; j < this.getTablePlayerNumber(); j++) {
				Player player = this.get_players()[j];
				if (player == null) {
					continue;
				}
				send_error_notify(j, 1, "游戏解散成功!");
			}
		}

		this.magic_card_decidor = 0;
		if (end && (!is_sys())) {
			PlayerServiceImpl.getInstance().delRoomId(this.getRoom_id());
		}

		if (!is_sys()) {
			GRR = null;
		} else {
			clear_score_in_gold_room();
		}

		return false;
	}

	/**
	 * 算分
	 * 
	 * @param isLiuJu
	 *            是否流局 流局只计算精分、杠分、杠精分 (上精、下精) 胡牌才多计算胡牌分、飘精分
	 * @param seatIndex
	 *            胡牌玩家
	 */
	private void calScore(boolean isLiuJu, int seatIndex) {
		if (seatIndex < 0) {
			return;
		}
		int bei = has_rule(Constants_MJ_GZCG.GAME_RULE_FAN_BEI) ? 2 : 1;
		int tempBeis[] = new int[this.getTablePlayerNumber()];
		Arrays.fill(tempBeis, 1);
		tempBeis[this._cur_banker] = bei;

		if (!isLiuJu) {
			// for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			// 计算飘精 胡牌玩家才有飘精
			int piaoScore = 0, zScore = 0, fScore = 0;
			for (int j = 0; j < GRR._discard_count[seatIndex]; j++) {
				if (jing[0] == (GRR._discard_cards[seatIndex][j] & 0x000FF)) {
					piaoScore += 10;
					zScore += 10;
				} else if (jing[1] == (GRR._discard_cards[seatIndex][j] & 0x000FF)) {
					piaoScore += 5;
					fScore += 5;
				}
			}

			// WalkerGeek 组合中的飘分也要算
			for (int k = 0; k < getTablePlayerNumber(); k++) {
				if (k == seatIndex) {
					continue;
				}

				for (int i = 0; i < GRR._weave_count[k]; i++) {
					boolean can = false;
					if (GRR._weave_items[k][i].type == GameConstants.GANG_TYPE_ADD_GANG) {
						if (GRR._weave_items[k][i].provide_player_before == seatIndex) {
							can = true;
						}
					} else {
						if (GRR._weave_items[k][i].provide_player == seatIndex) {
							can = true;
						}
					}
					if (can) {
						if (jing[0] == (GRR._weave_items[k][i].center_card & 0x000FF)) {
							piaoScore += 10;
							zScore += 10;
						} else if (jing[1] == (GRR._weave_items[k][i].center_card & 0x000FF)) {
							piaoScore += 5;
							fScore += 5;
						}
					}
				}
			}

			if (piaoScore > 0) {
				for (int z = 0; z < this.getTablePlayerNumber(); z++) {
					if (z == seatIndex) {
						this.jingScore[z] += piaoScore * (this.getTablePlayerNumber() - 1);
						this.piaoZhengScore[z] += zScore * (this.getTablePlayerNumber() - 1);
						this.piaoFuScore[z] += fScore * (this.getTablePlayerNumber() - 1);
					} else {
						this.jingScore[z] -= piaoScore;
						this.piaoZhengScore[z] -= zScore;
						this.piaoFuScore[z] -= fScore;
					}
				}
			}
			// }

			int fan = Constants_MJ_GZCG.FAN_BASE;
			boolean isDeguo = false;
			int dianPao = -1;
			chr[seatIndex] = this.GRR._chi_hu_rights[seatIndex];
			if (!(chr[seatIndex].opr_and(Constants_MJ_GZCG.CHR_GANG_SHANG_HUA).is_empty())) {
				fan *= Constants_MJ_GZCG.FAN_GANG_KAI;
			}
			if (!(chr[seatIndex].opr_and(Constants_MJ_GZCG.CHR_TIAN_HU).is_empty())) {
				fan *= Constants_MJ_GZCG.FAN_TIAN_HU;
			}
			if (!(chr[seatIndex].opr_and(Constants_MJ_GZCG.CHR_DI_HU).is_empty())) {
				fan *= Constants_MJ_GZCG.FAN_TIAN_HU;
			}
			if (!(chr[seatIndex].opr_and(Constants_MJ_GZCG.CHR_JING_DIAO).is_empty())) {
				fan *= Constants_MJ_GZCG.FAN_JING_DIAO;
			}
			if (!(chr[seatIndex].opr_and(Constants_MJ_GZCG.CHR_DE_GUO).is_empty())) {
				isDeguo = true;
				fan *= Constants_MJ_GZCG.FAN_DE_GUO;
			}
			if (!(chr[seatIndex].opr_and(Constants_MJ_GZCG.CHR_QI_XING_SHI_SAN_LAN).is_empty())) {
				fan *= Constants_MJ_GZCG.FAN_QI_XING_SHI_SAN_LAN;
			} else if (!(chr[seatIndex].opr_and(Constants_MJ_GZCG.CHR_SHI_SAN_LAN).is_empty())) {
				fan *= Constants_MJ_GZCG.FAN_SHI_SAN_LAN;
			}
			if (!(chr[seatIndex].opr_and(Constants_MJ_GZCG.CHR_XIAO_QI_DUI).is_empty())) {
				fan *= Constants_MJ_GZCG.FAN_XIAO_QI_DUI;
			}
			if (!(chr[seatIndex].opr_and(Constants_MJ_GZCG.CHR_PENG_HU).is_empty())) {
				fan *= Constants_MJ_GZCG.FAN_DA_QI_DUI;
			}
			// 点炮算分一样
			if (!(chr[seatIndex].opr_and(Constants_MJ_GZCG.CHR_FANG_PAO).is_empty())) {
				dianPao = GRR._provider[seatIndex];
			}

			int score = fan * this.get_di_fen();

			for (int z = 0; z < this.getTablePlayerNumber(); z++) {
				if (z == seatIndex) {
					this.huPaiScore[z] += ((score + (isDeguo ? Constants_MJ_GZCG.DE_GUO_EXTRA_SCORE : 0)) * (this.getTablePlayerNumber() - 1));
					if (dianPao >= 0) {
						this.huPaiScore[z] -= ((isDeguo ? Constants_MJ_GZCG.DE_GUO_EXTRA_SCORE : 0) * (this.getTablePlayerNumber() - 2)
								+ score * (this.getTablePlayerNumber() - 2) / 2);
					}
				} else {
					this.huPaiScore[z] -= (score + (isDeguo ? Constants_MJ_GZCG.DE_GUO_EXTRA_SCORE : 0));
					if (dianPao >= 0 && dianPao != z) {
						this.huPaiScore[z] += (score / 2 + (isDeguo ? Constants_MJ_GZCG.DE_GUO_EXTRA_SCORE : 0));
					}
				}
			}
			if (has_rule(Constants_MJ_GZCG.GAME_RULE_FAN_BEI) && chr[seatIndex].opr_and(Constants_MJ_GZCG.CHR_TIAN_HU).is_empty()
					&& chr[seatIndex].opr_and(Constants_MJ_GZCG.CHR_DI_HU).is_empty()) { // 庄闲翻倍
				if (seatIndex == this.preBanker) {
					for (int z = 0; z < this.getTablePlayerNumber(); z++) {
						if (z == seatIndex) {
							this.huPaiScore[z] += (score * (this.getTablePlayerNumber() - 1));
							if (dianPao >= 0) {
								this.huPaiScore[z] -= (score * (this.getTablePlayerNumber() - 2) / 2);
							}
						} else {
							this.huPaiScore[z] -= score;
							if (dianPao >= 0 && dianPao != z) {
								this.huPaiScore[z] += score / 2;
							}
						}
					}
				} else {
					this.huPaiScore[seatIndex] += score;
					this.huPaiScore[this.preBanker] -= score;
				}
			}
			if (!(chr[seatIndex].opr_and(Constants_MJ_GZCG.CHR_QIANG_GANG_HU).is_empty())) {
				for (int z = 0; z < this.getTablePlayerNumber(); z++) {
					if (z == seatIndex || z == GRR._provider[seatIndex]) {
						continue;
					}
					this.huPaiScore[GRR._provider[seatIndex]] += this.huPaiScore[z];
					this.huPaiScore[z] -= this.huPaiScore[z];
				}
			}

			boolean flag = true;
			for (int z = 0; z < this.getTablePlayerNumber(); z++) {
				if (this.fisrtOut[z] == 0) {
					flag = false;
					break;
				}
			}
			if (chaoZhuangSeat >= 0 && this.GRR._left_card_count <= this.beginCardCount - this.getTablePlayerNumber() + 1 && flag) {
				for (int z = 0; z < this.getTablePlayerNumber(); z++) {
					if (z == this.chaoZhuangSeat) {
						this.chaoZhuangScore[z] -= (this.getTablePlayerNumber() - 1) * Constants_MJ_GZCG.CHAO_ZHUANG_SCORE;
					} else {
						this.chaoZhuangScore[z] += Constants_MJ_GZCG.CHAO_ZHUANG_SCORE;
					}
				}
			}
		}
		countJingCount(jing[0], jing[1], seatIndex, true, 0); // 上精
		for (int i = 0; i < this.xiaJingNumber; i++) {
			countJingCount(this.xiaJing[i * 2], this.xiaJing[i * 2 + 1], seatIndex, false, i + 1);
		}

		// if (has_rule(Constants_MJ_GZCG.GAME_RULE_FAN_BEI)) {
		// for (int i = 0; i < this.getTablePlayerNumber(); i++) {
		// this.mingGangScore[i] *= 2;
		// this.anGangScore[i] *= 2;
		// }
		// }
	}

	/**
	 * 
	 * @param zheng
	 * @param fu
	 * @param seatIndex
	 * @param isShangJing
	 * @param index
	 *            表示第几组精
	 */
	private void countJingCount(int zheng, int fu, int seatIndex, boolean isShangJing, int index) {
		int jingCount[][] = new int[this.getTablePlayerNumber()][2];
		int lastJingCount[] = new int[this.getTablePlayerNumber()];
		int curJingScore[] = new int[this.getTablePlayerNumber()];

		if (seatIndex >= 0) {
			int card = this.GRR._chi_hu_card[seatIndex][0] & 0x000FF;
			if (zheng == card) {
				jingCount[seatIndex][0]++;
			} else if (fu == card) {
				jingCount[seatIndex][1]++;
			}
		}

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			for (int j = 0; j < GRR._discard_count[i]; j++) {
				if (zheng == (GRR._discard_cards[i][j] & 0x000FF)) {
					jingCount[i][0]++;
				} else if (fu == (GRR._discard_cards[i][j] & 0x000FF)) {
					jingCount[i][1]++;
				}
			}

			int cardDatas[] = new int[GameConstants.MAX_COUNT];
			int cardCount = this._logic.switch_to_cards_data(this.GRR._cards_index[i], cardDatas);
			for (int j = 0; j < cardCount; j++) {
				if (zheng == cardDatas[j]) {
					jingCount[i][0]++;
				} else if (fu == cardDatas[j]) {
					jingCount[i][1]++;
				}
			}

			for (int j = 0; j < this.GRR._weave_count[i]; j++) {
				if (this.GRR._weave_items[i][j].weave_kind == GameConstants.WIK_GANG) {
					if (this.GRR._weave_items[i][j].provide_player == i) { // 暗杠或者续杠
						if (has_rule(Constants_MJ_GZCG.GAME_RULE_QUAN_BU_JING)
								|| (has_rule(Constants_MJ_GZCG.GAME_RULE_JIN_SHANG_JING) && isShangJing)) {
							if (zheng == (this.GRR._weave_items[i][j].center_card & 0x000FF)) {
								// jingCount[i][0] += 4;
								for (int k = 0; k < this.getTablePlayerNumber(); k++) {
									if (k == i) {
										continue;
									}
									this.gangJingScore[i] += 10;
									this.gangJingScore[k] -= 10;
								}
							} else if (fu == (this.GRR._weave_items[i][j].center_card & 0x000FF)) {
								// jingCount[i][1] += 4;
								for (int k = 0; k < this.getTablePlayerNumber(); k++) {
									if (k == i) {
										continue;
									}
									this.gangJingScore[i] += 10;
									this.gangJingScore[k] -= 10;
								}
							}
						}

						// 暗杆精牌分数
						if (zheng == (this.GRR._weave_items[i][j].center_card & 0x000FF)) {
							jingCount[i][0] += 4;
						} else if (fu == (this.GRR._weave_items[i][j].center_card & 0x000FF)) {
							jingCount[i][1] += 4;
						}

						if (isShangJing) {
							if (this.GRR._weave_items[i][j].weave_card[0] == 1) { // 明杠
								for (int k = 0; k < this.getTablePlayerNumber(); k++) {
									if (k == i) {
										continue;
									}
									this.mingGangScore[i] += 2;
									this.mingGangScore[k] -= 2;
								}
							} else {
								for (int k = 0; k < this.getTablePlayerNumber(); k++) {
									if (k == i) {
										continue;
									}
									this.anGangScore[i] += 2;
									this.anGangScore[k] -= 2;
								}
							}
						}
					} else { // 接杠
						// if
						// (has_rule(Constants_MJ_GZCG.GAME_RULE_QUAN_BU_JING)
						// ||
						// (has_rule(Constants_MJ_GZCG.GAME_RULE_JIN_SHANG_JING)
						// && isShangJing)) {
						if (isShangJing) {
							if (zheng == (this.GRR._weave_items[i][j].center_card & 0x000FF)) {
								jingCount[i][0] += 4;
								this.gangJingScore[i] += (this.getTablePlayerNumber() - 1) * 10;
								this.gangJingScore[this.GRR._weave_items[i][j].provide_player] -= (this.getTablePlayerNumber() - 1) * 10;
							} else if (fu == (this.GRR._weave_items[i][j].center_card & 0x000FF)) {
								jingCount[i][1] += 4;
								this.gangJingScore[i] += (this.getTablePlayerNumber() - 1) * 10;
								this.gangJingScore[this.GRR._weave_items[i][j].provide_player] -= (this.getTablePlayerNumber() - 1) * 10;
							}
						} else {
							if (zheng == (this.GRR._weave_items[i][j].center_card & 0x000FF)) {
								jingCount[i][0] += 4;
								for (int k = 0; k < this.getTablePlayerNumber(); k++) {
									if (k == i) {
										continue;
									}
									this.gangJingScore[i] += 10;
									this.gangJingScore[k] -= 10;
								}
							} else if (fu == (this.GRR._weave_items[i][j].center_card & 0x000FF)) {
								jingCount[i][1] += 4;
								for (int k = 0; k < this.getTablePlayerNumber(); k++) {
									if (k == i) {
										continue;
									}
									this.gangJingScore[i] += 10;
									this.gangJingScore[k] -= 10;
								}
							}
						}
						if (isShangJing) {
							for (int k = 0; k < this.getTablePlayerNumber(); k++) {
								if (k == i) {
									continue;
								}
								this.mingGangScore[i] += 2;
								this.mingGangScore[k] -= 2;
							}
							// this.mingGangScore[i] +=
							// (this.getTablePlayerNumber() - 1) * 2;
							// this.mingGangScore[this.GRR._weave_items[i][j].provide_player]
							// -= (this.getTablePlayerNumber() - 1) * 2;
						}
					}
				} else {
					switch (this.GRR._weave_items[i][j].weave_kind) {
					case GameConstants.WIK_PENG:
						if (zheng == (this.GRR._weave_items[i][j].center_card & 0x000FF)) {
							jingCount[i][0] += 3;
						} else if (fu == (this.GRR._weave_items[i][j].center_card & 0x000FF)) {
							jingCount[i][1] += 3;
						}
						break;
					case GameConstants.WIK_RIGHT:
						if (this._logic.get_card_color(this.GRR._weave_items[i][j].center_card) == 3) {
							int center_card = this.GRR._weave_items[i][j].center_card & 0x000FF;
							int pre_card = 0x30 + this.getFive(this.GRR._weave_items[i][j].center_card);
							int next_card = 0x30 + this.getFour(this.GRR._weave_items[i][j].center_card);
							if (zheng == center_card || pre_card == zheng || next_card == zheng) {
								jingCount[i][0]++;
							}
							if (fu == center_card || pre_card == fu || next_card == fu) {
								jingCount[i][1]++;
							}
						} else {
							for (int k = 0; k < 3; k++) {
								if (zheng == (this.GRR._weave_items[i][j].center_card & 0x000FF) - k) {
									jingCount[i][0]++;
								}
								if (fu == (this.GRR._weave_items[i][j].center_card & 0x000FF) - k) {
									jingCount[i][1]++;
								}
							}
						}
						break;
					case GameConstants.WIK_LEFT:
						if (this._logic.get_card_color(this.GRR._weave_items[i][j].center_card) == 3) {
							int center_card = this.GRR._weave_items[i][j].center_card & 0x000FF;
							int pre_card = 0x30 + this.getFive(this.GRR._weave_items[i][j].center_card);
							int next_card = 0x30 + this.getFour(this.GRR._weave_items[i][j].center_card);
							if (zheng == center_card || pre_card == zheng || next_card == zheng) {
								jingCount[i][0]++;
							}
							if (fu == center_card || pre_card == fu || next_card == fu) {
								jingCount[i][1]++;
							}
						} else {
							for (int k = 0; k < 3; k++) {
								if (zheng == (this.GRR._weave_items[i][j].center_card & 0x000FF) + k) {
									jingCount[i][0]++;
								}
								if (fu == (this.GRR._weave_items[i][j].center_card & 0x000FF) + k) {
									jingCount[i][1]++;
								}
							}
						}
						break;
					case GameConstants.WIK_CENTER:
						if (this._logic.get_card_color(this.GRR._weave_items[i][j].center_card) == 3) {
							int center_card = this.GRR._weave_items[i][j].center_card & 0x000FF;
							int pre_card = 0x30 + this.getFive(this.GRR._weave_items[i][j].center_card);
							int next_card = 0x30 + this.getFour(this.GRR._weave_items[i][j].center_card);
							if (zheng == center_card || pre_card == zheng || next_card == zheng) {
								jingCount[i][0]++;
							}
							if (fu == center_card || pre_card == fu || next_card == fu) {
								jingCount[i][1]++;
							}
						} else {
							for (int k = -1; k < 2; k++) {
								if (zheng == (this.GRR._weave_items[i][j].center_card & 0x000FF) - k) {
									jingCount[i][0]++;
								}
								if (fu == (this.GRR._weave_items[i][j].center_card & 0x000FF) - k) {
									jingCount[i][1]++;
								}
							}
						}
						break;
					default:
						break;
					}
				}
			}
		}

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			this.jingCount[i][index][0] = jingCount[i][0];
			this.jingCount[i][index][1] = jingCount[i][1];

			lastJingCount[i] = jingCount[i][0] + jingCount[i][1];
			curJingScore[i] = jingCount[i][0] * 2 + jingCount[i][1];

			if (curJingScore[i] >= 5) {
				this.chongGuan[index][i] = curJingScore[i] - 3;
				curJingScore[i] = curJingScore[i] * (curJingScore[i] - 3);
			}
		}
		int baWangJing = -1;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (lastJingCount[i] > 0) {
				if (baWangJing >= 0) {
					baWangJing = -1;
					break;
				}
				baWangJing = i;
			}
		}
		if (baWangJing > -1) { // 霸王精翻倍
			curJingScore[baWangJing] *= 2;
			this.isBaWang[index][baWangJing] = true;
		}
		int countJingScore[] = new int[this.getTablePlayerNumber()];
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (isShangJing && curJingScore[i] > 5 && this.jingProvider[i] > -1 && this.jingProvider[i] != i) { // 上精才需要包精
				countJingScore[i] += curJingScore[i] * (this.getTablePlayerNumber() - 1);
				countJingScore[this.jingProvider[i]] -= curJingScore[i] * (this.getTablePlayerNumber() - 1);

				this.jingGetScore[i][index] = countJingScore[i];
				this.jingGetScore[this.jingProvider[i]][index] = countJingScore[this.jingProvider[i]];
			} else {
				for (int j = 0; j < this.getTablePlayerNumber(); j++) {
					if (j == i) {
						countJingScore[j] += curJingScore[i] * (this.getTablePlayerNumber() - 1);
						this.jingGetScore[i][index] = countJingScore[i];
					} else {
						countJingScore[j] -= curJingScore[i];
						this.jingGetScore[j][index] = countJingScore[j];
					}
				}
			}
		}
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			this.jingScore[i] += countJingScore[i];
		}
	}

	@Override
	public int analyse_chi_hu_card(int[] cards_index, WeaveItem[] weaveItems, int weave_count, int cur_card, ChiHuRight chiHuRight, int card_type,
			int seat_index) {
		return 0;
	}

	public int analyse_chi_hu_card_new(int[] cards_index, WeaveItem[] weaveItems, int weave_count, int cur_card, ChiHuRight chiHuRight, int card_type,
			int seat_index, int provide_index) {
		maxFan[seat_index] = 0;
		if (cur_card == 0) {
			return GameConstants.WIK_NULL;
		}

		int countJing = 0;
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
			if (cards_index[i] > 0) {
				int card = this._logic.switch_to_card_data(i);
				if (card == jing[0] || card == jing[1]) {
					countJing += cards_index[i];
				}
			}
		}

		int cbChiHuKind = GameConstants.WIK_NULL;

		// int[] tem_jing = new int[2]; // 精牌
		int[] magic_cards_index = new int[GameUtilConstants.MAX_MAGIC_INDEX_COUNT];
		int magic_card_count = 0;

		magic_card_count = this._logic.get_magic_card_count();

		// WalkerGeek 抢杠胡还原
		int fujing = GameConstants.INVALID_CARD;
		if ((card_type == Constants_MJ_GZCG.CHR_QIANG_GANG_HU && isJing(cur_card))) {
			fujing = this.jing[1];
			this.jing[1] = GameConstants.INVALID_CARD;
			magic_card_count = 1;
		}

		if (magic_card_count > 2) {
			magic_card_count = 2;
		}
		for (int i = 0; i < magic_card_count; i++) {
			magic_cards_index[i] = _logic.get_magic_card_index(i);
		}

		/*
		 * tem_jing[0] = jing[0]; tem_jing[1] = jing[1]; }
		 */

		if (cur_card != GameConstants.INVALID_VALUE) {
			cbCardIndexTemp[_logic.switch_to_card_index(cur_card)]++;
		}

		// 正常胡牌
		boolean can_win_with_magic = AnalyseCardUtil.analyse_feng_chi_by_cards_index(cards_index, _logic.switch_to_card_index(cur_card),
				magic_cards_index, magic_card_count);

		boolean isZimo = seat_index == provide_index;
		// 这里抢杠胡做自摸处理
		if (card_type == Constants_MJ_GZCG.CHR_QIANG_GANG_HU || card_type == Constants_MJ_GZCG.CHR_GANG_SHANG_HUA
				|| card_type == Constants_MJ_GZCG.CHR_DI_HU) {
			isZimo = true;
		}

		// 只有自摸才会有精钓 天胡、地胡、十三烂、大七对、小七对不受精必钓限制
		boolean jingDiao = this.check_jing_diao(cbCardIndexTemp, weaveItems, weave_count, cur_card, magic_cards_index, magic_card_count);

		// 大七对 即碰胡
		int daQiDui = is_da_qi_dui(cbCardIndexTemp, weaveItems, weave_count, cur_card, chiHuRight, magic_cards_index, magic_card_count,
				seat_index == provide_index || card_type == Constants_MJ_GZCG.CHR_TIAN_HU || card_type == Constants_MJ_GZCG.CHR_DI_HU, jingDiao);

		if (daQiDui > 0) {
			setMaxFan(seat_index, daQiDui, chiHuRight);
		}
		// 十三烂
		int shiSanLan = 0;
		if (!jingDiao) {
			shiSanLan = this.is_shi_san_lan(cbCardIndexTemp, weaveItems, weave_count, cur_card, chiHuRight, isZimo);
			setMaxFan(seat_index, shiSanLan, chiHuRight);
		}
		// 小七对
		int xiaoQiDui = is_qi_xiao_dui(cbCardIndexTemp, weaveItems, weave_count, cur_card, chiHuRight,
				seat_index == provide_index || card_type == Constants_MJ_GZCG.CHR_TIAN_HU || card_type == Constants_MJ_GZCG.CHR_DI_HU, jingDiao);
		setMaxFan(seat_index, xiaoQiDui, chiHuRight);
		// 平胡
		int pingHu = is_ping_hu(cards_index, cur_card, chiHuRight, magic_cards_index, magic_card_count, isZimo, jingDiao, card_type);
		setMaxFan(seat_index, pingHu, chiHuRight);

		// WalkerGeek 18.4.25 4. 精钓了不能抢杠胡和接炮，不管任何牌型
		boolean isJingDiao = check_jing_diao_qi_xiao_dui(cbCardIndexTemp, weaveItems, weave_count, cur_card);
		if ((jingDiao || isJingDiao) && (card_type == Constants_MJ_GZCG.CHR_QIANG_GANG_HU || card_type == Constants_MJ_GZCG.CHR_JIE_PAO)) {
			chiHuRight.set_empty();
			if (fujing != GameConstants.INVALID_CARD) {
				this.jing[1] = fujing;
			}
			return GameConstants.WIK_NULL;
		}

		chiHuRight.set_empty();
		boolean isTianHu = isZimo && card_type == Constants_MJ_GZCG.CHR_TIAN_HU
				&& (can_win_with_magic || daQiDui > 0 || shiSanLan > 0 || xiaoQiDui > 0 || pingHu > 0);
		boolean isDiHu = card_type == Constants_MJ_GZCG.CHR_DI_HU
				&& (can_win_with_magic || daQiDui > 0 || shiSanLan > 0 || xiaoQiDui > 0 || pingHu > 0);
		if ((isTianHu || isDiHu) && this.GRR._left_card_count == this.beginCardCount) {
			this.maxFan[seat_index] = Constants_MJ_GZCG.FAN_TIAN_HU;
			chiHuRight.opr_or(isTianHu ? Constants_MJ_GZCG.CHR_TIAN_HU : Constants_MJ_GZCG.CHR_DI_HU);
			if (fujing != GameConstants.INVALID_CARD) {
				this.jing[1] = fujing;
			}
			return GameConstants.WIK_CHI_HU;
		}

		chiHuRight.set_empty();
		chiHuRight.copy(this.maxChr[seat_index]);
		if (card_type == Constants_MJ_GZCG.CHR_GANG_SHANG_HUA) {
			chiHuRight.opr_or(Constants_MJ_GZCG.CHR_GANG_SHANG_HUA);
			this.maxFan[seat_index] *= Constants_MJ_GZCG.FAN_GANG_KAI;
		}

		if (countJing > 0 && has_rule(Constants_MJ_GZCG.GAME_RULE_ZI_MO)) { // 仅自摸
			if (!isZimo && seat_index != provide_index && chiHuRight.opr_and(Constants_MJ_GZCG.CHR_DE_GUO).is_empty()) {
				if (fujing != GameConstants.INVALID_CARD) {
					this.jing[1] = fujing;
				}
				return GameConstants.WIK_NULL;
			}
		}
		// 平胡杠开和抢杠胡：应该当做特殊牌型看，不受精必掉限制
		if (!(daQiDui > 0 || xiaoQiDui > 0 || shiSanLan > 0 || isTianHu || isDiHu || card_type == Constants_MJ_GZCG.CHR_QIANG_GANG_HU
				|| card_type == Constants_MJ_GZCG.CHR_GANG_SHANG_HUA)) {
			// if (!(daQiDui > 0 || xiaoQiDui > 0 || shiSanLan > 0 || isTianHu
			// || isDiHu || (pingHu > 0 && card_type ==
			// Constants_MJ_GZCG.CHR_QIANG_GANG_HU) || (pingHu > 0 && card_type
			// == Constants_MJ_GZCG.CHR_GANG_SHANG_HUA))) {
			if (countJing > 0 && has_rule(Constants_MJ_GZCG.GAME_RULE_BI_DIAO)) { // 精必钓
				if (!jingDiao && chiHuRight.opr_and(Constants_MJ_GZCG.CHR_DE_GUO).is_empty()) {
					if (fujing != GameConstants.INVALID_CARD) {
						this.jing[1] = fujing;
					}
					return GameConstants.WIK_NULL;
				}
				if (jingDiao && chiHuRight.opr_and(Constants_MJ_GZCG.CHR_DE_GUO).is_empty() && seat_index != provide_index) {
					if (fujing != GameConstants.INVALID_CARD) {
						this.jing[1] = fujing;
					}
					return GameConstants.WIK_NULL;
				}
			}
			if (has_rule(Constants_MJ_GZCG.GAME_RULE_ZI_MO)) {
				if ((cur_card & 0x000FF) == jing[0] || (cur_card & 0x000FF) == jing[1]) {
					if (!isZimo && chiHuRight.opr_and(Constants_MJ_GZCG.CHR_DE_GUO).is_empty()) {
						if (fujing != GameConstants.INVALID_CARD) {
							this.jing[1] = fujing;
						}
						return GameConstants.WIK_NULL;
					}
				}
			}
		}

		if (card_type == Constants_MJ_GZCG.HU_CARD_TYPE_ZI_MO && seat_index == provide_index) {
			chiHuRight.opr_or(Constants_MJ_GZCG.CHR_ZI_MO);
		} else if (card_type == Constants_MJ_GZCG.HU_CARD_TYPE_JIE_PAO) {
			chiHuRight.opr_or(Constants_MJ_GZCG.CHR_FANG_PAO);
		}

		if (this.maxFan[seat_index] == 0) { // 没胡牌
			if (card_type == Constants_MJ_GZCG.CHR_QIANG_GANG_HU && AnalyseCardUtil.analyse_feng_chi_by_cards_index(cards_index,
					_logic.switch_to_card_index(cur_card), magic_cards_index, magic_card_count)) {
				chiHuRight.opr_or(Constants_MJ_GZCG.CHR_QIANG_GANG_HU);
				if (fujing != GameConstants.INVALID_CARD) {
					this.jing[1] = fujing;
				}
				return GameConstants.WIK_CHI_HU;
			}
			chiHuRight.set_empty();
			if (fujing != GameConstants.INVALID_CARD) {
				this.jing[1] = fujing;
			}
			return GameConstants.WIK_NULL;
		}

		if (fujing != GameConstants.INVALID_CARD) {
			this.jing[1] = fujing;
		}
		cbChiHuKind = GameConstants.WIK_CHI_HU;

		return cbChiHuKind;
	}

	public void setMaxFan(int seat_index, int fan, ChiHuRight chiHuRight) {
		if (fan > maxFan[seat_index]) {
			maxFan[seat_index] = fan;
			maxChr[seat_index].copy(chiHuRight);
		}
	}

	/**
	 * 精钓检测 所有牌都能胡的就是精钓牌型
	 * 
	 * @param cards_index
	 * @param weaveItem
	 * @param cbWeaveCount
	 * @param cur_card
	 * @return
	 */
	public boolean check_jing_diao(int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int cur_card, int[] magic_cards_index,
			int magic_card_count) {
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}
		if (cur_card > 0) {
			cbCardIndexTemp[this._logic.switch_to_card_index(cur_card)]--;
		}
		int count = 0;
		for (int i = 0; i < GameConstants.MAX_ZI_FENG; i++) {
			// WalkerGeek 解决遗留手上有4张牌不算精钓
			if (cbCardIndexTemp[i] == 4) {
				count++;
				continue;
			}
			if (AnalyseCardUtil.analyse_feng_chi_by_cards_index(cbCardIndexTemp, i, magic_cards_index, magic_card_count)) {
				count++;
			}
		}
		if (count == GameConstants.MAX_ZI_FENG) {
			return true;
		}

		return false;
	}

	public boolean check_jing_diao_qi_xiao_dui(int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int cur_card) {
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}
		if (cur_card > 0) {
			cbCardIndexTemp[this._logic.switch_to_card_index(cur_card)]--;
		}
		int count = 0;
		for (int i = 0; i < GameConstants.MAX_ZI_FENG; i++) {
			if (is_qi_dui(cbCardIndexTemp, weaveItem, cbWeaveCount, _logic.switch_to_card_data(i))) {
				count++;
			}
		}
		if (count == GameConstants.MAX_ZI_FENG) {
			return true;
		}

		return false;
	}

	public boolean check_hu(int cards_index[], int[] magic_cards_index, int magic_card_count) {
		int cardDatas[] = new int[GameConstants.MAX_COUNT];
		int cardCount = this._logic.switch_to_cards_data(cards_index, cardDatas);

		for (int i = 0; i < cardCount; i++) {
			if (cardDatas[i] == jing[0] || cardDatas[i] == jing[1]) {
				continue;
			}
			int cardIndex = this._logic.switch_to_card_index(cardDatas[i]);
			cards_index[cardIndex]++;
			boolean hu = AnalyseCardUtil.analyse_feng_chi_by_cards_index(cards_index, _logic.switch_to_card_index(cardDatas[i]), magic_cards_index,
					magic_card_count);
			cards_index[cardIndex]--;
			if (hu) {
				return true;
			}
		}

		return false;
	}

	public boolean is_qi_dui(int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int cur_card) {
		if (cbWeaveCount != 0) {
			return false;
		}
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		int cbCurrentIndex = _logic.switch_to_card_index(cur_card);
		if (cur_card > 0) {
			cbCardIndexTemp[cbCurrentIndex]++;
		}

		int jingCount = 0, danCount = 0, duiCount = 0;

		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			int cbCardCount = cbCardIndexTemp[i];
			int card = this._logic.switch_to_card_data(i);

			if (card == jing[0] || jing[1] == card) {
				jingCount += cbCardCount;
			} else {
				danCount += cbCardCount % 2;
			}
			duiCount += cbCardCount / 2;
		}

		if (duiCount < 7) {
			if (jingCount < danCount) {
				return false;
			}
		}
		if (jingCount > 0) {
			return true;
		}

		return false;
	}

	public int is_qi_xiao_dui(int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int cur_card, ChiHuRight chiHuRight, boolean isZimo,
			boolean isJingDiao) {
		chiHuRight.set_empty();
		if (cbWeaveCount != 0) {
			return 0;
		}
		isJingDiao = check_jing_diao_qi_xiao_dui(cards_index, weaveItem, cbWeaveCount, cur_card);
		if (isJingDiao && !isZimo) { // 精钓不能炮胡
			return 0;
		}
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		int jingCount = 0, danCount = 0, duiCount = 0;

		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			int cbCardCount = cbCardIndexTemp[i];
			int card = this._logic.switch_to_card_data(i);

			if (card == jing[0] || jing[1] == card) {
				jingCount += cbCardCount;
			} else {
				danCount += cbCardCount % 2;
			}
			duiCount += cbCardCount / 2;
		}

		if (duiCount < 7) {
			if (jingCount < danCount) {
				return 0;
			}
		}
		int fan = Constants_MJ_GZCG.FAN_XIAO_QI_DUI;
		chiHuRight.opr_or(Constants_MJ_GZCG.CHR_XIAO_QI_DUI);
		if (duiCount == 7) { // 德国七对
			chiHuRight.opr_or(Constants_MJ_GZCG.CHR_DE_GUO);
			fan *= Constants_MJ_GZCG.FAN_DE_GUO;
		}
		if (isJingDiao) {
			chiHuRight.opr_or(Constants_MJ_GZCG.CHR_JING_DIAO);
			fan *= Constants_MJ_GZCG.FAN_JING_DIAO;
		}

		if (!isZimo && ((cur_card & 0xFF) == this.jing[0] || (cur_card & 0xFF) == this.jing[1])
				&& chiHuRight.opr_and(Constants_MJ_GZCG.CHR_DE_GUO).is_empty()) {
			return 0; // 大七对的时候不能炮胡精牌本身 除非德国
		}
		if (!isZimo && isJingDiao) { // 小七对精钓不能炮胡
			return 0;
		}

		return fan;
	}

	public int is_ping_hu(int[] cards_index, int cur_card, ChiHuRight chiHuRight, int[] magic_cards_index, int magic_card_count, boolean isZimo,
			boolean isJingDiao, int card_type) {
		chiHuRight.set_empty();
		boolean can_win_with_out_magic = AnalyseCardUtil.analyse_feng_chi_by_cards_index(cards_index, _logic.switch_to_card_index(cur_card),
				magic_cards_index, 0);
		if (!isZimo) {
			chiHuRight.opr_or(Constants_MJ_GZCG.CHR_FANG_PAO);
		}
		int fan = 1;
		if (!isZimo && isJingDiao) {
			return 0;
		}
		// WalkerGeek 平胡接炮炮只能得德国
		if (has_rule(Constants_MJ_GZCG.GAME_RULE_PING_HU) && card_type == Constants_MJ_GZCG.HU_CARD_TYPE_JIE_PAO && !can_win_with_out_magic) {
			return 0;
		}

		if (isJingDiao) {
			fan *= Constants_MJ_GZCG.FAN_JING_DIAO;
			chiHuRight.opr_or(Constants_MJ_GZCG.CHR_JING_DIAO);
		}
		if (!can_win_with_out_magic) {

			int temp = cur_card & 0x000FF;
			if (temp == jing[0] || temp == jing[1]) {
				// WalkerGeek 平胡杠开和抢杠胡不受精必掉限制
				if (card_type != Constants_MJ_GZCG.CHR_QIANG_GANG_HU && card_type != Constants_MJ_GZCG.CHR_GANG_SHANG_HUA) {
					if (!isJingDiao && has_rule(Constants_MJ_GZCG.GAME_RULE_BI_DIAO)) {
						return 0;
					}
				}
				if (!isZimo) {
					return 0;
				}
			}

			if (AnalyseCardUtil.analyse_feng_chi_by_cards_index(cards_index, _logic.switch_to_card_index(cur_card), magic_cards_index,
					magic_card_count)) {
				chiHuRight.opr_or(Constants_MJ_GZCG.CHR_PING_HU);
				return fan * Constants_MJ_GZCG.FAN_PING_HU;
			}
		} else {
			chiHuRight.opr_or(Constants_MJ_GZCG.CHR_DE_GUO);
			chiHuRight.opr_or(Constants_MJ_GZCG.CHR_PING_HU);
			return fan * Constants_MJ_GZCG.FAN_DE_GUO * Constants_MJ_GZCG.FAN_PING_HU;
		}

		return 0;
	}

	public int is_da_qi_dui(int cards_index[], WeaveItem weaveItems[], int cbWeaveCount, int cur_card, ChiHuRight chiHuRight, int[] magic_cards_index,
			int magic_card_count, boolean isZimo, boolean isJingDiao) {
		chiHuRight.set_empty();

		isJingDiao = false;
		if (exist_eat(weaveItems, cbWeaveCount)) { // 有吃不可能是碰胡
			return 0;
		}
		int tempCardsIndex[] = Arrays.copyOf(cards_index, cards_index.length);
		if (cur_card > 0) {
			tempCardsIndex[this._logic.switch_to_card_index(cur_card)]--;
		}
		int jing = -1;
		if (cards_index[magic_cards_index[0]] > 0) {
			jing = 0;
		} else if (cards_index[magic_cards_index[1]] > 0) {
			jing = 1;
		}
		if (jing >= 0) { // 有精则判断是否精必钓
			int count = 0;
			for (int i = 0; i < GameConstants.MAX_ZI_FENG; i++) {
				if (AnalyseCardUtil.analyse_peng_hu_by_cards_index(tempCardsIndex, i, magic_cards_index, magic_card_count)) {
					count++;
				}
			}
			if (count == GameConstants.MAX_ZI_FENG) {
				isJingDiao = true;
			}
		}
		if (isJingDiao && !isZimo) { // 精钓不能炮胡(自摸、天胡、地胡可以胡)
			return 0;
		}
		int fan = 1;
		boolean pengHu = AnalyseCardUtil.analyse_peng_hu_by_cards_index(tempCardsIndex, _logic.switch_to_card_index(cur_card), magic_cards_index, 0); // 德国大七对
		if (!pengHu) {
			if (!isZimo && ((cur_card & 0xFF) == this.jing[0] || (cur_card & 0xFF) == this.jing[1])) {
				return 0; // 大七对的时候不能炮胡精牌本身 除非德国
			}
			if (AnalyseCardUtil.analyse_peng_hu_by_cards_index(tempCardsIndex, _logic.switch_to_card_index(cur_card), magic_cards_index,
					magic_card_count)) {
				chiHuRight.opr_or(Constants_MJ_GZCG.CHR_PENG_HU);
				fan = Constants_MJ_GZCG.FAN_DA_QI_DUI; // 大七对
			}
		} else {
			chiHuRight.opr_or(Constants_MJ_GZCG.CHR_PENG_HU);
			chiHuRight.opr_or(Constants_MJ_GZCG.CHR_DE_GUO);
			fan = Constants_MJ_GZCG.FAN_DA_QI_DUI * Constants_MJ_GZCG.FAN_DE_GUO; // 大七对*德国
		}
		if (isJingDiao) {
			chiHuRight.opr_or(Constants_MJ_GZCG.CHR_JING_DIAO);
			fan *= Constants_MJ_GZCG.FAN_JING_DIAO;
		}

		return fan == 1 ? 0 : fan;
	}

	/**
	 * 精钓时候只能炮胡精牌本身
	 * 
	 * @param isZimo
	 * @param isJingDiao
	 * @param cards_index
	 * @param cur_card
	 * @return
	 */
	public boolean jingDiaoCanPaoHu(boolean isZimo, boolean isJingDiao, int cards_index[], int cur_card) {
		if (!isZimo && isJingDiao) {
			int temp = cur_card & 0x000FF;
			if ((temp == this.jing[0] || temp == this.jing[1])) {
				if ((cards_index[this._logic.switch_to_card_index(temp)] > 0)) {
					return true;
				}
			}
			return false;
		}
		return true;
	}

	public int is_shi_san_lan(int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int cur_card, ChiHuRight chiHuRight, boolean isZimo) {
		chiHuRight.set_empty();
		if (cbWeaveCount > 0) {
			return 0;
		}

		int cardCount = 0, cardCountWithJing = 0;
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX]; // 所有不带精牌的卡牌索引
		int cbCardIndexTempWithJing[] = new int[GameConstants.MAX_INDEX]; // 所有不带精牌的卡牌索引
		int cbIndexs[] = new int[GameConstants.MAX_COUNT]; // 所有带精牌的卡牌索引
		int cbReIndexs[] = new int[GameConstants.MAX_COUNT];
		boolean isDeGuo = true;

		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			if (cards_index[i] > 0) {
				int card = this._logic.switch_to_card_data(i);
				if (card == jing[0] || jing[1] == card) {
					if (cards_index[i] > 1) { // 精牌有两张以上相同的则肯定不是德国
						isDeGuo = false;
					}
				} else if (cards_index[i] > 1) {
					// 非精牌相同值的牌数量大于1 则肯定不是十三烂
					return 0;
				} else {
					cbCardIndexTemp[i] = cards_index[i];
					cbReIndexs[cardCount++] = card;
				}
				cbIndexs[cardCountWithJing++] = card;
				cbCardIndexTempWithJing[i] = cards_index[i];
			}
		}

		int preCard = cbReIndexs[0]; // 判断非精牌两个卡牌之间的间隔
		for (int i = 1; i < cardCount; i++) {
			if (this._logic.get_card_color(cbReIndexs[i]) == 3) { // 算到风牌的时候停止
				break;
			} else if (cbReIndexs[i] - preCard > 2) {
				preCard = cbReIndexs[i];
			} else {
				return 0;
			}
		}

		int fan = 1;
		if (isDeGuo) {
			// 有精牌的单牌数小于14 则肯定不是德国牌
			if (cardCountWithJing < GameConstants.MAX_COUNT) {
				isDeGuo = false;
			} else {
				preCard = cbIndexs[0];
				for (int i = 1; i < cardCountWithJing; i++) {
					if (this._logic.get_card_color(cbIndexs[i]) == 3) { // 算到风牌的时候停止
						break;
					} else if (cbIndexs[i] - preCard > 2) {
						preCard = cbIndexs[i];
					} else {
						isDeGuo = false;
						break;
					}
				}
			}
			if (isDeGuo) {
				chiHuRight.opr_or(Constants_MJ_GZCG.CHR_DE_GUO);
				fan *= Constants_MJ_GZCG.FAN_DE_GUO;
			}
		}

		boolean isQiXing = false;
		int begin = this._logic.switch_to_card_index(0x31);
		int end = this._logic.switch_to_card_index(0x37);
		int count = 0;
		for (int i = begin; i <= end; i++) {
			if (cbCardIndexTempWithJing[i] > 0) {
				count++;
			}
		}
		if (count >= 7) {
			isQiXing = true;
		}

		if (isQiXing) {
			chiHuRight.opr_or(Constants_MJ_GZCG.CHR_QI_XING_SHI_SAN_LAN);
			fan *= Constants_MJ_GZCG.FAN_QI_XING_SHI_SAN_LAN;
		} else {
			chiHuRight.opr_or(Constants_MJ_GZCG.CHR_SHI_SAN_LAN);
			fan *= Constants_MJ_GZCG.FAN_SHI_SAN_LAN;
		}

		if (!isZimo && ((cur_card & 0xFF) == this.jing[0] || (cur_card & 0xFF) == this.jing[1])) {
			int color = this._logic.get_card_color(cur_card);
			if (color < 3) {
				int value = this._logic.get_card_value(cur_card);
				int beginValue = color * 16 + (value > 2 ? value - 2 : 1);
				int endValue = color * 16 + (value < 8 ? value + 2 : 9);
				for (int i = beginValue; i <= endValue; i++) {
					if (i == this.jing[1] || i == this.jing[0]) {
						continue;
					}
					if (cards_index[this._logic.switch_to_card_index(i)] > 0) {
						return 0;
					}
				}
			}
		}

		return fan == 1 ? 0 : fan;
	}

	@Override
	public void process_chi_hu_player_score(int seat_index, int provide_index, int operate_card, boolean zimo) {
		GRR._chi_hu_card[seat_index][0] = operate_card;
		GRR._win_order[seat_index] = 1;

		ChiHuRight chr = GRR._chi_hu_rights[seat_index];
		int di_fen = this.get_di_fen();

		countCardType(chr, seat_index);

		if (zimo) {
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				if (i == seat_index) {
					continue;
				}
				GRR._lost_fan_shu[i][seat_index] = di_fen;
			}
		} else {
			GRR._lost_fan_shu[provide_index][seat_index] = di_fen;
		}

		if (zimo) {
			int tmpScore = 2;
			int score = tmpScore * di_fen;
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				if (i == seat_index) {
					continue;
				}
				GRR._game_score[i] -= score;
				GRR._game_score[seat_index] += score;
			}
		} else {
			int tmpScore = 2;
			int score = tmpScore * di_fen;
			GRR._game_score[provide_index] -= score;
			GRR._game_score[seat_index] += score;
		}

		GRR._provider[seat_index] = provide_index;

		change_player_status(seat_index, GameConstants.INVALID_VALUE);

		return;
	}

	@Override
	protected void set_result_describe() {

	}

	protected void set_result_describe(int seatIndex) {
		if (seatIndex < 0) {
			return;
		}
		int i = seatIndex;
		chr[i] = this.GRR._chi_hu_rights[i];
		StringBuffer des = new StringBuffer();

		if (!(chr[i].opr_and(Constants_MJ_GZCG.CHR_GANG_SHANG_HUA).is_empty())) {
			des.append(",杠上开花");
		}
		if (!(chr[i].opr_and(Constants_MJ_GZCG.CHR_FANG_PAO).is_empty())) {
			des.append(",点炮");
		}
		if (!(chr[i].opr_and(Constants_MJ_GZCG.CHR_TIAN_HU).is_empty())) {
			des.append(",天胡");
		}
		if (!(chr[i].opr_and(Constants_MJ_GZCG.CHR_DI_HU).is_empty())) {
			des.append(",地胡");
		}
		if (!(chr[i].opr_and(Constants_MJ_GZCG.CHR_QIANG_GANG_HU).is_empty())) {
			des.append(",抢杠胡");
		} else if (!(chr[i].opr_and(Constants_MJ_GZCG.CHR_ZI_MO).is_empty())) {
			des.append(",自摸");
		}
		if (!(chr[i].opr_and(Constants_MJ_GZCG.CHR_JING_DIAO).is_empty())) {
			des.append(",精钓");
		}
		if (!(chr[i].opr_and(Constants_MJ_GZCG.CHR_DE_GUO).is_empty())) {
			des.append(",德国");
		}
		if (!(chr[i].opr_and(Constants_MJ_GZCG.CHR_QI_XING_SHI_SAN_LAN).is_empty())) {
			des.append(",七星十三烂");
		} else if (!(chr[i].opr_and(Constants_MJ_GZCG.CHR_SHI_SAN_LAN).is_empty())) {
			des.append(",基本十三烂");
		}
		if (!(chr[i].opr_and(Constants_MJ_GZCG.CHR_XIAO_QI_DUI).is_empty())) {
			des.append(",小七对");
		}
		if (!(chr[i].opr_and(Constants_MJ_GZCG.CHR_PENG_HU).is_empty())) {
			des.append(",大七对");
		}
		if (!(chr[i].opr_and(Constants_MJ_GZCG.CHR_PING_HU).is_empty())) {
			des.append(",平胡");
		}

		Arrays.fill(GRR._result_des, Strings.isNullOrEmpty(des.toString()) ? "" : des.substring(1, des.length()));
		// GRR._result_des[i] = Strings.isNullOrEmpty(des.toString()) ? "" :
		// des.substring(1, des.length());
	}

	@Override
	public boolean trustee_timer(int operate_id, int seat_index) {
		return false;
	}

	/**
	 * 其他玩家对当前出牌信息的响应
	 * 
	 * @param seat_index
	 * @param card
	 * @param type
	 * @return
	 */
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
			playerStatus = _playerStatus[i];

			if (!this.isLast) {
				if (i == get_banker_next_seat(seat_index) && has_rule(Constants_MJ_GZCG.GAME_RULE_KE_CHI)) {
					action = _logic.check_chi_gzcg(GRR._cards_index[i], card);
					int number = 0;
					int cardChanged = card;
					int cardValue = _logic.get_card_value(card);
					if (3 == _logic.get_card_color(card)) {
						if (card == jing[0] || jing[1] == card) {
							cardChanged |= 0x100;
						}
						if (cardValue > 4) {
							switch (cardValue) {
							case 5:
								cardChanged |= 0x67000;
								break;
							case 6:
								cardChanged |= 0x57000;
								break;
							case 7:
								cardChanged |= 0x56000;
								break;
							}
						} else {
							int begin = _logic.switch_to_card_index(0x31);
							int c = _logic.switch_to_card_index(card);
							for (int k = begin; k < begin + 4; k++) {
								if (k == c) {
									continue;
								}
								if (GRR._cards_index[i][k] > 0) {
									number++;
								}
							}
							if (number == 2) {
								int value = 16 * 16 * 16;
								for (int k = begin; k < begin + 4; k++) {
									if (k == c) {
										continue;
									}
									if (GRR._cards_index[i][k] > 0) {
										if (number == 1) {
											value *= 16;
										}
										cardChanged += value * _logic.get_card_value(_logic.switch_to_card_data(k));
										number--;
									}
								}
							}
						}
					}
					if (number == 3) {
						switch (cardValue) {
						case 1: // 东
							_playerStatus[i].add_action(GameConstants.WIK_RIGHT);
							_playerStatus[i].add_chi_gzcg(cardChanged, GameConstants.WIK_RIGHT, seat_index);
							_playerStatus[i].add_action(GameConstants.WIK_RIGHT);
							_playerStatus[i].add_chi_gzcg(cardChanged, GameConstants.WIK_RIGHT, seat_index);
							_playerStatus[i].add_action(GameConstants.WIK_RIGHT);
							_playerStatus[i].add_chi_gzcg(cardChanged, GameConstants.WIK_RIGHT, seat_index);
							break;
						case 2: // 南
							_playerStatus[i].add_action(GameConstants.WIK_RIGHT);
							_playerStatus[i].add_chi_gzcg(cardChanged, GameConstants.WIK_RIGHT, seat_index);
							_playerStatus[i].add_action(GameConstants.WIK_CENTER);
							_playerStatus[i].add_chi_gzcg(cardChanged, GameConstants.WIK_CENTER, seat_index);
							_playerStatus[i].add_action(GameConstants.WIK_CENTER);
							_playerStatus[i].add_chi_gzcg(cardChanged, GameConstants.WIK_CENTER, seat_index);
							break;
						case 3: // 西
							_playerStatus[i].add_action(GameConstants.WIK_LEFT);
							_playerStatus[i].add_chi_gzcg(cardChanged, GameConstants.WIK_LEFT, seat_index);
							_playerStatus[i].add_action(GameConstants.WIK_CENTER);
							_playerStatus[i].add_chi_gzcg(cardChanged, GameConstants.WIK_CENTER, seat_index);
							_playerStatus[i].add_action(GameConstants.WIK_CENTER);
							_playerStatus[i].add_chi_gzcg(cardChanged, GameConstants.WIK_CENTER, seat_index);
							break;
						case 4: // 北
							_playerStatus[i].add_action(GameConstants.WIK_LEFT);
							_playerStatus[i].add_chi_gzcg(cardChanged, GameConstants.WIK_LEFT, seat_index);
							_playerStatus[i].add_action(GameConstants.WIK_LEFT);
							_playerStatus[i].add_chi_gzcg(cardChanged, GameConstants.WIK_LEFT, seat_index);
							_playerStatus[i].add_action(GameConstants.WIK_LEFT);
							_playerStatus[i].add_chi_gzcg(cardChanged, GameConstants.WIK_LEFT, seat_index);
							break;
						}
					} else {
						if ((action & GameConstants.WIK_LEFT) != 0) {
							_playerStatus[i].add_action(GameConstants.WIK_LEFT);
							_playerStatus[i].add_chi(cardChanged, GameConstants.WIK_LEFT, seat_index);
						}
						if ((action & GameConstants.WIK_CENTER) != 0) {
							_playerStatus[i].add_action(GameConstants.WIK_CENTER);
							_playerStatus[i].add_chi(cardChanged, GameConstants.WIK_CENTER, seat_index);
						}
						if ((action & GameConstants.WIK_RIGHT) != 0) {
							_playerStatus[i].add_action(GameConstants.WIK_RIGHT);
							_playerStatus[i].add_chi(cardChanged, GameConstants.WIK_RIGHT, seat_index);
						}
					}

					if (_playerStatus[i].has_action()) {
						bAroseAction = true;
					}
				}

				action = _logic.check_peng(GRR._cards_index[i], card);
				if (action != 0) {
					playerStatus.add_action(action);
					playerStatus.add_peng(card, seat_index);
					bAroseAction = true;
				}

				if (GRR._left_card_count > Constants_MJ_GZCG.END) {
					action = _logic.estimate_gang_card_out_card(GRR._cards_index[i], card);
					if (action != 0) {
						playerStatus.add_action(GameConstants.WIK_GANG);
						playerStatus.add_gang(card, seat_index, 1); // 加上杠
						bAroseAction = true;
					}
				}
			}
			boolean isDiHu = false;
			int count1 = 0;
			for (int j = 0; j < this.getTablePlayerNumber(); j++) {
				if (this.fisrtOut[j] != 0) {
					count1++;
				}
			}
			if (this.GRR._left_card_count == this.beginCardCount && seat_index == this._cur_banker && count1 == 1) {
				isDiHu = true;
			}
			if (_playerStatus[i].is_chi_hu_round() && (_playerStatus[i]._hu_card_count > 0 || isDiHu)) {
				ChiHuRight chr = GRR._chi_hu_rights[i];
				chr.set_empty();
				int cbWeaveCount = GRR._weave_count[i];

				int card_type = Constants_MJ_GZCG.HU_CARD_TYPE_JIE_PAO;
				int count = 0;
				for (int j = 0; j < this.getTablePlayerNumber(); j++) {
					if (this.fisrtOut[j] != 0) {
						count++;
					}
				}
				if (this.GRR._left_card_count == this.beginCardCount && seat_index == this._cur_banker && count == 1) {
					card_type = Constants_MJ_GZCG.CHR_DI_HU;
				}

				action = analyse_chi_hu_card_new(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr, card_type, i, seat_index);
				if (action != 0 && this.maxFan[i] > this.canNotHu[i]) {
					// 接炮时，牌型分有变动，才能接炮胡
					_playerStatus[i].add_action(GameConstants.WIK_CHI_HU);
					_playerStatus[i].add_chi_hu(card, seat_index);
					bAroseAction = true;
				} else {
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
	public void process_chi_hu_player_operate(int seat_index, int operate_card, boolean rm) {
		// 引用权位
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];

		long effect[] = new long[1];
		if (!(chr.opr_and(Constants_MJ_GZCG.CHR_GANG_SHANG_HUA).is_empty())) { // 杠上开花
			effect[0] = Constants_MJ_GZCG.CHR_GANG_SHANG_HUA;
		} else {
			effect[0] = Constants_MJ_GZCG.CHR_ZI_MO;
		}
		this.operate_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_HU, 1, effect, 1, GameConstants.INVALID_SEAT);

		// 手牌删掉
		this.operate_player_cards(seat_index, 0, null, 0, null);

		if (rm) {
			// 把摸的牌从手牌删掉,结算的时候不显示这张牌的
			GRR._cards_index[seat_index][_logic.switch_to_card_index(operate_card)]--;
		}

		// 显示胡牌
		// int cards[] = new int[GameConstants.MAX_COUNT];
		// int hand_card_count =
		// _logic.switch_to_cards_data_gzcg(GRR._cards_index[seat_index],
		// cards);
		// cards[hand_card_count] = operate_card +
		// GameConstants.CARD_ESPECIAL_TYPE_HU;
		// changeCard(cards, hand_card_count);
		// hand_card_count++;
		// this.operate_show_card(seat_index, GameConstants.Show_Card_HU,
		// hand_card_count, cards, GameConstants.INVALID_SEAT);

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			int cards[] = new int[GameConstants.MAX_COUNT];
			int hand_card_count = _logic.switch_to_cards_data_gzcg(GRR._cards_index[i], cards);
			operate_player_cards(i, 0, null, 0, null);

			operate_player_cards(i, 0, new int[] {}, GRR._weave_count[i], GRR._weave_items[i]);

			// 显示胡牌
			int[] temp_cards_index = Arrays.copyOf(GRR._cards_index[i], GRR._cards_index[i].length);
			cards = new int[GameConstants.MAX_COUNT];
			hand_card_count = _logic.switch_to_cards_data_gzcg(temp_cards_index, cards);
			if (i == seat_index) {
				cards[hand_card_count] = operate_card + GameConstants.CARD_ESPECIAL_TYPE_HU;
				hand_card_count++;
			}
			changeCard(cards, hand_card_count);

			operate_show_card(i, GameConstants.Show_Card_HU, hand_card_count, cards, GameConstants.INVALID_SEAT);
		}

		return;
	}

	protected int get_di_fen() {
		int score = 0;
		if (has_rule(Constants_MJ_GZCG.GAME_RULE_BASE_SCORE_ONE)) {
			score = 1;
		} else if (has_rule(Constants_MJ_GZCG.GAME_RULE_BASE_SCORE_TWO)) {
			score = 2;
		} else if (has_rule(Constants_MJ_GZCG.GAME_RULE_BASE_SCORE_FOUR)) {
			score = 4;
		}
		return score;
	}

	protected int getGangScore() {
		return 2;
	}

	@Override
	protected int get_banker_pre_seat(int banker_seat) {
		int count = 0;
		int seat = banker_seat;
		do {
			count++;
			seat = (4 + seat - 1) % 4;
		} while (get_players()[seat] == null && count <= 5);
		return seat;
	}

	@Override
	protected int get_banker_next_seat(int banker_seat) {
		int count = 0;
		int seat = banker_seat;
		do {
			count++;
			seat = (seat + 1) % this.getTablePlayerNumber();
		} while (get_players()[seat] == null && count <= 5);
		return seat;
	}

	@Override
	public int get_real_card(int card) {
		if (card > GameConstants.CARD_ESPECIAL_TYPE_TING && card != GameConstants.CARD_ESPECIAL_TYPE_CAN_NOT_OUT) {
			card -= GameConstants.CARD_ESPECIAL_TYPE_TING;
		} else if (card > GameConstants.CARD_ESPECIAL_TYPE_CAN_NOT_OUT) {
			card -= GameConstants.CARD_ESPECIAL_TYPE_CAN_NOT_OUT;
		}
		return card;
	}

	protected int get_seat(int nValue, int seat_index) {
		return (seat_index + (nValue - 1) % 4) % 4;
	}

	public int get_ting_card(int[] cards, int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int seat_index) {
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		ChiHuRight chr = new ChiHuRight();

		int count = 0;
		int max_ting_count = GameConstants.MAX_ZI_FENG;
		for (int i = 0; i < max_ting_count; i++) {
			int cbCurrentCard = _logic.switch_to_card_data(i);
			chr.set_empty();
			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card_new(cbCardIndexTemp, weaveItem, cbWeaveCount, cbCurrentCard, chr,
					Constants_MJ_GZCG.HU_CARD_TYPE_ZI_MO, seat_index, (seat_index + 1) % this.getTablePlayerNumber())) {
				cards[count++] = cbCurrentCard;
			}
		}

		// 代理要求: 只显示能炮胡的牌. 如果精钓就要显示全听
		int[] magic_cards_index = new int[GameUtilConstants.MAX_MAGIC_INDEX_COUNT];
		int magic_card_count = this._logic.get_magic_card_count();
		if (magic_card_count > 2) {
			magic_card_count = 2;
		}
		for (int i = 0; i < magic_card_count; i++) {
			magic_cards_index[i] = _logic.get_magic_card_index(i);
		}

		if (count > 27 || check_jing_diao(cbCardIndexTemp, weaveItem, cbWeaveCount, 0, magic_cards_index, magic_card_count)
				|| check_jing_diao_qi_xiao_dui(cbCardIndexTemp, weaveItem, cbWeaveCount, 0)) {
			count = 1;
			cards[0] = -1;
		}

		return count;
	}

	public int getCardType(int seatIndex, int card, int[] cardIndex) {
		return 0;
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
	public boolean operate_player_cards(int seat_index, int card_count, int cards[], int weave_count, WeaveItem weaveitems[]) {
		this.changeCard(cards, card_count);

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
				weaveItem_item.setProvidePlayer(weaveitems[j].provide_player >= 1000 ? weaveitems[j].provide_player
						: weaveitems[j].provide_player + GameConstants.WEAVE_SHOW_DIRECT);
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

	public boolean isJing(int card) {
		card = card & 0x000FF;
		if (this.jing[0] == card || card == this.jing[1]) {
			return true;
		}
		return false;
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
			if (seat_index == i) {
				continue;
			}

			playerStatus = _playerStatus[i];

			if (playerStatus.isAbandoned()) {
				continue;
			}

			ChiHuRight chr = GRR._chi_hu_rights[i];// playerStatus._chiHuRight;
			chr.set_empty();
			int cbWeaveCount = GRR._weave_count[i];
			action = analyse_chi_hu_card_new(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr, Constants_MJ_GZCG.CHR_QIANG_GANG_HU,
					i, seat_index);
			// 结果判断
			if (action != 0) {
				_playerStatus[i].add_action(GameConstants.WIK_CHI_HU);
				_playerStatus[i].add_chi_hu(card, seat_index); // 吃胡的组合
				chr.opr_or(Constants_MJ_GZCG.CHR_QIANG_GANG_HU); // 抢杠胡
				bAroseAction = true;
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
	protected void test_cards() {

		int cards[] = new int[] { 0x02, 0x03, 0x07, 0x07, 0x08, 0x14, 0x15, 0x15, 0x16, 0x23, 0x23, 0x23, 0x35 };
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			for (int j = 0; j < GameConstants.MAX_INDEX; j++) {
				GRR._cards_index[i][j] = 0;
			}
		}
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			for (int j = 0; j < cards.length; j++) {
				GRR._cards_index[i][_logic.switch_to_card_index(cards[j])] += 1;
			}
		}
		// for (int j = 0; j < cards.length; j++) {
		// GRR._cards_index[1][_logic.switch_to_card_index(j % 9 + 1)] += 1;
		// }

		// testRealyCard(realyCards);

		/********
		 * 下面这个给测试用的 如果开发用 把上面的注释去掉 realyCards
		 **************************/

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

	private int getFive(int card) {
		return (card & 0xF0000) >> 16;
	}

	private int getFour(int card) {
		return (card & 0xF000) >> 12;
	}

	/**
	 * 取消精牌特殊值
	 * 
	 * @param card
	 * @return
	 */
	public int getRealCard(int card) {
		if (card > 256 && card < 500) {
			return card & 0xFF;
		} else {
			return card;
		}
	}

}
