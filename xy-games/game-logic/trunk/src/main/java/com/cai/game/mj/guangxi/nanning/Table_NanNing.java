package com.cai.game.mj.guangxi.nanning;

import java.util.ArrayList;
import java.util.List;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.Constants_GXLZ;
import com.cai.common.constant.game.GameConstants_NanNing;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.Player;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.WeaveItem;
import com.cai.common.util.GameDescUtil;
import com.cai.game.mj.AbstractMJTable;
import com.cai.game.mj.MJGameLogic.AnalyseItem;
import com.cai.game.mj.MJType;
import com.cai.service.PlayerServiceImpl;

import protobuf.clazz.Protocol.GameEndResponse;
import protobuf.clazz.Protocol.GameStartResponse;
import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomInfo;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class Table_NanNing extends AbstractMJTable {
	private static final long serialVersionUID = 8876809848372123420L;

	boolean[] feng_pai;
	int[] feng_pai_4_orther_seat;
	int[] quan_bao;

	public Table_NanNing() {
		super(MJType.GAME_TYPE_GX_NAN_NING);
	}

	@Override
	protected void onInitTable() {
		_handler_dispath_card = new HandlerDispatchCard_NanNing();
		_handler_out_card_operate = new HandlerOutCardOperate_NanNing();
		_handler_gang = new HandlerGang_NanNing();
		_handler_chi_peng = new HandlerChiPeng_NanNing();
	}

	// 七小对牌 七小对：胡牌时，手上任意七对牌。
	public int is_qi_xiao_dui(int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int cur_card) {

		// 组合判断
		if (cbWeaveCount != 0)
			return GameConstants.WIK_NULL;

		// 单牌数目
		int cbReplaceCount = 0;
		int nGenCount = 0;

		// 临时数据
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		// 插入数据
		int cbCurrentIndex = _logic.switch_to_card_index(cur_card);
		cbCardIndexTemp[cbCurrentIndex]++;

		// 计算单牌
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			int cbCardCount = cbCardIndexTemp[i];

			if (_logic.get_magic_card_count() > 0) {
				for (int m = 0; m < _logic.get_magic_card_count(); m++) {
					// 王牌过滤
					if (i == _logic.get_magic_card_index(m))
						continue;

					// 单牌统计
					if (cbCardCount == 1 || cbCardCount == 3)
						cbReplaceCount++;

					if (cbCardCount == 4) {
						nGenCount++;
					}
				}
			} else {
				// 单牌统计
				if (cbCardCount == 1 || cbCardCount == 3)
					cbReplaceCount++;

				if (cbCardCount == 4) {
					nGenCount++;
				}
			}
		}

		// 王牌不够
		if (_logic.get_magic_card_count() > 0) {
			int count = 0;
			for (int m = 0; m < _logic.get_magic_card_count(); m++) {
				count += cbCardIndexTemp[_logic.get_magic_card_index(m)];
			}

			if (cbReplaceCount > count) {
				return GameConstants.WIK_NULL;
			}
		} else {
			if (cbReplaceCount > 0)
				return GameConstants.WIK_NULL;
		}

		if (nGenCount > 0) {
			if (nGenCount == 2) {
				return GameConstants_NanNing.CHR_SHUANG_QI_XIAO_DUI;
			} else if (nGenCount == 3) {
				return GameConstants_NanNing.CHR_SAN_QI_XIAO_DUI;
			}
			return GameConstants_NanNing.CHR_HAO_HUA_QI_XIAO_DUI;
		} else {
			return GameConstants_NanNing.CHR_XIAO_QI_DUI;
		}
	}

	@Override
	public int analyse_chi_hu_card(int[] cards_index, WeaveItem[] weaveItems, int weave_count, int cur_card, ChiHuRight chiHuRight, int card_type,
			int _seat_index) {
		if (cur_card == 0) {
			return GameConstants.WIK_NULL;
		}

		int cbChiHuKind = GameConstants.WIK_NULL;

		boolean basic_hu = true;

		int check_qi_xiao_dui = is_qi_xiao_dui(cards_index, weaveItems, weave_count, cur_card);

		if (check_qi_xiao_dui != GameConstants.WIK_NULL) {
			cbChiHuKind = GameConstants.WIK_CHI_HU;
			chiHuRight.opr_or(check_qi_xiao_dui);
			basic_hu = false;
		}

		boolean is_qing_yi_se = _logic.is_qing_yi_se(cards_index, weaveItems, weave_count, cur_card);

		if (is_qing_yi_se == true) {
			cbChiHuKind = GameConstants.WIK_CHI_HU;
			chiHuRight.opr_or(GameConstants_NanNing.CHR_QING_YI_SE);
			basic_hu = false;
		}

		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		if (cur_card != GameConstants.INVALID_VALUE) {
			cbCardIndexTemp[_logic.switch_to_card_index(cur_card)]++;
		}

		List<AnalyseItem> analyseItemArray = new ArrayList<AnalyseItem>();
		boolean bValue = _logic.analyse_card(cbCardIndexTemp, weaveItems, weave_count, analyseItemArray, true);

		if (bValue == false && check_qi_xiao_dui == GameConstants.WIK_NULL) {
			chiHuRight.set_empty();
			return GameConstants.WIK_NULL;
		}

		for (AnalyseItem analyseItem : analyseItemArray) {
			if (_logic.is_pengpeng_hu(analyseItem)) {
				chiHuRight.opr_or(GameConstants_NanNing.CHR_PENG_PENG_HU);
				basic_hu = false;
				break;
			}
		}

		int card_count = _logic.get_card_count_by_index(cards_index);
		if (card_count == 1) {
			chiHuRight.opr_or(GameConstants_NanNing.CHR_QUAN_QIU_REN);
		}

		cbChiHuKind = GameConstants.WIK_CHI_HU;

		if (card_type == GameConstants_NanNing.HU_CARD_TYPE_ZI_MO) { // 自摸
			chiHuRight.opr_or(GameConstants_NanNing.CHR_ZI_MO);
		} else if (card_type == GameConstants_NanNing.HU_CARD_TYPE_QIANG_GANG) { // 抢杠
			chiHuRight.opr_or(GameConstants_NanNing.CHR_QIANG_GANG_HU);
		} else if ((card_type & GameConstants_NanNing.HU_CARD_TYPE_GANG_KAI) != 0) { // 杠上花
			chiHuRight.opr_or(GameConstants_NanNing.CHR_GANG_KAI);
			chiHuRight.opr_or(GameConstants_NanNing.CHR_ZI_MO); // 杠上花算自摸
		} else if (card_type == GameConstants_NanNing.HU_CARD_TYPE_GANG_PAO) { // 杠上炮
			chiHuRight.opr_or(GameConstants_NanNing.CHR_GANG_SHANG_PAO);
		} else if (card_type == GameConstants_NanNing.HU_CARD_TYPE_JIE_PAO) { // 接炮
			chiHuRight.opr_or(GameConstants_NanNing.CHR_JIE_PAO);
		} else if (card_type == GameConstants_NanNing.HU_CARD_QUAN_BAO) { // 接炮
			chiHuRight.opr_or(GameConstants_NanNing.CHR_QUAN_BAO);
		}

		if (basic_hu == true) {
			chiHuRight.opr_or(GameConstants_NanNing.CHR_JI_BEN_HU); // 基本胡
		}

		if (!chiHuRight.opr_and(Constants_GXLZ.CHR_ZI_MO).is_empty()) {
			boolean check_men_qing = this.is_men_qing(weaveItems, weave_count);
			if (check_men_qing == true) {
				chiHuRight.opr_or(GameConstants_NanNing.CHR_MEN_QING); // 门清
			}
		}
		return cbChiHuKind;
	}

	protected boolean is_men_qing(WeaveItem weaveItem[], int cbWeaveCount) {
		if (cbWeaveCount == 0)
			return true;

		for (int i = 0; i < cbWeaveCount; i++) {
			if (!(weaveItem[i].weave_kind == GameConstants.WIK_GANG && weaveItem[i].public_card == 0)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * 抓鸟 个数
	 * 
	 * @return
	 */
	public int getCsDingNiaoNum() {
		int nNum = GameConstants.ZHANIAO_0;
		if (GameDescUtil.has_rule(gameRuleIndexEx, GameConstants_NanNing.GAME_RULE_MAI_MA_2)) {
			return GameConstants.ZHANIAO_2;
		} else if (GameDescUtil.has_rule(gameRuleIndexEx, GameConstants_NanNing.GAME_RULE_MAI_MA_4)) {
			return GameConstants.ZHANIAO_4;
		} else if (GameDescUtil.has_rule(gameRuleIndexEx, GameConstants_NanNing.GAME_RULE_MAI_MA_6)) {
			return GameConstants.ZHANIAO_6;
		}
		return nNum;
	}

	/**
	 * 
	 * @param seat_index
	 * @param card
	 *            固定的鸟
	 * @param show
	 * @param add_niao
	 *            额外奖鸟
	 * @param isTongPao
	 *            --通炮不抓飞鸟
	 */
	public void set_niao_card(int seat_index, int card, boolean show, int add_niao) {

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
		GRR._count_niao = getCsDingNiaoNum();

		if (GRR._count_niao > GameConstants.ZHANIAO_0) {
			if (card == GameConstants.INVALID_VALUE) {
				int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
				_logic.switch_to_cards_index(_repertory_card, _all_card_len - GRR._left_card_count, GRR._count_niao, cbCardIndexTemp);
				GRR._left_card_count -= GRR._count_niao;
				_logic.switch_to_cards_data(cbCardIndexTemp, GRR._cards_data_niao);
			} else {
				for (int i = 0; i < GRR._count_niao; i++) {
					GRR._cards_data_niao[i] = card;
				}
			}
		}
		// 中鸟个数
		GRR._count_pick_niao = _logic.get_pick_niao_count(GRR._cards_data_niao, GRR._count_niao);

		for (int i = 0; i < GRR._count_niao; i++) {
			int nValue = _logic.get_card_value(GRR._cards_data_niao[i]);
			int seat = 0;
			seat = (_cur_banker + (nValue - 1) % 4) % 4;
			GRR._player_niao_cards[seat][GRR._player_niao_count[seat]] = GRR._cards_data_niao[i];
			GRR._player_niao_count[seat]++;
		}

		GRR._count_pick_niao = 0;
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			for (int j = 0; j < GRR._player_niao_count[i]; j++) {
				if (seat_index == i) {
					GRR._count_pick_niao++;
					GRR._player_niao_cards[i][j] = this.set_ding_niao_valid(GRR._player_niao_cards[i][j], true);// 胡牌的鸟生效
				} else {
					GRR._player_niao_cards[i][j] = this.set_ding_niao_valid(GRR._player_niao_cards[i][j], false);// 胡牌的鸟生效
				}
			}
		}
	}

	@Override
	public boolean estimate_gang_respond(int seat_index, int card) {
		boolean bAroseAction = false;

		PlayerStatus playerStatus = null;

		int action = GameConstants.WIK_NULL;

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (seat_index == i)
				continue;

			playerStatus = _playerStatus[i];

			if (playerStatus.isAbandoned())
				continue;

			if (playerStatus.is_chi_hu_round() && !feng_pai[i]) {
				ChiHuRight chr = GRR._chi_hu_rights[i];
				chr.set_empty();

				action = analyse_chi_hu_card(GRR._cards_index[i], GRR._weave_items[i], GRR._weave_count[i], card, chr,
						GameConstants_NanNing.HU_CARD_TYPE_QIANG_GANG, i);

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

	public boolean estimate_player_out_card_respond(int seat_index, int card, int type) {
		boolean bAroseAction = false;

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_playerStatus[i].clean_action();
			_playerStatus[i].clean_weave();
		}

		PlayerStatus playerStatus = null;

		int action = GameConstants.WIK_NULL;

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (seat_index == i) {
				continue;
			}

			playerStatus = _playerStatus[i];

			if (GRR._left_card_count > 0) {
				action = _logic.check_peng(GRR._cards_index[i], card);
				if (action != 0) {
					playerStatus.add_action(action);
					playerStatus.add_peng(card, seat_index);
					bAroseAction = true;
				}

				if (GRR._left_card_count > 0) {
					action = _logic.estimate_gang_card_out_card(GRR._cards_index[i], card);
					if (action != 0) {
						playerStatus.add_action(GameConstants.WIK_GANG);
						playerStatus.add_gang(card, seat_index, 1); // 加上杠
						bAroseAction = true;
					}
				}
			}

			if (_playerStatus[i].is_chi_hu_round() && !feng_pai[i]) {
				boolean can_hu_this_card = true;
				int[] tmp_cards_data = _playerStatus[i].get_cards_abandoned_hu();
				for (int x = 0; x < GameConstants.MAX_ABANDONED_CARDS_COUNT; x++) {
					if (tmp_cards_data[x] == card) {
						can_hu_this_card = false;
						break;
					}
				}
				if (can_hu_this_card) {
					ChiHuRight chr = GRR._chi_hu_rights[i];
					chr.set_empty();
					int cbWeaveCount = GRR._weave_count[i];

					int card_type = GameConstants_NanNing.HU_CARD_TYPE_JIE_PAO;
					if (type == GameConstants.GANG_TYPE_AN_GANG || type == GameConstants.GANG_TYPE_ADD_GANG
							|| type == GameConstants.GANG_TYPE_JIE_GANG) {
						card_type = GameConstants_NanNing.HU_CARD_TYPE_GANG_PAO;
					}

					action = analyse_chi_hu_card(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr, card_type, i);

					if (action != 0) {
						_playerStatus[i].add_action(GameConstants.WIK_CHI_HU);
						_playerStatus[i].add_chi_hu(card, seat_index);
						bAroseAction = true;
					}
				}
			}
		}

		if (bAroseAction) {
			_resume_player = _current_player;
			_current_player = GameConstants.INVALID_SEAT;
			_provide_player = seat_index;
		} else {
			return false;
		}

		return true;
	}

	public int get_ting_card(int[] cards, int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int seat_index) {
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
					GameConstants_NanNing.HU_CARD_TYPE_ZI_MO, seat_index)) {
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
	protected int get_banker_next_seat(int banker_seat) {
		int count = 0;
		int seat = banker_seat;
		do {
			count++;
			seat = (seat + 1 + getTablePlayerNumber()) % getTablePlayerNumber();
		} while (get_players()[seat] == null && count <= 5);
		return seat;
	}

	@Override
	public int get_real_card(int card) {
		return card;
	}

	@Override
	public int getTablePlayerNumber() {
		if (has_rule(GameConstants_NanNing.GAME_RULE_PLAYER_3)) {
			return 3;
		}
		return 4;
	}

	protected int get_seat(int nValue, int seat_index) {
		int seat = 0;
		if (getTablePlayerNumber() == 4) { // 四人场
			seat = (seat_index + (nValue - 1) % 4) % 4;
		} else { // 三人场，所有胡牌人的对家都是那个空位置
			int v = (nValue - 1) % 4;
			switch (v) {
			case 0:
				seat = seat_index;
				break;
			case 1:
				seat = get_banker_next_seat(seat_index);
				break;
			case 2:
				seat = get_null_seat();
				break;
			case 3:
				seat = get_banker_pre_seat(seat_index);
				break;
			default:
				break;
			}
		}
		return seat;
	}

	public boolean is_ting_card(int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int seat_index) {
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		ChiHuRight chr = new ChiHuRight();
		for (int i = 0; i < GameConstants.MAX_ZI_FENG; i++) {
			chr.set_empty();
			int cbCurrentCard = _logic.switch_to_card_data(i);
			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card(cbCardIndexTemp, weaveItem, cbWeaveCount, cbCurrentCard, chr,
					GameConstants_NanNing.HU_CARD_TYPE_ZI_MO, seat_index))
				return true;
		}
		return false;
	}

	@Override
	protected boolean on_game_start() {
		feng_pai = new boolean[getTablePlayerNumber()];
		feng_pai_4_orther_seat = new int[getTablePlayerNumber()];
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			feng_pai_4_orther_seat[i] = -1;
		}
		quan_bao = new int[getTablePlayerNumber()];
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			quan_bao[i] = -1;
		}

		_game_status = GameConstants.GS_MJ_PLAY;

		GameStartResponse.Builder gameStartResponse = GameStartResponse.newBuilder();
		gameStartResponse.setBankerPlayer(GRR._banker_player);
		gameStartResponse.setCurrentPlayer(_current_player);
		gameStartResponse.setLeftCardCount(GRR._left_card_count);

		int hand_cards[][] = new int[this.getTablePlayerNumber()][GameConstants.MAX_COUNT];

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[i], hand_cards[i]);
			gameStartResponse.addCardsCount(hand_card_count);
		}

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

			gameStartResponse.clearCardData();
			for (int j = 0; j < GameConstants.MAX_COUNT; j++) {
				gameStartResponse.addCardData(hand_cards[i][j]);
			}

			GRR._video_recode.addHandCards(cards);

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			this.load_room_info_data(roomResponse);
			this.load_common_status(roomResponse);
			roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
			roomResponse.setGameStart(gameStartResponse);
			roomResponse.setCurrentPlayer(_current_player == GameConstants.INVALID_SEAT ? _resume_player : _current_player);
			roomResponse.setLeftCardCount(GRR._left_card_count);
			roomResponse.setGameStatus(_game_status);
			roomResponse.setLeftCardCount(GRR._left_card_count);
			this.send_response_to_player(i, roomResponse);
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
		this.load_room_info_data(roomResponse);
		this.load_common_status(roomResponse);
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

			for (int j = 0; j < GameConstants.MAX_COUNT; j++) {
				cards.addItem(hand_cards[i][j]);
			}
			gameStartResponse.addCardsData(cards);
		}
		roomResponse.setGameStart(gameStartResponse);
		roomResponse.setLeftCardCount(GRR._left_card_count);
		GRR.add_room_response(roomResponse);

		// 检测听牌
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_playerStatus[i]._hu_card_count = this.get_ting_card(_playerStatus[i]._hu_cards, GRR._cards_index[i], GRR._weave_items[i],
					GRR._weave_count[i], i);
			if (_playerStatus[i]._hu_card_count > 0) {
				this.operate_chi_hu_cards(i, _playerStatus[i]._hu_card_count, _playerStatus[i]._hu_cards);
			}
		}

		this.exe_dispatch_card(_current_player, GameConstants_NanNing.DispatchCard_Type_Tian_Hu, GameConstants.DELAY_SEND_CARD_DELAY);

		return true;
	}

	@Override
	protected boolean on_handler_game_finish(int seat_index, int reason) {
		int real_reason = reason;

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
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
		game_end.setRunPlayerId(_run_player_id);
		game_end.setRoundOverType(0);
		game_end.setGamePlayerNumber(this.getTablePlayerNumber());
		game_end.setEndTime(System.currentTimeMillis() / 1000L);
		if (GRR != null) {
			game_end.setRoundOverType(1);
			game_end.setStartTime(GRR._start_time);

			game_end.setGameTypeIndex(GRR._game_type_index);
			roomResponse.setLeftCardCount(GRR._left_card_count);

			for (int i = 0; i < GRR._especial_card_count; i++) {
				game_end.addEspecialShowCards(GRR._especial_show_cards[i]);
			}

			GRR._end_type = reason;

			float lGangScore[] = new float[this.getTablePlayerNumber()];

			for (int i = 0; i < this.getTablePlayerNumber(); i++) {

				if (GRR._end_type != GameConstants.Game_End_DRAW) { // 荒庄荒杠
				}
				for (int j = 0; j < GRR._gang_score[i].gang_count; j++) {
					for (int k = 0; k < this.getTablePlayerNumber(); k++) {
						lGangScore[k] += GRR._gang_score[i].scores[j][k];// 杠牌，每个人的分数
					}
				}

				for (int j = 0; j < this.getTablePlayerNumber(); j++) {
					_player_result.lost_fan_shu[i][j] += GRR._lost_fan_shu[i][j];
				}

			}

			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				GRR._game_score[i] += lGangScore[i];
				GRR._game_score[i] += GRR._start_hu_score[i];

				_player_result.game_score[i] += GRR._game_score[i];
			}

			this.load_player_info_data(roomResponse);

			game_end.setGameRound(_game_round);
			game_end.setCurRound(_cur_round);

			game_end.setCellScore(GameConstants.CELL_SCORE);

			game_end.setBankerPlayer(GRR._banker_player);
			game_end.setLeftCardCount(GRR._left_card_count);
			game_end.setShowBirdEffect(GRR._show_bird_effect == false ? 0 : 1);

			for (int i = 0; i < GameConstants.MAX_NIAO_CARD && i < GRR._count_niao; i++) {
				game_end.addCardsDataNiao(GRR._cards_data_niao[i]);
			}
			for (int i = 0; i < GameConstants.MAX_NIAO_CARD && i < GRR._count_niao_fei; i++) {
				game_end.addCardsDataNiao(GRR._cards_data_niao_fei[i]);
			}
			game_end.setCountPickNiao(GRR._count_pick_niao + GRR._count_pick_niao_fei);// 中鸟个数

			for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
				// 鸟牌，必须按四人计算
				Int32ArrayResponse.Builder pnc = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GRR._player_niao_count[i]; j++) {
					pnc.addItem(GRR._player_niao_cards[i][j]);
				}
				for (int j = 0; j < GRR._player_niao_count_fei[i]; j++) {
					pnc.addItem(GRR._player_niao_cards_fei[i][j]);
				}
				game_end.addPlayerNiaoCards(pnc);
			}

			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				game_end.addHuResult(GRR._hu_result[i]);
				Int32ArrayResponse.Builder hc = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GameConstants.MAX_COUNT; j++) {
					hc.addItem(GRR._chi_hu_card[i][j]);
				}

				for (int h = 0; h < GRR._chi_hu_card[i].length; h++) {
					game_end.addHuCardData(GRR._chi_hu_card[i][h]);
				}

				game_end.addHuCardArray(hc);
			}

			// 现在权值只有一位
			long rv[] = new long[GameConstants.MAX_RIGHT_COUNT];

			this.set_result_describe();

			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				GRR._card_count[i] = _logic.switch_to_cards_data(GRR._cards_index[i], GRR._cards_data[i]);

				Int32ArrayResponse.Builder cs = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GRR._card_count[i]; j++) {
					cs.addItem(GRR._cards_data[i][j]);
				}
				game_end.addCardsData(cs);

				WeaveItemResponseArrayResponse.Builder weaveItem_array = WeaveItemResponseArrayResponse.newBuilder();
				for (int j = 0; j < GRR._weave_count[i]; j++) {
					WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
					weaveItem_item.setCenterCard(GRR._weave_items[i][j].center_card);
					weaveItem_item.setProvidePlayer(GRR._weave_items[i][j].provide_player);
					weaveItem_item.setPublicCard(GRR._weave_items[i][j].public_card);
					weaveItem_item.setWeaveKind(GRR._weave_items[i][j].weave_kind);
					weaveItem_array.addWeaveItem(weaveItem_item);
				}
				game_end.addWeaveItemArray(weaveItem_array);

				GRR._chi_hu_rights[i].get_right_data(rv);
				game_end.addChiHuRight(rv[0]);

				GRR._start_hu_right[i].get_right_data(rv);
				game_end.addStartHuRight(rv[0]);

				game_end.addProvidePlayer(GRR._provider[i]);
				game_end.addGameScore(GRR._game_score[i]);
				game_end.addGangScore(lGangScore[i]);
				game_end.addStartHuScore(GRR._start_hu_score[i]);
				game_end.addResultDes(GRR._result_des[i]);

				game_end.addWinOrder(GRR._win_order[i]);

				Int32ArrayResponse.Builder lfs = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < this.getTablePlayerNumber(); j++) {
					lfs.addItem(GRR._lost_fan_shu[i][j]);
				}

				game_end.addLostFanShu(lfs);

			}

		}

		boolean end = false;
		if (reason == GameConstants.Game_End_NORMAL || reason == GameConstants.Game_End_DRAW) {
			if (_cur_round >= _game_round && (!is_sys())) {
				end = true;
				game_end.setRoomOverType(1);
				game_end.setPlayerResult(this.process_player_result(reason));
			} else {
			}
		} else if ((!is_sys()) && (reason == GameConstants.Game_End_RELEASE_PLAY || reason == GameConstants.Game_End_RELEASE_NO_BEGIN
				|| reason == GameConstants.Game_End_RELEASE_RESULT || reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT || reason == GameConstants.Game_End_RELEASE_SYSTEM)) {
			end = true;
			real_reason = GameConstants.Game_End_DRAW;
			game_end.setRoomOverType(1);
			game_end.setPlayerResult(this.process_player_result(reason));
		}
		game_end.setEndType(real_reason);

		roomResponse.setGameEnd(game_end);

		this.send_response_to_room(roomResponse);

		record_game_round(game_end);

		if (reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT || reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT) {
			for (int j = 0; j < this.getTablePlayerNumber(); j++) {
				Player player = this.get_players()[j];
				if (player == null)
					continue;
				send_error_notify(j, 1, "游戏解散成功!");

			}
		}

		if (end && (!is_sys())) {
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
	public void process_chi_hu_player_operate(int seat_index, int operate_card, boolean rm) {
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];
		int effect_count = chr.type_count;
		long effect_indexs[] = new long[effect_count];
		for (int i = 0; i < effect_count; i++) {
			effect_indexs[i] = chr.type_list[i];
		}

		this.operate_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_HU, effect_count, effect_indexs, 1, GameConstants.INVALID_SEAT);

		this.operate_player_cards(seat_index, 0, null, 0, null);

		if (rm) {
			GRR._cards_index[seat_index][_logic.switch_to_card_index(operate_card)]--;
		}

		int cards[] = new int[GameConstants.MAX_COUNT];
		int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[seat_index], cards);

		cards[hand_card_count] = operate_card + GameConstants.CARD_ESPECIAL_TYPE_HU;
		hand_card_count++;

		this.operate_show_card(seat_index, GameConstants.Show_Card_HU, hand_card_count, cards, GameConstants.INVALID_SEAT);

		return;
	}

	@Override
	public void process_chi_hu_player_score(int seat_index, int provide_index, int operate_card, boolean zimo) {
		GRR._chi_hu_card[seat_index][0] = operate_card;

		GRR._win_order[seat_index] = 1;

		ChiHuRight chr = GRR._chi_hu_rights[seat_index];

		// TODO:
		int di_fen = 1;

		countCardType(chr, seat_index);

		int lChiHuScore = di_fen * getProcessScore(chr, zimo, GRR._cards_index[provide_index]);
		lChiHuScore += lChiHuScore * GRR._count_pick_niao;

		if (zimo) {
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				if (i == seat_index)
					continue;

				GRR._lost_fan_shu[i][seat_index] = di_fen;
			}
		} else {
			GRR._lost_fan_shu[provide_index][seat_index] = di_fen;
		}

		if (zimo) {
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				if (i == seat_index)
					continue;

				int s = lChiHuScore;
				s *= 1 << getFan(chr, zimo, seat_index, provide_index, i);

				if (quan_bao[seat_index] != -1) {
					GRR._game_score[quan_bao[seat_index]] -= s;
				} else if (feng_pai_4_orther_seat[seat_index] != -1) {
					GRR._game_score[feng_pai_4_orther_seat[seat_index]] -= s;
				} else {
					GRR._game_score[i] -= s;
				}
				GRR._game_score[seat_index] += s;

				if (quan_bao[seat_index] != -1 && feng_pai_4_orther_seat[seat_index] != -1
						&& quan_bao[seat_index] != feng_pai_4_orther_seat[seat_index]) {
					GRR._game_score[feng_pai_4_orther_seat[seat_index]] -= s;
					GRR._game_score[seat_index] += s;
				}
			}
		} else {
			if (!GRR._chi_hu_rights[seat_index].opr_and(GameConstants_NanNing.CHR_GANG_KAI).is_empty()) {
				lChiHuScore = di_fen * getProcessScore(chr, true, GRR._cards_index[provide_index]);
				lChiHuScore += lChiHuScore * GRR._count_pick_niao;

				for (int i = 0; i < this.getTablePlayerNumber(); i++) {
					if (i == seat_index)
						continue;

					int s = lChiHuScore;
					s *= 1 << getFan(chr, zimo, seat_index, provide_index, i);

					if (quan_bao[seat_index] != -1) {
						GRR._game_score[quan_bao[seat_index]] -= s;
					} else if (feng_pai_4_orther_seat[seat_index] != -1) {
						GRR._game_score[feng_pai_4_orther_seat[seat_index]] -= s;
					} else {
						GRR._game_score[provide_index] -= s;
					}
					GRR._game_score[seat_index] += s;

					if (quan_bao[seat_index] != -1 && feng_pai_4_orther_seat[seat_index] != -1
							&& quan_bao[seat_index] != feng_pai_4_orther_seat[seat_index]) {
						GRR._game_score[provide_index] -= s;
						GRR._game_score[seat_index] += s;
					}
				}
			} else {
				int cards[] = new int[GameConstants.MAX_COUNT];
				int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[provide_index], cards);
				if (hand_card_count == 1) {
					GRR._chi_hu_rights[provide_index].opr_or(GameConstants_NanNing.CHR_QUAN_QIU_PAO);
				}

				int s = lChiHuScore;

				s *= 1 << getFan(chr, zimo, seat_index, provide_index, provide_index);

				GRR._game_score[provide_index] -= s;
				GRR._game_score[seat_index] += s;

				if (feng_pai_4_orther_seat[seat_index] != -1 && feng_pai_4_orther_seat[seat_index] != provide_index) {
					GRR._game_score[feng_pai_4_orther_seat[seat_index]] -= s;
					GRR._game_score[seat_index] += s;
				}
			}
		}

		GRR._provider[seat_index] = provide_index;

		change_player_status(seat_index, GameConstants.INVALID_VALUE);

		return;
	}

	private int getFan(ChiHuRight chr, boolean zi_mo, int hu_seat, int provide_index, int orther_index) {
		int fan = 0;
		if (!chr.opr_and(GameConstants_NanNing.CHR_GANG_KAI).is_empty()) {
			fan++;
		}
		if (zi_mo) {
			if (has_rule(GameConstants_NanNing.GAME_RULE_SI_SHUANG) && orther_index == feng_pai_4_orther_seat[hu_seat]) {
				fan++;
			} else if (orther_index == feng_pai_4_orther_seat[hu_seat]) {
				fan++;
			}
		} else {
			if (has_rule(GameConstants_NanNing.GAME_RULE_SI_SHUANG) && provide_index == feng_pai_4_orther_seat[hu_seat]) {
				fan++;
			} else if (provide_index == feng_pai_4_orther_seat[hu_seat]) {
				fan++;
			}

			if (!GRR._chi_hu_rights[hu_seat].opr_and(GameConstants_NanNing.CHR_GANG_SHANG_PAO).is_empty())
				fan++;
			if (!GRR._chi_hu_rights[hu_seat].opr_and(GameConstants_NanNing.CHR_QIANG_GANG_HU).is_empty())
				fan++;
		}
		return fan;
	}

	private int getProcessScore(ChiHuRight chr, boolean zi_mo, int[] cards_index) {

		int cards[] = new int[GameConstants.MAX_COUNT];
		int hand_card_count = _logic.switch_to_cards_data(cards_index, cards);

		// 基本胡
		if (!chr.opr_and(GameConstants_NanNing.CHR_JI_BEN_HU).is_empty()) {
			if (zi_mo) {
				if (!chr.opr_and(GameConstants_NanNing.CHR_MEN_QING).is_empty()) {
					return 4;
				}
				return 3;
			} else {
				if (hand_card_count == 1)
					return 9;
				return 2;
			}
		}
		// 清一色碰碰胡全球人
		if (!chr.opr_and(GameConstants_NanNing.CHR_QUAN_QIU_REN).is_empty() && !chr.opr_and(GameConstants_NanNing.CHR_PENG_PENG_HU).is_empty()
				&& !chr.opr_and(GameConstants_NanNing.CHR_QING_YI_SE).is_empty()) {
			if (zi_mo) {
				return 12;
			} else {
				return 72;
			}
		}

		// 清一色全球人
		if (!chr.opr_and(GameConstants_NanNing.CHR_QUAN_QIU_REN).is_empty() && !chr.opr_and(GameConstants_NanNing.CHR_QING_YI_SE).is_empty()) {
			if (zi_mo) {
				return 12;
			} else {
				return 36;
			}
		}
		// 碰碰胡全球人
		if (!chr.opr_and(GameConstants_NanNing.CHR_QUAN_QIU_REN).is_empty() && !chr.opr_and(GameConstants_NanNing.CHR_PENG_PENG_HU).is_empty()) {
			if (zi_mo) {
				return 12;
			} else {
				return 36;
			}
		}

		// 清一色碰碰胡
		if (!chr.opr_and(GameConstants_NanNing.CHR_PENG_PENG_HU).is_empty() && !chr.opr_and(GameConstants_NanNing.CHR_QING_YI_SE).is_empty()) {
			if (zi_mo) {
				if (!chr.opr_and(GameConstants_NanNing.CHR_MEN_QING).is_empty()) {
					return 16;
				}
				return 12;
			} else {
				if (hand_card_count == 1)
					return 36;
				return 18;
			}
		}

		// 清一色七小对
		if (!chr.opr_and(GameConstants_NanNing.CHR_XIAO_QI_DUI).is_empty() && !chr.opr_and(GameConstants_NanNing.CHR_QING_YI_SE).is_empty()) {
			if (zi_mo) {
				return 16;
			} else {
				if (hand_card_count == 1)
					return 36;
				return 18;
			}
		}

		// 碰碰胡
		if (!chr.opr_and(GameConstants_NanNing.CHR_PENG_PENG_HU).is_empty()) {
			if (zi_mo) {
				if (!chr.opr_and(GameConstants_NanNing.CHR_MEN_QING).is_empty()) {
					return 8;
				}
				return 6;
			} else {
				if (hand_card_count == 1)
					return 18;
				return 9;
			}
		}

		// 七小对
		if (!chr.opr_and(GameConstants_NanNing.CHR_XIAO_QI_DUI).is_empty()) {
			if (zi_mo) {
				return 8;
			} else {
				if (hand_card_count == 1)
					return 18;
				return 9;
			}
		}

		// 豪华七小对
		if (!chr.opr_and(GameConstants_NanNing.CHR_HAO_HUA_QI_XIAO_DUI).is_empty()) {
			if (zi_mo) {
				return 16;
			} else {
				if (hand_card_count == 1)
					return 36;
				return 18;
			}
		}

		// 双豪华七小对
		if (!chr.opr_and(GameConstants_NanNing.CHR_SHUANG_QI_XIAO_DUI).is_empty()) {
			if (zi_mo) {
				return 32;
			} else {
				if (hand_card_count == 1)
					return 72;
				return 36;
			}
		}

		// 三豪华七小对
		if (!chr.opr_and(GameConstants_NanNing.CHR_SAN_QI_XIAO_DUI).is_empty()) {
			if (zi_mo) {
				return 64;
			} else {
				if (hand_card_count == 1)
					return 144;
				return 72;
			}
		}

		// 清一色
		if (!chr.opr_and(GameConstants_NanNing.CHR_QING_YI_SE).is_empty()) {
			if (zi_mo) {
				if (!chr.opr_and(GameConstants_NanNing.CHR_MEN_QING).is_empty()) {
					return 8;
				}
				return 6;
			} else {
				if (hand_card_count == 1)
					return 18;
				return 9;
			}
		}

		return 0;
	}

	@Override
	protected void set_result_describe() {
		int chrTypes;
		long type = 0;
		for (int player = 0; player < this.getTablePlayerNumber(); player++) {
			StringBuilder result = new StringBuilder("");

			if (feng_pai[player]) {
				result.append("碰三比");
			}

			chrTypes = GRR._chi_hu_rights[player].type_count;

			for (int typeIndex = 0; typeIndex < chrTypes; typeIndex++) {
				type = GRR._chi_hu_rights[player].type_list[typeIndex];

				if (GRR._chi_hu_rights[player].is_valid()) {
					if (type == GameConstants_NanNing.CHR_ZI_MO) {
						result.append(" 自摸");
					}
					if (type == GameConstants_NanNing.CHR_JIE_PAO) {
						result.append(" 接炮");
					}
					if (type == GameConstants_NanNing.CHR_JI_BEN_HU) {
						result.append(" 基本胡");
					}
					if (type == GameConstants_NanNing.CHR_PENG_PENG_HU) {
						result.append(" 碰碰胡");
					}
					if (type == GameConstants_NanNing.CHR_QING_YI_SE) {
						result.append(" 清一色");
					}
					if (type == GameConstants_NanNing.CHR_XIAO_QI_DUI) {
						result.append(" 七小对");
					}
					if (type == GameConstants_NanNing.CHR_HAO_HUA_QI_XIAO_DUI) {
						result.append(" 七大对");
					}
					if (type == GameConstants_NanNing.CHR_QIANG_GANG_HU) {
						result.append(" 抢杠胡");
					}
					if (type == GameConstants_NanNing.CHR_GANG_KAI) {
						result.append(" 杠上开花");
					}
					if (type == GameConstants_NanNing.CHR_GANG_SHANG_PAO) {
						result.append(" 杠上炮");
					}
					if (type == GameConstants_NanNing.CHR_QUAN_QIU_REN) {
						result.append(" 全求人");
					}
					if (type == GameConstants_NanNing.CHR_MEN_QING) {
						result.append(" 门清");
					}
					if (type == GameConstants_NanNing.CHR_SHUANG_QI_XIAO_DUI) {
						result.append(" 双七大对");
					}
					if (type == GameConstants_NanNing.CHR_SAN_QI_XIAO_DUI) {
						result.append(" 三七大对");
					}

				} else if (type == GameConstants_NanNing.CHR_FANG_PAO) {
					result.append(" 放炮");
				} else if (type == GameConstants_NanNing.CHR_QUAN_QIU_PAO) {
					result.append(" 全求炮");
				}
			}

			if (GRR._chi_hu_rights[player].is_valid()) {
				if (GRR._count_pick_niao > 0)
					result.append(" 中码X" + GRR._count_pick_niao);
			}

			int jie_gang = 0, fang_gang = 0, ming_gang = 0, an_gang = 0;

			if (GRR != null) {
				for (int tmpPlayer = 0; tmpPlayer < this.getTablePlayerNumber(); tmpPlayer++) {
					for (int w = 0; w < GRR._weave_count[tmpPlayer]; w++) {
						if (GRR._weave_items[tmpPlayer][w].weave_kind != GameConstants.WIK_GANG) {
							continue;
						}
						if (tmpPlayer == player) {
							if (GRR._weave_items[tmpPlayer][w].provide_player != tmpPlayer) {
								jie_gang++;
							} else {
								if (GRR._weave_items[tmpPlayer][w].public_card == 1) {
									ming_gang++;
								} else {
									an_gang++;
								}
							}
						} else {
							if (GRR._weave_items[tmpPlayer][w].provide_player == player) {
								fang_gang++;
							}
						}
					}
				}
			}

			if (an_gang > 0) {
				result.append(" 暗杠X" + an_gang);
			}
			if (ming_gang > 0) {
				result.append(" 明杠X" + ming_gang);
			}
			if (fang_gang > 0) {
				result.append(" 放杠X" + fang_gang);
			}
			if (jie_gang > 0) {
				result.append(" 接杠X" + jie_gang);
			}
			// }

			GRR._result_des[player] = result.toString();
		}
	}

	@Override
	public void test_cards() {
		int[] cards_of_player0 = new int[] { 0x02, 0x02, 0x02, 0x03, 0x03, 0x03, 0x04, 0x04, 0x04, 0x05, 0x05, 0x05, 0x06 };
		int[] cards_of_player1 = new int[] { 0x03, 0x03, 0x03, 0x05, 0x05, 0x05, 0x06, 0x06, 0x06, 0x08, 0x08, 0x08, 0x09 };
		int[] cards_of_player3 = new int[] { 0x03, 0x03, 0x03, 0x05, 0x05, 0x05, 0x06, 0x06, 0x06, 0x08, 0x08, 0x08, 0x09 };
		int[] cards_of_player2 = new int[] { 0x03, 0x03, 0x03, 0x05, 0x05, 0x05, 0x06, 0x06, 0x06, 0x08, 0x08, 0x08, 0x09 };

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			for (int j = 0; j < GameConstants.MAX_INDEX; j++) {
				GRR._cards_index[i][j] = 0;
			}
		}

		for (int j = 0; j < 13; j++) {
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
				if (debug_my_cards.length > 14) {
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
	public boolean trustee_timer(int operate_id, int seat_index) {
		return true;
	}

}
