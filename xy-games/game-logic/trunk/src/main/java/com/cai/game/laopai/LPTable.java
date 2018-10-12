package com.cai.game.laopai;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.RedisConstant;
import com.cai.common.define.ECardType;
import com.cai.common.define.EMoneyOperateType;
import com.cai.common.domain.AddMoneyResultModel;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.Player;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.SysParamModel;
import com.cai.common.domain.WeaveItem;
import com.cai.common.rmi.ICenterRMIServer;
import com.cai.common.util.GameDescUtil;
import com.cai.common.util.RandomUtil;
import com.cai.common.util.SpringService;
import com.cai.dictionary.SysParamDict;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.AddDiscardRunnable;
import com.cai.future.runnable.DispatchLastCardRunnable;
import com.cai.future.runnable.JianPaoHuRunnable;
import com.cai.future.runnable.KickRunnable;
import com.cai.future.runnable.TuoGuanRunnable;
import com.cai.future.runnable.XiaoHuRunnable;
import com.cai.game.laopai.LPGameLogic.AnalyseItem;
import com.cai.game.laopai.handler.LPHandlerFinish;
import com.cai.redis.service.RedisService;
import com.cai.service.MongoDBServiceImpl;
import com.cai.service.PlayerServiceImpl;

import protobuf.clazz.Protocol.GameEndResponse;
import protobuf.clazz.Protocol.GameStartResponse;
import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.PlayerResultResponse;
import protobuf.clazz.Protocol.RoomInfo;
import protobuf.clazz.Protocol.RoomPlayerResponse;
import protobuf.clazz.Protocol.RoomRequest;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

///////////////////////////////////////////////////////////////////////////////////////////////
public class LPTable extends AbstractLPTable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 7060061356475703643L;

	private static Logger logger = Logger.getLogger(LPTable.class);

	public LPTable() {

		super();

		_status_cs_gang = false;
		// 结束信息
	}

	@Override
	protected void onInitTable() {
		_handler_finish = new LPHandlerFinish();
	}

	@Override
	public void on_init_table(int game_type_index, int game_rule_index, int game_round) {
		super.on_init_table(game_type_index, game_rule_index, game_round);

		if (is_mj_type(GameConstants.GAME_TYPE_HZ) || is_mj_type(GameConstants.GAME_TYPE_HENAN_HZ) || is_mj_type(GameConstants.GAME_TYPE_FLS_HZ_LX)
				|| is_mj_type(GameConstants.GAME_TYPE_HALL_HONG_ZHONG_MJ))// 红中
		// 红中玩法is_mj_type(MJGameConstants.GAME_TYPE_HZ)
		{
			// 设置红中为癞子
			_logic.add_magic_card_index(_logic.switch_to_card_index(GameConstants.HZ_MAGIC_CARD));
		} else {
			// _logic.add_magic_card_index(MJGameConstants.MAX_INDEX);
		}
	}

	@Override
	public void runnable_set_trustee(int _seat_index) {
		// TODO Auto-generated method stub
		if (_trustee_schedule == null)
			return;

		if (_trustee_schedule[_seat_index] != null) {
			_trustee_schedule[_seat_index] = null;
		}

		if (_game_status == GameConstants.GS_MJ_FREE || _game_status == GameConstants.GS_MJ_WAIT)
			return;

		// TODO Auto-generated method stub
		handler_request_trustee(_seat_index, true, 0);

	}

	////////////////////////////////////////////////////////////////////////

	/**
	 * 第一轮 初始化庄家 默认第一个。需要的继承
	 */
	@Override
	protected void initBanker() {
		if (is_mj_type(GameConstants.GAME_TYPE_CS) || is_mj_type(GameConstants.GAME_TYPE_HALL_CHANG_SHA_MJ)) {
			this.shuffle_players();

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				this.get_players()[i].set_seat_index(i);
				if (this.get_players()[i].getAccount_id() == this.getRoom_owner_account_id()) {
					this._cur_banker = i;
				}
			}
		}

		if (is_mj_type(GameConstants.GAME_TYPE_HENAN) || is_mj_type(GameConstants.GAME_TYPE_HENAN_ZMD) || is_mj_type(GameConstants.GAME_TYPE_HENAN_KF)
				|| is_mj_type(GameConstants.GAME_TYPE_HENAN_NY) || is_mj_type(GameConstants.GAME_TYPE_HENAN_LYGC)
				|| is_mj_type(GameConstants.GAME_TYPE_HENAN_ZHUAN_ZHUAN) || is_mj_type(GameConstants.GAME_TYPE_HENAN_XX)
				|| is_mj_type(GameConstants.GAME_TYPE_HENAN_XY) || is_mj_type(GameConstants.GAME_TYPE_HENAN_ZHOU_KOU)
				|| is_mj_type(GameConstants.GAME_TYPE_HENAN_SMX) || is_mj_type(GameConstants.GAME_TYPE_HENAN_PDS)
				|| is_mj_type(GameConstants.GAME_TYPE_HALL_ZHUAN_ZHUAN_MJ) || is_mj_type(GameConstants.GAME_TYPE_HALL_HONG_ZHONG_MJ)) {

			if (has_rule(GameConstants.GAME_RULE_HENAN_THREE)) {
				int banker = RandomUtil.getRandomNumber(GameConstants.GAME_PLAYER - 1);
				this._cur_banker = banker;
			} else {
				int banker = RandomUtil.getRandomNumber(GameConstants.GAME_PLAYER);
				this._cur_banker = banker;
			}

		}
	}

	// 游戏开始
	@Override
	protected boolean on_handler_game_start() {
		if (_cur_round == 2) {
			// real_kou_dou();// 记录真实扣豆
		}

		reset_init_data();

		// 庄家选择
		this.progress_banker_select();

		_game_status = GameConstants.GS_MJ_PLAY;

		// 信阳麻将
		GRR._banker_player = _cur_banker;
		_current_player = GRR._banker_player;

		if (is_mj_type(GameConstants.GAME_TYPE_HZ) || is_mj_type(GameConstants.GAME_TYPE_FLS_HZ_LX)
				|| is_mj_type(GameConstants.GAME_TYPE_HALL_HONG_ZHONG_MJ))// 红中玩法
		{
			_repertory_card = new int[GameConstants.CARD_COUNT_HZ];
			shuffle(_repertory_card, GameConstants.CARD_DATA_HZ);
		} else if (is_mj_type(GameConstants.GAME_TYPE_HENAN_AY)) {
			// 安阳麻将
			if (has_rule(GameConstants.GAME_RULE_HENAN_DAIFENG)) {
				_repertory_card = new int[GameConstants.CARD_COUNT_DAI_FENG_AY];
				shuffle(_repertory_card, GameConstants.CARD_DATA_DAI_FENG_AY);
			} else {
				_repertory_card = new int[GameConstants.CARD_COUNT_BU_DAI_FENG_AY];
				shuffle(_repertory_card, GameConstants.CARD_DATA_BU_DAI_FENG_AY);
			}
		} else if (is_mj_type(GameConstants.GAME_TYPE_HENAN_XY)) {
			// 信阳麻将
			_repertory_card = new int[GameConstants.CARD_COUNT_XY];
			shuffle(_repertory_card, GameConstants.CARD_DATA_XY);
		} else if (is_mj_type(GameConstants.GAME_TYPE_HENAN_SMX)) {
			// 三门峡麻将
			if (has_rule(GameConstants.GAME_RULE_HENAN_DAIFENG)) {
				_repertory_card = new int[GameConstants.CARD_COUNT_DAI_FENG_SMX];
				shuffle(_repertory_card, GameConstants.CARD_DATA_DAI_FENG_SMX);
			} else {
				_repertory_card = new int[GameConstants.CARD_COUNT_BU_DAI_FENG_SMX];
				shuffle(_repertory_card, GameConstants.CARD_DATA_BU_DAI_FENG_SMX);
			}

		} else if (is_mj_type(GameConstants.GAME_TYPE_HENAN_XC)) {
			// xuchang麻将
			_repertory_card = new int[GameConstants.CARD_COUNT_DAI_FENG_LZ];
			shuffle(_repertory_card, GameConstants.CARD_DATA_DAI_FENG_LZ);
		} else if (is_mj_type(GameConstants.GAME_TYPE_HENAN_LZ)) {
			// 林州
			if (has_rule(GameConstants.GAME_RULE_HENAN_DAIFENG)) {
				_repertory_card = new int[GameConstants.CARD_COUNT_DAI_FENG_LZ];
				shuffle(_repertory_card, GameConstants.CARD_DATA_DAI_FENG_LZ);
			} else {
				_repertory_card = new int[GameConstants.CARD_COUNT_BU_DAI_FENG_LZ];
				shuffle(_repertory_card, GameConstants.CARD_DATA_BU_DAI_FENG_LZ);
			}
		} else if (is_mj_type(GameConstants.GAME_TYPE_HENAN) || is_mj_type(GameConstants.GAME_TYPE_HENAN_ZMD)
				|| is_mj_type(GameConstants.GAME_TYPE_HENAN_KF) || is_mj_type(GameConstants.GAME_TYPE_HENAN_NY)
				|| is_mj_type(GameConstants.GAME_TYPE_HENAN_XX) || is_mj_type(GameConstants.GAME_TYPE_HENAN_PDS)) {
			// 河南通用麻将
			if (has_rule(GameConstants.GAME_RULE_HENAN_DAIFENG)) {
				_repertory_card = new int[GameConstants.CARD_COUNT_DAI_FENG_LZ];
				shuffle(_repertory_card, GameConstants.CARD_DATA_DAI_FENG_LZ);
			} else {
				_repertory_card = new int[GameConstants.CARD_COUNT_BU_DAI_FENG_LZ];
				shuffle(_repertory_card, GameConstants.CARD_DATA_BU_DAI_FENG_LZ);
			}
		} else if (is_mj_type(GameConstants.GAME_TYPE_HENAN_HZ)) {
			// 河南红中麻将
			if (has_rule(GameConstants.GAME_RULE_HENAN_DAIFENG)) {
				_repertory_card = new int[GameConstants.CARD_COUNT_DAI_FENG_HNHZ];
				shuffle(_repertory_card, GameConstants.CARD_DATA_DAI_FENG_LZ);
			} else {
				_repertory_card = new int[GameConstants.CARD_COUNT_BU_DAI_FENG_HNHZ];
				shuffle(_repertory_card, GameConstants.CARD_DATA_HZ);
			}
		} else if (is_mj_type(GameConstants.GAME_TYPE_HENAN_LYGC)) {
			// 洛阳杠次初始化牌组 (使用林州牌组)
			if (has_rule(GameConstants.GAME_RULE_HENAN_DAIFENG)) {
				_repertory_card = new int[GameConstants.CARD_COUNT_DAI_FENG_LZ];
				shuffle(_repertory_card, GameConstants.CARD_DATA_DAI_FENG_LZ);
			} else {
				_repertory_card = new int[GameConstants.CARD_COUNT_BU_DAI_FENG_LZ];
				shuffle(_repertory_card, GameConstants.CARD_DATA_BU_DAI_FENG_LZ);
			}
		} else if (is_mj_type(GameConstants.GAME_TYPE_HENAN_ZHOU_KOU)) {
			if (has_rule(GameConstants.GAME_RULE_HENAN_DAIFENG)) { // 136张
				_repertory_card = new int[GameConstants.CARD_COUNT_HE_NAN_ZHOU_KOU_DAI_FENG];
				shuffle(_repertory_card, GameConstants.CARD_DATA_DAI_FENG);
			} else if (GameDescUtil.has_rule(gameRuleIndexEx, GameConstants.GAME_RULE_HENAN_HONG_ZHONG_LAI_ZI)) { // 112张
				_repertory_card = new int[GameConstants.CARD_COUNT_HE_NAN_ZHOU_KOU_HONG_ZHONG_LAI_ZI];
				shuffle(_repertory_card, GameConstants.CARD_DATA_HONG_ZHONG_LAI_ZI);
			} else { // 108张
				_repertory_card = new int[GameConstants.CARD_COUNT_HE_NAN_ZHOU_KOU];
				shuffle(_repertory_card, GameConstants.CARD_DATA_WAN_TIAO_TONG);
			}
		} else if (is_mj_type(GameConstants.GAME_TYPE_HENAN_LH)) {
			if (has_rule(GameConstants.GAME_RULE_HENAN_DAIFENG)) { // 136张
				_repertory_card = new int[GameConstants.CARD_COUNT_HE_NAN_ZHOU_KOU_DAI_FENG];
				shuffle(_repertory_card, GameConstants.CARD_DATA_DAI_FENG);
			} else if (GameDescUtil.has_rule(gameRuleIndexEx, GameConstants.GAME_RULE_HENAN_HONG_ZHONG_LAI_ZI)) { // 112张
				_repertory_card = new int[GameConstants.CARD_COUNT_HE_NAN_ZHOU_KOU_HONG_ZHONG_LAI_ZI];
				shuffle(_repertory_card, GameConstants.CARD_DATA_HONG_ZHONG_LAI_ZI);
			} else { // 108张
				_repertory_card = new int[GameConstants.CARD_COUNT_HE_NAN_ZHOU_KOU];
				shuffle(_repertory_card, GameConstants.CARD_DATA_WAN_TIAO_TONG);
			}
		} else {
			// 晃晃麻将也可以用湖南麻将的牌（推倒胡）
			_repertory_card = new int[GameConstants.CARD_COUNT_HU_NAN];
			shuffle(_repertory_card, GameConstants.CARD_DATA_HU_NAN);
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

		}
		// 游戏开始时 初始化 未托管
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			istrustee[i] = false;
		}

		on_game_start();
		return false;
	}

	@Override
	protected boolean on_game_start() {
		return game_start_zz();
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

		int count = has_rule(GameConstants.GAME_RULE_HUNAN_THREE) ? GameConstants.GAME_PLAYER - 1 : GameConstants.GAME_PLAYER;
		// 分发扑克
		for (int i = 0; i < count; i++) {
			// if(GRR._banker_player == i){
			// send_count = MJGameConstants.MAX_COUNT;
			// }else{
			//
			// send_count = (MJGameConstants.MAX_COUNT - 1);
			// }
			send_count = (GameConstants.MAX_COUNT - 1);
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

	@Override
	public void test_cards() {
		int[] cards_of_player0 = new int[] { 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x11, 0x12, 0x13, 0x14 };
		int[] cards_of_player1 = new int[] { 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x11, 0x12, 0x13, 0x14 };
		int[] cards_of_player2 = new int[] { 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x11, 0x12, 0x13, 0x14 };
		int[] cards_of_player3 = new int[] { 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x11, 0x12, 0x13, 0x14 };

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

	// 开始转转麻将
	private boolean game_start_zz() {
		GameStartResponse.Builder gameStartResponse = GameStartResponse.newBuilder();
		// gameStartResponse.setSiceIndex(rand);
		gameStartResponse.setBankerPlayer(GRR._banker_player);
		gameStartResponse.setCurrentPlayer(_current_player);
		gameStartResponse.setLeftCardCount(GRR._left_card_count);

		int hand_cards[][] = new int[getTablePlayerNumber()][GameConstants.MAX_COUNT];
		// 发送数据
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[i], hand_cards[i]);
			gameStartResponse.addCardsCount(hand_card_count);
		}

		// 发送数据
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

			// 只发自己的牌
			gameStartResponse.clearCardData();
			for (int j = 0; j < GameConstants.MAX_COUNT; j++) {
				gameStartResponse.addCardData(hand_cards[i][j]);
			}

			// 回放数据
			GRR._video_recode.addHandCards(cards);

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			this.load_room_info_data(roomResponse);
			this.load_common_status(roomResponse);
			roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
			roomResponse.setGameStart(gameStartResponse);
			roomResponse.setCurrentPlayer(_current_player == GameConstants.INVALID_SEAT ? _resume_player : _current_player);
			roomResponse.setLeftCardCount(GRR._left_card_count);
			roomResponse.setGameStatus(_game_status);
			roomResponse.setLeftCardCount(GRR._left_card_count);
			this.send_response_to_player(i, roomResponse);
		}
		////////////////////////////////////////////////// 回放
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
		this.load_room_info_data(roomResponse);
		this.load_common_status(roomResponse);
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

			for (int j = 0; j < GameConstants.MAX_COUNT; j++) {
				cards.addItem(hand_cards[i][j]);
			}
			gameStartResponse.addCardsData(cards);
		}

		roomResponse.setGameStart(gameStartResponse);
		roomResponse.setLeftCardCount(GRR._left_card_count);
		GRR.add_room_response(roomResponse);
		///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

		// _table_scheduled = GameSchedule.put(new
		// DispatchCardRunnable(this.getRoom_id(), _current_player, false),
		// MJGameConstants.SEND_CARD_DELAY, TimeUnit.MILLISECONDS);

		this.exe_dispatch_card(_current_player, GameConstants.WIK_NULL, GameConstants.DELAY_SEND_CARD_DELAY);

		return false;
	}

	// 摸起来的牌 派发牌
	public boolean dispatch_card_data(int cur_player, int type, boolean tail) {
		// 牌局未开 或者等待状态 调度不需要执行 add by zain 2017/6/1
		if ((_game_status == GameConstants.GS_MJ_FREE || _game_status == GameConstants.GS_MJ_WAIT) && is_sys())
			return false;
		// add end
		// 发牌
		this.set_handler(this._handler_dispath_card);
		this._handler_dispath_card.reset_status(cur_player, type);
		this._handler.exe(this);

		return true;
	}

	/**
	 * 处理牌桌结束逻辑
	 * 
	 * @param seat_index
	 * @param reason
	 */
	protected boolean on_handler_game_finish(int seat_index, int reason) {

		return super.on_handler_game_finish(seat_index, reason);
	}

	public boolean handler_game_finish_xp(int seat_index, int reason) {
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

		RoomInfo.Builder room_info = getRoomInfo();
		game_end.setRoomInfo(room_info);

		game_end.setRoundOverType(0);
		game_end.setRoomOverType(0);

		if (GRR != null) {// reason == MJGameConstants.Game_End_NORMAL || reason
							// == MJGameConstants.Game_End_DRAW ||
			// (reason ==MJGameConstants.Game_End_RELEASE_PLAY && GRR!=null)
			game_end.setRoundOverType(1);
			game_end.setStartTime(GRR._start_time);
			game_end.setGameTypeIndex(GRR._game_type_index);
			roomResponse.setLeftCardCount(GRR._left_card_count);

			// 特别显示的牌
			for (int i = 0; i < GRR._especial_card_count; i++) {
				game_end.addEspecialShowCards(GRR._especial_show_cards[i] + GameConstants.CARD_ESPECIAL_TYPE_DING_GUI);
			}

			GRR._end_type = reason;

			// 杠牌，每个人的分数
			float lGangScore[] = new float[GameConstants.GAME_PLAYER];
			for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {

				for (int j = 0; j < GRR._gang_score[i].gang_count; j++) {
					for (int k = 0; k < GameConstants.GAME_PLAYER; k++) {
						lGangScore[k] += GRR._gang_score[i].scores[j][k];// 杠牌，每个人的分数
						// _allGRR._gang_score[i].scores[j][k]+=
						// GRR._gang_score[i].scores[j][k];//杠牌，每个人的分数

						// allGangScore[k]+=_allGRR._gang_score[i].scores[j][k];
					}
				}

				// 记录
				for (int j = 0; j < GameConstants.GAME_PLAYER; j++) {
					_player_result.lost_fan_shu[i][j] += GRR._lost_fan_shu[i][j];
				}

			}

			for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
				GRR._game_score[i] += lGangScore[i];
				GRR._game_score[i] += GRR._start_hu_score[i];// 起手胡分数

				// 记录
				// _all_start_hu_score[i]+=_start_hu_score[i];//起手胡分数
				_player_result.game_score[i] += GRR._game_score[i];

			}

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
				for (int j = 0; j < GameConstants.MAX_COUNT; j++) {
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
				GRR._card_count[i] = _logic.switch_to_cards_data(GRR._cards_index[i], GRR._cards_data[i]);

				Int32ArrayResponse.Builder cs = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GRR._card_count[i]; j++) {
					// 癞子
					if (_logic.is_magic_card(GRR._cards_data[i][j])) {
						cs.addItem(GRR._cards_data[i][j] + GameConstants.CARD_ESPECIAL_TYPE_GUI);
					} else {
						cs.addItem(GRR._cards_data[i][j]);
					}
				}
				game_end.addCardsData(cs);// 牌

				// 组合
				WeaveItemResponseArrayResponse.Builder weaveItem_array = WeaveItemResponseArrayResponse.newBuilder();
				for (int j = 0; j < GameConstants.MAX_WEAVE; j++) {
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
				game_end.addGameScore(GRR._game_score[i]);// 放炮的人？
				game_end.addGangScore(lGangScore[i]);// 杠牌得分
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
		} else if (reason == GameConstants.Game_End_RELEASE_PLAY || reason == GameConstants.Game_End_RELEASE_NO_BEGIN
				|| reason == GameConstants.Game_End_RELEASE_RESULT || reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT || reason == GameConstants.Game_End_RELEASE_SYSTEM) {
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
		if (reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT || reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT) {
			for (int j = 0; j < GameConstants.GAME_PLAYER; j++) {
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

	public int analyse_chi_hu_card(int cards_index[], WeaveItem weaveItems[], int weave_count, int cur_card, ChiHuRight chiHuRight, int card_type,
			int _seat_index) {

		return analyse_chi_hu_card_xp(cards_index, weaveItems, weave_count, cur_card, chiHuRight, card_type);
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
	public int analyse_chi_hu_card_xp(int cards_index[], WeaveItem weaveItems[], int weaveCount, int cur_card, ChiHuRight chiHuRight, int card_type) {

		if ((has_rule(GameConstants.GAME_RULE_HUNAN_QIANGGANGHU) == false) && (card_type == GameConstants.HU_CARD_TYPE_PAOHU))// 是否选择了自摸胡
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

		// if(is_mj_type(MJGameConstants.GAME_TYPE_HZ)){
		// if((cards_index[_logic.switch_to_card_index(MJGameConstants.ZZ_MAGIC_CARD)]==4)||
		// ((cards_index[_logic.switch_to_card_index(MJGameConstants.ZZ_MAGIC_CARD)]==3)
		// && (cur_card == MJGameConstants.ZZ_MAGIC_CARD))){
		//
		// cbChiHuKind = MJGameConstants.WIK_CHI_HU;
		// if (card_type == MJGameConstants.HU_CARD_TYPE_ZIMO) {
		// chiHuRight.opr_or(MJGameConstants.CHR_ZI_MO);
		// }else{
		// //这个没必要。一定是自摸
		// chiHuRight.opr_or(MJGameConstants.CHR_SHU_FAN);
		// }
		// }
		//
		// }

		// 设置变量
		// chiHuRight.set_empty();
		// chiHuRight.opr_or(MJGameConstants.CHR_SHU_FAN);

		// 抢杠胡
		if (_current_player == GameConstants.INVALID_SEAT && _status_gang && (cbChiHuKind == GameConstants.WIK_CHI_HU)) {
			if (has_rule(GameConstants.GAME_RULE_HUNAN_QIANGGANGHU))// 是否选择了抢杠胡
			{
				cbChiHuKind = GameConstants.WIK_CHI_HU;
				chiHuRight.opr_or(GameConstants.CHR_HUNAN_QIANG_GANG_HU);
			} else {
				chiHuRight.set_empty();
				return GameConstants.WIK_NULL;
			}

		}

		if (chiHuRight.is_empty() == false) {

			return cbChiHuKind;
		}

		// 构造扑克
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		// 插入扑克
		if (cur_card != GameConstants.INVALID_VALUE) {
			cbCardIndexTemp[_logic.switch_to_card_index(cur_card)]++;
		}

		List<AnalyseItem> analyseItemArray = new ArrayList<AnalyseItem>();

		// 分析扑克
		boolean bValue = _logic.analyse_card(cbCardIndexTemp, weaveItems, weaveCount, analyseItemArray, false);
		if (!bValue) {

			chiHuRight.set_empty();
			return GameConstants.WIK_NULL;
		}

		// 胡牌分析
		// 牌型分析 现在没有这个选项
		// for (int i=0;i<analyseItemArray.size();i++)
		// {
		// //变量定义
		// AnalyseItem pAnalyseItem=analyseItemArray.get(i);
		// if (has_rule(MJGameConstants.GAME_TYPE_ZZ_258))
		// {
		// int cbCardValue = _logic.get_card_value(pAnalyseItem.cbCardEye);
		// if( cbCardValue != 2 && cbCardValue != 5 && cbCardValue != 8 )
		// {
		// continue;
		// }
		// }
		// cbChiHuKind = MJGameConstants.WIK_CHI_HU;
		// break;
		// }

		cbChiHuKind = GameConstants.WIK_CHI_HU;

		if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
			// cbChiHuKind = MJGameConstants.WIK_CHI_HU;

			chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
		} else {
			chiHuRight.opr_or(GameConstants.CHR_SHU_FAN);

		}

		return cbChiHuKind;
	}

	public int get_xp_ting_card(int[] cards, int cards_index[], WeaveItem weaveItem[], int cbWeaveCount) {

		// 复制数据
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		ChiHuRight chr = new ChiHuRight();

		// int cbCurrentCard =
		// _logic.switch_to_card_data(this._logic.get_magic_card_index());
		// if( MJGameConstants.WIK_CHI_HU == analyse_chi_hu_card_hz(
		// cbCardIndexTemp,weaveItem,cbWeaveCount,cbCurrentCard,chr,true ) ){
		// cards[count] = cbCurrentCard;
		// count++;
		//
		// // cards[0] = -1;
		// }
		int count = 0;
		int cbCurrentCard;
		for (int i = 0; i < GameConstants.MAX_INDEX - GameConstants.CARD_HUA_COUNT - GameConstants.CARD_FENG_COUNT; i++) {
			if (this._logic.is_magic_index(i))
				continue;
			cbCurrentCard = _logic.switch_to_card_data(i);
			chr.set_empty();
			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card_xp(cbCardIndexTemp, weaveItem, cbWeaveCount, cbCurrentCard, chr,
					GameConstants.HU_CARD_TYPE_ZIMO)) {
				cards[count] = cbCurrentCard;
				count++;
			}
		}

		// 有胡的牌。癞子肯定能胡
		if (count > 0) {
			cards[count] = _logic.switch_to_card_data(this._logic.get_magic_card_index(0)) + GameConstants.CARD_ESPECIAL_TYPE_GUI;
			count++;
			cards[count] = _logic.switch_to_card_data(this._logic.get_magic_card_index(1)) + GameConstants.CARD_ESPECIAL_TYPE_GUI;
			count++;
		} else {
			// 看看鬼牌能不能胡
			cbCurrentCard = _logic.switch_to_card_data(this._logic.get_magic_card_index(0));
			chr.set_empty();
			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card_xp(cbCardIndexTemp, weaveItem, cbWeaveCount, cbCurrentCard, chr,
					GameConstants.HU_CARD_TYPE_ZIMO)) {
				cards[count] = cbCurrentCard + GameConstants.CARD_ESPECIAL_TYPE_GUI;
				count++;

				cbCurrentCard = _logic.switch_to_card_data(this._logic.get_magic_card_index(1));
				cards[count] = cbCurrentCard + GameConstants.CARD_ESPECIAL_TYPE_GUI;
				count++;
			}

			// cbCurrentCard = _logic.switch_to_card_data(
			// this._logic.get_magic_card_index(1) );
			// cards[count] =
			// cbCurrentCard+MJGameConstants.CARD_ESPECIAL_TYPE_GUI;
			// count++;
			// chr.set_empty();
			// if( MJGameConstants.WIK_CHI_HU == analyse_chi_hu_card_hz(
			// cbCardIndexTemp,weaveItem,cbWeaveCount,cbCurrentCard,chr,
			// MJGameConstants.HU_CARD_TYPE_ZIMO ) ){
			// cards[count] =
			// cbCurrentCard+MJGameConstants.CARD_ESPECIAL_TYPE_GUI;
			// count++;
			// }

		}

		if (count >= 27) {
			count = 1;
			cards[0] = -1;
		}

		return count;
	}

	// 强制把玩家踢出房间
	public boolean force_kick_player_out_room(int seat_index, String tip) {

		if (seat_index == GameConstants.INVALID_SEAT)
			return false;
		Player p = this.get_players()[seat_index];
		if (p == null)
			return false;

		RoomResponse.Builder quit_roomResponse = RoomResponse.newBuilder();
		quit_roomResponse.setType(MsgConstants.RESPONSE_FORCE_EXIT);// 直接拉出游戏
		send_response_to_player(seat_index, quit_roomResponse);

		send_error_notify(seat_index, 2, tip);// "您已退出该游戏"
		this.get_players()[seat_index] = null;
		_player_ready[seat_index] = 0;
		// _player_open_less[seat_index] = 0;

		PlayerServiceImpl.getInstance().quitRoomId(this.getRoom_id(), p.getAccount_id());

		if (getPlayerCount() == 0) {// 释放房间
			PlayerServiceImpl.getInstance().delRoomId(this.getRoom_id());
		} else {
			// 刷新玩家
			RoomResponse.Builder refreshroomResponse = RoomResponse.newBuilder();
			refreshroomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);
			this.load_player_info_data(refreshroomResponse);
			send_response_to_other(seat_index, refreshroomResponse);
		}
		if (_kick_schedule != null) {
			_kick_schedule.cancel(false);
			_kick_schedule = null;
		}
		return true;
	}

	public boolean check_if_kick_unready_player() {
		if (!is_sys())
			return false;
		if (getPlayerCount() == GameConstants.GAME_PLAYER && _kick_schedule == null) {// 人满
			for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
				if (_player_ready[i] == 1) {
					_kick_schedule = GameSchedule.put(new KickRunnable(getRoom_id()), GameConstants.TRUSTEE_TIME_OUT_SECONDS, TimeUnit.SECONDS);
					return true;
				}
			}
		}
		return false;
	}

	public boolean check_if_cancel_kick() {
		if (!is_sys())
			return false;

		if (getPlayerCount() == GameConstants.GAME_PLAYER && _kick_schedule != null) {// 人满
			for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
				if (_player_ready[i] == 1) {
					return false;
				}
			}

			_kick_schedule.cancel(false);
			_kick_schedule = null;
		}
		return false;
	}

	@Override
	public boolean handler_player_ready_in_gold(int get_seat_index, boolean is_cancel) {
		if (is_cancel) {// 取消准备
			if (this.get_players()[get_seat_index] == null) {
				return false;
			}
			if (GameConstants.GS_MJ_FREE != _game_status && GameConstants.GS_MJ_WAIT != _game_status) {
				return false;
			}
			check_if_cancel_kick();

			_player_ready[get_seat_index] = 0;
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_PLAYER_READY);
			roomResponse.setOperatePlayer(get_seat_index);
			roomResponse.setIsCancelReady(is_cancel);
			send_response_to_room(roomResponse);
			if (this._cur_round > 0) {
				// 结束后刷新玩家
				RoomResponse.Builder roomResponse2 = RoomResponse.newBuilder();
				roomResponse2.setGameStatus(_game_status);
				roomResponse2.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);// 刷新玩家
				this.load_player_info_data(roomResponse2);
				this.send_response_to_player(get_seat_index, roomResponse2);
			}
			return false;
		} else {
			Player p = this.get_players()[get_seat_index];
			if (p == null) {
				return false;
			}
			// 金币场配置
			SysParamModel sysParamModel = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(getGame_id()).get(5006);
			// 判断金币是否足够
			long gold = p.getMoney();
			long entrygold = sysParamModel.getVal4().longValue();
			if (gold < entrygold) {
				force_kick_player_out_room(get_seat_index, "金币必须大于" + entrygold + "才能游戏!");
				return false;
			}
			boolean ret = handler_player_ready(get_seat_index, is_cancel);
			check_if_kick_unready_player();
			return ret;
		}
	}

	@Override
	public boolean handler_player_ready(int seat_index, boolean is_cancel) {

		if (this.get_players()[seat_index] == null) {
			return false;
		}
		if (GameConstants.GS_MJ_FREE != _game_status && GameConstants.GS_MJ_WAIT != _game_status) {
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

		// 如果不是河南安阳麻将才重置跑呛，解决一下河南安阳麻将的跑呛bug
		if (!is_mj_type(GameConstants.GAME_TYPE_HENAN_AY)) {
			// 跑分
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				_player_result.pao[i] = 0;// 清掉 默认是-1
				_player_result.qiang[i] = 0;// 清掉 默认是-1
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
	public boolean handler_player_be_in_room(int seat_index) {
		if ((GameConstants.GS_MJ_FREE != _game_status && GameConstants.GS_MJ_WAIT != _game_status) && this.get_players()[seat_index] != null) {
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
	public void fixBugTemp(int seat_index) {
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
				for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
					roomResponse.addReleasePlayers(_gameRoomRecord.release_players[i]);
				}
				this.send_response_to_player(seat_index, roomResponse);
			}
		}
	}

	/**
	 * //用户出牌
	 * 
	 * @param seat_index
	 * @param card
	 * @return
	 */
	@Override
	public boolean handler_player_out_card(int seat_index, int card) {
		// 牌局未开 或者等待状态 调度不需要执行 add by zain 2017/6/1
		if ((_game_status == GameConstants.GS_MJ_FREE || _game_status == GameConstants.GS_MJ_WAIT) && is_sys())
			return false;
		// add end

		if (this._handler != null) {
			this._handler.handler_player_out_card(this, seat_index, card);
		}

		return true;
	}

	/**
	 * @param get_seat_index
	 * @param out_cards[]
	 * @param card_count
	 * @param b_out_card
	 * @return
	 */
	public boolean handler_operate_out_card_mul(int get_seat_index, List<Integer> list, int card_count, int b_out_card, String desc) {
		return true;
	}

	/***
	 * //用户操作
	 * 
	 * @param seat_index
	 * @param operate_code
	 * @param operate_card
	 * @return
	 */
	@Override
	public boolean handler_operate_card(int seat_index, int operate_code, int operate_card, int luoCode) {
		// 牌局未开 或者等待状态 调度不需要执行 add by zain 2017/6/1
		if ((_game_status == GameConstants.GS_MJ_FREE || _game_status == GameConstants.GS_MJ_WAIT) && is_sys())
			return false;
		// add end
		if (this._handler != null) {
			this._handler.handler_operate_card(this, seat_index, operate_code, operate_card);
		}

		return true;
	}

	/**
	 * @param get_seat_index
	 * @param operate_code
	 * @return
	 */
	@Override
	public boolean handler_operate_button(int seat_index, int operate_code) {
		return true;
	}

	/**
	 * @param get_seat_index
	 * @param call_banker
	 * @return
	 */
	@Override
	public boolean handler_call_banker(int seat_index, int call_banker) {
		return true;
	}

	/**
	 * @param seat_index
	 * @param jetton
	 * @return
	 */
	@Override
	public boolean handler_add_jetton(int seat_index, int jetton) {
		return true;
	}

	/**
	 * @param seat_index
	 * @param open_flag
	 * @return
	 */
	@Override
	public boolean handler_open_cards(int seat_index, boolean open_flag) {
		return true;
	}

	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private boolean process_xiao_hu(int seat_index, int operate_code) {
		PlayerStatus playerStatus = _playerStatus[seat_index];

		if (playerStatus.has_xiao_hu() == false) {
			this.log_player_error(seat_index, "没有小胡");
			return false;
		}

		if (operate_code != GameConstants.WIK_NULL) {
			ChiHuRight start_hu_right = GRR._start_hu_right[seat_index];

			start_hu_right.set_valid(true);// 小胡生效
			// int cbChiHuKind =
			// analyse_chi_hu_card_cs_xiaohu(GRR._cards_index[seat_index],
			// start_hu_right);
			//
			// //判断是不是有小胡
			// if(cbChiHuKind==MJGameConstants.WIK_NULL){
			// this.log_player_error(seat_index,"没有小胡");
			// return false;
			// }
			//
			int lStartHuScore = 0;

			int wFanShu = _logic.get_chi_hu_action_rank_cs(GRR._start_hu_right[seat_index]);

			lStartHuScore = wFanShu * GameConstants.CELL_SCORE;

			GRR._start_hu_score[seat_index] = lStartHuScore * 3;// 赢3个人的分数

			for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
				if (i == seat_index)
					continue;
				GRR._lost_fan_shu[i][seat_index] = wFanShu;// 自己杠了？？？？？？？？？？？
				GRR._start_hu_score[i] -= lStartHuScore;// 输的番薯
			}

			// 效果
			this.operate_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_HU, start_hu_right.type_count, start_hu_right.type_list,
					start_hu_right.type_count, GameConstants.INVALID_SEAT);

			// 构造扑克
			int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
			for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
				cbCardIndexTemp[i] = GRR._cards_index[seat_index][i];
			}

			int hand_card_indexs[] = new int[GameConstants.MAX_INDEX];
			int show_card_indexs[] = new int[GameConstants.MAX_INDEX];

			for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
				hand_card_indexs[i] = GRR._cards_index[seat_index][i];
			}

			if (start_hu_right._show_all) {
				for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
					show_card_indexs[i] = GRR._cards_index[seat_index][i];
					hand_card_indexs[i] = 0;
				}
			} else {
				if (start_hu_right._index_da_si_xi != GameConstants.MAX_INDEX) {
					hand_card_indexs[start_hu_right._index_da_si_xi] = 0;
					show_card_indexs[start_hu_right._index_da_si_xi] = 4;
				}
				if ((start_hu_right._index_liul_liu_shun_1 != GameConstants.MAX_INDEX)
						&& (start_hu_right._index_liul_liu_shun_2 != GameConstants.MAX_INDEX)) {
					hand_card_indexs[start_hu_right._index_liul_liu_shun_1] = 0;
					show_card_indexs[start_hu_right._index_liul_liu_shun_1] = 3;

					hand_card_indexs[start_hu_right._index_liul_liu_shun_2] = 0;
					show_card_indexs[start_hu_right._index_liul_liu_shun_2] = 3;
				}
			}

			int cards[] = new int[GameConstants.MAX_COUNT];

			// 刷新自己手牌
			int hand_card_count = _logic.switch_to_cards_data(hand_card_indexs, cards);
			this.operate_player_cards(seat_index, hand_card_count, cards, 0, null);

			// 显示 小胡排
			hand_card_count = _logic.switch_to_cards_data(show_card_indexs, cards);
			this.operate_show_card(seat_index, GameConstants.Show_Card_XiaoHU, hand_card_count, cards, GameConstants.INVALID_SEAT);

		} else {
			GRR._start_hu_right[seat_index].set_empty();
		}

		// 玩家操作
		playerStatus.operate(operate_code, 0);
		//
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			playerStatus = _playerStatus[i];
			if (playerStatus.has_xiao_hu() && playerStatus.is_respone() == false) {
				return false;
			}

		}

		// 用户状态
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			_playerStatus[i].clean_action();
			change_player_status(i, GameConstants.INVALID_VALUE);
			// _playerStatus[i].clean_status();
		}

		_table_scheduled = GameSchedule.put(new XiaoHuRunnable(this.getRoom_id(), seat_index, true), GameConstants.XIAO_HU_DELAY, TimeUnit.SECONDS);

		return true;
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
				|| is_mj_type(GameConstants.GAME_TYPE_HENAN_ZHUAN_ZHUAN) || is_mj_type(GameConstants.GAME_TYPE_HALL_ZHUAN_ZHUAN_MJ)
				|| is_mj_type(GameConstants.GAME_TYPE_HALL_HONG_ZHONG_MJ)) { // liuyan
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

	public int get_henan_lh_ting_card(int[] cards, int cards_index[], WeaveItem weaveItem[], int cbWeaveCount) {
		// 复制数据
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		ChiHuRight chr = new ChiHuRight();
		int cbCurrentCard;
		int count = 0;

		int l = GameConstants.MAX_INDEX - GameConstants.CARD_HUA_COUNT - GameConstants.CARD_FENG_COUNT;
		int ql = l;
		if (has_rule(GameConstants.GAME_RULE_HENAN_DAIFENG)) {
			if (GameDescUtil.has_rule(gameRuleIndexEx, GameConstants.GAME_RULE_HENAN_HONG_ZHONG_LAI_ZI)) {
				l += GameConstants.CARD_FENG_COUNT;
				ql += (GameConstants.CARD_FENG_COUNT - 1);
			} else {
				l += GameConstants.CARD_FENG_COUNT;
				ql = GameConstants.CARD_FENG_COUNT;
			}
		}
		for (int i = 0; i < l; i++) {
			if (GameDescUtil.has_rule(gameRuleIndexEx, GameConstants.GAME_RULE_HENAN_HONG_ZHONG_LAI_ZI)) {
				if (this._logic.is_magic_index(i))
					continue;
			}
			cbCurrentCard = _logic.switch_to_card_data(i);
			chr.set_empty();
			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card_xp(cbCardIndexTemp, weaveItem, cbWeaveCount, cbCurrentCard, chr,
					GameConstants.HU_CARD_TYPE_ZIMO)) {
				cards[count] = cbCurrentCard;
				count++;
			}
		}

		l -= 1;
		if (count == 0) {
		} else if (count > 0 && count < ql) {
			if (GameDescUtil.has_rule(gameRuleIndexEx, GameConstants.GAME_RULE_HENAN_HONG_ZHONG_LAI_ZI)) {
				// 有胡的牌。红中肯定能胡
				cards[count] = _logic.switch_to_card_data(this._logic.get_magic_card_index(0));
				count++;
			}
		} else {
			// 全听
			count = 1;
			cards[0] = -1;
		}

		return count;
	}

	/**
	 * 胡牌算分
	 * 
	 * @param seat_index
	 * @param provide_index
	 * @param operate_card
	 * @param zimo
	 */
	public void process_chi_hu_player_score(int seat_index, int provide_index, int operate_card, boolean zimo) {

		return;
	}

	// 过滤吃胡权值
	private void filtrate_right(int seat_index, ChiHuRight chr) {
		// 权位增加
		// 抢杠
		if (_current_player == GameConstants.INVALID_SEAT && _status_gang) {
			chr.opr_or(GameConstants.CHR_HUNAN_QIANG_GANG_HU);
		}
		// 海底捞
		// if (GRR._left_card_count == 0) {
		// chr.opr_or(MJGameConstants.CHR_HAI_DI_LAO);
		// }
		// 附加权位
		// 杠上花
		if (_current_player == seat_index && _status_gang) {
			chr.opr_or(GameConstants.CHR_HUNAN_GANG_KAI);
		}
		// 杠上炮
		if (_status_gang_hou_pao && !_status_gang) {
			chr.opr_or(GameConstants.CHR_HUNAN_GANG_SHANG_PAO);
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
	 */
	public void set_niao_card(int seat_index, int card, boolean show, int add_niao) {
		for (int i = 0; i < GameConstants.MAX_NIAO_CARD; i++) {
			GRR._cards_data_niao[i] = GameConstants.INVALID_VALUE;
		}

		// for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			GRR._player_niao_count[i] = 0;
			for (int j = 0; j < GameConstants.MAX_NIAO_CARD; j++) {
				GRR._player_niao_cards[i][j] = GameConstants.INVALID_VALUE;
			}

		}
		GRR._show_bird_effect = show;
		if (card == GameConstants.INVALID_VALUE) {
			GRR._count_niao = get_niao_card_num(true, add_niao);
		} else {
			GRR._count_niao = get_niao_card_num(false, add_niao);
		}

		if (GRR._count_niao > GameConstants.ZHANIAO_0) {
			if (card == GameConstants.INVALID_VALUE) {
				int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
				// if (is_mj_type(MJGameConstants.GAME_TYPE_HZ)) {
				// _logic.switch_to_cards_index(_repertory_card_zz,
				// _all_card_len-GRR._left_card_count, GRR._count_niao,
				// cbCardIndexTemp);
				// } else {
				// _logic.switch_to_cards_index(_repertory_card_cs,
				// _all_card_len-GRR._left_card_count, GRR._count_niao,
				// cbCardIndexTemp);
				// }
				_logic.switch_to_cards_index(_repertory_card, _all_card_len - GRR._left_card_count, GRR._count_niao, cbCardIndexTemp);
				// cbCardIndexTemp[0] = 3;
				// cbCardIndexTemp[1] = 0x25;
				GRR._left_card_count -= GRR._count_niao;
				_logic.switch_to_cards_data(cbCardIndexTemp, GRR._cards_data_niao);

				// for (int i = 0; i < GRR._count_niao; i++) {
				// GRR._cards_data_niao[i] = 0x13;
				// }
			} else {
				for (int i = 0; i < GRR._count_niao; i++) {
					GRR._cards_data_niao[i] = card;
				}
			}
		}
		// 中鸟个数
		GRR._count_pick_niao = _logic.get_pick_niao_count(GRR._cards_data_niao, GRR._count_niao);

		for (int i = 0; i < GRR._count_niao; i++) {
			int nValue = _logic.get_card_value(GRR._cards_data_niao[i]);
			int seat = 0;
			if ((is_mj_type(GameConstants.GAME_TYPE_FLS_HZ_LX) || is_mj_type(GameConstants.GAME_TYPE_SHUANGGUI)
					|| is_mj_type(GameConstants.GAME_TYPE_HENAN_HZ) || is_mj_type(GameConstants.GAME_TYPE_HENAN_ZHUAN_ZHUAN))) { // liuyan
				seat = get_zhong_seat_by_value_three(nValue, seat_index);
			} else if (GameConstants.GAME_TYPE_CS == _game_type_index || GameConstants.GAME_TYPE_ZHUZHOU == _game_type_index
					|| is_mj_type(GameConstants.GAME_TYPE_HALL_CHANG_SHA_MJ)) {
				seat = (GRR._banker_player + (nValue - 1) % 4) % 4;
			} else if (is_mj_type(GameConstants.GAME_TYPE_ZZ) || is_mj_type(GameConstants.GAME_TYPE_HZ)
					|| is_mj_type(GameConstants.GAME_TYPE_HALL_ZHUAN_ZHUAN_MJ) || is_mj_type(GameConstants.GAME_TYPE_HALL_HONG_ZHONG_MJ)) {
				seat = get_zhong_seat_by_value_three(nValue, seat_index);
			}
			GRR._player_niao_cards[seat][GRR._player_niao_count[seat]] = GRR._cards_data_niao[i];
			GRR._player_niao_count[seat]++;
		}
	}

	/**
	 * 获取坐飘分
	 */
	public int getZuoPiaoScore() {
		int score = 0;
		if (has_rule(GameConstants.GAME_RULE_HUNAN_CS_ZUOPIAO1)) {
			return GameConstants.ZUOPIAO_1;
		} else if (has_rule(GameConstants.GAME_RULE_HUNAN_CS_ZUOPIAO2)) {
			return GameConstants.ZUOPIAO_2;
		} else if (has_rule(GameConstants.GAME_RULE_HUNAN_CS_ZUOPIAO3)) {
			return GameConstants.ZUOPIAO_3;
		}
		return score;
	}

	/**
	 * 获取飞鸟 数量
	 * 
	 * @return
	 */
	public int getFeiNiaoNum() {
		int num = 0;
		if (has_rule(GameConstants.GAME_RULE_HUNAN_CS_FEI_NIAO2)) {
			return GameConstants.FEINIAO_2;
		} else if (has_rule(GameConstants.GAME_RULE_HUNAN_CS_FEI_NIAO4)) {
			return GameConstants.FEINIAO_4;
		}
		return num;
	}

	/**
	 * 是否乘法 定鸟
	 * 
	 * @return
	 */
	public boolean isMutlpDingNiao() {
		boolean isMutlp = has_rule(GameConstants.GAME_RULE_HUNAN_CS_DING_NIAO1) || has_rule(GameConstants.GAME_RULE_HUNAN_CS_DING_NIAO2);
		return isMutlp;
	}

	/**
	 * 乘法 定鸟 个数
	 * 
	 * @return
	 */
	public int getMutlpDingNiaoNum() {
		int num = 0;
		if (has_rule(GameConstants.GAME_RULE_HUNAN_CS_DING_NIAO1)) {
			num = GameConstants.ZHANIAO_1;
		} else if (has_rule(GameConstants.GAME_RULE_HUNAN_CS_DING_NIAO2)) {
			num = GameConstants.ZHANIAO_2;
		}
		return num;
	}

	/**
	 * 长沙 定鸟 个数
	 * 
	 * @return
	 */
	public int getCsDingNiaoNum() {
		int nNum = GameConstants.ZHANIAO_0;
		// 湖南麻将的抓鸟
		if (is_mj(GameConstants.GAME_ID_HUNAN) || is_mj(GameConstants.GAME_ID_FLS_LX)) {
			if (isMutlpDingNiao()) {// 乘法鸟
				nNum = getMutlpDingNiaoNum();
			} else {
				if (has_rule(GameConstants.GAME_RULE_HUNAN_ZHANIAO2)) {
					nNum = GameConstants.ZHANIAO_2;
				} else if (has_rule(GameConstants.GAME_RULE_HUNAN_ZHANIAO4)) {
					nNum = GameConstants.ZHANIAO_4;
				} else if (has_rule(GameConstants.GAME_RULE_HUNAN_ZHANIAO6)) {
					nNum = GameConstants.ZHANIAO_6;
				}
			}
		}
		return nNum;
	}

	/**
	 * 获取长沙能抓的定鸟的 数量
	 * 
	 * @return
	 */
	public int get_ding_niao_card_num_cs(boolean check) {
		int nNum = getCsDingNiaoNum();
		if (check == false) {
			return nNum;
		}
		if (nNum > GRR._left_card_count) {
			nNum = GRR._left_card_count;
		}
		return nNum;
	}

	/**
	 * 获取长沙能抓的飞鸟的 数量
	 * 
	 * @return
	 */
	public int get_fei_niao_card_num_cs(boolean check) {
		int fNum = getFeiNiaoNum();
		if (check == false) {
			return fNum;
		}
		if (fNum > GRR._left_card_count) {
			fNum = GRR._left_card_count;
		}
		return fNum;
	}

	/**
	 * 获取长沙能抓的鸟的总 数量
	 * 
	 * @return
	 */
	public int get_niao_card_num_cs(boolean check) {
		int nNum = get_ding_niao_card_num_cs(check);
		if (check == false) {
			return nNum;
		}
		nNum += get_fei_niao_card_num_cs(check);
		return nNum;
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
	public void set_niao_card_cs(int seat_index, int card, boolean show, int add_niao, boolean isTongPao) {
		for (int i = 0; i < GameConstants.MAX_NIAO_CARD; i++) {
			GRR._cards_data_niao[i] = GameConstants.INVALID_VALUE;
		}

		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			GRR._player_niao_count[i] = 0;
			for (int j = 0; j < GameConstants.MAX_NIAO_CARD; j++) {
				GRR._player_niao_cards[i][j] = GameConstants.INVALID_VALUE;
			}

		}
		GRR._show_bird_effect = show;
		if (card == GameConstants.INVALID_VALUE) {
			GRR._count_niao = get_ding_niao_card_num_cs(true);
		} else {
			GRR._count_niao = get_ding_niao_card_num_cs(false);
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
		// 中鸟个数
		GRR._count_pick_niao = _logic.get_pick_niao_count(GRR._cards_data_niao, GRR._count_niao);

		for (int i = 0; i < GRR._count_niao; i++) {
			int nValue = _logic.get_card_value(GRR._cards_data_niao[i]);
			int seat = 0;
			if ((GameConstants.GAME_TYPE_ZZ == _game_type_index) || is_mj_type(GameConstants.GAME_TYPE_HZ)
					|| is_mj_type(GameConstants.GAME_TYPE_FLS_HZ_LX) || is_mj_type(GameConstants.GAME_TYPE_SHUANGGUI)
					|| is_mj_type(GameConstants.GAME_TYPE_HENAN_HZ) || is_mj_type(GameConstants.GAME_TYPE_HALL_HONG_ZHONG_MJ)
					|| is_mj_type(GameConstants.GAME_TYPE_HALL_ZHUAN_ZHUAN_MJ)) {
				seat = (seat_index + (nValue - 1) % 4) % 4;
			} else if (GameConstants.GAME_TYPE_CS == _game_type_index || GameConstants.GAME_TYPE_ZHUZHOU == _game_type_index
					|| is_mj_type(GameConstants.GAME_TYPE_HALL_CHANG_SHA_MJ)) {
				seat = get_zhong_seat_by_value_three(nValue, GRR._banker_player);
			} else if (GameConstants.GAME_TYPE_FLS_CS_LX == _game_type_index) {
				seat = get_zhong_seat_by_value_cslx(nValue, GRR._banker_player);
			}
			GRR._player_niao_cards[seat][GRR._player_niao_count[seat]] = GRR._cards_data_niao[i];
			GRR._player_niao_count[seat]++;
		}

		if (!isTongPao) {
			set_niao_card_cs_fei(seat_index, card, show, add_niao);
		}

	}

	private int get_zhong_seat_by_value_cslx(int nValue, int banker_seat) {
		int seat = 0;
		if (getTablePlayerNumber() == 4) {
			seat = (banker_seat + (nValue - 1) % 4) % 4;
		} else {// 3人场//这里这么特殊处理是因为 算鸟是根据玩家逻辑位置,只有庄家上家,庄家下家,不存在对家
			int v = (nValue - 1) % 4;
			switch (v) {
			case 0:// 本家//159
				seat = GRR._banker_player;
				break;
			case 1:// 26//下家
				seat = get_banker_next_seat(GRR._banker_player);
				break;
			case 2:// 37//对家
				seat = get_null_seat();
				break;
			default:// 48//上家
				seat = get_banker_pre_seat(GRR._banker_player);
				break;
			}
		}
		return seat;
	}

	/**
	 * 三人场麻将获取飞鸟位置
	 * 
	 * @param nValue
	 * @param banker_seat
	 * @return
	 */
	private int get_zhong_seat_by_value_three(int nValue, int banker_seat) {
		int seat = 0;
		if (getTablePlayerNumber() == 4) {
			seat = (banker_seat + (nValue - 1) % 4) % 4;
		} else {// 3人场//这里这么特殊处理是因为 算鸟是根据玩家逻辑位置,只有庄家上家,庄家下家,不存在对家
			int v = (nValue - 1) % 4;
			switch (v) {
			case 0:// 本家//159
				seat = banker_seat;
				break;
			case 1:// 26//下家
				seat = get_banker_next_seat(banker_seat);
				break;
			case 2:// 37//对家
				seat = get_null_seat();
				break;
			default:// 48//上家
				seat = get_banker_pre_seat(banker_seat);
				break;
			}
		}
		return seat;
	}

	/**
	 * 
	 * @param seat_index
	 * @param card
	 *            固定的鸟
	 * @param show
	 * @param add_niao
	 *            额外奖鸟
	 */
	private void set_niao_card_cs_fei(int seat_index, int card, boolean show, int add_niao) {
		for (int i = 0; i < GameConstants.MAX_NIAO_CARD; i++) {
			GRR._cards_data_niao_fei[i] = GameConstants.INVALID_VALUE;
		}

		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			GRR._player_niao_count_fei[i] = 0;
			for (int j = 0; j < GameConstants.MAX_NIAO_CARD; j++) {
				GRR._player_niao_cards_fei[i][j] = GameConstants.INVALID_VALUE;
			}

		}
		GRR._count_niao_fei = get_fei_niao_card_num_cs(true);

		if (GRR._count_niao_fei > GameConstants.ZHANIAO_0) {
			if (card == GameConstants.INVALID_VALUE) {
				int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
				_logic.switch_to_cards_index(_repertory_card, _all_card_len - GRR._left_card_count, GRR._count_niao_fei, cbCardIndexTemp);
				GRR._left_card_count -= GRR._count_niao_fei;
				_logic.switch_to_cards_data(cbCardIndexTemp, GRR._cards_data_niao_fei);
			}
		}
		// 中鸟个数
		GRR._count_pick_niao_fei = _logic.get_pick_niao_count(GRR._cards_data_niao_fei, GRR._count_niao_fei);

		for (int i = 0; i < GRR._count_niao_fei; i++) {
			int nValue = _logic.get_card_value(GRR._cards_data_niao_fei[i]);
			int seat = 0;
			if ((GameConstants.GAME_TYPE_ZZ == _game_type_index) || is_mj_type(GameConstants.GAME_TYPE_HZ)
					|| is_mj_type(GameConstants.GAME_TYPE_FLS_HZ_LX) || is_mj_type(GameConstants.GAME_TYPE_SHUANGGUI)
					|| is_mj_type(GameConstants.GAME_TYPE_HENAN_HZ) || is_mj_type(GameConstants.GAME_TYPE_HALL_HONG_ZHONG_MJ)
					|| is_mj_type(GameConstants.GAME_TYPE_HALL_ZHUAN_ZHUAN_MJ)) {
				seat = (seat_index + (nValue - 1) % 4) % 4;
			} else if (GameConstants.GAME_TYPE_ZHUZHOU == _game_type_index) {
				seat = (GRR._banker_player + (nValue - 1) % 4) % 4;
			} else if (GameConstants.GAME_TYPE_CS == _game_type_index || is_mj_type(GameConstants.GAME_TYPE_HALL_CHANG_SHA_MJ)) {
				seat = get_zhong_seat_by_value_three(nValue, GRR._banker_player);
			}
			GRR._player_niao_cards_fei[seat][GRR._player_niao_count_fei[seat]] = GRR._cards_data_niao_fei[i];
			GRR._player_niao_count_fei[seat]++;
		}
	}

	/**
	 * 获取鸟的 数量
	 * 
	 * @param check
	 * @param add_niao
	 * @return
	 */
	public int get_niao_card_num(boolean check, int add_niao) {
		int nNum = GameConstants.ZHANIAO_0;

		// 湖南麻将的抓鸟
		if (is_mj(GameConstants.GAME_ID_HUNAN) || is_mj(GameConstants.GAME_ID_FLS_LX) || _game_type_index == GameConstants.GAME_TYPE_HENAN_ZHUAN_ZHUAN
				|| is_mj_type(GameConstants.GAME_TYPE_HALL_HONG_ZHONG_MJ) || is_mj_type(GameConstants.GAME_TYPE_HALL_ZHUAN_ZHUAN_MJ)) {
			if (has_rule(GameConstants.GAME_RULE_HUNAN_ZHANIAO1)) {
				nNum = GameConstants.ZHANIAO_1;
			} else if (has_rule(GameConstants.GAME_RULE_HUNAN_ZHANIAO2)) {
				nNum = GameConstants.ZHANIAO_2;
			} else if (has_rule(GameConstants.GAME_RULE_HUNAN_ZHANIAO4)) {
				nNum = GameConstants.ZHANIAO_4;
			} else if (has_rule(GameConstants.GAME_RULE_HUNAN_ZHANIAO6)) {
				nNum = GameConstants.ZHANIAO_6;
			}

		} else if (is_mj(GameConstants.GAME_ID_HENAN)) {
			// 河南麻将的抓鸟
			if (has_rule(GameConstants.GAME_RULE_HENAN_ZHANIAO2)) {
				nNum = GameConstants.ZHANIAO_2;
			} else if (has_rule(GameConstants.GAME_RULE_HENAN_ZHANIAO4)) {
				nNum = GameConstants.ZHANIAO_4;
			} else if (has_rule(GameConstants.GAME_RULE_HENAN_ZHANIAO6)) {
				nNum = GameConstants.ZHANIAO_6;
			}
		}

		nNum += add_niao;

		if (check == false) {
			return nNum;
		}
		if (nNum > GRR._left_card_count) {
			nNum = GRR._left_card_count;
		}
		return nNum;
	}

	@Override
	public boolean handler_requst_pao_qiang(Player player, int pao, int qiang) {

		/**
		 * if(_playerStatus[player.get_seat_index()]._is_pao_qiang ||
		 * _playerStatus[player.get_seat_index()]._is_pao){ return false; }
		 */

		return false;

	}

	@Override
	public boolean handler_requst_nao_zhuang(Player player, int nao) {
		return false;

	}

	/////////////////////////////////////////////////////// send///////////////////////////////////////////
	/////
	/**
	 * 基础状态
	 * 
	 * @return
	 */
	public boolean operate_player_status() {
		//// 发送玩家出牌 201 =玩家出牌(CMD_OutCard: operate_player,operate_card)
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_PLAYER_STATUS);
		this.load_common_status(roomResponse);
		return this.send_response_to_room(roomResponse);
	}

	/**
	 * 刷新玩家信息
	 * 
	 * @return
	 */
	public boolean operate_player_data() {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);
		this.load_player_info_data(roomResponse);

		GRR.add_room_response(roomResponse);

		return this.send_response_to_room(roomResponse);
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
	public boolean operate_out_card(int seat_index, int count, int cards[], int type, int to_player) {
		if (seat_index == GameConstants.INVALID_SEAT) {
			return false;
		}
		//// 发送玩家出牌 201 =玩家出牌(CMD_OutCard: operate_player,operate_card)
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		this.load_common_status(roomResponse);
		roomResponse.setType(MsgConstants.RESPONSE_OUT_CARD);
		roomResponse.setTarget(seat_index);
		roomResponse.setCardType(type);// 出牌
		roomResponse.setCardCount(count);

		int flashTime = 150;
		int standTime = 130;
		int gameId = this.getGame_id() == 0 ? 1 : this.getGame_id();
		SysParamModel sysParamModel105 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(gameId).get(1105);
		if (sysParamModel105 != null && sysParamModel105.getVal3() > 0 && sysParamModel105.getVal3() < 1000) {
			flashTime = sysParamModel105.getVal3();
			standTime = sysParamModel105.getVal4();
		}
		roomResponse.setFlashTime(flashTime);
		roomResponse.setStandTime(standTime);
		for (int i = 0; i < count; i++) {

			roomResponse.addCardData(cards[i]);
		}

		if (to_player == GameConstants.INVALID_SEAT) {
			GRR.add_room_response(roomResponse);
			return this.send_response_to_room(roomResponse);
		} else {
			return this.send_response_to_player(to_player, roomResponse);
		}

	}

	public boolean operate_out_card_bao_ting(int seat_index, int count, int cards[], int type, int to_player) {
		if (seat_index == GameConstants.INVALID_SEAT) {
			return false;
		}
		//// 发送玩家出牌 201 =玩家出牌(CMD_OutCard: operate_player,operate_card)
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		this.load_common_status(roomResponse);
		roomResponse.setType(MsgConstants.RESPONSE_OUT_CARD);
		roomResponse.setTarget(seat_index);
		roomResponse.setCardType(type);// 出牌
		roomResponse.setCardCount(count);
		int flashTime = 150;
		int standTime = 150;
		int gameId = this.getGame_id() == 0 ? 1 : this.getGame_id();
		SysParamModel sysParamModel105 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(gameId).get(1105);
		if (sysParamModel105 != null && sysParamModel105.getVal3() > 0 && sysParamModel105.getVal3() < 1000) {
			flashTime = sysParamModel105.getVal3();
			standTime = sysParamModel105.getVal4();
		}
		roomResponse.setFlashTime(flashTime);
		roomResponse.setStandTime(standTime);

		if (to_player == GameConstants.INVALID_SEAT) {
			for (int i = 0; i < count; i++) {

				roomResponse.addCardData(cards[i]);
			}
			this.send_response_to_player(seat_index, roomResponse);// 自己有值

			GRR.add_room_response(roomResponse);

			roomResponse.clearCardData();
			for (int i = 0; i < count; i++) {

				roomResponse.addCardData(GameConstants.BLACK_CARD);
			}
			this.send_response_to_other(seat_index, roomResponse);// 别人是背着的
		} else {
			if (to_player == seat_index) {
				for (int i = 0; i < count; i++) {
					roomResponse.addCardData(cards[i]);
				}
			} else {
				for (int i = 0; i < count; i++) {
					roomResponse.addCardData(GameConstants.BLACK_CARD);
				}
			}

			this.send_response_to_player(to_player, roomResponse);
		}

		return true;
	}

	// 添加牌到牌堆
	@Override
	public boolean operate_add_discard(int seat_index, int count, int cards[]) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		this.load_common_status(roomResponse);
		roomResponse.setType(MsgConstants.RESPONSE_ADD_DISCARD);
		roomResponse.setTarget(seat_index);
		roomResponse.setCardType(2);// 出牌
		roomResponse.setCardCount(count);
		for (int i = 0; i < count; i++) {
			roomResponse.addCardData(cards[i]);
		}
		GRR.add_room_response(roomResponse);
		this.send_response_to_room(roomResponse);

		return true;
	}

	/**
	 * 效果 (通知玩家弹出 吃碰杆 胡牌==效果)
	 * 
	 * @param seat_index
	 * @param effect_type
	 * @param effect_count
	 * @param effect_indexs
	 * @param time
	 * @param to_player
	 * @return
	 */
	public boolean operate_effect_action(int seat_index, int effect_type, int effect_count, long effect_indexs[], int time, int to_player) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_EFFECT_ACTION);
		roomResponse.setEffectType(effect_type);
		roomResponse.setTarget(seat_index);
		roomResponse.setEffectCount(effect_count);
		for (int i = 0; i < effect_count; i++) {
			roomResponse.addEffectsIndex(effect_indexs[i]);
		}

		roomResponse.setEffectTime(time);

		if (to_player == GameConstants.INVALID_SEAT) {
			this.send_response_to_room(roomResponse);
		} else {
			this.send_response_to_player(to_player, roomResponse);
		}
		GRR.add_room_response(roomResponse);

		return true;
	}

	// int seat_index, int effect_type, int effect_count, long effect_indexs[],
	// int time, int to_player
	public boolean operate_shai_zi_effect(int num1, int num2, int anim_time, int delay_time) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_EFFECT_ACTION);
		roomResponse.setEffectType(GameConstants.Effect_Action_SHAI_ZI);
		roomResponse.setTarget(get_target_shai_zi_player(num1, num2));
		roomResponse.setEffectCount(2);
		roomResponse.addEffectsIndex(num1);
		roomResponse.addEffectsIndex(num2);
		roomResponse.setEffectTime(anim_time);// anim time//摇骰子动画的时间
		roomResponse.setStandTime(delay_time); // delay time//停留时间
		this.send_response_to_room(roomResponse);
		GRR.add_room_response(roomResponse);
		return true;
	}

	// 计算中骰子的玩家
	public int get_target_shai_zi_player(int num1, int num2) {
		return get_zhong_seat_by_value_cslx(num1 + num2, GRR._banker_player);// GameConstants.INVALID_SEAT;
	}

	public String get_xiao_hu_shai_zi_desc(int seat) {
		int target_seat = get_target_shai_zi_player(_player_result.shaizi[seat][0], _player_result.shaizi[seat][1]);
		if (target_seat == seat)
			return "(骰子:" + _player_result.shaizi[seat][0] + "," + _player_result.shaizi[seat][1] + " 全中)";
		else {
			// 庄家dong
			// get_banker_next_seat(banker_seat)
			if (get_players()[target_seat] == null) {
				return "(骰子:" + _player_result.shaizi[seat][0] + "," + _player_result.shaizi[seat][1] + " 不中)";
			}

			String[] str = new String[4];
			str[0] = "东";
			str[1] = "南";
			str[2] = "西";
			str[3] = "北";
			return "(骰子:" + _player_result.shaizi[seat][0] + "," + _player_result.shaizi[seat][1] + " 中" + str[target_seat] + ")";
			/*
			 * if(GRR._banker_player == target_seat){//zhuang dong return
			 * "(骰子:"+_player_result.shaizi[seat][0]+","+_player_result.shaizi[
			 * seat][1]+" 中东)"; }
			 * 
			 * if( get_banker_next_seat(GRR._banker_player) == target_seat )
			 * return
			 * "(骰子:"+_player_result.shaizi[seat][0]+","+_player_result.shaizi[
			 * seat][1]+" 中南)";
			 * 
			 * if( get_banker_pre_seat(GRR._banker_player) == target_seat )
			 * return
			 * "(骰子:"+_player_result.shaizi[seat][0]+","+_player_result.shaizi[
			 * seat][1]+ ( getTablePlayerNumber()==3?" 中西)":" 中北)"); //4人场 对家
			 * return
			 * "(骰子:"+_player_result.shaizi[seat][0]+","+_player_result.shaizi[
			 * seat][1]+" 中西)";
			 */
		}
	}

	public boolean record_effect_action(int seat_index, int effect_type, int effect_count, long effect_indexs[], int time) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_EFFECT_ACTION);
		roomResponse.setEffectType(effect_type);
		roomResponse.setTarget(seat_index);
		roomResponse.setEffectCount(effect_count);
		for (int i = 0; i < effect_count; i++) {
			roomResponse.addEffectsIndex(effect_indexs[i]);
		}

		roomResponse.setEffectTime(time);

		GRR.add_room_response(roomResponse);

		return true;
	}

	public void record_discard_gang(int seat_index) {
		try {
			boolean gang = _playerStatus[seat_index].has_action_by_code(GameConstants.WIK_GANG)
					|| _playerStatus[seat_index].has_action_by_code(GameConstants.WIK_DA_CHAO_TIAN)
					|| _playerStatus[seat_index].has_action_by_code(GameConstants.WIK_MENG_XIAO)
					|| _playerStatus[seat_index].has_action_by_code(GameConstants.WIK_HUI_TOU_XIAO)
					|| _playerStatus[seat_index].has_action_by_code(GameConstants.WIK_DIAN_XIAO)
					|| _playerStatus[seat_index].has_action_by_code(GameConstants.WIK_XIAO_CHAO_TIAN);
			if (gang) {
				for (int i = 0; i < _playerStatus[seat_index]._weave_count; i++) {
					if ((_playerStatus[seat_index]._action_weaves[i].weave_kind == GameConstants.WIK_GANG
							|| _playerStatus[seat_index]._action_weaves[i].weave_kind == GameConstants.WIK_DA_CHAO_TIAN
							|| _playerStatus[seat_index]._action_weaves[i].weave_kind == GameConstants.WIK_MENG_XIAO
							|| _playerStatus[seat_index]._action_weaves[i].weave_kind == GameConstants.WIK_HUI_TOU_XIAO
							|| _playerStatus[seat_index]._action_weaves[i].weave_kind == GameConstants.WIK_DIAN_XIAO
							|| _playerStatus[seat_index]._action_weaves[i].weave_kind == GameConstants.WIK_XIAO_CHAO_TIAN)) {
						int card = _playerStatus[seat_index]._action_weaves[i].center_card;

						boolean find = false;
						for (int j = 0; j < GRR._discard_count_gang[seat_index]; j++) {
							if (GRR._discard_cards_gang[seat_index][j] == card) {
								find = true;
								break;
							}
						}
						if (!find) {
							GRR._discard_count_gang[seat_index]++;
							GRR._discard_cards_gang[seat_index][GRR._discard_count_gang[seat_index] - 1] = card;
						}

					}
				}
			}
		} catch (Exception e) {
			logger.error("error", e);
		}
	}

	public boolean isIndiscardGang(int seat_index, int card) {
		for (int j = 0; j < GRR._discard_count_gang[seat_index]; j++) {
			if (GRR._discard_cards_gang[seat_index][j] == card) {
				return true;
			}
		}
		return false;
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
			roomResponse.addWeaveItems(weaveItem_item);
		}
		GRR.add_room_response(roomResponse);
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
	public boolean operate_player_get_card(int seat_index, int count, int cards[], int to_player) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		this.load_common_status(roomResponse);
		roomResponse.setType(MsgConstants.RESPONSE_SEND_CARD);
		roomResponse.setTarget(seat_index);
		roomResponse.setCardType(2);// get牌
		roomResponse.setCardCount(count);

		if (to_player == GameConstants.INVALID_SEAT) {
			this.send_response_to_other(seat_index, roomResponse);

			for (int i = 0; i < count; i++) {
				roomResponse.addCardData(cards[i]);
			}
			GRR.add_room_response(roomResponse);
			return this.send_response_to_player(seat_index, roomResponse);

		} else {
			if (seat_index == to_player) {
				for (int i = 0; i < count; i++) {
					roomResponse.addCardData(cards[i]);
				}
			}
			// GRR.add_room_response(roomResponse);
			return this.send_response_to_player(to_player, roomResponse);
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
	public boolean operate_player_weave_cards(int seat_index, int weave_count, WeaveItem weaveitems[]) {
		// System.out.println(seat_index);
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYER_WEAVE_CARDS);// 12更新玩家手牌数据
		roomResponse.setGameStatus(_game_status);
		roomResponse.setTarget(seat_index);
		roomResponse.setCardType(1);

		this.load_common_status(roomResponse);

		// 手牌数量
		// roomResponse.setCardCount(card_count);
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

		/*
		 * // 手牌--将自己的手牌数据发给自己 for (int j = 0; j < card_count; j++) {
		 * roomResponse.addCardData(cards[j]); }
		 */
		GRR.add_room_response(roomResponse);
		// 自己才有牌数据
		this.send_response_to_player(seat_index, roomResponse);

		return true;
	}

	public boolean operate_player_cards_flag(int seat_index, int card_count, int cards[], int weave_count, WeaveItem weaveitems[]) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYER_CARDS);// 12更新玩家手牌数据
		roomResponse.setGameStatus(_game_status);
		roomResponse.setTarget(seat_index);
		roomResponse.setCardType(2);

		// this.load_common_status(roomResponse);
		//
		// //手牌数量
		// roomResponse.setCardCount(card_count);
		// roomResponse.setWeaveCount(weave_count);
		// //组合牌
		// if (weave_count>0) {
		// for (int j = 0; j < weave_count; j++) {
		// WeaveItemResponse.Builder weaveItem_item =
		// WeaveItemResponse.newBuilder();
		// weaveItem_item.setProvidePlayer(weaveitems[j].provide_player);
		// weaveItem_item.setPublicCard(weaveitems[j].public_card);
		// weaveItem_item.setWeaveKind(weaveitems[j].weave_kind);
		// weaveItem_item.setCenterCard(weaveitems[j].center_card);
		// roomResponse.addWeaveItems(weaveItem_item);
		// }
		// }
		//
		// this.send_response_to_other(seat_index, roomResponse);
		//
		// // 手牌
		// for (int j = 0; j < card_count; j++) {
		// roomResponse.addCardData(cards[j]);
		// }
		GRR.add_room_response(roomResponse);
		// 自己才有牌数据
		this.send_response_to_player(seat_index, roomResponse);

		return true;
	}

	/**
	 * 刷新玩家的牌
	 * 
	 * @param seat_index
	 * @param card_count
	 * @param cards
	 * @param weave_count
	 * @param weaveitems
	 * @return
	 */
	public boolean operate_player_cards_with_ting(int seat_index, int card_count, int cards[], int weave_count, WeaveItem weaveitems[]) {
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
				weaveItem_item.setCenterCard(weaveitems[j].center_card);
				roomResponse.addWeaveItems(weaveItem_item);
			}
		}

		this.send_response_to_other(seat_index, roomResponse);

		// 手牌
		for (int j = 0; j < card_count; j++) {
			roomResponse.addCardData(cards[j]);
		}

		int ting_count = _playerStatus[seat_index]._hu_out_card_count;

		// 打出去可以听牌的个数
		roomResponse.setOutCardCount(ting_count);

		for (int i = 0; i < ting_count; i++) {
			int ting_card_cout = _playerStatus[seat_index]._hu_out_card_ting_count[i];
			roomResponse.addOutCardTingCount(ting_card_cout);
			if (_logic.is_magic_card(_playerStatus[seat_index]._hu_out_card_ting[i])) {
				roomResponse.addOutCardTing(_playerStatus[seat_index]._hu_out_card_ting[i] + GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI);
			} else {
				roomResponse.addOutCardTing(_playerStatus[seat_index]._hu_out_card_ting[i] + GameConstants.CARD_ESPECIAL_TYPE_TING);
			}

			Int32ArrayResponse.Builder int_array = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < ting_card_cout; j++) {
				int_array.addItem(_playerStatus[seat_index]._hu_out_cards[i][j]);
			}
			roomResponse.addOutCardTingCards(int_array);
		}

		// 自己才有牌数据
		this.send_response_to_player(seat_index, roomResponse);

		GRR.add_room_response(roomResponse);

		return true;
	}

	/**
	 * 刷新玩家的牌
	 * 
	 * @param seat_index
	 * @param card_count
	 * @param cards
	 * @param weave_count
	 * @param weaveitems
	 * @return
	 */
	public boolean operate_player_cards_with_ting_xc(int seat_index, int card_count, int cards[], int weave_count, WeaveItem weaveitems[]) {
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
				weaveItem_item.setCenterCard(weaveitems[j].center_card);
				roomResponse.addWeaveItems(weaveItem_item);
			}
		}

		this.send_response_to_other(seat_index, roomResponse);

		// 手牌
		for (int j = 0; j < card_count; j++) {
			roomResponse.addCardData(cards[j]);
		}

		int ting_count = _playerStatus[seat_index]._hu_out_card_count;

		// 打出去可以听牌的个数
		roomResponse.setOutCardCount(ting_count);

		for (int i = 0; i < ting_count; i++) {
			int ting_card_cout = _playerStatus[seat_index]._hu_out_card_ting_count[i];
			roomResponse.addOutCardTingCount(ting_card_cout);
			if (_logic.is_magic_card(_playerStatus[seat_index]._hu_out_card_ting[i])) {
				roomResponse.addOutCardTing(_playerStatus[seat_index]._hu_out_card_ting[i] + GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI);
			} else {
				roomResponse.addOutCardTing(_playerStatus[seat_index]._hu_out_card_ting[i] + GameConstants.CARD_ESPECIAL_TYPE_TING);
			}

			Int32ArrayResponse.Builder int_array = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < ting_card_cout; j++) {
				if (this._logic.is_magic_card(_playerStatus[seat_index]._hu_out_cards[i][j])) {
					_playerStatus[seat_index]._hu_out_cards[i][j] += GameConstants.CARD_ESPECIAL_TYPE_HUN;
				}
				int_array.addItem(_playerStatus[seat_index]._hu_out_cards[i][j]);
			}
			roomResponse.addOutCardTingCards(int_array);
		}

		// 自己才有牌数据
		this.send_response_to_player(seat_index, roomResponse);

		GRR.add_room_response(roomResponse);

		return true;
	}

	/**
	 * 删除牌堆的牌 (吃碰杆的那张牌 从废弃牌堆上移除)
	 * 
	 * @param seat_index
	 * @param discard_index
	 * @return
	 */
	public boolean operate_remove_discard(int seat_index, int discard_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_REMOVE_DISCARD);
		roomResponse.setTarget(seat_index);
		roomResponse.setDiscardIndex(discard_index);

		GRR.add_room_response(roomResponse);
		this.send_response_to_room(roomResponse);

		return true;
	}

	/**
	 * 显示胡的牌
	 * 
	 * @param seat_index
	 * @param count
	 * @param cards
	 * @return
	 */
	public boolean operate_chi_hu_cards(int seat_index, int count, int cards[]) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_CHI_HU_CARDS);
		roomResponse.setTarget(seat_index);

		for (int i = 0; i < count; i++) {
			roomResponse.addChiHuCards(cards[i]);
		}

		this.send_response_to_player(seat_index, roomResponse);
		return true;
	}

	/**
	 * 显示胡的牌
	 * 
	 * @param seat_index
	 * @param count
	 * @param cards
	 * @return
	 */
	public boolean operate_chi_hu_henan_xc_cards(int seat_index, int count, int cards[]) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_CHI_HU_CARDS);
		roomResponse.setTarget(seat_index);
		int[] sendCards = new int[cards.length];
		if (has_rule(GameConstants.GAME_RULE_HENAN_DAI_HUN)) {
			for (int i = 0; i < count; i++) {
				sendCards[i] = cards[i];
				if (this._logic.is_magic_card(cards[i])) {
					sendCards[i] += GameConstants.CARD_ESPECIAL_TYPE_HUN;
				}
				roomResponse.addChiHuCards(sendCards[i]);
			}
		} else {
			for (int i = 0; i < count; i++) {
				roomResponse.addChiHuCards(cards[i]);
			}
		}
		this.send_response_to_player(seat_index, roomResponse);
		return true;
	}

	/***
	 * 刷新特殊描述
	 * 
	 * @param txt
	 * @param type
	 * @return
	 */
	public boolean operate_especial_txt(String txt, int type) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_GAME_ESPECIAL_TXT);
		roomResponse.setEspecialTxtType(type);
		roomResponse.setEspecialTxt(txt);
		this.send_response_to_room(roomResponse);
		return true;
	}

	/**
	 * 牌局中分数结算
	 * 
	 * @param seat_index
	 * @param score
	 * @return
	 */
	public boolean operate_player_score(int seat_index, float[] score) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_GAME_PLAYER_SCORE);
		roomResponse.setTarget(seat_index);
		this.load_common_status(roomResponse);
		this.load_player_info_data(roomResponse);
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			roomResponse.addScore(_player_result.game_score[i]);
			roomResponse.addOpereateScore(score[i]);
		}
		this.send_response_to_room(roomResponse);
		return true;
	}

	// 12更新玩家手牌数据
	// 13更新玩家牌数据(包含吃碰杠组合)
	// public boolean refresh_hand_cards(int seat_index, boolean send_weave) {
	// RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
	// if (send_weave == true) {
	// roomResponse.setType(13);// 13更新玩家牌数据(包含吃碰杠组合)
	// } else {
	// roomResponse.setType(12);// 12更新玩家手牌数据
	// }
	// roomResponse.setGameStatus(_game_status);
	// roomResponse.setOperatePlayer(seat_index);
	//
	// int hand_cards[] = new int[MJGameConstants.MAX_COUNT];
	// int hand_card_count =
	// _logic.switch_to_cards_data(GRR._cards_index[seat_index], hand_cards);
	//
	// //手牌数量
	// roomResponse.setCardCount(hand_card_count);
	//
	// // 手牌
	// for (int j = 0; j < hand_card_count; j++) {
	// roomResponse.addCardData(hand_cards[j]);
	// }
	//
	// //组合牌
	// if (send_weave == true) {
	// // 13更新玩家牌数据(包含吃碰杠组合)
	// for (int j = 0; j < GRR._weave_count[seat_index]; j++) {
	// WeaveItemResponse.Builder weaveItem_item =
	// WeaveItemResponse.newBuilder();
	// weaveItem_item.setProvidePlayer(GRR._weave_items[seat_index][j].provide_player);
	// weaveItem_item.setPublicCard(GRR._weave_items[seat_index][j].public_card);
	// weaveItem_item.setWeaveKind(GRR._weave_items[seat_index][j].weave_kind);
	// weaveItem_item.setCenterCard(GRR._weave_items[seat_index][j].center_card);
	// roomResponse.addWeaveItems(weaveItem_item);
	// }
	// }
	// // 自己才有牌数据
	// this.send_response_to_room(roomResponse);
	//
	// return true;
	// }

	// 14摊开玩家手上的牌
	// public boolean send_show_hand_card(int seat_index, int cards[]) {
	// RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
	// roomResponse.setType(14);
	// roomResponse.setCardCount(cards.length);
	// roomResponse.setOperatePlayer(seat_index);
	// for (int i = 0; i < cards.length; i++) {
	// roomResponse.addCardData(cards[i]);
	// }
	//
	// this.send_response_to_room(roomResponse);
	// return true;
	// }

	// public boolean refresh_discards(int seat_index) {
	// if (seat_index == MJGameConstants.INVALID_SEAT) {
	// return false;
	// }
	// RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
	// roomResponse.setType(MsgConstants.RESPONSE_REFRESH_DISCARD);
	// roomResponse.setOperatePlayer(seat_index);
	//
	// int l = GRR._discard_count[seat_index];
	// for (int i = 0; i < l; i++) {
	// roomResponse.addCardData(GRR._discard_cards[seat_index][i]);
	// }
	//
	// this.send_response_to_room(roomResponse);
	//
	// return true;
	// }

	public boolean handler_player_offline(Player player) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setGameStatus(_game_status);
		roomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);// 刷新玩家
		this.load_player_info_data(roomResponse);

		send_response_to_other(player.get_seat_index(), roomResponse);

		// 解散的时候默认同意解散
		// if(_gameRoomRecord!=null &&
		// (_gameRoomRecord.request_player_seat!=MJGameConstants.INVALID_SEAT)){
		// this.handler_release_room(player,
		// MJGameConstants.Release_Room_Type_AGREE);
		// }

		return true;
	}

	public boolean send_play_data(int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_RECONNECT_DATA);

		TableResponse.Builder tableResponse = TableResponse.newBuilder();

		load_room_info_data(roomResponse);
		load_player_info_data(roomResponse);

		this.load_common_status(roomResponse);
		// 游戏变量
		tableResponse.setBankerPlayer(GRR._banker_player);
		tableResponse.setCurrentPlayer(_current_player);
		tableResponse.setCellScore(0);

		// 状态变量
		tableResponse.setActionCard(_provide_card);
		// tableResponse.setActionMask((_response[seat_index] == false) ?
		// _player_action[seat_index] : MJGameConstants.WIK_NULL);

		// 历史记录
		tableResponse.setOutCardData(_out_card_data);
		tableResponse.setOutCardPlayer(_out_card_player);

		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			tableResponse.addTrustee(false);// 是否托管
			// 剩余牌数
			tableResponse.addDiscardCount(GRR._discard_count[i]);
			Int32ArrayResponse.Builder int_array = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < 55; j++) {
				int_array.addItem(GRR._discard_cards[i][j]);
			}
			tableResponse.addDiscardCards(int_array);

			// 组合扑克
			tableResponse.addWeaveCount(GRR._weave_count[i]);
			WeaveItemResponseArrayResponse.Builder weaveItem_array = WeaveItemResponseArrayResponse.newBuilder();
			for (int j = 0; j < GameConstants.MAX_WEAVE; j++) {
				WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
				weaveItem_item.setCenterCard(GRR._weave_items[i][j].center_card);
				weaveItem_item.setProvidePlayer(GRR._weave_items[i][j].provide_player);
				weaveItem_item.setPublicCard(GRR._weave_items[i][j].public_card);
				weaveItem_item.setWeaveKind(GRR._weave_items[i][j].weave_kind);
				weaveItem_array.addWeaveItem(weaveItem_item);
			}
			tableResponse.addWeaveItemArray(weaveItem_array);

			//
			tableResponse.addWinnerOrder(0);

			if (_status_send == true) {
				// 牌

				if (i == _current_player) {
					tableResponse.addCardCount(_logic.get_card_count_by_index(GRR._cards_index[i]) - 1);
				} else {
					tableResponse.addCardCount(_logic.get_card_count_by_index(GRR._cards_index[i]));
				}

			} else {
				// 牌
				tableResponse.addCardCount(_logic.get_card_count_by_index(GRR._cards_index[i]));

			}

		}

		// 数据
		tableResponse.setSendCardData(((_send_card_data != GameConstants.INVALID_VALUE) && (_provide_player == seat_index)) ? _send_card_data
				: GameConstants.INVALID_VALUE);
		int hand_cards[] = new int[GameConstants.MAX_COUNT];
		int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[seat_index], hand_cards);

		if (_status_send == true) {
			// 牌
			if (seat_index == _current_player) {
				_logic.remove_card_by_data(hand_cards, _send_card_data);
			}
		}

		for (int i = 0; i < GameConstants.MAX_COUNT; i++) {
			tableResponse.addCardsData(hand_cards[i]);
		}

		roomResponse.setTable(tableResponse);

		this.send_response_to_player(seat_index, roomResponse);

		if (_status_send == true) {
			// 牌
			this.operate_player_get_card(_current_player, 1, new int[] { _send_card_data }, seat_index);
		} else {
			if (_out_card_player != GameConstants.INVALID_SEAT && _out_card_data != GameConstants.INVALID_VALUE) {
				this.operate_out_card(_out_card_player, 1, new int[] { _out_card_data }, GameConstants.OUT_CARD_TYPE_MID, seat_index);
			} else if (_status_cs_gang == true) {
				this.operate_out_card(this._provide_player, 2, this._gang_card_data.get_cards(), GameConstants.OUT_CARD_TYPE_MID, seat_index);
			}
		}

		if (_playerStatus[seat_index].has_action()) {
			this.operate_player_action(seat_index, false);
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
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			int winner = -1;
			float s = -999999;
			for (int j = 0; j < GameConstants.GAME_PLAYER; j++) {
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
			for (int j = 0; j < GameConstants.GAME_PLAYER; j++) {
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
			player_result.addMingGangCount(_player_result.ming_gang_count[i]);
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
	 * 加载基础状态 癞子 定鬼 牌型状态 玩家状态
	 * 
	 * @param roomResponse
	 */
	public void load_common_status(RoomResponse.Builder roomResponse) {
		roomResponse.setCurrentPlayer(_current_player == GameConstants.INVALID_SEAT ? _resume_player : _current_player);
		if (GRR != null) {
			roomResponse.setLeftCardCount(GRR._left_card_count);
			for (int i = 0; i < GRR._especial_card_count; i++) {
				if (_logic.is_ding_gui_card(GRR._especial_show_cards[i])) {
					roomResponse.addEspecialShowCards(GRR._especial_show_cards[i] + GameConstants.CARD_ESPECIAL_TYPE_DING_GUI);
				} else if (_logic.is_lai_gen_card(GRR._especial_show_cards[i])) {
					roomResponse.addEspecialShowCards(GRR._especial_show_cards[i] + GameConstants.CARD_ESPECIAL_TYPE_LAI_GEN);
				} else if (_logic.is_ci_card(_logic.switch_to_card_index(GRR._especial_show_cards[i]))) {
					roomResponse.addEspecialShowCards(GRR._especial_show_cards[i] + GameConstants.CARD_ESPECIAL_TYPE_CI);
				} else if (_logic.is_ci_card(_logic.switch_to_card_index(GRR._especial_show_cards[i]))) {
					roomResponse.addEspecialShowCards(GRR._especial_show_cards[i] + GameConstants.CARD_ESPECIAL_TYPE_CI);
				} else if (_logic.is_wang_ba_card(GRR._especial_show_cards[i])) {
					roomResponse.addEspecialShowCards(GRR._especial_show_cards[i] + GameConstants.CARD_ESPECIAL_TYPE_WANG_BA);
				} else {
					roomResponse.addEspecialShowCards(GRR._especial_show_cards[i]);
				}

			}

			if (GRR._especial_txt != "") {
				roomResponse.setEspecialTxt(GRR._especial_txt);
				roomResponse.setEspecialTxtType(GRR._especial_txt_type);
			}
		}
		roomResponse.setGameStatus(_game_status);

		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			roomResponse.addCardStatus(_playerStatus[i]._card_status);
			roomResponse.addPlayerStatus(_playerStatus[i].get_status());
		}
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
			room_player.setQiang(_player_result.qiang[i]);
			room_player.setMoney(rplayer.getMoney());
			room_player.setGold(rplayer.getGold());
			room_player.setHasPiao(_player_result.haspiao[i]);
			room_player.setGvoiceStatus(rplayer.getGvoiceStatus());
			if (rplayer.locationInfor != null) {
				room_player.setLocationInfor(rplayer.locationInfor);
			}
			roomResponse.addPlayers(room_player);
		}
	}

	@Override
	public void clearHasPiao() {
		// int count = getTablePlayerNumber();
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			_player_result.haspiao[i] = 0;
		}
	}

	@Override
	public void progress_banker_select() {
		if (_cur_banker == GameConstants.INVALID_SEAT) {
			_cur_banker = 0;// 创建者的玩家为专家

		}

		if (is_sys()) {// 金币场 随机庄家
			Random random = new Random();//
			int rand = random.nextInt(6) + 1 + random.nextInt(6) + 1;
			_cur_banker = rand % GameConstants.GAME_PLAYER;//
		}
	}

	public boolean is_mj_type(int type) {
		return _game_type_index == type;
	}

	public boolean is_mj(int id) {
		return this.getGame_id() == id;
	}

	private void set_result_describe_he_nan_zhou_kou() {
		int chrTypes;
		long type = 0;
		for (int player = 0; player < GameConstants.GAME_PLAYER; player++) {
			StringBuilder gameDesc = new StringBuilder("");

			chrTypes = GRR._chi_hu_rights[player].type_count;

			for (int typeIndex = 0; typeIndex < chrTypes; typeIndex++) {
				type = GRR._chi_hu_rights[player].type_list[typeIndex];

				if (GRR._chi_hu_rights[player].is_valid()) {

					if (type == GameConstants.CHR_ZI_MO) {
						gameDesc.append(" 自摸");
					}

					if (type == GameConstants.CHR_SHU_FAN) {
						gameDesc.append(" 接炮");
					}

					if (type == GameConstants.CHR_HENAN_QIANG_GANG_HU) {
						gameDesc.append(" 抢杠胡");
					}

					if (type == GameConstants.CHR_HENAN_QI_XIAO_DUI) {
						gameDesc.append(" 七小对");
					}

					if (type == GameConstants.CHR_HENAN_GANG_KAI) {
						gameDesc.append(" 杠上开花");
					}

					if (type == GameConstants.CHR_HENAN_ZHOU_KOU_QING_YI_SE) {
						gameDesc.append(" 清一色");
					}
				} else if (type == GameConstants.CHR_FANG_PAO) {
					gameDesc.append(" 放炮");
				}
			}

			int jie_gang = 0, fang_gang = 0, ming_gang = 0, an_gang = 0;

			if (GRR != null) {
				for (int tmpPlayer = 0; tmpPlayer < GameConstants.GAME_PLAYER; tmpPlayer++) {
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
				gameDesc.append(" 暗杠X" + an_gang);
			}
			if (ming_gang > 0) {
				gameDesc.append(" 明杠X" + ming_gang);
			}
			if (fang_gang > 0) {
				gameDesc.append(" 放杠X" + fang_gang);
			}
			if (jie_gang > 0) {
				gameDesc.append(" 接杠X" + jie_gang);
			}

			GRR._result_des[player] = gameDesc.toString();
		}
	}

	private void set_result_describe_henan_lh() {
		int chrTypes;
		long type = 0;
		for (int player = 0; player < GameConstants.GAME_PLAYER; player++) {
			StringBuilder gameDesc = new StringBuilder("");

			chrTypes = GRR._chi_hu_rights[player].type_count;

			for (int typeIndex = 0; typeIndex < chrTypes; typeIndex++) {
				type = GRR._chi_hu_rights[player].type_list[typeIndex];

				if (GRR._chi_hu_rights[player].is_valid()) {

					if (type == GameConstants.CHR_ZI_MO) {
						gameDesc.append(" 自摸");
					}

					if (type == GameConstants.CHR_SHU_FAN) {
						gameDesc.append(" 接炮");
					}

					if (type == GameConstants.CHR_HENAN_QIANG_GANG_HU) {
						gameDesc.append(" 抢杠胡");
					}

					if (type == GameConstants.CHR_HENAN_QI_XIAO_DUI) {
						gameDesc.append(" 七小对");
					}

					if (type == GameConstants.CHR_HENAN_GANG_KAI) {
						gameDesc.append(" 杠上开花");
					}

					if (type == GameConstants.CHR_HENAN_ZHOU_KOU_QING_YI_SE) {
						gameDesc.append(" 清一色");
					}
				} else if (type == GameConstants.CHR_FANG_PAO) {
					gameDesc.append(" 放炮");
				}
			}

			int jie_gang = 0, fang_gang = 0, ming_gang = 0, an_gang = 0;

			if (GRR != null) {
				for (int tmpPlayer = 0; tmpPlayer < GameConstants.GAME_PLAYER; tmpPlayer++) {
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
				gameDesc.append(" 暗杠X" + an_gang);
			}
			if (ming_gang > 0) {
				gameDesc.append(" 明杠X" + ming_gang);
			}
			if (fang_gang > 0) {
				gameDesc.append(" 放杠X" + fang_gang);
			}
			if (jie_gang > 0) {
				gameDesc.append(" 接杠X" + jie_gang);
			}

			GRR._result_des[player] = gameDesc.toString();
		}
	}

	private void set_result_descibe_zz() {
		int l;
		long type = 0;
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			String des = "";

			l = GRR._chi_hu_rights[i].type_count;
			for (int j = 0; j < l; j++) {
				type = GRR._chi_hu_rights[i].type_list[j];
				if (GRR._chi_hu_rights[i].is_valid()) {
					if (type == GameConstants.CHR_TONG_PAO) {
						des += " 通炮";
						if (has_rule(GameConstants.GAME_RULE_HUNAN_ZHUANG_XIAN) && i == GRR._banker_player) {
							des += " 庄家加底";
						}
					}
					if (type == GameConstants.CHR_ZI_MO) {
						des += " 自摸";
						if (GRR._count_pick_niao > 0) {
							des += " 中鸟X" + GRR._count_pick_niao;
						}
					}
					if (type == GameConstants.CHR_SHU_FAN) {
						des += " 接炮";
						if (GRR._count_pick_niao > 0) {
							des += " 中鸟X" + GRR._count_pick_niao;
						}
						if (has_rule(GameConstants.GAME_RULE_HUNAN_ZHUANG_XIAN) && i == GRR._banker_player) {
							des += " 庄家加底";
						}
					}
					if (type == GameConstants.CHR_HUNAN_QIANG_GANG_HU) {
						des += " 抢杠胡";
					}
				} else {
					if (type == GameConstants.CHR_FANG_PAO) {
						des += " 放炮";
						if (has_rule(GameConstants.GAME_RULE_HUNAN_ZHUANG_XIAN) && i == GRR._banker_player) {
							des += " 庄家加底";
						}
					}
				}
			}
			int jie_gang = 0, fang_gang = 0, ming_gang = 0, an_gang = 0;
			if (GRR != null) {
				for (int p = 0; p < GameConstants.GAME_PLAYER; p++) {
					for (int w = 0; w < GRR._weave_count[p]; w++) {
						if (GRR._weave_items[p][w].weave_kind != GameConstants.WIK_GANG) {
							continue;
						}
						if (p == i) {// 自己
							// 接杠
							if (GRR._weave_items[p][w].provide_player != p) {
								jie_gang++;
							} else {
								if (GRR._weave_items[p][w].public_card == 1) {// 明杠
									ming_gang++;
								} else {
									an_gang++;
								}
							}
						} else {
							// 放杠
							if (GRR._weave_items[p][w].provide_player == i) {
								fang_gang++;
							}
						}
					}
				}
			}

			if (an_gang > 0) {
				des += " 暗杠X" + an_gang;
			}
			if (ming_gang > 0) {
				des += " 明杠X" + ming_gang;
			}
			if (fang_gang > 0) {
				des += " 放杠X" + fang_gang;
			}
			if (jie_gang > 0) {
				des += " 接杠X" + jie_gang;
			}

			GRR._result_des[i] = des;
		}
	}

	private void set_result_descibe_hz() {
		int l;
		long type = 0;
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			String des = "";

			l = GRR._chi_hu_rights[i].type_count;
			for (int j = 0; j < l; j++) {
				type = GRR._chi_hu_rights[i].type_list[j];
				if (GRR._chi_hu_rights[i].is_valid()) {
					if (type == GameConstants.CHR_TONG_PAO) {
						des += " 通炮";
					}
					if (type == GameConstants.CHR_ZI_MO) {
						if (!(GRR._chi_hu_rights[i].opr_and(GameConstants.CHR_HUNAN_HZ_QISHOU_HU).is_empty())) {
							des += " 起手自摸";
						} else {
							des += " 自摸";
						}

						if (GRR._count_pick_niao > 0) {
							des += " 中鸟X" + GRR._count_pick_niao;
						}
					}
					if (type == GameConstants.CHR_SHU_FAN) {
						des += " 接炮";
						if (GRR._count_pick_niao > 0) {
							des += " 中鸟X" + GRR._count_pick_niao;
						}
					}
					if (type == GameConstants.CHR_HUNAN_QIANG_GANG_HU) {
						des += " 抢杠胡";
					}
				} else {
					if (type == GameConstants.CHR_FANG_PAO) {
						des += " 放炮";
					}
				}
			}
			int jie_gang = 0, fang_gang = 0, ming_gang = 0, an_gang = 0;
			if (GRR != null) {
				for (int p = 0; p < GameConstants.GAME_PLAYER; p++) {
					for (int w = 0; w < GRR._weave_count[p]; w++) {
						if (GRR._weave_items[p][w].weave_kind != GameConstants.WIK_GANG) {
							continue;
						}
						if (p == i) {// 自己
							// 接杠
							if (GRR._weave_items[p][w].provide_player != p) {
								jie_gang++;
							} else {
								if (GRR._weave_items[p][w].public_card == 1) {// 明杠
									ming_gang++;
								} else {
									an_gang++;
								}
							}
						} else {
							// 放杠
							if (GRR._weave_items[p][w].provide_player == i) {
								fang_gang++;
							}
						}
					}
				}
			}

			if (an_gang > 0) {
				des += " 暗杠X" + an_gang;
			}
			if (ming_gang > 0) {
				des += " 明杠X" + ming_gang;
			}
			if (fang_gang > 0) {
				des += " 放杠X" + fang_gang;
			}
			if (jie_gang > 0) {
				des += " 接杠X" + jie_gang;
			}

			GRR._result_des[i] = des;
		}
	}

	private void set_result_descibe_lxcg() {
		int l;
		long type = 0;
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			String des = "";
			l = GRR._chi_hu_rights[i].type_count;
			for (int j = 0; j < l; j++) {
				type = GRR._chi_hu_rights[i].type_list[j];
				if (GRR._chi_hu_rights[i].is_valid()) {
					if (type == GameConstants.CHR_TONG_PAO) {
						des += " 通炮";
					}
					if (type == GameConstants.CHR_ZI_MO) {
						if (!(GRR._chi_hu_rights[i].opr_and(GameConstants.CHR_HUNAN_DADOU).is_empty())) {
							des += " 大刀";
						}
						if (!(GRR._chi_hu_rights[i].opr_and(GameConstants.CHR_HUNAN_XIADOU).is_empty())) {
							des += " 小刀";
						}
						if (GRR._count_pick_niao > 0) {
							des += " 中鸟X" + GRR._count_pick_niao;
						}
					}
					if (type == GameConstants.CHR_SHU_FAN) {
						des += " 接炮";
						if (GRR._count_pick_niao > 0) {
							des += " 中鸟X" + GRR._count_pick_niao;
						}
					}
				} else {
					if (type == GameConstants.CHR_FANG_PAO) {
						des += " 放炮";
					}
				}
			}
			GRR._result_des[i] = des;
		}
	}

	private void set_result_descibe_hnhz() {
		int l;
		long type = 0;
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			String des = "";

			l = GRR._chi_hu_rights[i].type_count;
			for (int j = 0; j < l; j++) {
				type = GRR._chi_hu_rights[i].type_list[j];
				if (GRR._chi_hu_rights[i].is_valid()) {
					if (type == GameConstants.CHR_TONG_PAO) {
						des += " 通炮";
					}
					if (type == GameConstants.CHR_ZI_MO) {
						if (!(GRR._chi_hu_rights[i].opr_and(GameConstants.CHR_HENAN_HZ_QISHOU_HU).is_empty())) {
							des += " 起手自摸";
						} else {
							des += " 自摸";
						}

						if (GRR._count_pick_niao > 0) {
							des += " 中鸟X" + GRR._count_pick_niao;
						}
					}
					if (type == GameConstants.CHR_SHU_FAN) {
						des += " 接炮";
						if (GRR._count_pick_niao > 0) {
							des += " 中鸟X" + GRR._count_pick_niao;
						}
					}
					if (type == GameConstants.CHR_HENAN_QIANG_GANG_HU) {
						des += " 抢杠胡";
					}
				} else {
					if (type == GameConstants.CHR_FANG_PAO) {
						des += " 放炮";
					}
				}
			}
			int jie_gang = 0, fang_gang = 0, ming_gang = 0, an_gang = 0;
			if (GRR != null) {
				for (int p = 0; p < GameConstants.GAME_PLAYER; p++) {
					for (int w = 0; w < GRR._weave_count[p]; w++) {
						if (GRR._weave_items[p][w].weave_kind != GameConstants.WIK_GANG) {
							continue;
						}
						if (p == i) {// 自己
							// 接杠
							if (GRR._weave_items[p][w].provide_player != p) {
								jie_gang++;
							} else {
								if (GRR._weave_items[p][w].public_card == 1) {// 明杠
									ming_gang++;
								} else {
									an_gang++;
								}
							}
						} else {
							// 放杠
							if (GRR._weave_items[p][w].provide_player == i) {
								fang_gang++;
							}
						}
					}
				}
			}

			if (an_gang > 0) {
				des += " 暗杠X" + an_gang;
			}
			if (ming_gang > 0) {
				des += " 明杠X" + ming_gang;
			}
			if (fang_gang > 0) {
				des += " 放杠X" + fang_gang;
			}
			if (jie_gang > 0) {
				des += " 接杠X" + jie_gang;
			}

			GRR._result_des[i] = des;
		}
	}

	@Override
	protected void countCardType(ChiHuRight chiHuRight, int seat_index) {
		try {
			int wFanShu = 0;
			if (getGame_id() == 1) {
				if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_PENGPENG_HU)).is_empty()) {
					MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.mjpph, "", _game_type_index, 0l,
							this.getRoom_id());
				}

				if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_JIANGJIANG_HU)).is_empty()) {
					MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.jjh, "", _game_type_index, 0l,
							this.getRoom_id());
				}

				if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_QING_YI_SE)).is_empty()) {
					MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.qingyise, "", _game_type_index, 0l,
							this.getRoom_id());
				}

				if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_HAI_DI_LAO)).is_empty()) {
					MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.cshaidilao, "", _game_type_index, 0l,
							this.getRoom_id());
				}

				if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_HAI_DI_PAO)).is_empty()) {
					MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.cshaidipao, "", _game_type_index, 0l,
							this.getRoom_id());
				}

				if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_QI_XIAO_DUI)).is_empty()) {
					MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.qidui, "", _game_type_index, 0l,
							this.getRoom_id());
				}

				if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_HAOHUA_QI_XIAO_DUI)).is_empty()) {
					MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.haohuaqidui, "", _game_type_index, 0l,
							this.getRoom_id());
				}
				if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_GANG_KAI)).is_empty()) {
					MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.mjgangshanghua, "", _game_type_index, 0l,
							this.getRoom_id());
				}
				if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_QIANG_GANG_HU)).is_empty()) {
					MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.qiangganghu, "", _game_type_index, 0l,
							this.getRoom_id());
				}

				if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_GANG_SHANG_PAO)).is_empty()) {
					MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.mjgangshangpao, "", _game_type_index, 0l,
							this.getRoom_id());
				}

				if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_QUAN_QIU_REN)).is_empty()) {
					MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.quanqiuren, "", _game_type_index, 0l,
							this.getRoom_id());
				}

				if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_SHUANG_HAO_HUA_QI_XIAO_DUI)).is_empty()) {
					MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.shaohuaqidui, "", _game_type_index, 0l,
							this.getRoom_id());
				}

				if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_SHUANG_GANG_KAI)).is_empty()) {
					MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.smjgangshanghua, "", _game_type_index, 0l,
							this.getRoom_id());
				}

				if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_SHUANG_GANG_SHANG_PAO)).is_empty()) {
					MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.smjgangshangpao, "", _game_type_index, 0l,
							this.getRoom_id());
				}

				// 小胡
				if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_XIAO_DA_SI_XI)).is_empty())
					wFanShu += 1;
				if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_XIAO_BAN_BAN_HU)).is_empty())// 8000
					wFanShu += 1;
				if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_XIAO_LIU_LIU_SHUN)).is_empty())
					MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.liuliushun, "", _game_type_index, 0l,
							this.getRoom_id());
				if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_XIAO_QUE_YI_SE)).is_empty())// 10000
					MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.qingyise, "", _game_type_index, 0l,
							this.getRoom_id());

			}

			if (getGame_id() == 2) {
				if (!(chiHuRight.opr_and(GameConstants.CHR_HUBEI_HEI_MO)).is_empty()) {
					MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.heimo, "", _game_type_index, 0l,
							this.getRoom_id());
				}
				if (!(chiHuRight.opr_and(GameConstants.CHR_HUBEI_RUAN_MO)).is_empty()) {
					MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.ruanmo, "", _game_type_index, 0l,
							this.getRoom_id());
				}
				if (!(chiHuRight.opr_and(GameConstants.CHR_HUBEI_ZHUO_CHONG)).is_empty()) {
					MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.zhuotong, "", _game_type_index, 0l,
							this.getRoom_id());
				}
				if (!(chiHuRight.opr_and(GameConstants.CHR_HUBEI_RE_CHONG)).is_empty()) {
					MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.rechong, "", _game_type_index, 0l,
							this.getRoom_id());
				}
			}

			if (getGame_id() == 3) {
				if (!(chiHuRight.opr_and(GameConstants.CHR_HENAN_QISHOU_HU)).is_empty()) {
					MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.sihun, "", _game_type_index, 0l,
							this.getRoom_id());
				}
				if (!(chiHuRight.opr_and(GameConstants.CHR_HENAN_QIANG_GANG_HU)).is_empty()) {
					MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.henangang, "", _game_type_index, 0l,
							this.getRoom_id());
				}

				if (!(chiHuRight.opr_and(GameConstants.CHR_HENAN_QI_XIAO_DUI)).is_empty()
						|| !(chiHuRight.opr_and(GameConstants.CHR_HENAN_HH_QI_XIAO_DUI)).is_empty()) {
					MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.henanqidui, "", _game_type_index, 0l,
							this.getRoom_id());
				}

				if (!(chiHuRight.opr_and(GameConstants.CHR_HENAN_HH_QI_XIAO_DUI)).is_empty()) {
					MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.henanqiduihaohua, "", _game_type_index, 0l,
							this.getRoom_id());
				}

				if (!(chiHuRight.opr_and(GameConstants.CHR_HENAN_GANG_KAI)).is_empty()) {
					MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.henankaihua, "", _game_type_index, 0l,
							this.getRoom_id());
				}
				if (!(chiHuRight.opr_and(GameConstants.CHR_HENAN_HZ_QISHOU_HU)).is_empty()) {
					MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.henan4hong, "", _game_type_index, 0l,
							this.getRoom_id());
				}

			}

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private void set_result_describe_sg() {
		int l;
		long type = 0;
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			String des = "";

			l = GRR._chi_hu_rights[i].type_count;
			for (int j = 0; j < l; j++) {
				type = GRR._chi_hu_rights[i].type_list[j];
				if (GRR._chi_hu_rights[i].is_valid()) {
					if (type == GameConstants.CHR_TONG_PAO) {
						des += " 通炮";
					}
					if (type == GameConstants.CHR_ZI_MO) {
						des += " 自摸";
						if (GRR._count_pick_niao > 0) {
							des += " 中鸟X" + GRR._count_pick_niao;
						}
					}
					if (type == GameConstants.CHR_SHU_FAN) {
						des += " 接炮";
						if (GRR._count_pick_niao > 0) {
							des += " 中鸟X" + GRR._count_pick_niao;
						}
					}
					if (type == GameConstants.CHR_HUNAN_QIANG_GANG_HU) {
						des += " 抢杠胡";
					}
				} else {
					if (type == GameConstants.CHR_FANG_PAO) {
						des += " 放炮";
					}
				}
			}
			int jie_gang = 0, fang_gang = 0, ming_gang = 0, an_gang = 0;
			if (GRR != null) {
				for (int p = 0; p < GameConstants.GAME_PLAYER; p++) {
					for (int w = 0; w < GRR._weave_count[p]; w++) {
						if (GRR._weave_items[p][w].weave_kind != GameConstants.WIK_GANG) {
							continue;
						}
						if (p == i) {// 自己
							// 接杠
							if (GRR._weave_items[p][w].provide_player != p) {
								jie_gang++;
							} else {
								if (GRR._weave_items[p][w].public_card == 1) {// 明杠
									ming_gang++;
								} else {
									an_gang++;
								}
							}
						} else {
							// 放杠
							if (GRR._weave_items[p][w].provide_player == i) {
								fang_gang++;
							}
						}
					}
				}
			}

			if (an_gang > 0) {
				des += " 暗杠X" + an_gang;
			}
			if (ming_gang > 0) {
				des += " 明杠X" + ming_gang;
			}
			if (fang_gang > 0) {
				des += " 放杠X" + fang_gang;
			}
			if (jie_gang > 0) {
				des += " 接杠X" + jie_gang;
			}

			GRR._result_des[i] = des;
		}
	}

	private void set_result_describe_xthh() {
		int l;
		long type = 0;
		// int hjh = this.hei_jia_hei();
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			String des = "";

			l = GRR._chi_hu_rights[i].type_count;
			for (int j = 0; j < l; j++) {
				type = GRR._chi_hu_rights[i].type_list[j];
				if (GRR._chi_hu_rights[i].is_valid()) {
					if (type == GameConstants.CHR_TONG_PAO) {
						des += " 通炮";
					} else if (type == GameConstants.CHR_HUBEI_HEI_MO) {
						des += " 黑摸";
					} else if (type == GameConstants.CHR_HUBEI_RUAN_MO) {
						des += " 软摸";
					} else if (type == GameConstants.CHR_HUBEI_ZHUO_CHONG) {
						des += " 捉铳";
					} else if (type == GameConstants.CHR_HUBEI_RE_CHONG) {
						des += " 热铳";
					} else if (type == GameConstants.CHR_HUBEI_QIANG_GANG_HU) {
						des += " 抢杠胡";
					}
					if (type == GameConstants.CHR_SHU_FAN) {
						des += " 放铳";
					}
				} else {
					if (type == GameConstants.CHR_FANG_PAO) {
						des += " 放铳";
					}
				}
			}
			int meng_xiao = 0, dian_xiao = 0, hui_tou_xiao = 0, xiao_chao_tian = 0, da_chao_tian = 0, fang_xiao = 0;
			if (GRR != null) {
				for (int p = 0; p < GameConstants.GAME_PLAYER; p++) {
					for (int w = 0; w < GRR._weave_count[p]; w++) {
						if (p == i) {// 自己
							if (GRR._weave_items[p][w].weave_kind == GameConstants.WIK_MENG_XIAO) {
								meng_xiao++;
							} else if (GRR._weave_items[p][w].weave_kind == GameConstants.WIK_DIAN_XIAO) {
								dian_xiao++;
							} else if (GRR._weave_items[p][w].weave_kind == GameConstants.WIK_HUI_TOU_XIAO) {
								hui_tou_xiao++;
							} else if (GRR._weave_items[p][w].weave_kind == GameConstants.WIK_XIAO_CHAO_TIAN) {
								xiao_chao_tian++;
							} else if (GRR._weave_items[p][w].weave_kind == GameConstants.WIK_DA_CHAO_TIAN) {
								da_chao_tian++;
							} else {
							}
						} else {
							// 放杠笑
							if ((GRR._weave_items[p][w].weave_kind == GameConstants.WIK_DIAN_XIAO
									|| GRR._weave_items[p][w].weave_kind == GameConstants.WIK_XIAO_CHAO_TIAN)
									&& GRR._weave_items[p][w].provide_player == i) {
								fang_xiao++;
							}

						}

					}
				}
			}

			if (meng_xiao > 0) {
				des += " 闷笑X" + meng_xiao;
			}
			if (dian_xiao > 0) {
				des += " 点笑X" + dian_xiao;
			}
			if (hui_tou_xiao > 0) {
				des += " 回头笑X" + hui_tou_xiao;
			}
			if (xiao_chao_tian > 0) {
				des += " 小朝天";
			}
			if (da_chao_tian > 0) {
				des += " 大朝天";
			}
			if (fang_xiao > 0) {
				des += " 放笑X" + fang_xiao;
			}

			// if(hjh == i){
			// des+=" 黑加黑";
			// }

			int piao_lai_cout = 0;
			for (int j = 0; j < GRR._piao_lai_count; j++) {
				if (GRR._piao_lai_seat[j] == i) {
					piao_lai_cout++;
				}
			}
			if (piao_lai_cout > 0) {
				des += " 飘赖*" + piao_lai_cout;
			}

			GRR._result_des[i] = des;
		}
	}

	/**
	 * 安阳麻将结算描述
	 */
	private void set_result_describe_ay() {
		int l;
		long type = 0;
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			String des = "";

			l = GRR._chi_hu_rights[i].type_count;
			for (int j = 0; j < l; j++) {
				type = GRR._chi_hu_rights[i].type_list[j];
				if (GRR._chi_hu_rights[i].is_valid()) {
					if (type == GameConstants.CHR_ZI_MO) {
						des += " 自摸";
					}
					if (type == GameConstants.CHR_SHU_FAN) {
						des += " 接炮";
					}
					if (type == GameConstants.CHR_HENAN_QIANG_GANG_HU) {
						des += " 抢杠胡";
					}

					if (type == GameConstants.CHR_HENAN_DAN_DIAO) {
						des += " 单吊";
					} else if (type == GameConstants.CHR_HENAN_KA_ZHANG) {
						des += " 卡张";
					} else if (type == GameConstants.CHR_HENAN_BIAN_ZHANG) {
						des += " 边张";
					}
				} else {
					if (type == GameConstants.CHR_FANG_PAO) {
						des += " 放炮";
					}
				}
			}
			if (GRR._chi_hu_rights[i].hua_count > 0) {
				des += " 财神";
			}
			int jie_gang = 0, fang_gang = 0, ming_gang = 0, an_gang = 0;
			if (GRR != null) {
				for (int p = 0; p < GameConstants.GAME_PLAYER; p++) {
					for (int w = 0; w < GRR._weave_count[p]; w++) {
						if (GRR._weave_items[p][w].weave_kind != GameConstants.WIK_GANG) {
							continue;
						}
						if (p == i) {// 自己
							// 接杠
							if (GRR._weave_items[p][w].provide_player != p) {
								jie_gang++;
							} else {
								if (GRR._weave_items[p][w].public_card == 1) {// 明杠
									ming_gang++;
								} else {
									an_gang++;
								}
							}
						} else {
							// 放杠
							if (GRR._weave_items[p][w].provide_player == i) {
								fang_gang++;
							}
						}
					}
				}
			}

			if (an_gang > 0) {
				des += " 暗杠X" + an_gang;
			}
			if (ming_gang > 0) {
				des += " 明杠X" + ming_gang;
			}
			if (fang_gang > 0) {
				des += " 放杠X" + fang_gang;
			}
			if (jie_gang > 0) {
				des += " 接杠X" + jie_gang;
			}

			GRR._result_des[i] = des;
		}
	}

	/**
	 * 林州麻将结算描述
	 */
	private void set_result_describe_lz() {
		int l;
		long type = 0;
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			String des = "";

			l = GRR._chi_hu_rights[i].type_count;
			for (int j = 0; j < l; j++) {
				type = GRR._chi_hu_rights[i].type_list[j];
				if (GRR._chi_hu_rights[i].is_valid()) {
					if (type == GameConstants.CHR_ZI_MO) {
						des += " 自摸";
					}
					if (type == GameConstants.CHR_SHU_FAN) {
						des += " 接炮";
					}
					if (type == GameConstants.CHR_HENAN_QIANG_GANG_HU) {
						des += " 抢杠胡";
					}
					if (type == GameConstants.CHR_HENAN_TIAN_HU) {
						des += " 天胡";
					}
					if (type == GameConstants.CHR_HENAN_QI_XIAO_DUI) {
						des += " 七小对";
					}
					if (type == GameConstants.CHR_HENAN_HH_QI_XIAO_DUI) {
						des += " 豪华七小对";
					}
				} else {
					if (type == GameConstants.CHR_FANG_PAO) {
						des += " 放炮";
					}
				}
			}
			int jie_gang = 0, fang_gang = 0, ming_gang = 0, an_gang = 0;
			if (GRR != null) {
				for (int p = 0; p < GameConstants.GAME_PLAYER; p++) {
					for (int w = 0; w < GRR._weave_count[p]; w++) {
						if (GRR._weave_items[p][w].weave_kind != GameConstants.WIK_GANG) {
							continue;
						}
						if (p == i) {// 自己
							// 接杠
							if (GRR._weave_items[p][w].provide_player != p) {
								jie_gang++;
							} else {
								if (GRR._weave_items[p][w].public_card == 1) {// 明杠
									ming_gang++;
								} else {
									an_gang++;
								}
							}
						} else {
							// 放杠
							if (GRR._weave_items[p][w].provide_player == i) {
								fang_gang++;
							}
						}
					}
				}
			}

			if (an_gang > 0) {
				des += " 暗杠X" + an_gang;
			}
			if (ming_gang > 0) {
				des += " 明杠X" + ming_gang;
			}
			if (fang_gang > 0) {
				des += " 放杠X" + fang_gang;
			}
			if (jie_gang > 0) {
				des += " 接杠X" + jie_gang;
			}

			GRR._result_des[i] = des;
		}
	}

	/**
	 * 河南麻将结算描述
	 */
	private void set_result_describe_henan() {
		// 杠牌，每个人的分数
		int lGangScore[] = new int[GameConstants.GAME_PLAYER];
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {

			for (int j = 0; j < GRR._gang_score[i].gang_count; j++) {
				for (int k = 0; k < GameConstants.GAME_PLAYER; k++) {
					lGangScore[k] += GRR._gang_score[i].scores[j][k];// 杠牌，每个人的分数
				}
			}
		}

		int l;
		long type = 0;
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			String des = "";

			if (has_rule(GameConstants.GAME_RULE_HENAN_JIA_DI) && i == GRR._banker_player) {
				des += " 庄家加底";
			}

			// if(has_rule(MJGameConstants.GAME_TYPE_HENAN_GANG_PAO) &&
			// i==GRR._banker_player) {
			// des+=" 杠跑";
			// }

			l = GRR._chi_hu_rights[i].type_count;
			for (int j = 0; j < l; j++) {
				type = GRR._chi_hu_rights[i].type_list[j];
				if (GRR._chi_hu_rights[i].is_valid()) {
					if (type == GameConstants.CHR_ZI_MO) {
						des += " 自摸";
					}

					if (type == GameConstants.CHR_HENAN_QISHOU_HU) {
						if (is_mj_type(GameConstants.GAME_TYPE_HENAN_NY)) {
							des += " 4金加倍";
						} else {
							des += " 4混加倍";
						}

					}

					if (type == GameConstants.CHR_SHU_FAN) {
						des += " 接炮";
					}
					if (type == GameConstants.CHR_HENAN_QIANG_GANG_HU) {
						des += " 抢杠胡";
					}
					if (type == GameConstants.CHR_HENAN_TIAN_HU) {
						des += " 天胡";
					}
					if (type == GameConstants.CHR_HENAN_QI_XIAO_DUI) {
						if (has_rule(GameConstants.GAME_RULE_HENAN_QIDUI_DOUBLE)) {
							des += " 七小对加倍";
						} else {
							des += " 七小对";
						}
					}
					if (type == GameConstants.CHR_HENAN_HH_QI_XIAO_DUI) {
						if (has_rule(GameConstants.GAME_RULE_HENAN_HAOQI)) {
							des += " 豪七四倍";
						} else {
							des += " 豪华七小对";
						}
					}
					if (type == GameConstants.CHR_HENAN_GANG_KAI) {
						if (has_rule(GameConstants.GAME_RULE_HENAN_GANGHUA_DOUBLE)) {
							des += " 杠上花加倍";
						} else {
							des += " 杠上花";
						}

					}
				} else {
					if (type == GameConstants.CHR_FANG_PAO) {
						des += " 放炮";
					}
				}
			}
			int jie_gang = 0, fang_gang = 0, ming_gang = 0, an_gang = 0;
			if (GRR != null) {
				for (int p = 0; p < GameConstants.GAME_PLAYER; p++) {
					for (int w = 0; w < GRR._weave_count[p]; w++) {
						if (GRR._weave_items[p][w].weave_kind != GameConstants.WIK_GANG) {
							continue;
						}
						if (p == i) {// 自己
							// 接杠
							if (GRR._weave_items[p][w].provide_player != p) {
								jie_gang++;
							} else {
								if (GRR._weave_items[p][w].public_card == 1) {// 明杠
									ming_gang++;
								} else {
									an_gang++;
								}
							}
						} else {
							// 放杠
							if (GRR._weave_items[p][w].provide_player == i) {
								fang_gang++;
							}
						}
					}
				}
			}

			if (an_gang > 0) {
				des += " 暗杠X" + an_gang;
			}
			if (ming_gang > 0) {
				des += " 明杠X" + ming_gang;
			}
			if (fang_gang > 0) {
				des += " 放杠X" + fang_gang;
			}
			if (jie_gang > 0) {
				des += " 接杠X" + jie_gang;
			}
			if (lGangScore[i] != 0 && GRR._end_type == GameConstants.Game_End_NORMAL) {
				des += " 杠总分(" + lGangScore[i] + ")";
			}
			GRR._result_des[i] = des;
		}
	}

	/**
	 * 河南洛阳杠次麻将结算描述
	 */
	private void set_result_describe_henan_lygc() {
		// 杠牌，每个人的分数
		int lGangScore[] = new int[GameConstants.GAME_PLAYER];
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {

			for (int j = 0; j < GRR._gang_score[i].gang_count; j++) {
				for (int k = 0; k < GameConstants.GAME_PLAYER; k++) {
					lGangScore[k] += GRR._gang_score[i].scores[j][k];// 杠牌，每个人的分数
				}
			}
		}

		int l;
		long type = 0;
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			String des = "";

			if (has_rule(GameConstants.GAME_RULE_HENAN_JIA_DI) && i == GRR._banker_player) {
				des += " 庄家翻倍";
			}

			// if(has_rule(MJGameConstants.GAME_TYPE_HENAN_GANG_PAO) &&
			// i==GRR._banker_player) {
			// des+=" 杠跑";
			// }

			l = GRR._chi_hu_rights[i].type_count;
			for (int j = 0; j < l; j++) {
				type = GRR._chi_hu_rights[i].type_list[j];
				if (GRR._chi_hu_rights[i].is_valid()) {
					if (type == GameConstants.CHR_ZI_MO) {
						des += " 自摸";
					}

					if (type == GameConstants.CHR_HENAN_PI_CI) {
						des += " 皮次";
					}

					if (type == GameConstants.CHR_HENAN_GANG_CI) {
						if (type == GameConstants.CHR_HENAN_BAO_CI_START) {
							des += " 包次";
						} else {
							des += " 杠次";
						}
					}

					if (type == GameConstants.CHR_SHU_FAN) {
						des += " 接炮";
					}
					if (type == GameConstants.CHR_HENAN_QIANG_GANG_HU) {
						des += " 抢杠胡";
					}
					if (type == GameConstants.CHR_HENAN_TIAN_HU) {
						des += " 天胡";
					}
					if (type == GameConstants.CHR_HENAN_QI_XIAO_DUI) {
						if (has_rule(GameConstants.GAME_RULE_HENAN_QIDUI_DOUBLE)) {
							des += " 七小对加倍";
						} else {
							des += " 七小对";
						}
					}
					if (type == GameConstants.CHR_HENAN_HH_QI_XIAO_DUI) {
						if (has_rule(GameConstants.GAME_RULE_HENAN_HAOQI)) {
							des += " 豪七四倍";
						} else {
							des += " 豪华七小对";
						}
					}
					/*
					 * if (type == GameConstants.CHR_HENAN_GANG_KAI) { if
					 * (has_rule(GameConstants.GAME_RULE_HENAN_GANGHUA_DOUBLE))
					 * { des += " 杠上花加倍"; } else { des += " 杠上花"; }
					 * 
					 * }
					 */
				} else {
					if (type == GameConstants.CHR_FANG_PAO) {
						des += " 放炮";
					}
				}
			}
			int jie_gang = 0, fang_gang = 0, ming_gang = 0, an_gang = 0;
			if (GRR != null) {
				for (int p = 0; p < GameConstants.GAME_PLAYER; p++) {
					for (int w = 0; w < GRR._weave_count[p]; w++) {
						if (GRR._weave_items[p][w].weave_kind != GameConstants.WIK_GANG) {
							continue;
						}
						if (p == i) {// 自己
							// 接杠
							if (GRR._weave_items[p][w].provide_player != p) {
								jie_gang++;
							} else {
								if (GRR._weave_items[p][w].public_card == 1) {// 明杠
									ming_gang++;
								} else {
									an_gang++;
								}
							}
						} else {
							// 放杠
							if (GRR._weave_items[p][w].provide_player == i) {
								fang_gang++;
							}
						}
					}
				}
			}

			if (an_gang > 0) {
				des += " 暗杠X" + an_gang;
			}
			if (ming_gang > 0) {
				des += " 明杠X" + ming_gang;
			}
			if (fang_gang > 0) {
				des += " 放杠X" + fang_gang;
			}
			if (jie_gang > 0) {
				des += " 接杠X" + jie_gang;
			}
			if (lGangScore[i] != 0 && GRR._end_type == GameConstants.Game_End_NORMAL) {
				des += " 杠总分(" + lGangScore[i] + ")";
			}
			GRR._result_des[i] = des;
		}
	}

	// 转转麻将结束描述
	@Override
	protected void set_result_describe() {
		if (this.is_mj_type(GameConstants.GAME_TYPE_ZZ) || this.is_mj_type(GameConstants.GAME_TYPE_HENAN_ZHUAN_ZHUAN)
				|| is_mj_type(GameConstants.GAME_TYPE_HALL_ZHUAN_ZHUAN_MJ)) { // liuyan
																				// 2017/7/10
			set_result_descibe_zz();
		} else if (is_mj_type(GameConstants.GAME_TYPE_HZ) || is_mj_type(GameConstants.GAME_TYPE_FLS_HZ_LX)
				|| is_mj_type(GameConstants.GAME_TYPE_HALL_HONG_ZHONG_MJ)) {
			set_result_descibe_hz();
		} else if (this.is_mj_type(GameConstants.GAME_TYPE_CS) || is_mj_type(GameConstants.GAME_TYPE_HALL_CHANG_SHA_MJ)) {
			set_result_describe_cs();
		} else if (is_mj_type(GameConstants.GAME_TYPE_SHUANGGUI)) {
			set_result_describe_sg();
		} else if (is_mj_type(GameConstants.GAME_TYPE_ZHUZHOU)) {
			set_result_describe_zhuzhou();
		} else if (is_mj_type(GameConstants.GAME_TYPE_XTHH)) {
			set_result_describe_xthh();
		} else if (is_mj_type(GameConstants.GAME_TYPE_HENAN_AY)) {
			set_result_describe_ay();
		} else if (is_mj_type(GameConstants.GAME_TYPE_HENAN_LZ)) {
			set_result_describe_lz();
		} else if (is_mj_type(GameConstants.GAME_TYPE_HENAN) || is_mj_type(GameConstants.GAME_TYPE_HENAN_ZMD)
				|| is_mj_type(GameConstants.GAME_TYPE_HENAN_KF) || is_mj_type(GameConstants.GAME_TYPE_HENAN_NY)
				|| is_mj_type(GameConstants.GAME_TYPE_HENAN_XX) || is_mj_type(GameConstants.GAME_TYPE_HENAN_PDS)) {
			set_result_describe_henan();
		} else if (is_mj_type(GameConstants.GAME_TYPE_HENAN_HZ)) {
			set_result_descibe_hnhz();
		} else if (is_mj_type(GameConstants.GAME_TYPE_FLS_LX_CG)) {
			set_result_descibe_lxcg();
		} else if (is_mj_type(GameConstants.GAME_TYPE_FLS_CS_LX)) {
			set_result_descibe_lx_cs();
		} else if (is_mj_type(GameConstants.GAME_TYPE_HENAN_LYGC)) {
			set_result_describe_henan_lygc();
		} else if (is_mj_type(GameConstants.GAME_TYPE_HENAN_ZHOU_KOU)) {
			set_result_describe_he_nan_zhou_kou();
		} else if (is_mj_type(GameConstants.GAME_TYPE_HENAN_LH)) {
			set_result_describe_henan_lh();
		} else if (is_mj_type(GameConstants.GAME_TYPE_HENAN_XC)) {
			set_result_describe_henan_xc();
		} else if (is_mj_type(GameConstants.GAME_TYPE_HENAN_XY)) {
			set_result_describe_henan_xy();
		}

	}

	/**
	 * 河南麻将结算描述
	 */
	private void set_result_describe_henan_xc() {
		// 杠牌，每个人的分数
		int lGangScore[] = new int[GameConstants.GAME_PLAYER];
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {

			for (int j = 0; j < GRR._gang_score[i].gang_count; j++) {
				for (int k = 0; k < GameConstants.GAME_PLAYER; k++) {
					lGangScore[k] += GRR._gang_score[i].scores[j][k];// 杠牌，每个人的分数
				}
			}
		}

		int l;
		long type = 0;
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			String des = "";

			if (has_rule(GameConstants.GAME_RULE_HENAN_JIA_DI) && i == GRR._banker_player) {
				des += " 庄家加底";
			}

			// if(has_rule(MJGameConstants.GAME_TYPE_HENAN_GANG_PAO) &&
			// i==GRR._banker_player) {
			// des+=" 杠跑";
			// }

			l = GRR._chi_hu_rights[i].type_count;
			for (int j = 0; j < l; j++) {
				type = GRR._chi_hu_rights[i].type_list[j];
				if (GRR._chi_hu_rights[i].is_valid()) {
					if (type == GameConstants.CHR_ZI_MO) {
						des += " 自摸";
					}

					if (type == GameConstants.CHR_HENAN_QISHOU_HU) {
						// if (is_mj_type(GameConstants.GAME_TYPE_HENAN_XC)) {
						des += " 福禄双全 4番";
						// } else {
						// des += " 4混加倍";
						// }

					}

					if (type == GameConstants.CHR_SHU_FAN) {
						des += " 接炮";
					}
					if (type == GameConstants.CHR_HENAN_QIANG_GANG_HU) {
						des += " 抢杠胡";
					}
					if (type == GameConstants.CHR_HENAN_TIAN_HU) {
						des += " 天胡";
					}
					if (type == GameConstants.CHR_HENAN_QI_XIAO_DUI || type == GameConstants.CHR_HENAN_HH_QI_XIAO_DUI
							|| type == GameConstants.CHR_HUNAN_SHUANG_HAO_HUA_QI_XIAO_DUI) {
						des += " 七小对";

					}
					if (type == GameConstants.CHR_HENAN_GANG_KAI) {
						if (has_rule(GameConstants.GAME_RULE_HENAN_GANGHUA_DOUBLE)) {
							des += " 杠上花加倍";
						} else {
							des += " 杠上花";
						}

					}
				} else {
					if (type == GameConstants.CHR_FANG_PAO) {
						des += " 放炮";
					}
				}
			}
			if (GRR._chi_hu_rights[i].baifeng_count > 0) {
				des += " 白风" + GRR._chi_hu_rights[i].baifeng_count + "组";
			}
			if (GRR._chi_hu_rights[i].heifeng_count > 0) {
				des += " 黑风" + GRR._chi_hu_rights[i].heifeng_count + "组";
			}
			if (GRR._chi_hu_rights[i].sanhun_kan > 0) {
				des += " 三混成坎";
			}
			if (GRR._chi_hu_rights[i].duanmen_count > 1) {
				des += " 断" + GRR._chi_hu_rights[i].duanmen_count + "门";
			}
			int jie_gang = 0, fang_gang = 0, ming_gang = 0, an_gang = 0;
			if (GRR != null) {
				for (int p = 0; p < GameConstants.GAME_PLAYER; p++) {
					for (int w = 0; w < GRR._weave_count[p]; w++) {
						if (GRR._weave_items[p][w].weave_kind != GameConstants.WIK_GANG) {
							continue;
						}
						if (p == i) {// 自己
							// 接杠
							if (GRR._weave_items[p][w].provide_player != p) {
								jie_gang++;
							} else {
								if (GRR._weave_items[p][w].public_card == 1) {// 明杠
									ming_gang++;
								} else {
									an_gang++;
								}
							}
						} else {
							// 放杠
							if (GRR._weave_items[p][w].provide_player == i) {
								fang_gang++;
							}
						}
					}
				}
			}

			if (an_gang > 0) {
				des += " 暗杠X" + an_gang;
			}
			if (ming_gang > 0) {
				des += " 明杠X" + ming_gang;
			}
			if (fang_gang > 0) {
				des += " 放杠X" + fang_gang;
			}
			if (jie_gang > 0) {
				des += " 接杠X" + jie_gang;
			}
			if (lGangScore[i] != 0 && GRR._end_type == GameConstants.Game_End_NORMAL) {
				des += " 杠总分(" + lGangScore[i] + ")";
			}
			GRR._result_des[i] = des;
		}
	}

	/**
	 * 信阳麻将结算描述
	 */
	private void set_result_describe_henan_xy() {
		// 杠牌，每个人的分数
		int lGangScore[] = new int[GameConstants.GAME_PLAYER];
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {

			for (int j = 0; j < GRR._gang_score[i].gang_count; j++) {
				for (int k = 0; k < GameConstants.GAME_PLAYER; k++) {
					lGangScore[k] += GRR._gang_score[i].scores[j][k];// 杠牌，每个人的分数
				}
			}
		}

		int l;
		long type = 0;
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			String des = "";

			// if(has_rule(MJGameConstants.GAME_TYPE_HENAN_GANG_PAO) &&
			// i==GRR._banker_player) {
			// des+=" 杠跑";
			// }
			if (GRR._nao_win_score[i] != 0) {
				des += "闹庄	" + GRR._nao_win_score[i];
			}
			if (GRR._chi_hu_rights[i].is_valid()) {
				des += " 牌钱";
				des += " 小跑";
			}
			l = GRR._chi_hu_rights[i].type_count;
			for (int j = 0; j < l; j++) {
				type = GRR._chi_hu_rights[i].type_list[j];
				if (GRR._chi_hu_rights[i].is_valid()) {
					if (type == GameConstants.CHR_ZI_MO) {
						des += " 自摸";
					}
					if (type == GameConstants.CHR_SHU_FAN) {
						des += " 接炮";
					}

					if (type == GameConstants.CHR_HENAN_XY_MENQING) {
						// if (is_mj_type(GameConstants.GAME_TYPE_HENAN_XC)) {
						des += " 门清";
						// } else {
						// des += " 4混加倍";
						// }

					}

					if (type == GameConstants.CHR_HENAN_XY_BAZHANG) {
						des += " 八张";
					}
					if (type == GameConstants.CHR_HENAN_XY_JIAZI) {
						des += " 夹子";
					}
					if (type == GameConstants.CHR_HENAN_XY_DUYING) {
						des += " 独赢";
					}
					if (type == GameConstants.CHR_HENAN_XY_QI_XIAO_DUI) {
						des += " 七小对";

					}
					if (type == GameConstants.CHR_HENAN_XY_QINGQUE) {
						des += " 清缺";
					}
					if (type == GameConstants.CHR_HENAN_XY_HUNQUE) {
						des += " 混缺";
					}
					if (type == GameConstants.CHR_HENAN_XY_QINGYISE) {
						des += " 清一色";
					}
					if (type == GameConstants.CHR_HENAN_XY_SANQIYING) {
						des += " 三七赢";
					}
					if (type == GameConstants.CHR_HENAN_XY_SANQIJIANG) {
						des += " 三七将";
					}
					if (type == GameConstants.CHR_HENAN_XY_ZHONGWU) {
						des += " 中五";
					}
					if (type == GameConstants.CHR_HENAN_XY_LIANLIU) {
						des += " 连六";
					}
					if (type == GameConstants.CHR_HENAN_XY_GANG_KAI) {
						des += " 杠上开花";
					}
				} else {
					if (type == GameConstants.CHR_FANG_PAO) {
						des += " 放炮";
					}
				}
			}
			if (has_rule(GameConstants.GAME_RULE_HENAN_TUIDAOHU)) {
				int jie_gang = 0, fang_gang = 0, ming_gang = 0, an_gang = 0;
				if (GRR != null) {
					for (int p = 0; p < GameConstants.GAME_PLAYER; p++) {
						for (int w = 0; w < GRR._weave_count[p]; w++) {
							if (GRR._weave_items[p][w].weave_kind != GameConstants.WIK_GANG) {
								continue;
							}
							if (p == i) {// 自己
								// 接杠
								if (GRR._weave_items[p][w].provide_player != p) {
									jie_gang++;
								} else {
									if (GRR._weave_items[p][w].public_card == 1) {// 明杠
										ming_gang++;
									} else {
										an_gang++;
									}
								}
							} else {
								// 放杠
								if (GRR._weave_items[p][w].provide_player == i) {
									fang_gang++;
								}
							}
						}
					}
				}
				if (an_gang > 0) {
					des += " 暗杠X" + an_gang;
				}
				if (ming_gang > 0) {
					des += " 明杠X" + ming_gang;
				}
				if (fang_gang > 0) {
					des += " 放杠X" + fang_gang;
				}
				if (jie_gang > 0) {
					des += " 接杠X" + jie_gang;
				}
				if (lGangScore[i] != 0 && GRR._end_type == GameConstants.Game_End_NORMAL) {
					des += " 杠总分(" + lGangScore[i] + ")";
				}
			}

			GRR._result_des[i] = des;
		}
	}

	// 长沙麻将结束描述
	private void set_result_descibe_lx_cs() {
		int l;
		long type = 0;
		// 有可能是同炮
		boolean has_da_hu = false;
		// 大胡
		boolean dahu[] = { false, false, false, false };
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			if (GRR._chi_hu_rights[i].is_valid() && GRR._chi_hu_rights[i].da_hu_count > 0) {
				if (GRR._chi_hu_rights[i].da_hu_count == 1 && !(GRR._chi_hu_rights[i].opr_and(GameConstants.CHR_HUNAN_QIANG_GANG_HU)).is_empty()) {
					dahu[i] = false;
					has_da_hu = false;
				} else {
					dahu[i] = true;
					has_da_hu = true;
				}

			}
		}
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			ChiHuRight chr = GRR._chi_hu_rights[i];
			String des = "";
			// 小胡
			if (GRR._start_hu_right[i].is_valid()) {
				l = GRR._start_hu_right[i].type_count;
				for (int j = 0; j < l; j++) {

					type = GRR._start_hu_right[i].type_list[j];
					if (type == GameConstants.CHR_HUNAN_XIAO_DA_SI_XI) {
						des += " 大四喜";

					}
					if (type == GameConstants.CHR_HUNAN_XIAO_BAN_BAN_HU) {
						des += " 板板胡";

					}
					if (type == GameConstants.CHR_HUNAN_XIAO_QUE_YI_SE) {
						des += " 缺一色";

					}
					if (type == GameConstants.CHR_HUNAN_XIAO_LIU_LIU_SHUN) {
						des += " 六六顺";
					}
					if (type == GameConstants.CHR_HUNAN_XIAO_JING_TONG_YU_NV) {
						des += " 金童玉女";

					}
					if (type == GameConstants.CHR_HUNAN_XIAO_YI_ZHI_HUA) {
						des += " 一枝花";

					}
					if (type == GameConstants.CHR_HUNAN_XIAO_SAN_TON) {
						des += " 三同";

					}
					if (type == GameConstants.CHR_HUNAN_XIAO_BU_BU_GAO) {
						des += " 步步高";
					}

				}
				des += get_xiao_hu_shai_zi_desc(i);
			}

			l = GRR._chi_hu_rights[i].type_count;
			for (int j = 0; j < l; j++) {
				type = GRR._chi_hu_rights[i].type_list[j];
				if (GRR._chi_hu_rights[i].is_valid()) {
					if (type == GameConstants.CHR_HUNAN_PENGPENG_HU) {
						des += " 碰碰胡";
					}
					if (type == GameConstants.CHR_HUNAN_JIANGJIANG_HU) {
						des += " 将将胡";
					}
					if (type == GameConstants.CHR_HUNAN_QING_YI_SE) {
						des += " 清一色";
					}
					if (type == GameConstants.CHR_HUNAN_HAI_DI_LAO) {
						des += " 海底捞";
					}
					if (type == GameConstants.CHR_HUNAN_HAI_DI_PAO) {
						des += " 海底炮";
					}
					if (type == GameConstants.CHR_HUNAN_QI_XIAO_DUI) {
						des += " 七小对";
					}
					if (type == GameConstants.CHR_HUNAN_HAOHUA_QI_XIAO_DUI) {
						des += " 豪华七小对";
					}
					if (type == GameConstants.CHR_HUNAN_SHUANG_HAO_HUA_QI_XIAO_DUI) {
						des += " 双豪华七小对";
					}
					if (type == GameConstants.CHR_HUNAN_GANG_KAI) {
						des += " 杠上开花";
					}
					if (type == GameConstants.CHR_HUNAN_SHUANG_GANG_KAI) {
						des += " 双杠上开花";
					}
					if (type == GameConstants.CHR_HUNAN_QIANG_GANG_HU) {
						des += " 抢杠胡";
					}
					if (type == GameConstants.CHR_HUNAN_GANG_SHANG_PAO) {
						des += " 杠上炮";
					}
					if (type == GameConstants.CHR_HUNAN_SHUANG_GANG_SHANG_PAO) {
						des += " 双杠上炮";
					}
					if (type == GameConstants.CHR_HUNAN_QUAN_QIU_REN) {

						des += " 全求人";
					}
					if (type == GameConstants.CHR_ZI_MO) {
						if (dahu[i] == true) {
							des += " 大胡自摸";
						} else {
							des += " 小胡自摸";
						}
					}
					if (type == GameConstants.CHR_SHU_FAN) {
						if (dahu[i] == true) {
							des += " 大胡接炮";
						} else {
							des += " 小胡接炮";
						}
					}
					if (type == GameConstants.CHR_FANG_PAO) {
						des += " 放炮";
					}
					if (type == GameConstants.CHR_TONG_PAO) {
						des += " 通炮";
					}
				} else {
					if (type == GameConstants.CHR_FANG_PAO) {
						if (has_da_hu == true) {
							des += " 大胡放炮";
						} else {
							des += " 小胡放炮";
						}
					}
				}
			}

			boolean isMutlp = isMutlpDingNiao();
			String mutlp = isMutlp ? "(乘法)" : "(加法)";
			if (GRR._player_niao_count[i] > 0) {
				des += " 定鸟X" + GRR._player_niao_count[i] + mutlp;
			}
			if (GRR._player_niao_count_fei[i] > 0) {
				des += " 飞鸟X" + GRR._player_niao_count_fei[i];
			}
			if (getZuoPiaoScore() > 0) {
				des += " 坐飘" + getZuoPiaoScore() + "分";
			}
			GRR._result_des[i] = des;
		}
	}

	// 长沙麻将结束描述
	private void set_result_describe_cs() {
		int l;
		long type = 0;
		// 有可能是同炮
		boolean has_da_hu = false;
		// 大胡
		boolean dahu[] = { false, false, false, false };
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			if (GRR._chi_hu_rights[i].is_valid() && GRR._chi_hu_rights[i].da_hu_count > 0) {
				dahu[i] = true;
				has_da_hu = true;
			}
		}
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			ChiHuRight chr = GRR._chi_hu_rights[i];
			String des = "";
			// 小胡
			if (GRR._start_hu_right[i].is_valid()) {
				l = GRR._start_hu_right[i].type_count;
				for (int j = 0; j < l; j++) {

					type = GRR._start_hu_right[i].type_list[j];
					if (type == GameConstants.CHR_HUNAN_XIAO_DA_SI_XI) {
						des += " 大四喜";

					}
					if (type == GameConstants.CHR_HUNAN_XIAO_BAN_BAN_HU) {
						des += " 板板胡";

					}
					if (type == GameConstants.CHR_HUNAN_XIAO_QUE_YI_SE) {
						des += " 缺一色";

					}
					if (type == GameConstants.CHR_HUNAN_XIAO_LIU_LIU_SHUN) {
						des += " 六六顺";
					}

				}
			}

			l = GRR._chi_hu_rights[i].type_count;
			for (int j = 0; j < l; j++) {
				type = GRR._chi_hu_rights[i].type_list[j];
				if (GRR._chi_hu_rights[i].is_valid()) {
					if (type == GameConstants.CHR_HUNAN_PENGPENG_HU) {
						if (chr.is_mul(GameConstants.CHR_HUNAN_PENGPENG_HU)) {
							des += " 碰碰胡*2";
						} else {
							des += " 碰碰胡";
						}
					}
					if (type == GameConstants.CHR_HUNAN_JIANGJIANG_HU) {
						if (chr.is_mul(GameConstants.CHR_HUNAN_JIANGJIANG_HU)) {
							des += " 将将胡*2";
						} else {
							des += " 将将胡";
						}
					}
					if (type == GameConstants.CHR_HUNAN_QING_YI_SE) {
						if (chr.is_mul(GameConstants.CHR_HUNAN_QING_YI_SE)) {
							des += " 清一色*2";
						} else {
							des += " 清一色";
						}
					}
					if (type == GameConstants.CHR_HUNAN_HAI_DI_LAO) {
						des += " 海底捞";
					}
					if (type == GameConstants.CHR_HUNAN_HAI_DI_PAO) {
						des += " 海底炮";
					}
					if (type == GameConstants.CHR_HUNAN_QI_XIAO_DUI) {
						if (chr.is_mul(GameConstants.CHR_HUNAN_QI_XIAO_DUI)) {
							des += " 七小对*2";
						} else {
							des += " 七小对";
						}
					}
					if (type == GameConstants.CHR_HUNAN_HAOHUA_QI_XIAO_DUI) {
						if (chr.is_mul(GameConstants.CHR_HUNAN_HAOHUA_QI_XIAO_DUI)) {
							des += " 豪华七小对*2";
						} else {
							des += " 豪华七小对";
						}
					}
					if (type == GameConstants.CHR_HUNAN_SHUANG_HAO_HUA_QI_XIAO_DUI) {
						if (chr.is_mul(GameConstants.CHR_HUNAN_SHUANG_HAO_HUA_QI_XIAO_DUI)) {
							des += " 双豪华七小对*2";
						} else {
							des += " 双豪华七小对";
						}
					}
					if (type == GameConstants.CHR_HUNAN_GANG_KAI) {
						if (chr.is_mul(GameConstants.CHR_HUNAN_GANG_KAI)) {
							des += " 杠上开花*2";
						} else {
							des += " 杠上开花";
						}
					}
					if (type == GameConstants.CHR_HUNAN_SHUANG_GANG_KAI) {
						if (chr.is_mul(GameConstants.CHR_HUNAN_SHUANG_GANG_KAI)) {
							des += " 双杠上开花*2";
						} else {
							des += " 双杠上开花";
						}
					}
					if (type == GameConstants.CHR_HUNAN_QIANG_GANG_HU) {
						des += " 抢杠胡";
					}
					if (type == GameConstants.CHR_HUNAN_GANG_SHANG_PAO) {
						if (chr.is_mul(GameConstants.CHR_HUNAN_GANG_SHANG_PAO)) {
							des += " 杠上炮*2";
						} else {
							des += " 杠上炮";
						}
					}
					if (type == GameConstants.CHR_HUNAN_SHUANG_GANG_SHANG_PAO) {
						if (chr.is_mul(GameConstants.CHR_HUNAN_SHUANG_GANG_SHANG_PAO)) {
							des += " 双杠上炮*2";
						} else {
							des += " 双杠上炮";
						}
					}
					if (type == GameConstants.CHR_HUNAN_QUAN_QIU_REN) {

						des += " 全求人";
					}
					if (type == GameConstants.CHR_ZI_MO) {
						if (dahu[i] == true) {
							des += " 大胡自摸";
						} else {
							des += " 小胡自摸";
						}
					}
					if (type == GameConstants.CHR_SHU_FAN) {
						if (dahu[i] == true) {
							des += " 大胡接炮";
						} else {
							des += " 小胡接炮";
						}
					}
					if (type == GameConstants.CHR_FANG_PAO) {
						des += " 放炮";
					}
					if (type == GameConstants.CHR_TONG_PAO) {
						des += " 通炮";
					}
				} else {
					if (type == GameConstants.CHR_FANG_PAO) {
						if (has_da_hu == true) {
							des += " 大胡放炮";
						} else {
							des += " 小胡放炮";
						}
					}
				}
			}

			boolean isMutlp = isMutlpDingNiao();
			String mutlp = isMutlp ? "(乘法)" : "(加法)";
			if (GRR._player_niao_count[i] > 0) {
				des += " 定鸟X" + GRR._player_niao_count[i] + mutlp;
			}
			if (GRR._player_niao_count_fei[i] > 0) {
				des += " 飞鸟X" + GRR._player_niao_count_fei[i];
			}
			if (getZuoPiaoScore() > 0) {
				des += " 坐飘" + getZuoPiaoScore() + "分";
			}
			GRR._result_des[i] = des;
		}
	}

	// 株洲麻将结束描述
	private void set_result_describe_zhuzhou() {
		int l;
		long type = 0;
		// 有可能是同炮
		boolean has_da_hu = false;
		// 大胡
		boolean dahu[] = { false, false, false, false };
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			if (GRR._chi_hu_rights[i].is_valid() && GRR._chi_hu_rights[i].da_hu_count > 0) {
				dahu[i] = true;
				has_da_hu = true;
			}
		}
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			String des = "";

			l = GRR._chi_hu_rights[i].type_count;
			for (int j = 0; j < l; j++) {
				type = GRR._chi_hu_rights[i].type_list[j];
				if (GRR._chi_hu_rights[i].is_valid()) {
					if (type == GameConstants.CHR_HUNAN_PENGPENG_HU) {
						des += " 碰碰胡";
					}
					// if (type == MJGameConstants.CHR_HUNAN_JIANGJIANG_HU) {
					// des += " 将将胡";
					// }
					if (type == GameConstants.CHR_HUNAN_QING_YI_SE) {
						des += " 清一色";
					}
					if (type == GameConstants.CHR_HUNAN_HAI_DI_LAO) {
						des += " 海底捞";
					}
					if (type == GameConstants.CHR_HUNAN_HAI_DI_PAO) {
						des += " 海底炮";
					}
					if (type == GameConstants.CHR_HUNAN_QI_XIAO_DUI) {
						des += " 七小对";
					}
					if (type == GameConstants.CHR_HUNAN_HAOHUA_QI_XIAO_DUI) {
						des += " 豪华七小对";
					}
					if (type == GameConstants.CHR_HUNAN_SHUANG_HAO_HUA_QI_XIAO_DUI) {
						des += " 双豪华七小对";
					}
					if (type == GameConstants.CHR_HUNAN_GANG_KAI) {
						des += " 杠上开花";
					}
					if (type == GameConstants.CHR_HUNAN_SHUANG_GANG_KAI) {
						des += " 双杠杠上开花";
					}
					// if (type == MJGameConstants.CHR_QIANG_GANG_HU) {
					// des += " 抢杠胡";
					// }
					if (type == GameConstants.CHR_HUNAN_GANG_SHANG_PAO) {
						des += " 杠上炮";
					}
					if (type == GameConstants.CHR_HUNAN_SHUANG_GANG_SHANG_PAO) {
						des += " 双杠上炮";
					}
					// if (type == MJGameConstants.CHR_QUAN_QIU_REN) {
					// des += " 全求人";
					// }
					if (type == GameConstants.CHR_HUNAN_TIAN_HU) {
						des += " 天胡";
					}
					if (type == GameConstants.CHR_HUNAN_DI_HU) {
						des += " 地胡";
					}
					if (type == GameConstants.CHR_HUNAN_MEN_QING) {
						des += " 门清";
					}
					if (type == GameConstants.CHR_ZI_MO) {
						if (dahu[i] == true) {
							des += " 大胡自摸";
						} else {
							des += " 小胡自摸";
						}
					}
					if (type == GameConstants.CHR_SHU_FAN) {
						if (dahu[i] == true) {
							des += " 大胡接炮";
						} else {
							des += " 小胡接炮";
						}
					}
					if (type == GameConstants.CHR_FANG_PAO) {
						des += " 放炮";
					}
					if (type == GameConstants.CHR_TONG_PAO) {
						des += " 通炮";
					}
				} else {
					if (type == GameConstants.CHR_FANG_PAO) {
						if (has_da_hu == true) {
							des += " 大胡放炮";
						} else {
							des += " 小胡放炮";
						}
					}
				}
			}

			if (GRR._player_niao_count[i] > 0) {
				des += " 中鸟X" + GRR._player_niao_count[i];
			}

			if (GRR._hu_result[i] == GameConstants.HU_RESULT_FANG_KAN_QUAN_BAO) {
				des += " 放坎全包";
			}

			GRR._result_des[i] = des;
		}
	}

	@Override
	public boolean is_zhuang_xian() {
		return true;
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

		this.set_handler(this._handler_finish);
		this._handler_finish.exe(this);

		return true;
	}

	/***
	 * 
	 * @param seat_index
	 * @param card_count
	 * @param card_data
	 * @param send_client
	 * @param delay
	 * @return
	 */
	public boolean exe_add_discard(int seat_index, int card_count, int card_data[], boolean send_client, int delay) {
		if (delay == 0) {
			this.runnable_add_discard(seat_index, card_count, card_data, send_client);
			return true;
		}
		GameSchedule.put(new AddDiscardRunnable(getRoom_id(), seat_index, card_count, card_data, send_client, getMaxCount()), delay,
				TimeUnit.MILLISECONDS);

		return true;

	}

	/**
	 * //执行发牌 是否延迟
	 * 
	 * @param seat_index
	 * @param delay
	 * @return
	 */
	public boolean exe_dispatch_last_card(int seat_index, int type, int delay_time) {

		if (delay_time > 0) {
			GameSchedule.put(new DispatchLastCardRunnable(this.getRoom_id(), seat_index, type, false), delay_time, TimeUnit.MILLISECONDS);// MJGameConstants.GANG_LAST_CARD_DELAY
		} else {

		}

		return true;
	}

	/**
	 * 显示中间出的牌
	 * 
	 * @param seat_index
	 */
	public void runnable_remove_hun_middle_cards(int seat_index) {
		// 牌局未开 或者等待状态 调度不需要执行 add by zain 2017/6/1
		if ((_game_status == GameConstants.GS_MJ_FREE || _game_status == GameConstants.GS_MJ_WAIT) && is_sys())
			return;
		// add end

		// 去掉
		this.operate_show_card(seat_index, GameConstants.Show_Card_Center, 0, null, GameConstants.INVALID_SEAT);

		// 刷新有癞子的牌
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			boolean has_lai_zi = false;
			for (int j = 0; j < GameConstants.MAX_INDEX; j++) {
				if (GRR._cards_index[i][j] > 0 && _logic.is_magic_index(j)) {
					has_lai_zi = true;
					break;
				}
			}
			if (has_lai_zi) {
				// 刷新自己手牌
				int cards[] = new int[GameConstants.MAX_COUNT];
				int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[i], cards);
				for (int j = 0; j < hand_card_count; j++) {
					if (_logic.is_magic_card(cards[j])) {
						cards[j] += GameConstants.CARD_ESPECIAL_TYPE_HUN;
					}
				}
				this.operate_player_cards(i, hand_card_count, cards, 0, null);
			}
		}

		// 检测听牌
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			this._playerStatus[i]._hu_card_count = this.get_xp_ting_card(this._playerStatus[i]._hu_cards, this.GRR._cards_index[i],
					this.GRR._weave_items[i], this.GRR._weave_count[i]);
			if (this._playerStatus[i]._hu_card_count > 0) {
				this.operate_chi_hu_cards(i, this._playerStatus[i]._hu_card_count, this._playerStatus[i]._hu_cards);
			}
		}
		this.exe_dispatch_card(this._current_player, GameConstants.WIK_NULL, GameConstants.DELAY_SEND_CARD_DELAY);

		//
		// this.exe_dispatch_card(seat_index,MJGameConstants.WIK_NULL, 0);
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
	public boolean exe_gang(int seat_index, int provide_player, int center_card, int action, int type, boolean self, boolean d) {
		// 是否有抢杠胡
		this.set_handler(this._handler_gang);
		this._handler_gang.reset_status(seat_index, provide_player, center_card, action, type, self, d);
		this._handler.exe(this);

		return true;
	}

	public boolean exe_jian_pao_hu(int seat_index, int action, int card) {

		GameSchedule.put(new JianPaoHuRunnable(this.getRoom_id(), seat_index, action, card), GameConstants.DELAY_JIAN_PAO_HU, TimeUnit.MILLISECONDS);

		return true;
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
		// add end

		return true;
	}

	public boolean runnable_chuli_first_card_data(int _seat_index, int _type, boolean _tail) {
		return true;
	}

	@Override
	public boolean runnable_dispatch_first_card_data(int _seat_index, int _type, boolean _tail) {
		return true;
	}

	@Override
	public boolean runnable_gang_card_data(int seat_index, int provide_player, int center_card, int action, int type, boolean depatch, boolean self,
			boolean d) {
		return true;

	}

	/**
	 * 调度,加入牌堆
	 **/
	public void runnable_add_discard(int seat_index, int card_count, int card_data[], boolean send_client) {
		// 牌局未开 或者等待状态 调度不需要执行 add by zain 2017/6/1
		if ((_game_status == GameConstants.GS_MJ_FREE || _game_status == GameConstants.GS_MJ_WAIT) && is_sys())
			return;
		// add end

		if (seat_index == GameConstants.INVALID_SEAT) {
			return;
		}
		if (GRR == null)
			return;// 最后一张
		for (int i = 0; i < card_count; i++) {
			GRR._discard_count[seat_index]++;

			GRR._discard_cards[seat_index][GRR._discard_count[seat_index] - 1] = card_data[i];
		}
		if (send_client == true) {
			this.operate_add_discard(seat_index, card_count, card_data);
		}
	}

	/**
	 * 调度,小胡结束
	 **//*
		 * public void runnable_xiao_hu(int seat_index,boolean is_dispatch) { //
		 * 牌局未开 或者等待状态 调度不需要执行 add by zain 2017/6/1 if ((_game_status ==
		 * GameConstants.GS_MJ_FREE || _game_status == GameConstants.GS_MJ_WAIT)
		 * && is_sys()) return; // add end
		 * 
		 * boolean change_handler = true; int cards[] = new
		 * int[GameConstants.MAX_COUNT]; for (int i = 0; i <
		 * GameConstants.GAME_PLAYER; i++) { // 清除动作 //
		 * _playerStatus[i].clean_status(); change_player_status(i,
		 * GameConstants.INVALID_VALUE); if (GRR._start_hu_right[i].is_valid())
		 * { change_handler = false; // 刷新自己手牌 int hand_card_count =
		 * _logic.switch_to_cards_data(GRR._cards_index[i], cards);
		 * this.operate_player_cards(i, hand_card_count, cards, 0, null);
		 * 
		 * // 去掉 小胡排 this.operate_show_card(i, GameConstants.Show_Card_XiaoHU,
		 * 0, null, GameConstants.INVALID_SEAT); } }
		 * 
		 * _game_status = GameConstants.GS_MJ_PLAY;
		 * 
		 * if(is_dispatch){ this.exe_dispatch_card(seat_index,
		 * GameConstants.WIK_NULL, 0); }else{ //小胡操作过后切换handler
		 * if(change_handler){ this.set_handler(_handler_out_card_operate); }
		 * //利用发牌刷新牌 this.operate_player_get_card(seat_index, 0, new int[] {},
		 * GameConstants.INVALID_SEAT); // 变更玩家状态
		 * this.change_player_status(seat_index,
		 * GameConstants.Player_Status_OUT_CARD); this.operate_player_status();
		 * }
		 * 
		 * }
		 */

	/**
	 * 移除赖根
	 * 
	 * @param seat_index
	 */
	public void runnable_finish_lai_gen(int seat_index) {
		// 去掉
		this.operate_show_card(seat_index, GameConstants.Show_Card_Center, 0, null, GameConstants.INVALID_SEAT);

		// 刷新有癞子的牌
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			boolean has_lai_zi = false;
			for (int j = 0; j < GameConstants.MAX_INDEX; j++) {
				if (GRR._cards_index[i][j] > 0 && _logic.is_magic_index(j)) {
					has_lai_zi = true;
					GRR.mo_lai_count[i] = GRR._cards_index[i][j];// 摸赖次数
					break;
				}
			}
			if (has_lai_zi) {
				// 刷新自己手牌
				int cards[] = new int[GameConstants.MAX_COUNT];
				int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[i], cards);
				for (int j = 0; j < hand_card_count; j++) {
					if (_logic.is_magic_card(cards[j])) {
						cards[j] += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
					}
				}
				this.operate_player_cards(i, hand_card_count, cards, 0, null);
			}
		}

		this.exe_dispatch_card(seat_index, GameConstants.WIK_NULL, 0);
	}

	/**
	 * 显示中间出的牌
	 * 
	 * @param seat_index
	 */
	public void runnable_remove_middle_cards(int seat_index) {
		// 牌局未开 或者等待状态 调度不需要执行 add by zain 2017/6/1
		if ((_game_status == GameConstants.GS_MJ_FREE || _game_status == GameConstants.GS_MJ_WAIT) && is_sys())
			return;
		// add end
		// 去掉
		this.operate_show_card(seat_index, GameConstants.Show_Card_Center, 0, null, GameConstants.INVALID_SEAT);

		// 刷新有癞子的牌
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			boolean has_lai_zi = false;
			for (int j = 0; j < GameConstants.MAX_INDEX; j++) {
				if (GRR._cards_index[i][j] > 0 && _logic.is_magic_index(j)) {
					has_lai_zi = true;
					break;
				}
			}
			if (has_lai_zi) {
				// 刷新自己手牌
				int cards[] = new int[GameConstants.MAX_COUNT];
				int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[i], cards);
				for (int j = 0; j < hand_card_count; j++) {
					if (_logic.is_magic_card(cards[j])) {
						cards[j] += GameConstants.CARD_ESPECIAL_TYPE_GUI;
					}
				}
				this.operate_player_cards(i, hand_card_count, cards, 0, null);
			}
		}

		this.exe_dispatch_card(seat_index, GameConstants.WIK_NULL, 0);
	}

	/**
	 * 
	 * @param seat_index
	 * @param type
	 */
	public void runnable_remove_out_cards(int seat_index, int type) {
		// 牌局未开 或者等待状态 调度不需要执行 add by zain 2017/6/1
		if ((_game_status == GameConstants.GS_MJ_FREE || _game_status == GameConstants.GS_MJ_WAIT) && is_sys())
			return;
		// add end
		// 删掉出来的那张牌
		operate_out_card(seat_index, 0, null, type, GameConstants.INVALID_SEAT);
	}

	public static void main(String[] args) {
		// int cards[] = new int[1000];
		//
		// String d = cards.toString();
		//
		// byte[] dd = d.getBytes();
		//
		// System.out.println("ddddddddd"+dd.length);
		//
		// for(int i=0;i<100000;i++) {
		// int[] _repertory_card_zz = new int[MJGameConstants.CARD_COUNT_ZZ];
		// MJGameLogic logic = new MJGameLogic();
		// logic.random_card_data(_repertory_card_zz);
		// }
		for (int i = 0; i < 10; i++) {
			int s = (int) Math.pow(2, i);
			System.out.println("s==" + s);
		}

		// 测试骰子
		// Random random=new Random();//
		//
		// for(int i=0; i<1000; i++){
		//
		// int rand=random.nextInt(6)+1;//
		// int lSiceCount =
		// FvMask.make_long(FvMask.make_word(rand,rand),FvMask.make_word(rand,rand));
		// int f =
		// ((lSiceCount>>24)+(lSiceCount>>16)-1)%MJGameConstants.GAME_PLAYER;
		//
		// rand=random.nextInt(MJGameConstants.GAME_PLAYER);//
		// System.out.println("==庄家"+rand);
		// }

		/*
		 * int test[][]={{0,0,0,0},{1,0,0,0},{1,0,0,0},{1,0,0,0}}; int sum[] =
		 * new int[4]; for (int i = 0; i < 4; i++) { for (int j = 0; j < 4; j++)
		 * { sum[i]-=test[i][j]; sum[j] += test[i][j]; } }
		 * 
		 * MJGameLogic test_logic = new MJGameLogic();
		 * test_logic.set_magic_card_index(test_logic.switch_to_card_index(
		 * MJGameConstants.ZZ_MAGIC_CARD)); int cards[] = new int[] { 0x35,
		 * 0x35, 0x01, 0x01, 0x03, 0x04, 0x05, 0x11, 0x11, 0x15, 0x15,
		 * 
		 * 0x29, 0x29 , 0x29};
		 * 
		 * List<AnalyseItem> analyseItemArray = new ArrayList<AnalyseItem>();
		 * int cards_index[] = new int[MJGameConstants.MAX_INDEX];
		 * test_logic.switch_to_cards_index(cards, 0, 14, cards_index);
		 * WeaveItem weaveItem[] = new WeaveItem[1]; boolean bValue =
		 * test_logic.analyse_card(cards_index, weaveItem, 0, analyseItemArray);
		 * if (!bValue) { System.out.println("==玩法" +
		 * MJGameConstants.CHR_QI_XIAO_DUI); } //七小队
		 */
		// int cards[] = new int[] { 0x01, 0x01, 0x01, 0x01, 0x12, 0x12, 0x18,
		// 0x18, 0x23, 0x23, 0x26,
		//
		// 0x26, 0x29 };
		//
		// Integer nGenCount = 0;
		// int cards_index[] = new int[MJGameConstants.MAX_INDEX];
		// test_logic.switch_to_cards_index(cards, 0, 13, cards_index);
		// WeaveItem weaveItem[] = new WeaveItem[1];
		// if (test_logic.is_qi_xiao_dui(cards_index, weaveItem, 0, 0x29)!=0) {
		// if (nGenCount > 0) {
		// System.out.println("==玩法" + MJGameConstants.CHR_HAOHUA_QI_XIAO_DUI);
		// //chiHuRight.opr_or(MJGameConstants.CHR_HAOHUA_QI_XIAO_DUI);
		// } else {
		// //chiHuRight.opr_or(MJGameConstants.CHR_QI_XIAO_DUI);
		// System.out.println("==玩法" + MJGameConstants.CHR_QI_XIAO_DUI);
		// }
		// }
		//
		//
		// PlayerResult _player_result = new PlayerResult();
		//
		// for (int i = 0; i < MJGameConstants.GAME_PLAYER; i++) {
		// _player_result.win_order[i] = -1;
		//
		// }
		// _player_result.game_score[0] = 400;
		// _player_result.game_score[1] = 400;
		// _player_result.game_score[2] = 300;
		// _player_result.game_score[3] = 200;
		//
		// int win_idx = 0;
		// int max_score = 0;
		// for (int i = 0; i < MJGameConstants.GAME_PLAYER; i++) {
		// int winner = -1;
		// int s = -999999;
		// for (int j = 0; j < MJGameConstants.GAME_PLAYER; j++) {
		// if (_player_result.win_order[j] != -1) {
		// continue;
		// }
		// if (_player_result.game_score[j] > s) {
		// s = _player_result.game_score[j];
		// winner = j;
		// }
		// }
		// if(s>=max_score){
		// max_score = s;
		// }else{
		// win_idx++;
		// }
		//
		// if (winner != -1) {
		// _player_result.win_order[winner] = win_idx;
		// //win_idx++;
		// }
		// }

		// 测试玩法的
		// int rule_1 = FvMask.mask(MJGameConstants.GAME_TYPE_ZZ_ZIMOHU);
		// int rule_2 = FvMask.mask(MJGameConstants.GAME_TYPE_ZZ_ZHANIAO2);
		// int rule = rule_1 | rule_2;
		// boolean has = FvMask.has_any(rule, rule_2);
		// System.out.println("==玩法" + has);
		//
		// // 测试牌局
		// MJTable table = new MJTable();
		// int game_type_index = MJGameConstants.GAME_TYPE_CS;
		// int game_rule_index = rule_1 | rule_2;//
		// MJGameConstants.GAME_TYPE_ZZ_HONGZHONG
		// int game_round = 8;
		// table.init_table(game_type_index, game_rule_index, game_round);
		// boolean start = table.handler_game_start();
		//
		// for (int i = 0; i < MJGameConstants.MAX_INDEX; i++) {
		// if (table._cards_index[table._current_player][i] > 0) {
		// table.handler_player_out_card(table._current_player,
		// _logic.switch_to_card_data(table._cards_index[table._current_player][i]));
		// break;
		// }
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.cai.common.domain.Room#getPlayerNumber()
	 */
	@Override
	public int getTablePlayerNumber() {
		return GameConstants.GAME_PLAYER;
	}

	// System.out.println("==进入心跳handler==" + start);

	// }

	// public StateMachine<MJTable> get_state_machine() {
	// return _state_machine;
	// }
	//
	// public void set_state_machine(StateMachine<MJTable> _state_machine) {
	// this._state_machine = _state_machine;
	// }

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.cai.common.domain.Room#handler_request_trustee(int, boolean)
	 */
	@Override
	public boolean handler_request_trustee(int get_seat_index, boolean isTrustee, int trustee_type) {
		if (_playerStatus == null || istrustee == null) {
			send_error_notify(get_seat_index, 2, "游戏未开始,无法托管!");
			return false;
		}
		if (!is_sys())// 麻将普通场 不需要托管
			return false;

		// 游戏开始前 无法托管
		if (_game_status == GameConstants.GS_MJ_FREE || _game_status == GameConstants.GS_MJ_WAIT) {
			send_error_notify(get_seat_index, 2, "游戏还未开始,无法托管!");
			return false;
		}

		istrustee[get_seat_index] = isTrustee;

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_IS_TRUSTEE);
		roomResponse.setOperatePlayer(get_seat_index);
		roomResponse.setIstrustee(isTrustee);
		this.send_response_to_room(roomResponse);

		if (GRR != null) {
			GRR.add_room_response(roomResponse);
		}

		if (_handler != null && isTrustee) {
			_handler.handler_be_set_trustee(this, get_seat_index);
		}
		return true;

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
	public int set_fei_niao_valid(int card_data, boolean val) {
		if (val) {
			if (card_data > GameConstants.FEI_NIAO_INVALID && card_data < GameConstants.FEI_NIAO_VALID) {
				card_data -= GameConstants.FEI_NIAO_INVALID;
			} else if (card_data > GameConstants.FEI_NIAO_VALID) {
				card_data -= GameConstants.FEI_NIAO_VALID;
			}
		} else {
			if (card_data > GameConstants.FEI_NIAO_INVALID) {
				return card_data;
			}
		}

		if (val == true) {
			// 生效
			return (card_data < GameConstants.FEI_NIAO_VALID ? card_data + GameConstants.FEI_NIAO_VALID : card_data);
		} else {
			return (card_data < GameConstants.FEI_NIAO_INVALID ? card_data + GameConstants.FEI_NIAO_INVALID : card_data);
		}

	}

	public void load_out_card_ting(int seat_index, RoomResponse.Builder roomResponse, TableResponse.Builder tableResponse) {
		int ting_count = _playerStatus[seat_index]._hu_out_card_count;
		if (ting_count <= 0)
			return;

		int l = tableResponse.getCardsDataCount();
		for (int i = 0; i < l; i++) {
			for (int j = 0; j < ting_count; j++) {
				if (tableResponse.getCardsData(i) == _playerStatus[seat_index]._hu_out_card_ting[j]) {
					tableResponse.setCardsData(i, tableResponse.getCardsData(i) + GameConstants.CARD_ESPECIAL_TYPE_TING);
				}
			}
		}
		// 打出去可以听牌的个数
		roomResponse.setOutCardCount(ting_count);

		for (int i = 0; i < ting_count; i++) {
			int ting_card_cout = _playerStatus[seat_index]._hu_out_card_ting_count[i];
			roomResponse.addOutCardTingCount(ting_card_cout);
			if (_logic.is_magic_card(_playerStatus[seat_index]._hu_out_card_ting[i])) {
				roomResponse.addOutCardTing(_playerStatus[seat_index]._hu_out_card_ting[i] + GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI);
			} else {
				roomResponse.addOutCardTing(_playerStatus[seat_index]._hu_out_card_ting[i] + GameConstants.CARD_ESPECIAL_TYPE_TING);
			}

			Int32ArrayResponse.Builder int_array = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < ting_card_cout; j++) {
				int_array.addItem(_playerStatus[seat_index]._hu_out_cards[i][j]);
			}
			roomResponse.addOutCardTingCards(int_array);
		}
	}

	@Override
	public boolean handler_refresh_player_data(int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);// 刷新玩家
		this.load_player_info_data(roomResponse);
		this.send_response_to_player(seat_index, roomResponse);
		// this.send_response_to_room(roomResponse);
		return true;
	}

	@Override
	public boolean kickout_not_ready_player() {
		_kick_schedule = null;

		if (!is_sys())
			return false;

		if (_game_status == GameConstants.GS_MJ_FREE || _game_status == GameConstants.GS_MJ_WAIT) {
			if (getPlayerCount() != GameConstants.GAME_PLAYER || _player_ready == null) {
				return false;
			}

			// 检查是否所有人都未准备
			int not_ready_count = 0;
			for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
				if (get_players()[i] != null && _player_ready[i] == 0) {// 未准备的玩家
					not_ready_count++;
				}
			}
			if (not_ready_count == GameConstants.GAME_PLAYER)// 所有人都未准备 不用踢
				return false;

			for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
				Player rPlayer = get_players()[i];
				if (rPlayer != null && _player_ready[i] == 0) {// 未准备的玩家
					send_error_notify(i, 2, "您长时间未准备,被踢出房间!");

					RoomResponse.Builder quit_roomResponse = RoomResponse.newBuilder();
					quit_roomResponse.setType(MsgConstants.RESPONSE_FORCE_EXIT);// 直接拉出游戏
					send_response_to_player(i, quit_roomResponse);

					this.get_players()[i] = null;
					_player_ready[i] = 0;
					// _player_open_less[i] = 0;
					PlayerServiceImpl.getInstance().quitRoomId(this.getRoom_id(), rPlayer.getAccount_id());
				}
			}
			//
			if (getPlayerCount() == 0) {// 释放房间
				// 删除房间
				PlayerServiceImpl.getInstance().delRoomId(this.getRoom_id());
			} else {
				// 刷新玩家
				RoomResponse.Builder refreshroomResponse = RoomResponse.newBuilder();
				refreshroomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);
				this.load_player_info_data(refreshroomResponse);
				send_response_to_room(refreshroomResponse);
			}
			return true;
		}
		return false;
	}

	public boolean open_card_timer() {
		return false;
	}

	public boolean robot_banker_timer() {
		return false;
	}

	public boolean ready_timer() {
		return false;
	}

	public boolean add_jetton_timer() {
		return false;
	}

	/*
	 * public void change_handler(MJHandler dest_handler) { _handler =
	 * dest_handler; }
	 */
	public void change_player_status(int seat_index, int st) {
		if (_trustee_schedule == null) {
			_trustee_schedule = new ScheduledFuture[GameConstants.GAME_PLAYER];
		}

		if (_trustee_schedule[seat_index] != null) {
			_trustee_schedule[seat_index].cancel(false);
			_trustee_schedule[seat_index] = null;
		}

		PlayerStatus curPlayerStatus = _playerStatus[seat_index];
		curPlayerStatus.set_status(st);// 操作状态

		// 如果是出牌或者操作状态 需要倒计时托管&& !istrustee[seat_index]
		if (is_sys() && (st == GameConstants.Player_Status_OPR_CARD || st == GameConstants.Player_Status_OUT_CARD)) {
			_trustee_schedule[seat_index] = GameSchedule.put(new TuoGuanRunnable(getRoom_id(), seat_index), GameConstants.TRUSTEE_TIME_OUT_SECONDS,
					TimeUnit.SECONDS);
		}
	}

	@Override
	public void clear_score_in_gold_room() {
		ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
		int score = 0;

		// 门槛判断
		SysParamModel sysParamModel = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(getGame_id()).get(5006);
		int beilv = sysParamModel.getVal2().intValue();// 倍率
		float[] scores = _player_result.getGame_score();
		// GameConstants.GAME_PLAYER
		for (int i = 0; i < scores.length; i++) {
			if (this.get_players()[i] == null) {
				continue;
			}
			score = (int) (scores[i] * beilv);
			// 逻辑处理
			this.get_players()[i].setMoney(this.get_players()[i].getMoney() + score);
			StringBuilder buf = new StringBuilder();
			buf.append("牌局消耗:" + scores[i]).append("game_id:" + getGame_id());
			AddMoneyResultModel addGoldResultModel = centerRMIServer.addAccountMoney(this.get_players()[i].getAccount_id(), score, false,
					buf.toString(), EMoneyOperateType.ROOM_COST);
			if (addGoldResultModel.isSuccess() == false) {
				// 扣费失败
				logger.error("玩家:" + this.get_players()[i].getAccount_id() + ",扣费:" + score + "失败");
			}
			// 结算后 清0
			scores[i] = 0;
		}
		// playerNumber = 0;
	}

	@Override
	public boolean handler_refresh_all_player_data() {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);// 刷新玩家
		this.load_player_info_data(roomResponse);
		this.send_response_to_room(roomResponse);
		return true;
	}

	@Override
	public boolean handler_requst_message_deal(Player player, int seat_index, RoomRequest room_rq, int type) {
		return true;
	}

	@Override
	public boolean trustee_timer(int operate_id, int seat_index) {
		// TODO Auto-generated method stub
		return false;
	}

}
