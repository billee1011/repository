package com.cai.game.wsk.handler.pcdz;

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
import com.cai.game.RoomUtil;
import com.cai.game.wsk.AbstractWSKTable;
import com.cai.game.wsk.WSKGameLogic_PC;
import com.cai.game.wsk.WSKType;
import com.cai.game.wsk.data.tagAnalyseIndexResult_WSK;
import com.cai.game.wsk.handler.pcdz.runnable.AutoPassRunnable;
import com.cai.service.MongoDBServiceImpl;
import com.cai.service.PlayerServiceImpl;

import protobuf.clazz.Protocol.GameEndResponse;
import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomPlayerResponse;
import protobuf.clazz.Protocol.RoomRequest;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.pcWsk.pcWskRsp.GameStart_Wsk_PC;
import protobuf.clazz.pcWsk.pcWskRsp.LiangPai_Result_Wsk_PC;
import protobuf.clazz.pcWsk.pcWskRsp.Opreate_RequestWsk_PC;
import protobuf.clazz.pcWsk.pcWskRsp.OutCardDataWsk_PC;
import protobuf.clazz.pcWsk.pcWskRsp.PukeGameEndWsk_PC;
import protobuf.clazz.pcWsk.pcWskRsp.RefreshCardData;
import protobuf.clazz.pcWsk.pcWskRsp.RefreshScore_Wsk_PC;
import protobuf.clazz.pcWsk.pcWskRsp.Refresh_Pai_PC;
import protobuf.clazz.pcWsk.pcWskRsp.TableResponse_Wsk_PC;
import protobuf.clazz.pcWsk.pcWskRsp.UserCardData;
import protobuf.clazz.pcWsk.pcWskRsp.ZhaDanData;
import protobuf.clazz.pcWsk.pcWskRsp.ZhaDanDataArray;

class OutCardItem {
	public int outCard[][] = new int[8][12];
	public int card_count[] = new int[8];
	public int award_score[] = new int[8];
	public int count;

	public OutCardItem() {

	}

}

public class WSKTable_PCDZ extends AbstractWSKTable {

	private static final long serialVersionUID = -2419887548488809773L;

	public int _get_score[];
	public int _is_call_banker[];

	public int _turn_have_score; // 当局得分
	public boolean _is_yi_da_san;
	public int _score_type;
	public int _fei_wang_card[][];
	public int _fei_wang_count[];
	public int _jiao_pai_card;
	public int _lose_num[];
	public int _win_num[];
	public int _win_lose_player[];
	public int _cur_end_count; // 结束的顺序
	public int _player_end[]; // 用户结束的顺序
	public int _end_score[]; // 结算分数
	public int _award_score[]; // 奖励分数
	public List<UserCardData.Builder> _user_data;
	protected static final int GAME_OPREATE_TYPE_LIANG_PAI = 1;
	protected static final int GAME_OPREATE_TYPE_CALL_BANKER = 2;
	protected static final int GAME_OPREATE_TYPE_CALL_BANKER_NO = 3;
	protected static final int GAME_OPREATE_TYPE_SORT_BY_CARD = 4;
	protected static final int GAME_OPREATE_TYPE_SORT_BY_COUNT = 5;
	protected static final int GAME_OPREATE_TYPE_SUBMIT_CARD = 6;

	public int sheng_dang_biaozhi; // 升档标志
	public int sort[];// 排序标志
	public int fa_wang[];// 开局是否已经计算过罚王
	public int mian_da_lei_xing;// 免打类型
	public int prev_out_palyer;// 上一轮出牌人
	public int _wang_count[];
	public int[] player_sort_card;
	public WSKGameLogic_PC _logic;
	public int _dan_kou_count[]; // 单扣次数
	public int _shuang_kou_count[]; // 双扣次数
	public int _da_du_count[]; // 打独次数
	public int _lose_win_count[]; // 输赢次数
	public OutCardItem _out_card_item[];
	public int _display_timer; // 显示时间
	public boolean _is_display_friend; // 显否显示庄家对家所有人可见
	public int _first_banker; // 初始庄家

	public WSKTable_PCDZ() {
		super(WSKType.GAME_TYPE_WSK_PC_ZD);
	}

	@Override
	protected void onInitTable() {

	}

	@Override
	public void on_init_table(int game_type_index, int game_rule_index, int game_round) {
		_logic = new WSKGameLogic_PC();
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
		_win_lose_player = new int[getTablePlayerNumber()];
		fa_wang = new int[getTablePlayerNumber()];
		_wang_count = new int[getTablePlayerNumber()];
		_player_end = new int[getTablePlayerNumber()];
		_dan_kou_count = new int[getTablePlayerNumber()]; // 单扣次数
		_shuang_kou_count = new int[getTablePlayerNumber()]; // 双扣次数
		_da_du_count = new int[getTablePlayerNumber()]; // 打独次数
		_lose_win_count = new int[getTablePlayerNumber()]; // 输赢次数
		_out_card_item = new OutCardItem[this.getTablePlayerNumber()];
		_end_score = new int[this.getTablePlayerNumber()];
		_award_score = new int[this.getTablePlayerNumber()];
		_game_type_index = game_type_index;//
		_game_rule_index = game_rule_index;
		_first_banker = -1;
		_user_data = new ArrayList<>();
		_player_result = new PlayerResult(this.getRoom_owner_account_id(), this.getRoom_id(), _game_type_index, _game_rule_index, _game_round,
				this.get_game_des(), this.getTablePlayerNumber());
		_logic.ruleMap = this.ruleMap;
		_score_type = GameConstants.WSK_ST_ORDER;
		_jiao_pai_card = GameConstants.INVALID_CARD;
		_handler_out_card_operate = new WSKHandlerOutCardOperate_PCDZ();
		_handler_call_banker = new WSKHandlerCallBnaker_PCDZ();
		_cur_end_count = 0;
		_display_timer = 15;
		_is_display_friend = false;
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_out_card_item[i] = new OutCardItem();
			_player_end[i] = 0;
			_friend_seat[i] = -1;
			_max_end_score[i] = 0;
			_wang_count[i] = 0;
			_user_data.add(UserCardData.newBuilder());
		}

	}

	@Override
	public void Refresh_user_get_score(int to_player) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		RefreshScore_Wsk_PC.Builder refresh_user_getscore = RefreshScore_Wsk_PC.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_WSK_PC_USER_GET_SCORE);
		Player rplayer;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			rplayer = this.get_players()[i];
			if (rplayer == null)
				continue;
			refresh_user_getscore.addUserGetScore(_get_score[i]);
			refresh_user_getscore.addXianQianScore(this._xi_qian_score[i] / 3);
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

		this._current_player = _cur_banker;
		_first_banker = _cur_banker;
		this._turn_out_card_count = 0;
		this.mian_da_lei_xing = 0;
		_cur_end_count = 0;
		_player_end = new int[getTablePlayerNumber()];
		_win_lose_player = new int[getTablePlayerNumber()];
		_end_score = new int[this.getTablePlayerNumber()];
		Arrays.fill(_turn_out_card_data, GameConstants.INVALID_CARD);
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_award_score[i] = 0;
			_out_card_item[i] = new OutCardItem();
			_end_score[i] = 0;
			_lose_win_count[i] = 0;
			_win_lose_player[i] = 0;
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
			_player_end[i] = 0;
			_wang_count[i] = 0;
			Arrays.fill(_cur_out_card_data[i], GameConstants.INVALID_CARD);
			Arrays.fill(_fei_wang_card[i], GameConstants.INVALID_CARD);
			_user_data.get(i).clear();
			sort[i] = GameConstants.WSK_ST_ORDER;
		}
		_is_display_friend = false;
		prev_out_palyer = -1;
		_score_type = GameConstants.WSK_ST_ORDER;
		_is_yi_da_san = false;
		_game_status = GameConstants.GS_PCWSK_CALLBANKER;
		_pai_score_count = 24;
		_pai_score = 200;
		_turn_have_score = 0;
		_jiao_pai_card = GameConstants.INVALID_CARD;
		Arrays.fill(_chuwan_shunxu, GameConstants.INVALID_SEAT);
		if (this.getRuleValue(GameConstants.GAME_RULE_WSK_PCZD_DA_XIAO_WANG) == 1) {
			_repertory_card = new int[GameConstants.CARD_COUNT_WSK_PC];
			shuffle(_repertory_card, GameConstants.CARD_DATA_WSK_PC);
		} else {
			_repertory_card = new int[GameConstants.CARD_COUNT_WSK_PC_NO_W];
			shuffle(_repertory_card, GameConstants.CARD_DATA_WSK_PC_NO_W);
		}

		if (DEBUG_CARDS_MODE || BACK_DEBUG_CARDS_MODE)
			test_cards();

		_liang_card_value = _repertory_card[RandomUtil.getRandomNumber(Integer.MAX_VALUE) % GameConstants.CARD_COUNT_WSK];
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

		// 庄家选择
		this.progress_banker_select();
		if (this._cur_round == 1 && this.getRuleValue(GameConstants.GAME_RULE_WSK_PCZD_NO_TRUSTEE) != 1) {
			if (this.getRuleValue(GameConstants.GAME_RULE_WSK_PCZD_60_TRUSTEE) == 1) {
				this.setPlayOutTime(60);
				_display_timer = 15;
			}
			if (this.getRuleValue(GameConstants.GAME_RULE_WSK_PCZD_120_TRUSTEE) == 1) {
				this.setPlayOutTime(120);
				_display_timer = 15;
			}
			enableRobot();
		}
		on_game_start();

		return true;
	}

	/// 洗牌
	@Override
	public void shuffle(int repertory_card[], int card_cards[]) {
		int xi_pai_count = 0;
		int rand = RandomUtil.generateRandomNumber(3, 6);

		while (xi_pai_count < 10 && xi_pai_count < rand) {
			if (xi_pai_count == 0)
				_logic.random_card_data(repertory_card, card_cards);
			else
				_logic.random_card_data(repertory_card, repertory_card);

			xi_pai_count++;
		}
		int count = this.getTablePlayerNumber();
		int index = 0;

		int is_xi_pai_again = 0;
		for (int i = 0; i < count; i++) {
			int hand_card[] = new int[get_hand_card_count_max()];
			for (int j = 0; j < this.get_hand_card_count_max(); j++) {
				hand_card[j] = repertory_card[index++];
			}
			tagAnalyseIndexResult_WSK hand_index = new tagAnalyseIndexResult_WSK();
			this._logic.AnalysebCardDataToIndex(hand_card, get_hand_card_count_max(), hand_index);
			for (int j = 0; j < this.get_hand_card_count_max();) {
				int card_index = this._logic.switch_card_to_idnex(hand_card[j]);
				if (hand_index.card_index[card_index] > 4) {
					is_xi_pai_again++;
				}
				if (hand_index.card_index[card_index] > 0) {
					j += hand_index.card_index[card_index];
				} else {
					j++;
				}
			}
			if (hand_index.card_index[13] + hand_index.card_index[14] == 4)
				is_xi_pai_again++;
			if (is_xi_pai_again >= 4) {
				break;
			}
		}
		if (is_xi_pai_again >= 4) {
			_logic.random_card_data(repertory_card, repertory_card);
		}

		index = 0;
		for (int j = 0; j < this.get_hand_card_count_max(); j++) {
			for (int i = 0; i < count; i++) {
				GRR._cards_data[i][j] = repertory_card[index++];

			}

		}
		for (int i = 0; i < count; i++) {
			GRR._card_count[i] = this.get_hand_card_count_max();
			_logic.SortCardList(GRR._cards_data[i], GRR._card_count[i], _score_type);
		}
		if (count == 2) {
			for (int j = 0; j < this.get_hand_card_count_max(); j++) {
				GRR._cards_data[2][j] = repertory_card[2 * this.get_hand_card_count_max() + j];
			}
		}
		GRR._left_card_count = 0;
		// 记录初始牌型
		_recordRoomRecord.setBeginArray(Arrays.toString(repertory_card));

		// int count = this.getTablePlayerNumber();
		// for (int i = 0; i < count; i++) {
		// for (int j = 0; j < this.get_hand_card_count_max(); j++) {
		// GRR._cards_data[i][j] = repertory_card[i *
		// this.get_hand_card_count_max() + j];
		// }
		// GRR._card_count[i] = this.get_hand_card_count_max();
		// _logic.SortCardList(GRR._cards_data[i], GRR._card_count[i],
		// _score_type);
		// }
		// // 记录初始牌型
		// _recordRoomRecord.setBeginArray(Arrays.toString(repertory_card));
	}

	@Override
	protected boolean on_game_start() {
		GRR._banker_player = _cur_banker;
		this._current_player = GRR._banker_player;
		_first_banker = _cur_banker;

		for (int play_index = 0; play_index < this.getTablePlayerNumber(); play_index++) {

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_WSK_PC_GAME_START);
			roomResponse.setGameStatus(this._game_status);
			roomResponse.setRoomInfo(getRoomInfo());
			// 发送数据
			GameStart_Wsk_PC.Builder gamestart = GameStart_Wsk_PC.newBuilder();
			gamestart.setRoomInfo(this.getRoomInfo());

			// gamestart.setCurBanker(GRR._banker_player);
			gamestart.setCurBanker(-1);

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
			gamestart.setDisplayTime(30);
			roomResponse.setCommResponse(PBUtil.toByteString(gamestart));

			// 自己才有牌数据
			this.send_response_to_player(play_index, roomResponse);
		}
		// 回放
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_WSK_PC_GAME_START);
		roomResponse.setGameStatus(this._game_status);
		roomResponse.setRoomInfo(this.getRoomInfo());
		// 发送数据
		GameStart_Wsk_PC.Builder gamestart = GameStart_Wsk_PC.newBuilder();
		gamestart.setRoomInfo(this.getRoomInfo());
		if (this._cur_round == 1) {
			this.load_player_info_data_game_start(gamestart);
		}
		this._current_player = GRR._banker_player;
		gamestart.setCurBanker(-1);

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			gamestart.addCardCount(GRR._card_count[i]);
			Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
			Int32ArrayResponse.Builder wang_cards_card = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < GRR._card_count[i]; j++) {
				cards_card.addItem(GRR._cards_data[i][j]);
			}

			gamestart.addCardsData(cards_card);
			gamestart.addFeiWang(wang_cards_card);
		}

		gamestart.setDisplayTime(30);
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
				|| reason == GameConstants.Game_End_RELEASE_RESULT || reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT || reason == GameConstants.Game_End_RELEASE_SYSTEM) {
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
		roomResponse.setType(MsgConstants.RESPONSE_WSK_PC_GAME_END);
		GameEndResponse.Builder game_end = GameEndResponse.newBuilder();
		PukeGameEndWsk_PC.Builder game_end_wsk_pc = PukeGameEndWsk_PC.newBuilder();
		game_end.setRoomInfo(getRoomInfo());
		game_end_wsk_pc.setRoomInfo(getRoomInfo());

		// 计算分数
		game_end.setGameRound(_game_round);
		game_end.setCurRound(_cur_round);
		game_end.setRoundOverType(0);
		if (GRR != null) {
			game_end.setRoundOverType(1);
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				game_end_wsk_pc.addCardCount(GRR._card_count[i]);
				Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GRR._card_count[i]; j++) {
					cards_card.addItem(GRR._cards_data[i][j]);
				}
				if (reason != GameConstants.Game_End_RELEASE_RESULT) {
					game_end_wsk_pc.addHandCardData(cards_card.build());
				} else {
					Int32ArrayResponse.Builder cards_card1 = Int32ArrayResponse.newBuilder();
					game_end_wsk_pc.addHandCardData(cards_card1.build());
				}
			}
			game_end.setStartTime(GRR._start_time);
			game_end.setGameTypeIndex(GRR._game_type_index);

			if (reason != GameConstants.Game_End_NORMAL) {
				for (int i = 0; i < getTablePlayerNumber(); i++) {
					tagAnalyseIndexResult_WSK hand_card_index = new tagAnalyseIndexResult_WSK();
					_logic.AnalysebCardDataToIndex(GRR._cards_data[i], GRR._card_count[i], hand_card_index);
					int wang_count = hand_card_index.card_index[14] + hand_card_index.card_index[13];
					int max_index = 13;
					for (int j = 0; j < GameConstants.WSK_MAX_INDEX; j++) {
						if (hand_card_index.card_index[j] >= 4) {
							if (hand_card_index.card_index[j] >= hand_card_index.card_index[max_index])
								max_index = j;

						}

					}
					if ((hand_card_index.card_index[max_index] < 6 || (hand_card_index.card_index[max_index] < 5 && max_index == 12))
							&& wang_count == 4) {
						int temp_count = _out_card_item[i].count;
						_out_card_item[i].outCard[temp_count][0] = 0x4e;
						_out_card_item[i].outCard[temp_count][1] = 0x4e;
						_out_card_item[i].outCard[temp_count][2] = 0x4f;
						_out_card_item[i].outCard[temp_count][3] = 0x4f;
						_out_card_item[i].card_count[temp_count] = wang_count;
						int type = _logic.GetCardType(_out_card_item[i].outCard[temp_count], wang_count);
						int xian_qian_score = _logic.GetCardXianScore(_out_card_item[i].outCard[temp_count], wang_count, type);
						_out_card_item[i].award_score[temp_count] = xian_qian_score;
						_out_card_item[i].count++;
						if (xian_qian_score > 0) {
							_xi_qian_times[i]++;
							_xi_qian_score[i] += xian_qian_score * (getTablePlayerNumber() - 1);

						}
					}
					for (int j = 0; j < GameConstants.WSK_MAX_INDEX; j++) {
						if (hand_card_index.card_index[j] >= 4) {
							int cards_data[] = new int[12];
							int card_count = 0;
							for (int k = 0; k < hand_card_index.card_index[j]; k++) {
								cards_data[card_count++] = hand_card_index.card_data[j][k];
							}
							if (max_index == j
									&& !((hand_card_index.card_index[max_index] < 6 || (hand_card_index.card_index[max_index] < 5 && max_index == 12))
											&& wang_count == 4)) {
								for (int k = 0; k < hand_card_index.card_index[13]; k++) {
									cards_data[card_count++] = hand_card_index.card_data[13][k];
								}
								for (int k = 0; k < hand_card_index.card_index[14]; k++) {
									cards_data[card_count++] = hand_card_index.card_data[14][k];
								}
							}
							int type = _logic.GetCardType(cards_data, card_count);
							int xian_qian_score = _logic.GetCardXianScore(cards_data, card_count, type);
							if (xian_qian_score > 0) {
								int temp_count = _out_card_item[i].count;
								for (int k = 0; k < card_count; k++) {
									_out_card_item[i].outCard[temp_count][k] = cards_data[k];
								}
								_out_card_item[i].card_count[temp_count] = card_count;
								_out_card_item[i].award_score[temp_count] = xian_qian_score;
								_out_card_item[i].count++;
							}
							if (xian_qian_score > 0) {
								_xi_qian_times[i]++;
								_xi_qian_score[i] += xian_qian_score * (getTablePlayerNumber() - 1);

							}

						}

					}
				}
				for (int i = 0; i < this.getTablePlayerNumber(); i++) {
					if (!_logic.have_card_num(GRR._cards_data[i], GRR._card_count[i], 4)) {
						for (int j = 0; j < GRR._card_count[i]; j++) {
							if (GRR._cards_data[i][j] == 0x4E || GRR._cards_data[i][j] == 0x4F || GRR._cards_data[i][j] == 0x4E + 0x100
									|| GRR._cards_data[i][j] == 0x4F + 0x100) {
								_fei_wang_card[i][_fei_wang_count[i]++] = GRR._cards_data[i][j];
							}
						}
					}
				}
				for (int i = 0; i < this.getTablePlayerNumber(); i++) {
					if (this._fei_wang_count[i] > 0) {
						for (int j = 0; j < this.getTablePlayerNumber(); j++) {
							if (j == i)
								continue;
							this._wang_count[j] += this._fei_wang_count[i];
							GRR._game_score[j] += this._fei_wang_count[i];
						}
						GRR._game_score[i] -= this._fei_wang_count[i] * (this.getTablePlayerNumber() - 1);
						this._wang_count[i] -= this._fei_wang_count[i] * (this.getTablePlayerNumber() - 1);
					}
				}
				for (int i = 0; i < this.getTablePlayerNumber(); i++) {

					this._award_score[i] += this._xi_qian_score[i];
					for (int j = 0; j < this.getTablePlayerNumber(); j++) {
						if (i == j)
							continue;
						this._award_score[j] -= this._xi_qian_score[i] / 3;
					}

				}
				for (int i = 0; i < this.getTablePlayerNumber(); i++) {
					_xi_qian_total_score[i] += this._award_score[i];
					GRR._game_score[i] += this._award_score[i];
					this._player_result.game_score[i] += GRR._game_score[i];
				}

			}
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				game_end_wsk_pc.addEndScore((int) GRR._game_score[i]);
				game_end_wsk_pc.addWinOrder(this._chuwan_shunxu[i]);
				game_end_wsk_pc.addPickupScore(this._get_score[i]);
				game_end_wsk_pc.addFaScore(this._wang_count[i]);
				game_end_wsk_pc.addBoomAward(this._award_score[i]);
				game_end_wsk_pc.addGameScore(this._end_score[i]);
				game_end.addGameScore((int) GRR._game_score[i]);
				if (istrustee[i] == true)
					handler_request_trustee(i, false, 0);
			}
			game_end.setStartTime(GRR._start_time);
			game_end.setGameTypeIndex(GRR._game_type_index);
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				ZhaDanDataArray.Builder zha_dan_array = ZhaDanDataArray.newBuilder();
				if (this._out_card_item[i].count > 0) {
					for (int j = 0; j < this._out_card_item[i].count; j++) {
						ZhaDanData.Builder zha_dan_item = ZhaDanData.newBuilder();
						for (int k = 0; k < _out_card_item[i].card_count[j]; k++) {
							zha_dan_item.addCardsData(_out_card_item[i].outCard[j][k]);
						}
						zha_dan_item.setAwardScore(_out_card_item[i].award_score[j]);
						zha_dan_array.addZhaDanItem(zha_dan_item);
					}
				}
				game_end_wsk_pc.addZhaDanItem(zha_dan_array);
			}
			this.load_player_info_data_game_end(game_end_wsk_pc);

		}

		boolean end = false;
		if (reason == GameConstants.Game_End_NORMAL || reason == GameConstants.Game_End_DRAW) {
			if (_cur_round >= _game_round) {// 局数到了
				game_end.setRoomOverType(1);
				end = true;
				for (int i = 0; i < getTablePlayerNumber(); i++) {
					game_end_wsk_pc.addAllEndScore((int) this._player_result.game_score[i]);
					game_end_wsk_pc.addAllRewardScore(_xi_qian_total_score[i]);
					game_end_wsk_pc.addAllDaDuNum(this._da_du_count[i]);
					game_end_wsk_pc.addAllShuangKouNum(this._shuang_kou_count[i]);
					game_end_wsk_pc.addAllWinNum(this._win_num[i]);

				}
				game_end.setPlayerResult(this.process_player_result(reason));
				real_reason = GameConstants.Game_End_ROUND_OVER;
			}
		} else if (reason == GameConstants.Game_End_RELEASE_PLAY || reason == GameConstants.Game_End_RELEASE_NO_BEGIN
				|| reason == GameConstants.Game_End_RELEASE_RESULT || reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT || reason == GameConstants.Game_End_RELEASE_SYSTEM) {
			end = true;
			game_end.setRoomOverType(1);
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				game_end_wsk_pc.addAllEndScore((int) this._player_result.game_score[i]);
				game_end_wsk_pc.addAllRewardScore(_xi_qian_total_score[i]);
				game_end_wsk_pc.addAllDaDuNum(this._da_du_count[i]);
				game_end_wsk_pc.addAllShuangKouNum(this._shuang_kou_count[i]);
				game_end_wsk_pc.addAllWinNum(this._win_num[i]);

			}

			game_end.setPlayerResult(this.process_player_result(reason));
			real_reason = GameConstants.Game_End_RELEASE_PLAY;
		}
		// game_end_wsk_PC.setMianDa(mian_da_lei_xing);
		game_end_wsk_pc.setReason(real_reason);
		////////////////////////////////////////////////////////////////////// 得分总的
		game_end.setEndType(real_reason);
		game_end.setGameRound(_game_round);
		game_end.setCurRound(_cur_round);
		game_end.setCommResponse(PBUtil.toByteString(game_end_wsk_pc));

		game_end.setEndTime(System.currentTimeMillis() / 1000L);// 结束时间

		roomResponse.setGameEnd(game_end);
		this.send_response_to_room(roomResponse);

		record_game_round(game_end, real_reason);
		this._cur_banker = this._chuwan_shunxu[0];
		// 超时解散
		if (reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT || reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT) {
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
		// 错误断言
		return false;
	}

	public boolean check_pass_next_player(int current_player, int turn_out_card_count, int turn_out_card_data[], int turn_out_card_type) {
		if (current_player == -1)
			return false;
		if (turn_out_card_count == 0)
			return false;
		int card_count = GRR._card_count[current_player];
		switch (card_count) {
		case 5: {
			if (turn_out_card_type > GameConstants.WSK_PC_CT_BOMB_5
					|| (turn_out_card_type == GameConstants.WSK_PC_CT_BOMB_5 && _logic.GetCardValue(turn_out_card_data[0]) == 2))
				return true;
			return false;
		}
		case 4: {
			if (turn_out_card_type > GameConstants.WSK_PC_CT_BOMB_4
					|| (turn_out_card_type == GameConstants.WSK_PC_CT_BOMB_4 && _logic.GetCardValue(turn_out_card_data[0]) == 2))
				return true;
			return false;
		}
		case 3: {
			if (turn_out_card_type > GameConstants.WSK_PC_CT_DOUBLE_LINK)
				return true;
			return false;
		}
		case 2: {
			if (turn_out_card_type > GameConstants.WSK_PC_CT_DOUBLE || _logic.GetCardLogicValue(turn_out_card_data[0]) == 17)
				return true;
			return false;
		}
		case 1: {
			if (turn_out_card_type > GameConstants.WSK_PC_CT_SINGLE || _logic.GetCardLogicValue(turn_out_card_data[0]) == 17)
				return true;
			return false;
		}
		}
		return false;
	}

	public void caculate_score() {
		boolean is_da_du = false;
		boolean is_dan_kou = false;
		boolean is_shuang_kou = false;
		if (this._is_yi_da_san) {
			is_da_du = true;
			if (this._chuwan_shunxu[0] == GRR._banker_player) {

				this._da_du_count[this._chuwan_shunxu[0]]++;

				for (int i = 0; i < this.getTablePlayerNumber(); i++) {
					if (i == GRR._banker_player)
						continue;
					this._out_card_item[i].count = 0;
				}
				this._award_score[GRR._banker_player] = this._xi_qian_score[GRR._banker_player];
				for (int i = 0; i < this.getTablePlayerNumber(); i++) {
					if (i == GRR._banker_player)
						continue;
					this._award_score[i] -= this._xi_qian_score[GRR._banker_player] / 3;

				}

				for (int i = 0; i < this.getTablePlayerNumber(); i++) {
					if (i == GRR._banker_player)
						continue;
					this._win_lose_player[i] = -1;
					_end_score[i] = -3;
				}
				_end_score[GRR._banker_player] = (this.getTablePlayerNumber() - 1) * 3;
				this._win_lose_player[this._chuwan_shunxu[0]] = 1;
				for (int i = 0; i < this.getTablePlayerNumber(); i++) {
					GRR._game_score[i] = _end_score[i] + this._award_score[i];
				}

				this._win_num[GRR._banker_player]++;
			} else {

				this._xi_qian_score[GRR._banker_player] = 0;
				this._out_card_item[GRR._banker_player].count = 0;
				for (int i = 0; i < this.getTablePlayerNumber(); i++) {
					if (i == GRR._banker_player)
						continue;
					this._award_score[i] += this._xi_qian_score[i];
					this._award_score[GRR._banker_player] -= this._xi_qian_score[i];
				}

				this._win_lose_player[GRR._banker_player] = -1;
				for (int i = 0; i < this.getTablePlayerNumber(); i++) {
					if (i == GRR._banker_player)
						continue;
					this._win_lose_player[i] = 1;
					_end_score[i] = 3;
					this._win_num[i]++;
				}
				_end_score[GRR._banker_player] = -(this.getTablePlayerNumber() - 1) * 3;
				this._win_lose_player[GRR._banker_player] = -1;
				for (int i = 0; i < this.getTablePlayerNumber(); i++) {
					GRR._game_score[i] = _end_score[i] + this._award_score[i];
				}

			}
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				if (getRuleValue(GameConstants.GAME_RULE_WSK_PCZD_DDNSJ) == 1)
					continue;
				if (this._fei_wang_count[i] > 0) {
					for (int j = 0; j < this.getTablePlayerNumber(); j++) {
						if (j == i)
							continue;
						this._wang_count[j] += this._fei_wang_count[i];
						GRR._game_score[j] += this._fei_wang_count[i];
					}
					GRR._game_score[i] -= this._fei_wang_count[i] * (this.getTablePlayerNumber() - 1);
					this._wang_count[i] -= this._fei_wang_count[i] * (this.getTablePlayerNumber() - 1);
				}
			}

		} else {
			if (this._friend_seat[this._chuwan_shunxu[0]] == this._chuwan_shunxu[1]) {
				is_shuang_kou = true;
				this._win_lose_player[this._chuwan_shunxu[0]] = 1;
				this._win_lose_player[this._chuwan_shunxu[1]] = 1;
				this._win_lose_player[this._chuwan_shunxu[2]] = -1;
				this._win_lose_player[this._chuwan_shunxu[3]] = -1;
				this._shuang_kou_count[this._chuwan_shunxu[0]]++;
				this._shuang_kou_count[this._chuwan_shunxu[1]]++;
			} else if (this._friend_seat[this._chuwan_shunxu[0]] == this._chuwan_shunxu[2]) {
				is_dan_kou = true;
				if (this._get_score[this._chuwan_shunxu[1]] >= 105) {
					this._win_lose_player[this._chuwan_shunxu[1]] = 1;
					this._win_lose_player[this._chuwan_shunxu[3]] = 1;
					this._win_lose_player[this._chuwan_shunxu[0]] = -1;
					this._win_lose_player[this._chuwan_shunxu[2]] = -1;
				} else {
					this._win_lose_player[this._chuwan_shunxu[1]] = -1;
					this._win_lose_player[this._chuwan_shunxu[3]] = -1;
					this._win_lose_player[this._chuwan_shunxu[0]] = 1;
					this._win_lose_player[this._chuwan_shunxu[2]] = 1;
				}
			} else if (this._friend_seat[this._chuwan_shunxu[0]] == this._chuwan_shunxu[3]) {
				is_dan_kou = true;
				if (this._get_score[this._chuwan_shunxu[0]] + this._get_score[this._chuwan_shunxu[3]] >= 100) {
					this._win_lose_player[this._chuwan_shunxu[0]] = 1;
					this._win_lose_player[this._chuwan_shunxu[3]] = 1;
					this._win_lose_player[this._chuwan_shunxu[1]] = -1;
					this._win_lose_player[this._chuwan_shunxu[2]] = -1;
				} else {
					this._win_lose_player[this._chuwan_shunxu[0]] = -1;
					this._win_lose_player[this._chuwan_shunxu[3]] = -1;
					this._win_lose_player[this._chuwan_shunxu[1]] = 1;
					this._win_lose_player[this._chuwan_shunxu[2]] = 1;
				}

			}
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				if (this._win_lose_player[i] == 1)
					this._win_num[i]++;
				this._award_score[i] += this._xi_qian_score[i];
				for (int j = 0; j < this.getTablePlayerNumber(); j++) {
					if (i == j)
						continue;
					this._award_score[j] -= this._xi_qian_score[i] / 3;
				}

			}
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				if (is_shuang_kou) {
					GRR._game_score[i] += 2 * this._win_lose_player[i] + this._award_score[i];
					_end_score[i] = 2 * this._win_lose_player[i];
				} else {

					GRR._game_score[i] += 1 * this._win_lose_player[i] + this._award_score[i];
					_end_score[i] = 1 * this._win_lose_player[i];
				}
			}
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				if (this._fei_wang_count[i] > 0) {
					for (int j = 0; j < this.getTablePlayerNumber(); j++) {
						if (j == i)
							continue;
						this._wang_count[j] += this._fei_wang_count[i];
						GRR._game_score[j] += this._fei_wang_count[i];
					}
					GRR._game_score[i] -= this._fei_wang_count[i] * (this.getTablePlayerNumber() - 1);
					this._wang_count[i] -= this._fei_wang_count[i] * (this.getTablePlayerNumber() - 1);
				}
			}
		}

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_player_result.game_score[i] += GRR._game_score[i];
			this._xi_qian_total_score[i] += this._award_score[i];
		}
		return;
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
			int score = _logic.GetHandCardXianScore(GRR._cards_data[i], GRR._card_count[i], sheng_dang_biaozhi, _user_data.get(i));
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

	public void load_player_info_data_game_start(GameStart_Wsk_PC.Builder roomResponse) {
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

	public void load_player_info_data_reconnect(TableResponse_Wsk_PC.Builder roomResponse) {
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

	public void load_player_info_data_game_end(PukeGameEndWsk_PC.Builder roomResponse) {
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
			room_player.setBiaoyan(_player_result.biaoyan[i]);
			if (rplayer.locationInfor != null) {
				room_player.setLocationInfor(rplayer.locationInfor);
			}
			roomResponse.addPlayers(room_player);
		}
	}

	@Override
	public boolean operate_out_card(int seat_index, int count, int cards_data[], int type, int to_player, boolean is_deal) {
		// if (seat_index == GameConstants.INVALID_SEAT) {
		// return false;
		// }
		//// 发送玩家出牌 201 =玩家出牌(CMD_OutCard: operate_player,operate_card)

		if (_is_yi_da_san) {
			_jiao_pai_card = -1;
		}
		if (to_player == GameConstants.INVALID_SEAT) {
			for (int index = 0; index < this.getTablePlayerNumber(); index++) {
				RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
				OutCardDataWsk_PC.Builder outcarddata = OutCardDataWsk_PC.newBuilder();
				// Int32ArrayResponse.Builder cards =
				// Int32ArrayResponse.newBuilder();
				roomResponse.setType(MsgConstants.RESPONSE_WSK_PC_OUT_CARD);// 201
				roomResponse.setTarget(seat_index);

				for (int j = 0; j < count; j++) {
					outcarddata.addCardsData(cards_data[j]);
				}
				// 上一出牌数据
				outcarddata.setPrCardsCount(this._turn_out_card_count);
				for (int i = 0; i < this._turn_out_card_count; i++) {
					outcarddata.addPrCardsData(this._turn_out_card_data[i]);
				}
				outcarddata.setCurBanker(this._cur_banker);
				outcarddata.setCardsCount(count);
				outcarddata.setOutCardPlayer(seat_index);
				outcarddata.setCardType(type);
				outcarddata.setCurPlayer(this._current_player);
				outcarddata.setDisplayTime(_display_timer);
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

				// 出牌提示
				if (index == this._current_player) {
					int tip_out_card[][] = new int[GRR._card_count[index] * 2][GRR._card_count[index]];
					int tip_out_count[] = new int[GRR._card_count[index] * 2];
					int tip_type_count = 0;
					int trun_out_card_data[] = new int[this._turn_out_card_count];
					for (int i = 0; i < this._turn_out_card_count; i++) {
						trun_out_card_data[i] = this._turn_out_card_data[i];
					}
					int card_data[] = new int[GRR._card_count[index]];
					for (int i = 0; i < GRR._card_count[index]; i++) {
						card_data[i] = GRR._cards_data[index][i];
					}
					tip_type_count = this._logic.search_out_card(GRR._cards_data[index], GRR._card_count[index], trun_out_card_data,
							this._turn_out_card_count, this._turn_three_link_num, tip_out_card, tip_out_count, tip_type_count);
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
				if (this._is_display_friend == false && this._is_yi_da_san == false) {
					if (index != this._friend_seat[this._cur_banker]) {
						outcarddata.clearFriendSeat();
						for (int i = 0; i < this.getTablePlayerNumber(); i++) {
							outcarddata.addFriendSeat(0);
						}
					}
				}
				roomResponse.setCommResponse(PBUtil.toByteString(outcarddata));

				this.send_response_to_player(index, roomResponse);

			}
			boolean is_pass = check_pass_next_player(this._current_player, this._turn_out_card_count, this._turn_out_card_data,
					this._turn_out_card_type);
			if (is_pass) {
				GameSchedule.put(new AutoPassRunnable(getRoom_id(), this._current_player), 1000, TimeUnit.MILLISECONDS);
			}
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			OutCardDataWsk_PC.Builder outcarddata = OutCardDataWsk_PC.newBuilder();
			// Int32ArrayResponse.Builder cards =
			// Int32ArrayResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_WSK_PC_OUT_CARD);// 201
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
			outcarddata.setDisplayTime(_display_timer);
			outcarddata.setPrOutCardType(_turn_out_card_type);
			outcarddata.setCurBanker(this._cur_banker);
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
				if (i == _cur_banker || i == _friend_seat[_cur_banker]) {
					outcarddata.addFriendSeat(_jiao_pai_card);
				} else {
					outcarddata.addFriendSeat(0);
				}

			}
			outcarddata.setLiangPai(_jiao_pai_card);
			roomResponse.setCommResponse(PBUtil.toByteString(outcarddata));
			GRR.add_room_response(roomResponse);
		} else {
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			OutCardDataWsk_PC.Builder outcarddata = OutCardDataWsk_PC.newBuilder();
			// Int32ArrayResponse.Builder cards =
			// Int32ArrayResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_WSK_PC_OUT_CARD);// 201
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
			outcarddata.setDisplayTime(_display_timer);
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
				tip_type_count = this._logic.search_out_card(GRR._cards_data[to_player], GRR._card_count[to_player], this._turn_out_card_data,
						this._turn_out_card_count, this._turn_three_link_num, tip_out_card, tip_out_count, tip_type_count);
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
				if (i == to_player || (to_player == (i + 2) % getTablePlayerNumber() && GRR._card_count[to_player] == 0)) {
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
			outcarddata.setCurBanker(this._cur_banker);
			outcarddata.setLiangPai(_jiao_pai_card);
			if (this._is_display_friend == false && this._is_yi_da_san == false) {
				if (to_player != this._friend_seat[this._cur_banker]) {
					outcarddata.clearFriendSeat();
					for (int i = 0; i < this.getTablePlayerNumber(); i++) {
						outcarddata.addFriendSeat(0);
					}
				}
			}
			roomResponse.setCommResponse(PBUtil.toByteString(outcarddata));

			this.send_response_to_player(to_player, roomResponse);

		}

		return true;
	}

	@Override

	public boolean handler_request_trustee(int get_seat_index, boolean isTrustee, int Trustee_type) {
		if (this.getRuleValue(GameConstants.GAME_RULE_WSK_PCZD_NO_TRUSTEE) == 1)
			return false;
		if (_playerStatus == null || istrustee == null)
			return false;
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_IS_TRUSTEE);
		roomResponse.setOperatePlayer(get_seat_index);
		roomResponse.setIstrustee(isTrustee);
		// if (isTrustee && (!is_sys())) {
		// roomResponse.setIstrustee(false);
		// send_response_to_player(get_seat_index, roomResponse);
		// return false;
		// }
		//

		// 金币场 游戏开始前 无法托管
		// if ((_game_status == GameConstants.GS_MJ_FREE || _game_status ==
		// GameConstants.GS_MJ_WAIT)&&isTrustee == true) {
		// send_error_notify(get_seat_index, 1, "游戏还未开始,无法托管!");
		// return false;
		// }
		// if ((!(_game_status == GameConstants.GS_MJ_WAIT || _game_status ==
		// GameConstants.GS_MJ_FREE)) && isTrustee == false) {
		// send_error_notify(get_seat_index, 2, "托管将在本轮结束后取消!");
		// return false;
		// }

		istrustee[get_seat_index] = isTrustee;

		if (GRR != null) {
			GRR.add_room_response(roomResponse);
		}

		this.send_response_to_room(roomResponse);

		return false;
	}

	@Override
	public boolean handler_requst_message_deal(Player player, int seat_index, RoomRequest room_rq, int type) {
		if (type == MsgConstants.REQUST_PC_WSK_OPERATE) {
			Opreate_RequestWsk_PC req = PBUtil.toObject(room_rq, Opreate_RequestWsk_PC.class);
			// 逻辑处理
			return handler_requst_opreate(seat_index, req.getOpreateType(), req.getCardData(), req.getSortCardList());
		}
		return true;
	}

	public boolean handler_requst_opreate(int seat_index, int opreate_type, int card_data, List<Integer> list) {
		switch (opreate_type) {
		case GAME_OPREATE_TYPE_LIANG_PAI: {
			deal_liang_pai(seat_index, card_data);
			return true;
		}
		case GAME_OPREATE_TYPE_CALL_BANKER: {
			this._handler_call_banker.handler_call_banker(this, seat_index, 1);
			return true;
		}
		case GAME_OPREATE_TYPE_CALL_BANKER_NO: {
			this._handler_call_banker.handler_call_banker(this, seat_index, 0);
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

	public void deal_sort_card_by_data(int seat_index, List<Integer> list) {
		int out_cards[] = new int[list.size()];
		for (int i = 0; i < list.size(); i++) {
			out_cards[i] = list.get(i);
		}
		// for (int i = 0; i < list.size(); i++) {
		// for (int j = 0; j < this.GRR._card_count[seat_index]; j++) {
		// if (GRR._cards_data[seat_index][j] > 0x100 &&
		// GRR._cards_data[seat_index][j] == out_cards[i]) {
		// GRR._cards_data[seat_index][j] -= 0x100;
		// }
		// }
		// }

		int flag = (player_sort_card[seat_index] + 1) & 0xf << 8;
		flag = ++player_sort_card[seat_index] & 0xF;
		flag = flag << 8;
		boolean flag_lost = true;
		for (int i = 0; i < list.size(); i++) {
			for (int j = 0; j < this.GRR._card_count[seat_index]; j++) {
				if (GRR._cards_data[seat_index][j] == out_cards[i]) {
					if (GRR._cards_data[seat_index][j] > 0x100) {
						GRR._cards_data[seat_index][j] = GRR._cards_data[seat_index][j] & 0xFF;
					} else {
						flag_lost = false;
						GRR._cards_data[seat_index][j] += flag;
					}
					break;
				}
			}
		}
		if (flag_lost) {
			player_sort_card[seat_index]--;
		}
		// int _score_type = sort[seat_index];
		// if (_score_type == GameConstants.WSK_ST_COUNT) {
		// _score_type = GameConstants.WSK_ST_ORDER;
		// } else if (_score_type == GameConstants.WSK_ST_ORDER || _score_type
		// == GameConstants.WSK_ST_CUSTOM) {
		// _score_type = GameConstants.WSK_ST_COUNT;
		// }
		// sort[seat_index] = _score_type;
		_score_type = GameConstants.WSK_ST_CUSTOM;
		this._logic.SortCardList(GRR._cards_data[seat_index], GRR._card_count[seat_index], _score_type);
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
		player_sort_card[seat_index] = 0;
		if (GRR == null)
			return;
		for (int j = 0; j < this.GRR._card_count[seat_index]; j++) {
			if (GRR._cards_data[seat_index][j] > 0x100) {
				GRR._cards_data[seat_index][j] = GRR._cards_data[seat_index][j] & 0xFF;
			}
		}
		this._logic.SortCardList(GRR._cards_data[seat_index], GRR._card_count[seat_index], _score_type);
		RefreshCard(seat_index);
	}

	public void deal_sort_card_by_count(int seat_index, int score_type, int card_data, int card_count) {

		this._logic.SortCardList(GRR._cards_data[seat_index], GRR._card_count[seat_index], score_type);
		RefreshCard(seat_index);
	}

	public void RefreshCard(int to_player) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_WSK_PC_REFRESH_CARD);
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

	public void deal_liang_pai(int seat_index, int card_data) {
		if (this._game_status != GameConstants.GS_PCWSK_LIANG_PAI || seat_index != this.GRR._banker_player) {
			return;
		}
		int temp_card_data = card_data;
		if (temp_card_data > 0x100) {
			temp_card_data -= 0x100;
		}

		int loop = 0;

		while (loop < this.getTablePlayerNumber()) {

			int cards[] = new int[GameConstants.CARD_COUNT_WSK_PC_MAX];
			_cur_banker = (seat_index + loop) % this.getTablePlayerNumber();
			temp_card_data = 0;
			for (int i = 15; i > 2; i--) {
				int card_count = _logic.get_cards(this.GRR._cards_data[_cur_banker], GRR._card_count[_cur_banker], cards, i);
				int color_index[] = new int[4];
				for (int j = 0; j < card_count; j++) {
					if (cards[j] != 0)
						color_index[_logic.GetCardColor(cards[j])]++;
				}
				int card_index[] = new int[4];
				int card_index_count = 0;
				for (int j = 0; j < 4; j++) {
					if (color_index[j] == 1)
						card_index[card_index_count++] = j;
				}
				int value = i > 13 ? i - 13 : i;
				if (card_index_count > 0) {
					int temp_index = (RandomUtil.generateRandomNumber(0, card_index_count - 1));
					temp_card_data = (card_index[temp_index] << 4) + value;
					break;
				}
			}
			if (temp_card_data != 0)
				break;
			loop++;
		}

		// 不能叫大小王
		if (_logic.GetCardColor(temp_card_data) == 0x40) {
			send_error_notify(seat_index, 2, "请选择正确的牌型!");
			return;
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_WSK_PC_LIANG_PAI_RESULT);
		// 发送数据
		LiangPai_Result_Wsk_PC.Builder liang_pai_result = LiangPai_Result_Wsk_PC.newBuilder();
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
		_jiao_pai_card = temp_card_data;

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
		liang_pai_result.setCardData(_jiao_pai_card);
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			liang_pai_result.clearSeatIndex();

			if (i == this._friend_seat[this._cur_banker]) {

				liang_pai_result.addSeatIndex(seat_index);
				liang_pai_result.addSeatIndex(other_seat_index);
			} else {
				liang_pai_result.addSeatIndex(-1);
				liang_pai_result.addSeatIndex(-1);
			}
			roomResponse.setCommResponse(PBUtil.toByteString(liang_pai_result));
			GRR.add_room_response(roomResponse);

			this.send_response_to_player(i, roomResponse);
		}

		this._game_status = GameConstants.GS_PCWSK_PLAY;
		this.operate_out_card(GameConstants.INVALID_SEAT, 0, null, GameConstants.WSK_PC_CT_ERROR, GameConstants.INVALID_SEAT, false);

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
	public boolean handler_operate_out_card_mul(int get_seat_index, List<Integer> list, int card_count, int b_out_card, String desc) {
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

	public boolean set_timer(int timer_type, int time) {

		_cur_game_timer = timer_type;
		if (_game_scheduled != null)
			this.kill_timer();
		if (time == 0) {
			return true;
		}
		_game_scheduled = GameSchedule.put(new AnimationRunnable(getRoom_id(), timer_type), time * 1000, TimeUnit.MILLISECONDS);
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
		// if (!zi_dong_tou_xiang) {
		// return false;
		// }
		// switch (timer_id) {
		// case TIME_OUT_NO_TOU_XIANG:
		// boolean is_beigin = true;
		// // for (int i = 0; i < getPlayerCount(); i++) {
		// // if (_is_tou_xiang[i] == 0 && _is_tou_xiang_agree[i] == -1) {
		// // is_beigin = true;
		// // }
		// // }
		// if (is_beigin) {
		// this._game_status = GameConstants.GS_PCWSK_PLAY;
		// operate_out_card(GameConstants.INVALID_SEAT, 0, null,
		// GameConstants.WSK_PC_CT_ERROR, GameConstants.INVALID_SEAT, false);
		// }
		// break;
		//
		// default:
		// break;
		// }
		return false;
	}

	@Override
	protected void test_cards() {
		// int cards[] = new int[] { 0x0d, 0x1d, 0x2d, 0x3D, 0x0D, 0x1D, 0x2D,
		// 0x4E, 0x4F, 0x06, 0x16, 0x26, 0x36, 0x07, 0x07, 0x07 };
		// int cards[] = new int[] { 0x02, 0x12, 0x22, 0x32, 0x02, 0x12, 0x22,
		// 0x01, 0x4F, 0x07, 0x17, 0x27, 0x37, 0x07, 0x17, 0x27, 0x01, 0x09,
		// 0x0a, 0x1a, 0x2a, 0x3a, 0x0a, 0x1a, 0x2a, 0x01, 0x19, };

		int cards[] = new int[] { 27, 2, 36, 33, 58, 25, 29, 11, 38, 54, 59, 59, 6, 56, 21, 28, 3, 57, 52, 22, 13, 4, 45, 8, 37, 54, 1, 42, 20, 28, 6,
				40, 55, 21, 12, 55, 43, 51, 10, 57, 40, 60, 58, 33, 19, 5, 35, 24, 29, 61, 5, 23, 44, 50, 56, 8, 53, 36, 11, 22, 23, 17, 4, 39, 50,
				18, 12, 42, 24, 27, 37, 13, 34, 18, 61, 44, 51, 43, 35, 3, 39, 49, 26, 41, 19, 78, 53, 9, 52, 34, 26, 79, 38, 7, 78, 2, 1, 20, 25, 7,
				9, 60, 10, 41, 79, 17, 45, 49 };

		int cards0[] = new int[27];
		int cards1[] = new int[27];
		int cards2[] = new int[27];
		int cards3[] = new int[27];
		for (int i = 0; i < this.get_hand_card_count_max(); i++) {
			cards0[i] = cards[i];
		}
		for (int i = 0; i < this.get_hand_card_count_max(); i++) {
			cards1[i] = cards[i + 27];
		}
		for (int i = 0; i < this.get_hand_card_count_max(); i++) {
			cards2[i] = cards[i + 54];
		}
		for (int i = 0; i < this.get_hand_card_count_max(); i++) {
			cards3[i] = cards[i + 81];
		}

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			for (int j = 0; j < get_hand_card_count_max(); j++) {
				GRR._cards_data[i][j] = 0;
			}
		}
		int index = 0;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			for (int j = 0; j < get_hand_card_count_max(); j++) {
				if (i == 0) {

					GRR._cards_data[i][j] = cards0[j];
				}
				if (i == 1) {

					GRR._cards_data[i][j] = cards1[j];
				}
				if (i == 2) {

					GRR._cards_data[i][j] = cards2[j];
				}
				if (i == 3) {
					GRR._cards_data[i][j] = cards3[j];
				}
			}
		}
		/********
		 * 下面这个给测试用的 如果开发用 把上面的注释去掉 realyCards
		 **************************/

		BACK_DEBUG_CARDS_MODE = true;
		// debug_my_cards = new int[] { 37, 34, 60, 4, 58, 18, 27, 8, 22, 22,
		// 17, 24, 49, 20, 43, 42, 28, 10, 40, 78, 11, 2, 1, 13, 23, 59, 9, 45,
		// 53, 9,
		// 57, 45, 12, 39, 54, 35, 6, 44, 28, 49, 54, 21, 26, 51, 12, 3, 20, 13,
		// 26, 3, 40, 7, 57, 24, 1, 38, 52, 61, 55, 23, 7, 10, 4, 42, 2,
		// 17, 79, 53, 44, 61, 36, 27, 38, 58, 18, 50, 41, 55, 25, 79, 35, 11,
		// 50, 43, 29, 29, 36, 39, 5, 5, 37, 60, 19, 8, 6, 33, 59, 25, 21,
		// 56, 19, 56, 78, 33, 51, 52, 34, 41 };

		int card_count = 27;
		if (this.getRuleValue(GameConstants.GAME_RULE_WSK_PCZD_NO_DXW) == 1)
			card_count = 26;
		if (BACK_DEBUG_CARDS_MODE) {
			if (debug_my_cards != null) {
				if (debug_my_cards.length > card_count) {
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

	@Override
	public int get_hand_card_count_max() {
		if (this.getRuleValue(GameConstants.GAME_RULE_WSK_PCZD_NO_DXW) == 1)
			return 26;
		else
			return 27;
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

		GRR = new GameRoundRecord(this.getTablePlayerNumber(), GameConstants.MAX_WEAVE_LAOPAI, GameConstants.WSK_MAX_COUNT,
				GameConstants.MAX_INDEX_LAOPAI);
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

		int score_card[] = { 0x05, 0x05, 0x15, 0x15, 0x25, 0x25, 0x35, 0x35, 0x0A, 0x0A, 0x1A, 0x1A, 0x2A, 0x2A, 0x3A, 0x3A, 0x0D, 0x0D, 0x1D, 0x1D,
				0x2D, 0x2D, 0x3D, 0x3D };
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
		if (this._is_display_friend == false && this._friend_seat[this._cur_banker] != seat_index)
			return;
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_WSK_PC_REFRESH_DUIYOU_CARD);
		// 发送数据
		Refresh_Pai_PC.Builder refresh_card = Refresh_Pai_PC.newBuilder();
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
		// if (GameConstants.GS_MJ_FREE != _game_status) {
		// return handler_player_ready(seat_index, false);
		// }
		return true;
	}
}
