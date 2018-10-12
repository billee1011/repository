/**
 * 
 */
package com.cai.game.phz.handler.yiyangwhz;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.define.ECardType;
import com.cai.common.define.EMoneyOperateType;
import com.cai.common.domain.AddMoneyResultModel;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.GangCardResult;
import com.cai.common.domain.Player;
import com.cai.common.domain.PlayerResult;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.SysParamModel;
import com.cai.common.domain.WeaveItem;
import com.cai.common.rmi.ICenterRMIServer;
import com.cai.common.util.PBUtil;
import com.cai.common.util.PerformanceTimer;
import com.cai.common.util.RandomUtil;
import com.cai.common.util.RoomComonUtil;
import com.cai.common.util.SpringService;
import com.cai.dictionary.SysParamDict;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.AddDiscardRunnable;
import com.cai.future.runnable.ChulifirstCardRunnable;
import com.cai.future.runnable.DispatchCardRunnable;
import com.cai.future.runnable.DispatchFirstCardRunnable;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.future.runnable.GangCardRunnable;
import com.cai.future.runnable.JianPaoHuRunnable;
import com.cai.game.RoomUtil;
import com.cai.game.phz.PHZGameLogicYIYANG;
import com.cai.game.phz.PHZTable;
import com.cai.game.phz.data.AnalyseItem;
import com.cai.game.phz.data.LouWeaveItem;
import com.cai.service.MongoDBServiceImpl;
import com.cai.service.PlayerServiceImpl;
import com.cai.util.SysParamUtil;

import protobuf.clazz.Protocol.ChiGroupCard;
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
import protobuf.clazz.whz.WhzRsp.make_sure_shen_card;
import protobuf.clazz.whz.WhzRsp.shen_yao_card;

//效验类型
class EstimatKind {
	public static int EstimatKind_OutCard = 1; // 出牌效验
	public static int EstimatKind_GangCard = 2; // 杠牌效验
}

///////////////////////////////////////////////////////////////////////////////////////////////
public class YiYangWHZTable extends PHZTable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 7060061356475703643L;
	protected static final int ID_TIMER_SHEN_TO_START = 1;// 神牌到开始
	protected static final int ID_TIMER_SHEN_TO_FINISH = 2;// 神牌到开始
	private static Logger logger = Logger.getLogger(YiYangWHZTable.class);

	public int _shen_pai;
	public int _max_xi_shu;

	public YiYangWHZTable() {

		_logic = new PHZGameLogicYIYANG();
	}

	@Override
	public void on_init_table(int game_type_index, int game_rule_index, int game_round) {
		_game_type_index = game_type_index;//
		_game_rule_index = game_rule_index;
		_game_round = game_round;
		_cur_round = 0;
		// 上次抓的牌
		_last_card = 0;
		_long_count = new int[this.getTablePlayerNumber()]; // 每个用户有几条龙
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_long_count[i] = 0;
			_ti_two_long[i] = false;
			_is_xiang_gong[i] = false;
		}
		// 不能吃，碰
		_cannot_chi = new int[this.getTablePlayerNumber()][30];
		_cannot_chi_count = new int[this.getTablePlayerNumber()];
		_cannot_peng = new int[this.getTablePlayerNumber()][30];
		_cannot_peng_count = new int[this.getTablePlayerNumber()];
		_user_out_card_count = new int[getTablePlayerNumber()];
		_da_pai_count = new int[this.getTablePlayerNumber()];
		_xiao_pai_count = new int[this.getTablePlayerNumber()];
		_huang_zhang_count = 0;
		_tuan_yuan_count = new int[this.getTablePlayerNumber()];
		_hong_pai_count = new int[this.getTablePlayerNumber()];
		_ying_hu_count = new int[this.getTablePlayerNumber()];
		_chun_ying_count = new int[this.getTablePlayerNumber()];
		_guo_hu_xt = new int[this.getTablePlayerNumber()];
		_ti_mul_long = new int[this.getTablePlayerNumber()];
		_guo_hu_pai_count = new int[this.getTablePlayerNumber()];
		_ting_card = new boolean[this.getTablePlayerNumber()];
		_tun_shu = new int[this.getTablePlayerNumber()];
		_fan_shu = new int[this.getTablePlayerNumber()];
		_hu_code = new int[this.getTablePlayerNumber()];
		_liu_zi_fen = new int[this.getTablePlayerNumber()];
		_zong_liu_zi_fen = 0;
		_all_xi = 0;
		_all_lz_fen = 0;
		_hu_xi = new int[this.getTablePlayerNumber()];
		_hu_pai_max_hu = new int[this.getTablePlayerNumber()];
		_xt_display_an_long = new boolean[this.getTablePlayerNumber()];
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_cannot_chi_count[i] = 0;
			_cannot_peng_count[i] = 0;
			_user_out_card_count[i] = 0;
			_da_pai_count[i] = 0;
			_xiao_pai_count[i] = 0;
			_tuan_yuan_count[i] = 0;
			_hong_pai_count[i] = 0;
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
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			for (int j = 0; j < 15; j++)
				_guo_hu_pai_cards[i][j] = 0;
		}
		_hu_weave_count = new int[this.getTablePlayerNumber()]; //

		_hu_weave_items = new WeaveItem[this.getTablePlayerNumber()][7];
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			for (int j = 0; j < 7; j++) {
				_hu_weave_items[i][j] = new WeaveItem();
			}
		}
		_lou_weave_item = new LouWeaveItem[this.getTablePlayerNumber()][7];
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			for (int j = 0; j < 7; j++)
				_lou_weave_item[i][j] = new LouWeaveItem();
		}

		// 胡牌信息
		// _hu_card_info = new HuCardInfo();
		_player_result = new PlayerResult(this.getRoom_owner_account_id(), this.getRoom_id(), _game_type_index,
				_game_rule_index, _game_round, this.get_game_des());
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_player_result.hu_pai_count[i] = 0;
			_player_result.ming_tang_count[i] = 0;
			_player_result.ying_xi_count[i] = 0;
		}
		_shen_pai = -1;
		_handler_dispath_firstcards = new WHZHandlerDispatchFirstCard_YiYang();
		_handler_chuli_firstcards = new WHZHandlerChuLiFirstCard_YiYang();
		_handler_dispath_card = new WHZHandlerDispatchCard_YiYang();
		_handler_out_card_operate = new WHZHandlerOutCardOperate_YiYang();
		_handler_gang = new WHZHandlerGang_YiYang();
		_handler_chi_peng = new WHZHandlerChiPeng_YiYang();
		_handler_wai = new WHZHandlerWai_YiYang();
		_handler_liu = new WHZHandlerQing_YiYang();
		_handler_piao = new WHZHandlerPiao_YiYang();

		if (has_rule(GameConstants.GAME_RULE_YIYANG_WHZ_XI_MAX_500)) {
			_max_xi_shu = 500;
		} else if (has_rule(GameConstants.GAME_RULE_YIYANG_WHZ_XI_MAX_400)) {
			_max_xi_shu = 400;
		} else if (has_rule(GameConstants.GAME_RULE_YIYANG_WHZ_XI_MAX_300)) {
			_max_xi_shu = 300;
		} else if (has_rule(GameConstants.GAME_RULE_YIYANG_WHZ_XI_MAX_200)) {
			_max_xi_shu = 200;
		} else {
			_max_xi_shu = 100;
		}
	}

	@Override
	public boolean has_rule(int cbRule) {
		return ruleMap.containsKey(cbRule);
	}

	@Override
	public void runnable_set_trustee(int _seat_index) {
		// TODO Auto-generated method stub

	}

	////////////////////////////////////////////////////////////////////////

	// 游戏开始
	@Override
	protected boolean on_handler_game_start() {

		_game_status = GameConstants.GS_MJ_PLAY;
		this.log_error("gme_status:" + this._game_status);

		reset_init_data();

		// 庄家选择
		this.progress_banker_select();

		GRR._banker_player = _cur_banker;
		_current_player = GRR._banker_player;

		//
		_repertory_card = new int[GameConstants.CARD_COUNT_PHZ_YYWHZ];
		shuffle(_repertory_card, GameConstants.CARD_DATA_PHZ_YYWHZ);
		_init_left_count = _all_card_len - (this.getTablePlayerNumber() * this.get_hand_card_count_init());

		if (this.has_rule(GameConstants.GAME_RULE_YIYANG_QU_PAI) && this.getTablePlayerNumber() == 2) {
			_init_left_count -= 19;
			GRR._left_card_count -= 19;
		}
		if (DEBUG_CARDS_MODE || BACK_DEBUG_CARDS_MODE)
			test_cards();
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			for (int j = 0; j < GameConstants.MAX_HH_INDEX; j++) {
				_hand_card_index[i][j] = GRR._cards_index[i][j];
			}
		}

		if (this._cur_round == 1) {
			this._shen_pai = _repertory_card[RandomUtil.getRandomNumber(Integer.MAX_VALUE)
					% GameConstants.CARD_COUNT_PHZ_YYWHZ];
			return send_shen_card(GameConstants.INVALID_SEAT);
		} else {
			send_shen_card(GameConstants.INVALID_SEAT);
			return game_start_WHZ_YY();
		}

	}

	public boolean send_shen_yao(int card_data, int seat_idnex, int reason) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		this.load_common_status(roomResponse);
		roomResponse.setType(MsgConstants.RESPONSE_WHZYIYANG_SHEN_YAO);
		roomResponse.setGameStatus(this._game_status);
		// 发送数据
		shen_yao_card.Builder shen_yao = shen_yao_card.newBuilder();
		shen_yao.setShenYaoValue(card_data);
		if (card_data % 2 == this._shen_pai % 2) {
			shen_yao.setHasShen(1);
		} else {
			shen_yao.setHasShen(0);
		}
		roomResponse.setCommResponse(PBUtil.toByteString(shen_yao));
		this.send_response_to_room(roomResponse);
		GRR.add_room_response(roomResponse);
		set_timer(ID_TIMER_SHEN_TO_FINISH, 3, seat_idnex, reason);

		return true;
	}

	public boolean send_shen_card(int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		this.load_common_status(roomResponse);
		roomResponse.setType(MsgConstants.RESPONSE_WHZYIYANG_SHEN_CARD);
		roomResponse.setGameStatus(this._game_status);
		// 发送数据
		make_sure_shen_card.Builder shen_card = make_sure_shen_card.newBuilder();
		shen_card.setShenPaiValue(_shen_pai);

		if (seat_index == GameConstants.INVALID_SEAT) {

			if (this._cur_round == 1) {
				shen_card.setIsAnimation(1);
				roomResponse.setCommResponse(PBUtil.toByteString(shen_card));
				GRR.add_room_response(roomResponse);
				this.send_response_to_room(roomResponse);
				set_timer(ID_TIMER_SHEN_TO_START, 2, null, null);
			} else {
				shen_card.setIsAnimation(0);
				roomResponse.setCommResponse(PBUtil.toByteString(shen_card));
				GRR.add_room_response(roomResponse);
			}
		} else {
			shen_card.setIsAnimation(0);
			roomResponse.setCommResponse(PBUtil.toByteString(shen_card));
			this.send_response_to_player(seat_index, roomResponse);
		}

		return true;
	}

	private boolean game_start_WHZ_YY() {
		_logic.clean_magic_cards();
		int playerCount = this.getTablePlayerNumber();
		this.GRR._banker_player = this._current_player = this._cur_banker;

		// 游戏开始
		this._game_status = GameConstants.GS_MJ_PLAY;// 设置状态
		GameStartResponse.Builder gameStartResponse = GameStartResponse.newBuilder();
		gameStartResponse.setBankerPlayer(this.GRR._banker_player);
		gameStartResponse.setCurrentPlayer(this._current_player);
		gameStartResponse.setLeftCardCount(this.GRR._left_card_count);

		int hand_cards[][] = new int[playerCount][get_hand_card_count_max()];
		// 发送数据
		for (int i = 0; i < playerCount; i++) {
			int hand_card_count = this._logic.switch_to_cards_data(this.GRR._cards_index[i], hand_cards[i]);
			gameStartResponse.addCardsCount(hand_card_count);
		}

		int FlashTime = 4000;
		int standTime = 1000;
		// 发送数据
		for (int i = 0; i < playerCount; i++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

			// 只发自己的牌
			gameStartResponse.clearCardData();
			for (int j = 0; j < get_hand_card_count_max(); j++) {
				gameStartResponse.addCardData(hand_cards[i][j]);
			}

			// 回放数据
			this.GRR._video_recode.addHandCards(cards);

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			this.load_room_info_data(roomResponse);
			this.load_common_status(roomResponse);

			if (this._cur_round == 1) {
				this.load_player_info_data(roomResponse);
			}
			roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
			roomResponse.setGameStart(gameStartResponse);
			roomResponse.setCurrentPlayer(
					this._current_player == GameConstants.INVALID_SEAT ? this._resume_player : this._current_player);
			roomResponse.setLeftCardCount(this.GRR._left_card_count);
			roomResponse.setGameStatus(this._game_status);
			roomResponse.setLeftCardCount(this.GRR._left_card_count);
			int gameId = this.getGame_id() == 0 ? 5 : this.getGame_id();
			SysParamModel sysParamModel1104 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(gameId)
					.get(1104);
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

			for (int j = 0; j < get_hand_card_count_max(); j++) {
				cards.addItem(hand_cards[i][j]);
			}
			gameStartResponse.addCardsData(cards);
		}

		roomResponse.setGameStart(gameStartResponse);
		roomResponse.setLeftCardCount(this.GRR._left_card_count);

		this.GRR.add_room_response(roomResponse);

		this._handler = this._handler_dispath_firstcards;
		this.exe_dispatch_first_card(_current_player, GameConstants.WIK_NULL, 0);

		return true;
	}

	@Override
	public boolean animation_timer(int timer_id, Object obj_one, Object obj_two) {
		_cur_game_timer = -1;
		switch (timer_id) {
		case ID_TIMER_SHEN_TO_START: {
			if (GRR == null) {
				return false;
			}
			return game_start_WHZ_YY();
		}
		case ID_TIMER_SHEN_TO_FINISH: {
			int seat_index = (int) obj_one;
			int reason = (int) obj_two;
			return this.handler_game_finish_yywhz(seat_index, reason);
		}
		}
		return true;
	}

	/**
	 * 是否托管
	 * 
	 * @param seat_index
	 * @return
	 */
	@Override
	public boolean isTrutess(int seat_index) {
		if (seat_index < 0 || seat_index > this.getTablePlayerNumber())
			return false;
		return istrustee[seat_index];
	}

	/// 洗牌
	private void shuffle(int repertory_card[], int mj_cards[]) {
		_all_card_len = repertory_card.length;
		GRR._left_card_count = _all_card_len;

		int xi_pai_count = 0;
		int rand = RandomUtil.generateRandomNumber(3, 6);

		while (xi_pai_count < 6 && xi_pai_count < rand) {
			if (xi_pai_count == 0)
				_logic.random_card_data(repertory_card, mj_cards);
			else
				_logic.random_card_data(repertory_card, repertory_card);

			xi_pai_count++;
		}

		if (DEBUG_CARDS_MODE) {
			int cards[] = new int[] { 1, 21, 26, 23, 24, 17, 7, 2, 8, 23, 22, 21, 3, 18, 9, 4, 4, 19, 3, 20, 9, 5, 2, 1,
					6, 25, 5, 2, 17, 25, 24, 19, 6, 21, 3, 22, 22, 18, 25, 8, 1, 19, 18, 5, 20, 7, 7, 18, 17, 25, 10,
					26, 21, 4, 23, 8, 3, 22, 7, 10, 10, 20, 17, 20, 9, 5, 6, 23, 24, 6, 10, 1, 9, 26, 19, 2, 24, 8, 4,
					26 };
			for (int i = 0; i < cards.length; i++) {
				_repertory_card[i] = cards[i];
			}
			GRR._banker_player = 2;
			_current_player = 2;
		}

		int send_count;
		int have_send_count = 0;

		int count = getTablePlayerNumber();
		for (int i = 0; i < count; i++) {
			send_count = this.get_hand_card_count_max() - 1;

			GRR._left_card_count -= send_count;
			// 一人20张牌,庄家多一张
			_logic.switch_to_cards_index(repertory_card, have_send_count, send_count, GRR._cards_index[i]);
			have_send_count += send_count;

		}

		// test_cards();
		// 记录初始牌型
		_recordRoomRecord.setBeginArray(Arrays.toString(repertory_card));
	}

	private void test_cards() {
		int cards[] = new int[] { 0x8, 0x8, 0x8, 0x8, 0x12, 0x4, 0x4, 0x6, 0x7, 0x14, 0x15, 0x16, 0x17, 0x18, 0x19, 0x9,
				0x9, 0x1a, 0x1a, 0x8, 0x8, 0x8, 0x8, 0x12, 0x4, 0x4, 0x6, 0x7, 0x14, 0x15, 0x16, 0x17, 0x18, 0x19, 0x9,
				0x9, 0x1a, 0x1a, 0x8, 0x8, 0x8, 0x8, 0x12, 0x4, 0x4, 0x6, 0x7, 0x14, 0x15, 0x16, 0x17, 0x18, 0x19, 0x9,
				0x9, 0x1a, 0x1a };
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			for (int j = 0; j < get_hand_card_count_max(); j++) {
				GRR._cards_index[i][j] = 0;
				_hand_card_index[i][j] = 0;
			}
		}
		GRR._banker_player = 0;
		_current_player = GRR._banker_player;
		int card_index = 0;
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			for (int j = 0; j < this.get_hand_card_count_init(); j++) {
				int index = _logic.switch_to_card_index(cards[card_index++]);
				GRR._cards_index[i][index] += 1;
				_hand_card_index[i][index] += 1;
			}
		}
		// _repertory_card = cards;
		// /********
		// * 下面这个给测试用的 如果开发用 把上面的注释去掉 realyCards
		// **************************/
		// int[] realyCards = new int[] {
		// 22, 9, 6, 20, 23, 19, 1, 24, 3, 26, 2, 20, 9, 17, 2, 7, 18, 24, 3,
		// 25, 10, 8, 25, 3, 10, 22, 9,
		// 6, 21, 19, 6, 24, 9, 20, 21, 17, 4, 26, 5, 1, 1, 10, 21, 17, 8, 18,
		// 2,
		// 24, 22, 10, 4, 5, 21, 5, 26, 2, 18, 22, 7, 4, 8, 19, 18, 7, 23,
		// 3, 5, 25, 23, 19, 25, 26, 23, 7, 1, 8, 4, 17, 6, 20
		//
		// };
		// this._cur_banker = 1;
		// testRealyCard(realyCards);
		if (BACK_DEBUG_CARDS_MODE) {
			if (debug_my_cards != null) {

				if (is_mj_type(GameConstants.GAME_TYPE_WMQ_AX)) {
					if (debug_my_cards.length > 19) {
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
				} else if (is_mj_type(GameConstants.GAME_TYPE_FPHZ_YX) || is_mj_type(GameConstants.GAME_TYPE_LHQ_HD)) {
					if (debug_my_cards.length > 15) {
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
				} else {
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
	}

	/**
	 * 测试线上 实际牌
	 */
	@Override
	public void testRealyCard(int[] realyCards) {
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			for (int j = 0; j < GameConstants.MAX_HH_INDEX; j++) {
				GRR._cards_index[i][j] = 0;
			}
		}
		this._repertory_card = realyCards;
		_all_card_len = _repertory_card.length;
		GRR._left_card_count = _all_card_len;

		int send_count;
		int have_send_count = 0;
		// 分发扑克
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			send_count = this.get_hand_card_count_init();
			GRR._left_card_count -= send_count;

			// 一人20张牌,庄家多一张
			_logic.switch_to_cards_index(_repertory_card, have_send_count, send_count, GRR._cards_index[i]);

			have_send_count += send_count;
		}
		DEBUG_CARDS_MODE = false;// 把调试模式关闭
		BACK_DEBUG_CARDS_MODE = false;
		System.err.println("=========开始调试线上牌型 调试模式自动关闭*=========");

	}

	/**
	 * 模拟牌型--相同牌
	 */
	@Override
	public void testSameCard(int[] cards) {
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			for (int j = 0; j < GameConstants.MAX_HH_INDEX; j++) {
				GRR._cards_index[i][j] = 0;
			}
		}

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			int send_count = 0;
			send_count = this.get_hand_card_count_max();
			if (send_count > cards.length) {
				send_count = cards.length;
			}
			for (int j = 0; j < send_count; j++) {
				GRR._cards_index[i][_logic.switch_to_card_index(cards[j])] += 1;
			}
		}
		DEBUG_CARDS_MODE = false;// 把调试模式关闭
		BACK_DEBUG_CARDS_MODE = false;
	}

	// 摸起来的牌 派发牌
	@Override
	public boolean dispatch_card_data(int cur_player, int type, boolean tail) {

		// 发牌
		this._handler = this._handler_dispath_card;
		this._handler_dispath_card.reset_status(cur_player, type);
		this._handler.exe(this);

		return true;
	}

	// 游戏结束
	@Override
	public boolean on_room_game_finish(int seat_index, int reason) {

		boolean ret = true;

		if (_cur_round == 1 && (reason == GameConstants.Game_End_NORMAL || reason == GameConstants.Game_End_DRAW)) {
			RoomUtil.realkou_dou(this);
		}
		if (reason == GameConstants.Game_End_NORMAL && GRR._left_card_count > 0) {
			this.send_shen_yao(_repertory_card[_all_card_len - GRR._left_card_count], seat_index, reason);
		} else {
			ret = this.handler_game_finish_yywhz(seat_index, reason);
		}

		if (is_sys()) {
			clear_score_in_gold_room();
		}
		return ret;
	}

	public boolean handler_game_finish_yywhz(int seat_index, int reason) {
		_game_status = GameConstants.GS_MJ_WAIT;
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

		// 这里记录了两次，先这样
		RoomInfo.Builder room_info = getRoomInfo();
		game_end.setRoomInfo(room_info);
		game_end.setGamePlayerNumber(count);
		game_end.setRoundOverType(0);
		game_end.setRoomOverType(0);
		game_end.setEndTime(System.currentTimeMillis() / 1000L);// 结束时间

		if (GRR != null) {// reason == MJGameConstants.Game_End_NORMAL ||
							// reason
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
			game_end.setGamePlayerNumber(getTablePlayerNumber());
			game_end.setCellScore(GameConstants.CELL_SCORE);

			game_end.setBankerPlayer(GRR._banker_player);// 专家
			game_end.setLeftCardCount(GRR._left_card_count);// 剩余牌
			int left_card_count = GRR._left_card_count;
			int cards[] = new int[_all_card_len - (_all_card_len - GRR._left_card_count)];
			int k = 0;
			for (int i = _all_card_len - GRR._left_card_count; i < _all_card_len; i++) {
				cards[k] = _repertory_card[_all_card_len - left_card_count];
				game_end.addCardsList(cards[k]);

				k++;
				left_card_count--;
			}

			game_end.setShowBirdEffect(GRR._show_bird_effect == false ? 0 : 1);

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				Int32ArrayResponse.Builder pnc = Int32ArrayResponse.newBuilder();
				game_end.addHuResult(GRR._hu_result[i]);

				// 胡的牌
				Int32ArrayResponse.Builder hc = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < this.get_hand_card_count_max(); j++) {
					hc.addItem(GRR._chi_hu_card[i][j]);
				}

				game_end.addHuCardData(GRR._chi_hu_card[i][0]);
				game_end.addHuCardArray(hc);
			}

			// 现在权值只有一位
			long rv[] = new long[GameConstants.MAX_RIGHT_COUNT];

			// 设置胡牌描述
			if (reason == GameConstants.Game_End_NORMAL) {
				this.set_result_describe(seat_index);
				game_end.setFanShu(_fan_shu[seat_index]);
				game_end.setCountPickNiao(GRR._count_pick_niao);// 中鸟个数
			}

			for (int i = 0; i < count; i++) {
				GRR._card_count[i] = _logic.switch_to_cards_data(GRR._cards_index[i], GRR._cards_data[i]);

				Int32ArrayResponse.Builder cs = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GRR._card_count[i]; j++) {

					cs.addItem(GRR._cards_data[i][j]);
				}
				game_end.addCardsData(cs);// 牌
				int all_hu_xi = 0;
				// 组合
				WeaveItemResponseArrayResponse.Builder weaveItem_array = WeaveItemResponseArrayResponse.newBuilder();

				if (_hu_weave_count[i] > 0) {
					for (int j = 0; j < _hu_weave_count[i]; j++) {
						WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
						weaveItem_item.setProvidePlayer(_hu_weave_items[i][j].provide_player);
						weaveItem_item.setPublicCard(_hu_weave_items[i][j].public_card);
						weaveItem_item.setWeaveKind(_hu_weave_items[i][j].weave_kind);
						weaveItem_item.setHuXi(_hu_weave_items[i][j].hu_xi);
						all_hu_xi += _hu_weave_items[i][j].hu_xi;
						weaveItem_item.setCenterCard(_hu_weave_items[i][j].center_card);
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
				game_end.addStartHuScore(this._hu_xi[i]);
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
		if (reason == GameConstants.Game_End_NORMAL || reason == GameConstants.Game_End_DRAW) {
			if (_cur_round >= _game_round) {// 局数到了
				end = true;
				game_end.setRoomOverType(1);
				game_end.setPlayerResult(this.process_player_result(reason));

				for (int i = 0; i < this.getTablePlayerNumber(); i++) {
					game_end.addGameScore(this._player_result.game_score[i]);
				}

			} else {
				// 确定下局庄家
				// 以后谁胡牌，下局谁做庄。
				// 流局,庄家下一个
				// 通炮,放炮的玩家

			}
		} else if (reason == GameConstants.Game_End_RELEASE_PLAY || reason == GameConstants.Game_End_RELEASE_NO_BEGIN
				|| reason == GameConstants.Game_End_RELEASE_RESULT
				|| reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_SYSTEM) {
			end = true;
			real_reason = GameConstants.Game_End_DRAW;// 刘局
			game_end.setRoomOverType(1);
			game_end.setPlayerResult(this.process_player_result(reason));

			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				game_end.addGameScore(this._player_result.game_score[i]);
			}
		}

		game_end.setEndType(real_reason);

		////////////////////////////////////////////////////////////////////// 得分总的
		roomResponse.setGameEnd(game_end);

		this.send_response_to_room(roomResponse);

		record_game_round(game_end, real_reason);

		// 超时解散
		if (reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT) {
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

	public int analyse_chi_hu_card(int cards_index[], WeaveItem weaveItems[], int weave_count, int seat_index,
			int provider_index, int cur_card, ChiHuRight chiHuRight, int card_type, int hu_xi[], boolean dispatch) {
		int action = analyse_chi_hu_card_yywhz(cards_index, weaveItems, weave_count, seat_index, provider_index,
				cur_card, chiHuRight, card_type, dispatch);
		return action;
	}

	/***
	 * 胡牌解析
	 * 
	 * @param cards_index
	 * @param weaveItems
	 * @param weaveCount
	 * @param cur_card
	 * @param chiHuRight
	 * @param card_type
	 * @return
	 */
	public int analyse_chi_hu_card_yywhz(int cards_index[], WeaveItem weaveItems[], int weaveCount, int seat_index,
			int provider_index, int cur_card, ChiHuRight chiHuRight, int card_type, boolean dispatch) {

		if (this._is_xiang_gong[seat_index] == true) {
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
		if (dispatch) {
			if (cur_card != GameConstants.INVALID_VALUE) {
				int index = _logic.switch_to_card_index(cur_card);
				cbCardIndexTemp[index]++;
			}
		}

		List<AnalyseItem> analyseItemArray = new ArrayList<AnalyseItem>();

		// 分析扑克
		this._hu_xi[seat_index] = 0;
		int hu_xi[] = new int[1];
		hu_xi[0] = 0;
		boolean yws_type = false;
		boolean bValue = _logic.analyse_card_yiyangwhz(cbCardIndexTemp, weaveItems, weaveCount, seat_index,
				provider_index, cur_card, analyseItemArray, false, hu_xi, yws_type);

		if (!bValue) {
			chiHuRight.set_empty();
			return GameConstants.WIK_NULL;
		}
		AnalyseItem analyseItem = new AnalyseItem();
		int temp_hu_xi;
		int temp_total_hu_xi;
		int max_hu_xi = 0;
		int max_hu_index = -1;
		int card_count = 0;
		for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
			card_count += cbCardIndexTemp[i];
		}
		for (int i = 0; i < analyseItemArray.size(); i++) {
			temp_hu_xi = 0;
			temp_total_hu_xi = 0;
			analyseItem = analyseItemArray.get(i);
			for (int j = 0; j < 7; j++) {
				if (analyseItem.cbWeaveKind[j] == GameConstants.WIK_NULL) {
					break;
				}

				WeaveItem weave_items = new WeaveItem();
				weave_items.center_card = analyseItem.cbCenterCard[j];
				weave_items.weave_kind = analyseItem.cbWeaveKind[j];
				temp_hu_xi += _logic.get_weave_hu_xi_yiyangwhz_yywzh(weave_items);
				temp_total_hu_xi += _logic.get_weave_hu_xi_yiyangwhz_yywzh(weave_items);
			}
			// 名堂
			if (has_rule(GameConstants.GAME_RULE_YIYANG_WHZ_MING_TANG)) {
				int piao_num = _logic.is_ji_piao(analyseItem);
				if (piao_num == 1) {
					temp_total_hu_xi *= 2;
				}
				if (piao_num == 2) {
					temp_total_hu_xi *= 2;
				}
				if (_logic.is_yin(analyseItem)) {
					temp_total_hu_xi *= 2;
				}
				if (_logic.is_hua_huzi(analyseItem)) {
					temp_total_hu_xi *= 64;
				}

			}
			if (analyseItem.curCardEye != false) {
				if (analyseItem.cbCardEye > GameConstants.CARD_ESPECIAL_TYPE_NIAO) {
					if (seat_index != provider_index) {
						temp_hu_xi += 1;
						temp_total_hu_xi += 1;
					} else {
						temp_hu_xi += 4;
						temp_total_hu_xi += 4;
					}
				} else {
					temp_hu_xi += 1;
					temp_total_hu_xi += 1;
				}
			} else {
				int cbMenEye[] = new int[2];
				cbMenEye[0] = analyseItem.cbMenEye[0];
				cbMenEye[1] = analyseItem.cbMenEye[1];
				if (cbMenEye[0] > GameConstants.CARD_ESPECIAL_TYPE_NIAO) {
					cbMenEye[0] -= GameConstants.CARD_ESPECIAL_TYPE_NIAO;
				}
				if (cbMenEye[1] > GameConstants.CARD_ESPECIAL_TYPE_NIAO) {
					cbMenEye[1] -= GameConstants.CARD_ESPECIAL_TYPE_NIAO;
				}
				if ((cbMenEye[0] % 16 == 2 || cbMenEye[0] % 16 == 7 || cbMenEye[0] % 16 == 10)) {
					if ((cbMenEye[1] % 16 == 2 || cbMenEye[1] % 16 == 7 || cbMenEye[1] % 16 == 10)) {
						temp_hu_xi += 1;
						temp_total_hu_xi += 1;
					}
				}
			}

			int base_xi = 7;
			if (has_rule(GameConstants.GAME_RULE_YIYANG_WHZ_BASE_XI_ONE)
					|| has_rule(GameConstants.GAME_RULE_HU_ADD_XI)) {
				base_xi = 6;
			}
			if (temp_hu_xi >= base_xi) {
				if (temp_total_hu_xi > max_hu_xi) {
					max_hu_index = i;
					max_hu_xi = temp_total_hu_xi;
				}
			}
		}
		if (max_hu_index == -1) {
			chiHuRight.set_empty();
			return GameConstants.WIK_NULL;
		}
		analyseItem = analyseItemArray.get(max_hu_index);
		// 听胡
		boolean bHuBaoting = true;
		if (this.has_rule(GameConstants.GAME_RULE_YIYANG_WHZ_TIAN_DI_HU)) {
			if (_init_left_count - GRR._left_card_count <= 2) {
				if (_init_left_count - GRR._left_card_count == 2) {
					if (seat_index != GRR._banker_player) {
						if (this._logic.get_card_count_by_index(GRR._cards_index[seat_index]) == this
								.get_hand_card_count_init()) {
							chiHuRight.opr_or(GameConstants.CHR_DI_HU_WHZ_YIYANG);
							bHuBaoting = false;
						}
					}
				} else {
					bHuBaoting = false;
				}
			}
		}

		if (seat_index == GRR._banker_player) {
			if (_user_out_card_count[seat_index] > 1) {
				bHuBaoting = false;
			}
		} else {
			if (_user_out_card_count[seat_index] > 0) {
				bHuBaoting = false;
			}
		}

		if (bHuBaoting) {
			if (has_rule(GameConstants.GAME_RULE_YIYANG_WHZ_BAO_TING)) {
				chiHuRight.opr_or(GameConstants.CHR_TING_HU_WHZ_YIYANG);
			}
		} else if (_init_left_count - GRR._left_card_count == 2) {
			if (has_rule(GameConstants.GAME_RULE_YIYANG_WHZ_TIAN_DI_HU)) {
				if (seat_index != GRR._banker_player) {
					if (this._logic.get_card_count_by_index(GRR._cards_index[seat_index]) == this
							.get_hand_card_count_init()) {
						chiHuRight.opr_or(GameConstants.CHR_DI_HU_WHZ_YIYANG);
					}
				} else {

				}
			}

		}
		// 一点红
		if (_logic.calculate_hong_pai_count_yiyang(analyseItem) == 1) {
			chiHuRight.opr_or(GameConstants.CHR_YI_DIAN_HONG_WHZ_YIYANG);
		}
		if (_logic.calculate_hong_pai_count_yiyang(analyseItem) >= 10) {
			chiHuRight.opr_or(GameConstants.CHR_HUO_HUO_WHZ_YIYANG);
		}
		// 全黑
		if (_logic.calculate_hei_pai_count_yiyang(analyseItem) == _logic.calculate_all_pai_count_yiyang(analyseItem)) {
			chiHuRight.opr_or(GameConstants.CHR_QUAN_HEI_WHZ_YIYANG);
		}

		if (has_rule(GameConstants.GAME_RULE_YIYANG_WHZ_DA_XIAO_ZI_HU)) {
			// 全大
			if (_logic.calculate_da_pai_count_yiyang(analyseItem) >= 20) {
				chiHuRight.opr_or(GameConstants.CHR_QUAN_DA_WHZ_YIYANG);
			}
			// 全小
			if (_logic.calculate_xiao_pai_count_yiyang(analyseItem) >= 20) {
				chiHuRight.opr_or(GameConstants.CHR_QUAN_XIAO_WHZ_YIYANG);
			}
		}

		// 对子息
		if (_logic.is_duizixi_yiyang(analyseItem)) {
			chiHuRight.opr_or(GameConstants.CHR_DUI_ZI_XI_WHZ_YIYANG);
		} else if (_logic.is_hanghangxi_yiyang(analyseItem)) {
			// 行行息
			chiHuRight.opr_or(GameConstants.CHR_HANG_HANG_XI_WHZ_YIYANG);
		}
		// 名堂
		if (has_rule(GameConstants.GAME_RULE_YIYANG_WHZ_MING_TANG)) {
			int piao_num = _logic.is_ji_piao(analyseItem);
			if (piao_num == 1) {
				chiHuRight.opr_or(GameConstants.CHR_HAI_DAN_PIAO_WHZ_YIYANG);
			}
			if (piao_num == 2) {
				chiHuRight.opr_or(GameConstants.CHR_HAI_SHUANG_PIAO_WHZ_YIYANG);
			}
			if (_logic.is_yin(analyseItem)) {
				chiHuRight.opr_or(GameConstants.CHR_HAI_YIN_WHZ_YIYANG);
			}
			if (_logic.is_hua_huzi(analyseItem)) {
				chiHuRight.opr_or(GameConstants.CHR_HAI_HUA_HUZI_WHZ_YIYANG);
			}

		}

		if (this.GRR._left_card_count == 0) {
			chiHuRight.opr_or(GameConstants.CHR_HAI_LAO_WHZ_YIYANG);
		}

		for (int j = 0; j < 7; j++) {

			if (analyseItem.cbWeaveKind[j] == GameConstants.WIK_NULL)
				break;

			_hu_weave_items[seat_index][j].weave_kind = analyseItem.cbWeaveKind[j];
			_hu_weave_items[seat_index][j].center_card = analyseItem.cbCenterCard[j];
			_hu_weave_items[seat_index][j].hu_xi = _logic
					.get_weave_hu_xi_yiyangwhz_yywzh(_hu_weave_items[seat_index][j]);

			for (int i = 0; i < 4; i++) {
				_hu_weave_items[seat_index][j].weave_card[i] = analyseItem.cbCardData[j][i];
			}

			_hu_weave_count[seat_index] = j + 1;
		}

		int add_xi = 0;
		if (analyseItem.curCardEye == true) {
			if (analyseItem.cbCardEye > GameConstants.CARD_ESPECIAL_TYPE_NIAO) {
				if (seat_index == provider_index) {
					_hu_weave_items[seat_index][_hu_weave_count[seat_index]].hu_xi = 4 + add_xi;
					_hu_weave_items[seat_index][_hu_weave_count[seat_index]].weave_kind = GameConstants.WIK_DAN_WAI;
				} else {
					_hu_weave_items[seat_index][_hu_weave_count[seat_index]].hu_xi = 1 + add_xi;
					_hu_weave_items[seat_index][_hu_weave_count[seat_index]].weave_kind = GameConstants.WIK_DAN_PENG;
				}
				_hu_weave_items[seat_index][_hu_weave_count[seat_index]].weave_card[0] = analyseItem.cbCardEye
						- GameConstants.CARD_ESPECIAL_TYPE_NIAO;
				_hu_weave_items[seat_index][_hu_weave_count[seat_index]].weave_card[1] = analyseItem.cbCardEye
						- GameConstants.CARD_ESPECIAL_TYPE_NIAO;
			} else {
				_hu_weave_items[seat_index][_hu_weave_count[seat_index]].hu_xi = 1;
				_hu_weave_items[seat_index][_hu_weave_count[seat_index]].weave_kind = GameConstants.WIK_DUI_ZI;
				_hu_weave_items[seat_index][_hu_weave_count[seat_index]].weave_card[0] = analyseItem.cbCardEye;
				_hu_weave_items[seat_index][_hu_weave_count[seat_index]].weave_card[1] = analyseItem.cbCardEye;
			}

			_hu_weave_items[seat_index][_hu_weave_count[seat_index]].center_card = analyseItem.cbCardEye;

			_hu_weave_count[seat_index]++;

		} else if (analyseItem.cbMenEye[0] != 0) {

			// 胡红色门子算两息，红色门子算一息
			if (analyseItem.cbMenEye[0] > GameConstants.CARD_ESPECIAL_TYPE_NIAO
					|| analyseItem.cbMenEye[1] > GameConstants.CARD_ESPECIAL_TYPE_NIAO) {
				_hu_weave_items[seat_index][_hu_weave_count[seat_index]].weave_kind = GameConstants.WIK_YYWHZ_MENZI_GUANG;
				this.GRR._count_pick_niao = GameConstants.WIK_YYWHZ_MENZI_GUANG;
				if (analyseItem.cbMenEye[0] > GameConstants.CARD_ESPECIAL_TYPE_NIAO) {
					analyseItem.cbMenEye[0] -= GameConstants.CARD_ESPECIAL_TYPE_NIAO;
					if (analyseItem.cbMenEye[0] % 16 == 2 || analyseItem.cbMenEye[0] % 16 == 7
							|| analyseItem.cbMenEye[0] % 16 == 10) {
						if (analyseItem.cbMenEye[1] % 16 == 2 || analyseItem.cbMenEye[1] % 16 == 7
								|| analyseItem.cbMenEye[1] % 16 == 10) {
							_hu_weave_items[seat_index][_hu_weave_count[seat_index]].hu_xi = 1 + add_xi;
						} else {
							_hu_weave_items[seat_index][_hu_weave_count[seat_index]].hu_xi = 0 + add_xi;
						}
					} else {
						_hu_weave_items[seat_index][_hu_weave_count[seat_index]].hu_xi = 0 + add_xi;
					}
				}
				if (analyseItem.cbMenEye[1] > GameConstants.CARD_ESPECIAL_TYPE_NIAO) {
					analyseItem.cbMenEye[1] -= GameConstants.CARD_ESPECIAL_TYPE_NIAO;
					if (analyseItem.cbMenEye[0] % 16 == 2 || analyseItem.cbMenEye[0] % 16 == 7
							|| analyseItem.cbMenEye[0] % 16 == 10) {
						if (analyseItem.cbMenEye[1] % 16 == 2 || analyseItem.cbMenEye[1] % 16 == 7
								|| analyseItem.cbMenEye[1] % 16 == 10) {
							_hu_weave_items[seat_index][_hu_weave_count[seat_index]].hu_xi = 1 + add_xi;
						} else {
							_hu_weave_items[seat_index][_hu_weave_count[seat_index]].hu_xi = 0 + add_xi;
						}
					} else {
						_hu_weave_items[seat_index][_hu_weave_count[seat_index]].hu_xi = 0 + add_xi;
					}
				}
			} else {
				_hu_weave_items[seat_index][_hu_weave_count[seat_index]].weave_kind = GameConstants.WIK_YYWHZ_MENZI;
				if (analyseItem.cbMenEye[0] % 16 == 2 || analyseItem.cbMenEye[0] % 16 == 7
						|| analyseItem.cbMenEye[0] % 16 == 10) {
					if (analyseItem.cbMenEye[1] % 16 == 2 || analyseItem.cbMenEye[1] % 16 == 7
							|| analyseItem.cbMenEye[1] % 16 == 10) {
						_hu_weave_items[seat_index][_hu_weave_count[seat_index]].hu_xi = 1;
					} else {
						_hu_weave_items[seat_index][_hu_weave_count[seat_index]].hu_xi = 0;
					}
				} else {
					_hu_weave_items[seat_index][_hu_weave_count[seat_index]].hu_xi = 0;
				}
			}

			_hu_weave_items[seat_index][_hu_weave_count[seat_index]].weave_card[0] = analyseItem.cbMenEye[0];
			_hu_weave_items[seat_index][_hu_weave_count[seat_index]].weave_card[1] = analyseItem.cbMenEye[1];
			_hu_weave_items[seat_index][_hu_weave_count[seat_index]].center_card = analyseItem.cbMenEye[0];
			_hu_weave_items[seat_index][_hu_weave_count[seat_index]].public_card = analyseItem.cbMenEye[1];

			_hu_weave_count[seat_index]++;
		}
		cbChiHuKind = GameConstants.WIK_CHI_HU;

		return cbChiHuKind;
	}

	/**
	 * 算分
	 * 
	 * @param seat_index
	 * @param provide_index
	 * @param operate_card
	 * @param zimo
	 */
	public void process_chi_hu_player_score_yywhz(int seat_index, int provide_index, int operate_card, boolean zimo) {
		GRR._chi_hu_card[seat_index][0] = operate_card;

		GRR._win_order[seat_index] = 1;

		// 引用权位
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];
		// 计算胡息
		int all_hu_xi = 0;
		int other_xi = 0;
		_fan_shu[seat_index] = 1;

		int calculate_score = 0;

		boolean tian_hu = false;
		boolean is_dahu = false;

		for (int i = 0; i < this._hu_weave_count[seat_index]; i++) {
			all_hu_xi += this._hu_weave_items[seat_index][i].hu_xi;
		}
		if (this.has_rule(GameConstants.GAME_RULE_HU_ADD_XI)) {
			all_hu_xi += 1;
		}
		if (!(chr.opr_and(GameConstants.CHR_TIAN_HU_WHZ_YIYANG)).is_empty()
				|| !(chr.opr_and(GameConstants.CHR_DI_HU_WHZ_YIYANG)).is_empty()) {
			// 天胡
			if (has_rule(GameConstants.GAME_RULE_YIYANG_WHZ_FAN_CAL)) {
				all_hu_xi *= 8;
				this._fan_shu[seat_index] *= 8;
			} else {
				if (!is_dahu) {
					all_hu_xi = 0;
					is_dahu = true;
				}

				all_hu_xi += 50;
			}

		}
		if (!(chr.opr_and(GameConstants.CHR_TING_HU_WHZ_YIYANG)).is_empty()) {
			// 听胡
			if (has_rule(GameConstants.GAME_RULE_YIYANG_WHZ_FAN_CAL)) {
				all_hu_xi *= 4;
				this._fan_shu[seat_index] *= 4;
			} else {
				if (!is_dahu) {
					all_hu_xi = 0;
					is_dahu = true;
				}
				all_hu_xi += 50;
			}
		}
		if (!(chr.opr_and(GameConstants.CHR_YI_DIAN_HONG_WHZ_YIYANG)).is_empty()) {
			// 一点红
			if (has_rule(GameConstants.GAME_RULE_YIYANG_WHZ_FAN_CAL)) {
				all_hu_xi *= 4;
				this._fan_shu[seat_index] *= 4;
			} else {
				if (!is_dahu) {
					all_hu_xi = 0;
					is_dahu = true;
				}
				all_hu_xi += 50;
			}
		}

		if (!(chr.opr_and(GameConstants.CHR_QUAN_HEI_WHZ_YIYANG)).is_empty()) {
			// 全黑
			if (has_rule(GameConstants.GAME_RULE_YIYANG_WHZ_FAN_CAL)) {
				all_hu_xi *= 8;
				this._fan_shu[seat_index] *= 8;
			} else {
				if (!is_dahu) {
					all_hu_xi = 0;
					is_dahu = true;
				}
				all_hu_xi += 100;
			}
		}
		if (!(chr.opr_and(GameConstants.CHR_QUAN_DA_WHZ_YIYANG)).is_empty()) {
			// 全大
			if (has_rule(GameConstants.GAME_RULE_YIYANG_WHZ_FAN_CAL)) {
				all_hu_xi *= 8;
				this._fan_shu[seat_index] *= 8;
			} else {
				if (!is_dahu) {
					all_hu_xi = 0;
					is_dahu = true;
				}
				all_hu_xi += 100;
			}
		}
		if (!(chr.opr_and(GameConstants.CHR_QUAN_XIAO_WHZ_YIYANG)).is_empty()) {
			// 全小
			if (has_rule(GameConstants.GAME_RULE_YIYANG_WHZ_FAN_CAL)) {
				all_hu_xi *= 8;
				this._fan_shu[seat_index] *= 8;
			} else {
				if (!is_dahu) {
					all_hu_xi = 0;
					is_dahu = true;
				}
				all_hu_xi += 100;
			}
		}

		if (!(chr.opr_and(GameConstants.CHR_HANG_HANG_XI_WHZ_YIYANG)).is_empty()) {
			// 行行息
			if (has_rule(GameConstants.GAME_RULE_YIYANG_WHZ_FAN_CAL)) {
				all_hu_xi *= 4;
				this._fan_shu[seat_index] *= 4;
			} else {
				if (!is_dahu) {
					all_hu_xi = 0;
					is_dahu = true;
				}
				all_hu_xi += 50;
			}
		}
		if (!(chr.opr_and(GameConstants.CHR_DUI_ZI_XI_WHZ_YIYANG)).is_empty()) {
			// 对子息
			if (has_rule(GameConstants.GAME_RULE_YIYANG_WHZ_FAN_CAL)) {
				all_hu_xi *= 8;
				this._fan_shu[seat_index] *= 8;
			} else {
				if (!is_dahu) {
					all_hu_xi = 0;
					is_dahu = true;
				}
				all_hu_xi += 100;
			}
		}
		if (!(chr.opr_and(GameConstants.CHR_HUO_HUO_WHZ_YIYANG)).is_empty()) {
			// 火火翻
			int hong_count = _logic.calculate_hong_pai_count_yiyang(_hu_weave_items[seat_index],
					_hu_weave_count[seat_index]);
			all_hu_xi *= Math.pow(2, hong_count - 9);
			this._fan_shu[seat_index] *= Math.pow(2, hong_count - 9);
		}
		// 花胡子
		if (!(chr.opr_and(GameConstants.CHR_HAI_HUA_HUZI_WHZ_YIYANG)).is_empty()) {
			all_hu_xi *= 64;
			this._fan_shu[seat_index] *= 64;
		}
		// 印
		if (!(chr.opr_and(GameConstants.CHR_HAI_YIN_WHZ_YIYANG)).is_empty()) {
			all_hu_xi *= 2;
			this._fan_shu[seat_index] *= 2;
		}
		// 双飘
		if (!(chr.opr_and(GameConstants.CHR_HAI_SHUANG_PIAO_WHZ_YIYANG)).is_empty()) {
			all_hu_xi *= 2;
			this._fan_shu[seat_index] *= 2;
		}
		// 单飘
		if (!(chr.opr_and(GameConstants.CHR_HAI_DAN_PIAO_WHZ_YIYANG)).is_empty()) {
			all_hu_xi *= 2;
			this._fan_shu[seat_index] *= 2;
		}

		if (!(chr.opr_and(GameConstants.CHR_HAI_LAO_WHZ_YIYANG)).is_empty()) {
			// 海底捞
			if (has_rule(GameConstants.GAME_RULE_YIYANG_WHZ_HAIDI_2)) {
				all_hu_xi *= 2;
			} else if (has_rule(GameConstants.GAME_RULE_YIYANG_WHZ_HAIDI_4)) {
				all_hu_xi *= 4;
			} else {
				all_hu_xi *= 4;
			}
			if (this._shen_pai % 2 == _repertory_card[_all_card_len - GRR._left_card_count - 1] % 2) {
				all_hu_xi *= 2;
			}

		} else {
			if (_all_card_len - GRR._left_card_count >= 0) {
				if (_repertory_card[_all_card_len - GRR._left_card_count] % 2 == this._shen_pai % 2) {
					all_hu_xi *= 2;
				}
			}
		}

		for (int j = 0; j < _hu_weave_count[seat_index]; j++) {

			if (_hu_weave_items[seat_index][j].weave_kind == GameConstants.WIK_YYWHZ_MENZI_GUANG) {
				this.GRR._count_pick_niao = _hu_weave_items[seat_index][j].weave_kind;
			} else {
				if (_hu_weave_items[seat_index][j].center_card > GameConstants.CARD_ESPECIAL_TYPE_NIAO) {
					this.GRR._count_pick_niao = _hu_weave_items[seat_index][j].weave_kind;
					_hu_weave_items[seat_index][j].center_card -= GameConstants.CARD_ESPECIAL_TYPE_NIAO;
				}
			}

		}

		int nwaihao = 0;

		int have_card_index[] = new int[GameConstants.MAX_HH_INDEX];
		for (int j = 0; j < 7; j++) {
			for (int i = 0; i < 4; i++) {
				if (_hu_weave_items[seat_index][j].weave_card[i] > 0) {
					int card = _hu_weave_items[seat_index][j].weave_card[i];
					if (card > GameConstants.CARD_ESPECIAL_TYPE_NIAO) {
						card -= GameConstants.CARD_ESPECIAL_TYPE_NIAO;
					}
					have_card_index[this._logic.switch_to_card_index(card)]++;
				}
			}
		}
		// 元
		for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
			int hand_count = _hand_card_index[seat_index][i];

			// 外元
			if (have_card_index[i] == 4) {
				if (hand_count == 4) {
					all_hu_xi *= 4;
					_hu_xi[seat_index] *= 4;
				} else {
					boolean is_wai_yuan = false;
					for (int j = 0; j < 7; j++) {
						if ((_hu_weave_items[seat_index][j].weave_kind == GameConstants.WIK_YIYANGWHZ_QING_NEI
								|| _hu_weave_items[seat_index][j].weave_kind == GameConstants.WIK_YIYANGWHZ_QING_WAI)
								&& _hu_weave_items[seat_index][j].center_card == this._logic.switch_to_card_data(i)) {
							all_hu_xi *= 4;
							_hu_xi[seat_index] *= 4;
							is_wai_yuan = true;
							break;
						}
						if ((_hu_weave_items[seat_index][j].weave_kind == GameConstants.WIK_YYWHZ_WAI
								&& hand_count == 3)
								&& _hu_weave_items[seat_index][j].center_card == this._logic.switch_to_card_data(i)) {
							all_hu_xi *= 4;
							_hu_xi[seat_index] *= 4;
							is_wai_yuan = true;
							break;
						}
						if ((_hu_weave_items[seat_index][j].weave_kind == GameConstants.WIK_DAN_WAI && hand_count == 3)
								&& _hu_weave_items[seat_index][j].center_card == this._logic.switch_to_card_data(i)) {
							all_hu_xi *= 4;
							_hu_xi[seat_index] *= 4;
							is_wai_yuan = true;
							break;
						}
					}
					if (!is_wai_yuan) {
						all_hu_xi *= 2;
						_hu_xi[seat_index] *= 2;
					}
				}
			}
		}
		if (all_hu_xi > _max_xi_shu) {
			all_hu_xi = _max_xi_shu;
		}
		this._hu_xi[seat_index] = all_hu_xi;
		calculate_score = all_hu_xi * 1;
		_player_result.ying_xi_count[seat_index] += calculate_score;

		float lChiHuScore = calculate_score;

		////////////////////////////////////////////////////// 自摸 算分
		if (zimo) {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index) {
					continue;
				}

				// 胡牌分
				GRR._game_score[i] -= lChiHuScore;
				GRR._game_score[seat_index] += lChiHuScore;
				_player_result.game_score[i] -= lChiHuScore;
				_player_result.game_score[seat_index] += lChiHuScore;
			}
		}

	}

	public boolean estimate_player_chipeng_qing_piao_respond_yywhz(int seat_index, int provider, int card) {
		if (this._is_xiang_gong[seat_index]) {
			return false;
		}
		boolean is_liu = false;
		GangCardResult gangCardResult = new GangCardResult();
		int cbActionMask = estimate_player_qing_nei_respond_yywhz(GRR._cards_index[seat_index],
				GRR._weave_items[seat_index], GRR._weave_count[seat_index], _send_card_data, gangCardResult);

		if (cbActionMask != GameConstants.WIK_NULL) {// 有溜
			for (int i = 0; i < gangCardResult.cbCardCount; i++) {
				_playerStatus[seat_index].add_liu(gangCardResult.cbCardData[i], seat_index, gangCardResult.isPublic[i],
						cbActionMask);
				_playerStatus[seat_index].add_action(cbActionMask);// 溜牌
				_playerStatus[seat_index].add_action(GameConstants.WIK_NULL);
				_playerStatus[seat_index].add_pass(card, seat_index);

				_playerStatus[seat_index].set_status(GameConstants.Player_Status_OPR_CARD);// 操作状态

			}
			is_liu = true;
		}
		// 外溜
		gangCardResult.cbCardCount = 0;
		cbActionMask = estimate_player_qing_wai_respond_yywhz(GRR._cards_index[seat_index],
				GRR._weave_items[seat_index], GRR._weave_count[seat_index], 0, gangCardResult, false);

		if (cbActionMask != GameConstants.WIK_NULL) {// 有溜
			for (int i = 0; i < gangCardResult.cbCardCount; i++) {
				_playerStatus[seat_index].add_liu(gangCardResult.cbCardData[i], seat_index, gangCardResult.isPublic[i],
						cbActionMask);
				_playerStatus[seat_index].add_action(cbActionMask);// 溜牌
				_playerStatus[seat_index].add_action(GameConstants.WIK_NULL);
				_playerStatus[seat_index].add_pass(card, seat_index);

				_playerStatus[seat_index].set_status(GameConstants.Player_Status_OPR_CARD);// 操作状态

			}
			is_liu = true;
		}

		// 飘
		gangCardResult.cbCardCount = 0;
		cbActionMask = estimate_player_piao_respond_yywhz(GRR._cards_index[seat_index], GRR._weave_items[seat_index],
				GRR._weave_count[seat_index], 0, gangCardResult, seat_index, provider, false);
		if (cbActionMask != GameConstants.WIK_NULL) {// 有飘
			for (int i = 0; i < gangCardResult.cbCardCount; i++) {
				_playerStatus[seat_index].add_liu(gangCardResult.cbCardData[i], seat_index, gangCardResult.isPublic[i],
						cbActionMask);
				_playerStatus[seat_index].add_action(cbActionMask);// 飘牌
				_playerStatus[seat_index].add_action(GameConstants.WIK_NULL);
				_playerStatus[seat_index].add_pass(card, seat_index);

				_playerStatus[seat_index].set_status(GameConstants.Player_Status_OPR_CARD);// 操作状态

			}
			is_liu = true;
		}
		return is_liu;
	}

	//
	public boolean estimate_player_dispatch_qing_piao_respond_yywhz(int seat_index, int card) {
		boolean bAroseAction = false;// 出现(是否)有
		// 玩家溜
		GangCardResult gangCardResult = new GangCardResult();
		int cbActionMask = estimate_player_qing_wai_respond_yywhz(GRR._cards_index[seat_index],
				GRR._weave_items[seat_index], GRR._weave_count[seat_index], card, gangCardResult, true);
		if (!this._is_xiang_gong[seat_index]) {
			if (cbActionMask != GameConstants.WIK_NULL) {// 有溜
				for (int i = 0; i < gangCardResult.cbCardCount; i++) {
					_playerStatus[seat_index].add_liu(gangCardResult.cbCardData[i], seat_index,
							gangCardResult.isPublic[i], cbActionMask);
					_playerStatus[seat_index].add_action(cbActionMask);// 溜牌
					_playerStatus[seat_index].add_action(GameConstants.WIK_NULL);
					_playerStatus[seat_index].add_pass(card, seat_index);

					_playerStatus[seat_index].set_status(GameConstants.Player_Status_OPR_CARD);// 操作状态

				}
			}
		}

		// 飘
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (this._is_xiang_gong[i]) {
				continue;
			}
			gangCardResult.cbCardCount = 0;
			cbActionMask = estimate_player_piao_respond_yywhz(GRR._cards_index[i], GRR._weave_items[seat_index],
					GRR._weave_count[i], card, gangCardResult, i, seat_index, true);
			if (cbActionMask != GameConstants.WIK_NULL) {// 有飘
				for (int j = 0; j < gangCardResult.cbCardCount; j++) {
					_playerStatus[i].add_liu(gangCardResult.cbCardData[j], i, gangCardResult.isPublic[j], cbActionMask);
					_playerStatus[i].add_action(cbActionMask);// 飘牌
					_playerStatus[i].add_action(GameConstants.WIK_NULL);
					_playerStatus[i].add_pass(card, i);

					_playerStatus[i].set_status(GameConstants.Player_Status_OPR_CARD);// 操作状态

				}
				bAroseAction = true;
			}
		}

		return bAroseAction;
	}

	public int estimate_player_piao_respond_yywhz(int cards_index[], WeaveItem WeaveItem[], int cbWeaveCount,
			int cur_card, GangCardResult gangCardResult, int seat_index, int provider, boolean is_dispath) {
		int cbActionMask = GameConstants.WIK_NULL;
		return _logic.check_piao_yiyangwhz(cards_index, WeaveItem, cbWeaveCount, cur_card, gangCardResult, seat_index,
				provider, is_dispath);
	}

	public int estimate_player_qing_nei_respond_yywhz(int cards_index[], WeaveItem WeaveItem[], int cbWeaveCount,
			int cur_card, GangCardResult gangCardResult) {
		int cbActionMask = GameConstants.WIK_NULL;

		return _logic.check_qing_nei_yiyangwhz(cards_index, WeaveItem, cbWeaveCount, cur_card, gangCardResult);
	}

	public int estimate_player_qing_wai_respond_yywhz(int cards_index[], WeaveItem WeaveItem[], int cbWeaveCount,
			int cur_card, GangCardResult gangCardResult, boolean is_dispath) {
		int cbActionMask = GameConstants.WIK_NULL;

		return _logic.check_qing_wai_yiyangwhz(cards_index, WeaveItem, cbWeaveCount, cur_card, gangCardResult,
				is_dispath);
	}

	public int estimate_player_wai_respond_yywhz(int seat_index, int card_data) {
		if (this._is_xiang_gong[seat_index]) {
			return GameConstants.WIK_NULL;
		}
		int bAroseAction = GameConstants.WIK_NULL;
		// 歪牌判断
		if ((bAroseAction == GameConstants.WIK_NULL)
				&& (_logic.check_wai_yywhz(this.GRR._cards_index[seat_index], card_data) != GameConstants.WIK_NULL)) {
			int action = GameConstants.WIK_YYWHZ_WAI;
			bAroseAction = GameConstants.WIK_YYWHZ_WAI;
		}
		return bAroseAction;
	}

	// 玩家出牌的胡的检测
	public boolean estimate_player_out_card_hu_yywhz(int seat_index, int card, WeaveItem weaveItems[], int _weave_count,
			boolean bDisdatch) {
		// 动作判断
		boolean bAroseAction = false;// 出现(是否)有
		int action = GameConstants.WIK_NULL;
		int player_pass[] = new int[this.getTablePlayerNumber()];
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			// 用户过滤
			if (seat_index == i)
				continue;
			// 胡牌判断
			if (_playerStatus[i].lock_huan_zhang() == false) {
				ChiHuRight chr = GRR._chi_hu_rights[seat_index];
				int card_type = GameConstants.HU_CARD_TYPE_ZIMO;
				int hu_xi_chi[] = new int[1];
				hu_xi_chi[0] = 0;
				int action_hu = analyse_chi_hu_card(GRR._cards_index[i], GRR._weave_items[i], GRR._weave_count[i], i,
						seat_index, card, chr, card_type, hu_xi_chi, false);
				if (action_hu != GameConstants.WIK_NULL) {
					_playerStatus[i].add_action(action);
					_playerStatus[i].add_chi_hu(action_hu, seat_index);
					bAroseAction = true;
					player_pass[i] = 1;
					chr.opr_or(GameConstants.CHR_SHU_FAN);
					if (this._out_card_count == 1) {
						chr.opr_or(GameConstants.CHR_DI_HU_WHZ);
					}
				}
			}

		}
		return bAroseAction;
	}

	// 检查长沙麻将,杠牌
	public boolean estimate_gang_hh_respond(int seat_index, int provider, int card, boolean d, boolean check_chi) {
		// 变量定义
		boolean bAroseAction = false;// 出现(是否)有

		return bAroseAction;
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
			return handler_player_ready(get_seat_index, is_cancel);
		}
	}

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
		int count = getTablePlayerNumber();
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
			playerNumber = this.getTablePlayerNumber();
		}
		handler_game_start();

		this.refresh_room_redis_data(GameConstants.PROXY_ROOM_UPDATE, nt);
		return false;
	}

	@Override
	public boolean handler_player_be_in_room(int seat_index) {
		this.log_error("gme_status:" + this._game_status + " seat_index:" + seat_index);
		if ((GameConstants.GS_MJ_FREE != _game_status && GameConstants.GS_MJ_WAIT != _game_status)
				&& this.get_players()[seat_index] != null) {
			// this.send_play_data(seat_index);

			this.log_error("gme_status:" + this._game_status + "GS_MJ_WAIT  seat_index:" + seat_index);
			if (this._handler != null)
				this._handler.handler_player_be_in_room(this, seat_index);

			send_shen_card(seat_index);

		}
		if (_gameRoomRecord != null) {
			if (_gameRoomRecord.request_player_seat != GameConstants.INVALID_SEAT) {
				RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
				roomResponse.setType(MsgConstants.RESPONSE_PLAYER_RELEASE);

				SysParamModel sysParamModel3007 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(1)
						.get(3007);
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
				this.send_response_to_player(seat_index, roomResponse);
			}
		}
		if (this._cur_round > 0) {
			return handler_player_ready(seat_index, false);
		} else if (this.has_rule(GameConstants.GAME_RULE_CAN_LESS)) {
			return handler_player_ready(seat_index, false);
		}

		return true;

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
	public boolean handler_operate_out_card_mul(int get_seat_index, List<Integer> list, int card_count,
			int b_out_card) {
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

		if (this._handler != null) {
			this._handler.handler_operate_card(this, seat_index, operate_code, operate_card, luoCode);
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

	/**
	 * 释放
	 */
	@Override
	public boolean process_release_room() {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_FORCE_EXIT);
		this.send_response_to_room(roomResponse);

		if (_cur_round == 0) {
			Player player = null;
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				player = this.get_players()[i];
				if (player == null)
					continue;
				send_error_notify(i, 2, "游戏等待超时解散");

			}
		} else {
			this.handler_game_finish(GameConstants.INVALID_SEAT, GameConstants.Game_End_RELEASE_PLAY);

		}

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			this.get_players()[i] = null;

		}

		if (_table_scheduled != null)
			_table_scheduled.cancel(false);

		// 删除房间
		PlayerServiceImpl.getInstance().delRoomId(this.getRoom_id());

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

		// 手牌删掉
		// this.operate_player_cards(seat_index, 0, null, 0, null);

		// if (rm) {
		// // 把摸的牌从手牌删掉,结算的时候不显示这张牌的
		// GRR._cards_index[seat_index][_logic.switch_to_card_index(operate_card)]--;
		// }

		// 显示胡牌
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			int cards[] = new int[this.get_hand_card_count_max()];
			int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[i], cards);

			this.operate_show_card(i, GameConstants.Show_Card_HU, hand_card_count, cards, this.GRR._weave_items[i],
					this.GRR._weave_count[i], GameConstants.INVALID_SEAT);

		}
		this.operate_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_HU, 1, chr.type_list, 1,
				GameConstants.INVALID_SEAT);
		return;
	}

	/**
	 * 长沙
	 * 
	 * @param seat_index
	 * @param operate_card
	 * @param rm
	 */
	@Override
	public void process_chi_hu_player_operate_hh(int seat_index, int operate_card[], int card_count, boolean rm) {
		// 引用权位
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];

		// 效果
		this.operate_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_HU, chr.type_count, chr.type_list, 1,
				GameConstants.INVALID_SEAT);

		// 手牌删掉
		this.operate_player_cards(seat_index, 0, null, 0, null);

		if (rm) {
			// 把摸的牌从手牌删掉,结算的时候不显示这张牌的
			for (int i = 0; i < card_count; i++) {
				GRR._cards_index[seat_index][_logic.switch_to_card_index(operate_card[i])]--;
			}
		}

		// 显示胡牌
		int cards[] = new int[GameConstants.MAX_HH_INDEX];
		int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[seat_index], cards);
		for (int i = 0; i < card_count; i++) {
			cards[hand_card_count++] = operate_card[i] + GameConstants.CARD_ESPECIAL_TYPE_HU;
		}
		this.operate_show_card(seat_index, GameConstants.Show_Card_HU, hand_card_count, cards,

				this.GRR._weave_items[seat_index], this.GRR._weave_count[seat_index], GameConstants.INVALID_SEAT);

		return;
	}

	/**
	 * 统计胡牌类型
	 * 
	 * @param _seat_index
	 */
	@Override
	public void countChiHuTimes(int _seat_index, boolean isZimo) {
		ChiHuRight chiHuRight = GRR._chi_hu_rights[_seat_index];
		if (isZimo) {
			_player_result.hu_pai_count[_seat_index]++;
		}
		if (!(chiHuRight.opr_and(GameConstants.CHR_TIAN_HU_WHZ)).is_empty()) {
			_player_result.ming_tang_count[_seat_index]++;
		}
		if (!(chiHuRight.opr_and(GameConstants.CHR_DI_HU_WHZ)).is_empty()) {
			_player_result.ming_tang_count[_seat_index]++;
		}
		if (!(chiHuRight.opr_and(GameConstants.CHR_TING_HU_WHZ)).is_empty()) {
			_player_result.ming_tang_count[_seat_index]++;
		}
		if (!(chiHuRight.opr_and(GameConstants.CHR_SHI_SAN_HONG_WHZ)).is_empty()) {
			_player_result.ming_tang_count[_seat_index]++;
		}
		if (!(chiHuRight.opr_and(GameConstants.CHR_YI_DIAN_HONG_WHZ)).is_empty()) {
			_player_result.ming_tang_count[_seat_index]++;
		}

		if (!(chiHuRight.opr_and(GameConstants.CHR_QUAN_HEI_WHZ)).is_empty()) {
			_player_result.ming_tang_count[_seat_index]++;
		}
		if (!(chiHuRight.opr_and(GameConstants.CHR_QUAN_DA_WHZ)).is_empty()) {
			_player_result.ming_tang_count[_seat_index]++;
		}
		if (!(chiHuRight.opr_and(GameConstants.CHR_QUAN_XIAO_WHZ)).is_empty()) {
			_player_result.ming_tang_count[_seat_index]++;
		}
		if (!(chiHuRight.opr_and(GameConstants.CHR_WU_DUI_WHZ)).is_empty()) {
			_player_result.ming_tang_count[_seat_index]++;
		}
		if (!(chiHuRight.opr_and(GameConstants.CHR_SHI_DUI_WHZ)).is_empty()) {
			_player_result.ming_tang_count[_seat_index]++;
		}
		if (!(chiHuRight.opr_and(GameConstants.CHR_YI_DUI_WHZ)).is_empty()) {
			_player_result.ming_tang_count[_seat_index]++;
		}
		if (!(chiHuRight.opr_and(GameConstants.CHR_JIU_DUI_WHZ)).is_empty()) {
			_player_result.ming_tang_count[_seat_index]++;
		}
		if (!(chiHuRight.opr_and(GameConstants.CHR_HANG_HANG_XI_WHZ)).is_empty()) {
			_player_result.ming_tang_count[_seat_index]++;
		}
		if (!(chiHuRight.opr_and(GameConstants.CHR_DUI_ZI_XI_WHZ)).is_empty()) {
			_player_result.ming_tang_count[_seat_index]++;
		}
		if (!(chiHuRight.opr_and(GameConstants.CHR_HAI_LAO_WHZ)).is_empty()) {
			_player_result.ming_tang_count[_seat_index]++;
		}
		if (!(chiHuRight.opr_and(GameConstants.CHR_QUAN_QIU_REN_WHZ)).is_empty()) {
			_player_result.ming_tang_count[_seat_index]++;
		}

	}

	// 18 玩家申请解散房间( operate_code=1发起解散2=同意解散3=不同意解散)
	/*
	 * 
	 * Release_Room_Type_SEND = 1, //发起解散 Release_Room_Type_AGREE, //同意
	 * Release_Room_Type_DONT_AGREE, //不同意 Release_Room_Type_CANCEL,
	 * //还没开始,房主解散房间 Release_Room_Type_QUIT //还没开始,普通玩家退出房间
	 */
	@Override
	public boolean handler_release_room(Player player, int opr_code) {
		int seat_index = 0;
		if (player != null) {
			seat_index = player.get_seat_index();
		}

		SysParamModel sysParamModel3007 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(1).get(3007);
		int delay = 150;
		if (sysParamModel3007 != null) {
			delay = sysParamModel3007.getVal1();
		}
		if (DEBUG_CARDS_MODE)
			delay = 10;
		int playerNumber = getTablePlayerNumber();
		if (playerNumber == 0) {
			playerNumber = this.getTablePlayerNumber();
		}

		switch (opr_code) {
		case GameConstants.Release_Room_Type_SEND: {
			// 发起解散
			if (GameConstants.GS_MJ_FREE == _game_status) {
				// 游戏还没开始,不能发起解散
				return false;
			}
			// 有人申请解散了
			if (_gameRoomRecord.request_player_seat != GameConstants.INVALID_SEAT) {
				return false;
			}

			// 取消之前的调度
			if (_release_scheduled != null)
				_release_scheduled.cancel(false);
			_request_release_time = System.currentTimeMillis() + delay * 1000;
			if (GRR == null) {
				_release_scheduled = GameSchedule.put(new GameFinishRunnable(this.getRoom_id(), seat_index,
						GameConstants.Game_End_RELEASE_WAIT_TIME_OUT), delay, TimeUnit.SECONDS);
			} else {
				_release_scheduled = GameSchedule.put(new GameFinishRunnable(this.getRoom_id(), seat_index,
						GameConstants.Game_End_RELEASE_PLAY_TIME_OUT), delay, TimeUnit.SECONDS);
			}

			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				_gameRoomRecord.release_players[i] = 0;
			}

			_gameRoomRecord.request_player_seat = seat_index;
			_gameRoomRecord.release_players[seat_index] = 1;// 同意

			// 不在线的玩家默认同意
			// for (int i = 0; i < MJGameConstants.GAME_PLAYER; i++) {
			// Player pl = this.get_players()[i];
			// if(pl==null || pl.isOnline()==false){
			// _gameRoomRecord.release_players[i] = 1;//同意
			// }
			// }
			int count = 0;
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				if (_gameRoomRecord.release_players[i] == 1) {// 都同意了

					count++;
				}
			}
			if (count == this.getTablePlayerNumber()) {
				if (GRR == null) {
					this.handler_game_finish(GameConstants.INVALID_SEAT, GameConstants.Game_End_RELEASE_RESULT);
				} else {
					this.handler_game_finish(GameConstants.INVALID_SEAT, GameConstants.Game_End_RELEASE_PLAY);
				}

				for (int j = 0; j < this.getTablePlayerNumber(); j++) {
					player = this.get_players()[j];
					if (player == null)
						continue;
					send_error_notify(j, 1, "游戏解散成功!");

				}
				return true;
			}

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_PLAYER_RELEASE);
			roomResponse.setReleaseTime(delay);
			roomResponse.setOperateCode(0);
			roomResponse.setRequestPlayerSeat(_gameRoomRecord.request_player_seat);
			roomResponse.setLeftTime(delay);
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				roomResponse.addReleasePlayers(_gameRoomRecord.release_players[i]);
			}
			this.send_response_to_room(roomResponse);

			return false;
		}

		case GameConstants.Release_Room_Type_AGREE: {
			if (GameConstants.GS_MJ_FREE == _game_status) {
				// 游戏还没开始,不能同意解散
				return false;
			}
			// 没人发起解散
			if (_gameRoomRecord.request_player_seat == GameConstants.INVALID_SEAT) {
				return false;
			}
			if (_gameRoomRecord.release_players[seat_index] == 1)
				return false;

			_gameRoomRecord.release_players[seat_index] = 1;

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_PLAYER_RELEASE);
			roomResponse.setReleaseTime(delay);
			roomResponse.setRequestPlayerSeat(_gameRoomRecord.request_player_seat);
			roomResponse.setOperateCode(1);
			roomResponse.setLeftTime((_request_release_time - System.currentTimeMillis()) / 1000);
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				roomResponse.addReleasePlayers(_gameRoomRecord.release_players[i]);
			}

			this.send_response_to_room(roomResponse);

			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				if (_gameRoomRecord.release_players[i] != 1) {
					return false;// 有一个不同意
				}
			}

			for (int j = 0; j < this.getTablePlayerNumber(); j++) {
				player = this.get_players()[j];
				if (player == null)
					continue;
				send_error_notify(j, 1, "游戏解散成功!");

			}

			if (_release_scheduled != null)
				_release_scheduled.cancel(false);
			_release_scheduled = null;
			if (GRR == null) {
				this.handler_game_finish(GameConstants.INVALID_SEAT, GameConstants.Game_End_RELEASE_RESULT);
			} else {
				this.handler_game_finish(GameConstants.INVALID_SEAT, GameConstants.Game_End_RELEASE_PLAY);
			}

			return false;
		}

		case GameConstants.Release_Room_Type_DONT_AGREE: {
			if (GameConstants.GS_MJ_FREE == _game_status) {
				// 游戏还没开始,不能同意解散
				return false;
			}
			if (_gameRoomRecord.request_player_seat == GameConstants.INVALID_SEAT) {
				return false;
			}
			_gameRoomRecord.release_players[seat_index] = 2;
			_gameRoomRecord.request_player_seat = GameConstants.INVALID_SEAT;

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setReleaseTime(delay);
			roomResponse.setType(MsgConstants.RESPONSE_PLAYER_RELEASE);
			roomResponse.setRequestPlayerSeat(_gameRoomRecord.request_player_seat);
			roomResponse.setOperateCode(1);
			int l = (int) (_request_release_time - System.currentTimeMillis() / 1000);
			if (l <= 0) {
				l = 1;
			}
			roomResponse.setLeftTime(l);
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				roomResponse.addReleasePlayers(_gameRoomRecord.release_players[i]);
			}
			this.send_response_to_room(roomResponse);

			_request_release_time = 0;
			_gameRoomRecord.request_player_seat = GameConstants.INVALID_SEAT;
			if (_release_scheduled != null)
				_release_scheduled.cancel(false);
			_release_scheduled = null;
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				_gameRoomRecord.release_players[i] = 0;
			}

			for (int j = 0; j < this.getTablePlayerNumber(); j++) {
				player = this.get_players()[j];
				if (player == null)
					continue;
				send_error_notify(j, 1, "游戏解散失败!玩家[" + this.get_players()[seat_index].getNick_name() + "]不同意解散");

			}

			return false;
		}

		case GameConstants.Release_Room_Type_CANCEL: {// 房主未开始游戏 解散
			if (GameConstants.GS_MJ_FREE != _game_status) {
				// 游戏已经来时
				return false;
			}
			if (_cur_round == 0) {
				// _gameRoomRecord.request_player_seat =
				// MJGameConstants.INVALID_SEAT;

				RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
				roomResponse.setType(MsgConstants.RESPONSE_FORCE_EXIT);// 直接拉出游戏
				this.send_response_to_room(roomResponse);

				for (int i = 0; i < this.getTablePlayerNumber(); i++) {
					Player p = this.get_players()[i];
					if (p == null)
						continue;
					if (i == seat_index) {
						send_error_notify(i, 2, "游戏已解散");
					} else {
						send_error_notify(i, 2, "游戏已被创建者解散");
					}

				}
				this.huan_dou(GameConstants.Game_End_RELEASE_NO_BEGIN);
				// 删除房间
				PlayerServiceImpl.getInstance().delRoomId(this.getRoom_id());
			} else {
				return false;

			}
		}
			break;
		case GameConstants.Release_Room_Type_QUIT: {// 玩家未开始游戏 退出
			if (GameConstants.GS_MJ_FREE != _game_status) {
				// 游戏已经开始
				return false;
			}
			RoomResponse.Builder quit_roomResponse = RoomResponse.newBuilder();
			quit_roomResponse.setType(MsgConstants.RESPONSE_FORCE_EXIT);// 直接拉出游戏
			send_response_to_player(seat_index, quit_roomResponse);

			send_error_notify(seat_index, 2, "您已退出该游戏");
			this.get_players()[seat_index] = null;
			_player_ready[seat_index] = 0;
			_player_open_less[seat_index] = 0;
			if (player != null) {
				PlayerServiceImpl.getInstance().quitRoomId(this.getRoom_id(), player.getAccount_id());
			}
			if (_kick_schedule != null) {
				_kick_schedule.cancel(false);
				_kick_schedule = null;
			}
			// 刷新玩家
			RoomResponse.Builder refreshroomResponse = RoomResponse.newBuilder();
			refreshroomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);
			this.load_player_info_data(refreshroomResponse);
			//
			send_response_to_other(seat_index, refreshroomResponse);

			// 通知代理
			this.refresh_room_redis_data(GameConstants.PROXY_ROOM_PLAYER, false);
		}
			break;
		case GameConstants.Release_Room_Type_PROXY: {
			// 游戏还没开始,不能解散
			if (GameConstants.GS_MJ_FREE != _game_status) {
				return false;
			}

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_FORCE_EXIT);// 直接拉出游戏
			this.send_response_to_room(roomResponse);

			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				Player p = this.get_players()[i];
				if (p == null)
					continue;
				send_error_notify(i, 2, "游戏已被创建者解散");

			}
			this.huan_dou(GameConstants.Game_End_RELEASE_NO_BEGIN);
			// 删除房间
			PlayerServiceImpl.getInstance().delRoomId(this.getRoom_id());

		}
			break;
		}

		return true;

	}

	@Override
	public boolean handler_requst_pao_qiang(Player player, int pao, int qiang) {

		return false;

	}

	@Override
	public boolean handler_requst_nao_zhuang(Player player, int nao) {

		return true;
	}

	/////////////////////////////////////////////////////// send///////////////////////////////////////////
	/////
	/**
	 * 基础状态
	 * 
	 * @return
	 */
	@Override
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
	@Override
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
	@Override
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

		roomResponse.setFlashTime(100);
		roomResponse.setStandTime(600);

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

	@Override
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
		if (is_mj_type(GameConstants.GAME_TYPE_HENAN_AY)) {
			roomResponse.setFlashTime(250);
			roomResponse.setStandTime(250);
		} else {
			roomResponse.setFlashTime(150);
			roomResponse.setStandTime(400);
		}

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

	// 显示在玩家前面的牌,小胡牌,摸杠牌
	@Override
	public boolean operate_show_card(int seat_index, int type, int count, int cards[], WeaveItem weaveitems[],
			int weave_count, int to_player) {
		//// 发送玩家出牌 201 =玩家出牌(CMD_OutCard: operate_player,operate_card)
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		this.load_common_status(roomResponse);
		roomResponse.setType(MsgConstants.RESPONSE_SHOW_CARD);
		roomResponse.setTarget(seat_index);
		roomResponse.setCardType(type);// 出牌
		roomResponse.setCardCount(count);

		for (int i = 0; i < count; i++) {

			roomResponse.addCardData(cards[i]);
		}
		int di_cards[];
		di_cards = new int[GRR._left_card_count];
		int k = 0;
		int left_card_count = GRR._left_card_count;
		for (int i = _all_card_len - GRR._left_card_count; i < _all_card_len; i++) {
			di_cards[k] = _repertory_card[_all_card_len - left_card_count];
			roomResponse.addCardsList(di_cards[k]);
			k++;
			left_card_count--;
		}
		// 已有 组合牌
		if (weave_count > 0) {
			for (int j = 0; j < weave_count; j++) {
				WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
				weaveItem_item.setProvidePlayer(weaveitems[j].provide_player);
				weaveItem_item.setPublicCard(weaveitems[j].public_card);
				weaveItem_item.setWeaveKind(weaveitems[j].weave_kind);
				weaveItem_item.setHuXi(weaveitems[j].hu_xi);
				weaveItem_item.setCenterCard(weaveitems[j].center_card);

				roomResponse.addWeaveItems(weaveItem_item);
			}
		}
		if (GRR != null) {
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				roomResponse.addScore(GRR._game_score[i]);
			}
		}
		// 手牌中的组合牌
		//
		// if (hu_weave_count > 0) {
		// for (int j = 0; j < hu_weave_count; j++) {
		// WeaveItemResponse.Builder weaveItem_item =
		// WeaveItemResponse.newBuilder();
		// weaveItem_item.setProvidePlayer(hu_weave_items[j].provide_player);
		// weaveItem_item.setPublicCard(hu_weave_items[j].public_card);
		// weaveItem_item.setWeaveKind(hu_weave_items[j].weave_kind);
		// weaveItem_item.setHuXi(weaveitems[j].hu_xi);
		// all_hu_xi += hu_weave_items[j].hu_xi;
		// weaveItem_item.setCenterCard(hu_weave_items[j].center_card);
		// roomResponse.addWeaveItems(weaveItem_item);
		// }
		// }
		//
		GRR.add_room_response(roomResponse);
		// roomResponse.setHuXiCount(all_hu_xi);
		if (to_player == GameConstants.INVALID_SEAT) {
			return this.send_response_to_room(roomResponse);
		} else {
			return this.send_response_to_player(to_player, roomResponse);
		}
	}

	// 添加牌到牌队
	private boolean operate_add_discard(int seat_index, int count, int cards[]) {
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

	// 不能出的牌
	@Override
	public boolean operate_cannot_card(int seat_index) {
		// RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		// // 刷新手牌
		// int cards[] = new int[this.get_hand_card_count_max()];
		// // 刷新自己手牌
		// int count =
		// _logic.switch_to_cards_data(GRR._cannot_out_index[seat_index],
		// cards);
		// roomResponse.setType(MsgConstants.RESPONSE_CANNOT_OUT_CARD);
		// roomResponse.setTarget(seat_index);
		// roomResponse.setCardCount(count);
		// for (int i = 0; i < count; i++) {
		// roomResponse.addCardData(cards[i]);
		// }
		// GRR.add_room_response(roomResponse);
		// this.send_response_to_player(seat_index,roomResponse);

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
	@Override
	public boolean operate_effect_action(int seat_index, int effect_type, int effect_count, long effect_indexs[],
			int time, int to_player) {
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
			GRR.add_room_response(roomResponse);
			this.send_response_to_room(roomResponse);
		} else {
			this.send_response_to_player(to_player, roomResponse);
		}

		return true;
	}

	@Override
	public boolean operate_dou_liu_zi(int seat_index, boolean win, int deng_shu) {
		if (this.has_rule(GameConstants.GAME_RULE_DOU_LIU_ZI_OFF))
			return true;
		int action = 0;
		if (win == false) {

			action = GameConstants.LZ_DOU_LZ;

		} else {

			action = GameConstants.LZ_WIN_LZ;

		}
		if (this._cur_round == this._game_round) {

			action = GameConstants.LZ_FEN_LZ;

		}

		// 效果
		this.operate_effect_action(seat_index, GameConstants.EFFECT_ACTION_LIU_ZI, 1, new long[] { action }, 1,
				GameConstants.INVALID_SEAT);
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_DOU_LIU_ZI);
		roomResponse.setZongliuzi(this._zong_liu_zi_fen);
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			roomResponse.addDouliuzi(this._liu_zi_fen[i]);

		}
		this.load_player_info_data(roomResponse);
		GRR.add_room_response(roomResponse);
		this.send_response_to_room(roomResponse);
		return true;
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
	public int get_yywhz_ting_card(int[] cards, int cards_index[], WeaveItem weaveItem[], int cbWeaveCount,
			int seat_index, int provate_index) {

		// 复制数据
		PerformanceTimer timer = new PerformanceTimer();
		int cbCardIndexTemp[] = new int[GameConstants.MAX_HH_INDEX];
		for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		ChiHuRight chr = new ChiHuRight();

		int count = 0;
		int cbCurrentCard;

		int mj_count = GameConstants.MAX_HH_INDEX;

		for (int i = 0; i < mj_count; i++) {
			if (this._logic.is_magic_index(i))
				continue;
			cbCurrentCard = _logic.switch_to_card_data(i);
			chr.set_empty();

			int hu_xi[] = new int[1];
			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card(cbCardIndexTemp, weaveItem, cbWeaveCount, seat_index,
					provate_index, cbCurrentCard, chr, GameConstants.HU_CARD_TYPE_ZIMO, hu_xi, true)) {
				cards[count] = cbCurrentCard;

				count++;
			}
		}
		if (timer.get() > 500) {
			this.log_warn("pao huzi  ting card cost time = " + timer.duration() + "  and cards is ="
					+ Arrays.toString(cbCardIndexTemp) + "Arrays weaveItem" + Arrays.toString(weaveItem));
		}
		if (count >= mj_count) {
			count = 1;
			cards[0] = -1;
		}

		return count;
	}

	@Override
	public boolean record_effect_action(int seat_index, int effect_type, int effect_count, long effect_indexs[],
			int time) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_EFFECT_ACTION_RECORD);
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

	/**
	 * 胡息 (通知玩家弹出 吃碰杆 胡牌==效果)
	 * 
	 * @param seat_index
	 * @param hu_xi_kind_type
	 * @param hu_xi_count
	 * @param time
	 * @param to_player
	 * @return
	 */
	// public boolean operate_update_hu_xi(int seat_index, int
	// hu_xi_kind_type,int hu_xi_count, int time, int to_player) {
	// RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
	// roomResponse.setType(MsgConstants.RESPONSE_UPDATE_HU_XI);
	// roomResponse.setHuXiType(hu_xi_kind_type);
	// roomResponse.setHuXiCount(hu_xi_count);
	// roomResponse.setEffectTime(time);
	//
	// if (to_player == GameConstants.INVALID_SEAT) {
	// GRR.add_room_response(roomResponse);
	// this.send_response_to_room(roomResponse);
	// } else {
	// this.send_response_to_player(to_player, roomResponse);
	// }
	//
	// return true;
	// }

	/**
	 * 玩家动作--通知玩家弹出/关闭操作
	 * 
	 * @param seat_index
	 * @param close
	 * @return
	 */
	@Override
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
			int eat_type = GameConstants.WIK_LEFT | GameConstants.WIK_CENTER | GameConstants.WIK_RIGHT
					| GameConstants.WIK_DDX | GameConstants.WIK_XXD | GameConstants.WIK_EQS | GameConstants.WIK_YWS;
			// this.log_error("weave.kind" +
			// curPlayerStatus._action_weaves[i].weave_kind + "center_card"
			// + curPlayerStatus._action_weaves[i].center_card);
			if ((eat_type & curPlayerStatus._action_weaves[i].weave_kind) != 0) {
				for (int j = 0; j < curPlayerStatus._action_weaves[i].lou_qi_count; j++) {
					ChiGroupCard.Builder chi_group = ChiGroupCard.newBuilder();
					weaveItem_item.addChiGroupCard(chi_group);
				}
			}
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
	@Override
	public boolean operate_player_get_card(int seat_index, int count, int cards[], int to_player, boolean sao) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		this.load_common_status(roomResponse);
		roomResponse.setType(MsgConstants.RESPONSE_SEND_CARD);
		roomResponse.setTarget(seat_index);
		roomResponse.setCardType(2);// get牌
		roomResponse.setCardCount(count);

		roomResponse.setFlashTime(150);
		roomResponse.setStandTime(1000);
		roomResponse.setInsertTime(150);

		if (to_player == GameConstants.INVALID_SEAT) {
			for (int i = 0; i < count; i++) {
				roomResponse.addCardData(cards[i]);// 给别人 牌数据
			}

			this.send_response_to_other(seat_index, roomResponse);
			GRR.add_room_response(roomResponse);
			if (seat_index != -1)
				this.send_response_to_player(seat_index, roomResponse);
			return true;

		} else {
			if (seat_index == to_player) {
				for (int i = 0; i < count; i++) {
					roomResponse.addCardData(cards[i]);
				}
				// GRR.add_room_response(roomResponse);
				return this.send_response_to_player(seat_index, roomResponse);
			} else {
				for (int i = 0; i < count; i++) {
					roomResponse.addCardData(cards[i]);// 给别人 牌数据
				}
				// GRR.add_room_response(roomResponse);
				return this.send_response_to_special(seat_index, to_player, roomResponse);
			}
		}

	}

	@Override
	public boolean operate_player_xiang_gong_flag(int seat_index, boolean is_xiang_gong) {

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_IS_XIANGGONG);
		roomResponse.setProvidePlayer(seat_index);
		roomResponse.setIsXiangGong(is_xiang_gong);
		GRR.add_room_response(roomResponse);
		this.send_response_to_room(roomResponse);
		return true;
	}

	@Override
	public boolean operate_player_cards_flag(int seat_index, int card_count, int cards[], int weave_count,
			WeaveItem weaveitems[]) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYER_CARDS);// 12更新玩家手牌数据
		roomResponse.setGameStatus(_game_status);
		roomResponse.setTarget(seat_index);
		roomResponse.setCardType(2);
		GRR.add_room_response(roomResponse);
		// 自己才有牌数据
		this.send_response_to_player(seat_index, roomResponse);

		return true;
	}

	/**
	 * 删除牌堆的牌 (吃碰杆的那张牌 从废弃牌堆上移除)
	 * 
	 * @param seat_index
	 * @param discard_index
	 * @return
	 */

	@Override
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
	@Override
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

	/***
	 * 刷新特殊描述
	 * 
	 * @param txt
	 * @param type
	 * @return
	 */
	@Override
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
	@Override
	public boolean operate_player_score(int seat_index, float[] score) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_GAME_PLAYER_SCORE);
		roomResponse.setTarget(seat_index);
		this.load_common_status(roomResponse);
		this.load_player_info_data(roomResponse);
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			roomResponse.addScore(_player_result.game_score[i]);
			roomResponse.addOpereateScore(score[i]);
		}
		this.send_response_to_room(roomResponse);
		return true;
	}

	@Override
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

	@Override
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

		for (int i = 0; i < getTablePlayerNumber(); i++) {
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
			for (int j = 0; j < GameConstants.MAX_WEAVE_HH; j++) {
				WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
				weaveItem_item.setCenterCard(GRR._weave_items[i][j].center_card);
				weaveItem_item.setProvidePlayer(GRR._weave_items[i][j].provide_player);
				weaveItem_item.setPublicCard(GRR._weave_items[i][j].public_card);
				weaveItem_item.setWeaveKind(GRR._weave_items[i][j].weave_kind);
				// weaveItem_item.setHuXi(GRR._weave_items[i][j].hu_xi);
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
		tableResponse
				.setSendCardData(((_send_card_data != GameConstants.INVALID_VALUE) && (_provide_player == seat_index))
						? _send_card_data : GameConstants.INVALID_VALUE);
		int hand_cards[] = new int[this.get_hand_card_count_max()];
		int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[seat_index], hand_cards);

		if (_status_send == true) {
			// 牌
			if (seat_index == _current_player) {
				_logic.remove_card_by_data(hand_cards, _send_card_data);
			}
		}

		for (int i = 0; i < this.get_hand_card_count_max(); i++) {
			tableResponse.addCardsData(hand_cards[i]);
		}

		roomResponse.setTable(tableResponse);

		this.send_response_to_player(seat_index, roomResponse);

		if (_status_send == true) {
			// 牌
			this.operate_player_get_card(_current_player, 1, new int[] { _send_card_data }, seat_index, false);
		} else {
			if (_out_card_player != GameConstants.INVALID_SEAT && _out_card_data != GameConstants.INVALID_VALUE) {
				this.operate_out_card(_out_card_player, 1, new int[] { _out_card_data },
						GameConstants.OUT_CARD_TYPE_MID, seat_index);
			} else if (_status_cs_gang == true) {
				this.operate_out_card(this._provide_player, 2, this._gang_card_data.get_cards(),
						GameConstants.OUT_CARD_TYPE_MID, seat_index);
			}
		}

		if (_playerStatus[seat_index].has_action()) {
			this.operate_player_action(seat_index, false);
		}

		return true;

	}

	private PlayerResultResponse.Builder process_player_result(int reason) {
		this.huan_dou(reason);
		// 大赢家
		int count = getTablePlayerNumber();
		if (count == 0) {
			count = this.getTablePlayerNumber();
		}
		for (int i = 0; i < count; i++) {
			_player_result.win_order[i] = -1;
		}
		int win_idx = 0;
		float max_score = 0;
		for (int i = 0; i < count; i++) {
			int winner = -1;
			float s = -999999;
			for (int j = 0; j < count; j++) {
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

		for (int i = 0; i < count; i++) {
			player_result.addGameScore(_player_result.game_score[i]);
			player_result.addWinOrder(_player_result.win_order[i]);

			Int32ArrayResponse.Builder lfs = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < count; j++) {
				lfs.addItem(_player_result.lost_fan_shu[i][j]);
			}

			player_result.addLostFanShu(lfs);

			player_result.addHuPaiCount(_player_result.hu_pai_count[i]);
			player_result.addMingTangCount(_player_result.ming_tang_count[i]);
			player_result.addYingXiCount(_player_result.ying_xi_count[i]);
			player_result.addLiuZiFen(_player_result.liu_zi_fen[i]);

			player_result.addPlayersId(i);
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
		return player_result;
	}

	/**
	 * 加载基础状态 癞子 定鬼 牌型状态 玩家状态
	 * 
	 * @param roomResponse
	 */
	@Override
	public void load_common_status(RoomResponse.Builder roomResponse) {
		roomResponse.setCurrentPlayer(_current_player == GameConstants.INVALID_SEAT ? _resume_player : _current_player);
		if (GRR != null) {
			roomResponse.setLeftCardCount(GRR._left_card_count);
			for (int i = 0; i < GRR._especial_card_count; i++) {
				if (_logic.is_ding_gui_card(GRR._especial_show_cards[i])) {
					roomResponse.addEspecialShowCards(
							GRR._especial_show_cards[i] + GameConstants.CARD_ESPECIAL_TYPE_DING_GUI);
				} else if (_logic.is_lai_gen_card(GRR._especial_show_cards[i])) {
					roomResponse.addEspecialShowCards(
							GRR._especial_show_cards[i] + GameConstants.CARD_ESPECIAL_TYPE_LAI_GEN);
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

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			roomResponse.addCardStatus(_playerStatus[i]._card_status);
			roomResponse.addPlayerStatus(_playerStatus[i].get_status());
		}
	}

	// /**
	// * //执行发牌 是否延迟
	// *
	// * @param seat_index
	// * @param delay
	// * @return
	// */
	// public boolean exe_dispatch_last_card(int seat_index, int type, int
	// delay_time) {
	//
	// if (delay_time > 0) {
	// GameSchedule.put(new DispatchLastCardRunnable(this.getRoom_id(),
	// seat_index, type, false), delay_time,
	// TimeUnit.MILLISECONDS);// MJGameConstants.GANG_LAST_CARD_DELAY
	// } else {
	// // 发牌
	// this._handler = this._handler_dispath_last_card;
	// this._handler_dispath_last_card.reset_status(seat_index, type);
	// this._handler.exe(this);
	// }
	//
	// return true;
	// }
	/**
	 * //执行发牌 首牌
	 * 
	 * @param seat_index
	 * @return
	 */
	@Override
	public boolean exe_dispatch_first_card(int seat_index, int type, int delay_time) {
		if (delay_time > 0) {
			GameSchedule.put(new DispatchFirstCardRunnable(this.getRoom_id(), seat_index, type, false), delay_time,
					TimeUnit.MILLISECONDS);// MJGameConstants.GANG_LAST_CARD_DELAY
		} else {
			// 发牌
			this._handler = this._handler_dispath_firstcards;
			this._handler_dispath_firstcards.reset_status(seat_index, type);
			this._handler.exe(this);
		}

		return true;
	}

	/**
	 * //处理首牌
	 * 
	 * @param seat_index
	 * @param delay_time
	 * @return
	 */
	@Override
	public boolean exe_chuli_first_card(int seat_index, int type, int delay_time) {

		if (delay_time > 0) {
			GameSchedule.put(new ChulifirstCardRunnable(this.getRoom_id(), seat_index, type, false), delay_time,
					TimeUnit.MILLISECONDS);// MJGameConstants.GANG_LAST_CARD_DELAY
		} else {
			// 发牌
			this._handler = this._handler_chuli_firstcards;
			this._handler_chuli_firstcards.reset_status(seat_index, type);
			this._handler.exe(this);
		}

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

	private void progress_banker_select() {
		if (this._cur_round == 1) {
			_cur_banker = RandomUtil.getRandomNumber(Integer.MAX_VALUE) % getTablePlayerNumber();// 创建者的玩家为专家

			// Random random = new Random();//
			// int rand = random.nextInt(6) + 1 + random.nextInt(6) + 1;
			// _banker_select = rand % MJthis.getTablePlayerNumber();//
			// ((lSiceCount>>24)+(lSiceCount>>16)-1)%MJthis.getTablePlayerNumber();

			_shang_zhuang_player = GameConstants.INVALID_SEAT;
			_lian_zhuang_player = GameConstants.INVALID_SEAT;
		}
	}

	private void countCardType(ChiHuRight chiHuRight, int seat_index) {
		try {
			int wFanShu = 0;
			if (!(chiHuRight.opr_and(GameConstants.CHR_TIAN_HU)).is_empty()) {
				MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.hhtianhu, "", 0, 0l,
						this.getRoom_id());
			}

			if (!(chiHuRight.opr_and(GameConstants.CHR_DI_HU)).is_empty()) {
				wFanShu = 3;
				MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.hhdihu, "", 0, 0l,
						this.getRoom_id());
			}

			if (!(chiHuRight.opr_and(GameConstants.CHR_ONE_HONG)).is_empty()) {
				MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.hhyidianhong, "", 0,
						0l, this.getRoom_id());
			}

			if (!(chiHuRight.opr_and(GameConstants.CHR_TEN_HONG_PAI).is_empty())) {
				MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.hhhonghu, "", 0, 0l,
						this.getRoom_id());
			}

			if (!(chiHuRight.opr_and(GameConstants.CHR_THIRTEEN_HONG_PAI)).is_empty()) {
				MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.hhhongfantian, "",
						0, 0l, this.getRoom_id());
			}
			if (!(chiHuRight.opr_and(GameConstants.CHR_ALL_HEI)).is_empty()) {
				MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.hhallhei, "", 0, 0l,
						this.getRoom_id());
			}
			if (!(chiHuRight.opr_and(GameConstants.CHR_HAI_HU)).is_empty()) {
				wFanShu = 3;
				MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.hhhaihu, "", 0, 0l,
						this.getRoom_id());
			}

			if (!(chiHuRight.opr_and(GameConstants.CHR_TING_HU)).is_empty()) {
				MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.hhtinghu, "", 0, 0l,
						this.getRoom_id());
			}

			if (!(chiHuRight.opr_and(GameConstants.CHR_DA_HU).is_empty())) {
				MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.hhdahu, "", 0, 0l,
						this.getRoom_id());
			}

			if (!(chiHuRight.opr_and(GameConstants.CHR_XIAO_HU)).is_empty()) {
				MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.hhxiaohu, "", 0, 0l,
						this.getRoom_id());
			}
			if (!(chiHuRight.opr_and(GameConstants.CHR_DUI_ZI_HU)).is_empty()) {
				MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.hhduizihu, "", 0,
						0l, this.getRoom_id());
			}

			if (!(chiHuRight.opr_and(GameConstants.CHR_SHUA_HOU)).is_empty()) {
				wFanShu = 3;
				MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.hhshuahou, "", 0,
						0l, this.getRoom_id());
			}

			if (!(chiHuRight.opr_and(GameConstants.CHR_TUAN_YUAN)).is_empty()) {
				MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.hhtuanyuan, "", 0,
						0l, this.getRoom_id());
			}

			if (!(chiHuRight.opr_and(GameConstants.CHR_HUANG_FAN).is_empty())) {
				MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.hhhuangfan, "", 0,
						0l, this.getRoom_id());
			}

			if (!(chiHuRight.opr_and(GameConstants.CHR_HANG_HANG_XI)).is_empty()) {
				MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.hhhanghangxi, "", 0,
						0l, this.getRoom_id());
			}

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	// 转转麻将结束描述
	private void set_result_describe(int seat_index) {
		int l;
		long type = 0;
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (seat_index != i) {
				continue;
			}
			String des = "";
			boolean da_hu = false;
			l = GRR._chi_hu_rights[i].type_count;
			int have_card_index[] = new int[GameConstants.MAX_HH_INDEX];
			for (int j = 0; j < 7; j++) {
				for (int x = 0; x < 4; x++) {
					if (_hu_weave_items[seat_index][j].weave_card[x] > 0) {
						int card = _hu_weave_items[seat_index][j].weave_card[x];
						if (card > GameConstants.CARD_ESPECIAL_TYPE_NIAO) {
							card -= GameConstants.CARD_ESPECIAL_TYPE_NIAO;
						}
						have_card_index[this._logic.switch_to_card_index(card)]++;
					}
				}
			}

			int nei_yuan_fan = 1;
			int wai_yuan_fan = 1;
			boolean is_hai_lao = false;
			for (int j = 0; j < GameConstants.MAX_HH_INDEX; j++) {
				int hand_count = _hand_card_index[seat_index][j];
				// 外元
				if (have_card_index[j] == 4) {

					if (hand_count == 4) {
						nei_yuan_fan *= 4;

					} else {
						boolean is_wai_yuan = false;
						for (int x = 0; x < 7; x++) {
							if ((_hu_weave_items[seat_index][x].weave_kind == GameConstants.WIK_YIYANGWHZ_QING_NEI
									|| _hu_weave_items[seat_index][x].weave_kind == GameConstants.WIK_YIYANGWHZ_QING_WAI)
									&& _hu_weave_items[seat_index][x].center_card == this._logic
											.switch_to_card_data(j)) {
								nei_yuan_fan *= 4;
								is_wai_yuan = true;
								break;
							}
							if ((_hu_weave_items[seat_index][x].weave_kind == GameConstants.WIK_YYWHZ_WAI
									&& hand_count == 3)
									&& _hu_weave_items[seat_index][x].center_card == this._logic
											.switch_to_card_data(j)) {
								nei_yuan_fan *= 4;
								is_wai_yuan = true;
								break;
							}
							if ((_hu_weave_items[seat_index][x].weave_kind == GameConstants.WIK_DAN_WAI
									&& hand_count == 3)
									&& _hu_weave_items[seat_index][x].center_card == this._logic
											.switch_to_card_data(j)) {
								nei_yuan_fan *= 4;
								is_wai_yuan = true;
								break;
							}
						}
						if (!is_wai_yuan) {
							wai_yuan_fan *= 2;
						}
					}
				}
			}
			if (nei_yuan_fan == 1) {
				nei_yuan_fan = 0;
			}
			if (wai_yuan_fan == 1) {
				wai_yuan_fan = 0;
			}

			for (int j = 0; j < l; j++) {
				type = GRR._chi_hu_rights[i].type_list[j];
				if (GRR._chi_hu_rights[i].is_valid()) {
					if (type == GameConstants.CHR_HAI_LAO_WHZ_YIYANG) {
						is_hai_lao = true;
					}
				}
			}
			if (is_hai_lao) {
				if (this._shen_pai % 2 == _repertory_card[_all_card_len - GRR._left_card_count - 1] % 2) {
					des += ",神腰 ：	" + 2 + "番";
				} else {
					des += ",神腰 ：	" + 0 + "番";
				}
				if (has_rule(GameConstants.GAME_RULE_YIYANG_WHZ_HAIDI_2)) {
					des += ",海捞 ：	" + 2 + "番";
				} else if (has_rule(GameConstants.GAME_RULE_YIYANG_WHZ_HAIDI_4)) {
					des += ",海捞 ：	" + 4 + "番";
				} else {
					des += ",海捞 ：	" + 4 + "番";
				}
			} else {
				if (this._shen_pai % 2 == _repertory_card[_all_card_len - GRR._left_card_count] % 2) {
					des += ",神腰 ：	" + 2 + "番";
				} else {
					des += ",神腰 ：	" + 0 + "番";
				}
				des += ",海捞 ：	" + 0 + "番";
			}

			des += ",内元 ：	" + nei_yuan_fan + "番";
			des += ",外元 ：	" + wai_yuan_fan + "番";

			for (int j = 0; j < l; j++) {
				type = GRR._chi_hu_rights[i].type_list[j];
				if (GRR._chi_hu_rights[i].is_valid()) {

					if (type == GameConstants.CHR_TIAN_HU_WHZ_YIYANG) {

						if (has_rule(GameConstants.GAME_RULE_YIYANG_WHZ_FAN_CAL)) {
							des += ",天胡  X8番";
						} else {
							des += ",天胡  +50";
						}
						da_hu = true;
					}
					if (type == GameConstants.CHR_DI_HU_WHZ_YIYANG) {
						if (has_rule(GameConstants.GAME_RULE_YIYANG_WHZ_FAN_CAL)) {
							des += ",地胡  X8番";
						} else {
							des += ",地胡  +50";
						}
						da_hu = true;
					}
					if (type == GameConstants.CHR_TING_HU_WHZ_YIYANG) {
						if (has_rule(GameConstants.GAME_RULE_YIYANG_WHZ_FAN_CAL)) {
							des += ",听胡  X4番";
						} else {
							des += ",听胡  +50";
						}
						da_hu = true;
					}
					if (type == GameConstants.CHR_YI_DIAN_HONG_WHZ_YIYANG) {
						if (has_rule(GameConstants.GAME_RULE_YIYANG_WHZ_FAN_CAL)) {
							des += ",一点红  X4番";
						} else {
							des += ",一点红  +50";
						}
						da_hu = true;
					}
					if (type == GameConstants.CHR_HUO_HUO_WHZ_YIYANG) {
						// 火火翻
						int hong_count = _logic.calculate_hong_pai_count_yiyang(_hu_weave_items[seat_index],
								_hu_weave_count[seat_index]);
						des += ",火火番X" + (int) Math.pow(2, hong_count - 9) + " 番";
					}
					if (type == GameConstants.CHR_QUAN_HEI_WHZ_YIYANG) {
						if (has_rule(GameConstants.GAME_RULE_YIYANG_WHZ_FAN_CAL)) {
							des += ",黑胡子  X8番";
						} else {
							des += ",黑胡子  +100";
						}
						da_hu = true;
					}
					if (type == GameConstants.CHR_QUAN_DA_WHZ_YIYANG) {
						if (has_rule(GameConstants.GAME_RULE_YIYANG_WHZ_FAN_CAL)) {
							des += ",大字胡  X8番";
						} else {
							des += ",大字胡 +100";
						}
						da_hu = true;
					}
					if (type == GameConstants.CHR_QUAN_XIAO_WHZ_YIYANG) {
						if (has_rule(GameConstants.GAME_RULE_YIYANG_WHZ_FAN_CAL)) {
							des += ",小字胡  X8番";
						} else {
							des += ",小字胡 +100";
						}
						da_hu = true;
					}
					if (type == GameConstants.CHR_HANG_HANG_XI_WHZ_YIYANG) {
						if (has_rule(GameConstants.GAME_RULE_YIYANG_WHZ_FAN_CAL)) {
							des += ",行行息 X4番";
						} else {
							des += ",行行息 +50";
						}
						da_hu = true;
					}
					if (type == GameConstants.CHR_DUI_ZI_XI_WHZ_YIYANG) {
						if (has_rule(GameConstants.GAME_RULE_YIYANG_WHZ_FAN_CAL)) {
							des += ",对子息 X8番";
						} else {
							des += ",对子息 +100";
						}
						da_hu = true;
					}
					if (type == GameConstants.CHR_HAI_HUA_HUZI_WHZ_YIYANG) {
						des += ",花胡子	X64番";
						da_hu = true;
					}
					if (type == GameConstants.CHR_HAI_YIN_WHZ_YIYANG) {
						des += ",印	X2番";
						da_hu = true;
					}
					if (type == GameConstants.CHR_HAI_SHUANG_PIAO_WHZ_YIYANG) {
						des += ",双飘	X2番";
						da_hu = true;
					}
					if (type == GameConstants.CHR_HAI_DAN_PIAO_WHZ_YIYANG) {
						des += ",单飘	X2番";
						da_hu = true;
					}
				}
			}
			GRR._result_des[i] = des;
		}
	}

	@Override
	public boolean is_zhuang_xian() {
		if ((GameConstants.GAME_TYPE_ZZ == _game_type_index) || is_mj_type(GameConstants.GAME_TYPE_HZ)
				|| is_mj_type(GameConstants.GAME_TYPE_SHUANGGUI) || is_mj_type(GameConstants.GAME_TYPE_ZHUZHOU)) {
			return false;
		}
		return true;
	}

	/**
	 * 取出实际牌数据
	 * 
	 * @param card
	 * @return
	 */
	@Override
	public int get_real_card(int card) {
		return card;
	}

	/**
	 * 结束调度
	 * 
	 * @return
	 */
	@Override
	public boolean exe_finish() {

		this._handler = this._handler_finish;
		this._handler_finish.exe(this);
		return true;
	}

	@Override
	public boolean exe_add_discard(int seat_index, int card_count, int card_data[], boolean send_client, int delay) {
		if (delay == 0) {
			this.runnable_add_discard(seat_index, card_count, card_data, send_client);
			return true;
		}
		GameSchedule.put(
				new AddDiscardRunnable(getRoom_id(), seat_index, card_count, card_data, send_client, getMaxCount()),
				delay, TimeUnit.MILLISECONDS);

		return true;

	}

	/**
	 * //执行发牌 是否延迟
	 * 
	 * @param seat_index
	 * @param delay
	 * @return
	 */
	@Override
	public boolean exe_dispatch_card(int seat_index, int type, int delay) {

		if (delay > 0) {
			GameSchedule.put(new DispatchCardRunnable(this.getRoom_id(), seat_index, type, false), delay,
					TimeUnit.MILLISECONDS);
		} else {
			// 发牌
			this._handler = this._handler_dispath_card;
			this._handler_dispath_card.reset_status(seat_index, type);
			this._handler.exe(this);
		}

		return true;
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
	@Override
	public boolean exe_gang(int seat_index, int provide_player, int center_card, int action, int type, boolean depatch,
			boolean self, boolean d, int delay) {
		delay = GameConstants.PAO_SAO_TI_TIME;
		int gameId = this.getGame_id() == 0 ? 5 : this.getGame_id();
		SysParamModel sysParamModel1104 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(gameId)
				.get(1104);
		if (sysParamModel1104 != null && sysParamModel1104.getVal3() > 0 && sysParamModel1104.getVal3() < 10000) {
			delay = sysParamModel1104.getVal3();
		}

		if (delay > 0) {
			GameSchedule.put(new GangCardRunnable(this.getRoom_id(), seat_index, provide_player, center_card, action,
					type, depatch, self, d), delay, TimeUnit.MILLISECONDS);
		} else {
			// 是否有抢杠胡
			this._handler = this._handler_gang;
			this._handler_gang.reset_status(seat_index, provide_player, center_card, action, type, depatch, self, d);
			this._handler.exe(this);
		}
		return true;
	}

	@Override
	public boolean runnable_gang_card_data(int seat_index, int provide_player, int center_card, int action, int type,
			boolean depatch, boolean self, boolean d) {
		// 是否有抢杠胡
		this._handler = this._handler_gang;
		this._handler_gang.reset_status(seat_index, provide_player, center_card, action, type, self, d, depatch);
		this._handler.exe(this);
		return true;

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

	/**
	 * 切换到出牌处理器
	 * 
	 * @param seat_index
	 * @param card
	 * @return
	 */
	@Override
	public boolean exe_out_card(int seat_index, int card, int type) {
		// 出牌
		this._handler = this._handler_out_card_operate;
		this._handler_out_card_operate.reset_status(seat_index, card, type);
		this._handler.exe(this);

		return true;
	}

	@Override
	public boolean exe_chi_peng(int seat_index, int provider, int action, int card, int type, int lou_operate) {
		// 出牌
		_last_player = provider;
		this._handler = this._handler_chi_peng;
		this._handler_chi_peng.reset_status(seat_index, provider, action, card, type, lou_operate);
		this._handler.exe(this);

		return true;
	}

	public boolean exe_wai(int seat_index, int provider, int action, int card, int type, int lou_operate) {
		// 出牌
		_last_player = provider;
		this._handler = this._handler_wai;
		this._handler_wai.reset_status(seat_index, provider, action, card, type, lou_operate);
		this._handler.exe(this);

		return true;
	}

	public boolean exe_liu(int seat_index, int provider, int action, int card, int type, int lou_operate) {
		// 出牌
		_last_player = provider;
		this._handler = this._handler_liu;
		this._handler_liu.reset_status(seat_index, provider, action, card, type, lou_operate);
		this._handler.exe(this);

		return true;
	}

	public boolean exe_piao(int seat_index, int provider, int action, int card, int type, int lou_operate) {
		// 出牌
		_last_player = provider;
		this._handler = this._handler_piao;
		this._handler_piao.reset_status(seat_index, provider, action, card, type, lou_operate);
		this._handler.exe(this);
		if (!this.has_rule(GameConstants.GAME_RULE_YIYANG_PIAO_TING_HU)) {
			this._user_out_card_count[seat_index]++;
		}

		return true;
	}

	@Override
	public boolean exe_jian_pao_hu(int seat_index, int action, int card) {

		GameSchedule.put(new JianPaoHuRunnable(this.getRoom_id(), seat_index, action, card),
				GameConstants.DELAY_AUTO_OUT_CARD_TRUTESS, TimeUnit.MILLISECONDS);

		return true;
	}

	/**
	 * 调度,加入牌堆
	 **/
	@Override
	public void runnable_add_discard(int seat_index, int card_count, int card_data[], boolean send_client) {
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

	@Override
	public boolean is_can_out_card(int seat_index) {
		if (this._is_xiang_gong[seat_index] == true)
			return false;
		boolean flag = false;
		for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
			if (GRR._cards_index[seat_index][i] >= 1) {
				if (GRR._cannot_out_index[seat_index][i] == 0) {
					flag = true;
					break;
				}

			}

		}
		return flag;
	}

	/**
	 * 显示中间出的牌
	 * 
	 * @param seat_index
	 */
	@Override
	public void runnable_remove_middle_cards(int seat_index) {
		// 去掉
		this.operate_show_card(seat_index, GameConstants.Show_Card_Center, 0, null, this.GRR._weave_items[seat_index],
				this.GRR._weave_count[seat_index], GameConstants.INVALID_SEAT);

		// 刷新有癞子的牌
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			boolean has_lai_zi = false;
			for (int j = 0; j < GameConstants.MAX_HH_INDEX; j++) {
				if (GRR._cards_index[i][j] > 0 && _logic.is_magic_index(j)) {
					has_lai_zi = true;
					break;
				}
			}
			if (has_lai_zi) {
				// 刷新自己手牌
				int cards[] = new int[this.get_hand_card_count_max()];
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
	@Override
	public void runnable_remove_out_cards(int seat_index, int type) {
		// 删掉出来的那张牌
		operate_out_card(seat_index, 0, null, type, GameConstants.INVALID_SEAT);
	}

	private void shuffle_players() {
		List<Player> pl = new ArrayList<Player>();
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			pl.add(this.get_players()[i]);
		}

		Collections.shuffle(pl);
		pl.toArray(this.get_players());

		// 重新排下位置
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			get_players()[i].set_seat_index(i);
		}
	}

	public static void main(String[] args) {
		int value = 0x00000200;
		int value1 = 0x200;

		System.out.println(value);
		System.out.println(value1);

	}

	@Override
	public int is_tian_hu(int seat_index, int provider, ChiHuRight chiHuRight, int cur_card_data) {
		int card_type = GameConstants.HU_CARD_TYPE_ZIMO;
		int hu_xi_chi[] = new int[1];
		hu_xi_chi[0] = 0;
		int action_hu = analyse_chi_hu_card(GRR._cards_index[seat_index], GRR._weave_items[seat_index],
				GRR._weave_count[seat_index], seat_index, seat_index, cur_card_data, chiHuRight, card_type, hu_xi_chi,
				false);

		if (action_hu != GameConstants.WIK_NULL) {
			return 1;
		}

		return 0;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.cai.common.domain.Room#handler_request_trustee(int, boolean)
	 */
	@Override
	public boolean handler_request_trustee(int get_seat_index, boolean isTrustee, int Trustee_type) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_IS_TRUSTEE);
		roomResponse.setOperatePlayer(get_seat_index);
		roomResponse.setIstrustee(isTrustee);
		boolean isTing = _playerStatus[get_seat_index]._hu_card_count <= 0 ? false : true;
		roomResponse.setIsTing(isTing);
		if (isTrustee && !isTing) {
			roomResponse.setIstrustee(false);
			send_response_to_player(get_seat_index, roomResponse);
			return false;
		}
		if (isTrustee && SysParamUtil.is_auto(GameConstants.GAME_ID_FLS_LX)) {
			istrustee[get_seat_index] = isTrustee;
		}
		this.send_response_to_room(roomResponse);
		GRR.add_room_response(roomResponse);
		return false;
	}

	@Override
	public boolean runnable_dispatch_last_card_data(int cur_player, int type, boolean tail) {

		return true;
	}

	@Override
	public boolean runnable_chuli_first_card_data(int _seat_index, int _type, boolean _tail) {
		// 发牌
		this._handler = this._handler_chuli_firstcards;
		this._handler_chuli_firstcards.reset_status(_seat_index, _type);
		this._handler.exe(this);
		return true;
	}

	@Override
	public boolean runnable_dispatch_first_card_data(int _seat_index, int _type, boolean _tail) {

		this._handler = this._handler_dispath_firstcards;
		this._handler_dispath_firstcards.reset_status(_seat_index, _type);
		this._handler.exe(this);
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.cai.common.domain.Room#getTablePlayerNumber()
	 */
	@Override
	public int getTablePlayerNumber() {
		if (playerNumber > 0) {
			return playerNumber;
		}
		return RoomComonUtil.getMaxNumber(getDescParams());
	}

	@Override
	public boolean exe_finish(int reason) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void clear_score_in_gold_room() {
		ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
		int score = 0;

		// 门槛判断
		SysParamModel sysParamModel = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(getGame_id())
				.get(5006);
		int beilv = sysParamModel.getVal2().intValue();// 倍率
		float[] scores = _player_result.getGame_score();
		// GameConstants.GAME_PLAYER
		for (int i = 0; i < scores.length; i++) {
			score = (int) (scores[i] * beilv);

			StringBuilder buf = new StringBuilder();
			buf.append("牌局消耗:" + scores[i]).append("game_id:" + getGame_id());
			AddMoneyResultModel addGoldResultModel = centerRMIServer.addAccountMoney(
					this.get_players()[i].getAccount_id(), score, false, buf.toString(), EMoneyOperateType.ROOM_COST);
			if (addGoldResultModel.isSuccess() == false) {
				// 扣费失败
				logger.error("玩家:" + this.get_players()[i].getAccount_id() + ",扣费:" + score + "失败");
			}
			// 结算后 清0
			scores[i] = 0;
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
		return true;
	}

	@Override
	public boolean open_card_timer() {
		return false;
	}

	@Override
	public boolean robot_banker_timer() {
		return false;
	}

	@Override
	public boolean ready_timer() {
		return false;
	}

	@Override
	public boolean add_jetton_timer() {
		return false;
	}

	/**
	 * @param seat_index
	 * @param room_rq
	 * @param type
	 * @return
	 */
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
