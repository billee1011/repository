/**
 * 
 */
package com.cai.game.pdk.handler.fifteenpdk_dl;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.define.EMoneyOperateType;
import com.cai.common.define.ETriggerType;
import com.cai.common.domain.AddMoneyResultModel;
import com.cai.common.domain.Player;
import com.cai.common.domain.PlayerResult;
import com.cai.common.domain.SysParamModel;
import com.cai.common.rmi.ICenterRMIServer;
import com.cai.common.util.PBUtil;
import com.cai.common.util.RandomUtil;
import com.cai.common.util.SpringService;
import com.cai.dictionary.SysParamDict;
import com.cai.domain.SheduleArgs;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.AddDiscardRunnable;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.future.runnable.JianPaoHuRunnable;
import com.cai.game.RoomUtil;
import com.cai.game.pdk.PDKGameLogic_AAA;
import com.cai.game.pdk.PDKTable;
import com.cai.game.pdk.data.tagAnalyseIndexResult;
import com.cai.game.pdk.handler.PDKHandlerFinish;
import com.cai.service.PlayerServiceImpl;

import protobuf.clazz.Protocol.GameEndResponse;
import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomRequest;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.pdk.PdkRsp.RoomInfoPdk;
import protobuf.clazz.pdk_all.PdkRsp.RefreshCardsPdk;
import protobuf.clazz.pdk_xy.PdkRsp.GameStart_PDK_xy;
import protobuf.clazz.pdk_xy.PdkRsp.OutCardDataPdk_xy;
import protobuf.clazz.pdk_xy.PdkRsp.PukeGameEndPdk_xy;
import protobuf.clazz.pdk_xy.PdkRsp.QiePaiResponse;
import protobuf.clazz.pdk_xy.PdkRsp.QiePaiResult;
import protobuf.clazz.pdk_xy.PdkRsp.QiePaiStart;
import protobuf.clazz.pdk_xy.PdkRsp.Qiepai_Req;
import protobuf.clazz.pdk_xy.PdkRsp.RefreshCardsPdk_xy;
import protobuf.clazz.pdk_xy.PdkRsp.TableResponse_PDK_xy;

//效验类型
class EstimatKind {
	public static int EstimatKind_OutCard = 1; // 出牌效验
	public static int EstimatKind_GangCard = 2; // 杠牌效验
}

///////////////////////////////////////////////////////////////////////////////////////////////
public class PDK_FIFTEENDL_Table extends PDKTable {

	protected static final int ID_TIMER_QIE_PAI = 1;// 切牌
	protected static final int ID_TIMER_QIE_PAI_TO_START = 2;// 切牌

	public PDK_FIFTEENDL_Table() {
		this.setPlay_card_time(10);
	}

	@Override
	public void on_init_table(int game_type_index, int game_rule_index, int game_round) {
		_game_type_index = game_type_index;//
		_game_rule_index = game_rule_index;
		_game_round = game_round;
		_cur_round = 0;
		_logic = new PDKGameLogic_AAA();
		this._logic._game_rule_index = _game_rule_index;
		this._logic._game_type_index = _game_type_index;
		this._logic.setRuleMap(this.getRuleMap());
		istrustee = new boolean[this.getTablePlayerNumber()];
		if (this.has_rule(GameConstants.GAME_RULE_BOOM)) {
			this._logic._is_boom = true;
		} else {
			this._logic._is_boom = false;
		}

		_player_result = new PlayerResult(this.getRoom_owner_account_id(), this.getRoom_id(), _game_type_index,
				_game_rule_index, _game_round, this.get_game_des());
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_player_result.hu_pai_count[i] = 0;
			_player_result.ming_tang_count[i] = 0;
			_player_result.ying_xi_count[i] = 0;
			_game_score[i] = 0;
		}
		_bomb_cell_score = 10;
		if (is_match()) {
			_bomb_cell_score = this.matchBase.getBoomBase();
		} else if (this.isCoinRoom()) {
			_bomb_cell_score = this.matchBase.getBoomBase() * this.matchBase.getBaseScore();
		}
		_handler_out_card_operate = new PDKHandlerOutCardOperate_FIFTEEN_DL();

		_handler_finish = new PDKHandlerFinish();

		this.setMinPlayerCount(getTablePlayerNumber());
	}

	@Override
	public void runnable_set_trustee(int _seat_index) {
		// TODO Auto-generated method stub

	}

	// 游戏开始
	@Override
	protected boolean on_handler_game_start() {

		_game_status = GameConstants.GS_MJ_PLAY;
		reset_init_data();

		// 庄家选择

		GRR._banker_player = _cur_banker;
		_current_player = GRR._banker_player;

		_turn_out__player = GameConstants.INVALID_SEAT;
		_bao_pei_palyer = GameConstants.INVALID_SEAT;
		_hong_tao_palyer = GameConstants.INVALID_SEAT;

		_turn_out_card_count = 0;
		for (int i = 0; i < this.get_hand_card_count_max(); i++) {
			_turn_out_card_data[i] = GameConstants.INVALID_CARD;

		}
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			GRR._cur_card_type[i] = GameConstants.PDK_CT_ERROR;
			GRR._cur_round_pass[i] = 0;
			_out_bomb_score[i] = 0;
			_out_bomb_score_zhaniao[i] = 0;
			_player_result.chun_tian[i] = 0;
			for (int j = 0; j < this.get_hand_card_count_max(); j++) {
				GRR._cur_round_data[i][j] = GameConstants.INVALID_CARD;
				GRR._cur_change_round_data[i][j] = GameConstants.INVALID_CARD;
			}
		}

		//

		_repertory_card = new int[GameConstants.CARD_COUNT_PDK_FIFTEEN];
		shuffle(_repertory_card, GameConstants.CARD_DATA_PDK_FIFTEEN);

		_cur_round++;

		// DEBUG_CARDS_MODE = true;
		// BACK_DEBUG_CARDS_MODE = true;
		if (DEBUG_CARDS_MODE || BACK_DEBUG_CARDS_MODE)
			test_cards();

		if (!_is_in_qie) {
			game_start_pkd();
		} else {
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			QiePaiStart.Builder qie_pai = QiePaiStart.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_PDK_QIE_START);// 201
			qie_pai.setQiepaiChair(_qie_pai_seat);
			qie_pai.setDisplayTime(6);
			roomResponse.setCommResponse(PBUtil.toByteString(qie_pai));
			this.send_response_to_room(roomResponse);

			schedule(ID_TIMER_QIE_PAI, SheduleArgs.newArgs(), 6000);
		}
		return true;
	}

	/**
	 * 调度回调
	 */
	@Override
	protected void timerCallBack(SheduleArgs args) {
		switch (args.getTimerId()) {
		case ID_TIMER_QIE_PAI: {
			game_start_pkd();
			return;
		}
		case ID_TIMER_QIE_PAI_TO_START: {
			game_start_pkd();
			return;
		}
		}
	}

	/**
	 * 开始 跑得快经典
	 * 
	 * @return
	 */
	private boolean game_start_pkd() {
		// 游戏开始
		this._game_status = GameConstants.GS_MJ_PLAY;// 设置状态

		int FlashTime = 4000;
		int standTime = 1000;
		_is_in_qie = false;
		// 初始化游戏变量
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_boom_num[i] = 0;
			_out_card_times[i] = 0;
		}
		if (this._cur_round == 1) {
			GRR._banker_player = this.find_banker();
		}

		for (int play_index = 0; play_index < this.getTablePlayerNumber(); play_index++) {

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();

			roomResponse.setType(MsgConstants.RESPONSE_PDK_GAME_START);
			roomResponse.setGameStatus(this._game_status);
			// 发送数据
			GameStart_PDK_xy.Builder gamestart_pdk = GameStart_PDK_xy.newBuilder();
			RoomInfoPdk.Builder room_info = getRoomInfoPdk();
			gamestart_pdk.setRoomInfo(room_info);
			if (this._cur_round == 1) {
				// shuffle_players();
				this.load_player_info_data_game_start(gamestart_pdk);
			}
			this._current_player = GRR._banker_player;
			gamestart_pdk.setCurBanker(GRR._banker_player);

			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				if (this.has_rule(GameConstants.GAME_RULE_DISPLAY_CARD)) {
					gamestart_pdk.addCardCount(GRR._card_count[i]);
				} else {
					if (i == play_index) {
						gamestart_pdk.addCardCount(GRR._card_count[i]);
					} else {
						gamestart_pdk.addCardCount(-1);
					}
				}
				Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
				if (i == play_index) {
					for (int j = 0; j < GRR._card_count[play_index]; j++) {
						if (this.has_rule(GameConstants.GAME_RULE_FANG_ZUO_BI) && play_index != _current_player) {
							cards_card.addItem(GameConstants.BLACK_CARD);
						} else {
							cards_card.addItem(GRR._cards_data[play_index][j]);
						}
					}
				} else {
					for (int j = 0; j < GRR._card_count[i]; j++) {
						cards_card.addItem(GameConstants.INVALID_CARD);
					}
				}

				gamestart_pdk.addCardsData(cards_card);

			}
			gamestart_pdk.setMagicCard(GameConstants.INVALID_CARD);
			gamestart_pdk.setDisplayTime(getPlay_card_time());
			roomResponse.setCommResponse(PBUtil.toByteString(gamestart_pdk));

			int gameId = this.getGame_id() == 0 ? 8 : this.getGame_id();
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

			GRR.add_room_response(roomResponse);

			// 自己才有牌数据
			this.send_response_to_player(play_index, roomResponse);
		}

		// 回放
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_PDK_GAME_START);
		roomResponse.setGameStatus(this._game_status);
		// 发送数据
		GameStart_PDK_xy.Builder gamestart_pdk = GameStart_PDK_xy.newBuilder();
		RoomInfoPdk.Builder room_info = getRoomInfoPdk();
		gamestart_pdk.setRoomInfo(room_info);
		if (this._cur_round == 1) {
			this.load_player_info_data_game_start(gamestart_pdk);
		}
		this._current_player = GRR._banker_player;
		gamestart_pdk.setCurBanker(GRR._banker_player);

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			gamestart_pdk.addCardCount(GRR._card_count[i]);
			Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < GRR._card_count[i]; j++) {
				cards_card.addItem(GRR._cards_data[i][j]);
			}
			gamestart_pdk.addCardsData(i, cards_card);
		}

		gamestart_pdk.setDisplayTime(getPlay_card_time());
		gamestart_pdk.setMagicCard(GameConstants.INVALID_CARD);
		roomResponse.setCommResponse(PBUtil.toByteString(gamestart_pdk));

		int gameId = this.getGame_id() == 0 ? 8 : this.getGame_id();
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

		GRR.add_room_response(roomResponse);

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			this.handler_request_trustee(i, this.istrustee[i], 0);
		}
		return true;

	}

	@Override
	public int find_banker() {

		int seat = -1;
		if (this.has_rule(GameConstants.GAME_RULE_ALL_GAME_THREE_PLAYER)) {
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				for (int j = 0; j < GRR._card_count[i]; j++)
					if (GRR._cards_data[i][j] == 0x33)
						return i;
			}
		}
		if (this.has_rule(GameConstants.GAME_RULE_ALL_GAME_TWO_PLAYER)) {
			if (!has_rule(GameConstants.GAME_RULE_RAND_SHOU_CHU)) {
				int first_mincardvalue = _logic.get_card_value(this.GRR._cards_data[0][this.GRR._card_count[0] - 1]);
				int next_mincardvalue = _logic.get_card_value(this.GRR._cards_data[1][this.GRR._card_count[1] - 1]);
				int first_mincardcolor = _logic.get_card_color(this.GRR._cards_data[0][this.GRR._card_count[0] - 1]);
				int next_mincardcolor = _logic.get_card_color(this.GRR._cards_data[1][this.GRR._card_count[1] - 1]);
				// 牌最小的玩家出
				if (first_mincardvalue > next_mincardvalue) {
					return 1;
				} else if (first_mincardvalue < next_mincardvalue) {
					return 0;
				} else {
					if (first_mincardcolor > next_mincardcolor) {
						return 1;
					} else {
						return 0;
					}
				}
			} else {
				return RandomUtil.getRandomNumber(Integer.MAX_VALUE) % this.getTablePlayerNumber();
			}

		}
		return seat;
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
		int xi_pai_count = 0;
		int rand = RandomUtil.generateRandomNumber(3, 6);

		while (xi_pai_count < 10 && xi_pai_count < rand) {
			if (xi_pai_count == 0)
				_logic.random_card_data(repertory_card, mj_cards);
			else
				_logic.random_card_data(repertory_card, repertory_card);

			xi_pai_count++;
		}

		int count = this.getTablePlayerNumber();
		int index = 0;
		boolean is_xi_pai_again = false;
		for (int i = 0; i < count; i++) {
			int hand_card[] = new int[get_hand_card_count_max()];
			for (int j = 0; j < this.get_hand_card_count_max(); j++) {
				hand_card[j] = repertory_card[index++];
			}
			tagAnalyseIndexResult hand_index = new tagAnalyseIndexResult();
			this._logic.AnalysebCardDataToIndex(hand_card, get_hand_card_count_max(), hand_index);
			for (int j = 0; j < this.get_hand_card_count_max();) {
				int card_index = this._logic.switch_card_to_idnex(hand_card[j]);
				if (hand_index.card_index[card_index] == 4) {
					is_xi_pai_again = true;
					break;
				}
				if (hand_index.card_index[card_index] > 0) {
					j += hand_index.card_index[card_index];
				} else {
					j++;
				}
			}
			if (is_xi_pai_again) {
				break;
			}
		}
		if (is_xi_pai_again) {
			_logic.random_card_data(repertory_card, repertory_card);
		}

		for (int i = 0; i < count; i++) {
			for (int j = 0; j < this.get_hand_card_count_max(); j++) {
				GRR._cards_data[i][j] = repertory_card[i * this.get_hand_card_count_max() + j];
				if (this.has_rule(GameConstants.GAME_RULE_HONGTAO10_ZANIAO) && GRR._cards_data[i][j] == 0x2A) {
					_hong_tao_palyer = i;
				}
			}
			GRR._card_count[i] = this.get_hand_card_count_max();
			_logic.sort_card_date_list(GRR._cards_data[i], GRR._card_count[i]);
		}
		if (count == 2) {
			for (int j = 0; j < this.get_hand_card_count_max(); j++) {
				GRR._cards_data[2][j] = repertory_card[2 * this.get_hand_card_count_max() + j];
				_logic.sort_card_date_list(GRR._cards_data[2], GRR._card_count[2]);
			}

		}

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			// 上报牌型
			Player player = get_players()[i];
			if (player != null) {
				triggerCardEvent(ETriggerType.START, player.getAccount_id(), GRR._cards_data[i]);
			}
		}
		GRR._left_card_count = 0;
		// test_cards();
		// 记录初始牌型
		_recordRoomRecord.setBeginArray(Arrays.toString(repertory_card));

	}

	private void test_cards() {
		int cards[] = new int[] { 0x13, 0x03, 0x23, 0x33, 0x14, 0x04, 0x24, 0x34, 0x16, 0x06, 0x25, 0x35, 0x17, 0x07,
				0x27, 0x37, 0x18, 0x08, 0x28, 0x38, 0x2a, 0x1a, 0x29, 0x39, 0x1b, 0x0b, 0x2b, 0x3b, 0x1c, 0x0c };
		// int cards[] = new int[]
		// {0x03,0x13,0x23,0x33,0x14,0x24,0x34,0x04,0x07,0x17,0x08,0x18,0x09,0x19,0x0a};
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			for (int j = 0; j < get_hand_card_count_max(); j++) {
				GRR._cards_data[i][j] = 0;
			}
		}
		int index = 0;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			for (int j = 0; j < get_hand_card_count_max(); j++) {
				GRR._cards_data[i][j] = cards[j];
			}
		}
		/********
		 * 下面这个给测试用的 如果开发用 把上面的注释去掉 realyCards
		 **************************/

		if (BACK_DEBUG_CARDS_MODE) {
			if (debug_my_cards != null) {
				if (debug_my_cards.length > get_hand_card_count_max()) {
					int[] temps = new int[debug_my_cards.length];
					System.arraycopy(debug_my_cards, 0, temps, 0, temps.length);
					testRealyCard(temps, debug_my_cards.length);
					debug_my_cards = null;
				} else {
					int[] temps = new int[debug_my_cards.length];
					System.arraycopy(debug_my_cards, 0, temps, 0, temps.length);
					testSameCard(temps, debug_my_cards.length);
					debug_my_cards = null;
				}
			}

		}
		int count = this.getTablePlayerNumber();
		for (int i = 0; i < count; i++) {
			_logic.sort_card_date_list(GRR._cards_data[i], GRR._card_count[i]);
		}
	}

	/**
	 * 测试线上 实际牌
	 */
	public void testRealyCard(int[] realyCards, int count) {
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			for (int j = 0; j < this.get_hand_card_count_max(); j++) {
				GRR._cards_index[i][j] = 0;
			}
		}
		this._repertory_card = realyCards;

		int send_count;
		int have_send_count = 0;
		_cur_banker = 0;
		// 分发扑克
		int k = 0;
		while (k < realyCards.length) {
			int i = 0;

			for (; i < this.getTablePlayerNumber(); i++) {
				for (int j = 0; j < get_hand_card_count_max(); j++) {
					GRR._cards_data[i][j] = realyCards[k++];
				}
			}
			if (i == this.getTablePlayerNumber())
				break;
		}

		DEBUG_CARDS_MODE = false;// 把调试模式关闭
		BACK_DEBUG_CARDS_MODE = false;
		System.err.println("=========开始调试线上牌型 调试模式自动关闭*=========");

	}

	/**
	 * 模拟牌型--相同牌
	 */
	public void testSameCard(int[] cards, int count) {
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			for (int j = 0; j < count; j++) {
				GRR._cards_index[i][j] = 0;
			}

		}
		this._repertory_card = cards;
		// 分发扑克
		int k = 0;
		int i = 0;

		for (; i < this.getTablePlayerNumber(); i++) {
			k = 0;
			for (int j = 0; j < count; j++) {
				GRR._cards_data[i][j] = cards[k++];
			}
		}
		DEBUG_CARDS_MODE = false;// 把调试模式关闭
		BACK_DEBUG_CARDS_MODE = false;
	}

	// 摸起来的牌 派发牌
	@Override
	public boolean dispatch_card_data(int cur_player, int type, boolean tail) {

		return true;
	}

	// 游戏结束
	@Override
	public boolean on_room_game_finish(int seat_index, int reason) {
		_game_status = GameConstants.GS_MJ_WAIT;
		boolean ret = false;

		if (_cur_round == 1 && (reason == GameConstants.Game_End_NORMAL || reason == GameConstants.Game_End_DRAW)) {
			RoomUtil.realkou_dou(this);
		}
		if (this.is_match()) {
			ret = this.handler_game_finish_pdk_match(seat_index, reason);
		} else if (this.isCoinRoom()) {
			ret = this.handler_game_finish_pdk_coin(seat_index, reason);
		} else {
			ret = this.handler_game_finish_pdk_jd(seat_index, reason);
		}

		return ret;
	}

	@Override
	public void cal_score_pdk_jd(int end_score[]) {
		int win_player = GameConstants.INVALID_SEAT;
		int win_score = 0;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (this.GRR._card_count[i] == 0) {
				win_player = GameConstants.INVALID_SEAT;
				win_player = i;
				_cur_banker = win_player;
			}
		}

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (i == win_player) {
				continue;
			}
			if (this.GRR._card_count[i] > 1) {
				if (i == this.GRR._banker_player) {
					// 反春天
					if (has_rule(GameConstants.GAME_RULE_TWO_PLAY) && this._out_card_times[i] == 1
							&& this.has_rule(GameConstants.GAME_RULE_KE_FAN_DE)) {
						end_score[i] -= 30;
					} else {
						end_score[i] -= this.GRR._card_count[i];
					}
				} else {
					if (this._out_card_times[i] == 0) {
						end_score[i] -= this.GRR._card_count[i] * 2;
					} else {
						end_score[i] -= this.GRR._card_count[i];
					}
				}
				// 红桃扎10鸟
				if (this.has_rule(GameConstants.GAME_RULE_HONGTAO10_ZANIAO)) {
					if (_hong_tao_palyer == win_player) {
						end_score[i] *= 2;
					} else if (_hong_tao_palyer == i) {
						end_score[i] *= 2;
					}
				}
				// 炸弹分
				win_score += Math.abs(end_score[i]);
			}

		}

		// 放走包赔
		if (this._bao_pei_palyer != GameConstants.INVALID_SEAT) {
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				if (i != _bao_pei_palyer && i != win_player) {
					end_score[_bao_pei_palyer] -= Math.abs(end_score[i]);
					end_score[i] = 0;
				}
			}
		}

		end_score[win_player] += win_score;

		if (!is_match()) {
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				end_score[i] += _out_bomb_score_zhaniao[i];
				_player_result.game_score[i] += end_score[i];
			}
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				end_score[i] += _out_bomb_score[i];

				if (end_score[i] > 0) {
					this._win_num[i]++;
				}
				if (end_score[i] < 0) {
					this._lose_num[i]++;
				}
				_game_score[i] += end_score[i];
			}
		} else {
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				end_score[i] += _out_bomb_score_zhaniao[i];
				end_score[i] += _out_bomb_score[i];

				end_score[i] *= getSettleBase(i);
				if (end_score[i] > 0) {
					this._win_num[i]++;
				}
				if (end_score[i] < 0) {
					this._lose_num[i]++;
				}
				_game_score[i] += end_score[i];
				_player_result.game_score[i] += end_score[i];
			}
		}

	}

	public void cal_score_pdk_match(int end_score[]) {
		int win_player = GameConstants.INVALID_SEAT;
		int win_score = 0;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (this.GRR._card_count[i] == 0) {
				win_player = GameConstants.INVALID_SEAT;
				win_player = i;
				_cur_banker = win_player;
			}
		}

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (i == win_player) {
				continue;
			}
			if (this.GRR._card_count[i] > 1) {
				if (i == this.GRR._banker_player) {
					// 反春天
					if (has_rule(GameConstants.GAME_RULE_TWO_PLAY) && this._out_card_times[i] == 1
							&& this.has_rule(GameConstants.GAME_RULE_KE_FAN_DE)) {
						end_score[i] -= 30;
					} else {
						end_score[i] -= this.GRR._card_count[i];
					}
				} else {
					if (this._out_card_times[i] == 0) {
						end_score[i] -= this.GRR._card_count[i] * 2;
					} else {
						end_score[i] -= this.GRR._card_count[i];
					}
				}
				// 红桃扎10鸟
				if (this.has_rule(GameConstants.GAME_RULE_HONGTAO10_ZANIAO)) {
					if (_hong_tao_palyer == win_player) {
						end_score[i] *= 2;
					} else if (_hong_tao_palyer == i) {
						end_score[i] *= 2;
					}
				}
				// 炸弹分
				win_score += Math.abs(end_score[i]);
			}

		}

		// 放走包赔
		if (this._bao_pei_palyer != GameConstants.INVALID_SEAT) {
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				if (i != _bao_pei_palyer && i != win_player) {
					end_score[_bao_pei_palyer] -= Math.abs(end_score[i]);
					end_score[i] = 0;
				}
			}
		}

		end_score[win_player] += win_score;

		if (!is_match()) {
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				end_score[i] += _out_bomb_score_zhaniao[i];
				_player_result.game_score[i] += end_score[i];
			}
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				end_score[i] += _out_bomb_score[i];

				if (end_score[i] > 0) {
					this._win_num[i]++;
				}
				if (end_score[i] < 0) {
					this._lose_num[i]++;
				}
				_game_score[i] += end_score[i];
			}
		} else {
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				end_score[i] += _out_bomb_score_zhaniao[i];
				end_score[i] += _out_bomb_score[i];

				end_score[i] *= getSettleBase(i);
				if (end_score[i] > 0) {
					this._win_num[i]++;
				}
				if (end_score[i] < 0) {
					this._lose_num[i]++;
				}
				_game_score[i] += end_score[i];
				_player_result.game_score[i] += end_score[i];
			}
		}

	}

	public void cal_score_pdk_coin(int end_score[]) {
		int win_player = GameConstants.INVALID_SEAT;
		int win_score = 0;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (this.GRR._card_count[i] == 0) {
				win_player = GameConstants.INVALID_SEAT;
				win_player = i;
				_cur_banker = win_player;
			}
		}
		if (win_player == GameConstants.INVALID_SEAT) {
			return;
		}
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (i == win_player) {
				continue;
			}
			if (this.GRR._card_count[i] > 1) {
				if (i == this.GRR._banker_player) {
					// 反春天
					if (has_rule(GameConstants.GAME_RULE_TWO_PLAY) && this._out_card_times[i] == 1
							&& this.has_rule(GameConstants.GAME_RULE_KE_FAN_DE)) {
						end_score[i] -= 30;
					} else {
						end_score[i] -= this.GRR._card_count[i];
					}
				} else {
					if (this._out_card_times[i] == 0) {
						end_score[i] -= this.GRR._card_count[i] * 2;
					} else {
						end_score[i] -= this.GRR._card_count[i];
					}
				}
				// 红桃扎10鸟
				if (this.has_rule(GameConstants.GAME_RULE_HONGTAO10_ZANIAO)) {
					if (_hong_tao_palyer == win_player) {
						end_score[i] *= 2;
					} else if (_hong_tao_palyer == i) {
						end_score[i] *= 2;
					}
				}
				end_score[i] *= this.getCoinExciteMultiple(this.get_players()[i].getAccount_id())
						* this.matchBase.getBaseScore();
				// 炸弹分
				win_score += Math.abs(end_score[i]);
			}

		}

		end_score[win_player] += win_score;

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (end_score[i] > 0) {
				this._win_num[i]++;
			}
			if (end_score[i] < 0) {
				this._lose_num[i]++;
			}
			_game_score[i] += end_score[i];
			_player_result.game_score[i] += end_score[i];
			_player_result.data_extra[i] += _out_bomb_score_zhaniao[i] + _out_bomb_score[i];
		}

	}

	public boolean handler_game_finish_pdk_coin(int seat_index, int reason) {
		int real_reason = reason;

		int count = getTablePlayerNumber();
		if (count == 0) {
			count = this.getTablePlayerNumber();
		}

		// 重制准备
		for (int i = 0; i < count; i++) {
			_player_ready[i] = 0;
		}
		for (int i = 0; i < count; i++) {
			if (this._cur_round > 0) {
				// 结束后刷新玩家
				RoomResponse.Builder roomResponse2 = RoomResponse.newBuilder();
				roomResponse2.setGameStatus(_game_status);
				roomResponse2.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);// 刷新玩家
				this.load_player_info_data(roomResponse2);
				this.send_response_to_player(i, roomResponse2);
			}
		}

		int end_score[] = new int[this.getTablePlayerNumber()];
		Arrays.fill(end_score, 0);
		// 计算分数
		if (reason == GameConstants.Game_End_NORMAL) {
			cal_score_pdk_coin(end_score);
		} else {
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				_player_result.game_score[i] -= _out_bomb_score[i];
			}
		}
		// 最高得分
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (end_score[i] > _game_score_max[i]) {
				_game_score_max[i] = end_score[i];
			}
		}
		// 失分最多
		_qie_pai_seat = GameConstants.INVALID_SEAT;
		int lost_score = 0;
		if (GRR != null) {
			this.operate_player_data();

			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				int index = (GRR._banker_player + 1 + i) % this.getTablePlayerNumber();
				if (end_score[index] < lost_score) {
					lost_score = end_score[index];
					_qie_pai_seat = index;
				}
			}
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_PDK_GAME_END);
		for (int index = 0; index < this.getTablePlayerNumber(); index++) {
			GameEndResponse.Builder game_end = GameEndResponse.newBuilder();
			PukeGameEndPdk_xy.Builder game_end_pdk = PukeGameEndPdk_xy.newBuilder();
			RoomInfoPdk.Builder room_info = getRoomInfoPdk();
			game_end_pdk.setRoomInfo(room_info);
			if (reason == GameConstants.Game_End_NORMAL) {
				game_end_pdk.setHongTaoPlayer(_hong_tao_palyer);
				game_end_pdk.setBaoPeiPlayer(_bao_pei_palyer);
				game_end_pdk.setFanDiPlayer(_fan_di_palyer);
				game_end_pdk.setTaoPaoPlayer(_tao_pao_palyer);
				game_end_pdk.setZhaNiaoPlayer(GameConstants.INVALID_SEAT);
				// 春天玩家
				for (int i = 0; i < this.getTablePlayerNumber(); i++) {
					if (this.has_rule(GameConstants.GAME_RULE_TWO_PLAY)
							&& this.has_rule(GameConstants.GAME_RULE_KE_FAN_DE) && i == GRR._banker_player
							&& this._out_card_times[i] == 1 && GRR._card_count[i] != 0) {
						game_end_pdk.setFanDiPlayer(i);
						_player_result.chun_tian[i] = 2;
					}
					if (GRR != null && i != GRR._banker_player && this._out_card_times[i] == 0) {
						_player_result.chun_tian[i] = 1;
						if (this._bao_pei_palyer == GameConstants.INVALID_SEAT) {
							game_end_pdk.addChunTianPlayer(i);
						} else {
							if (this._bao_pei_palyer == i) {
								game_end_pdk.addChunTianPlayer(i);
							}
						}

					}
				}
				if (_qie_pai_seat == index && this.has_rule(GameConstants.GAME_RULE_QIE_PAI)) {
					game_end_pdk.setIsshowQiepaiBtn(true);
				} else {
					game_end_pdk.setIsshowQiepaiBtn(false);
				}
				game_end_pdk.setHongTaoPlayer(_hong_tao_palyer);
			} else {
				game_end_pdk.setHongTaoPlayer(GameConstants.INVALID_SEAT);
				game_end_pdk.setBaoPeiPlayer(GameConstants.INVALID_SEAT);
				game_end_pdk.setFanDiPlayer(GameConstants.INVALID_SEAT);
				game_end_pdk.setTaoPaoPlayer(GameConstants.INVALID_SEAT);
				game_end_pdk.setIsshowQiepaiBtn(false);
			}
			this.load_player_info_data_game_end(game_end_pdk);
			game_end_pdk.setGameRound(_game_round);
			game_end_pdk.setCurRound(_cur_round);

			if (GRR != null) {
				game_end_pdk.setBankerPlayer(GRR._banker_player);// 庄家
				for (int i = 0; i < getTablePlayerNumber(); i++) {
					game_end.addGameScore(end_score[i]);
					game_end_pdk.addCardCount(GRR._card_count[i]);
					game_end_pdk.addBoomCardNum(_boom_num[i]);
					game_end_pdk.addEndScore(end_score[i]);
					Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
					for (int j = 0; j < GRR._card_count[i]; j++) {
						cards_card.addItem(GRR._cards_data[i][j]);
					}
					game_end_pdk.addCardsData(i, cards_card);

				}
				int playernum = this.getTablePlayerNumber();
				if (playernum == 2) {
					Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
					for (int j = 0; j < this.get_hand_card_count_max(); j++) {
						cards_card.addItem(GRR._cards_data[2][j]);
					}
					game_end_pdk.addCardsData(2, cards_card);
				}
				game_end.setStartTime(GRR._start_time);
				game_end.setGameTypeIndex(GRR._game_type_index);
			}

			boolean end = false;
			if (reason == GameConstants.Game_End_NORMAL || reason == GameConstants.Game_End_DRAW) {
				if (_cur_round >= _game_round) {// 局数到了
					end = true;
					game_end.setPlayerResult(this.process_player_result(reason));
					real_reason = GameConstants.Game_End_ROUND_OVER;
					for (int i = 0; i < this.getTablePlayerNumber(); i++) {
						game_end_pdk.addAllBoomCardNum(this._all_boom_num[i]);
						game_end_pdk.addAllEndScore(this._game_score[i]);
						game_end_pdk.addEndScoreMax(this._game_score_max[i]);
						game_end_pdk.addWinNum(this._win_num[i]);
						game_end_pdk.addLoseNum(this._lose_num[i]);
					}

				}
			} else if (reason == GameConstants.Game_End_RELEASE_PLAY
					|| reason == GameConstants.Game_End_RELEASE_NO_BEGIN
					|| reason == GameConstants.Game_End_RELEASE_RESULT
					|| reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT
					|| reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT
					|| reason == GameConstants.Game_End_RELEASE_SYSTEM) {
				game_end.setPlayerResult(this.process_player_result(reason));
				real_reason = GameConstants.Game_End_RELEASE_PLAY;
				end = true;
				for (int i = 0; i < this.getTablePlayerNumber(); i++) {
					game_end_pdk.addAllBoomCardNum(this._all_boom_num[i] - _boom_num[i]);
					game_end_pdk.addAllEndScore(this._game_score[i]);
					game_end_pdk.addEndScoreMax(this._game_score_max[i]);
					game_end_pdk.addWinNum(this._win_num[i]);
					game_end_pdk.addLoseNum(this._lose_num[i]);
				}
			}

			game_end_pdk.setReason(real_reason);
			////////////////////////////////////////////////////////////////////// 得分总的
			game_end.setRoomInfo(getRoomInfo());
			game_end.setEndType(real_reason);
			game_end.setGameRound(_game_round);
			game_end.setCurRound(_cur_round);
			game_end.setCommResponse(PBUtil.toByteString(game_end_pdk));
			game_end.setRoundOverType(1);
			game_end.setEndTime(System.currentTimeMillis() / 1000L);// 结束时间

			roomResponse.setGameEnd(game_end);
			this.send_response_to_player(index, roomResponse);
		}
		GameEndResponse.Builder game_end = GameEndResponse.newBuilder();
		PukeGameEndPdk_xy.Builder game_end_pdk = PukeGameEndPdk_xy.newBuilder();
		RoomInfoPdk.Builder room_info = getRoomInfoPdk();
		game_end_pdk.setRoomInfo(room_info);
		if (reason == GameConstants.Game_End_NORMAL) {
			game_end_pdk.setHongTaoPlayer(_hong_tao_palyer);
			game_end_pdk.setBaoPeiPlayer(_bao_pei_palyer);
			game_end_pdk.setFanDiPlayer(_fan_di_palyer);
			game_end_pdk.setTaoPaoPlayer(_tao_pao_palyer);
			game_end_pdk.setZhaNiaoPlayer(GameConstants.INVALID_SEAT);
			// 春天玩家
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				if (this.has_rule(GameConstants.GAME_RULE_TWO_PLAY) && this.has_rule(GameConstants.GAME_RULE_KE_FAN_DE)
						&& i == GRR._banker_player && this._out_card_times[i] == 1 && GRR._card_count[i] != 0) {
					game_end_pdk.setFanDiPlayer(i);
				}
				if (GRR != null && i != GRR._banker_player && this._out_card_times[i] == 0) {
					if (this._bao_pei_palyer == GameConstants.INVALID_SEAT) {
						game_end_pdk.addChunTianPlayer(i);
					} else {
						if (this._bao_pei_palyer == i) {
							game_end_pdk.addChunTianPlayer(i);
						}
					}

				}
			}

			game_end_pdk.setHongTaoPlayer(_hong_tao_palyer);
		} else {
			game_end_pdk.setHongTaoPlayer(GameConstants.INVALID_SEAT);
			game_end_pdk.setBaoPeiPlayer(GameConstants.INVALID_SEAT);
			game_end_pdk.setFanDiPlayer(GameConstants.INVALID_SEAT);
			game_end_pdk.setTaoPaoPlayer(GameConstants.INVALID_SEAT);
			game_end_pdk.setIsshowQiepaiBtn(false);
		}
		this.load_player_info_data_game_end(game_end_pdk);
		game_end_pdk.setGameRound(_game_round);
		game_end_pdk.setCurRound(_cur_round);

		if (GRR != null) {
			game_end_pdk.setBankerPlayer(GRR._banker_player);// 庄家
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				game_end.addGameScore(end_score[i]);
				game_end_pdk.addCardCount(GRR._card_count[i]);
				game_end_pdk.addBoomCardNum(_boom_num[i]);
				game_end_pdk.addEndScore(end_score[i]);
				Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GRR._card_count[i]; j++) {
					cards_card.addItem(GRR._cards_data[i][j]);
				}
				game_end_pdk.addCardsData(i, cards_card);

			}
			int playernum = this.getTablePlayerNumber();
			if (playernum == 2) {
				Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < this.get_hand_card_count_max(); j++) {
					cards_card.addItem(GRR._cards_data[2][j]);
				}
				game_end_pdk.addCardsData(2, cards_card);
			}
			game_end.setStartTime(GRR._start_time);
			game_end.setGameTypeIndex(GRR._game_type_index);
		}

		boolean end = false;
		if (reason == GameConstants.Game_End_NORMAL || reason == GameConstants.Game_End_DRAW) {
			if (_cur_round >= _game_round) {// 局数到了
				end = true;
				game_end.setPlayerResult(this.process_player_result(reason));
				real_reason = GameConstants.Game_End_ROUND_OVER;
				for (int i = 0; i < this.getTablePlayerNumber(); i++) {
					game_end_pdk.addAllBoomCardNum(this._all_boom_num[i]);
					game_end_pdk.addAllEndScore(this._game_score[i]);
					game_end_pdk.addEndScoreMax(this._game_score_max[i]);
					game_end_pdk.addWinNum(this._win_num[i]);
					game_end_pdk.addLoseNum(this._lose_num[i]);
				}

			}
		} else if (reason == GameConstants.Game_End_RELEASE_PLAY || reason == GameConstants.Game_End_RELEASE_NO_BEGIN
				|| reason == GameConstants.Game_End_RELEASE_RESULT
				|| reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_SYSTEM) {
			game_end.setPlayerResult(this.process_player_result(reason));
			real_reason = GameConstants.Game_End_RELEASE_PLAY;
			end = true;
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				game_end_pdk.addAllBoomCardNum(this._all_boom_num[i] - _boom_num[i]);
				game_end_pdk.addAllEndScore(this._game_score[i]);
				game_end_pdk.addEndScoreMax(this._game_score_max[i]);
				game_end_pdk.addWinNum(this._win_num[i]);
				game_end_pdk.addLoseNum(this._lose_num[i]);
			}
		}

		game_end_pdk.setReason(real_reason);
		////////////////////////////////////////////////////////////////////// 得分总的
		game_end.setRoomInfo(getRoomInfo());
		game_end.setEndType(real_reason);
		game_end.setGameRound(_game_round);
		game_end.setCurRound(_cur_round);
		game_end.setCommResponse(PBUtil.toByteString(game_end_pdk));
		game_end.setRoundOverType(1);
		game_end.setEndTime(System.currentTimeMillis() / 1000L);// 结束时间

		roomResponse.setGameEnd(game_end);

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

		GRR = null;
		if (end)// 删除
		{
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				this.handler_request_trustee(i, false, 0);
			}
			PlayerServiceImpl.getInstance().delRoomId(this.getRoom_id());
		}

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_out_bomb_score[i] = 0;
			_boom_num[i] = 0;
			_out_bomb_score_zhaniao[i] = 0;
		}
		// 错误断言
		return false;
	}

	public boolean handler_game_finish_pdk_match(int seat_index, int reason) {
		int real_reason = reason;

		int count = getTablePlayerNumber();
		if (count == 0) {
			count = this.getTablePlayerNumber();
		}

		// 重制准备
		for (int i = 0; i < count; i++) {
			_player_ready[i] = 0;
		}
		for (int i = 0; i < count; i++) {
			if (this._cur_round > 0) {
				// 结束后刷新玩家
				RoomResponse.Builder roomResponse2 = RoomResponse.newBuilder();
				roomResponse2.setGameStatus(_game_status);
				roomResponse2.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);// 刷新玩家
				this.load_player_info_data(roomResponse2);
				this.send_response_to_player(i, roomResponse2);
			}
		}

		int end_score[] = new int[this.getTablePlayerNumber()];
		Arrays.fill(end_score, 0);
		// 计算分数
		if (reason == GameConstants.Game_End_NORMAL) {
			cal_score_pdk_match(end_score);
		} else {
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				_player_result.game_score[i] -= _out_bomb_score[i];
			}
		}
		// 最高得分
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (end_score[i] > _game_score_max[i]) {
				_game_score_max[i] = end_score[i];
			}
		}
		// 失分最多
		_qie_pai_seat = GameConstants.INVALID_SEAT;
		int lost_score = 0;
		if (GRR != null) {
			this.operate_player_data();

			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				int index = (GRR._banker_player + 1 + i) % this.getTablePlayerNumber();
				if (end_score[index] < lost_score) {
					lost_score = end_score[index];
					_qie_pai_seat = index;
				}
			}
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_PDK_GAME_END);
		for (int index = 0; index < this.getTablePlayerNumber(); index++) {
			GameEndResponse.Builder game_end = GameEndResponse.newBuilder();
			PukeGameEndPdk_xy.Builder game_end_pdk = PukeGameEndPdk_xy.newBuilder();
			RoomInfoPdk.Builder room_info = getRoomInfoPdk();
			game_end_pdk.setRoomInfo(room_info);
			if (reason == GameConstants.Game_End_NORMAL) {
				game_end_pdk.setHongTaoPlayer(_hong_tao_palyer);
				game_end_pdk.setBaoPeiPlayer(_bao_pei_palyer);
				game_end_pdk.setFanDiPlayer(_fan_di_palyer);
				game_end_pdk.setTaoPaoPlayer(_tao_pao_palyer);
				game_end_pdk.setZhaNiaoPlayer(GameConstants.INVALID_SEAT);
				// 春天玩家
				for (int i = 0; i < this.getTablePlayerNumber(); i++) {
					if (this.has_rule(GameConstants.GAME_RULE_TWO_PLAY)
							&& this.has_rule(GameConstants.GAME_RULE_KE_FAN_DE) && i == GRR._banker_player
							&& this._out_card_times[i] == 1 && GRR._card_count[i] != 0) {
						game_end_pdk.setFanDiPlayer(i);
						_player_result.chun_tian[i] = 2;
					}
					if (GRR != null && i != GRR._banker_player && this._out_card_times[i] == 0) {
						_player_result.chun_tian[i] = 1;
						if (this._bao_pei_palyer == GameConstants.INVALID_SEAT) {
							game_end_pdk.addChunTianPlayer(i);
						} else {
							if (this._bao_pei_palyer == i) {
								game_end_pdk.addChunTianPlayer(i);
							}
						}

					}
				}
				if (_qie_pai_seat == index && this.has_rule(GameConstants.GAME_RULE_QIE_PAI)) {
					game_end_pdk.setIsshowQiepaiBtn(true);
				} else {
					game_end_pdk.setIsshowQiepaiBtn(false);
				}
				game_end_pdk.setHongTaoPlayer(_hong_tao_palyer);
			} else {
				game_end_pdk.setHongTaoPlayer(GameConstants.INVALID_SEAT);
				game_end_pdk.setBaoPeiPlayer(GameConstants.INVALID_SEAT);
				game_end_pdk.setFanDiPlayer(GameConstants.INVALID_SEAT);
				game_end_pdk.setTaoPaoPlayer(GameConstants.INVALID_SEAT);
				game_end_pdk.setIsshowQiepaiBtn(false);
			}
			this.load_player_info_data_game_end(game_end_pdk);
			game_end_pdk.setGameRound(_game_round);
			game_end_pdk.setCurRound(_cur_round);

			if (GRR != null) {
				game_end_pdk.setBankerPlayer(GRR._banker_player);// 庄家
				for (int i = 0; i < getTablePlayerNumber(); i++) {
					game_end.addGameScore(end_score[i]);
					game_end_pdk.addCardCount(GRR._card_count[i]);
					game_end_pdk.addBoomCardNum(_boom_num[i]);
					game_end_pdk.addEndScore(end_score[i]);
					Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
					for (int j = 0; j < GRR._card_count[i]; j++) {
						cards_card.addItem(GRR._cards_data[i][j]);
					}
					game_end_pdk.addCardsData(i, cards_card);

				}
				int playernum = this.getTablePlayerNumber();
				if (playernum == 2) {
					Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
					for (int j = 0; j < this.get_hand_card_count_max(); j++) {
						cards_card.addItem(GRR._cards_data[2][j]);
					}
					game_end_pdk.addCardsData(2, cards_card);
				}
				game_end.setStartTime(GRR._start_time);
				game_end.setGameTypeIndex(GRR._game_type_index);
			}

			boolean end = false;
			if (reason == GameConstants.Game_End_NORMAL || reason == GameConstants.Game_End_DRAW) {
				if (_cur_round >= _game_round) {// 局数到了
					end = true;
					game_end.setPlayerResult(this.process_player_result(reason));
					real_reason = GameConstants.Game_End_ROUND_OVER;
					for (int i = 0; i < this.getTablePlayerNumber(); i++) {
						game_end_pdk.addAllBoomCardNum(this._all_boom_num[i]);
						game_end_pdk.addAllEndScore(this._game_score[i]);
						game_end_pdk.addEndScoreMax(this._game_score_max[i]);
						game_end_pdk.addWinNum(this._win_num[i]);
						game_end_pdk.addLoseNum(this._lose_num[i]);
					}

				}
			} else if (reason == GameConstants.Game_End_RELEASE_PLAY
					|| reason == GameConstants.Game_End_RELEASE_NO_BEGIN
					|| reason == GameConstants.Game_End_RELEASE_RESULT
					|| reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT
					|| reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT
					|| reason == GameConstants.Game_End_RELEASE_SYSTEM) {
				game_end.setPlayerResult(this.process_player_result(reason));
				real_reason = GameConstants.Game_End_RELEASE_PLAY;
				end = true;
				for (int i = 0; i < this.getTablePlayerNumber(); i++) {
					game_end_pdk.addAllBoomCardNum(this._all_boom_num[i] - _boom_num[i]);
					game_end_pdk.addAllEndScore(this._game_score[i]);
					game_end_pdk.addEndScoreMax(this._game_score_max[i]);
					game_end_pdk.addWinNum(this._win_num[i]);
					game_end_pdk.addLoseNum(this._lose_num[i]);
				}
			}

			game_end_pdk.setReason(real_reason);
			////////////////////////////////////////////////////////////////////// 得分总的
			game_end.setRoomInfo(getRoomInfo());
			game_end.setEndType(real_reason);
			game_end.setGameRound(_game_round);
			game_end.setCurRound(_cur_round);
			game_end.setCommResponse(PBUtil.toByteString(game_end_pdk));
			game_end.setRoundOverType(1);
			game_end.setEndTime(System.currentTimeMillis() / 1000L);// 结束时间

			roomResponse.setGameEnd(game_end);
			this.send_response_to_player(index, roomResponse);
		}
		GameEndResponse.Builder game_end = GameEndResponse.newBuilder();
		PukeGameEndPdk_xy.Builder game_end_pdk = PukeGameEndPdk_xy.newBuilder();
		RoomInfoPdk.Builder room_info = getRoomInfoPdk();
		game_end_pdk.setRoomInfo(room_info);
		if (reason == GameConstants.Game_End_NORMAL) {
			game_end_pdk.setHongTaoPlayer(_hong_tao_palyer);
			game_end_pdk.setBaoPeiPlayer(_bao_pei_palyer);
			game_end_pdk.setFanDiPlayer(_fan_di_palyer);
			game_end_pdk.setTaoPaoPlayer(_tao_pao_palyer);
			game_end_pdk.setZhaNiaoPlayer(GameConstants.INVALID_SEAT);
			// 春天玩家
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				if (this.has_rule(GameConstants.GAME_RULE_TWO_PLAY) && this.has_rule(GameConstants.GAME_RULE_KE_FAN_DE)
						&& i == GRR._banker_player && this._out_card_times[i] == 1 && GRR._card_count[i] != 0) {
					game_end_pdk.setFanDiPlayer(i);
				}
				if (GRR != null && i != GRR._banker_player && this._out_card_times[i] == 0) {
					if (this._bao_pei_palyer == GameConstants.INVALID_SEAT) {
						game_end_pdk.addChunTianPlayer(i);
					} else {
						if (this._bao_pei_palyer == i) {
							game_end_pdk.addChunTianPlayer(i);
						}
					}

				}
			}

			game_end_pdk.setHongTaoPlayer(_hong_tao_palyer);
		} else {
			game_end_pdk.setHongTaoPlayer(GameConstants.INVALID_SEAT);
			game_end_pdk.setBaoPeiPlayer(GameConstants.INVALID_SEAT);
			game_end_pdk.setFanDiPlayer(GameConstants.INVALID_SEAT);
			game_end_pdk.setTaoPaoPlayer(GameConstants.INVALID_SEAT);
			game_end_pdk.setIsshowQiepaiBtn(false);
		}
		this.load_player_info_data_game_end(game_end_pdk);
		game_end_pdk.setGameRound(_game_round);
		game_end_pdk.setCurRound(_cur_round);

		if (GRR != null) {
			game_end_pdk.setBankerPlayer(GRR._banker_player);// 庄家
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				game_end.addGameScore(end_score[i]);
				game_end_pdk.addCardCount(GRR._card_count[i]);
				game_end_pdk.addBoomCardNum(_boom_num[i]);
				game_end_pdk.addEndScore(end_score[i]);
				Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GRR._card_count[i]; j++) {
					cards_card.addItem(GRR._cards_data[i][j]);
				}
				game_end_pdk.addCardsData(i, cards_card);

			}
			int playernum = this.getTablePlayerNumber();
			if (playernum == 2) {
				Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < this.get_hand_card_count_max(); j++) {
					cards_card.addItem(GRR._cards_data[2][j]);
				}
				game_end_pdk.addCardsData(2, cards_card);
			}
			game_end.setStartTime(GRR._start_time);
			game_end.setGameTypeIndex(GRR._game_type_index);
		}

		boolean end = false;
		if (reason == GameConstants.Game_End_NORMAL || reason == GameConstants.Game_End_DRAW) {
			if (_cur_round >= _game_round) {// 局数到了
				end = true;
				game_end.setPlayerResult(this.process_player_result(reason));
				real_reason = GameConstants.Game_End_ROUND_OVER;
				for (int i = 0; i < this.getTablePlayerNumber(); i++) {
					game_end_pdk.addAllBoomCardNum(this._all_boom_num[i]);
					game_end_pdk.addAllEndScore(this._game_score[i]);
					game_end_pdk.addEndScoreMax(this._game_score_max[i]);
					game_end_pdk.addWinNum(this._win_num[i]);
					game_end_pdk.addLoseNum(this._lose_num[i]);
				}

			}
		} else if (reason == GameConstants.Game_End_RELEASE_PLAY || reason == GameConstants.Game_End_RELEASE_NO_BEGIN
				|| reason == GameConstants.Game_End_RELEASE_RESULT
				|| reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_SYSTEM) {
			game_end.setPlayerResult(this.process_player_result(reason));
			real_reason = GameConstants.Game_End_RELEASE_PLAY;
			end = true;
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				game_end_pdk.addAllBoomCardNum(this._all_boom_num[i] - _boom_num[i]);
				game_end_pdk.addAllEndScore(this._game_score[i]);
				game_end_pdk.addEndScoreMax(this._game_score_max[i]);
				game_end_pdk.addWinNum(this._win_num[i]);
				game_end_pdk.addLoseNum(this._lose_num[i]);
			}
		}

		game_end_pdk.setReason(real_reason);
		////////////////////////////////////////////////////////////////////// 得分总的
		game_end.setRoomInfo(getRoomInfo());
		game_end.setEndType(real_reason);
		game_end.setGameRound(_game_round);
		game_end.setCurRound(_cur_round);
		game_end.setCommResponse(PBUtil.toByteString(game_end_pdk));
		game_end.setRoundOverType(1);
		game_end.setEndTime(System.currentTimeMillis() / 1000L);// 结束时间

		roomResponse.setGameEnd(game_end);

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

		GRR = null;
		if (end)// 删除
		{
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				this.handler_request_trustee(i, false, 0);
			}
			PlayerServiceImpl.getInstance().delRoomId(this.getRoom_id());
		}

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_out_bomb_score[i] = 0;
			_boom_num[i] = 0;
			_out_bomb_score_zhaniao[i] = 0;
		}
		// 错误断言
		return false;
	}

	@Override
	public boolean handler_game_finish_pdk_jd(int seat_index, int reason) {
		int real_reason = reason;

		int count = getTablePlayerNumber();
		if (count == 0) {
			count = this.getTablePlayerNumber();
		}

		// 重制准备
		for (int i = 0; i < count; i++) {
			_player_ready[i] = 0;
		}
		for (int i = 0; i < count; i++) {
			if (this._cur_round > 0) {
				// 结束后刷新玩家
				RoomResponse.Builder roomResponse2 = RoomResponse.newBuilder();
				roomResponse2.setGameStatus(_game_status);
				roomResponse2.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);// 刷新玩家
				this.load_player_info_data(roomResponse2);
				this.send_response_to_player(i, roomResponse2);
			}
		}

		int end_score[] = new int[this.getTablePlayerNumber()];
		Arrays.fill(end_score, 0);
		// 计算分数
		if (reason == GameConstants.Game_End_NORMAL) {
			cal_score_pdk_jd(end_score);
		} else {
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				_player_result.game_score[i] -= _out_bomb_score[i];
			}
		}
		// 最高得分
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (end_score[i] > _game_score_max[i]) {
				_game_score_max[i] = end_score[i];
			}
		}
		// 失分最多
		_qie_pai_seat = GameConstants.INVALID_SEAT;
		int lost_score = 0;
		if (GRR != null) {
			this.operate_player_data();

			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				int index = (GRR._banker_player + 1 + i) % this.getTablePlayerNumber();
				if (end_score[index] < lost_score) {
					lost_score = end_score[index];
					_qie_pai_seat = index;
				}
			}
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_PDK_GAME_END);
		for (int index = 0; index < this.getTablePlayerNumber(); index++) {
			GameEndResponse.Builder game_end = GameEndResponse.newBuilder();
			PukeGameEndPdk_xy.Builder game_end_pdk = PukeGameEndPdk_xy.newBuilder();
			RoomInfoPdk.Builder room_info = getRoomInfoPdk();
			game_end_pdk.setRoomInfo(room_info);
			if (reason == GameConstants.Game_End_NORMAL) {
				game_end_pdk.setHongTaoPlayer(_hong_tao_palyer);
				game_end_pdk.setBaoPeiPlayer(_bao_pei_palyer);
				game_end_pdk.setFanDiPlayer(_fan_di_palyer);
				game_end_pdk.setTaoPaoPlayer(_tao_pao_palyer);
				game_end_pdk.setZhaNiaoPlayer(GameConstants.INVALID_SEAT);
				// 春天玩家
				for (int i = 0; i < this.getTablePlayerNumber(); i++) {
					if (this.has_rule(GameConstants.GAME_RULE_TWO_PLAY)
							&& this.has_rule(GameConstants.GAME_RULE_KE_FAN_DE) && i == GRR._banker_player
							&& this._out_card_times[i] == 1 && GRR._card_count[i] != 0) {
						game_end_pdk.setFanDiPlayer(i);
						_player_result.chun_tian[i] = 2;
					}
					if (GRR != null && i != GRR._banker_player && this._out_card_times[i] == 0) {
						_player_result.chun_tian[i] = 1;
						if (this._bao_pei_palyer == GameConstants.INVALID_SEAT) {
							game_end_pdk.addChunTianPlayer(i);
						} else {
							if (this._bao_pei_palyer == i) {
								game_end_pdk.addChunTianPlayer(i);
							}
						}

					}
				}
				if (_qie_pai_seat == index && this.has_rule(GameConstants.GAME_RULE_QIE_PAI)) {
					game_end_pdk.setIsshowQiepaiBtn(true);
				} else {
					game_end_pdk.setIsshowQiepaiBtn(false);
				}
				game_end_pdk.setHongTaoPlayer(_hong_tao_palyer);
			} else {
				game_end_pdk.setHongTaoPlayer(GameConstants.INVALID_SEAT);
				game_end_pdk.setBaoPeiPlayer(GameConstants.INVALID_SEAT);
				game_end_pdk.setFanDiPlayer(GameConstants.INVALID_SEAT);
				game_end_pdk.setTaoPaoPlayer(GameConstants.INVALID_SEAT);
				game_end_pdk.setIsshowQiepaiBtn(false);
			}
			this.load_player_info_data_game_end(game_end_pdk);
			game_end_pdk.setGameRound(_game_round);
			game_end_pdk.setCurRound(_cur_round);

			if (GRR != null) {
				game_end_pdk.setBankerPlayer(GRR._banker_player);// 庄家
				for (int i = 0; i < getTablePlayerNumber(); i++) {
					game_end.addGameScore(end_score[i]);
					game_end_pdk.addCardCount(GRR._card_count[i]);
					game_end_pdk.addBoomCardNum(_boom_num[i]);
					game_end_pdk.addEndScore(end_score[i]);
					Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
					for (int j = 0; j < GRR._card_count[i]; j++) {
						cards_card.addItem(GRR._cards_data[i][j]);
					}
					game_end_pdk.addCardsData(i, cards_card);

				}
				int playernum = this.getTablePlayerNumber();
				if (playernum == 2) {
					Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
					for (int j = 0; j < this.get_hand_card_count_max(); j++) {
						cards_card.addItem(GRR._cards_data[2][j]);
					}
					game_end_pdk.addCardsData(2, cards_card);
				}
				game_end.setStartTime(GRR._start_time);
				game_end.setGameTypeIndex(GRR._game_type_index);
			}

			boolean end = false;
			if (reason == GameConstants.Game_End_NORMAL || reason == GameConstants.Game_End_DRAW) {
				if (_cur_round >= _game_round) {// 局数到了
					end = true;
					game_end.setPlayerResult(this.process_player_result(reason));
					real_reason = GameConstants.Game_End_ROUND_OVER;
					for (int i = 0; i < this.getTablePlayerNumber(); i++) {
						game_end_pdk.addAllBoomCardNum(this._all_boom_num[i]);
						game_end_pdk.addAllEndScore(this._game_score[i]);
						game_end_pdk.addEndScoreMax(this._game_score_max[i]);
						game_end_pdk.addWinNum(this._win_num[i]);
						game_end_pdk.addLoseNum(this._lose_num[i]);
					}

				}
			} else if (reason == GameConstants.Game_End_RELEASE_PLAY
					|| reason == GameConstants.Game_End_RELEASE_NO_BEGIN
					|| reason == GameConstants.Game_End_RELEASE_RESULT
					|| reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT
					|| reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT
					|| reason == GameConstants.Game_End_RELEASE_SYSTEM) {
				game_end.setPlayerResult(this.process_player_result(reason));
				real_reason = GameConstants.Game_End_RELEASE_PLAY;
				end = true;
				for (int i = 0; i < this.getTablePlayerNumber(); i++) {
					game_end_pdk.addAllBoomCardNum(this._all_boom_num[i] - _boom_num[i]);
					game_end_pdk.addAllEndScore(this._game_score[i]);
					game_end_pdk.addEndScoreMax(this._game_score_max[i]);
					game_end_pdk.addWinNum(this._win_num[i]);
					game_end_pdk.addLoseNum(this._lose_num[i]);
				}
			}

			game_end_pdk.setReason(real_reason);
			////////////////////////////////////////////////////////////////////// 得分总的
			game_end.setRoomInfo(getRoomInfo());
			game_end.setEndType(real_reason);
			game_end.setGameRound(_game_round);
			game_end.setCurRound(_cur_round);
			game_end.setCommResponse(PBUtil.toByteString(game_end_pdk));
			game_end.setRoundOverType(1);
			game_end.setEndTime(System.currentTimeMillis() / 1000L);// 结束时间

			roomResponse.setGameEnd(game_end);
			this.send_response_to_player(index, roomResponse);
		}
		GameEndResponse.Builder game_end = GameEndResponse.newBuilder();
		PukeGameEndPdk_xy.Builder game_end_pdk = PukeGameEndPdk_xy.newBuilder();
		RoomInfoPdk.Builder room_info = getRoomInfoPdk();
		game_end_pdk.setRoomInfo(room_info);
		if (reason == GameConstants.Game_End_NORMAL) {
			game_end_pdk.setHongTaoPlayer(_hong_tao_palyer);
			game_end_pdk.setBaoPeiPlayer(_bao_pei_palyer);
			game_end_pdk.setFanDiPlayer(_fan_di_palyer);
			game_end_pdk.setTaoPaoPlayer(_tao_pao_palyer);
			game_end_pdk.setZhaNiaoPlayer(GameConstants.INVALID_SEAT);
			// 春天玩家
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				if (this.has_rule(GameConstants.GAME_RULE_TWO_PLAY) && this.has_rule(GameConstants.GAME_RULE_KE_FAN_DE)
						&& i == GRR._banker_player && this._out_card_times[i] == 1 && GRR._card_count[i] != 0) {
					game_end_pdk.setFanDiPlayer(i);
				}
				if (GRR != null && i != GRR._banker_player && this._out_card_times[i] == 0) {
					if (this._bao_pei_palyer == GameConstants.INVALID_SEAT) {
						game_end_pdk.addChunTianPlayer(i);
					} else {
						if (this._bao_pei_palyer == i) {
							game_end_pdk.addChunTianPlayer(i);
						}
					}

				}
			}

			game_end_pdk.setHongTaoPlayer(_hong_tao_palyer);
		} else {
			game_end_pdk.setHongTaoPlayer(GameConstants.INVALID_SEAT);
			game_end_pdk.setBaoPeiPlayer(GameConstants.INVALID_SEAT);
			game_end_pdk.setFanDiPlayer(GameConstants.INVALID_SEAT);
			game_end_pdk.setTaoPaoPlayer(GameConstants.INVALID_SEAT);
			game_end_pdk.setIsshowQiepaiBtn(false);
		}
		this.load_player_info_data_game_end(game_end_pdk);
		game_end_pdk.setGameRound(_game_round);
		game_end_pdk.setCurRound(_cur_round);

		if (GRR != null) {
			game_end_pdk.setBankerPlayer(GRR._banker_player);// 庄家
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				game_end.addGameScore(end_score[i]);
				game_end_pdk.addCardCount(GRR._card_count[i]);
				game_end_pdk.addBoomCardNum(_boom_num[i]);
				game_end_pdk.addEndScore(end_score[i]);
				Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GRR._card_count[i]; j++) {
					cards_card.addItem(GRR._cards_data[i][j]);
				}
				game_end_pdk.addCardsData(i, cards_card);

			}
			int playernum = this.getTablePlayerNumber();
			if (playernum == 2) {
				Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < this.get_hand_card_count_max(); j++) {
					cards_card.addItem(GRR._cards_data[2][j]);
				}
				game_end_pdk.addCardsData(2, cards_card);
			}
			game_end.setStartTime(GRR._start_time);
			game_end.setGameTypeIndex(GRR._game_type_index);
		}

		boolean end = false;
		if (reason == GameConstants.Game_End_NORMAL || reason == GameConstants.Game_End_DRAW) {
			if (_cur_round >= _game_round) {// 局数到了
				end = true;
				game_end.setPlayerResult(this.process_player_result(reason));
				real_reason = GameConstants.Game_End_ROUND_OVER;
				for (int i = 0; i < this.getTablePlayerNumber(); i++) {
					game_end_pdk.addAllBoomCardNum(this._all_boom_num[i]);
					game_end_pdk.addAllEndScore(this._game_score[i]);
					game_end_pdk.addEndScoreMax(this._game_score_max[i]);
					game_end_pdk.addWinNum(this._win_num[i]);
					game_end_pdk.addLoseNum(this._lose_num[i]);
				}

			}
		} else if (reason == GameConstants.Game_End_RELEASE_PLAY || reason == GameConstants.Game_End_RELEASE_NO_BEGIN
				|| reason == GameConstants.Game_End_RELEASE_RESULT
				|| reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_SYSTEM) {
			game_end.setPlayerResult(this.process_player_result(reason));
			real_reason = GameConstants.Game_End_RELEASE_PLAY;
			end = true;
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				game_end_pdk.addAllBoomCardNum(this._all_boom_num[i] - _boom_num[i]);
				game_end_pdk.addAllEndScore(this._game_score[i]);
				game_end_pdk.addEndScoreMax(this._game_score_max[i]);
				game_end_pdk.addWinNum(this._win_num[i]);
				game_end_pdk.addLoseNum(this._lose_num[i]);
			}
		}

		game_end_pdk.setReason(real_reason);
		////////////////////////////////////////////////////////////////////// 得分总的
		game_end.setRoomInfo(getRoomInfo());
		game_end.setEndType(real_reason);
		game_end.setGameRound(_game_round);
		game_end.setCurRound(_cur_round);
		game_end.setCommResponse(PBUtil.toByteString(game_end_pdk));
		game_end.setRoundOverType(1);
		game_end.setEndTime(System.currentTimeMillis() / 1000L);// 结束时间

		roomResponse.setGameEnd(game_end);

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

		GRR = null;
		if (end)// 删除
		{
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				this.handler_request_trustee(i, false, 0);
			}
			PlayerServiceImpl.getInstance().delRoomId(this.getRoom_id());
		}

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_out_bomb_score[i] = 0;
			_boom_num[i] = 0;
			_out_bomb_score_zhaniao[i] = 0;
		}
		// 错误断言
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
		if (is_cancel) {//
			if (this.get_players()[seat_index] == null) {
				return false;
			}
			if (GameConstants.GS_MJ_FREE != _game_status && GameConstants.GS_MJ_WAIT != _game_status) {
				return false;
			}
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

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
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
		if ((GameConstants.GS_MJ_FREE != _game_status && GameConstants.GS_MJ_WAIT != _game_status)
				&& this.get_players()[seat_index] != null) {
			// this.send_play_data(seat_index);

			if (this._handler != null && !_is_in_qie) {
				this._handler.handler_player_be_in_room(this, seat_index);
			} else if (!_is_in_qie && GRR != null) {
				RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
				roomResponse.setType(MsgConstants.RESPONSE_PDK_RECONNECT_DATA);

				TableResponse_PDK_xy.Builder tableResponse_pdk = TableResponse_PDK_xy.newBuilder();
				load_player_info_data_reconnect(tableResponse_pdk);
				RoomInfoPdk.Builder room_info = getRoomInfoPdk();
				tableResponse_pdk.setRoomInfo(room_info);

				tableResponse_pdk.setBankerPlayer(GRR._banker_player);
				tableResponse_pdk.setCurrentPlayer(_current_player);
				tableResponse_pdk.setPrevPlayer(_prev_palyer);

				for (int i = 0; i < getTablePlayerNumber(); i++) {
					tableResponse_pdk.addOutCardsCount(GRR._cur_round_count[i]);
					tableResponse_pdk.addPlayerPass(GRR._cur_round_pass[i]);
					Int32ArrayResponse.Builder out_cards = Int32ArrayResponse.newBuilder();
					Int32ArrayResponse.Builder out_change_cards = Int32ArrayResponse.newBuilder();
					for (int j = 0; j < GRR._cur_round_count[i]; j++) {
						if (GRR._cur_round_count[i] > 0) {
							out_cards.addItem(GRR._cur_round_data[i][j]);
							out_change_cards.addItem(GRR._cur_round_data[i][j]);
						}
					}
					if (has_rule(GameConstants.GAME_RULE_DISPLAY_CARD)) {
						tableResponse_pdk.addCardCount(GRR._card_count[i]);
					} else {
						if (i == seat_index) {
							tableResponse_pdk.addCardCount(GRR._card_count[i]);
						} else {
							tableResponse_pdk.addCardCount(-1);
						}
					}
					tableResponse_pdk.addCardType(GRR._cur_card_type[i]);
					tableResponse_pdk.addOutCardsData(i, out_cards);
					tableResponse_pdk.addChangeCardsData(out_change_cards);
					Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
					for (int j = 0; j < GRR._card_count[i]; j++) {
						cards_card.addItem(GameConstants.INVALID_CARD);
					}
					tableResponse_pdk.addCardsData(i, cards_card);
				}

				// 手牌--将自己的手牌数据发给自己
				Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GRR._card_count[seat_index]; j++) {
					if (this.has_rule(GameConstants.GAME_RULE_FANG_ZUO_BI) && seat_index != _current_player) {
						cards_card.addItem(GameConstants.BLACK_CARD);
					} else {
						cards_card.addItem(GRR._cards_data[seat_index][j]);
					}
				}
				tableResponse_pdk.setCardsData(seat_index, cards_card);
				for (int i = 0; i < _turn_out_card_count; i++) {
					if (_turn_out_card_count > 0) {
						tableResponse_pdk.addPrCardsData(_turn_out_card_data[i]);
						tableResponse_pdk.addPrChangeCardsData(_turn_out_card_data[i]);
					}
				}
				tableResponse_pdk.setPrCardsCount(_turn_out_card_count);
				tableResponse_pdk.setPrOutCardType(_turn_out_card_type);
				tableResponse_pdk.setPrOutCardPlayer(_turn_out__player);
				tableResponse_pdk.setIsFirstOut(2);
				tableResponse_pdk.setDisplayTime(getPlay_card_time());
				tableResponse_pdk.setMagicCard(GameConstants.INVALID_CARD);

				roomResponse.setCommResponse(PBUtil.toByteString(tableResponse_pdk));
				send_response_to_player(seat_index, roomResponse);
			}
			if (_is_in_qie) {
				RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
				QiePaiStart.Builder qie_pai = QiePaiStart.newBuilder();
				roomResponse.setType(MsgConstants.RESPONSE_PDK_QIE_START);// 201
				qie_pai.setQiepaiChair(_qie_pai_seat);
				qie_pai.setDisplayTime(6);
				roomResponse.setCommResponse(PBUtil.toByteString(qie_pai));
				this.send_response_to_room(roomResponse);
			}
		} else {
			if (_is_in_qie) {
				RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
				QiePaiResponse.Builder qie_resopnse = QiePaiResponse.newBuilder();
				roomResponse.setType(MsgConstants.RESPONSE_PDK_QIE_RESPONSE);// 201
				qie_resopnse.setChair(_qie_pai_seat);
				roomResponse.setCommResponse(PBUtil.toByteString(qie_resopnse));
				send_response_to_player(seat_index, roomResponse);
			}
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
		if (_release_scheduled == null) {
			int release_players[] = new int[getTablePlayerNumber()];
			Arrays.fill(release_players, 2);
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setReleaseTime(100);
			roomResponse.setType(MsgConstants.RESPONSE_PLAYER_RELEASE);
			roomResponse.setRequestPlayerSeat(seat_index);
			roomResponse.setOperateCode(1);
			roomResponse.setLeftTime(50);
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				roomResponse.addReleasePlayers(release_players[i]);
			}
			send_response_to_room(roomResponse);
		}
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_IS_TRUSTEE);
			roomResponse.setOperatePlayer(i);
			roomResponse.setIstrustee(istrustee[i]);
			this.send_response_to_player(seat_index, roomResponse);
		}
		if (this._cur_round > 0)
			return handler_player_ready(seat_index, false);
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

		return true;
	}

	/**
	 * @param get_seat_index
	 * @param out_cards[]
	 * @param card_count
	 * @param b_out_card
	 * @return
	 */
	@Override
	public boolean handler_operate_out_card_mul(int get_seat_index, List<Integer> list, int card_count, int b_out_card,
			String desc) {
		if (get_seat_index != this._current_player) {
			return false;
		}
		if (this._handler_out_card_operate != null) {
			this._handler = this._handler_out_card_operate;
			int out_cards[] = new int[card_count];
			for (int i = 0; i < card_count; i++) {
				out_cards[i] = list.get(i);
			}

			log_info("_out_cards_data:" + Arrays.toString(out_cards) + "_out_card_count:" + card_count);
			log_info("desc:" + desc);
			this._handler_out_card_operate.reset_status(get_seat_index, out_cards, card_count, b_out_card, desc);
			this._handler.exe(this);
		}

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

		return true;
	}

	@Override
	public boolean handler_requst_message_deal(Player player, int seat_index, RoomRequest room_rq, int type) {
		if (type == MsgConstants.REQUST_PDK_QIE_PAI) {
			Qiepai_Req req = PBUtil.toObject(room_rq, Qiepai_Req.class);
			if (req.getOpreateType() == 1) {
				if (_qie_pai_seat != seat_index) {
					return true;
				}
				if (this._game_status == GameConstants.GS_MJ_WAIT) {
					_is_in_qie = true;
				}
				RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
				QiePaiResponse.Builder qie_resopnse = QiePaiResponse.newBuilder();
				roomResponse.setType(MsgConstants.RESPONSE_PDK_QIE_RESPONSE);// 201
				qie_resopnse.setChair(seat_index);
				roomResponse.setCommResponse(PBUtil.toByteString(qie_resopnse));
				this.send_response_to_room(roomResponse);
				this.handler_player_ready(seat_index, false);
			} else if (req.getOpreateType() == 2) {
				if (!_is_in_qie) {
					return true;
				}
				if (_qie_pai_seat != seat_index) {
					return true;
				}
				if (this._game_status != GameConstants.GS_MJ_PLAY) {
					return true;
				}
				this.cancelShedule(ID_TIMER_QIE_PAI);
				RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
				QiePaiResult.Builder qie_result = QiePaiResult.newBuilder();
				roomResponse.setType(MsgConstants.RESPONSE_PDK_QIE_RESULT);// 201
				qie_result.setCardIndex(req.getCardIndex());
				qie_result.setQiepaiChair(seat_index);
				roomResponse.setCommResponse(PBUtil.toByteString(qie_result));
				this.send_response_to_room(roomResponse);
				schedule(ID_TIMER_QIE_PAI_TO_START, SheduleArgs.newArgs(), 3000);
			} else if (req.getOpreateType() == 3) {
				if (!_is_in_qie) {
					return true;
				}
				if (_qie_pai_seat != seat_index) {
					return true;
				}
				if (this._game_status != GameConstants.GS_MJ_PLAY) {
					return true;
				}
				this.cancelShedule(ID_TIMER_QIE_PAI);
				game_start_pkd();
			}
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
				if (_game_status == GameConstants.GS_MJ_WAIT) {
					this.handler_game_finish(GameConstants.INVALID_SEAT, GameConstants.Game_End_RELEASE_PLAY);
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
			if (_game_status == GameConstants.GS_MJ_WAIT) {
				this.handler_game_finish(GameConstants.INVALID_SEAT, GameConstants.Game_End_RELEASE_PLAY);
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
		roomResponse.setType(MsgConstants.RESPONSE_PLAYER_STATUS);// 29
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

	@Override
	public void auto_out_card(int seat_index, int out_type) {
		if (this._handler_out_card_operate != null && GRR != null) {
			this._handler_out_card_operate.reset_status(seat_index, GRR._cards_data[seat_index],
					GRR._card_count[seat_index], out_type, "");
			this._handler_out_card_operate.exe(this);
		}

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
	public boolean operate_out_card(int seat_index, int count, int cards_data[], int type, int to_player) {
		if (seat_index == GameConstants.INVALID_SEAT) {
			return false;
		}
		//// 发送玩家出牌 201 =玩家出牌(CMD_OutCard: operate_player,operate_card)
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		OutCardDataPdk_xy.Builder outcarddata = OutCardDataPdk_xy.newBuilder();
		Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_PDK_OUT_CARD);// 201
		roomResponse.setTarget(seat_index);

		for (int j = 0; j < count; j++) {
			outcarddata.addCardsData(cards_data[j]);
		}
		// 上一出牌数据
		outcarddata.setPrCardsCount(this._turn_out_card_count);
		for (int i = 0; i < this._turn_out_card_count; i++) {
			outcarddata.addPrCardsData(this._turn_out_card_data[i]);
			outcarddata.addPrChangeCardsData(this._turn_out_card_data[i]);
		}
		outcarddata.setCardsCount(count);
		outcarddata.setOutCardPlayer(seat_index);
		outcarddata.setCardType(type);
		outcarddata.setCurPlayer(this._current_player);
		outcarddata.setPrOutCardType(this._turn_out_card_type);
		outcarddata.setDisplayTime(getPlay_card_time());
		if (_turn_out_card_count == 0) {
			outcarddata.setIsFirstOut(true);
		} else {
			outcarddata.setIsFirstOut(false);
		}

		if (_current_player != GameConstants.INVALID_SEAT) {
			int can_out_card_data[] = new int[this.get_hand_card_count_max()];
			int can_out_card_count = _logic.Player_Can_out_card(GRR._cards_data[_current_player],
					GRR._card_count[_current_player], _turn_out_card_data, _turn_out_card_count, can_out_card_data);

			for (int i = 0; i < can_out_card_count; i++) {
				outcarddata.addUserCanOutData(can_out_card_data[i]);
			}
			outcarddata.setUserCanOutCount(can_out_card_count);
		}

		roomResponse.setCommResponse(PBUtil.toByteString(outcarddata));
		if (to_player == GameConstants.INVALID_SEAT) {
			GRR.add_room_response(roomResponse);
			return this.send_response_to_room(roomResponse);
		} else {
			return this.send_response_to_player(to_player, roomResponse);
		}

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
	public boolean record_effect_action(int seat_index, int effect_type, int effect_count, long effect_indexs[],
			int time) {
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
	public boolean operate_player_cards() {

		for (int play_index = 0; play_index < this.getTablePlayerNumber(); play_index++) {
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_PDK_REFRESH_PLAYER_CARDS);// 12更新玩家手牌数据
			roomResponse.setGameStatus(_game_status);
			roomResponse.setCardType(1);
			// 刷新玩家手牌数量
			RefreshCardsPdk_xy.Builder refreshcards = RefreshCardsPdk_xy.newBuilder();
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				if (this.has_rule(GameConstants.GAME_RULE_DISPLAY_CARD)) {
					refreshcards.addCardCount(GRR._card_count[i]);
				} else if (i == play_index) {
					refreshcards.addCardCount(GRR._card_count[i]);
				} else {
					refreshcards.addCardCount(-1);
				}

				Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GRR._card_count[i]; j++) {
					cards_card.addItem(GameConstants.INVALID_CARD);
				}
				refreshcards.addCardsData(i, cards_card);
			}

			// 手牌--将自己的手牌数据发给自己
			Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < GRR._card_count[play_index]; j++) {
				cards_card.addItem(GRR._cards_data[play_index][j]);
			}
			refreshcards.setCardsData(play_index, cards_card);
			roomResponse.setCommResponse(PBUtil.toByteString(refreshcards));

			// 自己才有牌数据
			this.send_response_to_player(play_index, roomResponse);
		}

		// 回放
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_PDK_REFRESH_PLAYER_CARDS);// 12更新玩家手牌数据
		roomResponse.setGameStatus(_game_status);
		roomResponse.setCardType(1);
		// 刷新玩家手牌数量
		RefreshCardsPdk.Builder refreshcards = RefreshCardsPdk.newBuilder();
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			refreshcards.addCardCount(GRR._card_count[i]);

			Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < GRR._card_count[i]; j++) {
				cards_card.addItem(GRR._cards_data[i][j]);
			}
			refreshcards.addCardsData(i, cards_card);
		}
		roomResponse.setCommResponse(PBUtil.toByteString(refreshcards));
		GRR.add_room_response(roomResponse);

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

	@Override
	public boolean handler_player_offline(Player player) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setGameStatus(_game_status);
		roomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);// 刷新玩家
		this.load_player_info_data(roomResponse);

		send_response_to_other(player.get_seat_index(), roomResponse);
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

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			tableResponse.addTrustee(false);// 是否托管
			// 剩余牌数
			tableResponse.addDiscardCount(GRR._discard_count[i]);
			Int32ArrayResponse.Builder int_array = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < 55; j++) {
				int_array.addItem(GRR._discard_cards[i][j]);
			}
			tableResponse.addDiscardCards(int_array);

		}

		roomResponse.setTable(tableResponse);

		this.send_response_to_player(seat_index, roomResponse);

		return true;

	}

	/**
	 * 加载基础状态 癞子 定鬼 牌型状态 玩家状态
	 * 
	 * @param roomResponse
	 */
	@Override
	public void load_common_status(RoomResponse.Builder roomResponse) {
		roomResponse.setCurrentPlayer(_current_player);
		roomResponse.setGameStatus(_game_status);

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			roomResponse.addCardStatus(_playerStatus[i]._card_status);
			roomResponse.addPlayerStatus(_playerStatus[i].get_status());
		}
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
		// this._handler_finish.exe(this);
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

	@Override
	public boolean runnable_gang_card_data(int seat_index, int provide_player, int center_card, int action, int type,
			boolean depatch, boolean self, boolean d) {

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

		return true;
	}

	@Override
	public boolean exe_chi_peng(int seat_index, int provider, int action, int card, int type, int lou_operate) {

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

	public static void main(String[] args) {
		int value = 0x00000200;
		int value1 = 0x200;

		System.out.println(value);
		System.out.println(value1);

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
		istrustee[get_seat_index] = isTrustee;

		this.send_response_to_room(roomResponse);
		if (GRR != null) {
			GRR.add_room_response(roomResponse);
		}

		return false;
	}

	@Override
	public boolean runnable_dispatch_last_card_data(int cur_player, int type, boolean tail) {

		return true;
	}

	@Override
	public boolean runnable_chuli_first_card_data(int _seat_index, int _type, boolean _tail) {

		return true;
	}

	@Override
	public boolean runnable_dispatch_first_card_data(int _seat_index, int _type, boolean _tail) {

		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.cai.common.domain.Room#getTablePlayerNumber()
	 */

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

}
