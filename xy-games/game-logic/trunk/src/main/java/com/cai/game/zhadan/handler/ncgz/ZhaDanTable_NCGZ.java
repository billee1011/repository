package com.cai.game.zhadan.handler.ncgz;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.define.ERoomStatus;
import com.cai.common.domain.Player;
import com.cai.common.domain.PlayerResult;
import com.cai.common.util.PBUtil;
import com.cai.common.util.RandomUtil;
import com.cai.domain.SheduleArgs;
import com.cai.future.GameSchedule;
import com.cai.game.RoomUtil;
import com.cai.game.zhadan.AbstractZhaDanTable;
import com.cai.game.zhadan.Player_EX;
import com.cai.game.zhadan.ZhaDanConstants;
import com.cai.game.zhadan.ZhaDanGameLogic_NCGZ;
import com.cai.game.zhadan.ZhaDanMsgConstants;
import com.cai.game.zhadan.data.tagAnalyseIndexResult_ZhaDan;
import com.cai.service.PlayerServiceImpl;
import com.cai.util.ClubMsgSender;

import protobuf.clazz.Protocol.GameEndResponse;
import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomPlayerResponse;
import protobuf.clazz.Protocol.RoomRequest;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.ncgz.NcgzRsp.GameStart_Ncgz;
import protobuf.clazz.ncgz.NcgzRsp.OutCardDataWsk_Ncgz;
import protobuf.clazz.ncgz.NcgzRsp.PukeGameEndWsk_Ncgz;
import protobuf.clazz.ncgz.NcgzRsp.RefreshCardData_Ncgz;
import protobuf.clazz.ncgz.NcgzRsp.RefreshScore_Ncgz;
import protobuf.clazz.ncgz.NcgzRsp.SendCard_Ncgz;
import protobuf.clazz.ncgz.NcgzRsp.TableResponse_Ncgz;
import protobuf.clazz.ncgz.NcgzRsp.effect_type_Ncgz;
import protobuf.clazz.ncst.NcstRsp.Opreate_RequestWsk_Ncst;


public class ZhaDanTable_NCGZ extends AbstractZhaDanTable {

	private static final long serialVersionUID = -2419887548488809773L;

	protected static final int ID_TIMER_SEND_CARD = 1;// 发牌时间
	protected static final int ID_TIMER_ROUND_FINISH = 2;// 回合结束
	protected static final int ID_TIMER_NOT_OUT = 3;// 要不起
	// 特效
	protected static final int EFFECT_SCORE = 1;// 分数
	protected static final int EFFECT_FOUR_KING = 2;// 四小龙王
	protected static final int EFFECT_SIX_KING = 3;// 六六大顺
	protected static final int EFFECT_EIGHT_KING = 4;// 八仙过海
	protected static final int EFFECT_TWELVE_GOD = 5;// 十二罗汉
	protected static final int EFFECT_TOU_XIANG_YIN_CANG = 6;// 投降按钮消失
	protected static final int EFFECT_FEI_JI = 7;// 废机
	protected static final int EFFECT_WAIT_FRIEND_TOUXIANG = 8;// 等待对家投降
	protected static final int EFFECT_TOU_XIANG_ANSER_YIN_CANG = 9;// 询问投降界面隐藏
	
	public int _prv_get_score[];
	public int _flower_score[];
	public int _is_call_banker[];

	public int _turn_have_score; // 当局得分
	
	public int _dun_shu[];//墩数
	public int _get_score[];//捡分
	public int _jiang_score[];//奖分
	public int _wang_score[];//王分
	public int _win_score[];//输赢分
	public int _end_score[];//当局得分
	public int _all_score[];//总分
	
	public int _tmp_jiang_score[];//奖分
	public int _tmp_wang_score[];//王分
	
	//大结算
	public int _win_count[];//胜局数
	public int _yi_you_count[];//一游次数
	public int _max_dun_num[];//最高墩头数

	

	public int _jiao_pai_card;
	public int _out_card_ming_ji;
	
	public int _ming_pai_request[];
	public int _ming_pai_agree[];
	public int _is_ming_pai[];
	public int _round;
	public int _change_origin_seat = 0;
	
	public ZhaDanGameLogic_NCGZ _logic;

	public ZhaDanTable_NCGZ() {
	}

	public void on_init_table(int game_type_index, int game_rule_index, int game_round) {
		_logic = new ZhaDanGameLogic_NCGZ();
		_get_score = new int[getTablePlayerNumber()];
		_prv_get_score = new int[getTablePlayerNumber()];
		_dun_shu = new int[getTablePlayerNumber()];
		_jiang_score = new int[getTablePlayerNumber()];
		_wang_score = new int[getTablePlayerNumber()];
		_win_score = new int[getTablePlayerNumber()];
		_end_score = new int[getTablePlayerNumber()];
		_all_score = new int[getTablePlayerNumber()];
		_tmp_jiang_score = new int[getTablePlayerNumber()];
		_tmp_wang_score = new int[getTablePlayerNumber()];
		
		_win_count = new int[getTablePlayerNumber()];
		_yi_you_count = new int[getTablePlayerNumber()];
		_max_dun_num = new int[getTablePlayerNumber()];
		
		_flower_score = new int[getTablePlayerNumber()];
		_player_info = new Player_EX[getTablePlayerNumber()];
		_xi_qian_score = new int[getTablePlayerNumber()];
		_xi_qian_times = new int[getTablePlayerNumber()];
		_seat_team = new int[getTablePlayerNumber()];
		_is_call_banker = new int[getTablePlayerNumber()];
		_max_end_score = new int[getTablePlayerNumber()];
		
		_cur_out_card_type = new int[getTablePlayerNumber()];
		_out_card_times = new int[getTablePlayerNumber()];
		_turn_out_card_data = new int[get_hand_card_count_max()];
		_cur_out_card_count = new int[getTablePlayerNumber()];
		_cur_out_card_type = new int[getTablePlayerNumber()];
		_cur_out_card_data = new int[getTablePlayerNumber()][get_hand_card_count_max()];
		_win_order = new int[getTablePlayerNumber()];
		_ming_pai_request = new int[getTablePlayerNumber()];
		_init_card_data = new int[getTablePlayerNumber()][get_hand_card_count_max()];
		_init_card_count = new int[getTablePlayerNumber()];
		_is_ming_pai = new int[getTablePlayerNumber()];
		_ming_pai_agree = new int[getTablePlayerNumber()];
		_player_info = new Player_EX[getTablePlayerNumber()];
		_init_account_id = new long[getTablePlayerNumber()];
		_di_pai_count = 4;
		_di_pai = new int[_di_pai_count];
		_game_type_index = game_type_index;//
		_game_rule_index = game_rule_index;
		_player_result = new PlayerResult(this.getRoom_owner_account_id(), this.getRoom_id(), _game_type_index,
				_game_rule_index, _game_round, this.get_game_des(), this.getTablePlayerNumber());
		_logic.ruleMap = this.ruleMap;
		_score_type = new int[getTablePlayerNumber()];
		_jiao_pai_card = GameConstants.INVALID_CARD;
		_handler_out_card_operate = new ZhaDanHandlerOutCardOperate_NCGZ();
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_seat_team[i] = -1;
			_max_end_score[i] = 0;
			_player_info[i] = new Player_EX();
			_end_score[i] = 0;
			_score_type[i] = GameConstants.WSK_ST_CUSTOM;
			
		}

		game_cell = 10;

	}

	public void Refresh_user_get_score(int seat_index, int to_player, int is_animation) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		RefreshScore_Ncgz.Builder refresh_user_getscore = RefreshScore_Ncgz.newBuilder();
		roomResponse.setType(ZhaDanMsgConstants.RESPONSE_NCST_USER_GET_SCORE);
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			refresh_user_getscore.addUserGetScore(_get_score[i]);
			refresh_user_getscore.addRewardScore(this._dun_shu[i]);
			refresh_user_getscore.addTeam(this._seat_team[i]);
			refresh_user_getscore.addPrvGetScore(this._prv_get_score[i]);
		}
		refresh_user_getscore.setTableScore(_turn_have_score);
		refresh_user_getscore.setIsAnimation(is_animation);
		refresh_user_getscore.setGetScoreSeat(seat_index);
		// refresh_user_getscore.setTableScore(value);
		roomResponse.setCommResponse(PBUtil.toByteString(refresh_user_getscore));

		if (to_player == GameConstants.INVALID_SEAT) {
			GRR.add_room_response(roomResponse);
			this.send_response_to_room(roomResponse);
		} else {
			this.send_response_to_player(to_player, roomResponse);
		}
	}

	public void round_reset() {
		_handler_out_card_operate = new ZhaDanHandlerOutCardOperate_NCGZ();
		_round++;
		
		if(this.getTablePlayerNumber() == 3){
			if(this.has_rule(Constants_NCGZ.GAME_RULE_PUKE_COUNT_3)){
				_repertory_card = new int[Constants_NCGZ.CARD_DATA_NCGZ_3.length];
				shuffle(_repertory_card, Constants_NCGZ.CARD_DATA_NCGZ_3);
			}
			else if(this.has_rule(Constants_NCGZ.GAME_RULE_PUKE_COUNT_4)){
				_repertory_card = new int[Constants_NCGZ.CARD_DATA_NCGZ_4.length];
				shuffle(_repertory_card, Constants_NCGZ.CARD_DATA_NCGZ_4);
			}
		}else if(this.getTablePlayerNumber() == 4){
			if(this.has_rule(Constants_NCGZ.GAME_RULE_PUKE_COUNT_4)){
				_repertory_card = new int[Constants_NCGZ.CARD_DATA_NCGZ_4.length];
				shuffle(_repertory_card, Constants_NCGZ.CARD_DATA_NCGZ_4);
			}
			else if(this.has_rule(Constants_NCGZ.GAME_RULE_PUKE_COUNT_5)){
				_repertory_card = new int[Constants_NCGZ.CARD_DATA_NCGZ_5_1.length];
				shuffle(_repertory_card, Constants_NCGZ.CARD_DATA_NCGZ_5_1);
			}
		}else if(this.getTablePlayerNumber() == 5){
			if(this.has_rule(Constants_NCGZ.GAME_RULE_PUKE_COUNT_5)){
				_repertory_card = new int[Constants_NCGZ.CARD_DATA_NCGZ_5.length];
				shuffle(_repertory_card, Constants_NCGZ.CARD_DATA_NCGZ_5);
			}
			else if(this.has_rule(Constants_NCGZ.GAME_RULE_PUKE_COUNT_6)){
				_repertory_card = new int[Constants_NCGZ.CARD_DATA_NCGZ_6_1.length];
				shuffle(_repertory_card, Constants_NCGZ.CARD_DATA_NCGZ_6_1);
			}
		}
	
		Arrays.fill(_turn_out_card_data, GameConstants.INVALID_CARD);
		_turn_have_score = 0;
		_turn_out_card_count = 0;
		Arrays.fill(_turn_out_card_data, GameConstants.INVALID_CARD);
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_cur_out_card_count[i] = 0;
			_xi_qian_score[i] = 0;
			_is_call_banker[i] = 0;
			_xi_qian_times[i] = 0;
			_flower_score[i] = 0;
			_cur_out_card_type[i] = GameConstants.SXTH_CT_ERROR;
			_out_card_times[i] = 0;
			_win_order[i] = -1;
			_ming_pai_request[i] = 0;
			_ming_pai_agree[i] = 0;
			_is_ming_pai[i] = 0;
			Arrays.fill(_cur_out_card_data[i], GameConstants.INVALID_CARD);
			_prv_get_score[i] += this._get_score[i];
			_dun_shu[i] = 0;
			_get_score[i] = 0;
			_jiang_score[i] = 0;
			_wang_score[i] = 0;
			_win_score[i] = 0;
			_end_score[i] = 0;
			_tmp_jiang_score[i] = 0;
			_tmp_wang_score[i] = 0;
			_cur_out_card_count[i] = 0;
			_cur_out_card_type[i] = GameConstants.SXTH_CT_ERROR;
			Arrays.fill(_cur_out_card_data[i], GameConstants.INVALID_CARD);
		}
		this.Refresh_user_get_score(GameConstants.INVALID_SEAT, GameConstants.INVALID_SEAT, 0);

	}

	// 游戏开始
	@Override
	protected boolean on_handler_game_start() {
		if (_cur_round == 2) {
			// real_kou_dou();// 记录真实扣豆
		}

		reset_init_data();
		if (this._cur_round == 1) {
			_init_players = new Player[this.getTablePlayerNumber()];
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				_init_account_id[i] = this.get_players()[i].getAccount_id();
				_init_players[i] = this.get_players()[i];
			}
		}

		//if (has_rule(ZhaDanConstants.GAME_RULE_NCST_CHANGE_SEAT)) {
		//	shuffle_players_data();
		//}

		_handler_out_card_operate = new ZhaDanHandlerOutCardOperate_NCGZ();
		GRR._banker_player = GameConstants.INVALID_SEAT;
		this._current_player = GRR._banker_player;
		this._turn_out_card_count = 0;
		_round = 1;
		_change_origin_seat++;
		Arrays.fill(_turn_out_card_data, GameConstants.INVALID_CARD);
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_cur_out_card_count[i] = 0;
			_xi_qian_score[i] = 0;
			_is_call_banker[i] = 0;
			_xi_qian_times[i] = 0;
			_flower_score[i] = 0;
			_cur_out_card_type[i] = GameConstants.SXTH_CT_ERROR;
			_out_card_times[i] = 0;
			_seat_team[i] = i % 2;
			_win_order[i] = -1;
			_ming_pai_request[i] = 0;
			_ming_pai_agree[i] = 0;
			_is_ming_pai[i] = 0;
			_prv_get_score[i] = 0;
			_end_score[i] = 0;
			_prv_get_score[i] = 0;
			_dun_shu[i] = 0;
			_get_score[i] = 0;
			_jiang_score[i] = 0;
			_wang_score[i] = 0;
			_win_score[i] = 0;
			_end_score[i] = 0;
			_tmp_jiang_score[i] = 0;
			_tmp_wang_score[i] = 0;
			Arrays.fill(_cur_out_card_data[i], GameConstants.INVALID_CARD);
		}

		_turn_have_score = 0;
		_out_card_ming_ji = GameConstants.INVALID_CARD;

		if(this.getTablePlayerNumber() == 3){
			if(this.has_rule(Constants_NCGZ.GAME_RULE_PUKE_COUNT_3)){
				_repertory_card = new int[Constants_NCGZ.CARD_DATA_NCGZ_3.length];
				shuffle(_repertory_card, Constants_NCGZ.CARD_DATA_NCGZ_3);
			}
			else if(this.has_rule(Constants_NCGZ.GAME_RULE_PUKE_COUNT_4)){
				_repertory_card = new int[Constants_NCGZ.CARD_DATA_NCGZ_4.length];
				shuffle(_repertory_card, Constants_NCGZ.CARD_DATA_NCGZ_4);
			}
		}else if(this.getTablePlayerNumber() == 4){
			if(this.has_rule(Constants_NCGZ.GAME_RULE_PUKE_COUNT_4)){
				_repertory_card = new int[Constants_NCGZ.CARD_DATA_NCGZ_4.length];
				shuffle(_repertory_card, Constants_NCGZ.CARD_DATA_NCGZ_4);
			}
			else if(this.has_rule(Constants_NCGZ.GAME_RULE_PUKE_COUNT_5)){
				_repertory_card = new int[Constants_NCGZ.CARD_DATA_NCGZ_5_1.length];
				shuffle(_repertory_card, Constants_NCGZ.CARD_DATA_NCGZ_5_1);
			}
		}else if(this.getTablePlayerNumber() == 5){
			if(this.has_rule(Constants_NCGZ.GAME_RULE_PUKE_COUNT_5)){
				_repertory_card = new int[Constants_NCGZ.CARD_DATA_NCGZ_5.length];
				shuffle(_repertory_card, Constants_NCGZ.CARD_DATA_NCGZ_5);
			}
			else if(this.has_rule(Constants_NCGZ.GAME_RULE_PUKE_COUNT_6)){
				_repertory_card = new int[Constants_NCGZ.CARD_DATA_NCGZ_6_1.length];
				shuffle(_repertory_card, Constants_NCGZ.CARD_DATA_NCGZ_6_1);
			}
		}

		if (DEBUG_CARDS_MODE || BACK_DEBUG_CARDS_MODE)
			test_cards();

		getLocationTip();

		// 游戏开始时 初始化 未托管
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			istrustee[i] = false;
		}

		// 庄家选择
		this.progress_banker_select();

		on_game_start();

		return true;
	}

	private void shuffle_players_data() {
		List<Player> pl = new ArrayList<Player>();
		int team_seat[] = new int[2];
		team_seat[0] = _change_origin_seat % this.getTablePlayerNumber();
		team_seat[1] = (_change_origin_seat + 1) % this.getTablePlayerNumber();

		if (team_seat[0] == team_seat[1] || (team_seat[0] + 2) % this.getTablePlayerNumber() == team_seat[1]) {
			return;
		}
		if (team_seat[0] != team_seat[1] && team_seat[0] != GameConstants.INVALID_SEAT
				&& team_seat[1] != GameConstants.INVALID_SEAT) {
			// 换用户信息
			Player temp_player = new Player();
			temp_player = get_players()[(team_seat[0] + 2) % this.getTablePlayerNumber()];
			get_players()[(team_seat[0] + 2) % this.getTablePlayerNumber()] = get_players()[team_seat[1]];
			get_players()[team_seat[1]] = temp_player;
			get_players()[(team_seat[0] + 2) % this.getTablePlayerNumber()]
					.set_seat_index((team_seat[0] + 2) % this.getTablePlayerNumber());
			get_players()[team_seat[1]].set_seat_index(team_seat[1]);

			Player_EX temp_info = new Player_EX();
			temp_info = _player_info[(team_seat[0] + 2) % this.getTablePlayerNumber()];
			_player_info[(team_seat[0] + 2) % this.getTablePlayerNumber()] = _player_info[team_seat[1]];
			_player_info[team_seat[1]] = temp_info;

			float temp_score = _player_result.game_score[(team_seat[0] + 2) % this.getTablePlayerNumber()];
			_player_result.game_score[(team_seat[0] + 2)
					% this.getTablePlayerNumber()] = _player_result.game_score[team_seat[1]];
			_player_result.game_score[team_seat[1]] = temp_score;

		}

		RoomResponse.Builder roomResponse2 = RoomResponse.newBuilder();
		roomResponse2.setGameStatus(_game_status);
		roomResponse2.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);// 刷新玩家
		this.load_player_info_data(roomResponse2);
		this.send_response_to_room(roomResponse2);
		GRR.add_room_response(roomResponse2);

		this.RefreshCard(GameConstants.INVALID_SEAT);
		ClubMsgSender.roomPlayerStatusUpdate(ERoomStatus.TABLE_REFRESH, this);
	}

	/// 洗牌
	public void shuffle(int repertory_card[], int card_cards[]) {

		int xi_pai_count = 0;
		int rand = (int) RandomUtil.generateRandomNumber(3, 6);

		while (xi_pai_count < 6 && xi_pai_count < rand) {
			if (xi_pai_count == 0)
				_logic.random_card_data(repertory_card, card_cards);
			else
				_logic.random_card_data(repertory_card, repertory_card);

			xi_pai_count++;
		}

		_liang_card_value = RandomUtil.getRandomNumber(Integer.MAX_VALUE)
				% card_cards.length - 1;
		
		if(_liang_card_value < 0){
			_liang_card_value = 52;
			//return;
		}
		
		int count = this.getTablePlayerNumber();
		for (int i = 0; i < count; i++) {
			for (int j = 0; j < this.get_hand_card_count_max(); j++) {
				GRR._cards_data[i][j] = repertory_card[i * this.get_hand_card_count_max() + j];
				_init_card_data[i][j] = repertory_card[i * this.get_hand_card_count_max() + j];
				if (_liang_card_value == i * this.get_hand_card_count_max() + j) {
					if (this._round == 1) {
						_cur_banker = i;
					}
				}

			}
			_init_card_count[i] = this.get_hand_card_count_max();
			GRR._card_count[i] = this.get_hand_card_count_max();
			_logic.SortCardList(GRR._cards_data[i], GRR._card_count[i], _score_type[i]);
		}
		// 记录初始牌型
		_recordRoomRecord.setBeginArray(Arrays.toString(repertory_card));
	}

	public int get_hand_card_count_max() {
		if(this.getTablePlayerNumber() == 3){
			if(this.has_rule(Constants_NCGZ.GAME_RULE_PUKE_COUNT_4)){
				return Constants_NCGZ.MAX_COUNT_NCGZ_72;
			}else{
				return Constants_NCGZ.MAX_COUNT_NCGZ_54;
			}
		}else if(this.getTablePlayerNumber() == 4){
			if(this.has_rule(Constants_NCGZ.GAME_RULE_PUKE_COUNT_5)){
				return Constants_NCGZ.MAX_COUNT_NCGZ_68;
			}else{
				return Constants_NCGZ.MAX_COUNT_NCGZ_54;
			}
		}else if(this.getTablePlayerNumber() == 5){
			if(this.has_rule(Constants_NCGZ.GAME_RULE_PUKE_COUNT_6)){
				return Constants_NCGZ.MAX_COUNT_NCGZ_65;
			}else{
				return Constants_NCGZ.MAX_COUNT_NCGZ_54;
			}
		}
		return Constants_NCGZ.MAX_COUNT_NCGZ_54;
	}

	public int get_hand_card_count_init() {
		return get_hand_card_count_max();
	}

	protected void test_cards() {
		/*************** 如果要测试线上实际牌 把下面代码取消注释 放入实际牌型即可 ***********************/

		/*int cards[] = new int[] {1,17,33,49,1,17,33,49,1,17,33,49,1,17,33,49,1,17,33,49,1,17,33,49,2,18,34,50,2,18,34,50,2,18,34,50, 2,18,34,50,2,18,34,50,2,18,34,50,3,19,35,51,3,19,35,51,3,19,35,51,3,19,35,51,3,19,35,51,3,19,35,51, 4,20,36,52,4,20,36,52,4,20,36,52,4,20,36,52,4,20,36,52,4,20,36,52,5,21,37,53,5,21,37,53,5,21,37,53, 5,21,37,53,5,21,37,53,5,21,37,53,6,22,38,54,6,22,38,54,6,22,38,54,6,22,38,54,6,22,38,54,6,22,38,54, 7,23,39,55,7,23,39,55,7,23,39,55,7,23,39,55,7,23,39,55,7,23,39,55,8,24,40,56,8,24,40,56,8,24,40,56, 8,24,40,56,8,24,40,56,8,24,40,56,9,25,41,57,9,25,41,57,9,25,41,57,9,25,41,57,9,25,41,57,9,25,41,57, 10,26,42,58,10,26,42,58,10,26,42,58,10,26,42,58,10,26,42,58,10,26,42,58,11,27,43,59,11,27,43,59,11, 27,43,59,11,27,43,59,11,27,43,59,11,27,43,59,12,28,44,60,12,28,44,60,12,28,44,60,12,28,44,60,12,28, 44,60,12,28,44,60,13,29,45,61,13,29,45,61,13,29,45,61,13,29,45,61,13,29,45,61,13,29,45,61,78,78,78,78,79,78,79,78,79,79,79,79,79};
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (this.get_players()[i] == null) {
				continue;
			}
			for (int j = 0; j < this.get_hand_card_count_max(); j++) {
				GRR._cards_data[i][j] = 0;
			}
		}
		int index = 0;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (this.get_players()[i] == null) {
				continue;
			}

			int card_count = get_hand_card_count_max();
			for (int j = 0; j < card_count; j++) {
				GRR._cards_data[i][j] = cards[index++];
				_init_card_data[i][j] = GRR._cards_data[i][j];
			}
			GRR._card_count[i] = card_count;
			_logic.SortCardList(GRR._cards_data[i], GRR._card_count[i], _score_type[i]);
		}*/
		
		/*debug_my_cards = new int[] { 1,17,33,49,1,17,33,49,1,17,33,49,1,17,33,49,1,17,33,49,2,18,34,50,2,18,34,50,2,18,
				34,50,2,18,34,50, 2,18,34,50,3,19,35,51,3,19,35,51,3,19,35,51,3,19,35,51,3,19,35,51,4,20,36,52,4,20,
				36,52,4,20,36,52, 4,20,36,52,4,20,36,52,5,21,37,53,5,21,37,53,5,21,37,53,5,21,37,53,5,21,37,53,6,22,
				38,54,6,22,38,54, 6,22,38,54,6,22,38,54,6,22,38,54,7,23,39,55,7,23,39,55,7,23,39,55,7,23,39,55,7,23,
				39,55,8,24,40,56, 8,24,40,56,8,24,40,56,8,24,40,56,8,24,40,56,9,25,41,57,9,25,41,57,9,25,41,57,9,25,
				41,57,9,25,41,57, 10,26,42,58,10,26,42,58,10,26,42,58,10,26,42,58,10,26,42,58,11,27,43,59,11,27,43,
				59,11,27,43,59,1,27,43,59,11,27,43,59,12,28,44,60,12,28,44,60,12,28,44,60,12,28,44,60,12,28,44,60,
				13,29,45,61,13,29,45,61,13,29,45,61,13,29,45,61,13,29,45,61,78,78,78,78,78,79,79,79,79,79};*/
		/********
		 * 下面这个给测试用的 如果开发用 把上面的注释去掉 realyCards
		 **************************/

		//BACK_DEBUG_CARDS_MODE = true;
		if (BACK_DEBUG_CARDS_MODE) {
			if (debug_my_cards != null) {
				if (debug_my_cards.length > get_hand_card_count_max()) {
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
	@Override
	public void testRealyCard(int[] realyCards) {
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			for (int j = 0; j < this.get_hand_card_count_max(); j++) {
				GRR._cards_data[i][j] = 0;
			}
		}
		this._repertory_card = realyCards;
		_cur_banker = 0;
		// 分发扑克
		int k = 0;
		while (k < realyCards.length) {
			int i = 0;

			for (; i < this.getTablePlayerNumber(); i++) {
				if (this.get_players()[i] == null) {
					continue;
				}
				for (int j = 0; j < get_hand_card_count_init(); j++) {
					GRR._cards_data[i][j] = realyCards[k++];
					_init_card_data[i][j] = GRR._cards_data[i][j];
				}
				GRR._card_count[i] = get_hand_card_count_init();
				_logic.SortCardList(GRR._cards_data[i], GRR._card_count[i], _score_type[i]);
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
	@Override
	public void testSameCard(int[] cards) {
		int count = cards.length;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			for (int j = 0; j < count; j++) {
				GRR._cards_data[i][j] = 0;
			}

		}
		this._repertory_card = cards;
		// 分发扑克
		int k = 0;
		int i = 0;

		for (; i < this.getTablePlayerNumber(); i++) {
			k = 0;
			for (int j = 0; j < count; j++) {
				GRR._cards_data[i][j] = cards[j];
				_init_card_data[i][j] = GRR._cards_data[i][j];
			}
			GRR._card_count[i] = count;
			_logic.SortCardList(GRR._cards_data[i], GRR._card_count[i], _score_type[i]);
		}

		DEBUG_CARDS_MODE = false;// 把调试模式关闭
		BACK_DEBUG_CARDS_MODE = false;
	}

	protected boolean on_game_start() {
		this.GRR._banker_player = this._cur_banker;
		_game_status = GameConstants.GS_SXTH_PLAY;
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(ZhaDanMsgConstants.RESPONSE_NCST_GAME_START);
		roomResponse.setGameStatus(this._game_status);
		roomResponse.setRoomInfo(getRoomInfo());
		// 发送数据
		GameStart_Ncgz.Builder gamestart = GameStart_Ncgz.newBuilder();
		gamestart.setRoomInfo(this.getRoomInfo());
		this.load_player_info_data_game_start(gamestart);
		gamestart.setLiangPaiCard(_repertory_card[_liang_card_value]);
		gamestart.setSeatIndex(this._cur_banker);
		gamestart.setGameCell((int) this.game_cell);
		roomResponse.setCommResponse(PBUtil.toByteString(gamestart));
		// 自己才有牌数据
		this.send_response_to_room(roomResponse);
		GRR.add_room_response(roomResponse);
		Send_Card();
		Refresh_user_get_score(GameConstants.INVALID_SEAT, GameConstants.INVALID_SEAT, 0);
		schedule(ID_TIMER_SEND_CARD, SheduleArgs.newArgs(), 3000);
		this.set_handler(this._handler_out_card_operate);
		return true;
	}

	public void Send_Card() {
		for (int play_index = 0; play_index < this.getTablePlayerNumber(); play_index++) {

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(ZhaDanMsgConstants.RESPONSE_NCST_SEND_CARD);
			roomResponse.setGameStatus(this._game_status);
			roomResponse.setRoomInfo(getRoomInfo());
			// 发送数据
			SendCard_Ncgz.Builder send_card = SendCard_Ncgz.newBuilder();

			for (int i = 0; i < this.getTablePlayerNumber(); i++) {

				Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
				if (play_index == i) {
					for (int j = 0; j < this.GRR._card_count[i]; j++) {
						cards_card.addItem(GRR._cards_data[i][j]);
					}
					// 分析扑克
					send_card.addCardCount(GRR._card_count[i]);
				} else {
					// 分析扑克
					send_card.addCardCount(-1);
				}
				send_card.addCardsData(cards_card);
			}
			send_card.setRound(this._round);
			roomResponse.setCommResponse(PBUtil.toByteString(send_card));

			// 自己才有牌数据
			this.send_response_to_player(play_index, roomResponse);
		}
		// 回放
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(ZhaDanMsgConstants.RESPONSE_NCST_SEND_CARD);
		roomResponse.setGameStatus(this._game_status);
		roomResponse.setRoomInfo(this.getRoomInfo());

		// 发送数据
		SendCard_Ncgz.Builder send_card = SendCard_Ncgz.newBuilder();

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			// 分析扑克
			send_card.addCardCount(GRR._card_count[i]);

			Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < this.GRR._card_count[i]; j++) {
				cards_card.addItem(GRR._cards_data[i][j]);
			}
			send_card.addCardsData(cards_card);
		}
		send_card.setRound(this._round);
		roomResponse.setCommResponse(PBUtil.toByteString(send_card));

		GRR.add_room_response(roomResponse);
	}

	// 发送特效
	public void send_effect_type(int seat_index, int type, int data, int to_player) {
		//
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(ZhaDanMsgConstants.RESPONSE_NCST_EFFECT);
		effect_type_Ncgz.Builder effect = effect_type_Ncgz.newBuilder();
		effect.setSeatIndex(seat_index);
		effect.setType(type);
		effect.setData(data);
		roomResponse.setCommResponse(PBUtil.toByteString(effect));
		if (to_player == GameConstants.INVALID_SEAT) {
			this.send_response_to_room(roomResponse);
			GRR.add_room_response(roomResponse);
		} else {
			this.send_response_to_player(to_player, roomResponse);
		}

	}

	// 发送特效
	public void send_liang_dun(int seat_index) {
		//
		/*RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(ZhaDanMsgConstants.RESPONSE_NCST_LIANG_DUN);
		liang_dun_Ncgz.Builder laing_dun = liang_dun_Ncgz.newBuilder();
		roomResponse.setCommResponse(PBUtil.toByteString(laing_dun));
		boolean is_dun = false;
		for (int index = 0; index < this.getTablePlayerNumber(); index++) {
			Card_Arrary_Ncgz.Builder card_array = Card_Arrary_Ncgz.newBuilder();
			tagAnalyseIndexResult_ZhaDan hand_index = new tagAnalyseIndexResult_ZhaDan();
			this._logic.AnalysebCardDataToIndex(this._init_card_data[index], this._init_card_count[index], hand_index);
			for (int i = 0; i < ZhaDanConstants.ZHADAN_MAX_INDEX - 2; i++) {
				if (hand_index.card_index[i] > 6) {
					Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
					for (int j = 0; j < hand_index.card_index[i]; j++) {
						cards_card.addItem(hand_index.card_data[i][j]);
					}
					card_array.addCardData(cards_card);
					is_dun = true;
				}
			}
			if (hand_index.card_index[13] + hand_index.card_index[14] == 4) {
				Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < hand_index.card_index[13]; j++) {
					cards_card.addItem(hand_index.card_data[13][j]);
				}
				for (int j = 0; j < hand_index.card_index[14]; j++) {
					cards_card.addItem(hand_index.card_data[14][j]);
				}
				card_array.addCardData(cards_card);
				is_dun = true;
			} else if (hand_index.card_index[13] == 3) {
				Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < hand_index.card_index[13]; j++) {
					cards_card.addItem(hand_index.card_data[13][j]);
				}
				card_array.addCardData(cards_card);
				is_dun = true;
			}
			laing_dun.addCardsData(card_array);
		}
		roomResponse.setCommResponse(PBUtil.toByteString(laing_dun));
		if (!is_dun) {
			this.send_error_notify(seat_index, 2, "当前没有墩牌");
			return;
		}
		if (seat_index == GameConstants.INVALID_SEAT) {
			GRR.add_room_response(roomResponse);
			this.send_response_to_room(roomResponse);
		} else {
			this.send_response_to_player(seat_index, roomResponse);
		}*/

	}

	public void out_card_begin() {
		GRR._banker_player = _cur_banker;
		this._current_player = GRR._banker_player;
		for (int index = 0; index < this.getTablePlayerNumber(); index++) {
			tagAnalyseIndexResult_ZhaDan hand_index = new tagAnalyseIndexResult_ZhaDan();
			this._logic.AnalysebCardDataToIndex(this._init_card_data[index], this._init_card_count[index], hand_index);
			for (int i = 0; i < ZhaDanConstants.ZHADAN_MAX_INDEX - 2; i++) {
				if (hand_index.card_index[i] > 6) {
					for (int j = 0; j < this.getTablePlayerNumber(); j++) {
						if (j == index) {
							continue;
						}
						this._xi_qian_score[index] += hand_index.card_index[i] - 6;
						this._xi_qian_score[j] -= hand_index.card_index[i] - 6;
					}
				}
			}

			if (hand_index.card_index[13] + hand_index.card_index[14] == 4) {
				for (int j = 0; j < this.getTablePlayerNumber(); j++) {
					if (j == index) {
						continue;
					}
					this._xi_qian_score[index] += 2;
					this._xi_qian_score[j] -= 2;
				}
			} else if (hand_index.card_index[13] == 3) {
				for (int j = 0; j < this.getTablePlayerNumber(); j++) {
					if (j == index) {
						continue;
					}
					this._xi_qian_score[index] += 1;
					this._xi_qian_score[j] -= 1;
				}
			}
		}
		this.Refresh_user_get_score(GameConstants.INVALID_SEAT, GameConstants.INVALID_SEAT, 0);
		this.set_handler(this._handler_out_card_operate);
		this.operate_out_card(GameConstants.INVALID_SEAT, 0, null, GameConstants.SXTH_CT_ERROR,
				GameConstants.INVALID_SEAT, false);

	}

	/**
	 * 调度回调
	 */
	protected void timerCallBack(SheduleArgs args) {
		switch (args.getTimerId()) {
		case ID_TIMER_SEND_CARD: {
			send_liang_dun(GameConstants.INVALID_SEAT);
			out_card_begin();
			return;
		}
		case ID_TIMER_ROUND_FINISH: {
			this.round_reset();
			Send_Card();
			schedule(ID_TIMER_SEND_CARD, SheduleArgs.newArgs(), 5000);
			return;
		}
		case ID_TIMER_NOT_OUT:
			//_handler_out_card_operate.user_pass_card(this);
		}
	}

	// 回合结束
	public void send_round_finish() {
		/*RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(ZhaDanMsgConstants.RESPONSE_NCST_ROUND_END);
		round_end_Ncgz.Builder round_end = round_end_Ncgz.newBuilder();
		for (int i = 0; i < this._di_pai_count; i++) {
			round_end.addDiPaiCard(this._di_pai[i]);
		}
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			round_end.addUserGetScore(this._get_score[i]);
			round_end.addTeam(this._seat_team[i]);
		}
		roomResponse.setCommResponse(PBUtil.toByteString(round_end));
		this.send_response_to_room(roomResponse);*/
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
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(ZhaDanMsgConstants.RESPONSE_NCST_GAME_END);
		GameEndResponse.Builder game_end = GameEndResponse.newBuilder();
		PukeGameEndWsk_Ncgz.Builder game_end_ncst = PukeGameEndWsk_Ncgz.newBuilder();
		game_end.setRoomInfo(getRoomInfo());
		game_end_ncst.setRoomInfo(getRoomInfo());

		// 计算分数
		if (reason == GameConstants.Game_End_NORMAL) {
			cal_game_score();
			for(int i = 0;i < this.getTablePlayerNumber();i++){
				this._player_result.game_score[i] += this._end_score[i];
			}
			
		}else{
			for(int i = 0;i < getTablePlayerNumber();i++){
				_end_score[i] = _jiang_score[i] + _wang_score[i];
				this._player_result.game_score[i] += this._end_score[i];
			}
		}
		
		
		this.load_player_info_data_game_end(game_end_ncst);
		game_end.setGameRound(_game_round);
		game_end.setCurRound(_cur_round);
		if (GRR != null) {
			for (int i = 0; i < getTablePlayerNumber(); i++) {

				game_end_ncst.addCardCount(GRR._card_count[i]);
				Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GRR._card_count[i]; j++) {
					cards_card.addItem(GRR._cards_data[i][j]);
				}
				game_end_ncst.addCardsData(i, cards_card);
			}
			game_end.setStartTime(GRR._start_time);
			game_end.setGameTypeIndex(GRR._game_type_index);
		} else {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
				game_end_ncst.addCardsData(cards_card);
			}
		}

		for (int i = 0; i < getTablePlayerNumber(); i++) {

			if (this._end_score[i] > _max_end_score[i]) {
				_max_end_score[i] = _end_score[i];
			}
			for (int j = 0; j < getTablePlayerNumber(); j++) {
				if (this.get_players()[j].getAccount_id() == _init_account_id[i]) {
					game_end.addGameScore(_end_score[j]);
					break;
				}
			}
			game_end_ncst.addRewardCount(this._dun_shu[i]);//墩数
			game_end_ncst.addGetScore(this._get_score[i]);//捡分
			game_end_ncst.addRewardScore(this._tmp_jiang_score[i]);//奖分
			game_end_ncst.addKingScore(this._tmp_wang_score[i]);//王分
			game_end_ncst.addWinLoseScore(this._win_score[i]);//输赢分
			game_end_ncst.addEndScore(this._end_score[i]);//单局得分
			game_end_ncst.addAllScore((int) this._player_result.game_score[i]);//总得分
			//
			boolean is_out_finish = false;
			for (int j = 0; j < getTablePlayerNumber(); j++) {
				if (this._win_order[j] == i) {
					game_end_ncst.addWinOrder(j);
					is_out_finish = true;
					break;
				}
			}
			if (!is_out_finish) {
				game_end_ncst.addWinOrder(GameConstants.INVALID_SEAT);
			}

		}
		
		//大结算数据
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			game_end_ncst.addWinNum(this._win_count[i]);
			game_end_ncst.addYiYouNum(this._yi_you_count[i]);
			game_end_ncst.addMaxDunNum(this._max_dun_num[i]);
			game_end_ncst.addAllEndScore((int) this._player_result.game_score[i]);
		}
		boolean end = false;
		if (reason == GameConstants.Game_End_NORMAL || reason == GameConstants.Game_End_DRAW) {
			if (_cur_round >= _game_round) {// 局数到了
				end = true;
				Restore_Gamescore();
				game_end.setPlayerResult(this.process_player_result(reason));
				real_reason = GameConstants.Game_End_ROUND_OVER;
			}
		} else if (reason == GameConstants.Game_End_RELEASE_PLAY || reason == GameConstants.Game_End_RELEASE_NO_BEGIN
				|| reason == GameConstants.Game_End_RELEASE_RESULT
				|| reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_SYSTEM) {
			end = true;
			Restore_Gamescore();
			game_end.setPlayerResult(this.process_player_result(reason));
			real_reason = GameConstants.Game_End_RELEASE_PLAY;
		}
		game_end_ncst.setReason(real_reason);
		////////////////////////////////////////////////////////////////////// 得分总的
		game_end.setEndType(real_reason);
		game_end.setGameRound(_game_round);
		game_end.setCurRound(_cur_round);
		game_end.setCommResponse(PBUtil.toByteString(game_end_ncst));
		game_end.setRoundOverType(1);
		game_end.setEndTime(System.currentTimeMillis() / 1000L);// 结束时间

		roomResponse.setGameEnd(game_end);
		this.send_response_to_room(roomResponse);

		this.operate_player_data();
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
		Arrays.fill(_win_order, GameConstants.INVALID_SEAT);
		Arrays.fill(_flower_score, 0);

		this._handler_out_card_operate.reset();
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
	public int cal_score_zhadan(int end_score[], int win_seat_index) {

		return 0;
	}
	
	public int cal_game_score() {
		int base_get_score[] = new int[getTablePlayerNumber()];
		Arrays.fill(base_get_score, 0);
		if(this.getTablePlayerNumber() == 3){
			if(this.has_rule(Constants_NCGZ.GAME_RULE_PUKE_COUNT_3)){
				for(int i = 0;i < getTablePlayerNumber();i++){
					base_get_score[i] = 100;
				}
			}else if(this.has_rule(Constants_NCGZ.GAME_RULE_PUKE_COUNT_4)){
				base_get_score[this._win_order[0]] = 120;
				base_get_score[this._win_order[1]] = 120;
				base_get_score[this._win_order[2]] = 160;
			}
		}else if(this.getTablePlayerNumber() == 4){
			if(this.has_rule(Constants_NCGZ.GAME_RULE_PUKE_COUNT_4)){
				for(int i = 0;i < getTablePlayerNumber();i++){
					base_get_score[i] = 100;
				}
			}else if(this.has_rule(Constants_NCGZ.GAME_RULE_PUKE_COUNT_5)){
				base_get_score[this._win_order[0]] = 80;
				base_get_score[this._win_order[1]] = 130;
				base_get_score[this._win_order[2]] = 130;
				base_get_score[this._win_order[3]] = 160;
			}
		}else if(this.getTablePlayerNumber() == 5){
			if(this.has_rule(Constants_NCGZ.GAME_RULE_PUKE_COUNT_5)){
				for(int i = 0;i < getTablePlayerNumber();i++){
					base_get_score[i] = 100;
				}
			}else if(this.has_rule(Constants_NCGZ.GAME_RULE_PUKE_COUNT_6)){
				for(int i = 0;i < getTablePlayerNumber();i++){
					base_get_score[i] = 120;
				}
			}
		}
		
		_yi_you_count[_win_order[0]]++;
		for(int i = 0;i < getTablePlayerNumber();i++){
			_end_score[i] = _get_score[i] - base_get_score[i] + _jiang_score[i] + _wang_score[i] + this._win_score[i];
			if(_end_score[i] > 0){
				_win_count[i] ++;
			}
			if(_max_dun_num[i] < _dun_shu[i]){
				_max_dun_num[i] = _dun_shu[i];
			}
		}
		
		//加完了清一下，防止意外结算又加一次
		for(int i = 0;i < getTablePlayerNumber();i++){
			_jiang_score[i]  = 0;
			_wang_score[i] = 0;
		}
		
		return 0;
	}

	public void load_player_info_data_game_start(GameStart_Ncgz.Builder roomResponse) {
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
			room_player.setScore(this._player_result.game_score[i]);
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

	public void load_player_info_data_reconnect(TableResponse_Ncgz.Builder roomResponse) {
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
			room_player.setScore(this._player_result.game_score[i]);
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

	public void load_player_info_data_game_end(PukeGameEndWsk_Ncgz.Builder roomResponse) {
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
			room_player.setScore(this._player_result.game_score[i]);
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

	public boolean operate_out_card(int seat_index, int count, int cards_data[], int type, int to_player,
			boolean is_deal) {
		// if (seat_index == GameConstants.INVALID_SEAT) {
		// return false;
		// }
		//// 发送玩家出牌 201 =玩家出牌(CMD_OutCard: operate_player,operate_card)
		int team = -1;

		if (to_player == GameConstants.INVALID_SEAT) {
			for (int index = 0; index < this.getTablePlayerNumber(); index++) {
				RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
				OutCardDataWsk_Ncgz.Builder outcarddata = OutCardDataWsk_Ncgz.newBuilder();
				// Int32ArrayResponse.Builder cards =
				// Int32ArrayResponse.newBuilder();
				roomResponse.setType(ZhaDanMsgConstants.RESPONSE_NCST_OUT_CARD);// 201
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

				if (_turn_out_card_count == 0) {
					outcarddata.setIsCurrentFirstOut(1);

				} else {
					outcarddata.setIsCurrentFirstOut(0);
				}
				if (index == this._current_player) {
					int is_can_ya = this._logic.search_out_card(GRR._cards_data[index], GRR._card_count[index],
							this._turn_out_card_data, this._turn_out_card_count);
					outcarddata.setIsCanYa(is_can_ya);
				} else {
					outcarddata.setIsCanYa(1);
				}

				for (int i = 0; i < this.getTablePlayerNumber(); i++) {
					Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
					if (index == i) {
						for (int j = 0; j < this.GRR._card_count[i]; j++) {
							cards_card.addItem(GRR._cards_data[i][j]);
						}
					} 

					outcarddata.addHandCardsData(cards_card);
					if (i == index) {
						outcarddata.addHandCardCount(this.GRR._card_count[i]);
					} else if (this.GRR._card_count[i] > 11) {
						outcarddata.addHandCardCount(-1);
					} else {
						outcarddata.addHandCardCount(this.GRR._card_count[i]);
					}

					boolean is_out_finish = false;
					for (int j = 0; j < this.getTablePlayerNumber(); j++) {
						if (this._win_order[j] == i) {
							outcarddata.addWinOrder(j);
							is_out_finish = true;
							break;
						}

					}
					if (!is_out_finish) {
						outcarddata.addWinOrder(GameConstants.INVALID_SEAT);
					}
				}

				roomResponse.setCommResponse(PBUtil.toByteString(outcarddata));

				this.send_response_to_player(index, roomResponse);

			}

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			OutCardDataWsk_Ncgz.Builder outcarddata = OutCardDataWsk_Ncgz.newBuilder();
			// Int32ArrayResponse.Builder cards =
			// Int32ArrayResponse.newBuilder();
			roomResponse.setType(ZhaDanMsgConstants.RESPONSE_NCST_OUT_CARD);// 201
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

			if (_turn_out_card_count == 0) {
				outcarddata.setIsCurrentFirstOut(1);

			} else {
				outcarddata.setIsCurrentFirstOut(0);
			}

			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < this.GRR._card_count[i]; j++) {
					cards_card.addItem(this.GRR._cards_data[i][j]);
				}
				outcarddata.addHandCardsData(cards_card);
				outcarddata.addHandCardCount(this.GRR._card_count[i]);
				boolean is_out_finish = false;
				for (int j = 0; j < this.getTablePlayerNumber(); j++) {
					if (this._win_order[j] == i) {
						outcarddata.addWinOrder(j);
						is_out_finish = true;
						break;
					}

				}
				if (!is_out_finish) {
					outcarddata.addWinOrder(GameConstants.INVALID_SEAT);
				}
			}

			roomResponse.setCommResponse(PBUtil.toByteString(outcarddata));
			GRR.add_room_response(roomResponse);
		} else {
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			OutCardDataWsk_Ncgz.Builder outcarddata = OutCardDataWsk_Ncgz.newBuilder();
			// Int32ArrayResponse.Builder cards =
			// Int32ArrayResponse.newBuilder();
			roomResponse.setType(ZhaDanMsgConstants.RESPONSE_NCST_OUT_CARD);// 201
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

			if (_turn_out_card_count == 0) {
				outcarddata.setIsCurrentFirstOut(1);

			} else {
				outcarddata.setIsCurrentFirstOut(0);
			}
			if (to_player == this._current_player) {
				int is_can_ya = this._logic.search_out_card(GRR._cards_data[to_player], GRR._card_count[to_player],
						this._turn_out_card_data, this._turn_out_card_count);
				outcarddata.setIsCanYa(is_can_ya);
			} else {
				outcarddata.setIsCanYa(1);
			}

			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
				if (to_player == i ) {
					for (int j = 0; j < this.GRR._card_count[i]; j++) {
						cards_card.addItem(GRR._cards_data[i][j]);
					}
				}

				outcarddata.addHandCardsData(cards_card);
				if (i == to_player) {
					outcarddata.addHandCardCount(this.GRR._card_count[i]);
				} else if (this.GRR._card_count[i] > 11) {
					outcarddata.addHandCardCount(-1);
				} else {
					outcarddata.addHandCardCount(this.GRR._card_count[i]);
				}

				boolean is_out_finish = false;
				for (int j = 0; j < this.getTablePlayerNumber(); j++) {
					if (this._win_order[j] == i) {
						outcarddata.addWinOrder(j);
						is_out_finish = true;
						break;
					}

				}
				if (!is_out_finish) {
					outcarddata.addWinOrder(GameConstants.INVALID_SEAT);
				}
			}

			roomResponse.setCommResponse(PBUtil.toByteString(outcarddata));

			this.send_response_to_player(to_player, roomResponse);
		}

		return true;
	}

	@Override
	public boolean handler_requst_message_deal(Player player, int seat_index, RoomRequest room_rq, int type) {
		if (type == MsgConstants.REQUST_NCST_OPERATE) {
			Opreate_RequestWsk_Ncst req = PBUtil.toObject(room_rq, Opreate_RequestWsk_Ncst.class);
			// 逻辑处理
			return handler_requst_opreate(seat_index, req.getOpreateType(), req.getChatIndex());
		}

		return true;
	}

	public boolean handler_requst_opreate(int seat_index, int opreate_type, int chat_index) {
		
		/*switch (opreate_type) {
		case GAME_OPREATE_TYPE_LOOK_DUN: {
			send_liang_dun(seat_index);
			return true;
		}
		case GAME_OPREATE_TYPE_MING_PAI_REQUEST: {
			deal_ming_pai_request(seat_index);
			return true;
		}
		case GAME_OPREATE_TYPE_MING_PAI_AGREE: {
			deal_ming_pai_agree(seat_index);
			return true;
		}
		case GAME_OPREATE_TYPE_MING_PAI_REJECT: {
			deal_ming_pai_reject(seat_index);
			return true;
		}
		case GAME_OPREATE_TYPE_MING_PAI_OUR_SELF: {
			deal_ming_pai_us(seat_index);
			return true;
		}
		case GAME_OPREATE_TYPE_MING_PAI_OTHER: {
			deal_ming_pai_other(seat_index);
			return true;
		}
		case GAME_OPREATE_TYPE_ASK: {
			deal_message_switch(1, chat_index, seat_index);
			return true;
		}
		case GAME_OPREATE_TYPE_ANSER: {
			deal_message_switch(2, chat_index, seat_index);
			return true;
		}
		}*/
		return true;
	}

	// 聊天信息转发
	public void deal_message_switch(int type, int chat_index, int seat_index) {
		/*RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(ZhaDanMsgConstants.RESPONSE_NCST_MESSAGE_SWITCH);
		// 发送数据
		ChatResponse_Ncgz.Builder message_switch = ChatResponse_Ncgz.newBuilder();
		message_switch.setType(type);
		message_switch.setChatIndex(chat_index);
		message_switch.setSeatIndex(seat_index);
		roomResponse.setCommResponse(PBUtil.toByteString(message_switch));
		// 自己才有牌数据
		this.send_response_to_room(roomResponse);*/
	}

	public void deal_ming_pai_other(int seat_index) {
		int team = this._seat_team[seat_index];
		int friend_seat = GameConstants.INVALID_SEAT;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (_is_ming_pai[i] == 1) {
				return;
			}
			if (seat_index == i) {
				continue;
			}
			if (_seat_team[i] == team) {
				friend_seat = i;
				break;
			}
		}
		if (friend_seat == GameConstants.INVALID_SEAT) {
			return;
		}
		if (this._ming_pai_agree[seat_index] != 1 || this._ming_pai_request[friend_seat] != 1) {
			return;
		}
		this._is_ming_pai[friend_seat] = 1;
		send_ming_pai_sure_anser(friend_seat);
		this.RefreshCard(GameConstants.INVALID_SEAT);
	}

	public void deal_ming_pai_us(int seat_index) {
		int team = this._seat_team[seat_index];
		int friend_seat = GameConstants.INVALID_SEAT;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (_is_ming_pai[i] == 1) {
				return;
			}
			if (seat_index == i) {
				continue;
			}
			if (_seat_team[i] == team) {
				friend_seat = i;
				break;
			}
		}
		if (friend_seat == GameConstants.INVALID_SEAT) {
			return;
		}
		if (this._ming_pai_agree[seat_index] != 1 || this._ming_pai_request[friend_seat] != 1) {
			return;
		}
		this._is_ming_pai[seat_index] = 1;
		send_ming_pai_sure_anser(seat_index);
		this.RefreshCard(GameConstants.INVALID_SEAT);
	}

	public void send_ming_pai_anser(int seat_index, int type, int to_player) {

		/*RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(ZhaDanMsgConstants.RESPONSE_NCST_MING_PAI_ANSER);
		// 发送数据
		MingPaiAnser_Ncgz.Builder ming_pai_anser = MingPaiAnser_Ncgz.newBuilder();
		ming_pai_anser.setSeatIndex(seat_index);
		if (type == 1) {
			ming_pai_anser.setStr("亮");
			ming_pai_anser.setIsAgree(true);
		} else {
			ming_pai_anser.setStr("不亮");
			ming_pai_anser.setIsAgree(false);
		}

		roomResponse.setCommResponse(PBUtil.toByteString(ming_pai_anser));
		// 自己才有牌数据
		this.send_response_to_player(to_player, roomResponse);*/
	}

	public void send_ming_pai_sure(int to_player) {

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(ZhaDanMsgConstants.RESPONSE_NCST_MING_PAI_SURE);
		// 自己才有牌数据
		this.send_response_to_player(to_player, roomResponse);
	}

	public void send_ming_pai_sure_anser(int seat_index) {

		/*RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(ZhaDanMsgConstants.RESPONSE_NCST_MING_PAI_ANSER_ANSER);
		// 发送数据
		MingPaiSureAnser_Ncgz.Builder ming_pai_anser = MingPaiSureAnser_Ncgz.newBuilder();
		ming_pai_anser.setSeatIndex(seat_index);
		roomResponse.setCommResponse(PBUtil.toByteString(ming_pai_anser));
		// 自己才有牌数据
		GRR.add_room_response(roomResponse);
		this.send_response_to_room(roomResponse);*/
	}

	public void deal_ming_pai_reject(int seat_index) {
		if (this._game_status != GameConstants.GS_MJ_PLAY || GRR == null) {
			return;
		}
		int team = this._seat_team[seat_index];
		int friend_seat = GameConstants.INVALID_SEAT;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (_is_ming_pai[i] == 1) {
				return;
			}
			if (seat_index == i) {
				continue;
			}
			if (_seat_team[i] == team) {
				friend_seat = i;
				if (_ming_pai_request[i] == 0) {
					return;
				}
			}
		}
		_ming_pai_agree[seat_index] = 0;
		_ming_pai_request[friend_seat] = 0;
		send_ming_pai_anser(seat_index, _ming_pai_agree[seat_index], friend_seat);
		send_ming_pai_anser(seat_index, _ming_pai_agree[seat_index], seat_index);
		return;
	}

	public void deal_ming_pai_agree(int seat_index) {
		if (this._game_status != GameConstants.GS_MJ_PLAY || GRR == null) {
			return;
		}
		int team = this._seat_team[seat_index];
		int friend_seat = GameConstants.INVALID_SEAT;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (_is_ming_pai[i] == 1) {
				this.send_error_notify(seat_index, 2, "已经有人选择亮牌");
				return;
			}
			if (seat_index == i) {
				continue;
			}
			if (_ming_pai_agree[i] == 1) {
				this.send_error_notify(seat_index, 2, "已经有人选择亮牌");
				return;
			}
			if (_seat_team[i] == team) {
				friend_seat = i;
				if (_ming_pai_request[i] == 0) {
					return;
				}
			}
		}
		_ming_pai_agree[seat_index] = 1;
		send_ming_pai_anser(seat_index, _ming_pai_agree[seat_index], friend_seat);
		send_ming_pai_anser(seat_index, _ming_pai_agree[seat_index], seat_index);
		send_ming_pai_sure(seat_index);
		this.RefreshCard(GameConstants.INVALID_SEAT);
		return;
	}

	public void deal_ming_pai_request(int seat_index) {
		if (this._game_status != GameConstants.GS_MJ_PLAY || GRR == null || _ming_pai_request[seat_index] == 1) {
			return;
		}
		int team = this._seat_team[seat_index];
		int friend_seat = GameConstants.INVALID_SEAT;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (_is_ming_pai[i] == 1) {
				this.send_error_notify(seat_index, 2, "已经有人选择亮牌");
				return;
			}
			if (_ming_pai_agree[i] == 1) {
				this.send_error_notify(seat_index, 2, "已经有人选择亮牌");
				return;
			}
			if (seat_index == i) {
				continue;
			}
			if (_seat_team[i] == team) {
				friend_seat = i;
				if (_ming_pai_request[i] == 1) {

					return;
				}
			}
		}
		if (friend_seat == GameConstants.INVALID_SEAT) {
			return;
		}
		if (GRR._card_count[seat_index] == 0 || GRR._card_count[friend_seat] == 0) {
			return;
		}
		_ming_pai_request[seat_index] = 1;

		send_ming_pai_request(seat_index, friend_seat);
		send_ming_pai_request(seat_index, seat_index);
	}

	public void send_ming_pai_request(int seat_index, int to_player) {

		/*RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(ZhaDanMsgConstants.RESPONSE_NCST_MING_PAI_REQUEST);
		// 发送数据
		MingPaiRequest_Ncgz.Builder ming_pai = MingPaiRequest_Ncgz.newBuilder();
		ming_pai.setSeatIndex(seat_index);
		ming_pai.setStr("亮牌吗？");
		roomResponse.setCommResponse(PBUtil.toByteString(ming_pai));
		// 自己才有牌数据
		this.send_response_to_player(to_player, roomResponse);*/
	}

	public void deal_sort_card(int seat_index) {
		if (_game_status == GameConstants.GS_MJ_WAIT || _game_status == GameConstants.GS_MJ_FREE) {
			return;
		}
		if (_score_type[seat_index] == GameConstants.WSK_ST_CUSTOM) {
			_score_type[seat_index] = GameConstants.WSK_ST_ORDER;
		} else if (_score_type[seat_index] == GameConstants.WSK_ST_ORDER) {
			_score_type[seat_index] = GameConstants.WSK_ST_COUNT;
		} else if (_score_type[seat_index] == GameConstants.WSK_ST_COUNT) {
			_score_type[seat_index] = GameConstants.WSK_ST_CUSTOM;
		}

		this._logic.SortCardList(GRR._cards_data[seat_index], GRR._card_count[seat_index], _score_type[seat_index]);
		RefreshCard(seat_index);
	}

	public void RefreshCard(int to_player) {
		int team = -1;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (this._ming_pai_agree[i] == 1) {
				team = _seat_team[i];
				break;
			}
		}
		if (to_player == GameConstants.INVALID_SEAT) {

			for (int index = 0; index < this.getTablePlayerNumber(); index++) {
				RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
				roomResponse.setType(ZhaDanMsgConstants.RESPONSE_NCST_REFRESH_CARD);
				// 发送数据
				RefreshCardData_Ncgz.Builder refresh_card = RefreshCardData_Ncgz.newBuilder();
				for (int i = 0; i < this.getTablePlayerNumber(); i++) {
					Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
					if (index == i || this._is_ming_pai[i] == 1 || this._is_ming_pai[i] == 1
							|| (this.GRR._card_count[index] == 0 && this._seat_team[i] == this._seat_team[index])) {
						for (int j = 0; j < this.GRR._card_count[i]; j++) {
							cards_card.addItem(GRR._cards_data[i][j]);
						}
					} else if (_seat_team[i] == team && _seat_team[index] == team) {
						for (int j = 0; j < this.GRR._card_count[i]; j++) {
							cards_card.addItem(GRR._cards_data[i][j]);
						}
					}

					if (GRR._card_count[i] <= 11 || i == index) {
						refresh_card.addHandCardCount(GRR._card_count[i]);
					} else {
						refresh_card.addHandCardCount(-1);
					}
					refresh_card.addHandCardsData(cards_card);

				}

				roomResponse.setCommResponse(PBUtil.toByteString(refresh_card));
				// 自己才有牌数据
				this.send_response_to_player(index, roomResponse);
			}

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(ZhaDanMsgConstants.RESPONSE_NCST_REFRESH_CARD);
			// 发送数据
			RefreshCardData_Ncgz.Builder refresh_card = RefreshCardData_Ncgz.newBuilder();
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < this.GRR._card_count[i]; j++) {
					cards_card.addItem(GRR._cards_data[i][j]);
				}
				refresh_card.addHandCardsData(cards_card);
				refresh_card.addHandCardCount(GRR._card_count[i]);
			}

			roomResponse.setCommResponse(PBUtil.toByteString(refresh_card));
			GRR.add_room_response(roomResponse);
		} else {
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(ZhaDanMsgConstants.RESPONSE_NCST_REFRESH_CARD);
			// 发送数据
			RefreshCardData_Ncgz.Builder refresh_card = RefreshCardData_Ncgz.newBuilder();
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
				if (to_player == i || this._is_ming_pai[i] == 1 || this._is_ming_pai[i] == 1
						|| (this.GRR._card_count[to_player] == 0 && this._seat_team[i] == this._seat_team[to_player])) {
					for (int j = 0; j < this.GRR._card_count[i]; j++) {
						cards_card.addItem(GRR._cards_data[i][j]);
					}
				} else if (_seat_team[i] == team) {
					for (int j = 0; j < this.GRR._card_count[i]; j++) {
						cards_card.addItem(GRR._cards_data[i][j]);
					}
				}
				if (GRR._card_count[i] <= 11 || i == to_player) {
					refresh_card.addHandCardCount(GRR._card_count[i]);
				} else {
					refresh_card.addHandCardCount(-1);
				}
				refresh_card.addHandCardsData(cards_card);
			}

			roomResponse.setCommResponse(PBUtil.toByteString(refresh_card));
			// 自己才有牌数据
			this.send_response_to_player(to_player, roomResponse);
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

		return true;
	}

	/**
	 * @param get_seat_index
	 * @param out_cards[]
	 * @param card_count
	 * @param b_out_card
	 * @return
	 */
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
	
	public boolean cel_boom_socre(int card_data[],int card_count,int seat_index){
		if(card_count < 3){
			return false;
		}
		
		boolean is_king = false;
		is_king = this._logic.GetCardColor(card_data[0]) == 4;
		
		//王墩
		if(is_king){
			int da_king = 0;
			int xiao_king = 0;
			for(int i = 0;i < card_count;i++){
				if(card_data[i] == 0x4e){
					da_king++;
				}else if(card_data[i] == 0x4f){
					xiao_king++;
				}
			}
			int base_score = 0;
			int more = da_king > xiao_king ? da_king : xiao_king;
			int other_king = card_count - more;
			if(more <= 2){
				if(card_count == 4){//2+2
					base_score = 30;
				}else{
					base_score = 0;
				}
			}else{
				base_score = (other_king + ((more - 3) * 2 + 1)) * 30;
			}
			
			for(int i = 0;i < this.getTablePlayerNumber();i++){
				if(i == seat_index){
					continue;
				}
				this._wang_score[i] -= base_score;
				this._wang_score[seat_index] += base_score;
			}
		}else{
			if(card_count < 7){
				return true;
			}
			int dun = card_count - 6;
			this._dun_shu[seat_index] += dun;
			
			for(int i = 0;i < this.getTablePlayerNumber();i++){
				if(i == seat_index){
					continue;
				}
				this._jiang_score[i] -= dun * 30;
				this._jiang_score[seat_index] += dun * 30;
			}
			
		}
		
		for(int i = 0;i < this.getTablePlayerNumber();i++){
			_tmp_wang_score[i] = _wang_score[i];
			_tmp_jiang_score[i] = _jiang_score[i];
		}
		
		return true;
	}
}
