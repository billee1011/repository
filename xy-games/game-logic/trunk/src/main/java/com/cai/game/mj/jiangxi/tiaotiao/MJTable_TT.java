package com.cai.game.mj.jiangxi.tiaotiao;

import java.util.Arrays;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.mj.GameConstants_TIAOTIAO;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.Player;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.WeaveItem;
import com.cai.common.util.PBUtil;
import com.cai.common.util.PerformanceTimer;
import com.cai.game.mj.AbstractMJTable;
import com.cai.game.mj.MJType;
import com.cai.game.mj.handler.MJHandlerFinish;
import com.cai.game.util.AnalyseCardUtil;
import com.cai.game.util.GameUtilConstants;
import com.cai.service.PlayerServiceImpl;

import protobuf.clazz.Protocol.GameEndResponse;
import protobuf.clazz.Protocol.GameStartResponse;
import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.PlayerResultResponse;
import protobuf.clazz.Protocol.RoomInfo;
import protobuf.clazz.Protocol.RoomPlayerResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;
import protobuf.clazz.gzcg.GZCGRsp.GameEndResponse_GZCG;
import protobuf.clazz.mj.Basic.MJ_GAME_END_INFO_EXT;

public class MJTable_TT extends AbstractMJTable {

	public int hu_score[];// 红中麻将胡分统计
	public int niao_score[];// 红中麻将鸟分统计
	public int piao_score[];// 红中麻将飘分显示
	public int hu_type[]; // 红中麻将胡牌类型
	public MJHandlerQiShouHongZhong_TT _handler_qishou_hongzhong;
	public MJHandlerPiao_TT _handler_piao; // 红中飘分
	public boolean HongZhongJiangNiao[]; // 无红中奖鸟
	public int niaoPlayer;
	public int[] niao_count; // 鸟数据
	public int piao_count[];
	public int liuPiaCount; // 留牌数量

	/**
	* 
	*/
	public MJTable_TT() {
		super(MJType.GAME_TYPE_MJ_TT);
	}

	private static final long serialVersionUID = 1L;

	public int get_ting_card(int[] cards, int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, boolean dai_feng,
			int seatIndex) {
		PerformanceTimer timer = new PerformanceTimer();

		int cbCardIndexTemp[] = new int[GameConstants_TIAOTIAO.MAX_INDEX];
		for (int i = 0; i < GameConstants_TIAOTIAO.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		ChiHuRight chr = new ChiHuRight();

		int count = 0;
		int cbCurrentCard;

		int l = GameConstants_TIAOTIAO.MAX_ZI;
		for (int i = 0; i < l; i++) {
			if (this._logic.is_magic_index(i))
				continue;
			cbCurrentCard = _logic.switch_to_card_data(i);
			chr.set_empty();
			if (GameConstants_TIAOTIAO.WIK_CHI_HU == analyse_chi_hu_card(cbCardIndexTemp, weaveItem, cbWeaveCount,
					cbCurrentCard, chr, GameConstants_TIAOTIAO.HU_CARD_TYPE_ZIMO, seatIndex)) {
				cards[count] = cbCurrentCard;
				count++;
			}
		}

		l -= 1;
		if (count == 0) {
			int max_hz = 4;
			if (cards_index[this._logic.get_magic_card_index(0)] == (max_hz - 1)) {
				cards[count] = _logic.switch_to_card_data(this._logic.get_magic_card_index(0));
				count++;
			}
		} else if (count > 0) {
			cards[count] = _logic.switch_to_card_data(this._logic.get_magic_card_index(0));
			count++;
		} else {
			count = 1;
			cards[0] = -1;
		}

		if (timer.get() > 50) {
			logger.warn("cost time too long " + Arrays.toString(cards_index) + ", cost time = " + timer.duration());
		}

		return count;
	}

	/**
	 * 添加留牌
	 */
	public void addLiuPai() {
		int count = 0;
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			for (int l = 0; l < GRR._weave_count[i]; l++) {
				if (GRR._weave_items[i][l].weave_kind == GameConstants.WIK_GANG) {
					count++;
				}
			}
		}
		if (count % 2 == 1) {
			if (GRR._left_card_count - liuPiaCount >= 2) {
				liuPiaCount += 2;
			} else if (GRR._left_card_count - liuPiaCount == 1) {
				liuPiaCount++;
			}
		}
	}

	@Override
	protected void onInitTable() {
		niao_count = new int[GameConstants.GAME_PLAYER];
		piao_count = new int[GameConstants.GAME_PLAYER];
		_handler_dispath_card = new MJHandlerDispatchCard_TT();
		_handler_out_card_operate = new MJHandlerOutCardOperate_TT();
		_handler_gang = new MJHandlerGang_TT();
		_handler_chi_peng = new MJHandlerChiPeng_TT();

		_handler_qishou_hongzhong = new MJHandlerQiShouHongZhong_TT();
		_handler_piao = new MJHandlerPiao_TT();
		_handler_finish = new MJHandlerFinish();
		HongZhongJiangNiao = new boolean[getTablePlayerNumber()];
		niaoPlayer = GameConstants.INVALID_SEAT;
		liuPiaCount = 14;
		// 红中癞子添加
		_logic.add_magic_card_index(_logic.switch_to_card_index(GameConstants_TIAOTIAO.HZ_MAGIC_CARD));
	}

	@Override
	public int getTablePlayerNumber() {
		if(playerNumber > 0){
			return playerNumber;
		}else if (has_rule(GameConstants_TIAOTIAO.GAME_RULE_SAN_REN)) {
			return GameConstants_TIAOTIAO.GAME_PLAYER - 1;
		} else if (has_rule(GameConstants_TIAOTIAO.GAME_RULE_ER_REN)) {
			return GameConstants_TIAOTIAO.GAME_PLAYER - 2;
		}
		return GameConstants_TIAOTIAO.GAME_PLAYER;
	}

	@Override
	protected boolean on_game_start() {

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_player_result.pao[i] = -1;
		}

		// 红中麻将添加飘分
		if (!has_rule(GameConstants_TIAOTIAO.GAME_RULE_BU_PIAO)) {
			set_handler(_handler_piao);
			_handler_piao.exe(this);
			return true;
		}
		return on_game_start_hz_real();
	}

	public boolean on_game_start_hz_real() {
		hu_score = new int[getTablePlayerNumber()];// 红中麻将胡分统计
		niao_score = new int[getTablePlayerNumber()];// 红中麻将鸟分统计
		piao_score = new int[getTablePlayerNumber()];// 红中麻将飘分显示
		hu_type = new int[getTablePlayerNumber()]; // 红中麻将胡牌类型
		niaoPlayer = GameConstants.INVALID_SEAT;
		liuPiaCount = 14;
		this.show_tou_zi(_current_player);
		_game_status = GameConstants_TIAOTIAO.GS_MJ_PLAY;

		GameStartResponse.Builder gameStartResponse = GameStartResponse.newBuilder();
		gameStartResponse.setBankerPlayer(GRR._banker_player);
		gameStartResponse.setCurrentPlayer(_current_player);
		gameStartResponse.setLeftCardCount(GRR._left_card_count);

		int hand_cards[][] = new int[getTablePlayerNumber()][GameConstants_TIAOTIAO.MAX_COUNT];
		// 发送数据
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[i], hand_cards[i]);
			gameStartResponse.addCardsCount(hand_card_count);
		}

		// 发送数据
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

			// 只发自己的牌
			gameStartResponse.clearCardData();
			for (int j = 0; j < GameConstants_TIAOTIAO.MAX_COUNT; j++) {
				gameStartResponse.addCardData(hand_cards[i][j]);
			}

			// 回放数据
			GRR._video_recode.addHandCards(cards);

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			this.load_room_info_data(roomResponse);
			this.load_common_status(roomResponse);
			roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
			roomResponse.setGameStart(gameStartResponse);
			roomResponse.setCurrentPlayer(
					_current_player == GameConstants_TIAOTIAO.INVALID_SEAT ? _resume_player : _current_player);
			roomResponse.setLeftCardCount(GRR._left_card_count);
			roomResponse.setGameStatus(_game_status);
			roomResponse.setLeftCardCount(GRR._left_card_count);
			this.send_response_to_player(i, roomResponse);
		}
		////////////////////////////////////////////////// 回放
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
		this.load_room_info_data(roomResponse);
		this.load_common_status(roomResponse);
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

			for (int j = 0; j < GameConstants_TIAOTIAO.MAX_COUNT; j++) {
				cards.addItem(hand_cards[i][j]);
			}
			gameStartResponse.addCardsData(cards);
		}

		roomResponse.setGameStart(gameStartResponse);
		roomResponse.setLeftCardCount(GRR._left_card_count);
		GRR.add_room_response(roomResponse);
		///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

		// 检测听牌
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_playerStatus[i]._hu_card_count = this.get_ting_card(_playerStatus[i]._hu_cards, GRR._cards_index[i],
					GRR._weave_items[i], GRR._weave_count[i], false, i);
			if (_playerStatus[i]._hu_card_count > 0) {
				this.operate_chi_hu_cards(i, _playerStatus[i]._hu_card_count, _playerStatus[i]._hu_cards);
			}
		}
		boolean is_qishou_hu = false;
		/*for (int i = 0; i < getTablePlayerNumber(); i++) {
			// 起手4个红中
			int max_hz = 4;
			if (GRR._cards_index[i][_logic.get_magic_card_index(0)] == max_hz) {

				_playerStatus[i].add_action(GameConstants_TIAOTIAO.WIK_ZI_MO);
				_playerStatus[i].add_zi_mo(_logic.switch_to_card_data(_logic.get_magic_card_index(0)), i);
				GRR._chi_hu_rights[i].opr_or(GameConstants_TIAOTIAO.CHR_ZI_MO);
				GRR._chi_hu_rights[i].opr_or(GameConstants_TIAOTIAO.CHR_HUNAN_HZ_QISHOU_HU);
				this.exe_qishou_hongzhong(i);
				is_qishou_hu = true;
				MongoDBServiceImpl.getInstance().card_log(this.get_players()[i], ECardType.hongZhong4, "", 0, 0l,
						this.getRoom_id());
				break;
			}
		}*/
		if (is_qishou_hu == false) {
			this.exe_dispatch_card(_current_player, GameConstants_TIAOTIAO.WIK_NULL,
					GameConstants_TIAOTIAO.DELAY_SEND_CARD_DELAY);
		}

		return false;
	}

	public boolean exe_qishou_hongzhong(int seat_index) {
		this.set_handler(this._handler_qishou_hongzhong);
		this._handler_qishou_hongzhong.reset_status(seat_index);
		this._handler_qishou_hongzhong.exe(this);

		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.cai.game.mj.AbstractMJTable#analyse_chi_hu_card(int[],
	 * com.cai.common.domain.WeaveItem[], int, int,
	 * com.cai.common.domain.ChiHuRight, int, int)
	 */
	@Override
	public int analyse_chi_hu_card(int[] cards_index, WeaveItem[] weaveItems, int weaveCount, int cur_card,
			ChiHuRight chiHuRight, int card_type, int _seat_index) {
		if ((!has_rule(GameConstants_TIAOTIAO.GAME_RULE_KE_DIAN_PAO)
				&& (card_type == GameConstants_TIAOTIAO.HU_CARD_TYPE_PAOHU))) {
			return GameConstants_TIAOTIAO.WIK_NULL;
		}

		if (cur_card == 0) {
			return GameConstants_TIAOTIAO.WIK_NULL;
		}

		int result = GameConstants_TIAOTIAO.WIK_NULL;
		
		//4红中
		int max_hz = 4;
		int count = GameConstants_TIAOTIAO.HZ_MAGIC_CARD == cur_card ? 1 : 0;
		if ((cards_index[_logic.switch_to_card_index(GameConstants_TIAOTIAO.HZ_MAGIC_CARD)] + count == max_hz)) {
			result = GameConstants_TIAOTIAO.WIK_CHI_HU;
			chiHuRight.opr_or(GameConstants_TIAOTIAO.CHR_HUNAN_HZ_QISHOU_HU);
			if (card_type == GameConstants_TIAOTIAO.HU_CARD_TYPE_ZIMO) {
				chiHuRight.opr_or(GameConstants_TIAOTIAO.CHR_ZI_MO);
			} else {
				chiHuRight.opr_or(GameConstants_TIAOTIAO.CHR_SHU_FAN);
			}

		}

		if (chiHuRight.is_empty() == false) {
			return result;
		}

		//七小对
		if (has_rule(GameConstants_TIAOTIAO.GAME_RULE_KE_HU_QI_DUI)) {
			long qxd = _logic.is_qi_xiao_dui(cards_index, weaveItems, weaveCount, cur_card);
			if (qxd != GameConstants_TIAOTIAO.WIK_NULL) {
				result = GameConstants_TIAOTIAO.WIK_CHI_HU;
				chiHuRight.opr_or(GameConstants_TIAOTIAO.CHR_HUNAN_QI_XIAO_DUI);
				if (card_type == GameConstants_TIAOTIAO.HU_CARD_TYPE_ZIMO) {
					chiHuRight.opr_or(GameConstants_TIAOTIAO.CHR_ZI_MO);
				} else {
					chiHuRight.opr_or(GameConstants_TIAOTIAO.CHR_SHU_FAN);
				}
			}
		}
		if (chiHuRight.is_empty() == false) {
			return result;
		}
		

		int[] magic_cards_index = new int[GameUtilConstants.MAX_MAGIC_INDEX_COUNT];
		int magic_card_count = _logic.get_magic_card_count();

		if (magic_card_count > 2) { // 一般只有两种癞子牌存在
			magic_card_count = 2;
		}

		for (int i = 0; i < magic_card_count; i++) {
			magic_cards_index[i] = _logic.get_magic_card_index(i);
		}

		boolean can_win = AnalyseCardUtil.analyse_win_by_cards_index(cards_index, _logic.switch_to_card_index(cur_card),
				magic_cards_index, magic_card_count);

		if (!can_win) {
			chiHuRight.set_empty();
			return GameConstants_TIAOTIAO.WIK_NULL;
		}

		// 碰碰胡
		boolean pph = AnalyseCardUtil.analyse_peng_hu_by_cards_index(cards_index, _logic.switch_to_card_index(cur_card),
				magic_cards_index, magic_card_count);
		if (pph) {
			chiHuRight.opr_or(GameConstants_TIAOTIAO.CHR_HUNAN_PENGPENG_HU);
		}
		result = GameConstants_TIAOTIAO.WIK_CHI_HU;

		if (card_type == GameConstants_TIAOTIAO.HU_CARD_TYPE_ZIMO) {
			chiHuRight.opr_or(GameConstants_TIAOTIAO.CHR_ZI_MO);
		} else {
			chiHuRight.opr_or(GameConstants_TIAOTIAO.CHR_SHU_FAN);
		}

		return result;
	}

	/**
	 * 获取鸟的 数量
	 * 
	 * @param check
	 * @param add_niao
	 * @return
	 */
	public int get_niao_card_num(boolean check, int add_niao) {
		int nNum = GameConstants_TIAOTIAO.ZHANIAO_0;
		if (has_rule(GameConstants_TIAOTIAO.GAME_RULE_ZHUA_NIAO2)) {
			nNum = 2;
		} else if (has_rule(GameConstants_TIAOTIAO.GAME_RULE_ZHUA_NIAO4)) {
			nNum = 4;
		} else if (has_rule(GameConstants_TIAOTIAO.GAME_RULE_ZHUA_NIAO6)) {
			nNum = 6;
		}

		/*
		 * if (has_rule(GameConstants_TIAOTIAO.GAME_RULE_DUO_ZHUA_NIAO1)) { nNum
		 * += 1; } else if
		 * (has_rule(GameConstants_TIAOTIAO.GAME_RULE_DUO_ZHUA_NIAO2)) { nNum +=
		 * 2; }
		 */

		nNum += add_niao;

		if (check == false) {
			return nNum;
		}
		if (nNum > GRR._left_card_count) {
			nNum = GRR._left_card_count;
		}
		return nNum;
	}

	public void set_niao_card_hz(int seat_index, int card, boolean show, int add_niao, int hu_card) {
		niaoPlayer = seat_index;
		for (int i = 0; i < GameConstants_TIAOTIAO.MAX_NIAO_CARD; i++) {
			GRR._cards_data_niao[i] = GameConstants_TIAOTIAO.INVALID_VALUE;
		}

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			GRR._player_niao_count[i] = 0;
			for (int j = 0; j < GameConstants_TIAOTIAO.MAX_NIAO_CARD; j++) {
				GRR._player_niao_cards[i][j] = GameConstants_TIAOTIAO.INVALID_VALUE;
			}
		}

		GRR._show_bird_effect = show;

		GRR._count_niao = get_niao_card_num(true, add_niao);

		// 一码全鸟
		if (has_rule(GameConstants_TIAOTIAO.GAME_RULE_YI_MA_QUAN_NIAO)) {
			if (hu_card == GameConstants_TIAOTIAO.HZ_MAGIC_CARD) {
				GRR._count_niao = GRR._left_card_count > 10 ? 10 : GRR._left_card_count;
			} else {
				GRR._count_niao = _logic.get_card_value(hu_card);
			}
		}
		if (has_rule(GameConstants_TIAOTIAO.GAME_RULE_YI_MA_QUAN_NIAO)) {
			GRR._count_niao = 1;
		}

		if (GRR._count_niao > GRR._left_card_count) {
			GRR._count_niao = GRR._left_card_count;
		}
		if (GRR._count_niao == GameConstants_TIAOTIAO.ZHANIAO_0) {
			return;
		}

		// 4红中胡牌不翻鸟牌
		if (GRR._cards_index[seat_index][_logic.get_magic_card_index(0)] != 4
				|| !has_rule(GameConstants_TIAOTIAO.GAME_RULE_HONG_ZHONG_NIAO_ALL)) {

			int cbCardIndexTemp[] = new int[GameConstants_TIAOTIAO.MAX_INDEX];
			_logic.switch_to_cards_index(_repertory_card, _all_card_len - GRR._left_card_count, GRR._count_niao,
					cbCardIndexTemp);
			GRR._left_card_count -= GRR._count_niao;
			_logic.switch_to_cards_data(cbCardIndexTemp, GRR._cards_data_niao);
		}

		if (GRR._cards_index[seat_index][_logic.get_magic_card_index(0)] == 4
				&& has_rule(GameConstants_TIAOTIAO.GAME_RULE_HONG_ZHONG_NIAO_ALL)) {
			if (has_rule(GameConstants_TIAOTIAO.GAME_RULE_YI_MA_QUAN_NIAO)) {
				GRR._count_pick_niao = 10;
			}else {
				GRR._count_pick_niao = get_niao_card_num(false, 0);
			}
		} else if (has_rule(GameConstants_TIAOTIAO.GAME_RULE_YI_MA_QUAN_NIAO)) {
			GRR._count_pick_niao = _logic.get_pick_niao_count_new_hz(GRR._cards_data_niao, GRR._count_niao);
		} else {
			GRR._count_pick_niao = get_pick_niao_count(GRR._cards_data_niao, GRR._count_niao);
		}

		// 无红中
		if (GRR._cards_index[seat_index][_logic.get_magic_card_index(0)] == 0) {
			if (has_rule(GameConstants_TIAOTIAO.GAME_RULE_JIANG_NIAO1)) {
				GRR._count_pick_niao += 1;
			} else if (has_rule(GameConstants_TIAOTIAO.GAME_RULE_JIANG_NIAO2)) {
				GRR._count_pick_niao += 2;
			}
		}

		if (has_rule(GameConstants_TIAOTIAO.GAME_RULE_YI_MA_QUAN_NIAO)) {
			GRR._player_niao_cards[seat_index][GRR._player_niao_count[seat_index]++] = GRR._cards_data_niao[0];
		} else {
			for (int i = 0; i < GRR._count_niao; i++) {
				int nValue = _logic.get_card_value(GRR._cards_data_niao[i]);
				int seat = get_zhong_seat_by_value_three(nValue, seat_index);
				GRR._player_niao_cards[seat][GRR._player_niao_count[seat]] = GRR._cards_data_niao[i];
				GRR._player_niao_count[seat]++;
			}
		}

		int[] player_niao_count = new int[GameConstants_TIAOTIAO.GAME_PLAYER];
		int[][] player_niao_cards = new int[GameConstants_TIAOTIAO.GAME_PLAYER][GameConstants_TIAOTIAO.MAX_NIAO_CARD];
		for (int i = 0; i < GameConstants_TIAOTIAO.GAME_PLAYER; i++) {
			player_niao_count[i] = 0;
			for (int j = 0; j < GameConstants_TIAOTIAO.MAX_NIAO_CARD; j++) {
				player_niao_cards[i][j] = GameConstants_TIAOTIAO.INVALID_VALUE;
			}
		}
		for (int i = 0; i < GameConstants_TIAOTIAO.GAME_PLAYER; i++) {
			for (int j = 0; j < GRR._player_niao_count[i]; j++) {
				if (seat_index == i) {
					player_niao_cards[i][player_niao_count[i]] = this.set_ding_niao_valid(GRR._player_niao_cards[i][j],
							true);//
					// 胡牌的鸟生效
				} else {
					player_niao_cards[i][player_niao_count[i]] = this.set_ding_niao_valid(GRR._player_niao_cards[i][j],
							false);//
					// 胡牌的鸟生效
				}
				player_niao_count[i]++;
			}
		}
		niao_count[seat_index] += GRR._count_pick_niao;
		GRR._player_niao_cards = player_niao_cards;
		GRR._player_niao_count = player_niao_count;
	}

	/**
	 * 抓鸟 1 5 9
	 * 
	 * @param cards_data
	 * @param card_num
	 * @return
	 */
	public int get_pick_niao_count(int cards_data[], int card_num) {
		// MAX_NIAO_CARD
		int cbPickNum = 0;
		for (int i = 0; i < card_num; i++) {
			if (!_logic.is_valid_card(cards_data[i])) {
				return 0;
			}

			int nValue = _logic.get_card_value(cards_data[i]);
			if (nValue == 1 || nValue == 5 || nValue == 9) {
				cbPickNum++;
			}

		}
		return cbPickNum;
	}

	// 玩家出牌的动作检测
	public boolean estimate_player_out_card_respond(int seat_index, int card) {
		// 变量定义
		boolean bAroseAction = false;// 出现(是否)有

		// 用户状态
		for (int i = 0; i < getTablePlayerNumber(); i++) {

			_playerStatus[i].clean_action();
			_playerStatus[i].clean_weave();
		}

		int llcard = get_niao_card_num(true, 0);
		PlayerStatus playerStatus = null;

		int action = GameConstants_TIAOTIAO.WIK_NULL;

		// 动作判断
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			// 用户过滤
			if (seat_index == i)
				continue;

			playerStatus = _playerStatus[i];

			//// 碰牌判断
			action = _logic.check_peng(GRR._cards_index[i], card);
			if (action != 0 && _playerStatus[i].is_chi_peng_round()) {
				playerStatus.add_action(action);
				playerStatus.add_peng(card, seat_index);
				bAroseAction = true;
			}
			if (GRR._left_card_count > llcard) {
				// 杠牌判断
				action = _logic.estimate_gang_card_out_card_hy(GRR._cards_index[i], card);
				if (action != 0) {
					playerStatus.add_action(GameConstants_TIAOTIAO.WIK_GANG);
					playerStatus.add_gang(card, seat_index, 1);// 加上杠
					bAroseAction = true;
				}
			}

			// 红中不能胡
			if (card != _logic.switch_to_card_data(_logic.get_magic_card_index(0))) {
				// 可以胡的情况 判断
				if (_playerStatus[i].is_chi_hu_round()) {
					if (has_rule(GameConstants_TIAOTIAO.GAME_RULE_KE_DIAN_PAO)) {
						// 吃胡判断
						ChiHuRight chr = GRR._chi_hu_rights[i];// playerStatus._chiHuRight;
						chr.set_empty();
						int cbWeaveCount = GRR._weave_count[i];
						action = analyse_chi_hu_card(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr,
								GameConstants_TIAOTIAO.HU_CARD_TYPE_PAOHU, i);

						// 结果判断
						if (action != 0) {
							_playerStatus[i].add_action(GameConstants_TIAOTIAO.WIK_CHI_HU);
							_playerStatus[i].add_chi_hu(card, seat_index);// 吃胡的组合
							bAroseAction = true;
						}
					}
				}
			}
			// }
		}

		if (bAroseAction) {
			_resume_player = _current_player;// 保存当前轮到的玩家
			_current_player = GameConstants_TIAOTIAO.INVALID_SEAT;// 有需要操作的玩家。当前玩家为空
			_provide_player = seat_index;

		} else {

			return false;
		}

		return true;

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.cai.game.mj.AbstractMJTable#process_chi_hu_player_score(int,
	 * int, int, boolean)
	 */
	@Override
	public void process_chi_hu_player_score(int seat_index, int provide_index, int operate_card, boolean zimo) {

		GRR._chi_hu_card[seat_index][0] = operate_card;

		GRR._win_order[seat_index] = 1;
		// 引用权位
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];

		int wFanShu = 2;
		countCardType(chr, seat_index);

		int lChiHuScore = 2 * GameConstants.CELL_SCORE;
		// 统计
		if (zimo) {
			// 自摸
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index) {
					continue;
				}
				GRR._lost_fan_shu[i][seat_index] = wFanShu;

			}
		} else {// 点炮
			GRR._lost_fan_shu[provide_index][seat_index] = wFanShu;//
		}

		/////////////////////////////////////////////// 算分//////////////////////////
		/*
		 * int tmp_niao_count = GRR._count_pick_niao; for (int i = 0; i <
		 * getTablePlayerNumber(); i++) { for (int j = 0; j <
		 * GRR._player_niao_count[i]; j++) { GRR._count_pick_niao =
		 * tmp_niao_count; GRR._player_niao_cards[i][j] =
		 * this.set_ding_niao_valid(GRR._player_niao_cards[i][j], true); } }
		 */

		////////////////////////////////////////////////////// 自摸 算分
		if (zimo) {
			hu_type[seat_index] = GameConstants.ZI_MO;

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index) {
					continue;
				}

				int s = lChiHuScore;
				hu_score[i] -= s;
				hu_score[seat_index] += s;

				niao_score[i] -= GRR._count_pick_niao;
				niao_score[seat_index] += GRR._count_pick_niao;
				s += GRR._count_pick_niao;

				// WalkerGeek 湖南红中添加飘分选项
				if (!has_rule(GameConstants_TIAOTIAO.GAME_RULE_BU_PIAO)) {
					int piao = (_player_result.pao[i] + _player_result.pao[seat_index]);
					if (has_rule(GameConstants_TIAOTIAO.GAME_RULE_BU_DUI_PIAO)) {
						piao = (_player_result.pao[seat_index] > _player_result.pao[i] ? _player_result.pao[seat_index]
								: _player_result.pao[i]);
					}
					s += piao;
					piao_score[i] -= piao;
					piao_score[seat_index] += piao;
				}

				// 胡牌分
				GRR._game_score[i] -= s;
				GRR._game_score[seat_index] += s;
			}
		}
		////////////////////////////////////////////////////// 点炮 算分
		else {
			int s = lChiHuScore;
			hu_score[provide_index] -= s;
			hu_score[seat_index] += s;

			// 这个玩家全包
			int niao = GRR._count_pick_niao;
			niao_score[provide_index] -= niao;
			niao_score[seat_index] += niao;
			s += niao;

			// WalkerGeek 湖南红中添加飘分选项
			if (!has_rule(GameConstants_TIAOTIAO.GAME_RULE_BU_PIAO)) {
				int piao = (_player_result.pao[provide_index] + _player_result.pao[seat_index]);
				if (has_rule(GameConstants_TIAOTIAO.GAME_RULE_BU_DUI_PIAO)) {
					piao = (_player_result.pao[seat_index] > _player_result.pao[provide_index]
							? _player_result.pao[seat_index] : _player_result.pao[provide_index]);
				}
				s += piao;
				piao_score[provide_index] -= piao;
				piao_score[seat_index] += piao;
			}

			GRR._game_score[provide_index] -= s;

			GRR._game_score[seat_index] += s;

			// 点炮的时候，删掉这张牌显示
			GRR._chi_hu_rights[provide_index].opr_or(GameConstants.CHR_FANG_PAO);

		}

		GRR._provider[seat_index] = provide_index;

		// 设置变量
		_status_gang = false;
		_status_gang_hou_pao = false;

		change_player_status(seat_index, GameConstants.INVALID_VALUE);
		return;
	}

	@Override
	protected void set_result_describe() {
		int l;
		long type = 0;
		for (int i = 0; i < GameConstants_TIAOTIAO.GAME_PLAYER; i++) {
			String des = "";

			l = GRR._chi_hu_rights[i].type_count;
			boolean is_ping_hu = true;
			for (int j = 0; j < l; j++) {
				type = GRR._chi_hu_rights[i].type_list[j];
				if (GRR._chi_hu_rights[i].is_valid()) {
					if (type == GameConstants_TIAOTIAO.CHR_TONG_PAO) {
						des += " 通炮";
					}
					if (type == GameConstants_TIAOTIAO.CHR_ZI_MO) {
						if (!(GRR._chi_hu_rights[i].opr_and(GameConstants_TIAOTIAO.CHR_HUNAN_HZ_QISHOU_HU)
								.is_empty())) {
							des += " 四红中全鸟";
						} else {
							des += " 自摸";
						}

						if (GRR._count_pick_niao > 0) {
							des += " 中鸟X" + GRR._count_pick_niao;
						}
					}
					if (type == GameConstants_TIAOTIAO.CHR_SHU_FAN) {
						des += " 接炮";
						if (GRR._count_pick_niao > 0) {
							des += " 中鸟X" + GRR._count_pick_niao;
						}
					}
					if (type == GameConstants_TIAOTIAO.CHR_HUNAN_HZ_QISHOU_HU) {
						des += " 四红中";
						is_ping_hu = false;
					}
					if (type == GameConstants_TIAOTIAO.CHR_HUNAN_QIANG_GANG_HU) {
						des += " 抢杠胡";
						is_ping_hu = false;
					}
					if (type == GameConstants_TIAOTIAO.CHR_HUNAN_QI_XIAO_DUI) {
						des += " 七小对";
						is_ping_hu = false;
					}
					if (type == GameConstants_TIAOTIAO.CHR_HUNAN_PENGPENG_HU) {
						des += " 碰碰胡";
						is_ping_hu = false;
					}

					
				} else {
					if (type == GameConstants_TIAOTIAO.CHR_FANG_PAO) {
						des += " 放炮";
					}
				}
			}
			
			if (is_ping_hu) {
				des += " 平胡";
			}
			int jie_gang = 0, fang_gang = 0, ming_gang = 0, an_gang = 0;
			if (GRR != null) {
				for (int p = 0; p < GameConstants_TIAOTIAO.GAME_PLAYER; p++) {
					for (int w = 0; w < GRR._weave_count[p]; w++) {
						if (GRR._weave_items[p][w].weave_kind != GameConstants_TIAOTIAO.WIK_GANG) {
							continue;
						}
						if (p == i) {// 自己
							// 接杠
							if (GRR._weave_items[p][w].provide_player != p) {
								jie_gang++;
							} else {
								if (GRR._weave_items[p][w].public_card == 1) {// 明杠
									ming_gang++;
								} else {
									an_gang++;
								}
							}
						} else {
							// 放杠
							if (GRR._weave_items[p][w].provide_player == i) {
								fang_gang++;
							}
						}
					}
				}
			}

			if (an_gang > 0) {
				des += " 暗杠X" + an_gang;
			}
			if (ming_gang > 0) {
				des += " 明杠X" + ming_gang;
			}
			if (fang_gang > 0) {
				des += " 放杠X" + fang_gang;
			}
			if (jie_gang > 0) {
				des += " 接杠X" + jie_gang;
			}

			GRR._result_des[i] = des;
		}

	}

	/**
	 * 湖南红中麻将的游戏结束
	 * 
	 * @param seat_index
	 * @param reason
	 */
	protected boolean handler_game_finish_hunan_hz(int seat_index, int reason) {

		int real_reason = reason;

		for (int i = 0; i < GameConstants_TIAOTIAO.GAME_PLAYER; i++) {
			_player_ready[i] = 0;
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_GAME_END);
		GameEndResponse.Builder game_end = GameEndResponse.newBuilder();
		MJ_GAME_END_INFO_EXT.Builder gameEndExtBuilder = MJ_GAME_END_INFO_EXT.newBuilder();

		roomResponse.setLeftCardCount(0);

		this.load_common_status(roomResponse);
		this.load_room_info_data(roomResponse);

		// 这里记录了两次，先这样
		RoomInfo.Builder room_info = getRoomInfo();
		game_end.setRoomInfo(room_info);
		game_end.setRunPlayerId(_run_player_id);
		game_end.setRoundOverType(0);
		game_end.setGamePlayerNumber(getTablePlayerNumber());
		game_end.setEndTime(System.currentTimeMillis() / 1000L);// 结束时间
		if (is_mj_type(GameConstants_TIAOTIAO.GAME_TYPE_HALL_HONG_ZHONG_MJ)
				|| is_mj_type(GameConstants_TIAOTIAO.GAME_TYPE_HONG_ZHONG_MJ_TH)) {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				game_end.addPao(_player_result.pao[i]);
			}
		}
		if (GRR != null) {
			game_end.setRoundOverType(1);
			game_end.setStartTime(GRR._start_time);

			game_end.setGameTypeIndex(GRR._game_type_index);
			roomResponse.setLeftCardCount(GRR._left_card_count);

			// 特别显示的牌
			for (int i = 0; i < GRR._especial_card_count; i++) {
				game_end.addEspecialShowCards(GRR._especial_show_cards[i]);

			}

			GRR._end_type = reason;

			int ming_gang_score[] = new int[getTablePlayerNumber()];
			int an_gang_score[] = new int[getTablePlayerNumber()];
			// 杠牌，每个人的分数
			float lGangScore[] = new float[getTablePlayerNumber()];
			for (int i = 0; i < getTablePlayerNumber(); i++) {

				for (int j = 0; j < GRR._gang_score[i].gang_count; j++) {
					for (int k = 0; k < getTablePlayerNumber(); k++) {

						lGangScore[k] += GRR._gang_score[i].scores[j][k];// 杠牌，每个人的分数
					}
				}

				for (int j = 0; j < GRR._weave_count[i]; j++) {
					if (GRR._weave_items[i][j].weave_kind == GameConstants_TIAOTIAO.WIK_GANG) {
						int type = GRR._weave_items[i][j].type;
						if (type == GameConstants_TIAOTIAO.GANG_TYPE_JIE_GANG) {
							int score = GRR._weave_items[i][j].weave_score;

							ming_gang_score[GRR._weave_items[i][j].provide_player] -= score;
							ming_gang_score[i] += score;
						} else {
							for (int k = 0; k < getTablePlayerNumber(); k++) {
								int score = GRR._weave_items[i][j].weave_score;
								if (k == i) {
									continue;
								}
								if (type == GameConstants_TIAOTIAO.GANG_TYPE_ADD_GANG) {
									ming_gang_score[k] -= score;
									ming_gang_score[i] += score;
								} else {
									an_gang_score[k] -= score;
									an_gang_score[i] += score;
								}
							}
						}
					}
				}

				// 记录
				for (int j = 0; j < getTablePlayerNumber(); j++) {
					_player_result.lost_fan_shu[i][j] += GRR._lost_fan_shu[i][j];
				}
			}

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				gameEndExtBuilder.addPiao(piao_score[i]);
				gameEndExtBuilder.addHuScore(hu_score[i]);
				gameEndExtBuilder.addHuType(hu_type[i]);
				gameEndExtBuilder.addNiaoScore(niao_score[i]);
				gameEndExtBuilder.addMingGangScore(ming_gang_score[i]);
				gameEndExtBuilder.addAnGangScore(an_gang_score[i]);
			}

			// WalkerGeek 红中比赛场杠分等比变更
			if (is_mj_type(GameConstants_TIAOTIAO.GAME_TYPE_HALL_HONG_ZHONG_MJ)
					|| is_mj_type(GameConstants_TIAOTIAO.GAME_TYPE_HONG_ZHONG_MJ_TH)) {
				for (int k = 0; k < getTablePlayerNumber(); k++) {
					lGangScore[k] *= getSettleBase(k);// 杠牌分*比赛场倍数
				}
			}

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				GRR._game_score[i] += lGangScore[i];
				GRR._game_score[i] += GRR._start_hu_score[i]; // 起手胡分数

				// 记录
				_player_result.game_score[i] += GRR._game_score[i];

			}

			this.load_player_info_data(roomResponse);

			game_end.setGameRound(_game_round);
			game_end.setCurRound(_cur_round);

			game_end.setCellScore(GameConstants_TIAOTIAO.CELL_SCORE);

			game_end.setBankerPlayer(GRR._banker_player);// 专家
			game_end.setLeftCardCount(GRR._left_card_count);// 剩余牌
			game_end.setShowBirdEffect(GRR._show_bird_effect == false ? 0 : 1);

			// 设置中鸟数据
			for (int i = 0; i < GameConstants_TIAOTIAO.MAX_NIAO_CARD && i < GRR._count_niao; i++) {
				game_end.addCardsDataNiao(GRR._cards_data_niao[i]);
			}

			// 鸟的数据 这里要注意三人场的特殊处理 三人场必须发四个人的鸟 不然显示不全
			for (int i = 0; i < GameConstants_TIAOTIAO.GAME_PLAYER; i++) {
				Int32ArrayResponse.Builder pnc = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GRR._player_niao_count[i]; j++) {
					pnc.addItem(GRR._player_niao_cards[i][j]);
				}
				game_end.addPlayerNiaoCards(pnc);
			}

			game_end.setCountPickNiao(GRR._count_pick_niao);// 中鸟个数

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				Int32ArrayResponse.Builder pnc = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GRR._player_niao_count[i]; j++) {
					pnc.addItem(GRR._player_niao_cards[i][j]);
				}
				game_end.addPlayerNiaoCards(pnc);
				game_end.addHuResult(GRR._hu_result[i]);

				// 胡的牌
				Int32ArrayResponse.Builder hc = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GameConstants_TIAOTIAO.MAX_COUNT; j++) {
					hc.addItem(GRR._chi_hu_card[i][j]);
				}

				game_end.addHuCardData(GRR._chi_hu_card[i][0]);
				game_end.addHuCardArray(hc);
			}

			// 现在权值只有一位
			long rv[] = new long[GameConstants_TIAOTIAO.MAX_RIGHT_COUNT];

			// 设置胡牌描述
			this.set_result_describe();

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				GRR._card_count[i] = _logic.switch_to_cards_data(GRR._cards_index[i], GRR._cards_data[i]);

				Int32ArrayResponse.Builder cs = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GRR._card_count[i]; j++) {

					cs.addItem(GRR._cards_data[i][j]);
				}
				game_end.addCardsData(cs);// 牌

				// 组合
				WeaveItemResponseArrayResponse.Builder weaveItem_array = WeaveItemResponseArrayResponse.newBuilder();
				for (int j = 0; j < GameConstants_TIAOTIAO.MAX_WEAVE; j++) {
					WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
					weaveItem_item.setCenterCard(GRR._weave_items[i][j].center_card);
					weaveItem_item.setProvidePlayer(GRR._weave_items[i][j].provide_player);
					weaveItem_item.setPublicCard(GRR._weave_items[i][j].public_card);
					weaveItem_item.setWeaveKind(GRR._weave_items[i][j].weave_kind);
					weaveItem_array.addWeaveItem(weaveItem_item);
				}
				game_end.addWeaveItemArray(weaveItem_array);

				GRR._chi_hu_rights[i].get_right_data(rv);// 获取权位数值
				game_end.addChiHuRight(rv[0]);

				GRR._start_hu_right[i].get_right_data(rv);// 获取权位数值
				game_end.addStartHuRight(rv[0]);

				game_end.addProvidePlayer(GRR._provider[i]);
				game_end.addGameScore(GRR._game_score[i]);// 放炮的人？
				game_end.addGangScore(lGangScore[i]);// 杠牌得分
				game_end.addStartHuScore(GRR._start_hu_score[i]);
				game_end.addResultDes(GRR._result_des[i]);

				// 胡牌
				game_end.addWinOrder(GRR._win_order[i]);

				Int32ArrayResponse.Builder lfs = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < getTablePlayerNumber(); j++) {
					lfs.addItem(GRR._lost_fan_shu[i][j]);
				}

				game_end.addLostFanShu(lfs);

			}

		}

		boolean end = false;
		if (reason == GameConstants_TIAOTIAO.Game_End_NORMAL || reason == GameConstants_TIAOTIAO.Game_End_DRAW) {
			if (_cur_round >= _game_round && (!is_sys())) {// 局数到了
				end = true;
				game_end.setRoomOverType(1);
				game_end.setPlayerResult(this.process_player_result(reason));
			} else {
				// 确定下局庄家
				// 以后谁胡牌，下局谁做庄。
				// 流局,庄家下一个
				// 通炮,放炮的玩家

			}
		} else if ((!is_sys()) && (reason == GameConstants_TIAOTIAO.Game_End_RELEASE_PLAY
				|| reason == GameConstants_TIAOTIAO.Game_End_RELEASE_NO_BEGIN
				|| reason == GameConstants_TIAOTIAO.Game_End_RELEASE_RESULT
				|| reason == GameConstants_TIAOTIAO.Game_End_RELEASE_PLAY_TIME_OUT
				|| reason == GameConstants_TIAOTIAO.Game_End_RELEASE_WAIT_TIME_OUT
				|| reason == GameConstants_TIAOTIAO.Game_End_RELEASE_SYSTEM)) {
			end = true;
			real_reason = GameConstants_TIAOTIAO.Game_End_DRAW;// 刘局
			game_end.setRoomOverType(1);
			game_end.setPlayerResult(this.process_player_result(reason));
		}
		game_end.setEndType(real_reason);
		game_end.setCommResponse(PBUtil.toByteString(gameEndExtBuilder));
		roomResponse.setGameEnd(game_end);

		roomResponse.setCommResponse(PBUtil.toByteString(gameEndExtBuilder));

		this.send_response_to_room(roomResponse);

		record_game_round(game_end);

		// 超时解散
		if (reason == GameConstants_TIAOTIAO.Game_End_RELEASE_PLAY_TIME_OUT
				|| reason == GameConstants_TIAOTIAO.Game_End_RELEASE_WAIT_TIME_OUT) {
			for (int j = 0; j < GameConstants_TIAOTIAO.GAME_PLAYER; j++) {
				Player player = this.get_players()[j];
				if (player == null)
					continue;
				send_error_notify(j, 1, "游戏解散成功!");

			}
		}

		if (end && (!is_sys()))// 删除
		{
			PlayerServiceImpl.getInstance().delRoomId(this.getRoom_id());
		}

		if (!is_sys()) {
			GRR = null;
		}

		if (is_sys()) {
			clear_score_in_gold_room();
		}
		// 错误断言
		return false;
	}

	@Override
	public boolean handler_requst_pao_qiang(Player player, int pao, int qiang) {
		return _handler_piao.handler_pao_qiang(this, player.get_seat_index(), pao, qiang);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.cai.common.domain.Room#trustee_timer(int, int)
	 */
	@Override
	public boolean trustee_timer(int operate_id, int seat_index) {
		// WalkerGeek Auto-generated method stub
		return false;
	}

	/**
	 * 处理牌桌结束逻辑
	 * 
	 * @param seat_index
	 * @param reason
	 */
	protected boolean on_handler_game_finish(int seat_index, int reason) {
		int real_reason = reason;

		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			_player_ready[i] = 0;
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(1013); // 跳跳麻将结算
		GameEndResponse.Builder game_end = GameEndResponse.newBuilder();
		GameEndResponse_GZCG.Builder gameEndGzcg = GameEndResponse_GZCG.newBuilder();
		// MJ_GAME_END_INFO_EXT.Builder builder =
		// MJ_GAME_END_INFO_EXT.newBuilder();
		// 查牌数据
		this.setGameEndBasicPrama(game_end);

		roomResponse.setLeftCardCount(0);

		this.load_common_status(roomResponse);
		this.load_room_info_data(roomResponse);

		// 这里记录了两次，先这样
		RoomInfo.Builder room_info = getRoomInfo();
		game_end.setRoomInfo(room_info);
		game_end.setRunPlayerId(_run_player_id);
		game_end.setRoundOverType(0);
		game_end.setGamePlayerNumber(getTablePlayerNumber());
		game_end.setEndTime(System.currentTimeMillis() / 1000L);// 结束时间
		if (GRR != null) {
			game_end.setRoundOverType(1);
			game_end.setStartTime(GRR._start_time);

			game_end.setGameTypeIndex(GRR._game_type_index);
			roomResponse.setLeftCardCount(GRR._left_card_count);

			for (int i = 0; i < GRR._especial_card_count; i++) {
				game_end.addEspecialShowCards(GRR._especial_show_cards[i]);
			}

			GRR._end_type = reason;

			int hupaiscore[] = new int[getTablePlayerNumber()];// 胡牌分
			int totalScore[] = new int[getTablePlayerNumber()];// 总分=胡牌分+杠分+跟庄
			int genZhuangScore[] = new int[getTablePlayerNumber()];// 跟庄分
			int mingGangScore[] = new int[getTablePlayerNumber()]; // 明杆得分
			int zhiGangScore[] = new int[getTablePlayerNumber()]; // 直杆得分
			int anGangScore[] = new int[getTablePlayerNumber()]; // 暗杠得分
			int mingGangCard[][] = new int[getTablePlayerNumber()][4]; // 明杠牌
			int zhiGangCard[][] = new int[getTablePlayerNumber()][4]; // 直杠牌
			int anGangCard[][] = new int[getTablePlayerNumber()][4]; // 暗杆牌

			// 杠牌，每个人的分数
			float lGangScore[] = new float[getTablePlayerNumber()];

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				for (int j = 0; j < GRR._gang_score[i].gang_count; j++) {
					for (int k = 0; k < getTablePlayerNumber(); k++) {
						lGangScore[k] += GRR._gang_score[i].scores[j][k];// 杠牌，每个人的分数
					}
				}
				if(reason != GameConstants.Game_End_DRAW){
					for (int j = 0; j < GRR._weave_count[i]; j++) {
						if (GRR._weave_items[i][j].weave_kind == GameConstants.WIK_GANG) {
							if (GRR._weave_items[i][j].type == GameConstants.GANG_TYPE_JIE_GANG) {
								int score = GRR._weave_items[i][j].weave_score;
								
								zhiGangScore[GRR._weave_items[i][j].provide_player] -= score;
								zhiGangScore[i] += score;
								zhiGangCard[i][j] = GRR._weave_items[i][j].center_card;
							} else {
								for (int k = 0; k < getTablePlayerNumber(); k++) {
									int score = GRR._weave_items[i][j].weave_score;
									if (k == i) {
										continue;
									}
									if (GRR._weave_items[i][j].type == GameConstants.GANG_TYPE_AN_GANG) {
										anGangScore[k] -= score;
										anGangScore[i] += score;
										anGangCard[i][j] = GRR._weave_items[i][j].center_card;
									} else {
										mingGangScore[k] -= score;
										mingGangScore[i] += score;
										mingGangCard[i][j] = GRR._weave_items[i][j].center_card;
									}
								}
							}
						}
					}
				}

				// 记录
				for (int j = 0; j < getTablePlayerNumber(); j++) {
					_player_result.lost_fan_shu[i][j] += GRR._lost_fan_shu[i][j];
				}

			}

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				// hupaiscore[i] = (int) GRR._game_score[i] - maPaiScore[i];
				
				GRR._game_score[i] += GRR._start_hu_score[i]; // 起手胡分数

				totalScore[i] = (int) GRR._game_score[i];
				if(reason != GameConstants.Game_End_DRAW){
					totalScore[i] += lGangScore[i];
				}
				// 记录
				_player_result.game_score[i] += GRR._game_score[i];
				//流局退还杠分
				if(reason == GameConstants.Game_End_DRAW){
					_player_result.game_score[i] -= lGangScore[i];
				}

			}

			// 结算统计
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				gameEndGzcg.addHuPaiScore(hupaiscore[i]); // 胡牌的牌型分： 客户端可能不需要
				gameEndGzcg.addMingGang(mingGangScore[i]);// 弯杆
				gameEndGzcg.addAnGang(anGangScore[i]); // 暗杠
				gameEndGzcg.addChaoZhuang(genZhuangScore[i]);
				gameEndGzcg.addTotalScore(totalScore[i]); // 小局总分
				gameEndGzcg.addJingScore(zhiGangScore[i]); // 直杠
				gameEndGzcg.addGangJing(piao_score[i]);// 飘分
				if (niaoPlayer == i) {
					gameEndGzcg.addJiangLiScore(GRR._count_pick_niao); // 中鸟个数
				} else {
					gameEndGzcg.addJiangLiScore(0); // 中鸟个数
				}

			}

			this.load_player_info_data(roomResponse);

			game_end.setGameRound(_game_round);
			game_end.setCurRound(_cur_round);

			game_end.setCellScore(GameConstants.CELL_SCORE);

			game_end.setBankerPlayer(GRR._banker_player);// 专家
			game_end.setLeftCardCount(GRR._left_card_count);// 剩余牌
			game_end.setShowBirdEffect(GRR._show_bird_effect == false ? 0 : 1);

			// 设置中鸟数据
			for (int i = 0; i < GameConstants.MAX_NIAO_CARD && i < GRR._count_niao; i++) {
				game_end.addCardsDataNiao(GRR._cards_data_niao[i]);
			}
			// 设置中鸟数据
			for (int i = 0; i < GameConstants.MAX_NIAO_CARD && i < GRR._count_niao_fei; i++) {
				game_end.addCardsDataNiao(GRR._cards_data_niao_fei[i]);
			}
			game_end.setCountPickNiao(GRR._count_pick_niao + GRR._count_pick_niao_fei);// 中鸟个数

			for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
				Int32ArrayResponse.Builder pnc = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GRR._player_niao_count[i]; j++) {
					pnc.addItem(GRR._player_niao_cards[i][j]);
				}
				for (int j = 0; j < GRR._player_niao_count_fei[i]; j++) {
					pnc.addItem(GRR._player_niao_cards_fei[i][j]);
				}
				game_end.addPlayerNiaoCards(pnc);
			}

			for (int i = 0; i < getTablePlayerNumber(); i++) {

				game_end.addHuResult(GRR._hu_result[i]);

				// 胡的牌
				Int32ArrayResponse.Builder hc = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GameConstants.MAX_COUNT; j++) {
					hc.addItem(GRR._chi_hu_card[i][j]);
				}

				game_end.addHuCardData(GRR._chi_hu_card[i][0]);
				game_end.addHuCardArray(hc);
			}

			// 现在权值只有一位
			long rv[] = new long[GameConstants.MAX_RIGHT_COUNT];

			// 设置胡牌描述
			this.set_result_describe();

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				GRR._card_count[i] = _logic.switch_to_cards_data(GRR._cards_index[i], GRR._cards_data[i]);

				Int32ArrayResponse.Builder cs = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GRR._card_count[i]; j++) {

					cs.addItem(GRR._cards_data[i][j]);
				}
				game_end.addCardsData(cs);// 牌

				// 组合
				WeaveItemResponseArrayResponse.Builder weaveItem_array = WeaveItemResponseArrayResponse.newBuilder();
				for (int j = 0; j < GameConstants.MAX_WEAVE; j++) {
					WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
					weaveItem_item.setCenterCard(GRR._weave_items[i][j].center_card);
					weaveItem_item.setProvidePlayer(GRR._weave_items[i][j].provide_player);
					weaveItem_item.setPublicCard(GRR._weave_items[i][j].public_card);
					weaveItem_item.setWeaveKind(GRR._weave_items[i][j].weave_kind);
					weaveItem_array.addWeaveItem(weaveItem_item);
				}
				game_end.addWeaveItemArray(weaveItem_array);

				GRR._chi_hu_rights[i].get_right_data(rv);// 获取权位数值
				game_end.addChiHuRight(rv[0]);

				GRR._start_hu_right[i].get_right_data(rv);// 获取权位数值
				game_end.addStartHuRight(rv[0]);

				game_end.addProvidePlayer(GRR._provider[i]);
				game_end.addGameScore(GRR._game_score[i]);// 放炮的人？
				game_end.addGangScore(lGangScore[i]);// 杠牌得分
				game_end.addStartHuScore(GRR._start_hu_score[i]);
				game_end.addResultDes(GRR._result_des[i]);

				// 胡牌
				game_end.addWinOrder(GRR._win_order[i]);

				Int32ArrayResponse.Builder lfs = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < getTablePlayerNumber(); j++) {
					lfs.addItem(GRR._lost_fan_shu[i][j]);
				}

				game_end.addLostFanShu(lfs);

			}

		}

		boolean end = false;
		if (reason == GameConstants.Game_End_NORMAL || reason == GameConstants.Game_End_DRAW) {
			if (_cur_round >= _game_round && (!is_sys())) {// 局数到了
				end = true;
				game_end.setRoomOverType(1);
				game_end.setPlayerResult(this.process_player_result(reason));
			} else {
				// 确定下局庄家
				// 以后谁胡牌，下局谁做庄。
				// 流局,庄家下一个
				// 通炮,放炮的玩家

			}
		} else if ((!is_sys()) && (reason == GameConstants.Game_End_RELEASE_PLAY
				|| reason == GameConstants.Game_End_RELEASE_NO_BEGIN || reason == GameConstants.Game_End_RELEASE_RESULT
				|| reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_SYSTEM)) {
			end = true;
			real_reason = GameConstants.Game_End_DRAW;// 刘局
			game_end.setRoomOverType(1);
			game_end.setPlayerResult(this.process_player_result(reason));
		}
		game_end.setEndType(real_reason);

		////////////////////////////////////////////////////////////////////// 得分总的
		gameEndGzcg.setGameEnd(game_end);
		roomResponse.setGameEnd(game_end);
		roomResponse.setCommResponse(PBUtil.toByteString(gameEndGzcg));
		this.send_response_to_room(roomResponse);
		game_end.setCommResponse(PBUtil.toByteString(gameEndGzcg));
		record_game_round(game_end);

		// 超时解散
		if (reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT) {
			for (int j = 0; j < GameConstants.GAME_PLAYER; j++) {
				Player player = this.get_players()[j];
				if (player == null)
					continue;
				send_error_notify(j, 1, "游戏解散成功!");

			}
		}

		if (end && (!is_sys()))// 删除
		{
			PlayerServiceImpl.getInstance().delRoomId(this.getRoom_id());
		}

		if (!is_sys()) {
			GRR = null;
		}

		if (is_sys()) {
			clear_score_in_gold_room();
		}
		// 错误断言
		return false;
	}

	/**
	 * 无红中多加鸟
	 * 
	 * @param seat_index
	 * @return
	 */
	public int getMoreZhuaNiao(int seat_index) {
		int add_niao = 0;
		if (GRR._cards_index[seat_index][_logic.get_magic_card_index(0)] == 0) {
			// 手上没有红中
			if (has_rule(GameConstants_TIAOTIAO.GAME_RULE_DUO_ZHUA_NIAO1)) {
				add_niao = 1;
			} else if (has_rule(GameConstants_TIAOTIAO.GAME_RULE_DUO_ZHUA_NIAO2)) {
				add_niao = 2;
			}
		}
		return add_niao;
	}

	public PlayerResultResponse.Builder process_player_result(int result) {

		this.huan_dou(result);

		// 大赢家
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			_player_result.win_order[i] = -1;
		}
		int win_idx = 0;
		float max_score = 0;
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			int winner = -1;
			float s = -999999;
			for (int j = 0; j < GameConstants.GAME_PLAYER; j++) {
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

		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			player_result.addGameScore(_player_result.game_score[i]);
			player_result.addWinOrder(_player_result.win_order[i]);

			Int32ArrayResponse.Builder lfs = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < GameConstants.GAME_PLAYER; j++) {
				lfs.addItem(_player_result.lost_fan_shu[i][j]);
			}

			player_result.addLostFanShu(lfs);

			// if(is_mj_type(MJGameConstants.GAME_TYPE_ZZ) ||
			// is_mj_type(MJGameConstants.GAME_TYPE_HZ)||
			// is_mj_type(MJGameConstants.GAME_TYPE_SHUANGGUI)){
			player_result.addZiMoCount(_player_result.zi_mo_count[i]);
			player_result.addJiePaoCount(_player_result.jie_pao_count[i]);
			player_result.addDianPaoCount(_player_result.dian_pao_count[i]);
			player_result.addAnGangCount(_player_result.an_gang_count[i]);
			player_result.addMingGangCount(_player_result.ming_gang_count[i]);

			player_result.addMenQingCount(_player_result.zhi_gang_count[i]);

			player_result.addHaiDiCount(niao_count[i]);
			// }else if(is_mj_type(MJGameConstants.GAME_TYPE_CS)||
			// is_mj_type(MJGameConstants.GAME_TYPE_ZHUZHOU)){
			player_result.addDaHuZiMo(_player_result.da_hu_zi_mo[i]);
			player_result.addDaHuJiePao(_player_result.da_hu_jie_pao[i]);
			player_result.addDaHuDianPao(_player_result.da_hu_dian_pao[i]);
			player_result.addXiaoHuZiMo(_player_result.xiao_hu_zi_mo[i]);
			player_result.addXiaoHuJiePao(_player_result.xiao_hu_jie_pao[i]);
			player_result.addXiaoHuDianPao(_player_result.xiao_hu_dian_pao[i]);

			player_result.addPiaoLaiCount(_player_result.piao_lai_count[i]);
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

		RoomPlayerResponse.Builder room_player = RoomPlayerResponse.newBuilder();
		room_player.setAccountId(this.getCreate_player().getAccount_id());
		room_player.setHeadImgUrl(this.getCreate_player().getAccount_icon());
		room_player.setIp(this.getCreate_player().getAccount_ip());
		room_player.setUserName(this.getCreate_player().getNick_name());
		room_player.setSeatIndex(this.getCreate_player().get_seat_index());
		room_player.setOnline(this.getCreate_player().isOnline() ? 1 : 0);
		room_player.setIpAddr(this.getCreate_player().getAccount_ip_addr());
		room_player.setSex(this.getCreate_player().getSex());
		room_player.setScore(0);
		room_player.setReady(0);
		room_player.setPao(0);
		room_player.setQiang(0);
		if (this.getCreate_player().locationInfor != null) {
			room_player.setLocationInfor(this.getCreate_player().locationInfor);
		}
		player_result.setCreatePlayer(room_player);
		return player_result;
	}

}
