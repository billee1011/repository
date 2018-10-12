package com.cai.game.mj.sichuan.xueliuchenghe;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.mj.Constants_SiChuan;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.Player;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.WeaveItem;
import com.cai.common.util.PBUtil;
import com.cai.common.util.RandomUtil;
import com.cai.game.mj.MJAIGameLogic;
import com.cai.game.mj.MJType;
import com.cai.game.mj.MahjongUtils;
import com.cai.game.mj.ThreeDimension;
import com.cai.game.mj.sichuan.AbstractSiChuanMjTable;
import com.cai.game.mj.sichuan.ScoreRowType;
import com.cai.game.util.AnalyseCardUtil;
import com.cai.game.util.GameUtilConstants;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.PlayerResultResponse;
import protobuf.clazz.Protocol.RoomPlayerResponse;
import protobuf.clazz.Protocol.RoomRequest;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.mj.Basic.MJ_Game_End_Basic;
import protobuf.clazz.mj.Basic.ScoreRow;

@ThreeDimension
public class Table_XueLiuChengHe extends AbstractSiChuanMjTable {
	private static final long serialVersionUID = 1L;

	public HandlerSwitchCard_XueLiuChengHe _handler_switch_card;
	public HandlerDingQue_XueLiuChengHe _handler_ding_que;

	public Table_XueLiuChengHe(MJType type) {
		super(type);
	}

	@Override
	public boolean handler_requst_pao_qiang(Player player, int pai_se, int other) {
		return _handler_ding_que.handler_ding_que(this, player.get_seat_index(), pai_se);
	}

	@Override
	public boolean exe_switch_card() {
		set_handler(_handler_switch_card);
		_handler.exe(this);
		return true;
	}

	@Override
	public boolean exe_ding_que() {
		set_handler(_handler_ding_que);
		_handler.exe(this);
		return true;
	}

	@Override
	public boolean handler_requst_message_deal(Player player, int seat_index, RoomRequest room_rq, int type) {
		if (type == MsgConstants.REQUEST_SWITCH_CARDS) {
			int size = room_rq.getOutCardsList().size();
			int[] cards = new int[size];

			for (int i = 0; i < size; i++) {
				cards[i] = room_rq.getOutCardsList().get(i);
			}

			_handler_switch_card.handler_switch_cards(this, seat_index, cards);
		} else if (type == MsgConstants.REQUEST_SC_MJ_LIU_SHUI) {
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_SC_SHOW_SCORE_DETAIL);

			MJ_Game_End_Basic.Builder basic_game_end = MJ_Game_End_Basic.newBuilder();

			// 添加流水明显
			for (int i = 0; i < scoreDetails.size(); i++) {
				ScoreRow.Builder scoreRow = ScoreRow.newBuilder();
				int[] row = scoreDetails.get(i);
				if (row.length == SCORE_DETAIL_COLUMN) {
					scoreRow.setType(row[0]);
					scoreRow.setPScore1(row[1]);
					scoreRow.setPScore2(row[2]);
					scoreRow.setPScore3(row[3]);
					scoreRow.setPScore4(row[4]);
				}
				basic_game_end.addAllScoreDetails(scoreRow);
			}

			roomResponse.setCommResponse(PBUtil.toByteString(basic_game_end));

			send_response_to_player(seat_index, roomResponse);
		}

		return true;
	}

	@Override
	public void load_player_info_data(RoomResponse.Builder roomResponse) {
		MahjongUtils.dealScheduleCounter(this);

		Player rplayer;
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			rplayer = get_players()[i];
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

			// 初始分
			room_player.setScore(player_left_score[i]);

			room_player.setReady(_player_ready[i]);

			// 玩家定缺的牌色，1，万，2，条，3，筒，0，还没选定缺
			room_player.setPao(null != ding_que_pai_se ? ding_que_pai_se[i] : 0);
			// 玩家胡牌的顺序，1，2，3
			room_player.setNao(null != win_order ? win_order[i] : 0);
			// 玩家胡牌的类型，1，自摸，2，胡
			room_player.setQiang(null != win_type ? win_type[i] : 0);
			// 玩家是否已经换三张，1，是，2，否，3，没换三张玩法
			if (!has_rule(Constants_SiChuan.GAME_RULE_HUAN_SAN_ZHANG)) {
				room_player.setBiaoyan(3);
			} else {
				room_player.setBiaoyan((null != had_switch_card) ? (had_switch_card[i] ? 1 : 2) : 2);
			}

			room_player.setMoney(rplayer.getMoney());
			room_player.setGold(rplayer.getGold());

			// 是否是无叫状态 1表示有叫 0表示无叫 为0时 显示无叫
			room_player.setHasPiao(is_ting_when_finish[i] == false ? 0 : 1);

			// 玩家底分
			room_player.setZiBa(player_basic_score[i]);

			// 玩家本局小计
			room_player.setDuanMen(small_round_total_score[i]);

			room_player.setOpenThree(_player_open_less[i] == 0 ? false : true);

			// 超时托管
			if (over_time_trustee != null) {
				room_player.setIsTrustee(over_time_trustee[i]);
			} else {
				room_player.setIsTrustee(false);
			}

			// 玩家超时托管的读秒时间，如果是-1表示不读秒，如果是大于0就表示是读秒
			if (over_time_left != null) {
				room_player.setQiangDiZhu(over_time_left[i]);
			} else {
				room_player.setQiangDiZhu(-1);
			}

			// 是否已经胡牌，1表示已经有胡牌，0表示还没有胡牌
			room_player.setJiaoDiZhu(had_hu_pai[i] ? 1 : 0);

			if (rplayer.locationInfor != null) {
				room_player.setLocationInfor(rplayer.locationInfor);
			}

			roomResponse.addPlayers(room_player);
		}
	}

	@Override
	public PlayerResultResponse.Builder process_player_result(int result) {
		huan_dou(result);

		int pCount = getTablePlayerNumber();

		for (int i = 0; i < pCount; i++) {
			_player_result.win_order[i] = -1;
		}

		int win_idx = 0;
		float max_score = 0;

		for (int i = 0; i < pCount; i++) {
			int winner = -1;
			float s = -999999;
			for (int j = 0; j < pCount; j++) {
				if (_player_result.win_order[j] != -1) {
					continue;
				}
				if (player_left_score[j] > s) {
					s = player_left_score[j];
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
			}
		}

		PlayerResultResponse.Builder player_result = PlayerResultResponse.newBuilder();

		for (int i = 0; i < pCount; i++) {
			player_result.addWinOrder(_player_result.win_order[i]);

			Int32ArrayResponse.Builder lfs = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < pCount; j++) {
				lfs.addItem(_player_result.lost_fan_shu[i][j]);
			}

			player_result.addLostFanShu(lfs);

			// 坐庄次数
			player_result.addPiaoLaiCount(player_banker_count[i]);
			// 胡牌次数
			player_result.addZiMoCount(_player_result.zi_mo_count[i] + _player_result.jie_pao_count[i]);
			// 点炮次数
			player_result.addDianPaoCount(_player_result.dian_pao_count[i]);
			// 超时次数
			player_result.addHaiDiCount(player_over_time_count[i]);
			// 总成绩
			player_result.addGameScore(player_left_score[i] - getStart_score());
		}

		player_result.setRoomId(getRoom_id());
		player_result.setRoomOwnerAccountId(getRoom_owner_account_id());
		player_result.setRoomOwnerName(getRoom_owner_name());
		player_result.setCreateTime(getCreate_time());
		player_result.setRecordId(get_record_id());
		player_result.setGameRound(_game_round);
		player_result.setGameRuleDes(get_game_des());
		player_result.setGameRuleIndex(_game_rule_index);
		player_result.setGameTypeIndex(_game_type_index);

		RoomPlayerResponse.Builder room_player = RoomPlayerResponse.newBuilder();
		room_player.setAccountId(getCreate_player().getAccount_id());
		room_player.setHeadImgUrl(getCreate_player().getAccount_icon());
		room_player.setIp(getCreate_player().getAccount_ip());
		room_player.setUserName(getCreate_player().getNick_name());
		room_player.setSeatIndex(getCreate_player().get_seat_index());
		room_player.setOnline(getCreate_player().isOnline() ? 1 : 0);
		room_player.setIpAddr(getCreate_player().getAccount_ip_addr());
		room_player.setSex(getCreate_player().getSex());
		room_player.setScore(0);
		room_player.setReady(0);
		room_player.setPao(0);
		room_player.setQiang(0);

		if (getCreate_player().locationInfor != null) {
			room_player.setLocationInfor(getCreate_player().locationInfor);
		}
		player_result.setCreatePlayer(room_player);

		return player_result;
	}

	@Override
	public boolean handler_request_trustee(int get_seat_index, boolean isTrustee, int Trustee_type) {
		if (_playerStatus == null || over_time_trustee == null || _game_status == GameConstants.GS_MJ_FREE
				|| _game_status == GameConstants.GS_MJ_WAIT) {
			send_error_notify(get_seat_index, 2, "游戏还未开始,无法托管!");
			return false;
		}

		over_time_trustee[get_seat_index] = isTrustee;
		over_time_left[get_seat_index] = -1;

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_IS_TRUSTEE);
		roomResponse.setOperatePlayer(get_seat_index);
		roomResponse.setIstrustee(isTrustee);
		load_player_info_data(roomResponse);
		send_response_to_room(roomResponse);

		if (GRR != null) {
			GRR.add_room_response(roomResponse);
		}

		if (is_match() || isCoinRoom() || isClubMatch()) {
			if (istrustee[get_seat_index] && _current_player == get_seat_index) {
				if (_playerStatus[get_seat_index].get_status() == GameConstants.Player_Status_OUT_CARD) {
					int card = MJAIGameLogic.get_card(this, get_seat_index);
					if (card != 0) {
						handler_player_out_card(get_seat_index, card);
					}
				} else if (_playerStatus[get_seat_index].get_status() == GameConstants.Player_Status_OPR_CARD) {
					MJAIGameLogic.AI_Operate_Card(this, get_seat_index);
				}
			} else if (istrustee[get_seat_index]) {
				if (_playerStatus[get_seat_index].get_status() == GameConstants.Player_Status_OPR_CARD) {
					MJAIGameLogic.AI_Operate_Card(this, get_seat_index);
				}
			}
		}

		if (_handler != null && isTrustee) {
			player_over_time_count[get_seat_index]++;
			_handler.handler_be_set_trustee(this, get_seat_index);
		}

		return true;
	}

	@Override
	public boolean estimate_gang_respond(int seat_index, int card) {
		boolean bAroseAction = false;

		PlayerStatus playerStatus = null;

		int action = GameConstants.WIK_NULL;

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (seat_index == i)
				continue;

			playerStatus = _playerStatus[i];

			if (playerStatus.is_chi_hu_round() && no_score_left[i] == false) {
				ChiHuRight chr = GRR._chi_hu_rights[i];
				chr.set_empty();

				analyse_state = FROM_NORMAL;
				action = analyse_chi_hu_card(GRR._cards_index[i], GRR._weave_items[i], GRR._weave_count[i], card, chr,
						Constants_SiChuan.HU_CARD_TYPE_QIANG_GANG, i);

				if (action != 0) {
					_playerStatus[i].add_action(GameConstants.WIK_CHI_HU);
					_playerStatus[i].add_chi_hu(card, seat_index);
					bAroseAction = true;
				}
			}
		}

		if (bAroseAction == true) {
			_provide_player = seat_index;
			_provide_card = card;
			_resume_player = _current_player;
			_current_player = GameConstants.INVALID_SEAT;
		}

		return bAroseAction;
	}

	@Override
	public void show_tou_zi(int seat_index) {
		tou_zi_dian_shu[0] = RandomUtil.getRandomNumber(Integer.MAX_VALUE) % 6 + 1;
		tou_zi_dian_shu[1] = 0;

		operate_tou_zi_effect(tou_zi_dian_shu[0], tou_zi_dian_shu[1], time_for_tou_zi_animation, time_for_tou_zi_fade);
	}

	@Override
	protected void onInitTable() {
		super.onInitTable();

		_handler_chi_peng = new HandlerChiPeng_XueLiuChengHe();
		_handler_dispath_card = new HandlerDispatchCard_XueLiuChengHe();
		_handler_gang = new HandlerGang_XueLiuChengHe();
		_handler_out_card_operate = new HandlerOutCardOperate_XueLiuChengHe();

		_handler_switch_card = new HandlerSwitchCard_XueLiuChengHe();
		_handler_ding_que = new HandlerDingQue_XueLiuChengHe();

		_handler_qi_shou = new HandlerQiShou_XueLiuChengHe();
	}

	@Override
	public void process_gang_score() {
	}

	public int get_score_row_type_cha_jiao(ChiHuRight chr) {
		if (!chr.opr_and_long(Constants_SiChuan.CHR_PING_HU).is_empty()) {
			return ScoreRowType.CHA_JIAO_PING_HU.getType();
		}
		if (!chr.opr_and_long(Constants_SiChuan.CHR_DUI_DUI_HU).is_empty()) {
			return ScoreRowType.CHA_JIAO_PENG_PENG_HU.getType();
		}
		if (!chr.opr_and_long(Constants_SiChuan.CHR_QING_YI_SE).is_empty()) {
			return ScoreRowType.CHA_JIAO_QING_YI_SE.getType();
		}
		if (!chr.opr_and_long(Constants_SiChuan.CHR_PENG_PENG_DIAO).is_empty()) {
			return ScoreRowType.CHA_JIAO_PENG_PENG_DIAO.getType();
		}
		if (!chr.opr_and_long(Constants_SiChuan.CHR_YAO_JIU).is_empty()) {
			return ScoreRowType.CHA_JIAO_DAI_YAO_JIU.getType();
		}
		if (!chr.opr_and_long(Constants_SiChuan.CHR_QI_DUI).is_empty()) {
			return ScoreRowType.CHA_JIAO_QI_DUI.getType();
		}
		if (!chr.opr_and_long(Constants_SiChuan.CHR_QING_PENG_HU).is_empty()) {
			return ScoreRowType.CHA_JIAO_QING_PENG_PENG_HU.getType();
		}
		if (!chr.opr_and_long(Constants_SiChuan.CHR_QING_PENG_DIAO).is_empty()) {
			return ScoreRowType.CHA_JIAO_QING_PENG_PENG_DIAO.getType();
		}
		if (!chr.opr_and_long(Constants_SiChuan.CHR_QING_QI_DUI).is_empty()) {
			return ScoreRowType.CHA_JIAO_QING_QI_DUI.getType();
		}
		return ScoreRowType.CHA_JIAO_PING_HU.getType();
	}

	public int get_score_row_type_zi_mo(ChiHuRight chr) {
		if (!chr.opr_and_long(Constants_SiChuan.CHR_PING_HU).is_empty()) {
			return ScoreRowType.ZI_MO_PING_HU.getType();
		}
		if (!chr.opr_and_long(Constants_SiChuan.CHR_DUI_DUI_HU).is_empty()) {
			return ScoreRowType.ZI_MO_PENG_PENG_HU.getType();
		}
		if (!chr.opr_and_long(Constants_SiChuan.CHR_QING_YI_SE).is_empty()) {
			return ScoreRowType.ZI_MO_QING_YI_SE.getType();
		}
		if (!chr.opr_and_long(Constants_SiChuan.CHR_PENG_PENG_DIAO).is_empty()) {
			return ScoreRowType.ZI_MO_PENG_PENG_DIAO.getType();
		}
		if (!chr.opr_and_long(Constants_SiChuan.CHR_YAO_JIU).is_empty()) {
			return ScoreRowType.ZI_MO_DAI_YAO_JIU.getType();
		}
		if (!chr.opr_and_long(Constants_SiChuan.CHR_QI_DUI).is_empty()) {
			return ScoreRowType.ZI_MO_QI_DUI.getType();
		}
		if (!chr.opr_and_long(Constants_SiChuan.CHR_QING_PENG_HU).is_empty()) {
			return ScoreRowType.ZI_MO_QING_PENG_PENG_HU.getType();
		}
		if (!chr.opr_and_long(Constants_SiChuan.CHR_QING_PENG_DIAO).is_empty()) {
			return ScoreRowType.ZI_MO_QING_PENG_PENG_DIAO.getType();
		}
		if (!chr.opr_and_long(Constants_SiChuan.CHR_QING_QI_DUI).is_empty()) {
			return ScoreRowType.ZI_MO_QING_QI_DUI.getType();
		}
		if (!chr.opr_and_long(Constants_SiChuan.CHR_TIAN_HU).is_empty()) {
			return ScoreRowType.ZI_MO_TIAN_HU.getType();
		}
		return ScoreRowType.ZI_MO_PING_HU.getType();
	}

	public int get_score_row_type_jie_pao(ChiHuRight chr) {
		if (!chr.opr_and_long(Constants_SiChuan.CHR_PING_HU).is_empty()) {
			return ScoreRowType.JIE_PAO_PING_HU.getType();
		}
		if (!chr.opr_and_long(Constants_SiChuan.CHR_DUI_DUI_HU).is_empty()) {
			return ScoreRowType.JIE_PAO_PENG_PENG_HU.getType();
		}
		if (!chr.opr_and_long(Constants_SiChuan.CHR_QING_YI_SE).is_empty()) {
			return ScoreRowType.JIE_PAO_QING_YI_SE.getType();
		}
		if (!chr.opr_and_long(Constants_SiChuan.CHR_PENG_PENG_DIAO).is_empty()) {
			return ScoreRowType.JIE_PAO_PENG_PENG_DIAO.getType();
		}
		if (!chr.opr_and_long(Constants_SiChuan.CHR_YAO_JIU).is_empty()) {
			return ScoreRowType.JIE_PAO_DAI_YAO_JIU.getType();
		}
		if (!chr.opr_and_long(Constants_SiChuan.CHR_QI_DUI).is_empty()) {
			return ScoreRowType.JIE_PAO_QI_DUI.getType();
		}
		if (!chr.opr_and_long(Constants_SiChuan.CHR_QING_PENG_HU).is_empty()) {
			return ScoreRowType.JIE_PAO_QING_PENG_PENG_HU.getType();
		}
		if (!chr.opr_and_long(Constants_SiChuan.CHR_QING_PENG_DIAO).is_empty()) {
			return ScoreRowType.JIE_PAO_QING_PENG_PENG_DIAO.getType();
		}
		if (!chr.opr_and_long(Constants_SiChuan.CHR_QING_QI_DUI).is_empty()) {
			return ScoreRowType.JIE_PAO_QING_QI_DUI.getType();
		}
		if (!chr.opr_and_long(Constants_SiChuan.CHR_DI_HU).is_empty()) {
			return ScoreRowType.JIE_PAO_DI_HU.getType();
		}
		return ScoreRowType.JIE_PAO_PING_HU.getType();
	}

	@Override
	public void process_show_hand_card() {
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			int cards[] = new int[GameConstants.MAX_COUNT];
			int hand_card_count = _logic.switch_to_cards_data_sichuan(GRR._cards_index[i], cards, ding_que_pai_se[i]);

			operate_show_card(i, GameConstants.Show_Card_HU, hand_card_count, cards, GameConstants.INVALID_SEAT);
		}
	}

	/**
	 * 查大叫和查花猪，流局时，没听牌的玩家，配付给听牌玩家，最大的牌型分，胡牌了的，不用管
	 */
	@Override
	public void cha_da_jiao() {
		analyse_state = FROM_MAX_COUNT;

		int[] max_pai_xing_fen = new int[getTablePlayerNumber()];
		boolean[] is_ting_state = new boolean[getTablePlayerNumber()];

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			max_pai_xing_fen[i] = -1;

			is_ting_state[i] = is_ting_state(GRR._cards_index[i], GRR._weave_items[i], GRR._weave_count[i], i);

			if (is_ting_state[i]) {
				// 获取最大牌型分
				max_pai_xing_fen[i] = get_max_pai_xing_fen(i);
			} else {
				is_ting_when_finish[i] = false;
			}
		}

		// 赔付
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (is_ting_state[i] == false) {
				for (int p = 0; p < getTablePlayerNumber() && player_left_score[i] > 0; p++) {
					int j = (i + p) % getTablePlayerNumber();

					if (i == j)
						continue;

					if (is_ting_state[j] == false)
						continue;

					int nScore = max_pai_xing_fen[j] + player_basic_score[j] + (player_basic_score[i] - BASIC_SCORE);
					nScore = player_left_score[i] >= nScore ? nScore : player_left_score[i];

					int[] row = new int[SCORE_DETAIL_COLUMN];
					row[0] = cha_jiao_pai_xing_type[j];

					row[j + 1] += nScore;
					row[i + 1] -= nScore;

					scoreDetails.add(row);

					GRR._game_score[j] += nScore;
					GRR._game_score[i] -= nScore;

					player_left_score[j] += nScore;
					player_left_score[i] -= nScore;

					small_round_total_score[j] += nScore;
					small_round_total_score[i] -= nScore;
				}
			}
		}

		// 查花猪
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (had_hu_pai[i])
				continue;

			if (is_ting_state[i])
				continue;

			int color = _logic.get_se_count(GRR._cards_index[i], GRR._weave_items[i], GRR._weave_count[i]);

			if (color != 3)
				continue;

			for (int p = 0; p < getTablePlayerNumber() && player_left_score[i] > 0; p++) {
				int j = (i + p) % getTablePlayerNumber();

				if (i == j)
					continue;

				color = _logic.get_se_count(GRR._cards_index[j], GRR._weave_items[j], GRR._weave_count[j]);

				if (color != 3) {
					int score = 16 + player_basic_score[j];
					score = player_left_score[i] >= score ? score : player_left_score[i];

					int[] row = new int[SCORE_DETAIL_COLUMN];
					row[0] = ScoreRowType.CHA_HUA_ZHU.getType();

					row[j + 1] += score;
					row[i + 1] -= score;

					scoreDetails.add(row);

					GRR._game_score[j] += score;
					GRR._game_score[i] -= score;

					player_left_score[j] += score;
					player_left_score[i] -= score;

					small_round_total_score[j] += score;
					small_round_total_score[i] -= score;
				}
			}
		}
	}

	@Override
	public int get_max_pai_xing_fen(int seat_index) {
		int max_score = -1;

		int cbCurrentCard = -1;

		for (int i = 0; i < GameConstants.MAX_ZI; i++) {
			cbCurrentCard = _logic.switch_to_card_data(i);

			// 如果当前牌，不是听牌数据里的数据。这样能节省很多时间。
			if (!is_ting_card(cbCurrentCard, seat_index)) {
				continue;
			}

			ChiHuRight chr = new ChiHuRight();
			chr.set_empty();

			int card_type = Constants_SiChuan.HU_CARD_TYPE_JIE_PAO;

			boolean flag = false;
			if (GRR._cards_index[seat_index][i] == 5) {
				flag = true;
				GRR._cards_index[seat_index][i] = 2;
			}

			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card(GRR._cards_index[seat_index], GRR._weave_items[seat_index],
					GRR._weave_count[seat_index], cbCurrentCard, chr, card_type, seat_index)) {
				if (flag) {
					GRR._cards_index[seat_index][i] = 5;
				}

				int score = get_fan_shu(chr);

				if (score > max_score) {
					cha_jiao_pai_xing_type[seat_index] = get_score_row_type_cha_jiao(chr);
					max_score = score;
					finallyFanShu[seat_index] = score;
				}
			}
		}

		return max_score;
	}

	@Override
	public int analyse_chi_hu_card(int[] cards_index, WeaveItem[] weaveItems, int weave_count, int cur_card, ChiHuRight chiHuRight, int card_type,
			int _seat_index) {
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		if (cur_card >= 0x01 && cur_card <= 0x29) {
			cbCardIndexTemp[_logic.switch_to_card_index(cur_card)]++;
		}

		int temp_cards[] = new int[GameConstants.MAX_COUNT];
		int temp_hand_card_count = _logic.switch_to_cards_data_sichuan(cbCardIndexTemp, temp_cards, ding_que_pai_se[_seat_index]);

		for (int i = 0; i < temp_hand_card_count; i++) {
			int pai_se = _logic.get_card_color(temp_cards[i]);
			if ((pai_se + 1) == ding_que_pai_se[_seat_index]) {
				// 手牌里有定缺的牌色的牌，不能胡牌
				return 0;
			}
		}

		int[] magic_cards_index = new int[GameUtilConstants.MAX_MAGIC_INDEX_COUNT];

		// 平胡
		boolean can_win_ping_hu = AnalyseCardUtil.analyse_win_by_cards_index(cbCardIndexTemp, -1, magic_cards_index, 0);

		long qi_dui = analyse_qi_xiao_dui(cbCardIndexTemp, weave_count);
		// 七对
		boolean can_win_qi_dui = (qi_dui != 0);

		if (!can_win_ping_hu && !can_win_qi_dui) {
			return 0;
		}

		// 清一色
		boolean can_win_qing_yi_se = _logic.is_qing_yi_se_qishou(cbCardIndexTemp, weaveItems, weave_count);

		// 碰碰胡
		boolean can_win_peng_peng_hu = AnalyseCardUtil.analyse_peng_hu_by_cards_index(cbCardIndexTemp, -1, magic_cards_index, 0);

		// 带幺九
		boolean can_win_dai_yao_jiu = AnalyseCardUtil.analyse_win_yao_jiu(cbCardIndexTemp, -1, magic_cards_index, 0)
				&& is_yao_jiu_weave(GRR._weave_items[_seat_index], GRR._weave_count[_seat_index]);

		// 天胡
		boolean can_win_tian_hu = (_out_card_count == 0 && _seat_index == GRR._banker_player) && (analyse_state != FROM_TING);

		// 地胡
		boolean can_win_di_hu = (weave_count == 0 && _out_card_count == 1 && _out_card_player == GRR._banker_player
				&& _seat_index != GRR._banker_player && card_type == Constants_SiChuan.HU_CARD_TYPE_JIE_PAO);

		// 一条龙
		boolean can_win_yi_tiao_long = _logic.is_yi_tiao_long(cbCardIndexTemp, weave_count);
		if (can_win_yi_tiao_long) {
			chiHuRight.opr_or_long(Constants_SiChuan.CHR_YI_TIAO_LONG);
		}

		// 钓
		boolean can_win_diao = (_logic.get_card_count_by_index(cbCardIndexTemp) == 2);

		// 海底
		if (GRR._left_card_count <= LEFT_CARD && analyse_state != FROM_MAX_COUNT) {
			chiHuRight.opr_or_long(Constants_SiChuan.CHR_HAI_DI);
		}

		if (card_type == Constants_SiChuan.HU_CARD_TYPE_GANG_KAI) {
			chiHuRight.opr_or_long(Constants_SiChuan.CHR_GANG_KAI);
		} else if (card_type == Constants_SiChuan.HU_CARD_TYPE_GANG_PAO) {
			chiHuRight.opr_or_long(Constants_SiChuan.CHR_GANG_PAO);
		} else if (card_type == Constants_SiChuan.HU_CARD_TYPE_QIANG_GANG) {
			chiHuRight.opr_or_long(Constants_SiChuan.CHR_QIANG_GANG);
		} else if (card_type == Constants_SiChuan.HU_CARD_TYPE_ZI_MO && analyse_state != FROM_MAX_COUNT) {
			chiHuRight.opr_or_long(Constants_SiChuan.CHR_ZI_MO);
		} else if (card_type == Constants_SiChuan.HU_CARD_TYPE_JIE_PAO) {
			chiHuRight.opr_or_long(Constants_SiChuan.CHR_JIE_PAO);
		}

		boolean need_to_continue = true;

		if (need_to_continue && getRuleValue(Constants_SiChuan.GAME_RULE_TIAN_DI_HU_16) != 0 && (can_win_tian_hu || can_win_di_hu)) {
			boolean can_tian_di_hu = false;

			if (can_win_ping_hu) {
				can_tian_di_hu = true;
			} else if (can_win_qi_dui) {
				if (getRuleValue(Constants_SiChuan.GAME_RULE_QI_DUI_4) != 0 && can_win_qi_dui) {
					can_tian_di_hu = true;
				} else if (getRuleValue(Constants_SiChuan.GAME_RULE_QING_QI_DUI_8) != 0 && can_win_qing_yi_se && can_win_qi_dui) {
					can_tian_di_hu = true;
				}
			}

			if (can_tian_di_hu) {
				if (can_win_tian_hu)
					chiHuRight.opr_or_long(Constants_SiChuan.CHR_TIAN_HU);
				if (can_win_di_hu)
					chiHuRight.opr_or_long(Constants_SiChuan.CHR_DI_HU);
				need_to_continue = false;
			}
		}

		if (need_to_continue && getRuleValue(Constants_SiChuan.GAME_RULE_QING_QI_DUI_8) != 0 && can_win_qing_yi_se && can_win_qi_dui) {
			chiHuRight.opr_or_long(Constants_SiChuan.CHR_QING_QI_DUI);
			need_to_continue = false;
		}

		if (need_to_continue && getRuleValue(Constants_SiChuan.GAME_RULE_QING_PENG_PENG_DIAO_8) != 0 && can_win_qing_yi_se && can_win_peng_peng_hu
				&& can_win_diao) {
			chiHuRight.opr_or_long(Constants_SiChuan.CHR_QING_PENG_DIAO);
			need_to_continue = false;
		}

		if (need_to_continue && getRuleValue(Constants_SiChuan.GAME_RULE_QING_PENG_PENG_HU_6) != 0 && can_win_qing_yi_se && can_win_peng_peng_hu) {
			chiHuRight.opr_or_long(Constants_SiChuan.CHR_QING_PENG_HU);
			need_to_continue = false;
		}

		if (need_to_continue && getRuleValue(Constants_SiChuan.GAME_RULE_QING_YI_SE_4) != 0 && can_win_qing_yi_se && can_win_ping_hu) {
			chiHuRight.opr_or_long(Constants_SiChuan.CHR_QING_YI_SE);
			need_to_continue = false;
		}

		if (need_to_continue && getRuleValue(Constants_SiChuan.GAME_RULE_DAI_YAO_JIU_4) != 0 && can_win_dai_yao_jiu && can_win_ping_hu) {
			chiHuRight.opr_or_long(Constants_SiChuan.CHR_YAO_JIU);
			need_to_continue = false;
		}

		if (need_to_continue && getRuleValue(Constants_SiChuan.GAME_RULE_PENG_PENG_DIAO_4) != 0 && can_win_peng_peng_hu && can_win_diao) {
			chiHuRight.opr_or_long(Constants_SiChuan.CHR_PENG_PENG_DIAO);
			need_to_continue = false;
		}

		if (need_to_continue && getRuleValue(Constants_SiChuan.GAME_RULE_QI_DUI_4) != 0 && can_win_qi_dui) {
			chiHuRight.opr_or_long(Constants_SiChuan.CHR_QI_DUI);
			need_to_continue = false;
		}

		if (need_to_continue && getRuleValue(Constants_SiChuan.GAME_RULE_PENG_PENG_HU_2) != 0 && can_win_peng_peng_hu) {
			chiHuRight.opr_or_long(Constants_SiChuan.CHR_DUI_DUI_HU);
			need_to_continue = false;
		}

		if (need_to_continue) {
			if (can_win_ping_hu) {
				chiHuRight.opr_or_long(Constants_SiChuan.CHR_PING_HU);
			} else {
				chiHuRight.set_empty();
				return 0;
			}
		}

		return GameConstants.WIK_CHI_HU;
	}

	@Override
	public boolean estimate_player_out_card_respond(int seat_index, int card, int type) {
		boolean bAroseAction = false;

		int pai_se = _logic.get_card_color(card) + 1;

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_playerStatus[i].clean_action();
			_playerStatus[i].clean_weave();
		}

		PlayerStatus playerStatus = null;

		int action = GameConstants.WIK_NULL;

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (seat_index == i) {
				continue;
			}

			if (no_score_left[i]) {
				continue;
			}

			if (pai_se == ding_que_pai_se[i]) {
				continue;
			}

			playerStatus = _playerStatus[i];

			boolean can_peng_this_card = true;
			int[] tmp_cards_data = _playerStatus[i].get_cards_abandoned_peng();
			for (int x = 0; x < GameConstants.MAX_ZI_FENG; x++) {
				if (tmp_cards_data[x] == card) {
					can_peng_this_card = false;
					break;
				}
			}

			if (can_peng_this_card && !had_hu_pai[i]) {
				action = _logic.check_peng(GRR._cards_index[i], card);
				if (action != 0) {
					playerStatus.add_action(action);
					playerStatus.add_peng(card, seat_index);
					bAroseAction = true;
				}
			}

			if (GRR._left_card_count > LEFT_CARD) {
				if (had_hu_pai[i]) {
					boolean need_display_gang = true;
					int hu_card_count = _playerStatus[i]._hu_card_count;
					for (int y = 0; y < hu_card_count; y++) {
						if (card == _playerStatus[i]._hu_cards[y]) {
							need_display_gang = false;
							break;
						}
					}

					if (need_display_gang) {
						action = _logic.estimate_gang_card_out_card(GRR._cards_index[i], card);
						if (action != 0) {
							// 删除手牌并放入落地牌之前，保存状态数据信息
							int tmp_card_index = _logic.switch_to_card_index(card);
							int tmp_card_count = GRR._cards_index[i][tmp_card_index];
							int tmp_weave_count = GRR._weave_count[i];

							// 删除手牌并加入一个落地牌组合，别人出牌时，杠都是明杠，因为等下分析听牌时要用
							GRR._cards_index[i][tmp_card_index] = 0;
							GRR._weave_items[i][tmp_weave_count].public_card = 1;
							GRR._weave_items[i][tmp_weave_count].center_card = card;
							GRR._weave_items[i][tmp_weave_count].weave_kind = GameConstants.WIK_GANG;
							GRR._weave_items[i][tmp_weave_count].provide_player = seat_index;
							++GRR._weave_count[i];

							boolean has_huan_zhang = check_gang_huan_zhang(i, card);

							// 还原手牌数据和落地牌数据
							GRR._cards_index[i][tmp_card_index] = tmp_card_count;
							GRR._weave_count[i] = tmp_weave_count;

							// 杠牌之后还是听牌状态，并不需要在gang handler里更新听牌状态，只要出牌时更新就可以
							if (!has_huan_zhang) {
								playerStatus.add_action(GameConstants.WIK_GANG);
								playerStatus.add_gang(card, seat_index, 1);
								bAroseAction = true;
							}
						}
					}
				} else {
					action = _logic.estimate_gang_card_out_card(GRR._cards_index[i], card);
					if (action != 0) {
						playerStatus.add_action(GameConstants.WIK_GANG);
						playerStatus.add_gang(card, seat_index, 1);
						bAroseAction = true;
					}
				}
			}

			if (_playerStatus[i].is_chi_hu_round()) {
				ChiHuRight chr = GRR._chi_hu_rights[i];
				chr.set_empty();

				int hu_card_type = Constants_SiChuan.HU_CARD_TYPE_JIE_PAO;
				if (type == GameConstants.WIK_GANG)
					hu_card_type = Constants_SiChuan.HU_CARD_TYPE_GANG_PAO;

				analyse_state = FROM_NORMAL;
				action = analyse_chi_hu_card(GRR._cards_index[i], GRR._weave_items[i], GRR._weave_count[i], card, chr, hu_card_type, i);
				if (action != 0) {
					playerStatus.add_action(GameConstants.WIK_CHI_HU);
					playerStatus.add_chi_hu(card, seat_index);
					bAroseAction = true;
				}
			}
		}

		if (bAroseAction) {
			_resume_player = _current_player;
			_current_player = GameConstants.INVALID_SEAT;
			_provide_player = seat_index;
		}

		return bAroseAction;
	}

	@Override
	public int get_ting_card(int[] cards, int[] cards_index, WeaveItem[] weaveItem, int cbWeaveCount, int seat_index, int ting_count) {
		analyse_state = FROM_TING;

		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		ChiHuRight chr = new ChiHuRight();

		int count = 0;
		int cbCurrentCard;

		int max_ting_count = GameConstants.MAX_ZI_FENG;

		for (int i = 0; i < max_ting_count; i++) {
			cbCurrentCard = _logic.switch_to_card_data(i);
			chr.set_empty();

			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card(cbCardIndexTemp, weaveItem, cbWeaveCount, cbCurrentCard, chr,
					Constants_SiChuan.HU_CARD_TYPE_JIE_PAO, seat_index)) {
				ting_pai_fan_shu[seat_index][ting_count][count] = get_fan_shu(chr) + player_basic_score[seat_index];

				cards[count] = cbCurrentCard;
				count++;
			}
		}

		if (count == 0) {
		} else if (count == max_ting_count) {
			count = 1;
			cards[0] = -1;
		}

		return count;
	}

	@Override
	public void process_chi_hu_player_score(int seat_index, int provide_index, int operate_card, boolean zimo) {
		GRR._chi_hu_card[seat_index][0] = operate_card;

		GRR._win_order[seat_index] = 1;

		ChiHuRight chr = GRR._chi_hu_rights[seat_index];

		int score = get_fan_shu(chr);

		if (zimo) {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index)
					continue;

				GRR._lost_fan_shu[i][seat_index] = score;
			}
		} else {
			GRR._lost_fan_shu[provide_index][seat_index] = score;
		}

		finallyFanShu[seat_index] = score;

		if (zimo) {
			int[] row = new int[SCORE_DETAIL_COLUMN];
			row[0] = get_score_row_type_zi_mo(chr);

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index) {
					continue;
				}

				if (no_score_left[i] == true)
					continue;

				int nScore = score + player_basic_score[seat_index] + (player_basic_score[i] - BASIC_SCORE);
				nScore = player_left_score[i] >= nScore ? nScore : player_left_score[i];

				row[seat_index + 1] += nScore;
				row[i + 1] -= nScore;

				GRR._game_score[seat_index] += nScore;
				GRR._game_score[i] -= nScore;

				player_left_score[seat_index] += nScore;
				player_left_score[i] -= nScore;

				small_round_total_score[seat_index] += nScore;
				small_round_total_score[i] -= nScore;
			}

			scoreDetails.add(row);
		} else {
			int nScore = score + player_basic_score[seat_index] + (player_basic_score[provide_index] - BASIC_SCORE);
			nScore = player_left_score[provide_index] >= nScore ? nScore : player_left_score[provide_index];

			int[] row = new int[SCORE_DETAIL_COLUMN];
			row[0] = get_score_row_type_jie_pao(chr);

			row[seat_index + 1] += nScore;
			row[provide_index + 1] -= nScore;

			scoreDetails.add(row);

			GRR._game_score[seat_index] += nScore;
			GRR._game_score[provide_index] -= nScore;

			player_left_score[seat_index] += nScore;
			player_left_score[provide_index] -= nScore;

			small_round_total_score[seat_index] += nScore;
			small_round_total_score[provide_index] -= nScore;
		}

		GRR._provider[seat_index] = provide_index;

		change_player_status(seat_index, GameConstants.INVALID_VALUE);

		return;
	}

	@Override
	public void process_chi_hu_player_operate(int seat_index, int operate_card, boolean rm) {
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];
		long effect_indexs[] = new long[1];
		if (!chr.opr_and_long(Constants_SiChuan.CHR_ZI_MO).is_empty() || !chr.opr_and_long(Constants_SiChuan.CHR_GANG_KAI).is_empty()
				|| !chr.opr_and_long(Constants_SiChuan.CHR_DG_GANG_KAI).is_empty()) {
			effect_indexs[0] = Constants_SiChuan.CHR_ZI_MO;
		} else {
			effect_indexs[0] = Constants_SiChuan.CHR_JIE_PAO;
		}

		operate_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_HU, 1, effect_indexs, 1, GameConstants.INVALID_SEAT);

		if (rm) {
			GRR._cards_index[seat_index][_logic.switch_to_card_index(get_real_card(operate_card))]--;

			int cards[] = new int[GameConstants.MAX_COUNT];
			int hand_card_count = _logic.switch_to_cards_data_sichuan(GRR._cards_index[seat_index], cards, ding_que_pai_se[seat_index]);

			operate_player_cards(seat_index, hand_card_count, cards, 0, null);
		}
	}

	@Override
	public int get_next_seat(int seat_index) {
		int count = 0;
		int seat = seat_index;
		do {
			count++;
			seat = (seat + 1) % getTablePlayerNumber();
		} while (count <= 5 && (get_players()[seat] == null || no_score_left[seat]));
		return seat;
	}

	public int get_fan_shu(ChiHuRight chr) {
		int fan = 0;

		if (!chr.opr_and_long(Constants_SiChuan.CHR_TIAN_HU).is_empty()) {
			fan += 16;
		}
		if (!chr.opr_and_long(Constants_SiChuan.CHR_DI_HU).is_empty()) {
			fan += 16;
		}
		if (!chr.opr_and_long(Constants_SiChuan.CHR_QING_QI_DUI).is_empty()) {
			fan += 8;
		}
		if (!chr.opr_and_long(Constants_SiChuan.CHR_QING_PENG_DIAO).is_empty()) {
			fan += 8;
		}
		if (!chr.opr_and_long(Constants_SiChuan.CHR_QING_PENG_HU).is_empty()) {
			fan += 6;
		}
		if (!chr.opr_and_long(Constants_SiChuan.CHR_QING_YI_SE).is_empty()) {
			fan += 4;
		}
		if (!chr.opr_and_long(Constants_SiChuan.CHR_QI_DUI).is_empty()) {
			fan += 4;
		}
		if (!chr.opr_and_long(Constants_SiChuan.CHR_PENG_PENG_DIAO).is_empty()) {
			fan += 4;
		}
		if (!chr.opr_and_long(Constants_SiChuan.CHR_YAO_JIU).is_empty()) {
			fan += 4;
		}
		if (!chr.opr_and_long(Constants_SiChuan.CHR_DUI_DUI_HU).is_empty()) {
			fan += 2;
		}

		if (!chr.opr_and_long(Constants_SiChuan.CHR_JIE_PAO).is_empty()) {
			fan += 0;
		}
		if (!chr.opr_and_long(Constants_SiChuan.CHR_ZI_MO).is_empty()) {
			fan += 1;
		}
		if (!chr.opr_and_long(Constants_SiChuan.CHR_GANG_KAI).is_empty()) {
			fan += 1;
			fan += 1;
		}
		if (!chr.opr_and_long(Constants_SiChuan.CHR_GANG_PAO).is_empty()) {
			fan += 1;
		}
		if (!chr.opr_and_long(Constants_SiChuan.CHR_QIANG_GANG).is_empty()) {
			fan += 3;
		}
		if (!chr.opr_and_long(Constants_SiChuan.CHR_YI_TIAO_LONG).is_empty()) {
			fan += 1;
		}
		if (!chr.opr_and_long(Constants_SiChuan.CHR_HAI_DI).is_empty()) {
			fan += 1;
		}

		return fan;
	}

	@Override
	protected void set_result_describe() {
		for (int player = 0; player < getTablePlayerNumber(); player++) {
			StringBuilder result = new StringBuilder("");

			if (an_gang_count[player] > 0) {
				result.append(" 暗杠x" + an_gang_count[player]);
			}
			if (zhi_gang_count[player] > 0) {
				result.append(" 直杠x" + zhi_gang_count[player]);
			}
			if (wan_gang_count[player] > 0) {
				result.append(" 弯杠x" + wan_gang_count[player]);
			}

			GRR._result_des[player] = result.toString();
		}
	}
}
