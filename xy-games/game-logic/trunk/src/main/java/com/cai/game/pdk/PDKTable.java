/**
 * 
 */
package com.cai.game.pdk;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.define.EMoneyOperateType;
import com.cai.common.domain.AddMoneyResultModel;
import com.cai.common.domain.GameRoundRecord;
import com.cai.common.domain.Player;
import com.cai.common.domain.PlayerResult;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.SysParamModel;
import com.cai.common.rmi.ICenterRMIServer;
import com.cai.common.util.PBUtil;
import com.cai.common.util.RandomUtil;
import com.cai.common.util.RoomComonUtil;
import com.cai.common.util.SpringService;
import com.cai.dictionary.SysParamDict;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.AddDiscardRunnable;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.future.runnable.JianPaoHuRunnable;
import com.cai.game.AbstractRoom;
import com.cai.game.RoomUtil;
import com.cai.game.pdk.handler.PDKHandler;
import com.cai.game.pdk.handler.PDKHandlerCallBanker;
import com.cai.game.pdk.handler.PDKHandlerFinish;
import com.cai.game.pdk.handler.PDKHandlerMingPai;
import com.cai.game.pdk.handler.PDKHandlerOutCardOperate;
import com.cai.game.pdk.handler.jdpdk.PDKHandlerOutCardOperate_JD;
import com.cai.service.PlayerServiceImpl;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.PlayerResultResponse;
import protobuf.clazz.Protocol.RoomPlayerResponse;
import protobuf.clazz.Protocol.RoomRequest;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.pdk.PdkRsp.RoomInfoPdk;
import protobuf.clazz.pdk.PdkRsp.RoomPlayerResponsePdk;
import protobuf.clazz.pdk_all.PdkRsp.GameStart_PDK_Error;
import protobuf.clazz.pdk_all.PdkRsp.Piao_Score_Begin;
import protobuf.clazz.pdk_all.PdkRsp.PukeGameEndPdk;
import protobuf.clazz.pdk_all.PdkRsp.TableResponse_PDK_Error;
import protobuf.clazz.pdk_xy.PdkRsp.GameStart_PDK_xy;
import protobuf.clazz.pdk_xy.PdkRsp.PukeGameEndPdk_xy;
import protobuf.clazz.pdk_xy.PdkRsp.TableResponse_PDK_xy;

//效验类型
class EstimatKind {
	public static int EstimatKind_OutCard = 1; // 出牌效验
	public static int EstimatKind_GangCard = 2; // 杠牌效验
}

///////////////////////////////////////////////////////////////////////////////////////////////
public class PDKTable extends AbstractRoom {
	/**
	 */
	protected static final long serialVersionUID = 7060061356475703643L;

	protected static Logger logger = Logger.getLogger(PDKTable.class);

	/**
	 * 这个值 不能在这里改--这个参数开发用 通过 命令行改牌
	 */
	public static boolean DEBUG_CARDS_MODE = false;

	// 运行变量
	public int _bomb_cell_score;
	public int _current_player = GameConstants.INVALID_SEAT; // 当前用户
	public int _prev_palyer = GameConstants.INVALID_SEAT; // 上一操作用户
	public int _out_card_player = GameConstants.INVALID_SEAT; // 出牌用户
	public int _bao_pei_palyer = GameConstants.INVALID_SEAT;// 包赔玩家
	public int _hong_tao_palyer = GameConstants.INVALID_SEAT;// 红桃玩家
	public int _fan_di_palyer = GameConstants.INVALID_SEAT;// 反的玩家
	public int _tao_pao_palyer = GameConstants.INVALID_SEAT;// 逃跑玩家
	public int _zha_niao_palyer = GameConstants.INVALID_SEAT;// 扎鸟玩家

	public int _turn_out__player = GameConstants.INVALID_SEAT;// 上一出牌玩家
	public int _turn_out_card_count; // 上一次出牌数目
	public int _turn_out_card_data[]; // 上一次 出牌扑克
	public int _turn_out_card_type; // 上一次 出牌牌型
	public int _turn_out_change_card_data[];// 上一变牌数据

	public int _init_hand_card[][];
	public int _init_hand_card_count[];
	// 结算变量
	public int _game_score[];// 总得分
	public int _game_score_max[] = new int[this.getTablePlayerNumber()];// 得分最高
	public int _all_boom_num[] = new int[this.getTablePlayerNumber()];// 总炸弹数
	public int _boom_num[] = new int[this.getTablePlayerNumber()];// 每局炸弹数
	public int _win_num[] = new int[this.getTablePlayerNumber()];// 赢的局数
	public int _lose_num[] = new int[this.getTablePlayerNumber()];// 输的局数
	public int _out_card_times[] = new int[this.getTablePlayerNumber()];// 出牌次数
	public int _opreate_times[] = new int[this.getTablePlayerNumber()];// 出牌次数
	public int _out_bomb_score[] = new int[getTablePlayerNumber()];
	public int _out_bomb_score_zhaniao[] = new int[getTablePlayerNumber()];
	public int _out_beichuntian[] = new int[getTablePlayerNumber()];// 被春天的玩家
	public int _out_beibaichun[] = new int[getTablePlayerNumber()];// 被摆春的玩家
	public boolean _enter_back[];
	public int _history_out_card[][][];
	public int _history_out_count[][];
	public int _piao_fen[];
	public int _piao_fen_select[];
	public int _game_end_score[][];// 总得分

	public int _call_action[];
	public int _ming_pai[];
	public boolean _is_call_banker;

	public boolean _is_in_qie;
	public int _first_out_card = 0x33;// 首出牌
	public int _qie_pai_seat;
	public PlayerStatus _playerStatus[];

	// 发牌信息

	public PDKGameLogic _logic = null;

	protected long _request_release_time;
	protected ScheduledFuture _release_scheduled;
	protected ScheduledFuture _table_scheduled;

	public ScheduledFuture _auto_out_card_scheduled;

	public ScheduledFuture _auto_yibao_scheduled;

	public PDKHandler _handler;

	public PDKHandlerOutCardOperate _handler_out_card_operate;
	public PDKHandlerCallBanker _handler_call_banker;
	public PDKHandlerMingPai _handler_ming_pai;

	// public HHHandlerDispatchLastCard_YX _handler_dispath_last_card;

	public PDKHandlerFinish _handler_finish; // 结束

	public PDKTable() {
		super(RoomType.HH, GameConstants.GAME_PLAYER);

		_logic = new PDKGameLogic();

		// 设置等待状态

		// 玩家
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			get_players()[i] = null;

		}
		_game_status = GameConstants.GS_MJ_FREE;
		// 游戏变量
		_player_ready = new int[this.getTablePlayerNumber()];
		_game_score = new int[this.getTablePlayerNumber()];
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_player_ready[i] = 0;
			_game_score[i] = 0;
			_all_boom_num[i] = 0;
			_boom_num[i] = 0;
			_game_score_max[i] = 0;
			_win_num[i] = 0;
			_lose_num[i] = 0;
			_out_card_times[i] = 0;
			_out_bomb_score[i] = 0;
			_out_bomb_score_zhaniao[i] = 0;
			_out_beichuntian[i] = GameConstants.INVALID_SEAT;
			_out_beibaichun[i] = GameConstants.INVALID_SEAT;
		}

		_turn_out_card_data = new int[this.get_hand_card_count_max()];
		_turn_out_change_card_data = new int[this.get_hand_card_count_max()];

		_turn_out_card_count = 0;
		_turn_out_card_type = GameConstants.PDK_CT_ERROR;
		for (int i = 0; i < this.get_hand_card_count_max(); i++) {
			_turn_out_card_data[i] = GameConstants.INVALID_CARD;
			_turn_out_change_card_data[i] = GameConstants.INVALID_CARD;
		}
	}

	public ScheduledFuture Get_Release_scheduled() {
		return _release_scheduled;
	}

	@Override
	public void on_init_table(int game_type_index, int game_rule_index, int game_round) {
		_game_type_index = game_type_index;//
		_game_rule_index = game_rule_index;
		_game_round = game_round;
		_cur_round = 0;
		_bomb_cell_score = 10;
		_player_result = new PlayerResult(this.getRoom_owner_account_id(), this.getRoom_id(), _game_type_index,
				_game_rule_index, _game_round, this.get_game_des());
		_playerStatus = new PlayerStatus[this.getTablePlayerNumber()];
		istrustee = new boolean[this.getTablePlayerNumber()];
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_playerStatus[i] = new PlayerStatus(this.get_hand_card_count_max());
		}
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_player_result.hu_pai_count[i] = 0;
			_player_result.ming_tang_count[i] = 0;
			_player_result.ying_xi_count[i] = 0;
			_game_score[i] = 0;
		}
		if (is_mj_type(GameConstants.GAME_TYPE_PDK_JD)) {
			// 初始化基础牌局handler

			_handler_out_card_operate = new PDKHandlerOutCardOperate_JD();

		}
		if (this.matchId != 0) {
			_bomb_cell_score = 4;
		}

		_handler_finish = new PDKHandlerFinish();

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
	@Override
	public boolean reset_init_data() {
		// 刷新玩家托管状态
		send_trustee_info();
		if (_cur_round == 0) {
			record_game_room();
		}

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_player_ready[i] = 0;
		}

		_prev_palyer = GameConstants.INVALID_SEAT;
		_current_player = GameConstants.INVALID_SEAT;

		this._handler = null;
		GRR = new GameRoundRecord(GameConstants.MAX_WEAVE_HH, get_hand_card_count_max(), GameConstants.MAX_HH_INDEX);
		GRR.setRoom(this);
		GRR._start_time = System.currentTimeMillis() / 1000L;
		GRR._game_type_index = _game_type_index;
		GRR._cur_round = _cur_round;

		// 新建
		_playerStatus = new PlayerStatus[this.getTablePlayerNumber()];
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_playerStatus[i] = new PlayerStatus(get_hand_card_count_max());
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
		if (gameRuleIndexEx != null) {
			int[] ruleEx = new int[gameRuleIndexEx.length];
			for (int i = 0; i < gameRuleIndexEx.length; i++) {
				ruleEx[i] = gameRuleIndexEx[i];
				GRR._room_info.addGameRuleIndexEx(ruleEx[i]);
			}

		}
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
		GRR._video_recode.setBankerPlayer(this._cur_banker);
		GRR._room_info = this.getRoomInfo();
		return true;
	}

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

		_turn_out__player = GameConstants.INVALID_SEAT;
		_bao_pei_palyer = GameConstants.INVALID_SEAT;
		_hong_tao_palyer = GameConstants.INVALID_SEAT;

		_turn_out_card_count = 0;
		for (int i = 0; i < this.get_hand_card_count_max(); i++) {
			_turn_out_card_data[i] = GameConstants.INVALID_CARD;
			_turn_out_change_card_data[i] = GameConstants.INVALID_CARD;
		}
		_turn_out_card_type = GameConstants.PDK_CT_ERROR;
		//
		if (is_mj_type(GameConstants.GAME_TYPE_PDK_JD)) {
			_repertory_card = new int[GameConstants.CARD_COUNT_PDK_JD];
			shuffle(_repertory_card, GameConstants.CARD_DATA_PDK_JD);
		}

		// DEBUG_CARDS_MODE = true;
		// BACK_DEBUG_CARDS_MODE = true;
		if (DEBUG_CARDS_MODE || BACK_DEBUG_CARDS_MODE)
			test_cards();

		if (is_mj_type(GameConstants.GAME_TYPE_PDK_JD)) {
			return game_start_pkd();
		}
		return false;
	}

	/**
	 * 开始 跑得快经典
	 * 
	 * @return
	 */
	private boolean game_start_pkd() {
		int playerCount = getPlayerCount();
		// 游戏开始
		this._game_status = GameConstants.GS_MJ_PLAY;// 设置状态

		int FlashTime = 4000;
		int standTime = 1000;

		// 初始化游戏变量
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_boom_num[i] = 0;
			_out_card_times[i] = 0;
		}

		for (int play_index = 0; play_index < this.getTablePlayerNumber(); play_index++) {

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			this.load_common_status(roomResponse);
			roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
			roomResponse.setGameStatus(this._game_status);
			// 发送数据
			GameStart_PDK_Error.Builder gamestart_pdk = GameStart_PDK_Error.newBuilder();
			RoomInfoPdk.Builder room_info = getRoomInfoPdk();
			gamestart_pdk.setRoomInfo(room_info);
			if (this._cur_round == 1) {
				// shuffle_players();
				GRR._banker_player = this.find_hei_tao_san();
				this.load_player_info_data(roomResponse);
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
				for (int j = 0; j < GRR._card_count[i]; j++) {
					cards_card.addItem(GameConstants.INVALID_CARD);
				}
				gamestart_pdk.addCardsData(i, cards_card);
			}

			// 手牌--将自己的手牌数据发给自己
			Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < GRR._card_count[play_index]; j++) {
				cards_card.addItem(GRR._cards_data[play_index][j]);
			}
			gamestart_pdk.setCardsData(play_index, cards_card);
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

		PlayerStatus curPlayerStatus = this._playerStatus[this._current_player];
		curPlayerStatus.set_status(GameConstants.Player_Status_OUT_CARD);// 出牌状态
		this.operate_player_status();

		return true;

	}

	public int find_hei_tao_san() {
		int seat = GameConstants.INVALID_SEAT;
		for (int i = 0; i < this.getPlayerCount(); i++) {
			for (int j = 0; j < GRR._card_count[i]; j++)
				if (GRR._cards_data[i][j] == 0x33)
					return i;
		}

		return seat;
	}

	/**
	 * 是否托管
	 * 
	 * @param seat_index
	 * @return
	 */
	public boolean isTrutess(int seat_index) {
		if (seat_index < 0 || seat_index > this.getTablePlayerNumber())
			return false;
		return istrustee[seat_index];
	}

	public int find_banker() {

		int seat = -1;
		if (this.has_rule(GameConstants.GAME_RULE_THREE_PLAY)) {
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				for (int j = 0; j < GRR._card_count[i]; j++)
					if (GRR._cards_data[i][j] == 0x33)
						return i;
			}
		}
		if (this.has_rule(GameConstants.GAME_RULE_TWO_PLAY)) {
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

	/// 洗牌
	private void shuffle(int repertory_card[], int mj_cards[]) {

		int xi_pai_count = 0;
		int rand = RandomUtil.generateRandomNumber(3, 6);

		while (xi_pai_count < 6 && xi_pai_count < rand) {
			if (xi_pai_count == 0)
				_logic.random_card_data(repertory_card, mj_cards);
			else
				_logic.random_card_data(repertory_card, repertory_card);

			xi_pai_count++;
		}

		int count = this.getPlayerCount();
		for (int i = 0; i < count; i++) {
			for (int j = 0; j < GameConstants.MAX_PDK_COUNT_JD; j++) {
				GRR._cards_data[i][j] = repertory_card[i * GameConstants.MAX_PDK_COUNT_JD + j];
				if (this.has_rule(GameConstants.GAME_RULE_HONGTAO10_ZANIAO) && GRR._cards_data[i][j] == 0x29) {
					_hong_tao_palyer = i;
				}
			}
			GRR._card_count[i] = GameConstants.MAX_PDK_COUNT_JD;
			_logic.sort_card_date_list(GRR._cards_data[i], GRR._card_count[i]);
		}
		GRR._left_card_count = 0;
		// test_cards();
		// 记录初始牌型
		_recordRoomRecord.setBeginArray(Arrays.toString(repertory_card));

	}

	private void test_cards() {
		/********
		 * 下面这个给测试用的 如果开发用 把上面的注释去掉 realyCards
		 **************************/

		if (BACK_DEBUG_CARDS_MODE) {
			if (debug_my_cards != null) {
				if (this.is_mj_type(GameConstants.GAME_PLAYER_FPHZ) == false) {
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
				} else {
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
				}
			}

		}
	}

	/**
	 * 测试线上 实际牌
	 */
	public void testRealyCard(int[] realyCards) {
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			for (int j = 0; j < GameConstants.MAX_HH_INDEX; j++) {
				GRR._cards_index[i][j] = 0;
			}
		}
		this._repertory_card = realyCards;

		int send_count;
		int have_send_count = 0;
		_cur_banker = 0;
		// 分发扑克
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			send_count = (GameConstants.MAX_HH_COUNT - 1);
			GRR._left_card_count -= send_count;

			have_send_count += send_count;
		}
		DEBUG_CARDS_MODE = false;// 把调试模式关闭
		BACK_DEBUG_CARDS_MODE = false;
		System.err.println("=========开始调试线上牌型 调试模式自动关闭*=========");

	}

	/**
	 * 模拟牌型--相同牌
	 */
	public void testSameCard(int[] cards) {
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			for (int j = 0; j < GameConstants.MAX_HH_INDEX; j++) {
				GRR._cards_index[i][j] = 0;
			}
		}

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			int send_count = 0;
			if (this.is_mj_type(GameConstants.GAME_PLAYER_FPHZ) == false)
				send_count = (GameConstants.MAX_HH_COUNT - 1);
			else
				send_count = GameConstants.MAX_FPHZ_COUNT - 1;

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
		if (is_mj_type(GameConstants.GAME_TYPE_PDK_JD)) {
			ret = this.handler_game_finish_pdk_jd(seat_index, reason);
		}

		return ret;
	}

	/**
	 * @return
	 */
	public RoomInfoPdk.Builder getRoomInfoPdk() {
		RoomInfoPdk.Builder room_info = RoomInfoPdk.newBuilder();

		room_info.setRoomId(this.getRoom_id());
		room_info.setGameRuleIndex(_game_rule_index);
		room_info.setGameRuleDes(get_game_des());
		room_info.setGameTypeIndex(_game_type_index);
		room_info.setGameRound(_game_round);
		room_info.setCurRound(_cur_round);
		room_info.setGameStatus(_game_status);
		room_info.setCreatePlayerId(this.getRoom_owner_account_id());
		room_info.setBankerPlayer(this._cur_banker);
		room_info.setMatchId(this.matchId);
		room_info.setCreateName(this.getRoom_owner_name());
		room_info.setClubId(club_id);
		if (clubInfo.clubId > 0) {
			room_info.setRuleId(clubInfo.ruleId);
		}
		room_info.setGameMaxPlayer(this.getTablePlayerNumber());
		if (commonGameRuleProtos != null) {
			room_info.setNewRules(commonGameRuleProtos);
		}

		if (gameRuleIndexEx != null) {
			int[] ruleEx = new int[gameRuleIndexEx.length];
			for (int i = 0; i < gameRuleIndexEx.length; i++) {
				ruleEx[i] = gameRuleIndexEx[i];
				room_info.addGameRuleIndexEx(ruleEx[i]);
			}

		}
		return room_info;
	}

	public void cal_score_pdk_jd(int end_score[]) {
		int win_player = GameConstants.INVALID_SEAT;
		int win_score = 0;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (this.GRR._card_count[i] == 0) {
				win_player = GameConstants.INVALID_SEAT;
				win_player = i;
			}
		}

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (i == win_player) {
				continue;
			}
			if (this.GRR._card_count[i] > 1) {
				if (i == this.GRR._banker_player) {
					// 反春天
					if (this._out_card_times[i] == 1 && this.has_rule(GameConstants.GAME_RULE_KE_FAN_DE)) {
						end_score[i] -= this.GRR._card_count[i] * 2;
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
				win_score += Math.abs(end_score[i]);
			}
			// 放走包赔
			if (this._bao_pei_palyer != GameConstants.INVALID_SEAT) {
				if (_bao_pei_palyer == i && end_score[i] < 0) {
					this._lose_num[i]++;
				}
			} else {
				if (end_score[i] < 0) {
					this._lose_num[i]++;
				}
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

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (end_score[i] > 0) {
				this._win_num[win_player]++;
			}
			_game_score[i] += end_score[i];
		}
	}

	public boolean handler_game_finish_pdk_jd(int seat_index, int reason) {

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
			godViewObservers().sendAll(roomResponse);
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
		int count = 3;
		if (is_mj_type(GameConstants.GAME_TYPE_FPHZ_YX))
			count = 4;
		else
			count = 3;
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
			// if (this._handler != null)
			// this._handler.handler_player_be_in_room(this, seat_index);

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
		if (this._cur_round > 0)
			return handler_player_ready(seat_index, false);
		return true;

	}

	public void auto_out_card(int seat_index, int out_type) {

	}

	public void auto_yin_bao(int seat_index, int out_type, int[] cards_data, int card_count) {

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
		if (this._handler_out_card_operate != null) {
			this._handler = this._handler_out_card_operate;
			int out_cards[] = new int[card_count];
			for (int i = 0; i < card_count; i++) {
				out_cards[i] = list.get(i);
			}

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
			this.huan_dou(GameConstants.Game_End_RELEASE_PLAY);
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
	public boolean operate_out_card(int seat_index, int count, int cards_data[], int type, int to_player) {
		return false;

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
	public boolean operate_player_cards() {
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

	@Override
	public boolean handler_player_offline(Player player) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setGameStatus(_game_status);
		roomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);// 刷新玩家
		this.load_player_info_data(roomResponse);

		send_response_to_other(player.get_seat_index(), roomResponse);
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

	public PlayerResultResponse.Builder process_player_result(int reason) {
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
			Player rplayer = this.get_players()[i];
			if (rplayer == null) {
				player_result.addGameScore(_player_result.game_score[i]);
			} else {
				player_result.addGameScore(_player_result.game_score[i] * rplayer.getMyTimes());
			}

			player_result.addWinOrder(_player_result.win_order[i]);

			Int32ArrayResponse.Builder lfs = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < count; j++) {
				lfs.addItem(_player_result.lost_fan_shu[i][j]);
			}

			player_result.addLostFanShu(lfs);

			player_result.addHuPaiCount(_player_result.hu_pai_count[i]);
			player_result.addMingTangCount(_player_result.ming_tang_count[i]);
			player_result.addYingXiCount(_player_result.ying_xi_count[i]);

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
	public void load_common_status(RoomResponse.Builder roomResponse) {
		roomResponse.setCurrentPlayer(_current_player);
		roomResponse.setGameStatus(_game_status);

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			roomResponse.addCardStatus(_playerStatus[i]._card_status);
			roomResponse.addPlayerStatus(_playerStatus[i].get_status());
		}
	}

	public void load_player_info_data_game_start(GameStart_PDK_Error.Builder roomResponse) {
		Player rplayer;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			rplayer = this.get_players()[i];
			if (rplayer == null)
				continue;
			RoomPlayerResponsePdk.Builder room_player = RoomPlayerResponsePdk.newBuilder();
			room_player.setAccountId(rplayer.getAccount_id());
			if (isFraud()) {
				room_player.setUserName("***");
				room_player.setHeadImgUrl("***");
			} else {
				room_player.setUserName(rplayer.getNick_name());
				room_player.setHeadImgUrl(rplayer.getAccount_icon());
			}

			room_player.setIp(rplayer.getAccount_ip());
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
			roomResponse.addPlayers(room_player);
		}
	}

	public void load_player_info_data_paifenbegin(Piao_Score_Begin.Builder roomResponse) {
		Player rplayer;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			rplayer = this.get_players()[i];
			if (rplayer == null)
				continue;
			RoomPlayerResponsePdk.Builder room_player = RoomPlayerResponsePdk.newBuilder();
			room_player.setAccountId(rplayer.getAccount_id());
			room_player.setIp(rplayer.getAccount_ip());
			if (isFraud()) {
				room_player.setUserName("***");
				room_player.setHeadImgUrl("***");
			} else {
				room_player.setUserName(rplayer.getNick_name());
				room_player.setHeadImgUrl(rplayer.getAccount_icon());
			}
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
			roomResponse.addPlayers(room_player);
		}
	}

	public void load_player_info_data_game_end(PukeGameEndPdk.Builder roomResponse) {
		Player rplayer;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			rplayer = this.get_players()[i];
			if (rplayer == null)
				continue;
			RoomPlayerResponsePdk.Builder room_player = RoomPlayerResponsePdk.newBuilder();
			room_player.setAccountId(rplayer.getAccount_id());
			room_player.setIp(rplayer.getAccount_ip());
			if (isFraud()) {
				room_player.setUserName("***");
				room_player.setHeadImgUrl("***");
			} else {
				room_player.setUserName(rplayer.getNick_name());
				room_player.setHeadImgUrl(rplayer.getAccount_icon());
			}
			room_player.setSeatIndex(rplayer.get_seat_index());
			room_player.setOnline(rplayer.isOnline() ? 1 : 0);
			room_player.setIpAddr(rplayer.getAccount_ip_addr());
			room_player.setSex(rplayer.getSex());
			room_player.setScore(_player_result.game_score[i] * rplayer.getMyTimes());
			room_player.setReady(_player_ready[i]);
			room_player.setMoney(rplayer.getMoney());
			room_player.setGold(rplayer.getGold());
			if (rplayer.locationInfor != null) {
				room_player.setLocationInfor(rplayer.locationInfor);
			}
			roomResponse.addPlayers(room_player);
		}
	}

	public void load_player_info_data_reconnect(TableResponse_PDK_Error.Builder roomResponse) {
		Player rplayer;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			rplayer = this.get_players()[i];
			if (rplayer == null)
				continue;
			RoomPlayerResponsePdk.Builder room_player = RoomPlayerResponsePdk.newBuilder();
			if (isFraud()) {
				room_player.setHeadImgUrl("***");
				room_player.setUserName("***");
			} else {
				room_player.setHeadImgUrl(rplayer.getAccount_icon());
				room_player.setUserName(rplayer.getNick_name());
			}
			room_player.setAccountId(rplayer.getAccount_id());
			room_player.setIp(rplayer.getAccount_ip());
			room_player.setSeatIndex(rplayer.get_seat_index());
			room_player.setOnline(rplayer.isOnline() ? 1 : 0);
			room_player.setIpAddr(rplayer.getAccount_ip_addr());
			room_player.setSex(rplayer.getSex());
			room_player.setScore(_player_result.game_score[i] * rplayer.getMyTimes());
			room_player.setReady(_player_ready[i]);
			room_player.setMoney(rplayer.getMoney());
			room_player.setGold(rplayer.getGold());
			if (rplayer.locationInfor != null) {
				room_player.setLocationInfor(rplayer.locationInfor);
			}
			roomResponse.addPlayers(room_player);
		}
	}

	public void load_player_info_data_game_start(GameStart_PDK_xy.Builder roomResponse) {
		Player rplayer;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			rplayer = this.get_players()[i];
			if (rplayer == null)
				continue;
			RoomPlayerResponsePdk.Builder room_player = RoomPlayerResponsePdk.newBuilder();
			room_player.setAccountId(rplayer.getAccount_id());
			room_player.setIp(rplayer.getAccount_ip());
			if (isFraud()) {
				room_player.setUserName("***");
				room_player.setHeadImgUrl("***");
			} else {
				room_player.setUserName(rplayer.getNick_name());
				room_player.setHeadImgUrl(rplayer.getAccount_icon());
			}
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
			roomResponse.addPlayers(room_player);
		}
	}

	public void load_player_info_data_game_end(PukeGameEndPdk_xy.Builder roomResponse) {
		Player rplayer;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			rplayer = this.get_players()[i];
			if (rplayer == null)
				continue;
			RoomPlayerResponsePdk.Builder room_player = RoomPlayerResponsePdk.newBuilder();
			room_player.setAccountId(rplayer.getAccount_id());
			room_player.setIp(rplayer.getAccount_ip());
			if (isFraud()) {
				room_player.setUserName("***");
				room_player.setHeadImgUrl("***");
			} else {
				room_player.setUserName(rplayer.getNick_name());
				room_player.setHeadImgUrl(rplayer.getAccount_icon());
			}
			room_player.setSeatIndex(rplayer.get_seat_index());
			room_player.setOnline(rplayer.isOnline() ? 1 : 0);
			room_player.setIpAddr(rplayer.getAccount_ip_addr());
			room_player.setSex(rplayer.getSex());
			room_player.setScore(_player_result.game_score[i] * rplayer.getMyTimes());
			room_player.setReady(_player_ready[i]);
			room_player.setMoney(rplayer.getMoney());
			room_player.setGold(rplayer.getGold());
			if (rplayer.locationInfor != null) {
				room_player.setLocationInfor(rplayer.locationInfor);
			}
			roomResponse.addPlayers(room_player);
		}
	}

	public void load_player_info_data_reconnect(TableResponse_PDK_xy.Builder roomResponse) {
		Player rplayer;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			rplayer = this.get_players()[i];
			if (rplayer == null)
				continue;
			RoomPlayerResponsePdk.Builder room_player = RoomPlayerResponsePdk.newBuilder();
			if (isFraud()) {
				room_player.setHeadImgUrl("***");
				room_player.setUserName("***");
			} else {
				room_player.setHeadImgUrl(rplayer.getAccount_icon());
				room_player.setUserName(rplayer.getNick_name());
			}
			room_player.setAccountId(rplayer.getAccount_id());
			room_player.setIp(rplayer.getAccount_ip());
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
			roomResponse.addPlayers(room_player);
		}
	}

	private void progress_banker_select() {
		if (_cur_banker == GameConstants.INVALID_SEAT) {
			_cur_banker = 0;// 创建者的玩家为专家
		}
	}

	public boolean is_mj_type(int type) {
		return _game_type_index == type;
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
	public boolean exe_finish() {

		this._handler = this._handler_finish;
		// this._handler_finish.exe(this);
		return true;
	}

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
	public boolean exe_out_card(int seat_index, int card, int type) {

		return true;
	}

	public boolean exe_chi_peng(int seat_index, int provider, int action, int card, int type, int lou_operate) {

		return true;
	}

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
	 * 显示中间出的牌
	 * 
	 * @param seat_index
	 */
	public void runnable_remove_middle_cards(int seat_index) {

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
		if (isTrustee) {
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
	public int getTablePlayerNumber() {
		return RoomComonUtil.getMaxNumber(getDescParams());
	}

	public int get_hand_card_count_max() {
		if (is_mj_type(GameConstants.GAME_TYPE_PDK_JD) || is_mj_type(GameConstants.GAME_TYPE_PDK_JD_LL)
				|| is_mj_type(GameConstants.GAME_TYPE_PDK_JD_YY) || is_mj_type(GameConstants.GAME_TYPE_PDK_JD_YUEY)
				|| is_mj_type(GameConstants.GAME_TYPE_PDK_JD_CZ) || is_mj_type(GameConstants.GAME_TYPE_PDK_JD_DT)
				|| is_mj_type(GameConstants.GAME_TYPE_PDK_KL) || is_mj_type(GameConstants.GAME_TYPE_PDK_JD_XY)
				|| is_mj_type(GameConstants.GAME_TYPE_PDK_JD_NP) || is_mj_type(GameConstants.GAME_TYPE_PDK_JD_LD)
				|| is_mj_type(GameConstants.GAME_TYPE_PDK_JD_DL)) {
			return GameConstants.MAX_PDK_COUNT_JD;
		} else if (is_mj_type(GameConstants.GAME_TYPE_PDK_SW) || is_mj_type(GameConstants.GAME_TYPE_PDK_SW_LL)
				|| is_mj_type(GameConstants.GAME_TYPE_PDK_SW_YY) || is_mj_type(GameConstants.GAME_TYPE_PDK_SW_YUEY)
				|| is_mj_type(GameConstants.GAME_TYPE_PDK_SW_CZ) || is_mj_type(GameConstants.GAME_TYPE_PDK_SW_DT)
				|| is_mj_type(GameConstants.GAME_TYPE_PDK_SW_KL) || is_mj_type(GameConstants.GAME_TYPE_PDK_SW_XY)
				|| is_mj_type(GameConstants.GAME_TYPE_PDK_SW_NP) || is_mj_type(GameConstants.GAME_TYPE_PDK_SW_LD)
				|| is_mj_type(GameConstants.GAME_TYPE_PDK_SW_DL) || is_mj_type(GameConstants.GAME_TYPE_PDK_SW_HN)) {
			return GameConstants.MAX_PDK_COUNT_SW;
		} else if (is_mj_type(GameConstants.GAME_TYPE_PDK_LZ)) {
			if (this.has_rule(GameConstants.GAME_RULE_FOUR_PLAY)) {
				return GameConstants.MAX_PDK_COUNT_SS;
			} else if (this.has_rule(GameConstants.GAME_RULE_FIFTEEN_COUNT)) {
				return GameConstants.MAX_PDK_COUNT_SW;
			} else {
				return GameConstants.MAX_PDK_COUNT_JD;
			}
		} else if (is_mj_type(GameConstants.GAME_TYPE_PDK_FP)) {
			if (this.has_rule(GameConstants.GAME_RULE_YIFU_COUNT)) {
				return GameConstants.MAX_PDK_COUNT_SS;
			} else {
				return GameConstants.MAX_PDK_COUNT_EQ;
			}
		} else if (is_mj_type(GameConstants.GAME_TYPE_PDK_ZN) || is_mj_type(GameConstants.GAME_TYPE_PDK_ZN_CZ)
				|| is_mj_type(GameConstants.GAME_TYPE_PDK_ZN_KL)) {
			return GameConstants.MAX_PDK_COUNT_JD;
		}
		return GameConstants.PDK_MAX_COUNT;
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
	 * @param call_banker
	 * @param qiang_banker
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
