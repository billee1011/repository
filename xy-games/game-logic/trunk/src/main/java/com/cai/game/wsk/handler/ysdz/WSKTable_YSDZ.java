package com.cai.game.wsk.handler.ysdz;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.define.ECardType;
import com.cai.common.domain.GameRoundRecord;
import com.cai.common.domain.Player;
import com.cai.common.domain.PlayerResult;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.util.PBUtil;
import com.cai.common.util.RandomUtil;
import com.cai.domain.SheduleArgs;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.AnimationRunnable;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.game.RoomUtil;
import com.cai.game.wsk.AbstractWSKTable;
import com.cai.game.wsk.WSKConstants;
import com.cai.game.wsk.WSKGameLogic_YSDZ;
import com.cai.game.wsk.WSKType;
import com.cai.game.wsk.data.tagAnalyseIndexResult_WSK;
import com.cai.service.MongoDBServiceImpl;
import com.cai.service.PlayerServiceImpl;

import protobuf.clazz.Protocol.GameEndResponse;
import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomPlayerResponse;
import protobuf.clazz.Protocol.RoomRequest;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.ysdz.ysdzRsp.GameStart_ysdz;
import protobuf.clazz.ysdz.ysdzRsp.Opreate_Request_ysdz;
import protobuf.clazz.ysdz.ysdzRsp.OutCardData_ysdz;
import protobuf.clazz.ysdz.ysdzRsp.PukeGameEnd_ysdz;
import protobuf.clazz.ysdz.ysdzRsp.RefreshCardData_ysdz;
import protobuf.clazz.ysdz.ysdzRsp.RefreshScore_ysdz;
import protobuf.clazz.ysdz.ysdzRsp.TableResponse_ysdz;
import protobuf.clazz.ysdz.ysdzRsp.UserCardData;
import protobuf.clazz.ysdz.ysdzRsp.effect_type_ysdz;

public class WSKTable_YSDZ extends AbstractWSKTable {

	private static final long serialVersionUID = -2419887548488809773L;

	public int _get_score[];
	public int _is_call_banker[];

	public int _turn_have_score; // 当局得分
	public int _score_type;
	public int _fei_wang_card[][];
	public int _fei_wang_count[];
	public int _lose_num[];
	public int _win_num[];
	public List<UserCardData.Builder> _user_data;

	protected static final int ID_TIMER_SEND_CARD = 1;// 发牌时间

	// 摊牌类型
	protected static final int TAB_PAI_TYPE_FA_WANG = 1;// 罚王摊牌
	protected static final int TAB_PAI_TYPE_WU_ZHA = 2;// 无炸摊牌
	// 特效 // 1：摊牌动画 2:伙伴动画 2:报警动画 3:奖励分动画
	protected static final int EFFECT_TAN_PAI = 1;// 摊牌
	protected static final int EFFECT_FRIEND = 2;// 伙伴
	protected static final int EFFECT_BAOJING = 3;// 报警
	protected static final int EFFECT_REWARD_SCORE = 4;// 奖励分
	protected static final int EFFECT_OUT_FINISH = 5;// 出完动画

	protected static final int GAME_OPREATE_TYPE_SORT_BY_CARD = 1;
	protected static final int GAME_OPREATE_TYPE_SORT_BY_COUNT = 2;
	private boolean is_touxiang;
	protected static final int TIME_OUT_NO_TOU_XIANG = 1;// 定时不投降
	public int sheng_dang_biaozhi; // 升档标志
	public boolean zi_dong_tou_xiang;// 是否自动投降
	public int sort[];// 排序标志
	public int fa_wang[];// 开局是否已经计算过罚王
	public int mian_da_lei_xing;// 免打类型
	public int prev_out_palyer;// 上一轮出牌人
	public int _wang_count[];
	public int[] player_sort_card;
	public WSKGameLogic_YSDZ _logic;

	public WSKTable_YSDZ() {
		super(WSKType.GAME_TYPE_WSK_GF);
	}

	@Override
	protected void onInitTable() {

	}

	@Override
	public void on_init_table(int game_type_index, int game_rule_index, int game_round) {
		_logic = new WSKGameLogic_YSDZ();
		_get_score = new int[getTablePlayerNumber()];
		_xi_qian_total_score = new int[getTablePlayerNumber()];
		_xi_qian_score = new int[getTablePlayerNumber()];
		_xi_qian_times = new int[getTablePlayerNumber()];
		_friend_seat = new int[getTablePlayerNumber()];
		_is_call_banker = new int[getTablePlayerNumber()];
		_tou_xiang_times = new int[getTablePlayerNumber()];
		_fei_wang_card = new int[getTablePlayerNumber()][this.get_hand_card_count_max()];
		sort = new int[getTablePlayerNumber()];
		_fei_wang_count = new int[getTablePlayerNumber()];
		_max_end_score = new int[getTablePlayerNumber()];
		_lose_num = new int[getTablePlayerNumber()];
		_win_num = new int[getTablePlayerNumber()];
		fa_wang = new int[getTablePlayerNumber()];
		_wang_count = new int[getTablePlayerNumber()];
		_game_type_index = game_type_index;//
		_game_rule_index = game_rule_index;
		_user_data = new ArrayList<>();
		_player_result = new PlayerResult(this.getRoom_owner_account_id(), this.getRoom_id(), _game_type_index,
				_game_rule_index, _game_round, this.get_game_des(), this.getTablePlayerNumber());
		_logic.ruleMap = this.ruleMap;
		_score_type = GameConstants.WSK_ST_ORDER;
		_handler_out_card_operate = new WSKHandlerOutCardOperate_YSDZ();
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_friend_seat[i] = -1;
			_max_end_score[i] = 0;
			_wang_count[i] = 0;
			_user_data.add(UserCardData.newBuilder());
		}

	}

	@Override
	public void Refresh_user_get_score(int to_player) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		RefreshScore_ysdz.Builder refresh_user_getscore = RefreshScore_ysdz.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_WSK_YSDZ_USER_GET_SCORE);
		Player rplayer;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			rplayer = this.get_players()[i];
			if (rplayer == null)
				continue;
			refresh_user_getscore.addUserGetScore(_get_score[i]);
			refresh_user_getscore.addXianQianScore(this._xi_qian_score[i]);
		}
		refresh_user_getscore.setTableScore(_turn_have_score);

		// refresh_user_getscore.setTableScore(value);
		roomResponse.setCommResponse(PBUtil.toByteString(refresh_user_getscore));

		if (to_player == GameConstants.INVALID_SEAT) {
			GRR.add_room_response(roomResponse);
			this.send_response_to_room(roomResponse);
		} else {
			this.send_response_to_player(to_player, roomResponse);
		}
	}

	// 游戏开始
	@Override
	protected boolean on_handler_game_start() {
		if (_cur_round == 2) {
			// real_kou_dou();// 记录真实扣豆
		}

		reset_init_data();

		// if(this.has_rule(GameConstants.GAME_RULE_DMZ_MODUI)){
		// shuffle_players();
		// this.operate_player_data();
		// }
		_handler_out_card_operate = new WSKHandlerOutCardOperate_YSDZ();
		this._turn_out_card_count = 0;
		this.mian_da_lei_xing = 0;
		Arrays.fill(_turn_out_card_data, GameConstants.INVALID_CARD);
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_cur_out_card_count[i] = 0;
			_is_tou_xiang_agree[i] = -1;
			_is_tou_xiang[i] = 0;
			_xi_qian_score[i] = 0;
			_is_call_banker[i] = 0;
			_tou_xiang_times[i] = 0;
			_xi_qian_times[i] = 0;
			_fei_wang_count[i] = 0;
			_friend_seat[i] = -1;
			fa_wang[i] = 0;
			_wang_count[i] = 0;
			Arrays.fill(_cur_out_card_data[i], GameConstants.INVALID_CARD);
			Arrays.fill(_fei_wang_card[i], GameConstants.INVALID_CARD);
			_user_data.get(i).clear();
			sort[i] = GameConstants.WSK_ST_ORDER;
		}
		prev_out_palyer = -1;
		_score_type = GameConstants.WSK_ST_ORDER;
		_game_status = GameConstants.GS_MJ_PLAY;
		_pai_score_count = 24;
		_pai_score = 200;
		_turn_have_score = 0;
		Arrays.fill(_chuwan_shunxu, GameConstants.INVALID_SEAT);
		_repertory_card = new int[GameConstants.CARD_COUNT_WSK];
		shuffle(_repertory_card, GameConstants.CARD_DATA_WSK);

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

		// 庄家选择
		this.progress_banker_select();

		GRR._banker_player = _cur_banker;
		on_game_start();

		return true;
	}

	/// 洗牌
	@Override
	public void shuffle(int repertory_card[], int card_cards[]) {
		int xi_pai_count = 0;
		int rand = RandomUtil.generateRandomNumber(3, 6);

		while (xi_pai_count < 6 && xi_pai_count < rand) {
			if (xi_pai_count == 0)
				_logic.random_card_data(repertory_card, card_cards);
			else
				_logic.random_card_data(repertory_card, repertory_card);

			xi_pai_count++;
		}

		int count = this.getTablePlayerNumber();
		int hei_tao_seat = GameConstants.INVALID_SEAT;
		for (int i = 0; i < count; i++) {
			for (int j = 0; j < this.get_hand_card_count_max(); j++) {
				GRR._cards_data[i][j] = repertory_card[i * this.get_hand_card_count_max() + j];
				if (GRR._cards_data[i][j] == 0x33 && hei_tao_seat == GameConstants.INVALID_SEAT) {
					hei_tao_seat = i;
				} else if (GRR._cards_data[i][j] == 0x33) {
					this._friend_seat[i] = hei_tao_seat;
					_friend_seat[hei_tao_seat] = i;
				}
			}
			GRR._card_count[i] = this.get_hand_card_count_max();
			_logic.SortCardList(GRR._cards_data[i], GRR._card_count[i], _score_type);
		}

		// 找朋友
		if (_friend_seat[hei_tao_seat] != hei_tao_seat) {
			int other_seat = GameConstants.INVALID_SEAT;
			for (int i = 0; i < count; i++) {
				if (i != hei_tao_seat && i != _friend_seat[hei_tao_seat] && other_seat == GameConstants.INVALID_SEAT) {
					other_seat = i;
				} else if (i != hei_tao_seat && i != _friend_seat[hei_tao_seat]) {
					this._friend_seat[i] = other_seat;
					_friend_seat[other_seat] = i;
				}
			}
		} else {
			for (int i = 0; i < count; i++) {
				_friend_seat[i] = (i + 2) % getTablePlayerNumber();
			}
		}

		// 记录初始牌型
		_recordRoomRecord.setBeginArray(Arrays.toString(repertory_card));
	}

	@Override
	protected boolean on_game_start() {

		cal_fa_wang();
		for (int play_index = 0; play_index < this.getTablePlayerNumber(); play_index++) {

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_WSK_YSDZ_GAME_START);
			roomResponse.setGameStatus(this._game_status);
			roomResponse.setRoomInfo(getRoomInfo());
			// 发送数据
			GameStart_ysdz.Builder gamestart = GameStart_ysdz.newBuilder();
			gamestart.setRoomInfo(this.getRoomInfo());
			gamestart.setCurBanker(this._cur_banker);
			this.load_player_info_data_game_start(gamestart);

			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				// 分析扑克
				if (this.GRR._card_count[i] <= 5) {
					gamestart.addCardCount(this.GRR._card_count[i]);
				} else {
					gamestart.addCardCount(-1);
				}
				Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
				Int32ArrayResponse.Builder wang_cards_card = Int32ArrayResponse.newBuilder();
				if (play_index == i) {
					for (int j = 0; j < GRR._card_count[i]; j++) {
						cards_card.addItem(GRR._cards_data[i][j]);
					}
				}

				for (int j = 0; j < _fei_wang_count[i]; j++) {
					wang_cards_card.addItem(_fei_wang_card[i][j]);
				}

				gamestart.addCardsData(cards_card);
				gamestart.addFeiWang(wang_cards_card);
			}
			gamestart.setDisplayTime(10);
			roomResponse.setCommResponse(PBUtil.toByteString(gamestart));

			// 自己才有牌数据
			this.send_response_to_player(play_index, roomResponse);
		}
		// 回放
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_WSK_YSDZ_GAME_START);
		roomResponse.setGameStatus(this._game_status);
		roomResponse.setRoomInfo(this.getRoomInfo());
		// 发送数据
		GameStart_ysdz.Builder gamestart = GameStart_ysdz.newBuilder();
		gamestart.setRoomInfo(this.getRoomInfo());
		this.load_player_info_data_game_start(gamestart);
		gamestart.setCurBanker(this._current_player);

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			gamestart.addCardCount(GRR._card_count[i]);
			Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
			Int32ArrayResponse.Builder wang_cards_card = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < GRR._card_count[i]; j++) {
				cards_card.addItem(GRR._cards_data[i][j]);
			}
			for (int j = 0; j < _fei_wang_count[i]; j++) {
				wang_cards_card.addItem(_fei_wang_card[i][j]);
			}

			gamestart.addCardsData(cards_card);
			gamestart.addFeiWang(wang_cards_card);
		}

		gamestart.setDisplayTime(10);
		roomResponse.setCommResponse(PBUtil.toByteString(gamestart));

		GRR.add_room_response(roomResponse);

		schedule(ID_TIMER_SEND_CARD, SheduleArgs.newArgs(), 1000);
		this.set_handler(this._handler_out_card_operate);
		Refresh_user_get_score(GameConstants.INVALID_SEAT);

		return true;
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

	@Override
	protected boolean on_handler_game_finish(int seat_index, int reason) {
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
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_WSK_YSDZ_GAME_END);
		GameEndResponse.Builder game_end = GameEndResponse.newBuilder();
		PukeGameEnd_ysdz.Builder game_end_wsk = PukeGameEnd_ysdz.newBuilder();
		game_end.setRoomInfo(getRoomInfo());
		game_end_wsk.setRoomInfo(getRoomInfo());

		int score_type = -1;
		int end_score[] = new int[this.getTablePlayerNumber()];
		int dang_ju_score[] = new int[this.getTablePlayerNumber()];
		int jia_fa_socre[] = new int[this.getTablePlayerNumber()];
		Arrays.fill(end_score, 0);
		Arrays.fill(dang_ju_score, 0);
		Arrays.fill(jia_fa_socre, 0);
		// 计算分数
		if (reason == GameConstants.Game_End_NORMAL) {
			if (this._chuwan_shunxu[0] == GameConstants.INVALID_SEAT) {
				score_type = mian_da_lei_xing;
				cal_score_tan_pai(end_score, dang_ju_score, seat_index, jia_fa_socre);
			} else {
				cal_fa_wang();
				if (this._friend_seat[this._chuwan_shunxu[0]] == this._chuwan_shunxu[1]) {
					score_type = 4;
				} else {
					score_type = 3;
				}
				cal_score_wsk(end_score, dang_ju_score, seat_index, jia_fa_socre);
			}

		} else if (GRR != null) {
			score_type = -1;
			cal_fa_wang();
			cal_fa_score(jia_fa_socre);
			cal_reward_score();
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				end_score[i] += this._xi_qian_score[i] + jia_fa_socre[i];
				this._player_result.game_score[i] += end_score[i];
			}
		}

		game_end.setGameRound(_game_round);
		game_end.setCurRound(_cur_round);
		if (GRR != null) {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				game_end_wsk.addCardCount(GRR._card_count[i]);
				Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GRR._card_count[i]; j++) {
					cards_card.addItem(GRR._cards_data[i][j]);
				}
				if (reason != GameConstants.Game_End_RELEASE_RESULT) {
					game_end_wsk.addHandCardData(cards_card.build());
				} else {
					Int32ArrayResponse.Builder cards_card1 = Int32ArrayResponse.newBuilder();
					game_end_wsk.addHandCardData(cards_card1.build());
				}
			}
			game_end.setStartTime(GRR._start_time);
			game_end.setGameTypeIndex(GRR._game_type_index);
			game_end_wsk.setBankerPlayer(GRR._banker_player);
			game_end_wsk.setOtherBanker(this._friend_seat[GRR._banker_player]);
			game_end_wsk.setScoreType(score_type);
		} else {
			game_end_wsk.setBankerPlayer(GameConstants.INVALID_SEAT);
			game_end_wsk.setOtherBanker(GameConstants.INVALID_SEAT);
		}
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_xi_qian_total_score[i] += _xi_qian_score[i];
			if (end_score[i] > _max_end_score[i]) {
				_max_end_score[i] = end_score[i];
			}
			if (end_score[i] > 0) {
				this._win_num[i]++;
			} else if (end_score[i] < 0) {
				this._lose_num[i]++;
			}
			Player player = this.get_players()[i];
			if (player != null) {
				player.setGame_score(end_score[i]);
				game_end.addGameScore(player.getGame_score());
			}
			game_end_wsk.addEndScore(end_score[i]);
			game_end_wsk.addRewardScore(this._xi_qian_score[i]);
			game_end_wsk.addDangJuScore(dang_ju_score[i]);
			game_end_wsk.addZhuaFen(this._get_score[i]);
			game_end_wsk.addJiaFaSocre(jia_fa_socre[i]);
			game_end_wsk.addCardsData(_user_data.get(i));
			game_end_wsk.addAllEndScore((int) this._player_result.game_score[i]);
			boolean is_out_finish = false;
			for (int j = 0; j < getTablePlayerNumber(); j++) {
				if (_chuwan_shunxu[j] == i) {
					game_end_wsk.addWinOrder(j);
					is_out_finish = true;
					break;
				}
			}
			if (!is_out_finish) {
				game_end_wsk.addWinOrder(GameConstants.INVALID_SEAT);
			}
		}

		this.load_player_info_data_game_end(game_end_wsk);
		boolean end = false;
		if (reason == GameConstants.Game_End_NORMAL || reason == GameConstants.Game_End_DRAW) {
			if (_cur_round >= _game_round) {// 局数到了
				end = true;
				for (int i = 0; i < getTablePlayerNumber(); i++) {
					game_end_wsk.addAllRewardScore(_xi_qian_total_score[i]);
					game_end_wsk.addEndScoreMax(_max_end_score[i]);
					game_end_wsk.addLoseNum(this._lose_num[i]);
					game_end_wsk.addWinNum(this._win_num[i]);
				}
				game_end.setPlayerResult(this.process_player_result(reason));
				real_reason = GameConstants.Game_End_ROUND_OVER;
			}
		} else if (reason == GameConstants.Game_End_RELEASE_PLAY || reason == GameConstants.Game_End_RELEASE_NO_BEGIN
				|| reason == GameConstants.Game_End_RELEASE_RESULT
				|| reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_SYSTEM) {
			end = true;

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				game_end_wsk.addAllRewardScore(_xi_qian_total_score[i]);
				game_end_wsk.addEndScoreMax(_max_end_score[i]);
				game_end_wsk.addLoseNum(this._lose_num[i]);
				game_end_wsk.addWinNum(this._win_num[i]);
			}

			game_end.setPlayerResult(this.process_player_result(reason));
			real_reason = GameConstants.Game_End_RELEASE_PLAY;
		}
		game_end_wsk.setReason(real_reason);
		////////////////////////////////////////////////////////////////////// 得分总的
		game_end.setEndType(real_reason);
		game_end.setGameRound(_game_round);
		game_end.setCurRound(_cur_round);
		game_end.setCommResponse(PBUtil.toByteString(game_end_wsk));
		game_end.setRoundOverType(1);
		game_end.setEndTime(System.currentTimeMillis() / 1000L);// 结束时间

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

		if (!is_sys()) {
			GRR = null;
		}
		if (end)// 删除
		{
			PlayerServiceImpl.getInstance().delRoomId(this.getRoom_id());
		}

		Arrays.fill(_xi_qian_score, 0);
		Arrays.fill(_get_score, 0);
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_user_data.get(i).clear();
			_chuwan_shunxu[i] = GameConstants.INVALID_SEAT;
		}
		// 错误断言
		return false;
	}

	/**
	 * 
	 * @param end_score
	 * @param dang_ju_fen
	 * @param win_seat_index
	 * @param jia_fa_socre
	 */
	public void cal_score_tan_pai(int end_score[], int dang_ju_fen[], int win_seat_index, int jia_fa_socre[]) {
		cal_fa_score(jia_fa_socre);
		cal_reward_score();
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			end_score[i] += dang_ju_fen[i] + this._xi_qian_score[i] + jia_fa_socre[i];
			this._player_result.game_score[i] += end_score[i];
		}
	}

	public void cal_score_wsk(int end_score[], int dang_ju_fen[], int win_seat_index, int jia_fa_socre[]) {
		cal_fa_score(jia_fa_socre);
		if (this._friend_seat[this._chuwan_shunxu[0]] == this._chuwan_shunxu[1]) {
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				if (i != this._chuwan_shunxu[0] && i != this._chuwan_shunxu[1]) {
					dang_ju_fen[i] -= 4;
				} else {
					dang_ju_fen[i] += 4;
				}
			}
		} else {
			cal_reward_score();
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				if (i != this._chuwan_shunxu[0] && i != _friend_seat[_chuwan_shunxu[0]]) {
					dang_ju_fen[i] -= 2;
				} else {
					dang_ju_fen[i] += 2;
				}
			}
		}
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			end_score[i] += dang_ju_fen[i] + this._xi_qian_score[i] + jia_fa_socre[i];
			this._player_result.game_score[i] += end_score[i];
		}
	}

	public void cal_fa_wang() {
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			int wang_count = this._logic.Get_Wang_Count(GRR._cards_data[i], GRR._card_count[i]);
			_wang_count[i] = wang_count;
			if (wang_count != 0 && wang_count != 4
					&& !this._logic.have_card_num(GRR._cards_data[i], GRR._card_count[i], 4) && fa_wang[i] == 0) {
				for (int j = 0; j < GRR._card_count[i]; j++) {
					if (GRR._cards_data[i][j] == 0x4E || GRR._cards_data[i][j] == 0x4F
							|| GRR._cards_data[i][j] == 0x4E + 0x100 || GRR._cards_data[i][j] == 0x4F + 0x100) {
						fa_wang[i] = 1;
						_fei_wang_card[i][_fei_wang_count[i]++] = GRR._cards_data[i][j];
					}
				}
			}
		}
	}

	public void cal_fa_score(int jia_fa_socre[]) {
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (_fei_wang_count[i] > 0) {
				jia_fa_socre[i] -= _fei_wang_count[i] * 3;
				for (int j = 0; j < this.getTablePlayerNumber(); j++) {
					if (i == j) {
						continue;
					}
					jia_fa_socre[j] += _fei_wang_count[i];
				}
			}
		}
	}

	public void cal_reward_score() {
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			int xi_qian_score = _logic.GetHandCardXianScore(GRR._cards_data[i], GRR._card_count[i], sheng_dang_biaozhi,
					_user_data.get(i));
			for (int j = 0; j < getTablePlayerNumber(); j++) {
				if (i == j) {
					continue;
				}
				_xi_qian_score[i] += xi_qian_score;
				_xi_qian_score[j] -= xi_qian_score;
			}
		}
	}

	public void cal_score_mian_da(int end_score[], int dang_ju_fen[], int jia_fa_socre[]) {
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (_fei_wang_count[i] > 0) {
				jia_fa_socre[i] -= _fei_wang_count[i] * 3;
				for (int j = 0; j < this.getTablePlayerNumber(); j++) {
					if (i == j) {
						continue;
					}
					jia_fa_socre[j] += _fei_wang_count[i];
				}
			}
			int score = _logic.GetHandCardXianScore(GRR._cards_data[i], GRR._card_count[i], sheng_dang_biaozhi,
					_user_data.get(i));
			for (int j = 0; j < getTablePlayerNumber(); j++) {
				if (i == j) {
					continue;
				}
				_xi_qian_score[i] += score;
				_xi_qian_score[j] -= score;
			}
		}

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			end_score[i] += dang_ju_fen[i] + this._xi_qian_score[i] + jia_fa_socre[i];
			this._player_result.game_score[i] += end_score[i];

		}
	}

	public void load_player_info_data_game_start(GameStart_ysdz.Builder roomResponse) {
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
			room_player.setScore(get_players()[i].getGame_score());
			room_player.setReady(_player_ready[i]);
			room_player.setPao(_player_result.pao[i]);
			room_player.setNao(_player_result.nao[i]);
			room_player.setQiang(_player_result.qiang[i]);
			room_player.setMoney(rplayer.getMoney());
			room_player.setGold(rplayer.getGold());
			room_player.setHasPiao(_player_result.haspiao[i]);
			room_player.setBiaoyan(_player_result.biaoyan[i]);
			if (rplayer.locationInfor != null) {
				room_player.setLocationInfor(rplayer.locationInfor);
			}
			roomResponse.addPlayers(room_player);
		}
	}

	public void load_player_info_data_reconnect(TableResponse_ysdz.Builder roomResponse) {
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
			room_player.setScore(get_players()[i].getGame_score());
			room_player.setReady(_player_ready[i]);
			room_player.setPao(_player_result.pao[i]);
			room_player.setNao(_player_result.nao[i]);
			room_player.setQiang(_player_result.qiang[i]);
			room_player.setMoney(rplayer.getMoney());
			room_player.setGold(rplayer.getGold());
			room_player.setHasPiao(_player_result.haspiao[i]);
			room_player.setBiaoyan(_player_result.biaoyan[i]);
			if (rplayer.locationInfor != null) {
				room_player.setLocationInfor(rplayer.locationInfor);
			}
			roomResponse.addPlayers(room_player);
		}
	}

	public void load_player_info_data_game_end(PukeGameEnd_ysdz.Builder roomResponse) {
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
			room_player.setScore(get_players()[i].getGame_score());
			room_player.setReady(_player_ready[i]);
			room_player.setPao(_player_result.pao[i]);
			room_player.setNao(_player_result.nao[i]);
			room_player.setQiang(_player_result.qiang[i]);
			room_player.setMoney(rplayer.getMoney());
			room_player.setGold(rplayer.getGold());
			room_player.setHasPiao(_player_result.haspiao[i]);
			room_player.setBiaoyan(_player_result.biaoyan[i]);
			if (rplayer.locationInfor != null) {
				room_player.setLocationInfor(rplayer.locationInfor);
			}
			roomResponse.addPlayers(room_player);
		}
	}

	@Override
	public boolean operate_out_card(int seat_index, int count, int cards_data[], int type, int to_player,
			boolean is_deal) {
		// if (seat_index == GameConstants.INVALID_SEAT) {
		// return false;
		// }
		//// 发送玩家出牌 201 =玩家出牌(CMD_OutCard: operate_player,operate_card)
		if (to_player == GameConstants.INVALID_SEAT) {
			for (int index = 0; index < this.getTablePlayerNumber(); index++) {
				RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
				OutCardData_ysdz.Builder outcarddata = OutCardData_ysdz.newBuilder();
				// Int32ArrayResponse.Builder cards =
				// Int32ArrayResponse.newBuilder();
				roomResponse.setType(MsgConstants.RESPONSE_WSK_YSDZ_OUT_CARD);// 201
				roomResponse.setTarget(seat_index);

				for (int j = 0; j < count; j++) {
					outcarddata.addCardsData(cards_data[j] % 0x100);
				}
				// 上一出牌数据
				outcarddata.setPrCardsCount(this._turn_out_card_count);
				for (int i = 0; i < this._turn_out_card_count; i++) {
					outcarddata.addPrCardsData(this._turn_out_card_data[i]);
				}
				outcarddata.setCardsCount(count);
				outcarddata.setOutCardPlayer(seat_index);
				outcarddata.setCardType(type);
				outcarddata.setCurPlayer(this._current_player);
				outcarddata.setDisplayTime(10);
				outcarddata.setPrOutCardType(_turn_out_card_type);
				outcarddata.setThreeLinkCount(this._turn_three_link_num);

				if (_is_shou_chu == 1) {
					outcarddata.setIsFirstOut(true);
				} else {
					outcarddata.setIsFirstOut(false);
				}
				if (_turn_out_card_count == 0) {
					outcarddata.setIsCurrentFirstOut(1);
				} else {
					outcarddata.setIsCurrentFirstOut(0);
				}
				if (is_deal) {
					outcarddata.setIsHaveNotCard(1);
				} else {
					outcarddata.setIsHaveNotCard(0);
				}

				// 出票提示
				if (index == this._current_player) {
					int tip_out_card[][] = new int[GRR._card_count[index] * 2][GRR._card_count[index]];
					int tip_out_count[] = new int[GRR._card_count[index] * 2];
					int tip_type_count = 0;
					tip_type_count = this._logic.search_out_card(GRR._cards_data[index], GRR._card_count[index],
							this._turn_out_card_data, this._turn_out_card_count, this._turn_three_link_num,
							tip_out_card, tip_out_count, tip_type_count);
					for (int i = 0; i < tip_type_count; i++) {
						Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
						for (int j = 0; j < tip_out_count[i]; j++) {
							cards_card.addItem(tip_out_card[i][j]);
						}
						outcarddata.addUserCanOutData(cards_card);
						outcarddata.addUserCanOutCount(tip_out_count[i]);
					}
				}

				for (int i = 0; i < this.getTablePlayerNumber(); i++) {
					Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
					if (i == index) {
						for (int j = 0; j < this.GRR._card_count[i]; j++) {
							cards_card.addItem(this.GRR._cards_data[i][j]);
						}
					}

					outcarddata.addHandCardsData(cards_card);
					if (this.GRR._card_count[i] <= 5) {
						outcarddata.addHandCardCount(this.GRR._card_count[i]);
					} else {
						outcarddata.addHandCardCount(-1);
					}

					outcarddata.addWinOrder(this._chuwan_shunxu[i]);
				}
				roomResponse.setCommResponse(PBUtil.toByteString(outcarddata));

				this.send_response_to_player(index, roomResponse);

			}

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			OutCardData_ysdz.Builder outcarddata = OutCardData_ysdz.newBuilder();
			// Int32ArrayResponse.Builder cards =
			// Int32ArrayResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_WSK_YSDZ_OUT_CARD);// 201
			roomResponse.setTarget(seat_index);

			for (int j = 0; j < count; j++) {
				outcarddata.addCardsData(cards_data[j]);
			}
			// 上一出牌数据
			outcarddata.setPrCardsCount(this._turn_out_card_count);
			for (int i = 0; i < this._turn_out_card_count; i++) {
				outcarddata.addPrCardsData(this._turn_out_card_data[i]);
			}
			outcarddata.setCardsCount(count);
			outcarddata.setOutCardPlayer(seat_index);
			outcarddata.setCardType(type);
			outcarddata.setCurPlayer(this._current_player);
			outcarddata.setDisplayTime(10);
			outcarddata.setPrOutCardType(_turn_out_card_type);

			if (_is_shou_chu == 1) {
				outcarddata.setIsFirstOut(true);
			} else {
				outcarddata.setIsFirstOut(false);
			}
			if (_turn_out_card_count == 0) {
				outcarddata.setIsCurrentFirstOut(1);

			} else {
				outcarddata.setIsCurrentFirstOut(0);
			}
			if (_current_player != GameConstants.INVALID_SEAT) {
				if (this.GRR._card_count[_current_player] == 0) {
					outcarddata.setIsHaveNotCard(1);
				} else {
					outcarddata.setIsHaveNotCard(0);
				}
			} else {
				outcarddata.setIsHaveNotCard(0);
			}

			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < this.GRR._card_count[i]; j++) {
					cards_card.addItem(this.GRR._cards_data[i][j]);
				}
				outcarddata.addHandCardsData(cards_card);
				outcarddata.addHandCardCount(this.GRR._card_count[i]);
				outcarddata.addWinOrder(this._chuwan_shunxu[i]);
			}

			roomResponse.setCommResponse(PBUtil.toByteString(outcarddata));
			GRR.add_room_response(roomResponse);
		} else {
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			OutCardData_ysdz.Builder outcarddata = OutCardData_ysdz.newBuilder();
			// Int32ArrayResponse.Builder cards =
			// Int32ArrayResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_WSK_YSDZ_OUT_CARD);// 201
			roomResponse.setTarget(seat_index);

			for (int j = 0; j < count; j++) {
				outcarddata.addCardsData(cards_data[j]);
			}
			// 上一出牌数据
			outcarddata.setPrCardsCount(this._turn_out_card_count);
			for (int i = 0; i < this._turn_out_card_count; i++) {
				outcarddata.addPrCardsData(this._turn_out_card_data[i]);
			}
			outcarddata.setCardsCount(count);
			outcarddata.setOutCardPlayer(seat_index);
			outcarddata.setCardType(type);
			outcarddata.setCurPlayer(this._current_player);
			outcarddata.setDisplayTime(10);
			outcarddata.setPrOutCardType(_turn_out_card_type);

			if (_is_shou_chu == 1) {
				outcarddata.setIsFirstOut(true);
			} else {
				outcarddata.setIsFirstOut(false);
			}
			if (_turn_out_card_count == 0) {
				outcarddata.setIsCurrentFirstOut(1);
			} else {
				outcarddata.setIsCurrentFirstOut(0);
			}
			if (is_deal) {
				outcarddata.setIsHaveNotCard(1);
			} else {
				outcarddata.setIsHaveNotCard(0);
			}

			// 出票提示
			if (to_player == this._current_player) {
				int tip_out_card[][] = new int[GRR._card_count[to_player] * 2][GRR._card_count[to_player]];
				int tip_out_count[] = new int[GRR._card_count[to_player] * 2];
				int tip_type_count = 0;
				tip_type_count = this._logic.search_out_card(GRR._cards_data[to_player], GRR._card_count[to_player],
						this._turn_out_card_data, this._turn_out_card_count, this._turn_three_link_num, tip_out_card,
						tip_out_count, tip_type_count);
				for (int i = 0; i < tip_type_count; i++) {
					Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
					for (int j = 0; j < tip_out_count[i]; j++) {
						cards_card.addItem(tip_out_card[i][j]);
					}
					outcarddata.addUserCanOutData(cards_card);
					outcarddata.addUserCanOutCount(tip_out_count[i]);
				}
			}

			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
				if (i == to_player) {
					for (int j = 0; j < this.GRR._card_count[i]; j++) {
						cards_card.addItem(this.GRR._cards_data[i][j]);
					}
				}

				outcarddata.addHandCardsData(cards_card);
				if (this.GRR._card_count[i] <= 5) {
					outcarddata.addHandCardCount(this.GRR._card_count[i]);
				} else {
					outcarddata.addHandCardCount(-1);
				}
				outcarddata.addWinOrder(this._chuwan_shunxu[i]);
			}
			roomResponse.setCommResponse(PBUtil.toByteString(outcarddata));

			this.send_response_to_player(to_player, roomResponse);

		}

		return true;
	}

	@Override
	public boolean handler_requst_message_deal(Player player, int seat_index, RoomRequest room_rq, int type) {
		if (type == MsgConstants.REQUST_YSDZ_OPERATE) {
			Opreate_Request_ysdz req = PBUtil.toObject(room_rq, Opreate_Request_ysdz.class);
			// 逻辑处理
			return handler_requst_opreate(seat_index, req.getOpreateType(), req.getSortCardList());
		}
		return true;
	}

	public boolean handler_requst_opreate(int seat_index, int opreate_type, List<Integer> list) {
		switch (opreate_type) {
		case GAME_OPREATE_TYPE_SORT_BY_CARD: {
			deal_sort_card_by_data(seat_index, list);
			return true;
		}
		case GAME_OPREATE_TYPE_SORT_BY_COUNT: {
			deal_sort_card_by_count(seat_index);
			return true;
		}
		}
		return true;
	}

	public void out_card_sort_card_by_data(int seat_index, int out_card_data[], int out_card_count) {
		int out_cards[] = new int[out_card_count];
		int temp_count = 0;
		int cut_color = 0;
		for (int i = 0; i < out_card_count; i++) {
			if (out_card_data[i] > 0x100) {
				out_cards[temp_count++] = out_card_data[i];
			}
		}
		if (temp_count <= 0) {
			return;
		}
		int color_add = out_cards[0] / 0x100;
		if (color_add > 0) {
			cut_color++;
		}
		for (int i = 1; i < temp_count; i++) {
			if (color_add != out_cards[i] / 0x100) {
				color_add = out_cards[i] / 0x100;
				cut_color++;
			}
		}
		for (int i = 0; i < temp_count; i++) {
			if (out_cards[i] > 0x100) {
				for (int j = 0; j < this.GRR._card_count[seat_index]; j++) {

					int temp = GRR._cards_data[seat_index][j];
					GRR._cards_data[seat_index][j] = temp & (~(out_cards[i] / 0x100 * 0x100));
				}

			}
		}

		int temp_color = 0;
		for (int i = 0; i < temp_count; i++) {
			if ((out_cards[i] / 0x100 < temp_color || temp_color == 0) && out_cards[i] > 0x100) {
				temp_color = out_cards[i] / 0x100;
				for (int j = 0; j < this.GRR._card_count[seat_index]; j++) {
					int temp = GRR._cards_data[seat_index][j];
					if (temp > (out_cards[i] / 0x100 * 0x100) && temp > 0x100) {
						GRR._cards_data[seat_index][j] = (temp % 0x100) + ((temp / 0x100 * 0x100) >> 1);
					}

				}
			}
		}

		player_sort_card[seat_index] -= cut_color;

		// int _score_type = sort[seat_index];
		// if (_score_type == GameConstants.WSK_ST_COUNT) {
		// _score_type = GameConstants.WSK_ST_ORDER;
		// } else if (_score_type == GameConstants.WSK_ST_ORDER || _score_type
		// == GameConstants.WSK_ST_CUSTOM) {
		// _score_type = GameConstants.WSK_ST_COUNT;
		// }
		// sort[seat_index] = _score_type;
		_score_type = GameConstants.WSK_ST_CUSTOM;
		this._logic.SortCardList(GRR._cards_data[seat_index], GRR._card_count[seat_index], this.sort[seat_index]);
		RefreshCard(seat_index);
	}

	public void deal_sort_card_by_data(int seat_index, List<Integer> list) {
		if (list.size() == 0) {
			return;
		}
		int out_cards[] = new int[list.size()];
		int cut_color = 0;
		boolean same_color = true;
		for (int i = 0; i < list.size(); i++) {
			out_cards[i] = list.get(i);
		}
		int color_add = out_cards[0] / 0x100;
		if (color_add > 0) {
			cut_color++;
		}
		for (int i = 1; i < list.size(); i++) {
			if (color_add != out_cards[i] / 0x100) {
				same_color = false;
				color_add = out_cards[i] / 0x100;
				if (out_cards[i] > 0x100) {
					cut_color++;
				}
			}
		}
		for (int i = 0; i < list.size(); i++) {
			if (out_cards[i] > 0x100) {
				for (int j = 0; j < this.GRR._card_count[seat_index]; j++) {

					int temp = GRR._cards_data[seat_index][j];
					GRR._cards_data[seat_index][j] = temp & (~(out_cards[i] / 0x100 * 0x100));
					temp = GRR._cards_data[seat_index][j];
				}

			}
		}

		int temp_color = 0;
		for (int i = 0; i < list.size(); i++) {
			if ((out_cards[i] / 0x100 > temp_color || temp_color == 0) && out_cards[i] > 0x100) {
				temp_color = out_cards[i] / 0x100;
				for (int j = 0; j < this.GRR._card_count[seat_index]; j++) {
					int temp = GRR._cards_data[seat_index][j];
					if (temp > (out_cards[i] / 0x100 * 0x100) && temp > 0x100) {
						GRR._cards_data[seat_index][j] = (temp % 0x100) + ((temp / 0x100 * 0x100) >> 1);
					}
					temp = GRR._cards_data[seat_index][j];
				}
			}
			if (!same_color) {
				out_cards[i] %= 0x100;
			}
		}
		player_sort_card[seat_index] -= cut_color;
		if (!same_color || cut_color == 0) {
			int flag = 0x100 << (player_sort_card[seat_index]++);

			for (int i = 0; i < list.size(); i++) {
				for (int j = 0; j < this.GRR._card_count[seat_index]; j++) {
					if (GRR._cards_data[seat_index][j] == out_cards[i]) {
						GRR._cards_data[seat_index][j] |= flag;
						break;
					}
				}
			}
		}

		this._logic.SortCardList(GRR._cards_data[seat_index], GRR._card_count[seat_index], this.sort[seat_index]);
		RefreshCard(seat_index);
	}

	public void deal_sort_card_by_count(int seat_index) {
		int _score_type = sort[seat_index];
		if (_score_type == GameConstants.WSK_ST_COUNT) {
			_score_type = GameConstants.WSK_ST_ORDER;
		} else if (_score_type == GameConstants.WSK_ST_ORDER || _score_type == GameConstants.WSK_ST_CUSTOM) {
			_score_type = GameConstants.WSK_ST_COUNT;
		}
		sort[seat_index] = _score_type;
		this._logic.SortCardList(GRR._cards_data[seat_index], GRR._card_count[seat_index], _score_type);
		RefreshCard(seat_index);
	}

	public void RefreshCard(int to_player) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_WSK_GF_REFRESH_CARD);
		// 发送数据
		RefreshCardData_ysdz.Builder refresh_card = RefreshCardData_ysdz.newBuilder();
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
			if (to_player == i) {
				for (int j = 0; j < this.GRR._card_count[i]; j++) {
					cards_card.addItem(this.GRR._cards_data[i][j]);
				}
			}
			refresh_card.addHandCardsData(cards_card);
			refresh_card.addHandCardCount(GRR._card_count[i]);
		}
		roomResponse.setCommResponse(PBUtil.toByteString(refresh_card));
		// 自己才有牌数据
		this.send_response_to_player(to_player, roomResponse);
	}

	@Override
	public int getTablePlayerNumber() {
		return 4;
	}

	@Override
	protected void set_result_describe() {
		// TODO Auto-generated method stub

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

			this._handler_out_card_operate.reset_status(get_seat_index, out_cards, card_count, b_out_card);
			this._handler.exe(this);
		}
		return true;
	}

	// 发送特效
	public void send_effect_type(int seat_index, int type, int data[], int is_animation, int to_player) {
		// 1：摊牌动画 2:伙伴动画 2:报警动画 3:奖励分动画
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_WSK_YSDZ_EFFECT);
		effect_type_ysdz.Builder effect = effect_type_ysdz.newBuilder();
		effect.setSeatIndex(seat_index);
		effect.setType(type);
		if (data != null) {
			for (int i = 0; i < data.length; i++) {
				effect.addData(data[i]);
			}
		}

		effect.setIsAnimation(is_animation);
		roomResponse.setCommResponse(PBUtil.toByteString(effect));
		if (to_player == GameConstants.INVALID_SEAT) {
			this.send_response_to_room(roomResponse);
			GRR.add_room_response(roomResponse);
		} else {
			this.send_response_to_player(to_player, roomResponse);
		}

	}

	/**
	 * 调度回调
	 */
	@Override
	protected void timerCallBack(SheduleArgs args) {
		switch (args.getTimerId()) {
		case ID_TIMER_SEND_CARD: {
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				this.send_effect_type(this._friend_seat[GRR._banker_player], EFFECT_FRIEND, null, 1, i);

			}

			if (!check_tan_pai()) {
				_current_player = this._cur_banker;
				this.operate_out_card(GameConstants.INVALID_SEAT, 0, null, WSKConstants.WSK_YSDZ_CT_ERROR,
						GameConstants.INVALID_SEAT, false);
			} else {
				this.send_effect_type(GameConstants.INVALID_SEAT, EFFECT_TAN_PAI, null, 1, GameConstants.INVALID_SEAT);
			}
			return;
		}
		}
	}

	@Override
	public boolean trustee_timer(int operate_id, int seat_index) {
		// TODO Auto-generated method stub
		return false;
	}

	// 摊牌检测
	public boolean check_tan_pai() {
		if (has_rule(WSKConstants.GAME_RULE_YSDZ_FA_WANG_BU_DA)) {
			boolean is_fa_wang = false;
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (_fei_wang_count[i] > 0) {
					_current_player = GameConstants.INVALID_SEAT;
					is_fa_wang = true;
					break;
				}
			}
			if (is_fa_wang) {
				mian_da_lei_xing = TAB_PAI_TYPE_FA_WANG;
				GameSchedule.put(
						new GameFinishRunnable(getRoom_id(), GRR._banker_player, GameConstants.Game_End_NORMAL), 3,
						TimeUnit.SECONDS);
				return true;
			}
		}
		// 9炸摊牌
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			int wang_count = this._logic.Get_Wang_Count(GRR._cards_data[i], GRR._card_count[i]);
			if (wang_count == 4) {
				GameSchedule.put(
						new GameFinishRunnable(getRoom_id(), GRR._banker_player, GameConstants.Game_End_NORMAL), 3,
						TimeUnit.SECONDS);
				return true;
			}
			if (has_rule(WSKConstants.GAME_RULE_YSDZ_YING_EIGHT_ZHA)
					&& this._logic.have_card_num(GRR._cards_data[i], GRR._card_count[i], 8)) {
				GameSchedule.put(
						new GameFinishRunnable(getRoom_id(), GRR._banker_player, GameConstants.Game_End_NORMAL), 3,
						TimeUnit.SECONDS);
				return true;
			}

			boolean is_nine_boom = false;
			tagAnalyseIndexResult_WSK card_card_index = new tagAnalyseIndexResult_WSK();
			this._logic.AnalysebCardDataToIndex(GRR._cards_data[i], GRR._card_count[i], card_card_index);
			for (int j = 0; j < GameConstants.WSK_MAX_INDEX; j++) {
				if (j == 12) {
					if (card_card_index.card_index[j] + wang_count >= 8) {
						is_nine_boom = true;
						break;
					}
				} else if (has_rule(WSKConstants.GAME_RULE_YSDZ_7_REWARD) && j == 4) {
					if (card_card_index.card_index[j] + wang_count >= 8) {
						is_nine_boom = true;
						break;
					}
				} else if (has_rule(WSKConstants.GAME_RULE_YSDZ_J_REWARD) && j == 8) {
					if (card_card_index.card_index[j] + wang_count >= 8) {
						is_nine_boom = true;
						break;
					}
				} else if (has_rule(WSKConstants.GAME_RULE_YSDZ_K_REWARD) && j == 10) {
					if (card_card_index.card_index[j] + wang_count >= 8) {
						is_nine_boom = true;
						break;
					}
				} else if (has_rule(WSKConstants.GAME_RULE_YSDZ_A_REWARD) && j == 11) {
					if (card_card_index.card_index[j] + wang_count >= 8) {
						is_nine_boom = true;
						break;
					}
				} else if (card_card_index.card_index[j] + wang_count >= 9) {
					if (card_card_index.card_index[j] + wang_count >= 8) {
						is_nine_boom = true;
						break;
					}
				}
			}
			if (is_nine_boom) {
				GameSchedule.put(
						new GameFinishRunnable(getRoom_id(), GRR._banker_player, GameConstants.Game_End_NORMAL), 3,
						TimeUnit.SECONDS);
				return true;
			}
		}
		// 无炸摊牌
		if (has_rule(WSKConstants.GAME_RULE_YSDZ_WU_ZHA_TAN_PAI)) {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (!this._logic.have_card_num(GRR._cards_data[i], GRR._card_count[i], 4)) {
					mian_da_lei_xing = TAB_PAI_TYPE_WU_ZHA;
					GameSchedule.put(
							new GameFinishRunnable(getRoom_id(), GRR._banker_player, GameConstants.Game_End_NORMAL), 3,
							TimeUnit.SECONDS);
					return true;
				}
			}
		}

		// 罚王摊牌
		boolean is_fa_wang = false;
		int fei_wang_total_count = 0;
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (_fei_wang_count[i] > 0 && _fei_wang_count[this._friend_seat[i]] > 0) {
				_current_player = GameConstants.INVALID_SEAT;
				is_fa_wang = true;
				break;
			}
			fei_wang_total_count += _fei_wang_count[i];
		}
		if (is_fa_wang || fei_wang_total_count >= 3) {
			mian_da_lei_xing = TAB_PAI_TYPE_FA_WANG;
			GameSchedule.put(new GameFinishRunnable(getRoom_id(), GRR._banker_player, GameConstants.Game_End_NORMAL), 3,
					TimeUnit.SECONDS);
			return true;
		}

		return false;
	}

	/**
	 * 是否春天
	 * 
	 * @return
	 */
	public boolean checkChunTian(int seatIndex) {
		for (int player = 0; player < getTablePlayerNumber(); player++) {
			if (player == seatIndex) {
				continue;
			}
			// 27张牌，一张没出
			if (GRR._card_count[player] != get_hand_card_count_max()) {
				return false;
			}
		}
		return true;
	}

	public boolean set_timer(int timer_type, int time) {

		_cur_game_timer = timer_type;
		if (_game_scheduled != null)
			this.kill_timer();
		if (time == 0) {
			return true;
		}
		zi_dong_tou_xiang = true;
		_game_scheduled = GameSchedule.put(new AnimationRunnable(getRoom_id(), timer_type), time * 1000,
				TimeUnit.MILLISECONDS);
		_operate_start_time = (int) (System.currentTimeMillis() / 1000L);
		this._cur_operate_time = time;

		return true;
	}

	public void kill_timer() {
		if (_game_scheduled != null) {
			_game_scheduled.cancel(false);
			_game_scheduled = null;
		}
	}

	@Override
	public boolean animation_timer(int timer_id) {
		if (!zi_dong_tou_xiang) {
			return false;
		}
		switch (timer_id) {
		case TIME_OUT_NO_TOU_XIANG:
			boolean is_beigin = true;
			// for (int i = 0; i < getPlayerCount(); i++) {
			// if (_is_tou_xiang[i] == 0 && _is_tou_xiang_agree[i] == -1) {
			// is_beigin = true;
			// }
			// }
			if (this._game_status == GameConstants.GS_GFWSK_PLAY) {
				return false;
			}
			if (is_beigin) {
				this._game_status = GameConstants.GS_GFWSK_PLAY;
				this._current_player = this.GRR._banker_player;
				operate_out_card(GameConstants.INVALID_SEAT, 0, null, GameConstants.WSK_GF_CT_ERROR,
						GameConstants.INVALID_SEAT, false);
			}
			break;

		default:
			break;
		}
		return false;
	}

	@Override
	protected void test_cards() {
		// int cards[] = new int[] { 0x0d, 0x1d, 0x2d, 0x3D, 0x0D, 0x1D, 0x2D,
		// 0x4E, 0x4F, 0x06, 0x16, 0x26, 0x36, 0x07, 0x07, 0x07 };
		// int cards[] = new int[] { 0x02, 0x12, 0x22, 0x32, 0x02, 0x12, 0x22,
		// 0x01, 0x4F, 0x07, 0x17, 0x27, 0x37, 0x07, 0x17, 0x27, 0x01, 0x09,
		// 0x0a, 0x1a, 0x2a, 0x3a, 0x0a, 0x1a, 0x2a, 0x01, 0x19, };

		int cards[] = new int[] { 22, 34, 18, 11, 23, 41, 36, 37, 49, 59, 43, 52, 19, 39, 20, 37, 60, 54, 50, 6, 34, 20,
				8, 41, 17, 12, 13, 55, 24, 39, 44, 42, 55, 40, 51, 43, 27, 51, 29, 35, 52, 8, 6, 53, 12, 54, 29, 33, 49,
				7, 38, 10, 56, 57, 26, 42, 58, 79, 25, 40, 53, 3, 1, 7, 17, 19, 24, 33, 60, 44, 61, 57, 27, 9, 59, 45,
				45, 79, 78, 2, 78, 35, 21, 1, 5, 23, 58, 5, 61, 2, 21, 3, 4, 10, 18, 25, 13, 28, 11, 50, 9, 28, 36, 38,
				26, 56, 22, 4 };
		int index = 0;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			for (int j = 0; j < get_hand_card_count_max(); j++) {
				GRR._cards_data[i][j] = cards[index++];
			}
		}
		/********
		 * 下面这个给测试用的 如果开发用 把上面的注释去掉 realyCards
		 **************************/
		if (BACK_DEBUG_CARDS_MODE) {
			if (debug_my_cards != null) {
				if (debug_my_cards.length > this.get_hand_card_count_max()) {
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
			_logic.SortCardList(GRR._cards_data[i], GRR._card_count[i], _score_type);
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

	/**
	 * 处理一些值的清理
	 * 
	 * @return
	 */
	@Override
	public boolean reset_init_data() {
		if (_cur_round == 0) {

			this.initBanker();
			record_game_room();
		}

		_run_player_id = 0;
		// 设置变量

		GRR = new GameRoundRecord(this.getTablePlayerNumber(), GameConstants.MAX_WEAVE_LAOPAI,
				GameConstants.WSK_MAX_COUNT, GameConstants.MAX_INDEX_LAOPAI);
		GRR._start_time = System.currentTimeMillis() / 1000L;
		GRR._game_type_index = _game_type_index;
		GRR._cur_round = _cur_round;
		_end_reason = GameConstants.INVALID_VALUE;
		istrustee = new boolean[4];
		// 新建
		_playerStatus = new PlayerStatus[4];
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_playerStatus[i] = new PlayerStatus();
			_is_ming_pai[i] = -1;
			_is_tou_xiang[i] = -1;
			_is_tou_xiang_agree[i] = -1;
		}

		// _cur_round=8;

		_is_shou_chu = 1;
		for (int i = 0; i < getTablePlayerNumber(); i++) {
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

		// if (_cur_round == 0) {
		Player rplayer;
		for (int i = 0; i < getTablePlayerNumber(); i++) {
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
			if (rplayer.locationInfor != null) {
				room_player.setLocationInfor(rplayer.locationInfor);
			}
			GRR._video_recode.addPlayers(room_player);
		}
		// }
		_cur_round++;
		GRR._video_recode.setBankerPlayer(this._cur_banker);

		int score_card[] = { 0x05, 0x05, 0x15, 0x15, 0x25, 0x25, 0x35, 0x35, 0x0A, 0x0A, 0x1A, 0x1A, 0x2A, 0x2A, 0x3A,
				0x3A, 0x0D, 0x0D, 0x1D, 0x1D, 0x2D, 0x2D, 0x3D, 0x3D };
		_pai_score_card = score_card;
		player_sort_card = new int[getTablePlayerNumber()];
		return true;
	}

	@Override
	public boolean handler_player_be_in_room(int seat_index) {
		if ((GameConstants.GS_MJ_FREE != _game_status && GameConstants.GS_MJ_WAIT != _game_status)
				&& this.get_players()[seat_index] != null) {
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
		// if (GameConstants.GS_MJ_FREE != _game_status) {
		// return handler_player_ready(seat_index, false);
		// }
		return true;
	}
}
