/**
 * 
 */
package com.cai.game.qjqf;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.math.RandomUtils;
import org.apache.log4j.Logger;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.QJQFConstants;
import com.cai.common.define.EMoneyOperateType;
import com.cai.common.domain.AddMoneyResultModel;
import com.cai.common.domain.GameRoundRecord;
import com.cai.common.domain.Player;
import com.cai.common.domain.PlayerResult;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.SysParamModel;
import com.cai.common.rmi.ICenterRMIServer;
import com.cai.common.util.FvMask;
import com.cai.common.util.PBUtil;
import com.cai.common.util.RandomUtil;
import com.cai.common.util.SpringService;
import com.cai.dictionary.SysParamDict;
import com.cai.domain.SheduleArgs;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.AddDiscardRunnable;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.future.runnable.JianPaoHuRunnable;
import com.cai.future.runnable.QJQFNotOutCardRunnable;
import com.cai.game.AbstractRoom;
import com.cai.game.RoomUtil;
import com.cai.game.qjqf.handler.QJQFHandler;
import com.cai.game.qjqf.handler.QJQFHandlerFinish;
import com.cai.game.qjqf.handler.QJQFHandlerOutCardOperate;
import com.cai.service.PlayerServiceImpl;
import com.google.common.collect.Lists;

import protobuf.clazz.Protocol;
import protobuf.clazz.Protocol.GameEndResponse;
import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.PlayerResultResponse;
import protobuf.clazz.Protocol.Response;
import protobuf.clazz.Protocol.Response.ResponseType;
import protobuf.clazz.Protocol.RoomInfo;
import protobuf.clazz.Protocol.RoomPlayerResponse;
import protobuf.clazz.Protocol.RoomRequest;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.qjqf.QJQFRsp.GameStart_QJQF;
import protobuf.clazz.qjqf.QJQFRsp.Opreate_Request_QJQF;
import protobuf.clazz.qjqf.QJQFRsp.OutCardDataQJQF;
import protobuf.clazz.qjqf.QJQFRsp.PukeGameEndQJQF;
import protobuf.clazz.qjqf.QJQFRsp.RefreshCardsQJQF;
import protobuf.clazz.qjqf.QJQFRsp.RoomInfoQJQF;

///////////////////////////////////////////////////////////////////////////////////////////////
public class QJQFTable extends AbstractRoom {
	/**
	 * 
	 */
	protected static final long serialVersionUID = 7060061356475703643L;

	protected static Logger logger = Logger.getLogger(QJQFTable.class);

	// 运行变量
	public int _current_player = GameConstants.INVALID_SEAT; // 当前用户
	public int _prev_palyer = GameConstants.INVALID_SEAT; // 上一操作用户
	public int _out_card_player = GameConstants.INVALID_SEAT; // 出牌用户
	public int _bao_pei_palyer = GameConstants.INVALID_SEAT;// 包赔玩家

	public int _turn_out__player = GameConstants.INVALID_SEAT;// 上一出牌玩家
	public int _turn_out_card_count; // 上一次出牌数目
	public int _turn_out_card_data[]; // 上一次 出牌扑克
	public int _turn_out_card_type; // 上一次 出牌牌型
	public int _turn_out_change_card_data[];// 上一变牌数据
	public int _score_type[];
	// 结算变量
	public int _game_score[];// 总得分
	public int _game_score_max[] = new int[this.getTablePlayerNumber()];// 得分最高
	public int _boom_num[] = new int[this.getTablePlayerNumber()];// 每局炸弹数
	public int _win_num[] = new int[this.getTablePlayerNumber()];// 赢的局数
	public int _lose_num[] = new int[this.getTablePlayerNumber()];// 输的局数
	public int _out_card_times[] = new int[this.getTablePlayerNumber()];// 出牌次数
	public int big_boom_index = -1;// 当局总炸弹数

	public QJQFGameLogic _logic = null;

	public static final int ID_TIMER_AOTU_OUT_CARD = 1;// 自动出牌
	public static final int ID_TIMER_TRUSTEE = 2;// 托管
	public static final int ID_TIMER_READY = 3;// 准备

	// 玩家私人定时器
	public int _ready_time[];
	/**
	 * 玩家游数
	 */
	public int[] player_you = new int[getTablePlayerNumber()];

	/**
	 * 当局分数
	 */
	public int[] round_score = new int[getTablePlayerNumber()];

	/**
	 * 
	 */
	public List<Integer> score_card = new ArrayList<>(4);

	/**
	 * 当前桌子内玩家数量
	 */
	protected int playerNumber;

	protected long _request_release_time;
	protected ScheduledFuture<Object> _release_scheduled;
	protected ScheduledFuture<Object> _table_scheduled;
	public ScheduledFuture<Object> _out_card_scheduled = null;

	public QJQFHandler _handler;

	public QJQFHandlerOutCardOperate _handler_out_card_operate;

	public QJQFHandlerFinish _handler_finish; // 结束

	public int out_index;

	public long outCardTime;

	protected static final int OPREATE_SORT = 1;// 排序

	public QJQFTable() {
		super(RoomType.HH, GameConstants.GAME_PLAYER);

		_logic = new QJQFGameLogic();

		// 玩家
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			get_players()[i] = null;
		}
		_game_status = GameConstants.GS_MJ_FREE;
		// 游戏变量
		_player_ready = new int[this.getTablePlayerNumber()];
		_game_score = new int[this.getTablePlayerNumber()];
		player_you = new int[this.getTablePlayerNumber()];
		round_score = new int[this.getTablePlayerNumber()];
		_score_type = new int[getTablePlayerNumber()];
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_player_ready[i] = 0;
			_game_score[i] = 0;
			_boom_num[i] = 0;
			_game_score_max[i] = 0;
			_win_num[i] = 0;
			_lose_num[i] = 0;
			_out_card_times[i] = 0;
			_score_type[i] = GameConstants.WSK_ST_ORDER;
		}

		_turn_out_card_data = new int[this.get_hand_card_count_max()];
		_turn_out_change_card_data = new int[this.get_hand_card_count_max()];

		_turn_out_card_count = 0;
		_turn_out_card_type = QJQFConstants.ERROR;
		for (int i = 0; i < this.get_hand_card_count_max(); i++) {
			_turn_out_card_data[i] = GameConstants.INVALID_CARD;
			_turn_out_change_card_data[i] = GameConstants.INVALID_CARD;
		}
	}

	@Override
	public void on_init_table(int game_type_index, int game_rule_index, int game_round) {
		_game_type_index = game_type_index;//
		_game_rule_index = game_rule_index;
		_game_round = game_round;
		_cur_round = 0;

		_player_result = new PlayerResult(this.getRoom_owner_account_id(), this.getRoom_id(), _game_type_index, _game_rule_index, _game_round,
				this.get_game_des());
		istrustee = new boolean[this.getTablePlayerNumber()];
		score_card.clear();
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_player_result.hu_pai_count[i] = 0;
			_player_result.ming_tang_count[i] = 0;
			_player_result.ying_xi_count[i] = 0;
			_game_score[i] = 0;

		}
		_handler_out_card_operate = new QJQFHandlerOutCardOperate();

		_handler_finish = new QJQFHandlerFinish();

		this.setMinPlayerCount(getTablePlayerNumber()); //

		_ready_time = new int[getTablePlayerNumber()];
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_ready_time[i] = 100 + i;
		}
	}

	@Override
	public boolean has_rule(int cbRule) {
		return FvMask.has_any(_game_rule_index, FvMask.mask(cbRule));
	}

	@Override
	public void runnable_set_trustee(int _seat_index) {
		// TODO Auto-generated method stub

	}

	////////////////////////////////////////////////////////////////////////
	@Override
	public boolean reset_init_data() {

		if (_cur_round == 0) {
			record_game_room();
		}

		_prev_palyer = GameConstants.INVALID_SEAT;
		_current_player = GameConstants.INVALID_SEAT;

		this._handler = null;

		GRR = new GameRoundRecord(GameConstants.MAX_WEAVE_HH, QJQFConstants.HAND_CARD, GameConstants.MAX_HH_INDEX);
		GRR.setRoom(this);
		GRR._start_time = System.currentTimeMillis() / 1000L;
		GRR._game_type_index = _game_type_index;
		GRR._cur_round = _cur_round;

		// 新建
		_playerStatus = new PlayerStatus[this.getTablePlayerNumber()];
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_playerStatus[i] = new PlayerStatus(GameConstants.MAX_HH_COUNT);
		}

		_cur_round++;

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_playerStatus[i].reset();
			round_score[i] = 0;
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
		this.cancelShedule(ID_TIMER_READY);
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			cancelShedule(_ready_time[i]);
		}

		_game_status = GameConstants.GS_MJ_PLAY;
		this.log_error("gme_status:" + this._game_status);

		reset_init_data();

		// 庄家选择
		this.progress_banker_select();

		GRR._banker_player = _cur_banker;
		_current_player = GRR._banker_player;

		_turn_out__player = GameConstants.INVALID_SEAT;
		_bao_pei_palyer = GameConstants.INVALID_SEAT;

		_turn_out_card_count = 0;
		for (int i = 0; i < this.get_hand_card_count_max(); i++) {
			_turn_out_card_data[i] = GameConstants.INVALID_CARD;
			_turn_out_change_card_data[i] = GameConstants.INVALID_CARD;
		}
		score_card.clear();
		_turn_out_card_type = QJQFConstants.ERROR;
		//
		_repertory_card = new int[QJQFConstants.CARD_DATA_SR_TWO.length];
		shuffle(_repertory_card, QJQFConstants.CARD_DATA_SR_TWO);

		// DEBUG_CARDS_MODE = false;
		// BACK_DEBUG_CARDS_MODE = true;
		if (DEBUG_CARDS_MODE || BACK_DEBUG_CARDS_MODE)
			test_cards();

		return game_start();
	}

	/**
	 * 
	 * @return
	 */
	private boolean game_start() {
		// 游戏开始
		this._game_status = GameConstants.GS_MJ_PLAY;// 设置状态
		this._handler = this._handler_out_card_operate;
		int FlashTime = 4000;
		int standTime = 1000;

		// 初始化游戏变量
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (player_you[i] == 1) {
				GRR._banker_player = i;
			}

			_boom_num[i] = 0;
			_out_card_times[i] = 0;
			player_you[i] = 0;
			round_score[i] = 0;
			if (this.DEBUG_CARDS_MODE) {
				this.istrustee[i] = true;
			}
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		if (this._cur_round == 1) {
			GRR._banker_player = this.find_hei_tao_san();
			this.load_player_info_data(roomResponse);
		}

		if (GRR._banker_player <= -1) {
			GRR._banker_player = 0;
		}

		big_boom_index = -1;

		this._current_player = GRR._banker_player;
		this.load_common_status(roomResponse);
		roomResponse.setType(QJQFConstants.RESPONSE_QJQF_GAME_START);
		roomResponse.setGameStatus(this._game_status);

		int gameId = this.getGame_id() == 0 ? 8 : this.getGame_id();
		SysParamModel sysParamModel1104 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(gameId).get(1104);
		cancelShedule(ID_TIMER_AOTU_OUT_CARD);
		if (has_rule(QJQFConstants.QJQF_TRUSTEE)) {
			// 托管时间
			if (this.istrustee[_current_player]) {
				List<Integer> cards = _logic.getOutCard(GRR._cards_data[_current_player], GRR._card_count[_current_player], this._turn_out_card_data,
						_turn_out_card_count, false);
				SheduleArgs args = SheduleArgs.newArgs();
				args.set("seat_index", _current_player);
				args.set("count", cards.size());
				for (int i = 0; i < cards.size(); i++) {
					args.set("card_value" + i, cards.get(i));
				}
				schedule(ID_TIMER_AOTU_OUT_CARD, args, 5000);
			} else {
				SheduleArgs args = SheduleArgs.newArgs();
				args.set("seat_index", _current_player);
				schedule(ID_TIMER_TRUSTEE, args, sysParamModel1104.getVal1());
			}

		}

		for (int play_index = 0; play_index < this.getTablePlayerNumber(); play_index++) {

			// 发送数据
			GameStart_QJQF.Builder gamestart_qjqf = GameStart_QJQF.newBuilder();

			gamestart_qjqf.setCurBanker(GRR._banker_player);
			System.out.println("=========qf" + GRR._banker_player);

			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				gamestart_qjqf.addCardCount(GRR._card_count[i]);
				Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
				// 回放用 看到所有的牌
				for (int j = 0; j < GRR._card_count[i]; j++) {
					cards_card.addItem(GameConstants.INVALID_CARD);
				}
				gamestart_qjqf.addCardsData(i, cards_card);
				gamestart_qjqf.addGameScore(this._game_score[i]);
			}

			// 手牌--将自己的手牌数据发给自己
			Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();

			for (int j = 0; j < GRR._card_count[play_index]; j++) {
				cards_card.addItem(GRR._cards_data[play_index][j]);
			}
			gamestart_qjqf.setRoomInfo(getRoomInfoPdk());
			gamestart_qjqf.setCardsData(play_index, cards_card);
			if (this.has_rule(QJQFConstants.QJQF_TRUSTEE)) {
				gamestart_qjqf.setDisplayTime(sysParamModel1104.getVal1() / 1000);
			} else {
				gamestart_qjqf.setDisplayTime(15);
			}

			roomResponse.setCommResponse(PBUtil.toByteString(gamestart_qjqf));

			// 自己才有牌数据
			this.send_response_to_player(play_index, roomResponse);
		}

		// 发送数据
		GameStart_QJQF.Builder gamestart_qjqf = GameStart_QJQF.newBuilder();

		gamestart_qjqf.setCurBanker(GRR._banker_player);

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			gamestart_qjqf.addCardCount(GRR._card_count[i]);
			Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
			// 回放用 看到所有的牌
			for (int j = 0; j < GRR._card_count[i]; j++) {
				cards_card.addItem(GRR._cards_data[i][j]);
			}
			gamestart_qjqf.addCardsData(i, cards_card);
			gamestart_qjqf.addGameScore(this._game_score[i]);
		}
		gamestart_qjqf.setDisplayTime(sysParamModel1104.getVal1() / 1000);
		roomResponse.setCommResponse(PBUtil.toByteString(gamestart_qjqf));
		GRR.add_room_response(roomResponse);

		PlayerStatus curPlayerStatus = this._playerStatus[this._current_player];
		curPlayerStatus.set_status(GameConstants.Player_Status_OUT_CARD);// 出牌状态
		this.operate_player_status();

		outCardTime = System.currentTimeMillis() + 19000;
		// 发牌时间4秒
		// _out_card_scheduled = GameSchedule.put(new
		// OutCardRunnable(getRoom_id(), this._current_player, ++out_index),
		// 19000, TimeUnit.MILLISECONDS);

		return true;
	}

	public int find_hei_tao_san() {
		// int seat = GameConstants.INVALID_SEAT;
		// for (int i = 0; i < this.getPlayerCount(); i++) {
		// for (int j = 0; j < GRR._card_count[i]; j++)
		// if (GRR._cards_data[i][j] == 0x33)
		// return i;
		// }

		return RandomUtils.nextInt(this.getPlayerCount());
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
			for (int j = 0; j < QJQFConstants.HAND_CARD; j++) {
				GRR._cards_data[i][j] = repertory_card[i * QJQFConstants.HAND_CARD + j];

			}
			GRR._card_count[i] = QJQFConstants.HAND_CARD;
			_logic.sort_card_date_list(GRR._cards_data[i], GRR._card_count[i], _score_type[i]);
		}
		GRR._left_card_count = 0;
		// test_cards();
		// 记录初始牌型

		// int[] card =
		// {0x05,0x05,0x05,0x05,0x05,0x05,0x05,0x05,0x0a,0x0a,0x0a,0x0a,0x0a,0x0a,0x0a,0x0a,0x0d,0x0d,0x0d,0x0d,0x0d,0x0d,0x0d,0x0d};
		//
		// for (int i = 0; i < count; i++) {
		// for (int j = 0; j < 6; j++) {
		// GRR._cards_data[i][j] = card[i * 6 + j];
		//
		// }
		// GRR._card_count[i] = 6;
		// _logic.sort_card_date_list(GRR._cards_data[i], GRR._card_count[i]);
		// }
		// GRR._left_card_count = 0;

		// if (SystemConfig.gameDebug == 1) {
		// for (int i = 0; i < count; i++) {
		// for (int j = 0; j < 8; j++) {
		// if(j < card.length){
		// GRR._cards_data[i][j] = card[j];
		// }
		//
		// }
		// GRR._card_count[i] = QJQFConstants.HAND_CARD;
		// _logic.sort_card_date_list(GRR._cards_data[i], GRR._card_count[i]);
		// }
		// }

		_recordRoomRecord.setBeginArray(Arrays.toString(repertory_card));

	}

	private void test_cards() {
		/********
		 * 下面这个给测试用的 如果开发用 把上面的注释去掉 realyCards
		 **************************/

		if (BACK_DEBUG_CARDS_MODE) {
			if (debug_my_cards != null) {
				if (debug_my_cards.length > QJQFConstants.HAND_CARD) {
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
	 * 测试线上 实际牌
	 */
	public void testRealyCard(int[] realyCards) {
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			for (int j = 0; j < this.get_hand_card_count_max(); j++) {
				GRR._cards_index[i][j] = 0;
			}
		}
		this._repertory_card = realyCards;

		_cur_banker = 0;
		// 分发扑克
		int k = 0;
		while (k < realyCards.length) {
			int i = 0;

			for (; i < this.getTablePlayerNumber(); i++) {
				for (int j = 0; j < this.get_hand_card_count_max(); j++) {
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
	public void testSameCard(int[] cards) {
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			for (int j = 0; j < GameConstants.MAX_HH_INDEX; j++) {
				GRR._cards_index[i][j] = 0;
			}
		}
		// 分发扑克
		int k = 0;
		int i = 0;
		for (; i < this.getTablePlayerNumber(); i++) {
			k = 0;
			for (int j = 0; j < cards.length; j++) {
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

		if (_cur_round == 1 && (reason == GameConstants.Game_End_NORMAL || reason == GameConstants.Game_End_DRAW)) {
			RoomUtil.realkou_dou(this);
		}

		if (_out_card_scheduled != null) {
			// _out_card_scheduled.cancel(false);
			_out_card_scheduled = null;
		}

		int real_reason = reason;

		int count = getTablePlayerNumber();
		if (count == 0) {
			count = this.getTablePlayerNumber();
		}
		SysParamModel sysParamModel1104 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(getGame_id()).get(1104);
		// 重制准备
		for (int i = 0; i < count; i++) {
			_player_ready[i] = 0;
		}

		// 结束后刷新玩家
		RoomResponse.Builder roomResponse2 = RoomResponse.newBuilder();
		roomResponse2.setGameStatus(_game_status);
		roomResponse2.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);// 刷新玩家
		this.load_player_info_data(roomResponse2);

		for (int i = 0; i < count; i++) {
			if (this._cur_round > 0) {
				this.send_response_to_player(i, roomResponse2);
			}
		}

		int end_score[] = new int[this.getTablePlayerNumber()];
		Arrays.fill(end_score, 0);
		// 计算分数
		if (reason == GameConstants.Game_End_NORMAL) {
			cal_score(end_score);
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(QJQFConstants.RESPONSE_QJQF_GAME_END);
		GameEndResponse.Builder game_end = GameEndResponse.newBuilder();

		PukeGameEndQJQF.Builder game_end_QJQF = PukeGameEndQJQF.newBuilder();
		this.load_common_status(roomResponse);
		this.load_room_info_data(roomResponse);

		RoomInfo.Builder room_info = getRoomInfo();
		roomResponse.setRoomInfo(room_info);

		this.load_player_info_data(roomResponse);

		game_end.setRoundOverType(1);
		game_end.setGameRound(_game_round);
		game_end.setCurRound(_cur_round);

		game_end.setGamePlayerNumber(getTablePlayerNumber());
		game_end.setCellScore(GameConstants.CELL_SCORE);
		game_end.setGameTypeIndex(_game_type_index);

		game_end_QJQF.setRoomInfo(getRoomInfoPdk());
		if (GRR != null) {
			game_end.setStartTime(GRR._start_time);

			game_end.setBankerPlayer(GRR._banker_player);// 庄家

			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				game_end_QJQF.addCardCount(GRR._card_count[i]);
				Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GRR._card_count[i]; j++) {
					cards_card.addItem(GRR._cards_data[i][j]);
				}
				game_end.addCardsData(i, cards_card);
			}
		}

		boolean end = false;
		if (reason == GameConstants.Game_End_NORMAL || reason == GameConstants.Game_End_DRAW) {
			if (_cur_round >= _game_round) {// 局数到了
				end = true;
				real_reason = GameConstants.Game_End_ROUND_OVER;
				for (int i = 0; i < this.getTablePlayerNumber(); i++) {
					game_end_QJQF.addEndScoreMax(this._game_score_max[i]);
				}
				game_end.setRoomOverType(1);
				game_end.setPlayerResult(this.process_player_result(reason));
			}
		} else if (reason == GameConstants.Game_End_RELEASE_PLAY || reason == GameConstants.Game_End_RELEASE_NO_BEGIN
				|| reason == GameConstants.Game_End_RELEASE_RESULT || reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT || reason == GameConstants.Game_End_RELEASE_SYSTEM) {
			end = true;
			if (reason == GameConstants.Game_End_RELEASE_NO_BEGIN)
				real_reason = reason;
			else
				real_reason = GameConstants.Game_End_RELEASE_PLAY;// 刘局
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				game_end_QJQF.addEndScoreMax(this._game_score_max[i]);
				// 没有正常结算。、当小局的分无效
				_game_score[i] -= round_score[i];
				round_score[i] = 0;
			}
			game_end.setRoomOverType(1);
			game_end.setPlayerResult(this.process_player_result(reason));
		}
		// 最高得分
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (_game_score[i] > _game_score_max[i]) {
				_game_score_max[i] = _game_score[i];
			}
		}

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			game_end_QJQF.addBoomCardNum(_boom_num[i]);
			game_end_QJQF.addEndScore(round_score[i]);
			game_end_QJQF.addAllEndScore(this._game_score[i]);

			game_end_QJQF.addWinNum(this._win_num[i]);
			game_end_QJQF.addLoseNum(this._lose_num[i]);
			game_end.addGameScore(this.round_score[i]);
		}
		game_end_QJQF.setDisplayTime(sysParamModel1104.getVal1() / 1000);
		game_end_QJQF.setBigBoomIndex(big_boom_index);
		if (big_boom_index != -1) {
			game_end_QJQF.setBigBoomScore(30);
		}

		game_end.setEndType(real_reason);
		game_end_QJQF.setReason(real_reason);
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			game_end_QJQF.addYou(player_you[i]);
		}
		game_end.setCommResponse(PBUtil.toByteString(game_end_QJQF));
		////////////////////////////////////////////////////////////////////// 得分总的
		// roomResponse_pdk.setGameEnd(game_end);
		roomResponse.setGameEnd(game_end);
		this.send_response_to_room(roomResponse);
		game_end.setRoomInfo(room_info);
		record_game_round(game_end, real_reason);

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			round_score[i] = 0;
		}

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

		if (has_rule(QJQFConstants.QJQF_TRUSTEE)) {

			SheduleArgs args = SheduleArgs.newArgs();
			schedule(ID_TIMER_READY, args, sysParamModel1104.getVal1());
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				if (this.istrustee[i]) {
					SheduleArgs args_ready = SheduleArgs.newArgs();
					schedule(_ready_time[i], args_ready, 5000);
				}
			}

		}

		return false;
	}

	public void cal_score(int end_score[]) {
		int you3 = -1;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (player_you[i] == 3) {
				you3 = i;
			}
		}
		// 最后一位玩家没出完 5 10 k 分牌。。分数都给3you
		if (you3 != -1) {
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				for (int j = 0; j < this.GRR._card_count[i]; j++) {

					if (this.GRR._cards_data[i][j] == 0x4e || this.GRR._cards_data[i][j] == 0x4f) {
						continue;
					}
					int value = QJQFGameLogic.get_card_value(this.GRR._cards_data[i][j]);
					if (value == 5) {
						round_score[you3] += 5;
						_game_score[you3] += 5;
					} else if (value == 10 || value == 0x0d) {
						round_score[you3] += 10;
						_game_score[you3] += 10;
					}
				}
			}
		}

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			// 从上游到下游依次扣20 40 60 80；
			if (player_you[i] == 0) {
				player_you[i] = 4;
			}
			round_score[i] -= player_you[i] * 20;
			end_score[i] = -player_you[i] * 20;
			if (round_score[i] <= 0) {
				this._lose_num[i]++;
			} else if (round_score[i] > 0) {
				this._win_num[i]++;
			}
		}

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_game_score[i] += end_score[i];
		}
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
		for (int i = 0; i < getTablePlayerNumber(); i++) {
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
		if ((GameConstants.GS_MJ_FREE != _game_status && GameConstants.GS_MJ_WAIT != _game_status) && this.get_players()[seat_index] != null) {
			// this.send_play_data(seat_index);

			this.log_error("gme_status:" + this._game_status + "GS_MJ_WAIT  seat_index:" + seat_index);
			if (this._handler != null) {
				this._handler.handler_player_be_in_room(this, seat_index);
			}
		}
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
				for (int i = 0; i < getTablePlayerNumber(); i++) {
					roomResponse.addReleasePlayers(_gameRoomRecord.release_players[i]);
				}
				this.send_response_to_player(seat_index, roomResponse);
			}
		}
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_IS_TRUSTEE);
		roomResponse.setOperatePlayer(seat_index);
		roomResponse.setIstrustee(istrustee[seat_index]);
		this.send_response_to_player(seat_index, roomResponse);
		// if (this._cur_round > 0)
		// return handler_player_ready(seat_index, false);
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
	public boolean handler_player_out_card(int seat_index, int index) {
		if ((_game_status == GameConstants.GS_MJ_FREE || _game_status == GameConstants.GS_MJ_WAIT))
			return false;
		if (this._handler_out_card_operate != null && seat_index == _current_player && out_index == index) {
			if (this._turn_out_card_count > 0) {
				List<Integer> cards = _logic.getOutCard(GRR._cards_data[seat_index], GRR._card_count[seat_index], _turn_out_card_data,
						_turn_out_card_count, false);
				if (cards.isEmpty()) {
					this.handler_operate_out_card_mul(seat_index, cards, 0, 0, "");
				} else {
					this.handler_operate_out_card_mul(seat_index, cards, cards.size(), 1, "");
				}

			} else {
				if (GRR._card_count[seat_index] > 0) {
					if (QJQFConstants.ERROR == _logic.getCardType(GRR._cards_data[seat_index], GRR._card_count[seat_index],
							GRR._cards_data[seat_index])) {
						this.handler_operate_out_card_mul(seat_index,
								Lists.newArrayList(GRR._cards_data[seat_index][GRR._card_count[seat_index] - 1]), 1, 1, "");
					} else {
						List<Integer> list = new ArrayList<>();
						for (int i = 0; i < GRR._card_count[seat_index]; i++) {
							list.add(GRR._cards_data[seat_index][i]);
						}
						this.handler_operate_out_card_mul(seat_index, list, GRR._card_count[seat_index], 1, "");
					}

				}
			}
		}
		return true;
	}

	public static void main(String[] args) {
	}

	/**
	 * @param get_seat_index
	 * @param out_cards[]
	 * @param card_count
	 * @param b_out_card
	 * @return
	 */
	@Override
	public boolean handler_operate_out_card_mul(int get_seat_index, List<Integer> list, int card_count, int b_out_card, String desc) {
		if ((_game_status == GameConstants.GS_MJ_FREE || _game_status == GameConstants.GS_MJ_WAIT))
			return false;
		if (this._handler_out_card_operate != null && get_seat_index == _current_player) {
			this._handler = this._handler_out_card_operate;
			int out_cards[] = new int[card_count];
			for (int i = 0; i < card_count; i++) {
				out_cards[i] = list.get(i);
			}

			this._handler_out_card_operate.reset_status(get_seat_index, out_cards, card_count, b_out_card);
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
				_release_scheduled = GameSchedule.put(
						new GameFinishRunnable(this.getRoom_id(), seat_index, GameConstants.Game_End_RELEASE_WAIT_TIME_OUT), delay, TimeUnit.SECONDS);
			} else {
				_release_scheduled = GameSchedule.put(
						new GameFinishRunnable(this.getRoom_id(), seat_index, GameConstants.Game_End_RELEASE_PLAY_TIME_OUT), delay, TimeUnit.SECONDS);
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
				if (player != null && this.get_players()[seat_index].getAccount_id() != getRoom_owner_account_id()) {
					send_error_notify(seat_index, 2, "不是房主不能解散");
					return false;
				}
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
	public boolean operate_out_card(int seat_index, int count, int cards_data[], int type, int to_player, boolean isNewTurn) {
		if (seat_index == GameConstants.INVALID_SEAT) {
			return false;
		}

		//// 发送玩家出牌 201 =玩家出牌(CMD_OutCard: operate_player,operate_card)
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		OutCardDataQJQF.Builder outcarddata = OutCardDataQJQF.newBuilder();
		roomResponse.setType(QJQFConstants.RESPONSE_QJQF_OUT_CARD);// 201
		roomResponse.setTarget(seat_index);
		for (int j = 0; j < count; j++) {
			outcarddata.addCardsData(cards_data[j]);
			int value = QJQFGameLogic.get_card_value(cards_data[j]);

			if (cards_data[j] != 0x4e && cards_data[j] != 0x4f && value % 5 == 0 || value == 0x0d) {
				this.score_card.add(cards_data[j]);
			}
		}
		// 上一出牌数据
		outcarddata.setPrCardsCount(this._turn_out_card_count);
		for (int i = 0; i < this._turn_out_card_count; i++) {
			outcarddata.addPrCardsData(this._turn_out_card_data[i]);
		}

		SysParamModel sysParamModel1104 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(getGame_id()).get(1104);
		outcarddata.setCardsCount(count);
		outcarddata.setOutCardPlayer(seat_index);
		outcarddata.setCardType(type);
		outcarddata.setCurPlayer(this._current_player);
		if (has_rule(QJQFConstants.QJQF_TRUSTEE)) {
			outcarddata.setDisplayTime(sysParamModel1104.getVal1() / 1000);
		} else {
			outcarddata.setDisplayTime(15);
		}

		outcarddata.setIsFirstOut(_turn_out_card_count == 0);
		outcarddata.setYou(this.player_you[seat_index]);
		outcarddata.setIsFirstOut(isNewTurn);

		if (isNewTurn) {
			int score = 0;
			for (int card : score_card) {
				int value = QJQFGameLogic.get_card_value(card);
				if (value == 5) {
					score += 5;
				} else {
					score += 10;
				}
			}
			// 计算本轮赢家的牌
			if (score > 0) {
				round_score[_out_card_player] += score;
				_game_score[_out_card_player] += score;
			}
			this.score_card.clear();
		}
		for (int i = 0; i < this.round_score.length; i++) {
			outcarddata.addPlayerScores(_game_score[i]);
			outcarddata.addRoundScores(round_score[i]);
		}

		outcarddata.addAllScoreCard(this.score_card);
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
			GRR.add_room_response(roomResponse);
			this.send_response_to_room(roomResponse);
		} else {
			this.send_response_to_player(to_player, roomResponse);
		}

		return true;
	}

	@Override
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
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(QJQFConstants.RESPONSE_QJQF_REFRESH_PLAYER_CARDS);// 12更新玩家手牌数据
		roomResponse.setGameStatus(_game_status);
		roomResponse.setCardType(1);

		for (int play_index = 0; play_index < this.getTablePlayerNumber(); play_index++) {
			// 刷新玩家手牌数量
			RefreshCardsQJQF.Builder refreshcards = RefreshCardsQJQF.newBuilder();
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				refreshcards.addCardCount(GRR._card_count[i]);

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

		// 刷新玩家手牌数量
		RefreshCardsQJQF.Builder refreshcards = RefreshCardsQJQF.newBuilder();

		for (int play_index = 0; play_index < this.getTablePlayerNumber(); play_index++) {

			// 手牌--将自己的手牌数据发给自己
			Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < GRR._card_count[play_index]; j++) {
				cards_card.addItem(GRR._cards_data[play_index][j]);
			}
			refreshcards.addCardsData(cards_card);
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

	private PlayerResultResponse.Builder process_player_result(int reason) {
		this.huan_dou(reason);
		// 大赢家
		int count = getTablePlayerNumber();

		for (int i = 0; i < count; i++) {
			_player_result.win_order[i] = -1;
		}
		int win_idx = 0;
		float max_score = 0;
		for (int i = 0; i < count; i++) {
			int winner = -1;
			_player_result.game_score[i] = this._game_score[i];
			float s = -999999;
			for (int j = 0; j < count; j++) {
				if (_player_result.win_order[j] != -1) {
					continue;
				}
				if (this._game_score[j] > s) {
					s = this._game_score[j];
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
			player_result.addGameScore(_game_score[i]);
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

		// for (int i = 0; i < getTablePlayerNumber(); i++) {
		// roomResponse.addCardStatus(_playerStatus[i]._card_status);
		// roomResponse.addPlayerStatus(_playerStatus[i].get_status());
		// }
	}

	/**
	 * @return
	 */
	public RoomInfoQJQF.Builder getRoomInfoPdk() {
		RoomInfoQJQF.Builder room_info = RoomInfoQJQF.newBuilder();
		room_info.setRoomId(this.getRoom_id());
		room_info.setGameRuleIndex(_game_rule_index);
		room_info.setGameRuleDes(get_game_des());
		room_info.setGameTypeIndex(_game_type_index);
		room_info.setGameRound(_game_round);
		room_info.setCurRound(_cur_round);
		room_info.setGameStatus(_game_status);
		room_info.setCreatePlayerId(this.getRoom_owner_account_id());
		room_info.setBankerPlayer(this._cur_banker);
		room_info.setCreateName(this.getRoom_owner_name());

		int beginLeftCard = GameConstants.CARD_COUNT_HH_YX - 60;
		room_info.setBeginLeftCard(beginLeftCard);
		return room_info;
	}

	/**
	 * 加载房间里的玩家信息
	 * 
	 * @param roomResponse
	 */

	public boolean send_response_to_special(int seat_index, int to_player, RoomResponse.Builder roomResponse) {

		roomResponse.setRoomId(super.getRoom_id());// 日志用的
		Player player = null;

		player = this.get_players()[to_player];
		if (player == null)
			return true;
		if (to_player == seat_index)
			return true;
		Response.Builder responseBuilder = Response.newBuilder();
		responseBuilder.setResponseType(ResponseType.ROOM);
		responseBuilder.setExtension(Protocol.roomResponse, roomResponse.build());

		PlayerServiceImpl.getInstance().send(this.get_players()[to_player], responseBuilder.build());

		return true;
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
		GameSchedule.put(new AddDiscardRunnable(getRoom_id(), seat_index, card_count, card_data, send_client, getMaxCount()), delay,
				TimeUnit.MILLISECONDS);

		return true;

	}

	@Override
	public boolean runnable_gang_card_data(int seat_index, int provide_player, int center_card, int action, int type, boolean depatch, boolean self,
			boolean d) {

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

		GameSchedule.put(new JianPaoHuRunnable(this.getRoom_id(), seat_index, action, card), GameConstants.DELAY_AUTO_OUT_CARD_TRUTESS,
				TimeUnit.MILLISECONDS);

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
		operate_out_card(seat_index, 0, null, type, GameConstants.INVALID_SEAT, false);
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
		if (!has_rule(QJQFConstants.QJQF_TRUSTEE)) {
			return false;
		}
		istrustee[get_seat_index] = isTrustee;
		if (get_seat_index == this._current_player) {
			if (istrustee[get_seat_index]) {
				List<Integer> cards = new ArrayList<>();
				int only_card_count_player = (_current_player + 1) % getTablePlayerNumber();
				for (int i = 0; i < this.getTablePlayerNumber(); i++) {
					if (GRR._card_count[only_card_count_player] == 0) {
						only_card_count_player = (only_card_count_player + 1) % getTablePlayerNumber();
					} else {
						break;
					}
				}
				if (GRR._card_count[only_card_count_player] == 1) {
					cards = _logic.getOutCard(GRR._cards_data[get_seat_index], GRR._card_count[get_seat_index], _turn_out_card_data,
							_turn_out_card_count, true);
				} else {
					cards = _logic.getOutCard(GRR._cards_data[get_seat_index], GRR._card_count[get_seat_index], _turn_out_card_data,
							_turn_out_card_count, false);
				}

				if (cards.isEmpty()) {
					GameSchedule.put(new QJQFNotOutCardRunnable(getRoom_id(), get_seat_index, this), 700, TimeUnit.MILLISECONDS);
				} else {
					if (istrustee[get_seat_index]) {
						SheduleArgs args_two = SheduleArgs.newArgs();
						args_two.set("seat_index", get_seat_index);
						args_two.set("count", cards.size());
						for (int i = 0; i < cards.size(); i++) {
							args_two.set("card_value" + i, cards.get(i));
						}
						schedule(ID_TIMER_AOTU_OUT_CARD, args_two, 1000);
					}

				}
			}

		}
		if (!istrustee[get_seat_index]) {

			if (this.hasShedule(this._ready_time[get_seat_index])) {
				this.cancelShedule(this._ready_time[get_seat_index]);
			}
		}

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
	public int getTablePlayerNumber() {
		return GameConstants.GAME_PLAYER;
	}

	public int get_hand_card_count_max() {
		return QJQFConstants.HAND_CARD;
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
		SysParamModel sysParamModel = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(getGame_id()).get(5006);
		int beilv = sysParamModel.getVal2().intValue();// 倍率
		float[] scores = _player_result.getGame_score();
		// GameConstants.GAME_PLAYER
		for (int i = 0; i < scores.length; i++) {
			score = (int) (scores[i] * beilv);

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

	@Override
	public boolean handler_requst_message_deal(Player player, int seat_index, RoomRequest room_rq, int type) {
		// if (type == QJQFConstants.RESPONSE_QJQF_OUT_CARD) {
		// handler_operate_out_card_mul(seat_index,
		// room_rq.getOutCardsList(),room_rq.getOutCardCount(),
		// room_rq.getBOutCardType());
		// }

		Opreate_Request_QJQF req = PBUtil.toObject(room_rq, Opreate_Request_QJQF.class);
		handler_requst_opreate(seat_index, req.getOpreateType());
		return true;
	}

	public void handler_requst_opreate(int seat_index, int opreate_type) {

		switch (opreate_type) {
		case OPREATE_SORT: {
			deal_sort(seat_index);
			break;
		}
		}
	}

	// 排序
	public void deal_sort(int seat_index) {
		if (this._game_status != GameConstants.GS_MJ_PLAY || this.GRR == null) {
			return;
		}
		if (this._score_type[seat_index] == GameConstants.WSK_ST_510K) {
			this._score_type[seat_index] = GameConstants.WSK_ST_ORDER;
		} else {
			this._score_type[seat_index] = GameConstants.WSK_ST_510K;
		}
		this._logic.sort_card_date_list(this.GRR._cards_data[seat_index], this.GRR._card_count[seat_index], this._score_type[seat_index]);
		operate_player_cards();
	}

	@Override
	public boolean trustee_timer(int operate_id, int seat_index) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	protected void timerCallBack(SheduleArgs args) {
		switch (args.getTimerId()) {
		case ID_TIMER_AOTU_OUT_CARD: {
			if (!has_rule(QJQFConstants.QJQF_TRUSTEE)) {
				return;
			}
			int seat_index = args.get("seat_index");
			int count = args.get("count");
			int card_data[] = new int[count];

			for (int i = 0; i < count; i++) {
				card_data[i] = args.get("card_value" + i);
			}

			this._handler_out_card_operate.reset_status(seat_index, card_data, count, 1);
			this._handler.exe(this);
			return;
		}
		case ID_TIMER_TRUSTEE: {
			if (!has_rule(QJQFConstants.QJQF_TRUSTEE)) {
				return;
			}
			int seat_index = args.get("seat_index");
			if (this._game_status == GameConstants.GS_MJ_PLAY) {
				if (seat_index != this._current_player) {
					return;
				}
			}
			handler_request_trustee(seat_index, true, 0);
			return;
		}
		case ID_TIMER_READY: {
			if (this._game_status != GameConstants.GS_MJ_WAIT) {
				return;
			}
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				if (this._player_ready[i] == 0) {
					handler_request_trustee(i, true, 0);
					this.handler_player_ready(i, false);
				}
			}
			return;
		}
		}
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (args.getTimerId() == this._ready_time[i]) {
				if (this._player_ready[i] == 0) {

					this.handler_player_ready(i, false);
					handler_request_trustee(i, true, 0);
				}
				return;
			}
		}
	}

}
