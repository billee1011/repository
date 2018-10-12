package com.cai.game.wsk.handler.gf;

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
import com.cai.future.GameSchedule;
import com.cai.future.runnable.AnimationRunnable;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.game.RoomUtil;
import com.cai.game.wsk.AbstractWSKTable;
import com.cai.game.wsk.WSKGameLogic_GF;
import com.cai.game.wsk.WSKType;
import com.cai.service.MongoDBServiceImpl;
import com.cai.service.PlayerServiceImpl;

import protobuf.clazz.Protocol.GameEndResponse;
import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomPlayerResponse;
import protobuf.clazz.Protocol.RoomRequest;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.gfWsk.gfWskRsp.GameStart_Wsk_GF;
import protobuf.clazz.gfWsk.gfWskRsp.LiangPai_Result_Wsk_GF;
import protobuf.clazz.gfWsk.gfWskRsp.Opreate_RequestWsk_GF;
import protobuf.clazz.gfWsk.gfWskRsp.OutCardDataWsk_GF;
import protobuf.clazz.gfWsk.gfWskRsp.PukeGameEndWsk_GF;
import protobuf.clazz.gfWsk.gfWskRsp.RefreshCardData;
import protobuf.clazz.gfWsk.gfWskRsp.RefreshScore_Wsk_GF;
import protobuf.clazz.gfWsk.gfWskRsp.Refresh_Pai_GF;
import protobuf.clazz.gfWsk.gfWskRsp.TableResponse_Wsk_GF;
import protobuf.clazz.gfWsk.gfWskRsp.TouXiang_Anser_Wsk_GF;
import protobuf.clazz.gfWsk.gfWskRsp.TouXiang_Result_Wsk_GF;
import protobuf.clazz.gfWsk.gfWskRsp.UserCardData;

public class WSKTable_GF extends AbstractWSKTable {

	private static final long serialVersionUID = -2419887548488809773L;

	public int _get_score[];
	public int _is_call_banker[];

	public int _turn_have_score; // 当局得分
	public boolean _is_yi_da_san;
	public int _score_type;
	public int _fei_wang_card[][];
	public int _fei_wang_count[];
	public int _jiao_pai_card;
	public int _jiao_pai_seat;
	public int _lose_num[];
	public int _win_num[];
	public List<UserCardData.Builder> _user_data;
	public int _user_data_score[][];
	protected static final int GAME_OPREATE_TYPE_TOU_XIANG_REQUEST = 1;
	protected static final int GAME_OPREATE_TYPE_TOU_XIANG_AGREE = 2;
	protected static final int GAME_OPREATE_TYPE_TOU_XIANG_DISAGREE = 3;
	protected static final int GAME_OPREATE_TYPE_LIANG_PAI = 4;
	protected static final int GAME_OPREATE_TYPE_CALL_BANKER = 5;
	protected static final int GAME_OPREATE_TYPE_CALL_BANKER_NO = 6;
	protected static final int GAME_OPREATE_TYPE_TOU_XIANG_NO = 7;
	protected static final int GAME_OPREATE_TYPE_SORT_BY_CARD = 8;
	protected static final int GAME_OPREATE_TYPE_SORT_BY_COUNT = 9;
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
	public WSKGameLogic_GF _logic;

	public WSKTable_GF() {
		super(WSKType.GAME_TYPE_WSK_GF);
	}

	@Override
	protected void onInitTable() {

	}

	@Override
	public void on_init_table(int game_type_index, int game_rule_index, int game_round) {
		_logic = new WSKGameLogic_GF();
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
		_user_data_score = new int[getTablePlayerNumber()][get_hand_card_count_max()];
		_player_result = new PlayerResult(this.getRoom_owner_account_id(), this.getRoom_id(), _game_type_index,
				_game_rule_index, _game_round, this.get_game_des(), this.getTablePlayerNumber());
		_logic.ruleMap = this.ruleMap;
		_score_type = GameConstants.WSK_ST_ORDER;
		_jiao_pai_card = GameConstants.INVALID_CARD;
		_jiao_pai_seat = GameConstants.INVALID_SEAT;
		_handler_out_card_operate = new WSKHandlerOutCardOperate_GF();
		_handler_call_banker = new WSKHandlerCallBnakerGF();
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_friend_seat[i] = -1;
			_max_end_score[i] = 0;
			_wang_count[i] = 0;
			_user_data.add(UserCardData.newBuilder());
		}

		if (has_rule(GameConstants.GAME_RULE_WSK_GF_TWO_SHENG_DANG)) {
			sheng_dang_biaozhi |= 1;
		}
		if (has_rule(GameConstants.GAME_RULE_WSK_GF_SEVEN_SHENG_DANG)) {
			sheng_dang_biaozhi |= 2;
		}
		if (has_rule(GameConstants.GAME_RULE_WSK_GF_J_SHENG_DANG)) {
			sheng_dang_biaozhi |= 4;
		}
	}

	@Override
	public void Refresh_user_get_score(int to_player) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		RefreshScore_Wsk_GF.Builder refresh_user_getscore = RefreshScore_Wsk_GF.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_WSK_GF_USER_GET_SCORE);
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
			Arrays.fill(_user_data_score[i], 0);
			_user_data.get(i).clear();
			sort[i] = GameConstants.WSK_ST_ORDER;
		}
		prev_out_palyer = -1;
		_score_type = GameConstants.WSK_ST_ORDER;
		_is_yi_da_san = false;
		_game_status = GameConstants.GS_GFWSK_CALLBANKER;
		_pai_score_count = 24;
		_pai_score = 200;
		_turn_have_score = 0;
		_jiao_pai_card = GameConstants.INVALID_CARD;
		_jiao_pai_seat = GameConstants.INVALID_SEAT;
		Arrays.fill(_chuwan_shunxu, GameConstants.INVALID_SEAT);

		_repertory_card = new int[GameConstants.CARD_COUNT_WSK];
		shuffle(_repertory_card, GameConstants.CARD_DATA_WSK);

		if (DEBUG_CARDS_MODE || BACK_DEBUG_CARDS_MODE)
			test_cards();

		_liang_card_value = _repertory_card[RandomUtil.getRandomNumber(Integer.MAX_VALUE)
				% GameConstants.CARD_COUNT_WSK];
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
		for (int i = 0; i < count; i++) {
			for (int j = 0; j < this.get_hand_card_count_max(); j++) {
				GRR._cards_data[i][j] = repertory_card[i * this.get_hand_card_count_max() + j];
			}
			GRR._card_count[i] = this.get_hand_card_count_max();
			_logic.SortCardList(GRR._cards_data[i], GRR._card_count[i], _score_type);
		}
		// 记录初始牌型
		_recordRoomRecord.setBeginArray(Arrays.toString(repertory_card));
	}

	@Override
	protected boolean on_game_start() {
		is_touxiang = false;
		this._current_player = _cur_banker;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			int wang_count = this._logic.Get_Wang_Count(GRR._cards_data[i], GRR._card_count[i]);
			_wang_count[i] = wang_count;
			if (wang_count != 0 && wang_count != 4
					&& !this._logic.have_card_num(GRR._cards_data[i], GRR._card_count[i], 4)) {
				for (int j = 0; j < GRR._card_count[i]; j++) {
					if (GRR._cards_data[i][j] == 0x4E || GRR._cards_data[i][j] == 0x4F
							|| GRR._cards_data[i][j] == 0x4E + 0x100 || GRR._cards_data[i][j] == 0x4F + 0x100) {
						fa_wang[i] = 1;
						_fei_wang_card[i][_fei_wang_count[i]++] = GRR._cards_data[i][j];
					}
				}
			}
		}

		for (int play_index = 0; play_index < this.getTablePlayerNumber(); play_index++) {

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_WSK_GF_GAME_START);
			roomResponse.setGameStatus(this._game_status);
			roomResponse.setRoomInfo(getRoomInfo());
			// 发送数据
			GameStart_Wsk_GF.Builder gamestart = GameStart_Wsk_GF.newBuilder();
			gamestart.setRoomInfo(this.getRoomInfo());
			gamestart.setCurBanker(this._current_player);

			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				// 分析扑克
				gamestart.addCardCount(GRR._card_count[i]);
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
		roomResponse.setType(MsgConstants.RESPONSE_WSK_GF_GAME_START);
		roomResponse.setGameStatus(this._game_status);
		roomResponse.setRoomInfo(this.getRoomInfo());
		// 发送数据
		GameStart_Wsk_GF.Builder gamestart = GameStart_Wsk_GF.newBuilder();
		gamestart.setRoomInfo(this.getRoomInfo());
		if (this._cur_round == 1) {
			this.load_player_info_data_game_start(gamestart);
		}
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

		this.set_handler(this._handler_call_banker);

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
		roomResponse.setType(MsgConstants.RESPONSE_WSK_GF_GAME_END);
		GameEndResponse.Builder game_end = GameEndResponse.newBuilder();
		PukeGameEndWsk_GF.Builder game_end_wsk_gf = PukeGameEndWsk_GF.newBuilder();
		game_end.setRoomInfo(getRoomInfo());
		game_end_wsk_gf.setRoomInfo(getRoomInfo());

		int end_score[] = new int[this.getTablePlayerNumber()];
		int dang_ju_score[] = new int[this.getTablePlayerNumber()];
		int jia_fa_socre[] = new int[this.getTablePlayerNumber()];
		Arrays.fill(end_score, 0);
		Arrays.fill(dang_ju_score, 0);
		Arrays.fill(jia_fa_socre, 0);
		// 计算分数
		if (reason == GameConstants.Game_End_NORMAL && GRR != null) {
			cal_score_wsk(end_score, dang_ju_score, seat_index, jia_fa_socre);
		} else if (reason == GameConstants.Game_End_DRAW) {
			if (GRR != null) {
				cal_score_mian_da(end_score, dang_ju_score, jia_fa_socre);
			}
		}

		game_end.setGameRound(_game_round);
		game_end.setCurRound(_cur_round);
		if (GRR != null) {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				game_end_wsk_gf.addCardCount(GRR._card_count[i]);
				Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
				Int32ArrayResponse.Builder fei_wang_card = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GRR._card_count[i]; j++) {
					cards_card.addItem(GRR._cards_data[i][j]);
				}
				for (int j = 0; j < this._fei_wang_count[i]; j++) {
					fei_wang_card.addItem(this._fei_wang_card[i][j]);
				}
				if (reason != GameConstants.Game_End_RELEASE_RESULT) {
					game_end_wsk_gf.addHandCardData(cards_card.build());
				} else {
					Int32ArrayResponse.Builder cards_card1 = Int32ArrayResponse.newBuilder();
					game_end_wsk_gf.addHandCardData(cards_card1.build());
				}
				game_end_wsk_gf.addFaWangCardData(fei_wang_card);
			}
			game_end.setStartTime(GRR._start_time);
			game_end.setGameTypeIndex(GRR._game_type_index);
		} else {
			Int32ArrayResponse.Builder fei_wang_card = Int32ArrayResponse.newBuilder();
			game_end_wsk_gf.addFaWangCardData(fei_wang_card);
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
			game_end_wsk_gf.addEndScore(end_score[i]);
			game_end_wsk_gf.addRewardScore(this._xi_qian_score[i]);
			game_end_wsk_gf.addDangJuScore(dang_ju_score[i]);
			game_end_wsk_gf.addZhuaFen(this._get_score[i]);
			game_end_wsk_gf.addJiaFaSocre(jia_fa_socre[i]);
			game_end_wsk_gf.addWinOrder(_chuwan_shunxu[i]);
			game_end_wsk_gf.addCardsData(_user_data.get(i));
			Int32ArrayResponse.Builder data_score = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < _user_data.get(i).getCardsDataCount(); j++) {
				data_score.addItem(_user_data_score[i][j]);
			}
			game_end_wsk_gf.addCardDataScore(data_score);
			// if (reason != GameConstants.Game_End_RELEASE_RESULT) {
			// }else{
			// game_end_wsk_gf.addCardsData(UserCardData.newBuilder());
			// }
		}

		this.load_player_info_data_game_end(game_end_wsk_gf);
		boolean end = false;
		if (reason == GameConstants.Game_End_NORMAL || reason == GameConstants.Game_End_DRAW) {
			if (_cur_round >= _game_round) {// 局数到了
				end = true;
				for (int i = 0; i < getTablePlayerNumber(); i++) {
					game_end_wsk_gf.addAllEndScore((int) this._player_result.game_score[i]);
					game_end_wsk_gf.addAllRewardScore(_xi_qian_total_score[i]);
					game_end_wsk_gf.addEndScoreMax(_max_end_score[i]);
					game_end_wsk_gf.addLoseNum(this._lose_num[i]);
					game_end_wsk_gf.addWinNum(this._win_num[i]);
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
				game_end_wsk_gf.addAllEndScore((int) this._player_result.game_score[i]);
				game_end_wsk_gf.addAllRewardScore(_xi_qian_total_score[i]);
				game_end_wsk_gf.addEndScoreMax(_max_end_score[i]);
				game_end_wsk_gf.addLoseNum(this._lose_num[i]);
				game_end_wsk_gf.addWinNum(this._win_num[i]);
			}

			game_end.setPlayerResult(this.process_player_result(reason));
			real_reason = GameConstants.Game_End_RELEASE_PLAY;
		}
		game_end_wsk_gf.setMianDa(mian_da_lei_xing);
		game_end_wsk_gf.setReason(real_reason);
		////////////////////////////////////////////////////////////////////// 得分总的
		game_end.setEndType(real_reason);
		game_end.setGameRound(_game_round);
		game_end.setCurRound(_cur_round);
		game_end.setCommResponse(PBUtil.toByteString(game_end_wsk_gf));
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
	public void cal_score_wsk(int end_score[], int dang_ju_fen[], int win_seat_index, int jia_fa_socre[]) {

		// boolean is_touxiang = true;
		// if (!this._is_yi_da_san) {
		// for (int i = 0; i < this.getTablePlayerNumber(); i++) {
		// if (this._is_tou_xiang[i] == 1 &&
		// this._is_tou_xiang_agree[_friend_seat[i]] == 1) {
		// is_touxiang = true;
		// break;
		// }
		// }
		// } else {
		// is_touxiang = true;
		// for (int i = 0; i < this.getTablePlayerNumber(); i++) {
		// if (i == GRR._banker_player) {
		// continue;
		// }
		// if (this._is_tou_xiang[i] != 1 || this._is_tou_xiang_agree[i] != 1) {
		// is_touxiang = false;
		// break;
		// }
		// }
		// }
		int score = (int) this.game_cell;
		if (!is_touxiang) {
			int shang_you_score = 0;
			int xia_you_score = 0;
			if (!_is_yi_da_san) {
				score *= 2;
				if (_friend_seat[_chuwan_shunxu[0]] == _chuwan_shunxu[1]) {
					shang_you_score = _get_score[this._chuwan_shunxu[0]] + _get_score[_chuwan_shunxu[1]];
					for (int i = 0; i < getTablePlayerNumber(); i++) {
						if (i != _chuwan_shunxu[0] && i != _chuwan_shunxu[1]) {
							xia_you_score += _get_score[i];
						}
					}
					if (shang_you_score >= xia_you_score) {
						if (shang_you_score == 200) {
							score *= 3;
						} else {
							score *= 2;
						}
					} else {
						score *= 2;
					}
					dang_ju_fen[_chuwan_shunxu[0]] += score;
					dang_ju_fen[_chuwan_shunxu[1]] += score;
					dang_ju_fen[_chuwan_shunxu[2]] -= score;
					dang_ju_fen[_chuwan_shunxu[3]] -= score;
				} else {
					shang_you_score = _get_score[this._chuwan_shunxu[0]] + _get_score[_friend_seat[_chuwan_shunxu[0]]];
					for (int i = 0; i < getTablePlayerNumber(); i++) {
						if (i != _chuwan_shunxu[0] && i != _friend_seat[_chuwan_shunxu[0]]) {
							xia_you_score += _get_score[i];
						}
					}
					if (shang_you_score >= xia_you_score) {
						if (shang_you_score == 200) {
							score *= 3;
						}
						dang_ju_fen[_chuwan_shunxu[0]] += score;
						dang_ju_fen[_friend_seat[_chuwan_shunxu[0]]] += score;
						for (int i = 0; i < getTablePlayerNumber(); i++) {
							if (i != _chuwan_shunxu[0] && i != _friend_seat[_chuwan_shunxu[0]]) {
								dang_ju_fen[i] -= score;
							}
						}
					} else {
						if (shang_you_score == 0) {
							score *= 3;
						}
						dang_ju_fen[_chuwan_shunxu[0]] -= score;
						dang_ju_fen[_friend_seat[_chuwan_shunxu[0]]] -= score;
						for (int i = 0; i < getTablePlayerNumber(); i++) {
							if (i != _chuwan_shunxu[0] && i != _friend_seat[_chuwan_shunxu[0]]) {
								dang_ju_fen[i] += score;
							}
						}
					}
				}
			} else {
				score *= 6;
				if (_chuwan_shunxu[0] == GRR._banker_player) {
					for (int i = 0; i < getTablePlayerNumber(); i++) {
						if (i == GRR._banker_player) {
							dang_ju_fen[i] += score * (getTablePlayerNumber() - 1);
						} else {
							dang_ju_fen[i] -= score;
						}
					}
				} else {
					for (int i = 0; i < getTablePlayerNumber(); i++) {
						if (i == GRR._banker_player) {
							dang_ju_fen[i] -= score * (getTablePlayerNumber() - 1);
						} else {
							dang_ju_fen[i] += score;
						}
					}
				}
			}

			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				if (_fei_wang_count[i] > 0) {
					_fei_wang_count[i] = _fei_wang_count[i] > 3 ? 3 : _fei_wang_count[i];
					jia_fa_socre[i] -= _fei_wang_count[i] * 3;
					for (int j = 0; j < this.getTablePlayerNumber(); j++) {
						if (i == j) {
							continue;
						}
						jia_fa_socre[j] += _fei_wang_count[i];
					}
				}
			}

			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				end_score[i] += dang_ju_fen[i] + this._xi_qian_score[i] + jia_fa_socre[i];
				this._player_result.game_score[i] += end_score[i];
			}
		} else {
			int score1 = (int) this.game_cell;
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				int xi_qian_score = _logic.GetHandCardXianScore(GRR._cards_data[i], GRR._card_count[i],
						sheng_dang_biaozhi, _user_data.get(i), _user_data_score[i]);
				for (int j = 0; j < getTablePlayerNumber(); j++) {
					if (i == j) {
						continue;
					}
					_xi_qian_score[i] += xi_qian_score;
					_xi_qian_score[j] -= xi_qian_score;
				}
			}
			if (_is_yi_da_san) {
				score1 *= 6;
				dang_ju_fen[win_seat_index] = (getTablePlayerNumber() - 1) * score1;

			} else {
				score1 *= 2;
				dang_ju_fen[win_seat_index] = score1;
				dang_ju_fen[_friend_seat[win_seat_index]] = score1;
			}
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				if (_fei_wang_count[i] > 0) {
					_fei_wang_count[i] = _fei_wang_count[i] > 3 ? 3 : _fei_wang_count[i];
					jia_fa_socre[i] -= _fei_wang_count[i] * 3;
					for (int j = 0; j < this.getTablePlayerNumber(); j++) {
						if (i == j) {
							continue;
						}
						jia_fa_socre[j] += _fei_wang_count[i];
					}
				}
			}
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i != win_seat_index && i != _friend_seat[win_seat_index]) {
					dang_ju_fen[i] -= score1;
				}
			}
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				_xi_qian_score[i] /= game_cell;
				end_score[i] += dang_ju_fen[i] + this._xi_qian_score[i];

				this._player_result.game_score[i] += end_score[i];
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
					_user_data.get(i), _user_data_score[i]);
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

	public void load_player_info_data_game_start(GameStart_Wsk_GF.Builder roomResponse) {
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

	public void load_player_info_data_reconnect(TableResponse_Wsk_GF.Builder roomResponse) {
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

	public void load_player_info_data_game_end(PukeGameEndWsk_GF.Builder roomResponse) {
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

		if (_is_yi_da_san) {
			_jiao_pai_card = GameConstants.INVALID_CARD;
		}
		if (to_player == GameConstants.INVALID_SEAT) {
			for (int index = 0; index < this.getTablePlayerNumber(); index++) {
				RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
				OutCardDataWsk_GF.Builder outcarddata = OutCardDataWsk_GF.newBuilder();
				// Int32ArrayResponse.Builder cards =
				// Int32ArrayResponse.newBuilder();
				roomResponse.setType(MsgConstants.RESPONSE_WSK_GF_OUT_CARD);// 201
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
				if (index == this._current_player) {
					int tip_out_card[][] = new int[GRR._card_count[index] * 4][GRR._card_count[index]];
					int tip_out_count[] = new int[GRR._card_count[index] * 4];
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
					if (i == index || (index == (i + 2) % getTablePlayerNumber() && GRR._card_count[index] == 0)) {
						for (int j = 0; j < this.GRR._card_count[i]; j++) {
							cards_card.addItem(this.GRR._cards_data[i][j]);
						}
					} else if (this._is_ming_pai[i] == 1) {
						for (int j = 0; j < this.GRR._card_count[i]; j++) {
							cards_card.addItem(this.GRR._cards_data[i][j]);
						}
					}

					if (i == _cur_banker || i == _friend_seat[_cur_banker]) {
						outcarddata.addFriendSeat(_jiao_pai_card);
					} else {
						outcarddata.addFriendSeat(0);
					}

					outcarddata.addHandCardsData(cards_card);
					outcarddata.addHandCardCount(this.GRR._card_count[i]);
					outcarddata.addWinOrder(this._chuwan_shunxu[i]);
				}
				outcarddata.setLiangPai(_jiao_pai_card);
				roomResponse.setCommResponse(PBUtil.toByteString(outcarddata));

				this.send_response_to_player(index, roomResponse);

			}

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			OutCardDataWsk_GF.Builder outcarddata = OutCardDataWsk_GF.newBuilder();
			// Int32ArrayResponse.Builder cards =
			// Int32ArrayResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_WSK_GF_OUT_CARD);// 201
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
			OutCardDataWsk_GF.Builder outcarddata = OutCardDataWsk_GF.newBuilder();
			// Int32ArrayResponse.Builder cards =
			// Int32ArrayResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_WSK_GF_OUT_CARD);// 201
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
				if (i == to_player
						|| (to_player == (i + 2) % getTablePlayerNumber() && GRR._card_count[to_player] == 0)) {
					for (int j = 0; j < this.GRR._card_count[i]; j++) {
						cards_card.addItem(this.GRR._cards_data[i][j]);
					}
				} else if (this._is_ming_pai[i] == 1) {
					for (int j = 0; j < this.GRR._card_count[i]; j++) {
						cards_card.addItem(this.GRR._cards_data[i][j]);
					}
				}

				if (i == _cur_banker || i == _friend_seat[_cur_banker]) {
					outcarddata.addFriendSeat(_jiao_pai_card);
				} else {
					outcarddata.addFriendSeat(0);
				}

				outcarddata.addHandCardsData(cards_card);
				outcarddata.addHandCardCount(this.GRR._card_count[i]);
				outcarddata.addWinOrder(this._chuwan_shunxu[i]);
			}
			outcarddata.setLiangPai(_jiao_pai_card);
			roomResponse.setCommResponse(PBUtil.toByteString(outcarddata));

			this.send_response_to_player(to_player, roomResponse);

		}

		return true;
	}

	@Override
	public boolean handler_requst_message_deal(Player player, int seat_index, RoomRequest room_rq, int type) {
		if (type == MsgConstants.REQUST_GF_WSK_OPERATE) {
			Opreate_RequestWsk_GF req = PBUtil.toObject(room_rq, Opreate_RequestWsk_GF.class);
			// 逻辑处理
			return handler_requst_opreate(seat_index, req.getOpreateType(), req.getCardData(), req.getSortCardList());
		}
		return true;
	}

	public boolean handler_requst_opreate(int seat_index, int opreate_type, int card_data, List<Integer> list) {
		switch (opreate_type) {
		case GAME_OPREATE_TYPE_TOU_XIANG_REQUEST: {
			deal_tou_xiang_request(seat_index, GameConstants.INVALID_SEAT);
			return true;
		}
		case GAME_OPREATE_TYPE_TOU_XIANG_AGREE: {
			deal_tou_xiang_anser_agree(seat_index);
			return true;
		}
		case GAME_OPREATE_TYPE_TOU_XIANG_DISAGREE: {
			deal_tou_xiang_anser_disagree(seat_index);
			return true;
		}
		case GAME_OPREATE_TYPE_LIANG_PAI: {
			deal_liang_pai(seat_index, card_data);
			return true;
		}
		case GAME_OPREATE_TYPE_CALL_BANKER: {
			if (_handler_call_banker != null) {
				this._handler_call_banker.handler_call_banker(this, seat_index, 1);
			}
			return true;
		}
		case GAME_OPREATE_TYPE_CALL_BANKER_NO: {
			if (_handler_call_banker != null) {
				this._handler_call_banker.handler_call_banker(this, seat_index, 0);
			}
			return true;
		}
		case GAME_OPREATE_TYPE_TOU_XIANG_NO: {
			deal_request_no_tou_xiang_(seat_index);
			return true;
		}
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

	public void deal_tou_xiang_request(int seat_index, int to_player) {
		if (this._game_status != GameConstants.GS_GFWSK_TOU_XIANG) {
			return;
		}
		if (_is_tou_xiang[seat_index] != 0) {
			return;
		}
		if (GRR == null) {
			return;
		}
		if (_is_yi_da_san) {
			if (seat_index == this.GRR._banker_player) {
				return;
			}
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == GRR._banker_player || seat_index == i) {
					continue;
				}
				if (_is_tou_xiang[i] == 1) {
					this.send_error_notify(seat_index, 2, "等待搭档投降确认");
					return;
				}
			}
		} else if (_is_tou_xiang[_friend_seat[seat_index]] == 1) {
			this.send_error_notify(seat_index, 2, "等待搭档投降确认");
			return;
		}
		if (!_is_yi_da_san) {
			if (_tou_xiang_times[seat_index] >= 1) {
				int win_sort = GameConstants.INVALID_SEAT;
				for (int i = 0; i < this.getTablePlayerNumber(); i++) {
					if (i != seat_index && i != _friend_seat[seat_index]) {
						win_sort = i;
						break;
					}
				}

				RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
				roomResponse.setType(MsgConstants.RESPONSE_WSK_GF_TOUXIANG_RESULT);
				// 发送数据
				for (int i = 0; i < getTablePlayerNumber(); i++) {
					TouXiang_Result_Wsk_GF.Builder tou_xiang_result = TouXiang_Result_Wsk_GF.newBuilder();
					if (i == seat_index || i == _friend_seat[seat_index]) {
						tou_xiang_result.setOpreateSeatIndex(seat_index);
						tou_xiang_result.setOpreateStr(this.get_players()[seat_index].getNick_name() + "强烈要求投降");
					} else {
						tou_xiang_result.setOpreateSeatIndex(seat_index);
						tou_xiang_result.setOpreateStr("对方已投降");
					}
					tou_xiang_result.setIsOkCancel(0);
					roomResponse.setCommResponse(PBUtil.toByteString(tou_xiang_result));
					this.send_response_to_player(i, roomResponse);
				}

				GameSchedule.put(new GameFinishRunnable(getRoom_id(), win_sort, GameConstants.Game_End_NORMAL), 3,
						TimeUnit.SECONDS);
			} else {
				send_is_tou_xiang(seat_index, true, _friend_seat[seat_index]);
				send_is_tou_xiang(seat_index, true, seat_index);
			}
		} else {
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				if (i == this.GRR._banker_player) {
					continue;
				}
				send_is_tou_xiang(seat_index, true, i);
			}
		}
		_is_tou_xiang_agree[seat_index] = 1;
		_is_tou_xiang[seat_index] = 1;
		_tou_xiang_times[seat_index]++;
	}

	public void deal_request_no_tou_xiang_(int seat_index) {
		if (this._game_status != GameConstants.GS_GFWSK_TOU_XIANG) {
			return;
		}
		if (GRR == null) {
			return;
		}
		if (_is_yi_da_san) {
			if (seat_index == this.GRR._banker_player) {
				return;
			}
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == GRR._banker_player || seat_index == i) {
					continue;
				}
				if (_is_tou_xiang[i] == 1) {
					this.send_error_notify(seat_index, 2, "等待搭档投降确认");
					return;
				}
			}
		} else if (_is_tou_xiang[_friend_seat[seat_index]] == 1) {
			this.send_error_notify(seat_index, 2, "等待搭档投降确认");
			return;
		}

		if (!_is_yi_da_san) {
			send_is_tou_xiang(seat_index, false, _friend_seat[seat_index]);
			send_is_tou_xiang(seat_index, false, seat_index);
		} else {
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				if (i == this.GRR._banker_player) {
					continue;
				}
				send_is_tou_xiang(seat_index, false, i);
				_is_tou_xiang[i] = 2;
			}

		}
		_is_tou_xiang[seat_index] = 2;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (_is_tou_xiang[i] < 1) {
				return;
			}
		}
		int temp_tou_xiang_player = -1;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (_is_tou_xiang[i] == 1 && temp_tou_xiang_player == -1) {
				temp_tou_xiang_player = i;
			} else if (_is_tou_xiang[i] == 1 && temp_tou_xiang_player == -1) {
				if (_friend_seat[temp_tou_xiang_player] == i) {
					return;
				}
			}
		}
		// zi_dong_tou_xiang = false;
		this._game_status = GameConstants.GS_GFWSK_PLAY;
		this._current_player = this.GRR._banker_player;
		this.operate_out_card(GameConstants.INVALID_SEAT, 0, null, GameConstants.WSK_GF_CT_ERROR,
				GameConstants.INVALID_SEAT, false);
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
		if (list.size() == 0 || GRR == null) {
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
				}

			}
		}

		int temp_color = 0;
		for (int i = 0; i < list.size(); i++) {
			if ((out_cards[i] / 0x100 < temp_color || temp_color == 0) && out_cards[i] > 0x100) {
				temp_color = out_cards[i] / 0x100;
				for (int j = 0; j < this.GRR._card_count[seat_index]; j++) {
					int temp = GRR._cards_data[seat_index][j];
					if (temp > (out_cards[i] / 0x100 * 0x100) && temp > 0x100) {
						GRR._cards_data[seat_index][j] = (temp % 0x100) + ((temp / 0x100 * 0x100) >> 1);
					}

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

		// int _score_type = sort[seat_index];
		// if (_score_type == GameConstants.WSK_ST_COUNT) {
		// _score_type = GameConstants.WSK_ST_ORDER;
		// } else if (_score_type == GameConstants.WSK_ST_ORDER || _score_type
		// == GameConstants.WSK_ST_CUSTOM) {
		// _score_type = GameConstants.WSK_ST_COUNT;
		// }
		// sort[seat_index] = _score_type;
		this._logic.SortCardList(GRR._cards_data[seat_index], GRR._card_count[seat_index], this.sort[seat_index]);
		RefreshCard(seat_index);
	}

	public void deal_sort_card_by_count(int seat_index) {
		if (GRR == null) {
			return;
		}
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
		RefreshCardData.Builder refresh_card = RefreshCardData.newBuilder();
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

	public void send_is_tou_xiang(int seat_index, boolean is_touxiang, int to_player) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_WSK_GF_TOUXIANG_ANSER);
		// 发送数据
		TouXiang_Anser_Wsk_GF.Builder tou_xiang_anser = TouXiang_Anser_Wsk_GF.newBuilder();
		tou_xiang_anser.setOpreateSeatIndex(seat_index);
		tou_xiang_anser.setOpreateStr("您的搭档[" + this.get_players()[seat_index].getNick_name() + "]请求投降，您是否同意投降(投降输两分)");
		tou_xiang_anser.setIsTouXiang(is_touxiang);
		roomResponse.setCommResponse(PBUtil.toByteString(tou_xiang_anser));
		// 自己才有牌数据
		this.send_response_to_player(to_player, roomResponse);
	}

	public void deal_tou_xiang_anser_agree(int seat_index) {
		// 广丰五十K一打三不能投降
		// if (_is_yi_da_san) {
		// if (seat_index == this.GRR._banker_player) {
		// return;
		// }
		//
		// boolean is_touxiang = true;
		// boolean is_have_request = false;
		// int tou_xiang_seat = GameConstants.INVALID_SEAT;
		// for (int i = 0; i < getTablePlayerNumber(); i++) {
		// if (i == GRR._banker_player || seat_index == i) {
		// continue;
		// }
		// if (_is_tou_xiang[i] != 0) {
		// is_have_request = true;
		// tou_xiang_seat = i;
		// break;
		// }
		// }
		// if (!is_have_request) {
		// // 没有玩家进行投降请求
		// return;
		// }
		//
		// _is_tou_xiang[seat_index] = 1;
		// _is_tou_xiang_agree[seat_index] = 1;
		// for (int i = 0; i < getTablePlayerNumber(); i++) {
		// if (i == GRR._banker_player || tou_xiang_seat == i) {
		// continue;
		// }
		// if (_is_tou_xiang_agree[i] != 1) {
		// is_touxiang = false;
		// break;
		// }
		// }
		//
		// RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		// roomResponse.setType(MsgConstants.RESPONSE_WSK_GF_TOUXIANG_RESULT);
		// // 发送数据
		// for (int i = 0; i < getTablePlayerNumber(); i++) {
		// TouXiang_Result_Wsk_GF.Builder tou_xiang_result =
		// TouXiang_Result_Wsk_GF.newBuilder();
		// if (is_touxiang) {
		// if (i == GRR._banker_player) {
		// tou_xiang_result.setOpreateSeatIndex(seat_index);
		// tou_xiang_result.setOpreateStr("对方已投降");
		// }
		// } else {
		// if (i == GRR._banker_player) {
		// continue;
		// }
		// tou_xiang_result.setOpreateSeatIndex(seat_index);
		// tou_xiang_result.setRequestTouXiang(tou_xiang_seat);
		// tou_xiang_result.setOpreateStr(this.get_players()[seat_index].getNick_name()
		// + "同意投降");
		// }
		// if (!"".equals(tou_xiang_result.getOpreateStr())) {
		// tou_xiang_result.setIsOkCancel(0);
		// roomResponse.setCommResponse(PBUtil.toByteString(tou_xiang_result));
		// this.send_response_to_player(i, roomResponse);
		// }
		// }
		// if (is_touxiang) {
		// GameSchedule.put(new GameFinishRunnable(getRoom_id(),
		// GRR._banker_player, GameConstants.Game_End_NORMAL), 3,
		// TimeUnit.SECONDS);
		// }
		// return;
		// } else {
		if (_is_tou_xiang[this._friend_seat[seat_index]] != 1) {
			return;
		}
		if (GRR == null) {
			return;
		}
		_is_tou_xiang[seat_index] = 1;
		_is_tou_xiang_agree[seat_index] = 1;

		int win_sort = GameConstants.INVALID_SEAT;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (i != seat_index && i != _friend_seat[seat_index]) {
				win_sort = i;
				break;
			}
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_WSK_GF_TOUXIANG_RESULT);
		// 发送数据
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			TouXiang_Result_Wsk_GF.Builder tou_xiang_result = TouXiang_Result_Wsk_GF.newBuilder();
			if (i == seat_index || i == _friend_seat[seat_index]) {
				tou_xiang_result.setOpreateSeatIndex(seat_index);
				tou_xiang_result.setOpreateStr("你和搭档都同意投降");
			} else {
				tou_xiang_result.setOpreateSeatIndex(seat_index);
				tou_xiang_result.setOpreateStr("对方已投降");
			}
			tou_xiang_result.setIsOkCancel(0);
			roomResponse.setCommResponse(PBUtil.toByteString(tou_xiang_result));
			this.send_response_to_player(i, roomResponse);
		}

		is_touxiang = true;
		zi_dong_tou_xiang = false;
		GameSchedule.put(new GameFinishRunnable(getRoom_id(), win_sort, GameConstants.Game_End_NORMAL), 1,
				TimeUnit.SECONDS);
		// }
	}

	public void deal_tou_xiang_anser_disagree(int seat_index) {
		if (GRR == null) {
			return;
		}
		// zi_dong_tou_xiang = false;
		if (_is_yi_da_san) {
			if (seat_index == this.GRR._banker_player) {
				return;
			}

			// boolean is_touxiang=true;
			boolean is_have_request = false;
			int tou_xiang_seat = GameConstants.INVALID_SEAT;
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == GRR._banker_player || seat_index == i) {
					continue;
				}
				if (_is_tou_xiang[i] != 0) {
					is_have_request = true;
					tou_xiang_seat = i;
					break;
				}
			}
			if (!is_have_request || tou_xiang_seat == seat_index) {
				// 没有玩家进行投降请求
				return;
			}

			_is_tou_xiang[tou_xiang_seat] = 0;
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				_is_tou_xiang_agree[i] = 0;
				if (i == GRR._banker_player) {
					continue;
				}
				_is_tou_xiang[i] = 2;
			}

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_WSK_GF_TOUXIANG_RESULT);
			// 发送数据
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				TouXiang_Result_Wsk_GF.Builder tou_xiang_result = TouXiang_Result_Wsk_GF.newBuilder();
				if (i == GRR._banker_player) {
					continue;
				}
				tou_xiang_result.setOpreateSeatIndex(seat_index);
				tou_xiang_result.setRequestTouXiang(tou_xiang_seat);
				tou_xiang_result.setOpreateStr(this.get_players()[seat_index].getNick_name() + "拒绝投降");
				tou_xiang_result.setIsOkCancel(0);
				roomResponse.setCommResponse(PBUtil.toByteString(tou_xiang_result));
				this.send_response_to_player(i, roomResponse);
			}

			return;
		} else {
			if (_is_tou_xiang[this._friend_seat[seat_index]] != 1) {
				return;
			}

			_is_tou_xiang_agree[seat_index] = 0;
			_is_tou_xiang[this._friend_seat[seat_index]] = 2;

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_WSK_GF_TOUXIANG_RESULT);
			// 发送数据
			TouXiang_Result_Wsk_GF.Builder tou_xiang_result = TouXiang_Result_Wsk_GF.newBuilder();
			tou_xiang_result.setOpreateSeatIndex(seat_index);
			// if (_tou_xiang_times[_friend_seat[seat_index]] == 1) {
			// tou_xiang_result.setOpreateStr("你的搭档拒绝投降，你是否投降");
			// }
			tou_xiang_result.setOpreateStr("你的搭档拒绝投降");
			tou_xiang_result.setIsOkCancel(0);
			tou_xiang_result.setOpreateSeatIndex(seat_index);
			roomResponse.setCommResponse(PBUtil.toByteString(tou_xiang_result));
			this.send_response_to_player(_friend_seat[seat_index], roomResponse);
			// for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			// if (_is_tou_xiang[i] < 2) {
			// return;
			// }
			// }
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				if (_is_tou_xiang[i] < 1) {
					return;
				}
			}
			int temp_tou_xiang_player = -1;
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				if (_is_tou_xiang[i] == 1 && temp_tou_xiang_player == -1) {
					temp_tou_xiang_player = i;
				} else if (_is_tou_xiang[i] == 1 && temp_tou_xiang_player == -1) {
					if (_friend_seat[temp_tou_xiang_player] == i) {
						return;
					}
				}
			}
			this._game_status = GameConstants.GS_GFWSK_PLAY;
			this._current_player = this.GRR._banker_player;
			this.operate_out_card(GameConstants.INVALID_SEAT, 0, null, GameConstants.WSK_GF_CT_ERROR,
					GameConstants.INVALID_SEAT, false);
		}

	}

	public void deal_liang_pai(int seat_index, int card_data) {
		if (this._game_status != GameConstants.GS_GFWSK_LIANG_PAI || seat_index != this.GRR._banker_player) {
			return;
		}
		if (GRR == null) {
			return;
		}
		int temp_card_data = card_data;
		if (temp_card_data > 0x100) {
			temp_card_data -= 0x100;
		}
		_cur_banker = seat_index;
		// 不能叫大小王
		if (_logic.GetCardColor(temp_card_data) == 4) {
			send_error_notify(seat_index, 2, "请选择正确的牌型!");
			return;
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_WSK_GF_LIANG_PAI_RESULT);
		// 发送数据
		LiangPai_Result_Wsk_GF.Builder liang_pai_result = LiangPai_Result_Wsk_GF.newBuilder();
		int other_seat_index = GameConstants.INVALID_SEAT;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (i == seat_index) {
				continue;
			}
			for (int j = 0; j < this.GRR._card_count[i]; j++) {
				if (temp_card_data == this.GRR._cards_data[i][j]) {
					liang_pai_result.setCardData(this.GRR._cards_data[i][j]);
					other_seat_index = i;
					break;
				}
				if (temp_card_data == this.GRR._cards_data[i][j] - 0x100) {
					liang_pai_result.setCardData(this.GRR._cards_data[i][j] - 0x100);
					other_seat_index = i;
					break;
				}
			}
			if (other_seat_index != GameConstants.INVALID_SEAT) {
				break;
			}
		}

		if (other_seat_index == GameConstants.INVALID_SEAT) {
			this.send_error_notify(seat_index, 2, "请选择正确的牌");
			return;
		}
		_jiao_pai_card = card_data;

		// 保存搭档信息
		_friend_seat[seat_index] = other_seat_index;
		_friend_seat[other_seat_index] = seat_index;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (i != seat_index && i != other_seat_index) {
				for (int j = 0; j < this.getTablePlayerNumber(); j++) {
					if (j != seat_index && j != other_seat_index && i != j) {
						_friend_seat[i] = j;
					}
				}
			}
		}

		liang_pai_result.setOpreatePlayer(seat_index);
		liang_pai_result.setCardData(card_data);
		liang_pai_result.addSeatIndex(seat_index);
		liang_pai_result.addSeatIndex(other_seat_index);
		roomResponse.setCommResponse(PBUtil.toByteString(liang_pai_result));
		GRR.add_room_response(roomResponse);
		this.send_response_to_room(roomResponse);

		if (has_rule(GameConstants.GAME_RULE_WSK_GF_TOU_XIANG)) {
			roomResponse.setType(MsgConstants.RESPONSE_WSK_GF_TOUXIANG_BEGIN);
			this.send_response_to_room(roomResponse);
			this._game_status = GameConstants.GS_GFWSK_TOU_XIANG;
			set_timer(TIME_OUT_NO_TOU_XIANG, 20);
		} else {
			this._game_status = GameConstants.GS_GFWSK_PLAY;
			this._current_player = this.GRR._banker_player;
			this.operate_out_card(GameConstants.INVALID_SEAT, 0, null, GameConstants.WSK_GF_CT_ERROR,
					GameConstants.INVALID_SEAT, false);
		}

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

	@Override
	public boolean trustee_timer(int operate_id, int seat_index) {
		// TODO Auto-generated method stub
		return false;
	}

	/**
	 * 免打检测
	 */
	public boolean checkMianDa() {
		boolean mianDa = false;
		if (has_rule(GameConstants.GAME_RULE_WSK_GF_FA_WANG_BU_DA)) {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (_fei_wang_count[i] > 0) {
					_current_player = GameConstants.INVALID_SEAT;
					break;
				}
			}
			if (_current_player == GameConstants.INVALID_SEAT) {
				mian_da_lei_xing = 1;
				mianDa = true;
				GameSchedule.put(new GameFinishRunnable(getRoom_id(), GRR._banker_player, GameConstants.Game_End_DRAW),
						3, TimeUnit.SECONDS);
				return mianDa;
			}
		}
		if (!mianDa && has_rule(GameConstants.GAME_RULE_WSK_GF_FA_WANG_510K_BUDA)) {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (_fei_wang_count[i] > 0 && _logic.Get_510K_Count(GRR._cards_data[i], GRR._card_count[i]) == 0) {
					_current_player = GameConstants.INVALID_SEAT;
					break;
				}
			}
			if (_current_player == GameConstants.INVALID_SEAT) {
				mian_da_lei_xing = 2;
				mianDa = true;
				GameSchedule.put(new GameFinishRunnable(getRoom_id(), GRR._banker_player, GameConstants.Game_End_DRAW),
						3, TimeUnit.SECONDS);
				return true;
			}
		}
		if (!mianDa && has_rule(GameConstants.GAME_RULE_WSK_GF_JIU_ZHA_MIAN_DA)) {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				int wang_count = _logic.Get_Wang_Count(GRR._cards_data[i], GRR._card_count[i]);
				if (wang_count == 4) {
					_current_player = GameConstants.INVALID_SEAT;
					break;
				}
				if (_logic.have_card_num(GRR._cards_data[i], GRR._card_count[i], 9 - wang_count)) {
					_current_player = GameConstants.INVALID_SEAT;
					break;
				}
				if (has_rule(GameConstants.GAME_RULE_WSK_GF_TWO_SHENG_DANG)
						&& _logic.have_pai_num(GRR._cards_data[i], GRR._card_count[i], 12) == 9 - wang_count - 1) {
					_current_player = GameConstants.INVALID_SEAT;
					break;
				} else if (has_rule(GameConstants.GAME_RULE_WSK_GF_SEVEN_SHENG_DANG)
						&& _logic.have_pai_num(GRR._cards_data[i], GRR._card_count[i], 4) == 9 - wang_count - 1) {
					_current_player = GameConstants.INVALID_SEAT;
					break;
				} else if (has_rule(GameConstants.GAME_RULE_WSK_GF_J_SHENG_DANG)
						&& _logic.have_pai_num(GRR._cards_data[i], GRR._card_count[i], 8) == 9 - wang_count - 1) {
					_current_player = GameConstants.INVALID_SEAT;
					break;
				}
			}

			if (_current_player == GameConstants.INVALID_SEAT) {
				mian_da_lei_xing = 3;
				mianDa = true;
				GameSchedule.put(new GameFinishRunnable(getRoom_id(), GRR._banker_player, GameConstants.Game_End_DRAW),
						3, TimeUnit.SECONDS);
				return mianDa;
			}
		}
		return mianDa;
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

		int cards[] = new int[] { 1, 2, 40, 26, 38, 12, 2, 49, 40, 3, 24, 61, 56, 60, 18, 57, 59, 13, 23, 33, 55, 29,
				28, 6, 79, 79, 1, 42, 37, 37, 41, 52, 22, 13, 29, 27, 50, 35, 54, 36, 25, 7, 28, 78, 25, 53, 17, 44, 7,
				8, 39, 5, 19, 50, 51, 18, 45, 23, 45, 54, 43, 4, 10, 52, 9, 17, 9, 21, 53, 34, 33, 24, 44, 20, 11, 58,
				43, 8, 21, 26, 22, 34, 55, 12, 51, 78, 11, 6, 35, 41, 57, 5, 38, 27, 49, 60, 3, 42, 19, 4, 36, 59, 39,
				58, 56, 61, 20, 10 };
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
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
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

	/**
	 * 刷新队友牌
	 * 
	 * @param to_player
	 */
	public void Refresh_Dui_You_Card(int seat_index) {
		if (GRR._card_count[seat_index] != 0) {
			return;
		}
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_WSK_GF_REFRESH_DUIYOU_CARD);
		// 发送数据
		Refresh_Pai_GF.Builder refresh_card = Refresh_Pai_GF.newBuilder();
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
			if (_friend_seat[seat_index] == i) {
				refresh_card.setSeatIndex(i);
				for (int j = 0; j < this.GRR._card_count[i]; j++) {
					if (this.GRR._cards_data[i][j] > 0x100) {
						cards_card.addItem(this.GRR._cards_data[i][j] - 0x100);
					} else {
						cards_card.addItem(this.GRR._cards_data[i][j]);
					}
				}
				refresh_card.addCardsData(cards_card);
				refresh_card.setCardCount(GRR._card_count[i]);

				roomResponse.setCommResponse(PBUtil.toByteString(refresh_card));
				// 自己才有牌数据h
				this.send_response_to_player(seat_index, roomResponse);
			}
		}
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
