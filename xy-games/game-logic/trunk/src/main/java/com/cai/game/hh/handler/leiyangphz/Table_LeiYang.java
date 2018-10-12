package com.cai.game.hh.handler.leiyangphz;

import java.util.ArrayList;
import java.util.List;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.RedisConstant;
import com.cai.common.constant.game.Constants_LeiYang;
import com.cai.common.constant.game.Constants_YZCHZ;
import com.cai.common.constant.game.phz.Constants_YongZhou;
import com.cai.common.define.ECardType;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.GameRoundRecord;
import com.cai.common.domain.Player;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.SysParamModel;
import com.cai.common.domain.WeaveItem;
import com.cai.common.util.SpringService;
import com.cai.dictionary.SysParamDict;
import com.cai.game.RoomUtil;
import com.cai.game.hh.HHGameLogic.AnalyseItem;
import com.cai.game.hh.HHTable;
import com.cai.redis.service.RedisService;
import com.cai.service.MongoDBServiceImpl;
import com.cai.service.PlayerServiceImpl;

import protobuf.clazz.Protocol.GameEndResponse;
import protobuf.clazz.Protocol.GameStartResponse;
import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomInfo;
import protobuf.clazz.Protocol.RoomPlayerResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class Table_LeiYang extends HHTable {

	private static final long serialVersionUID = 6620275622777197542L;

	protected HandlerPlayerHandsUp_LeiYang _handler_hands_up;

	public int _game_mid_score[];
	public int not_can_hu_score[];// 不能胡的分

	int all_hu_xi = 0;

	int[] out_status = new int[3];// 地胡专用，玩家有没有出过牌

	int[] tianting_status = new int[3];// 地胡专用，玩家是否天听

	int hu_pai_zu_he;// 胡的那个牌再那个组合

	int banker_out_card_count; // 庄家出牌数量

	int hu_action;// 胡牌动作

	public Table_LeiYang() {
		super();
	}

	@Override
	public void on_init_table(int game_type_index, int game_rule_index, int game_round) {
		super.on_init_table(game_type_index, game_rule_index, game_round);

		_handler_chi_peng = new HandlerChiPeng_LeiYang();
		_handler_dispath_card = new HandlerDispatchCard_LeiYang();
		_handler_out_card_operate = new HandlerOutCard_LeiYang();
		_handler_gang = new HandlerGang_LeiYang();
		_handler_chuli_firstcards = new HandlerLiangPai_LeiYang();
		_handler_dispath_firstcards = new HandlerSanTiWuKan_LeiYang();
		_handler_hands_up = new HandlerPlayerHandsUp_LeiYang();
	}

	@Override
	public int analyse_chi_hu_card(int cards_index[], WeaveItem weaveItems[], int weaveCount, int seat_index, int provider_index, int cur_card,
			ChiHuRight chiHuRight, int card_type, int[] hu_xi_hh, boolean dispatch) {
		if (this._is_xiang_gong[seat_index] == true) {
			return GameConstants.WIK_NULL;
		}

		if (_ti_mul_long[seat_index] > 0) {
			return GameConstants.WIK_NULL;
		}

		if (is_hands_up[seat_index] && out_status[seat_index] != 0) {
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
		if (cur_card != GameConstants.INVALID_VALUE) {
			int index = _logic.switch_to_card_index(cur_card);
			cbCardIndexTemp[index]++;
		}

		List<AnalyseItem> analyseItemArray = new ArrayList<AnalyseItem>();

		// 分析扑克
		this._hu_xi[seat_index] = 0;
		int hu_xi[] = new int[1];
		hu_xi[0] = 0;
		boolean yws_type = false;
		boolean bValue = _logic.analyse_card_phz(cbCardIndexTemp, weaveItems, weaveCount, seat_index, provider_index, cur_card, analyseItemArray,
				false, hu_xi, yws_type);

		if (cur_card != 0) {
			for (int i = 0; i < weaveCount; i++) {
				if ((cur_card == weaveItems[i].center_card) && ((weaveItems[i].weave_kind == GameConstants.WIK_PENG && dispatch == true)
						|| (weaveItems[i].weave_kind == GameConstants.WIK_WEI))) {

					int index = _logic.switch_to_card_index(cur_card);
					cbCardIndexTemp[index]--;
					int temp_index = analyseItemArray.size();

					boolean temp_bValue = _logic.analyse_card_phz(cbCardIndexTemp, weaveItems, weaveCount, seat_index, provider_index, cur_card,
							analyseItemArray, false, hu_xi, yws_type);

					if (temp_index < analyseItemArray.size()) {
						bValue = temp_bValue;
						AnalyseItem analyseItem = new AnalyseItem();
						for (; temp_index < analyseItemArray.size(); temp_index++) {
							analyseItem = analyseItemArray.get(temp_index);
							hu_xi[0] = 0;
							for (int j = 0; j < 7; j++) {
								if ((cur_card == analyseItem.cbCenterCard[j])
										&& ((analyseItem.cbWeaveKind[j] == GameConstants.WIK_PENG && dispatch == true)
												|| analyseItem.cbWeaveKind[j] == GameConstants.WIK_WEI))
									analyseItem.cbWeaveKind[j] = GameConstants.WIK_PAO;
								analyseItem.hu_xi[j] = _logic.get_analyse_hu_xi(analyseItem.cbWeaveKind[j], analyseItem.cbCenterCard[j]);
								hu_xi[0] += analyseItem.hu_xi[j];

							}

						}
					}
					break;
				}
			}

			// 扫牌判断
			WeaveItem sao_WeaveItem = new WeaveItem();
			int cur_index = _logic.switch_to_card_index(cur_card);
			if (cards_index[cur_index] == 3) {
				cbCardIndexTemp[cur_index] = 1;
				sao_WeaveItem.weave_kind = GameConstants.WIK_KAN;
				sao_WeaveItem.center_card = cur_card;
				sao_WeaveItem.hu_xi = _logic.get_weave_hu_xi(sao_WeaveItem);

				int sao_index = analyseItemArray.size();
				boolean temp_bValue = _logic.analyse_card_phz(cbCardIndexTemp, weaveItems, weaveCount, seat_index, provider_index, cur_card,
						analyseItemArray, false, hu_xi, yws_type);
				if (sao_index < analyseItemArray.size()) {
					bValue = temp_bValue;
					for (; sao_index < analyseItemArray.size(); sao_index++) {
						AnalyseItem analyseItem = new AnalyseItem();
						analyseItem = analyseItemArray.get(sao_index);
						for (int j = 0; j < 7; j++) {
							if (analyseItem.cbWeaveKind[j] == GameConstants.WIK_NULL) {
								analyseItem.cbWeaveKind[j] = sao_WeaveItem.weave_kind;
								analyseItem.cbCenterCard[j] = sao_WeaveItem.center_card;
								analyseItem.hu_xi[j] = sao_WeaveItem.hu_xi;
								break;
							}
						}
					}
					
				}
			}
		}

		if (!bValue) {
			chiHuRight.set_empty();
			return GameConstants.WIK_NULL;
		}

		AnalyseItem analyseItem = new AnalyseItem();
		int temp_hu_xi;
		int max_hu_xi = 0;
		int max_hu_index = 0;
		boolean is_wu_hu = false;
		for (int i = 0; i < analyseItemArray.size(); i++) {
			temp_hu_xi = 0;
			analyseItem = analyseItemArray.get(i);

			for (int j = 0; j < 7; j++) {
				if (analyseItem.cbWeaveKind[j] == GameConstants.WIK_NULL)
					break;
				WeaveItem weave_items = new WeaveItem();
				weave_items.center_card = analyseItem.cbCenterCard[j];
				weave_items.weave_kind = analyseItem.cbWeaveKind[j];
				temp_hu_xi += _logic.get_weave_hu_xi(weave_items);
			}

			if (temp_hu_xi == 0 && has_rule(Constants_LeiYang.GAME_RULE_WU_HU)) {
				max_hu_xi = 21;
				is_wu_hu = true;
			}

			if (temp_hu_xi < 16 && max_hu_xi == 10) // 有小卡胡时，按小卡胡计算
				continue;

			if (temp_hu_xi > max_hu_xi) {
				max_hu_index = i;
				max_hu_xi = temp_hu_xi;
			}
		}

		if ((max_hu_xi == 0 || (max_hu_xi == 21 && is_wu_hu)) && has_rule(Constants_LeiYang.GAME_RULE_WU_HU)) { // 无胡
			max_hu_xi = 21;
			chiHuRight.opr_or(Constants_LeiYang.CHR_WU_HU);
		} else if (max_hu_xi >= 0 && max_hu_xi < 10) { // 10胡起胡
			chiHuRight.set_empty();
			return GameConstants.WIK_NULL;
		}

		hu_xi_hh[0] = max_hu_xi;

		analyseItem = analyseItemArray.get(max_hu_index);

		for (int j = 0; j < 7; j++) {
			if (analyseItem.cbWeaveKind[j] == GameConstants.WIK_NULL)
				break;
			_hu_weave_items[seat_index][j].weave_kind = analyseItem.cbWeaveKind[j];
			_hu_weave_items[seat_index][j].center_card = analyseItem.cbCenterCard[j];
			_hu_weave_items[seat_index][j].hu_xi = _logic.get_weave_hu_xi(_hu_weave_items[seat_index][j]);
			_hu_weave_count[seat_index] = j + 1;
		}

		if (analyseItem.curCardEye == true) {
			_hu_weave_items[seat_index][_hu_weave_count[seat_index]].weave_kind = GameConstants.WIK_DUI_ZI;
			_hu_weave_items[seat_index][_hu_weave_count[seat_index]].center_card = analyseItem.cbCardEye;
			_hu_weave_items[seat_index][_hu_weave_count[seat_index]].hu_xi = 0;
			_hu_weave_count[seat_index]++;
		}

		if (is_hands_up[seat_index] || has_rule(Constants_LeiYang.GAME_RULE_UP_NO_VOICE)) {
			chiHuRight.opr_or(Constants_LeiYang.CHR_HANDS_UP);
		}

		int hong_pai_count = _logic.calculate_hong_pai_count(analyseItem);

		if (hong_pai_count == 0) {
			chiHuRight.opr_or(Constants_LeiYang.CHR_HEI_HU);
		} else if (hong_pai_count == 1 && has_rule(Constants_LeiYang.GAME_RULE_YI_DIAN_HONG)) {
			chiHuRight.opr_or(Constants_LeiYang.CHR_DIAN_HU);
		} else if (hong_pai_count >= 13) {
			chiHuRight.opr_or(Constants_LeiYang.CHR_HONG_HU);
		}

		cbChiHuKind = GameConstants.WIK_CHI_HU;

		if (card_type == Constants_LeiYang.HU_CARD_FAN_PAI_ZI_MO) {
			chiHuRight.opr_or(Constants_LeiYang.CHR_ZI_MO);
		} else if (card_type == Constants_LeiYang.HU_CARD_JIE_PAO) {
			chiHuRight.opr_or(Constants_LeiYang.CHR_JIE_PAO);
		} else if (card_type == Constants_LeiYang.HU_CARD_FAN_PAI_OTHER) {
			chiHuRight.opr_or(Constants_LeiYang.CHR_CHI_HU);
		}

		if (dispatch == true) { // 判断海底
			if (has_rule(Constants_LeiYang.GAME_RULE_LAST_CARD_DOUBLE)) {
				if (GRR._left_card_count == 0)
					chiHuRight.opr_or(Constants_LeiYang.CHR_HAI_DI);
			}
		}

		if (seat_index != _cur_banker && out_status[seat_index] == 0 && tianting_status[seat_index] == 1 && GRR._weave_count[seat_index] < 2) {
			chiHuRight.opr_or(Constants_LeiYang.CHR_DI_HU);
		}

		if (seat_index == _cur_banker && banker_out_card_count == 1 && has_rule(Constants_LeiYang.GAME_RULE_ZHUANG_HANDS_UP))
			chiHuRight.opr_or(Constants_LeiYang.CHR_Z_HANDS_UP);

		if (card_type != Constants_LeiYang.HU_CARD_FAN_PAI_ZI_MO) {
			int huScore = getHuScore(seat_index);
			if (not_can_hu_score[seat_index] >= huScore)
				return GameConstants.WIK_NULL;
		}
		return cbChiHuKind;
	}

	public boolean reset_init_data() {

		if (_cur_round == 0) {

			if (is_mj_type(GameConstants.GAME_TYPE_HH_YX)) {
				// if (getTablePlayerNumber() != this.getTablePlayerNumber()) {
				// this.shuffle_players();
				//
				// for (int i = 0; i < getTablePlayerNumber(); i++) {
				// this.get_players()[i].set_seat_index(i);
				// if (this.get_players()[i].getAccount_id() ==
				// this.getRoom_owner_account_id()) {
				// this._banker_select = i;
				// }
				// }
				// }
				// 胡牌信息

			}

			record_game_room();
		}
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_long_count[i] = 0;
		} // 不能吃，碰

		_game_mid_score = new int[getTablePlayerNumber()];

		has_first_qi_shou_ti = new boolean[this.getTablePlayerNumber()];

		_cannot_chi = new int[this.getTablePlayerNumber()][30];
		_cannot_chi_count = new int[this.getTablePlayerNumber()];
		_cannot_peng = new int[this.getTablePlayerNumber()][30];
		_cannot_peng_count = new int[this.getTablePlayerNumber()];
		_hu_weave_count = new int[this.getTablePlayerNumber()]; //
		_da_pai_count = new int[this.getTablePlayerNumber()];
		_xiao_pai_count = new int[this.getTablePlayerNumber()];
		_hu_weave_items = new WeaveItem[this.getTablePlayerNumber()][7];
		_guo_hu_xt = new int[this.getTablePlayerNumber()];
		_ti_mul_long = new int[this.getTablePlayerNumber()];
		_guo_hu_pai_count = new int[this.getTablePlayerNumber()];
		_ting_card = new boolean[this.getTablePlayerNumber()];
		_tun_shu = new int[this.getTablePlayerNumber()];
		_fan_shu = new int[this.getTablePlayerNumber()];
		_hu_code = new int[this.getTablePlayerNumber()];
		_liu_zi_fen = new int[this.getTablePlayerNumber()];
		_hu_xi = new int[this.getTablePlayerNumber()];
		_hu_pai_max_hu = new int[this.getTablePlayerNumber()];
		_xt_display_an_long = new boolean[this.getTablePlayerNumber()];
		_xian_ming_zhao = new boolean[this.getTablePlayerNumber()];
		_guo_hu_hu_xi = new int[this.getTablePlayerNumber()][GameConstants.MAX_HH_INDEX];
		_zong_liu_zi_fen = 0;
		_all_xi = 0;
		_all_lz_fen = 0;
		_is_di_hu = new int[this.getTablePlayerNumber()];
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			has_shoot[i] = false;
			is_hands_up[i] = false;
			player_ti_count[i][0] = 0;
			player_ti_count[i][1] = 0;
			is_wang_diao[i] = false;
			is_wang_diao_wang[i] = false;
			is_wang_chuang[i] = false;
			_is_di_hu[i] = 0;
		}
		for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
			cards_has_wei[i] = 0;
		}
		card_for_fan_xing = 0;
		fan_xing_count = 0;

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			for (int j = 0; j < 7; j++) {
				_hu_weave_items[i][j] = new WeaveItem();
			}
		}

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_cannot_chi_count[i] = 0;
			_cannot_peng_count[i] = 0;
			_da_pai_count[i] = 0;
			_xiao_pai_count[i] = 0;
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
			_xian_ming_zhao[i] = false;

		}
		_guo_hu_pai_cards = new int[this.getTablePlayerNumber()][15];
		_guo_hu_xi = new int[this.getTablePlayerNumber()][15];
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			for (int j = 0; j < 15; j++) {
				_guo_hu_pai_cards[i][j] = 0;
				_guo_hu_xi[i][j] = 0;
				_guo_hu_hu_xi[i][j] = 0;

			}
		}
		_last_card = 0;
		_last_player = -1;// 上次发牌玩家
		_long_count = new int[this.getTablePlayerNumber()]; // 每个用户有几条龙
		_ti_two_long = new boolean[this.getTablePlayerNumber()];
		_is_xiang_gong = new boolean[this.getTablePlayerNumber()];
		// 设置变量
		_provide_card = GameConstants.INVALID_VALUE;
		_out_card_data = GameConstants.INVALID_VALUE;
		_send_card_data = GameConstants.INVALID_VALUE;

		_provide_player = GameConstants.INVALID_SEAT;
		_out_card_player = GameConstants.INVALID_SEAT;
		_current_player = GameConstants.INVALID_SEAT;

		_send_card_count = 0;
		_out_card_count = 0;
		this._handler = null;

		GRR = new GameRoundRecord(GameConstants.MAX_WEAVE_HH, GameConstants.MAX_HH_COUNT, Constants_YongZhou.MAX_CARD_INDEX);
		GRR.setRoom(this);
		GRR._start_time = System.currentTimeMillis() / 1000L;
		GRR._game_type_index = _game_type_index;
		GRR._cur_round = _cur_round;

		// 新建
		_playerStatus = new PlayerStatus[this.getTablePlayerNumber()];
		istrustee = new boolean[this.getTablePlayerNumber()];
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_playerStatus[i] = new PlayerStatus(GameConstants.MAX_HH_COUNT);
		}

		_cur_round++;

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
			room_player.setOpenThree(_player_open_less[i] == 0 ? false : true);
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

	public boolean operate_game_mid_score() {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_MID_SCORE);

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			roomResponse.addScore(_game_mid_score[i]);
		}

		GRR.add_room_response(roomResponse);

		send_response_to_room(roomResponse);

		return true;
	}

	@Override
	public void process_chi_hu_player_score_phz(int seat_index, int provide_index, int operate_card, boolean zimo) {
		GRR._chi_hu_card[seat_index][0] = operate_card;
		GRR._win_order[seat_index] = 1;
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];

		for (int i = 0; i < this._hu_weave_count[seat_index]; i++) {
			all_hu_xi += this._hu_weave_items[seat_index][i].hu_xi;
		}

		if (all_hu_xi == 0 && has_rule(Constants_LeiYang.GAME_RULE_WU_HU))
			chr.opr_or(Constants_LeiYang.CHR_WU_HU);
		else if (all_hu_xi == 10)
			chr.opr_or(Constants_LeiYang.CHR_XIAO_KA_HU);
		else if (all_hu_xi == 20)
			chr.opr_or(Constants_LeiYang.CHR_DA_KA_HU);

		this._hu_xi[seat_index] = all_hu_xi;

		if (all_hu_xi == 0 && has_rule(Constants_LeiYang.GAME_RULE_WU_HU)) { // 无胡
			all_hu_xi = 21;
		} else if (all_hu_xi == 10) {
			all_hu_xi = 16;
		} else if (all_hu_xi == 20) {
			all_hu_xi = 24;
		}

		countCardType(chr, seat_index);

		all_hu_xi = this.get_final_hu_xi(chr, all_hu_xi);

		int calculate_score = this.get_tun_count(all_hu_xi);

		this._tun_shu[seat_index] = calculate_score;

		this._fan_shu[seat_index] = get_fan_shu(chr);

		// 翻醒
		if (has_rule(Constants_LeiYang.GAME_RULE_FAN_XING)) {
			calculate_score += GRR._count_pick_niao;
		}

		if (!chr.opr_and(Constants_LeiYang.CHR_ZI_MO).is_empty())
			calculate_score *= 2;
		if (!chr.opr_and(Constants_LeiYang.CHR_HAI_DI).is_empty())
			calculate_score *= 2;

		if (zimo) {
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				if (i == seat_index)
					continue;

				GRR._lost_fan_shu[i][seat_index] = calculate_score;
			}
		} else {
			GRR._lost_fan_shu[provide_index][seat_index] = calculate_score * (this.getTablePlayerNumber() - 1);
		}

		int lChiHuScore = calculate_score;

		if (zimo) {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index) {
					continue;
				}
				GRR._game_score[i] -= lChiHuScore;
				GRR._game_score[seat_index] += lChiHuScore;
			}
		} else {
			int s = lChiHuScore * (this.getTablePlayerNumber() - 1);

			GRR._game_score[provide_index] -= s;
			GRR._game_score[seat_index] += s;

			GRR._chi_hu_rights[provide_index].opr_or(Constants_LeiYang.CHR_FANG_PAO);
		}

		GRR._provider[seat_index] = provide_index;
	}

	public void process_ti_long_score() {
		// 计算玩家提龙分数
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			for (int j = 0; j < player_ti_count[i][0]; j++) {
				for (int x = 0; x < getTablePlayerNumber(); x++) {
					if (x == i) {
						GRR._game_score[x] += 1 * (this.getTablePlayerNumber() - 1);
					} else {
						GRR._game_score[x] -= 1;
					}
				}
			}
			for (int j = 0; j < player_ti_count[i][1]; j++) {
				for (int x = 0; x < getTablePlayerNumber(); x++) {
					if (x == i) {
						GRR._game_score[x] += 2 * (this.getTablePlayerNumber() - 1);
					} else {
						GRR._game_score[x] -= 2;
					}
				}
			}
		}
	}

	public void process_mid_score() {
		// 计算玩家提龙分数
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			for (int j = 0; j < player_ti_count[i][0]; j++) {
				for (int x = 0; x < getTablePlayerNumber(); x++) {
					if (x == i) {
						_game_mid_score[x] += 1 * (this.getTablePlayerNumber() - 1);
					} else {
						_game_mid_score[x] -= 1;
					}
				}
			}
			for (int j = 0; j < player_ti_count[i][1]; j++) {
				for (int x = 0; x < getTablePlayerNumber(); x++) {
					if (x == i) {
						_game_mid_score[x] += 2 * (this.getTablePlayerNumber() - 1);
					} else {
						_game_mid_score[x] -= 2;
					}
				}
			}
		}
	}

	@Override
	public void set_result_describe(int seat_index) {
		int chr_count;
		long chr_type = 0;

		for (int player = 0; player < this.getTablePlayerNumber(); player++) {
			StringBuilder resultDesc = new StringBuilder("");

			chr_count = GRR._chi_hu_rights[player].type_count;
			boolean up = false;
			boolean di = false;
			boolean dz = false;
			for (int typeIndex = 0; typeIndex < chr_count; typeIndex++) {
				chr_type = GRR._chi_hu_rights[player].type_list[typeIndex];

				if (GRR._chi_hu_rights[player].is_valid()) {
					if (_send_card_count == 60) {
						resultDesc.append("\n三提五坎");
					}
					if (chr_type == Constants_LeiYang.CHR_CHI_HU) {
						resultDesc.append("\n平胡");
					}
					if (chr_type == Constants_LeiYang.CHR_JIE_PAO) {
						resultDesc.append("\n点炮胡");
					}
					if (chr_type == Constants_LeiYang.CHR_ZI_MO) {
						resultDesc.append("\n自摸2倍");
					}
					if (chr_type == Constants_LeiYang.CHR_HONG_HU) {
						resultDesc.append("\n红胡2番");
					}
					if (chr_type == Constants_LeiYang.CHR_DIAN_HU) {
						resultDesc.append("\n一点红2番");
					}
					if (chr_type == Constants_LeiYang.CHR_HEI_HU) {
						resultDesc.append("\n黑胡2番");
					}
					if (chr_type == Constants_LeiYang.CHR_TIAN_HU) {
						resultDesc.append("\n天胡2番");
					}
					if (chr_type == Constants_LeiYang.CHR_DI_HU) {
						di = true;
					}
					if (chr_type == Constants_LeiYang.CHR_WU_HU) {
						resultDesc.append("\n无胡21胡息");
					}
					if (chr_type == Constants_LeiYang.CHR_XIAO_KA_HU) {
						resultDesc.append("\n小卡胡16胡息");
					}
					if (chr_type == Constants_LeiYang.CHR_DA_KA_HU) {
						resultDesc.append("\n大卡胡24胡息");
					}
					if (chr_type == Constants_LeiYang.CHR_Z_HANDS_UP) {
						dz = true;
					}
					if (chr_type == Constants_LeiYang.CHR_HANDS_UP) {
						up = true;
					}
					if (chr_type == Constants_LeiYang.CHR_HAI_DI) {
						resultDesc.append("\n海底2倍");
					}

				} else if (chr_type == Constants_LeiYang.CHR_FANG_PAO) {
					resultDesc.append("放炮");
				}
			}

			if (di && up)
				resultDesc.append("\n地胡2番");
			if (dz && up)
				resultDesc.append("\n带庄举手2番");
			// if (_game_mid_score[player] != 0) {
			// resultDesc.append("\n提龙分 " + _game_mid_score[player]);
			// }

			GRR._result_des[player] = resultDesc.toString();
		}
	}

	public int get_tun_count(int hu_xi) {
		int tun_count = 0;

		if (hu_xi < 10) {
			tun_count = 0;
		} else if (hu_xi == 10) {
			tun_count = 2;
		} else if (hu_xi > 10 && hu_xi < 16) {
			tun_count = 1;
		} else if (hu_xi >= 16 && hu_xi < 20) {
			tun_count = 2;
		} else if (hu_xi == 20) {
			tun_count = 4;
		} else if (hu_xi > 20 && hu_xi < 24) {
			tun_count = 3;
		} else if (hu_xi >= 24 && hu_xi < 27) {
			tun_count = 4;
		} else if (hu_xi >= 27) {
			tun_count = 4 + (hu_xi - 26) / 3 + ((hu_xi - 26) % 3 == 0 ? 0 : 1);
		}

		return tun_count;
	}

	public int get_fan_shu(ChiHuRight chr) {
		int fan_shu = 1;

		if (!chr.opr_and(Constants_LeiYang.CHR_HONG_HU).is_empty())
			fan_shu *= 2;
		if (!chr.opr_and(Constants_LeiYang.CHR_DIAN_HU).is_empty())
			fan_shu *= 2;
		if (!chr.opr_and(Constants_LeiYang.CHR_HEI_HU).is_empty())
			fan_shu *= 2;
		if (!chr.opr_and(Constants_LeiYang.CHR_TIAN_HU).is_empty())
			fan_shu *= 2;
		if (!chr.opr_and(Constants_LeiYang.CHR_DI_HU).is_empty() && !chr.opr_and(Constants_LeiYang.CHR_HANDS_UP).is_empty())
			fan_shu *= 2;
		if (!chr.opr_and(Constants_LeiYang.CHR_HANDS_UP).is_empty() && !chr.opr_and(Constants_LeiYang.CHR_Z_HANDS_UP).is_empty())
			fan_shu *= 2;

		return fan_shu;
	}

	public int get_final_hu_xi(ChiHuRight chr, int hu_xi) {
		int all_hu_xi = hu_xi;

		if (!chr.opr_and(Constants_LeiYang.CHR_HONG_HU).is_empty())
			all_hu_xi *= 2;
		if (!chr.opr_and(Constants_LeiYang.CHR_DIAN_HU).is_empty())
			all_hu_xi *= 2;
		if (!chr.opr_and(Constants_LeiYang.CHR_HEI_HU).is_empty())
			all_hu_xi *= 2;
		if (!chr.opr_and(Constants_LeiYang.CHR_TIAN_HU).is_empty())
			all_hu_xi *= 2;
		if (!chr.opr_and(Constants_LeiYang.CHR_DI_HU).is_empty() && !chr.opr_and(Constants_LeiYang.CHR_HANDS_UP).is_empty())
			all_hu_xi *= 2;
		if (!chr.opr_and(Constants_LeiYang.CHR_HANDS_UP).is_empty() && !chr.opr_and(Constants_LeiYang.CHR_Z_HANDS_UP).is_empty())
			all_hu_xi *= 2;

		return all_hu_xi;
	}

	@Override
	public int get_real_card(int card) {
		if (card > GameConstants.CARD_ESPECIAL_TYPE_CAN_NOT_SHOOT && card < GameConstants.CARD_ESPECIAL_TYPE_CAN_SHOOT) {
			card -= GameConstants.CARD_ESPECIAL_TYPE_CAN_NOT_SHOOT;
		} else if (card > GameConstants.CARD_ESPECIAL_TYPE_CAN_SHOOT) {
			card -= GameConstants.CARD_ESPECIAL_TYPE_CAN_SHOOT;
		}

		return card;
	}

	public int getHuScore(int seat_index) {
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];
		int all_hu_xi = 0;
		for (int i = 0; i < this._hu_weave_count[seat_index]; i++) {
			all_hu_xi += this._hu_weave_items[seat_index][i].hu_xi;
		}

		if (all_hu_xi == 0 && has_rule(Constants_LeiYang.GAME_RULE_WU_HU)) { // 无胡
			all_hu_xi = 21;
		} else if (all_hu_xi == 10) {
			all_hu_xi = 16;
		} else if (all_hu_xi == 20) {
			all_hu_xi = 24;
		}

		all_hu_xi = get_final_hu_xi(chr, all_hu_xi);

		int calculate_score = get_tun_count(all_hu_xi);

		if (!chr.opr_and(Constants_LeiYang.CHR_ZI_MO).is_empty())
			calculate_score *= 2;
		if (!chr.opr_and(Constants_LeiYang.CHR_HAI_DI).is_empty())
			calculate_score *= 2;

		return calculate_score;
	}

	@Override
	public void test_cards() {

		int[] cards_of_player0 = new int[] { 0x01, 0x01, 0x11, 0x02, 0x02, 0x12, 0x03, 0x03, 0x13, 0x04, 0x05, 0x06, 0x14, 0x15, 0x16, 0x17, 0x18,
				0x19, 0x07, 0x09 };
		int[] cards_of_player1 = new int[] { 0x02, 0x02, 0x02, 0x05, 0x05, 0x15, 0x0a, 0x0a, 0x18, 0x18, 0x08, 0x06, 0x06, 0x06, 0x1a, 0x1a, 0x1a,
				0x11, 0x12, 0x13 };
		int[] cards_of_player2 = new int[] { 0x08, 0x08, 0x05, 0x1a, 0x14, 0x14, 0x14, 0x02, 0x12, 0x12, 0x03, 0x04, 0x05, 0x15, 0x15, 0x07, 0x07,
				0x07, 0x18, 0x18 };
		int[] cards_of_player3 = new int[] { 0x01, 0x01, 0x11, 0x02, 0x02, 0x12, 0x03, 0x03, 0x13, 0x04, 0x05, 0x06, 0x14, 0x15, 0x16, 0x17, 0x18,
				0x19, 0x07, 0x08 };

		/*
		 * int[] cards_of_player0 = new int[] {
		 * 0x11,0x11,0x11,0x11,0x12,0x12,0x12,0x12,0x01,0x02,0x03,0x04,0x05,0x06
		 * ,0x07,0x08,0x09,0x15,0x16,0x17 }; int[] cards_of_player1 = new int[]
		 * {
		 * 0x11,0x11,0x11,0x11,0x12,0x12,0x12,0x12,0x01,0x02,0x03,0x04,0x05,0x06
		 * ,0x07,0x08,0x09,0x15,0x16,0x17 }; int[] cards_of_player2 = new int[]
		 * {
		 * 0x11,0x11,0x11,0x11,0x12,0x12,0x12,0x12,0x01,0x02,0x03,0x04,0x05,0x06
		 * ,0x07,0x08,0x09,0x15,0x16,0x17 }; int[] cards_of_player3 = new int[]
		 * { 0x11, 0x12, 0x13, 0x14, 0x14, 0x04, 0x01, 0x02, 0x03, 0x15, 0x15,
		 * 0x05, 0x07, 0x17, 0x17, 0x09, 0x19, 0x19, 0x16, 0x06 };
		 */

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			for (int j = 0; j < GameConstants.MAX_HH_COUNT - 1; j++) {
				GRR._cards_index[i][j] = 0;
			}
		}

		for (int j = 0; j < GameConstants.MAX_HH_COUNT - 1; j++) {
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

	@Override
	public void countChiHuTimes(int _seat_index, boolean zimo) {
		ChiHuRight chiHuRight = GRR._chi_hu_rights[_seat_index];

		if (zimo) {
			_player_result.hu_pai_count[_seat_index]++;
			_player_result.ying_xi_count[_seat_index] += this._hu_xi[_seat_index];
		}

		if (!(chiHuRight.opr_and(Constants_LeiYang.CHR_HONG_HU)).is_empty()) {
			_player_result.ming_tang_count[_seat_index]++;
		}
		if (!(chiHuRight.opr_and(Constants_LeiYang.CHR_DIAN_HU)).is_empty()) {
			_player_result.ming_tang_count[_seat_index]++;
		}
		if (!(chiHuRight.opr_and(Constants_LeiYang.CHR_HEI_HU)).is_empty()) {
			_player_result.ming_tang_count[_seat_index]++;
		}
		if (!(chiHuRight.opr_and(Constants_LeiYang.CHR_TIAN_HU)).is_empty()) {
			_player_result.ming_tang_count[_seat_index]++;
		}
		if (!(chiHuRight.opr_and(Constants_LeiYang.CHR_DI_HU)).is_empty()) {
			_player_result.ming_tang_count[_seat_index]++;
		}
		if (!(chiHuRight.opr_and(Constants_LeiYang.CHR_WU_HU)).is_empty()) {
			_player_result.ming_tang_count[_seat_index]++;
		}
		if (!(chiHuRight.opr_and(Constants_LeiYang.CHR_XIAO_KA_HU)).is_empty()) {
			_player_result.ming_tang_count[_seat_index]++;
		}
		if (!(chiHuRight.opr_and(Constants_LeiYang.CHR_DA_KA_HU)).is_empty()) {
			_player_result.ming_tang_count[_seat_index]++;
		}
		if (!(chiHuRight.opr_and(Constants_LeiYang.CHR_HANDS_UP)).is_empty()) {
			_player_result.ming_tang_count[_seat_index]++;
		}
		if (!(chiHuRight.opr_and(Constants_LeiYang.CHR_Z_HANDS_UP)).is_empty()) {
			_player_result.ming_tang_count[_seat_index]++;
		}
		if (!(chiHuRight.opr_and(Constants_LeiYang.CHR_HAI_DI)).is_empty()) {
			_player_result.ming_tang_count[_seat_index]++;
		}
	}

	@Override
	public int get_hh_ting_card_twenty(int[] cards, int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int seat_index, int provate_index) {
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
			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card(cbCardIndexTemp, weaveItem, cbWeaveCount, seat_index, provate_index, cbCurrentCard,
					chr, Constants_LeiYang.HU_CARD_FAN_PAI_ZI_MO, hu_xi, true)) {
				cards[count] = cbCurrentCard;
				count++;
			}
		}

		if (count >= mj_count) {
			count = 1;
			cards[0] = -1;
		}

		return count;
	}

	public boolean is_card_has_wei(int card) {
		boolean bTmp = false;

		for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
			if (this.cards_has_wei[i] != 0) {
				if (i == this._logic.switch_to_card_index(card)) {
					bTmp = true;
					break;
				}
			}
		}

		return bTmp;
	}

	// 是否听牌
	public boolean is_ting_state(int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int seat_index) {
		// 复制数据
		int cbCardIndexTemp[] = new int[GameConstants.MAX_HH_INDEX];
		for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		ChiHuRight chr = new ChiHuRight();
		chr.set_empty();
		int hu_xi_chi[] = new int[1];
		hu_xi_chi[0] = 0;
		for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
			int cbCurrentCard = _logic.switch_to_card_data(i);
			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card(cbCardIndexTemp, weaveItem, cbWeaveCount, seat_index, seat_index, cbCurrentCard, chr,
					Constants_LeiYang.HU_CARD_FAN_PAI_ZI_MO, hu_xi_chi, true))
				return true;
		}
		return false;
	}

	public int get_hong_pai_count(WeaveItem weaveItems[], int weaveCount, int cards_index[]) {
		int count = 0;

		for (int i = 0; i < weaveCount; i++) {
			switch (weaveItems[i].weave_kind) {
			case GameConstants.WIK_TI_LONG:
			case GameConstants.WIK_AN_LONG:
			case GameConstants.WIK_PAO:
				if (_logic.color_hei(weaveItems[i].center_card) == false)
					count += 4;
				break;
			case GameConstants.WIK_SAO:
			case GameConstants.WIK_PENG:
			case GameConstants.WIK_CHOU_SAO:
			case GameConstants.WIK_KAN:
			case GameConstants.WIK_WEI:
			case GameConstants.WIK_XIAO:
			case GameConstants.WIK_CHOU_XIAO:
			case GameConstants.WIK_CHOU_WEI:
				if (_logic.color_hei(weaveItems[i].center_card) == false)
					count += 3;
				break;
			}
		}

		int hand_cards_data[] = new int[GameConstants.MAX_HH_COUNT];
		int hand_card_count = _logic.switch_to_cards_data(cards_index, hand_cards_data);
		for (int i = 0; i < hand_card_count; i++) {
			if (_logic.color_hei(hand_cards_data[i]) == false)
				count++;
		}

		return count;
	}

	@Override
	public void countCardType(ChiHuRight chiHuRight, int seat_index) {
		try {
			if (!(chiHuRight.opr_and(Constants_LeiYang.CHR_HONG_HU)).is_empty()) {
				MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.hhhonghu, "", 0, 0l, this.getRoom_id());
			}
			if (!(chiHuRight.opr_and(Constants_LeiYang.CHR_DIAN_HU)).is_empty()) {
				MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.hhyidianhong, "", 0, 0l, this.getRoom_id());
			}
			if (!(chiHuRight.opr_and(Constants_LeiYang.CHR_HEI_HU)).is_empty()) {
				MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.hhallhei, "", 0, 0l, this.getRoom_id());
			}
			if (!(chiHuRight.opr_and(Constants_LeiYang.CHR_TIAN_HU)).is_empty()) {
				MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.hhtianhu, "", 0, 0l, this.getRoom_id());
			}
			if (!(chiHuRight.opr_and(Constants_LeiYang.CHR_DI_HU)).is_empty()) {
				MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.hhdihu, "", 0, 0l, this.getRoom_id());
			}
			if (!(chiHuRight.opr_and(Constants_LeiYang.CHR_WU_HU)).is_empty()) {
				MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.hhwuhu, "", 0, 0l, this.getRoom_id());
			}
			if (!(chiHuRight.opr_and(Constants_LeiYang.CHR_XIAO_KA_HU)).is_empty()) {
				MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.hhxiaokahu, "", 0, 0l, this.getRoom_id());
			}
			if (!(chiHuRight.opr_and(Constants_LeiYang.CHR_DA_KA_HU)).is_empty()) {
				MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.hhdakahu, "", 0, 0l, this.getRoom_id());
			}
			if (!(chiHuRight.opr_and(Constants_LeiYang.CHR_HAI_DI)).is_empty()) {
				MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.hhhaidihu, "", 0, 0l, this.getRoom_id());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public boolean check_hands_up(AnalyseItem analyseItem) {
		boolean bState = true;

		for (int j = 0; j < 7; j++) {
			if (analyseItem.cbWeaveKind[j] == GameConstants.WIK_NULL)
				break;

			if (analyseItem.cbWeaveKind[j] != GameConstants.WIK_TI_LONG) { // 如果不是提龙
				bState = false;
				break;
			}
		}

		return bState;
	}

	@Override
	public void set_niao_card(int seat_index, int card, boolean show) {
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
		GRR._count_niao = 0;

		if (this.GRR._left_card_count == 0) {
			GRR._cards_data_niao[0] = GRR._chi_hu_card[seat_index][0];
			GRR._count_niao++;
		} else {
			GRR._cards_data_niao[0] = this._repertory_card[this._all_card_len - this.GRR._left_card_count];
			GRR._count_niao++;
			this.GRR._left_card_count--;
		}

		this.operate_player_get_card(GameConstants.INVALID_SEAT, 1, new int[] { GRR._cards_data_niao[0] }, GameConstants.INVALID_SEAT, false);

		GRR._count_pick_niao = _logic.get_xing_pai_count(this._hu_weave_items[seat_index], this._hu_weave_count[seat_index], GRR._cards_data_niao[0]);
	}

	public int count_xing_pai(int[] cards_index, int card) {
		int tmpCount = 0;

		int[] hand_cards = new int[GameConstants.MAX_HH_COUNT];
		int hand_card_count = _logic.switch_to_cards_data(cards_index, hand_cards);

		for (int i = 0; i < hand_card_count; i++) {
			if (hand_cards[i] == card) {
				tmpCount = hand_cards[i];
				break;
			}
		}

		return tmpCount;
	}

	@Override
	public boolean handler_requst_pao_qiang(Player player, int pao, int qiang) {
		return _handler_hands_up.handler_hands_up(this, player.get_seat_index(), pao, qiang);
	}

	/** 胡的牌 */
	@Override
	public void process_chi_hu_player_operate(int seat_index, int operate_card, boolean rm) {
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];

		operate_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_HU, 1, new long[] { hu_action }, 1, GameConstants.INVALID_SEAT);

		for (int i = 0; i < _hu_weave_count[seat_index]; i++) {
			switch (_hu_weave_items[seat_index][i].weave_kind) {
			case GameConstants.WIK_AN_LONG:
			case GameConstants.WIK_TI_LONG:
			case GameConstants.WIK_PAO:
			case GameConstants.WIK_PENG:
			case GameConstants.WIK_WEI:
			case GameConstants.WIK_CHOU_WEI:
			case GameConstants.WIK_DUI_ZI:
				if (_hu_weave_items[seat_index][i].center_card == operate_card) {
					hu_pai_zu_he = _hu_weave_items[seat_index][i].weave_kind;
					break;
				}
				break;
			case GameConstants.WIK_LEFT:
				if (_hu_weave_items[seat_index][i].center_card == operate_card) {
					hu_pai_zu_he = _hu_weave_items[seat_index][i].weave_kind;
					break;
				} else if (_hu_weave_items[seat_index][i].center_card + 1 == operate_card) {
					hu_pai_zu_he = _hu_weave_items[seat_index][i].weave_kind;
					break;
				} else if (_hu_weave_items[seat_index][i].center_card + 2 == operate_card) {
					hu_pai_zu_he = _hu_weave_items[seat_index][i].weave_kind;
					break;
				}
				break;
			case GameConstants.WIK_CENTER:
				if (_hu_weave_items[seat_index][i].center_card == operate_card) {
					hu_pai_zu_he = _hu_weave_items[seat_index][i].weave_kind;
					break;
				} else if (_hu_weave_items[seat_index][i].center_card - 1 == operate_card) {
					hu_pai_zu_he = _hu_weave_items[seat_index][i].weave_kind;
					break;
				} else if (_hu_weave_items[seat_index][i].center_card + 1 == operate_card) {
					hu_pai_zu_he = _hu_weave_items[seat_index][i].weave_kind;
					break;
				}
				break;
			case GameConstants.WIK_RIGHT:
				if (_hu_weave_items[seat_index][i].center_card == operate_card) {
					hu_pai_zu_he = _hu_weave_items[seat_index][i].weave_kind;
					break;
				} else if (_hu_weave_items[seat_index][i].center_card - 1 == operate_card) {
					hu_pai_zu_he = _hu_weave_items[seat_index][i].weave_kind;
					break;
				} else if (_hu_weave_items[seat_index][i].center_card - 2 == operate_card) {
					hu_pai_zu_he = _hu_weave_items[seat_index][i].weave_kind;
					break;
				}
				break;
			case GameConstants.WIK_EQS:
				if (_logic.get_card_color(operate_card) != _logic.get_card_color(_hu_weave_items[seat_index][i].center_card)) {
					break;
				}
				if (_logic.get_card_value(operate_card) == 2) {
					hu_pai_zu_he = _hu_weave_items[seat_index][i].weave_kind;
					break;
				} else if (_logic.get_card_value(operate_card) == 7) {
					hu_pai_zu_he = _hu_weave_items[seat_index][i].weave_kind;
					break;
				} else if (_logic.get_card_value(operate_card) == 10) {
					hu_pai_zu_he = _hu_weave_items[seat_index][i].weave_kind;
					break;
				}
				break;
			case GameConstants.WIK_YWS:
				if (_logic.get_card_color(operate_card) != _logic.get_card_color(_hu_weave_items[seat_index][i].center_card)) {
					break;
				}
				if (_logic.get_card_value(operate_card) == 1) {
					hu_pai_zu_he = _hu_weave_items[seat_index][i].weave_kind;
					break;
				} else if (_logic.get_card_value(operate_card) == 5) {
					hu_pai_zu_he = _hu_weave_items[seat_index][i].weave_kind;
					break;
				} else if (_logic.get_card_value(operate_card) == 10) {
					hu_pai_zu_he = _hu_weave_items[seat_index][i].weave_kind;
					break;
				}
				break;
			case GameConstants.WIK_DDX:
			case GameConstants.WIK_XXD:
				if (_logic.get_card_value(operate_card) == _logic.get_card_value(_hu_weave_items[seat_index][i].center_card)) {
					hu_pai_zu_he = _hu_weave_items[seat_index][i].weave_kind;
					break;
				}
				break;
			}
		}

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			int cards[] = new int[GameConstants.MAX_HH_COUNT];
			int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[i], cards);

			operate_show_card(i, GameConstants.Show_Card_HU, hand_card_count, cards, GRR._weave_items[i], GRR._weave_count[i],
					GameConstants.INVALID_SEAT);
		}

		return;
	}

	@Override
	public boolean on_room_game_finish(int seat_index, int reason) {
		_game_status = GameConstants.GS_MJ_WAIT;

		if (_cur_round == 1 && (reason == GameConstants.Game_End_NORMAL || reason == GameConstants.Game_End_DRAW)) {
			RoomUtil.realkou_dou(this);
		}

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

		RoomInfo.Builder room_info = getRoomInfo();
		game_end.setRoomInfo(room_info);
		game_end.setGamePlayerNumber(count);
		game_end.setRoundOverType(0);
		game_end.setRoomOverType(0);
		game_end.setEndTime(System.currentTimeMillis() / 1000L);// 结束时间

		if (seat_index != GameConstants.INVALID_SEAT)
			game_end.setZongXi(all_hu_xi);// 总胡息
		all_hu_xi = 0;// 清空状态
		banker_out_card_count = 0;
		for (int i = 0; i < out_status.length; i++) {
			out_status[i] = 0;
			tianting_status[i] = 0;
		}
		if (GRR != null) {
			// TODO 胡的那张牌，是在哪一个组合里面
			game_end.setCountPickNiao(hu_pai_zu_he);
			hu_pai_zu_he = 0;
			game_end.setRoundOverType(1);
			game_end.setStartTime(GRR._start_time);

			game_end.setGameTypeIndex(GRR._game_type_index);
			roomResponse.setLeftCardCount(GRR._left_card_count);

			for (int i = 0; i < GRR._especial_card_count; i++) {
				game_end.addEspecialShowCards(GRR._especial_show_cards[i]);
			}

			GRR._end_type = reason;

			process_ti_long_score();

			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				_player_result.game_score[i] += this.GRR._game_score[i];
			}

			this.load_player_info_data(roomResponse);

			game_end.setGameRound(_game_round);
			game_end.setCurRound(_cur_round);
			game_end.setGamePlayerNumber(getTablePlayerNumber());
			game_end.setCellScore(GameConstants.CELL_SCORE);

			game_end.setBankerPlayer(GRR._banker_player);// 专家
			game_end.setLeftCardCount(GRR._left_card_count);// 剩余牌
			int cards[] = new int[GRR._left_card_count];
			int k = 0;
			int left_card_count = GRR._left_card_count;
			for (int i = _all_card_len - GRR._left_card_count; i < _all_card_len; i++) {
				cards[k] = _repertory_card[_all_card_len - left_card_count];
				game_end.addCardsList(cards[k]);

				k++;
				left_card_count--;
			}
			game_end.setShowBirdEffect(GRR._show_bird_effect == false ? 0 : 1);

			for (int i = 0; i < GRR._count_niao; i++) {
				game_end.addCardsDataNiao(GRR._cards_data_niao[i]);
			}
			game_end.setCountPickNiao(GRR._count_pick_niao);

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				Int32ArrayResponse.Builder pnc = Int32ArrayResponse.newBuilder();
				game_end.addHuResult(GRR._hu_result[i]);

				// 胡的牌
				Int32ArrayResponse.Builder hc = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GameConstants.MAX_HH_COUNT; j++) {
					hc.addItem(GRR._chi_hu_card[i][j]);
				}

				game_end.addHuCardData(GRR._chi_hu_card[i][0]);
				game_end.addHuCardArray(hc);
			}

			// 现在权值只有一位
			long rv[] = new long[GameConstants.MAX_RIGHT_COUNT];

			// 设置胡牌描述
			this.set_result_describe(seat_index);

			if (seat_index != -1) {
				game_end.setTunShu(this._tun_shu[seat_index]);
				game_end.setFanShu(this._fan_shu[seat_index]);
			}

			if (this.has_rule(GameConstants.GAME_RULE_DI_HUANG_FAN)) {
				if (reason == GameConstants.Game_End_DRAW) {
					this._huang_zhang_count++;
				} else {
					this._huang_zhang_count = 0;
				}
			}

			for (int i = 0; i < count; i++) {
				GRR._card_count[i] = _logic.switch_to_cards_data(GRR._cards_index[i], GRR._cards_data[i]);

				Int32ArrayResponse.Builder cs = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GRR._card_count[i]; j++) {

					cs.addItem(GRR._cards_data[i][j]);
				}
				game_end.addCardsData(cs);// 牌

				int all_hu_xi = 0;

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
				game_end.addStartHuScore(GRR._start_hu_score[i]);
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

		if (real_reason == GameConstants.Game_End_NORMAL)
			game_end.setWinLziFen(seat_index);

		roomResponse.setGameEnd(game_end);
		this.send_response_to_room(roomResponse);

		record_game_round(game_end, real_reason);

		// 超时解散
		if (reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT || reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT) {
			for (int j = 0; j < this.getTablePlayerNumber(); j++) {
				Player player = this.get_players()[j];
				if (player == null)
					continue;
				send_error_notify(j, 1, "游戏解散成功!");
			}
		}

		if (end) {
			PlayerServiceImpl.getInstance().delRoomId(this.getRoom_id());
		}

		if (!is_sys()) {
			GRR = null;
		}

		if (is_sys()) {
			clear_score_in_gold_room();
		}

		return false;
	}

	@Override
	protected boolean on_handler_game_start() {

		_game_status = GameConstants.GS_MJ_PLAY;

		reset_init_data();

		// 庄家选择
		this.progress_banker_select();

		GRR._banker_player = _cur_banker;
		_current_player = GRR._banker_player;

		if (is_mj_type(GameConstants.GAME_TYPE_PHZ_YONG_ZHOU)) {
			_repertory_card = new int[Constants_YZCHZ.CARD_COUNT_YZCHZ];
			shuffle(_repertory_card, Constants_YZCHZ.CARD_FOR_YZCHZ);
		} else {
			_repertory_card = new int[GameConstants.CARD_COUNT_HH_YX];
			shuffle(_repertory_card, GameConstants.CARD_DATA_HH_YX);
		}


		// DEBUG_CARDS_MODE = true;
		// BACK_DEBUG_CARDS_MODE = true;
		if (DEBUG_CARDS_MODE || BACK_DEBUG_CARDS_MODE)
			test_cards();

		return game_start_HH();
	}

	private boolean game_start_HH() {

		not_can_hu_score = new int[getTablePlayerNumber()];
		_logic.clean_magic_cards();
		int playerCount = getPlayerCount();
		this.GRR._banker_player = this._current_player = this._cur_banker;
		// 游戏开始
		this._game_status = GameConstants.GS_MJ_PLAY;// 设置状态
		GameStartResponse.Builder gameStartResponse = GameStartResponse.newBuilder();
		gameStartResponse.setBankerPlayer(this.GRR._banker_player);
		gameStartResponse.setCurrentPlayer(this._current_player);
		gameStartResponse.setLeftCardCount(this.GRR._left_card_count);

		int hand_cards[][] = new int[playerCount][GameConstants.MAX_HH_COUNT];
		// 发送数据
		for (int i = 0; i < playerCount; i++) {
			int hand_card_count = this._logic.switch_to_cards_data(this.GRR._cards_index[i], hand_cards[i]);
			gameStartResponse.addCardsCount(hand_card_count);
		}

		boolean can_ti = false;
		int ti_card_count[] = new int[this.getTablePlayerNumber()];
		int ti_card_index[][] = new int[this.getTablePlayerNumber()][5];

		for (int i = 0; i < playerCount; i++) {
			ti_card_count[i] = this._logic.get_action_ti_Card(this.GRR._cards_index[i], ti_card_index[i]);
			if (ti_card_count[i] > 0)
				can_ti = true;
		}
		int FlashTime = 500;
		int standTime = 500;
		// 发送数据
		for (int i = 0; i < playerCount; i++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

			// 只发自己的牌
			gameStartResponse.clearCardData();
			for (int j = 0; j < GameConstants.MAX_HH_COUNT; j++) {
				gameStartResponse.addCardData(hand_cards[i][j]);
			}

			// 回放数据
			this.GRR._video_recode.addHandCards(cards);

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			this.load_room_info_data(roomResponse);
			this.load_common_status(roomResponse);

			if (this._cur_round == 1) {
				// shuffle_players();
				this.load_player_info_data(roomResponse);
			}
			roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
			roomResponse.setGameStart(gameStartResponse);
			roomResponse.setCurrentPlayer(this._current_player == GameConstants.INVALID_SEAT ? this._resume_player : this._current_player);
			roomResponse.setLeftCardCount(this.GRR._left_card_count);
			roomResponse.setGameStatus(this._game_status);
			roomResponse.setLeftCardCount(this.GRR._left_card_count);

			int gameId = this.getGame_id() == 0 ? 8 : this.getGame_id();
			SysParamModel sysParamModel1104 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(gameId).get(1104);
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

			for (int j = 0; j < GameConstants.MAX_HH_COUNT; j++) {
				cards.addItem(hand_cards[i][j]);
			}
			gameStartResponse.addCardsData(cards);
		}

		roomResponse.setGameStart(gameStartResponse);

		roomResponse.setLeftCardCount(this.GRR._left_card_count);
		this.GRR.add_room_response(roomResponse);

		// 检测听牌
		for (int i = 0; i < playerCount; i++) {
			this._playerStatus[i]._hu_card_count = get_hh_ting_card_twenty(_playerStatus[i]._hu_cards, GRR._cards_index[i], GRR._weave_items[i],
					GRR._weave_count[i], i, i);
			if (this._playerStatus[i]._hu_card_count > 0) {
				tianting_status[i] = 1;
				this.operate_chi_hu_cards(i, this._playerStatus[i]._hu_card_count, this._playerStatus[i]._hu_cards);
			}
		}
		this._handler = this._handler_dispath_firstcards;
		this.exe_dispatch_first_card(_current_player, GameConstants.WIK_NULL, FlashTime + standTime);

		return true;
	}
}
