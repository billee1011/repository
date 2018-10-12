package com.cai.game.mj.handler.yiyang;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import protobuf.clazz.Protocol.GameEndResponse;
import protobuf.clazz.Protocol.GameStartResponse;
import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomInfo;
import protobuf.clazz.Protocol.RoomRequest;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.GameConstants_YiYang;
import com.cai.common.domain.BrandLogModel;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.Player;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.WeaveItem;
import com.cai.common.util.GameDescUtil;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.DispatchLastCardRunnable;
import com.cai.game.mj.AbstractMJTable;
import com.cai.game.mj.MJGameLogic.AnalyseItem;
import com.cai.game.mj.MJType;
import com.cai.game.util.AnalyseCardUtil;
import com.cai.service.PlayerServiceImpl;

public class MJTable_YiYang extends AbstractMJTable {
	private static final long serialVersionUID = -1296484943449208552L;

	BrandLogModel _recordRoomRecord;
	public int[] player_dispatch_count = new int[getTablePlayerNumber()];
	// 玩家抓的牌的数目，用来处理报听
	public int[] zhua_pai_count = new int[getTablePlayerNumber()];

	protected MJHandlerOutCardBaoTing_YiYang _handler_out_card_bao_ting; // 报听
	protected MJHandlerDispatchLastCard_YiYang _handler_dispath_last_card;

	/**
	 * 胡牌时玩家的输赢牌型分
	 */
	public int[] pai_xing_fen = new int[getTablePlayerNumber()];
	public boolean[] yi_zi_qiao = new boolean[getTablePlayerNumber()];

	public MJTable_YiYang() {
		super(MJType.GAME_TYPE_HUNAN_YIYANG);
	}

	@Override
	protected void onInitTable() {
		_handler_dispath_card = new MJHandlerDispatchCard_YiYang();
		_handler_out_card_operate = new MJHandlerOutCardOperate_YiYang();
		_handler_gang = new MJHandlerGang_YiYang();
		_handler_chi_peng = new MJHandlerChiPeng_YiYang();
		_handler_out_card_bao_ting = new MJHandlerOutCardBaoTing_YiYang();
		_handler_dispath_last_card = new MJHandlerDispatchLastCard_YiYang();
	}

	@Override
	protected boolean on_game_start() {
		zhua_pai_count = new int[getTablePlayerNumber()];
		pai_xing_fen = new int[getTablePlayerNumber()];
		player_dispatch_count = new int[getTablePlayerNumber()];
		yi_zi_qiao = new boolean[getTablePlayerNumber()];
		// 游戏开始
		this._game_status = GameConstants.GS_MJ_PLAY;// 设置状态
		GameStartResponse.Builder gameStartResponse = GameStartResponse.newBuilder();
		// gameStartResponse.setSiceIndex(rand);
		gameStartResponse.setBankerPlayer(this.GRR._banker_player);
		gameStartResponse.setCurrentPlayer(this._current_player);
		gameStartResponse.setLeftCardCount(this.GRR._left_card_count);

		int hand_cards[][] = new int[getTablePlayerNumber()][GameConstants.MAX_COUNT];
		// 发送数据
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			int hand_card_count = this._logic.switch_to_cards_data(this.GRR._cards_index[i], hand_cards[i]);
			gameStartResponse.addCardsCount(hand_card_count);
		}

		// 发送数据
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

			// 只发自己的牌
			gameStartResponse.clearCardData();
			for (int j = 0; j < GameConstants.MAX_COUNT; j++) {
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
			this.send_response_to_player(i, roomResponse);
		}
		////////////////////////////////////////////////// 回放
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
		this.load_room_info_data(roomResponse);
		this.load_common_status(roomResponse);
		this.load_player_info_data(roomResponse);
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

			for (int j = 0; j < GameConstants.MAX_COUNT; j++) {
				cards.addItem(hand_cards[i][j]);
			}
			gameStartResponse.addCardsData(cards);
		}

		roomResponse.setGameStart(gameStartResponse);
		roomResponse.setLeftCardCount(this.GRR._left_card_count);
		this.GRR.add_room_response(roomResponse);

		// 检测听牌
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_playerStatus[i]._hu_card_count = this.get_ting_card(_playerStatus[i]._hu_cards, GRR._cards_index[i], GRR._weave_items[i],
					GRR._weave_count[i], i);
			if (_playerStatus[i]._hu_card_count > 0) {
				this.operate_chi_hu_cards(i, _playerStatus[i]._hu_card_count, _playerStatus[i]._hu_cards);
			}
		}

		this.exe_dispatch_card(_current_player, GameConstants.DispatchCard_Type_Tian_Hu, 0);

		return true;
	}

	@Override
	public int analyse_chi_hu_card(int[] cards_index, WeaveItem[] weaveItems, int weaveCount, int cur_card, ChiHuRight chiHuRight, int card_type,
			int _seat_index) {

		boolean isWin = false;
		// 构造扑克
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		// 插入扑克
		if (cur_card != GameConstants.INVALID_VALUE) {
			cbCardIndexTemp[_logic.switch_to_card_index(cur_card)]++;
		}
		
		if(is_mj_type(GameConstants.GAME_TYPE_MJ_YI_YANG_QIN_YOU)){
			// 一条龙
			if (has_rule(GameConstants_YiYang.GAME_YI_TIAO_LONG) && is_yi_tiao_long(cbCardIndexTemp)) {
				chiHuRight.opr_or(GameConstants_YiYang.CHR_YI_TIAO_LONG);
				chiHuRight.da_hu_count++;
				isWin = true;
			}
		}else{
			if (GameDescUtil.has_rule(gameRuleIndexEx, GameConstants_YiYang.GAME_RULE_HUNAN_M_Q_J_J_HU)) {
				if (_logic.is_men_qing(weaveItems, weaveCount) == GameConstants.CHR_HUNAN_MEN_QING
						&& _logic.is_jiangjiang_hu(cards_index, weaveItems, weaveCount, cur_card)) {
					chiHuRight.opr_or(GameConstants.CHR_HUNAN_MEN_QING);
					chiHuRight.opr_or(GameConstants.CHR_HUNAN_JIANGJIANG_HU);

					isWin = true;
				}
			}
		}

		

		// 清一色
		if (_logic.is_qing_yi_se(cbCardIndexTemp, weaveItems, weaveCount, cur_card)) {
			chiHuRight.opr_or(GameConstants.CHR_HUNAN_QING_YI_SE);
		}

		// 七对
		int qiXiaoDui = is_qi_xiao_dui(cards_index, weaveItems, weaveCount, cur_card);
		if (qiXiaoDui != GameConstants.WIK_NULL) {
			chiHuRight.opr_or(qiXiaoDui);
			isWin = true;
		}

		// 全求人
		if (_logic.is_dan_diao(cards_index, cur_card)) {
			chiHuRight.opr_or(GameConstants.CHR_HUNAN_QUAN_QIU_REN);
			isWin = true;
		}

		// 将将胡
		if (_logic.is_jiangjiang_hu(cards_index, weaveItems, weaveCount, cur_card)) {
			chiHuRight.opr_or(GameConstants.CHR_HUNAN_JIANGJIANG_HU);
			isWin = true;
		}

		// 杠上开花
		if (card_type == GameConstants.HU_CARD_TYPE_GANG_KAI) {
			chiHuRight.opr_or(GameConstants.CHR_HUNAN_GANG_KAI);
		}

		if(is_mj_type(GameConstants.GAME_TYPE_MJ_YI_YANG_QIN_YOU)){
			if (card_type != GameConstants.HU_CARD_TYPE_QIANGGANG && GameDescUtil.has_rule(gameRuleIndexEx, GameConstants_YiYang.GAME_RULE_HUNAN_MEN_QING)) {
				// 门清
				if (_logic.is_men_qing(weaveItems, weaveCount) == GameConstants.CHR_HUNAN_MEN_QING) {
					chiHuRight.opr_or(GameConstants.CHR_HUNAN_MEN_QING);
				}
			}
		}else{
			if (GameDescUtil.has_rule(gameRuleIndexEx, GameConstants_YiYang.GAME_RULE_HUNAN_MEN_QING)
					|| GameDescUtil.has_rule(gameRuleIndexEx, GameConstants.GAME_RULE_HUNAN_M_Q_J_J_HU)) {
				// 门清
				if (_logic.is_men_qing(weaveItems, weaveCount) == GameConstants.CHR_HUNAN_MEN_QING) {
					chiHuRight.opr_or(GameConstants.CHR_HUNAN_MEN_QING);
				}
			}
		}

		List<AnalyseItem> analyseItemArray = new ArrayList<AnalyseItem>();
		boolean bValue = _logic.analyse_card(cbCardIndexTemp, weaveItems, weaveCount, analyseItemArray, true);
		if (!bValue) {
			if (isWin) {
				return GameConstants.WIK_CHI_HU;
			}
			chiHuRight.set_empty();
			return GameConstants.WIK_NULL;
		}
		// 胡牌分析
		if (analyseItemArray.size() > 0) {
			// 牌型分析
			for (int i = 0; i < analyseItemArray.size(); i++) {
				// 变量定义
				AnalyseItem analyseItem = analyseItemArray.get(i);
				// 碰碰和
				if (_logic.is_pengpeng_hu(analyseItem))
					chiHuRight.opr_or(GameConstants.CHR_HUNAN_PENGPENG_HU);
			}
		}
		return GameConstants.WIK_CHI_HU;
	}
	
	/**
	 * ③一条龙
	 */
	public boolean is_yi_tiao_long(int cards_index[]) {
		Set<Integer> set = new HashSet<Integer>();
		for (int i = 0; i < GameConstants.MAX_ZI; i++) {
			if(cards_index[i] == 0){
				set.add(_logic.get_card_color(_logic.switch_to_card_data(i)));
			}
 		}
		if(set.size() != 3){
			int[] copyOf = Arrays.copyOf(cards_index, cards_index.length);
			if(!set.contains(0)){
				for(int i = 0; i < 9; i++)
					copyOf[i]--;
			}else if(!set.contains(1)){
				for(int i = 9; i < 18; i++)
					copyOf[i]--;
			}else if(!set.contains(2)){
				for(int i = 18; i < 27; i++)
					copyOf[i]--;
			}
			boolean hu;
				hu = AnalyseCardUtil.analyse_win_by_cards_index(copyOf, -1, null, 0);
			if(hu)
				return hu;
		}
		return false;
	}

	public int analyse_chi_hu_card_chi_hu(int[] cards_index, WeaveItem[] weaveItems, int weaveCount, int cur_card, ChiHuRight chiHuRight,
			int card_type, int _seat_index) {

		boolean isWin = false;
		// 构造扑克
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		// 插入扑克
		if (cur_card != GameConstants.INVALID_VALUE) {
			cbCardIndexTemp[_logic.switch_to_card_index(cur_card)]++;
		}
		
		if(is_mj_type(GameConstants.GAME_TYPE_MJ_YI_YANG_QIN_YOU)){
			if (_logic.is_men_qing(weaveItems, weaveCount) == GameConstants.CHR_HUNAN_MEN_QING
					&& _logic.is_jiangjiang_hu(cards_index, weaveItems, weaveCount, cur_card)) {
				chiHuRight.opr_or(GameConstants.CHR_HUNAN_MEN_QING);
				chiHuRight.opr_or(GameConstants.CHR_HUNAN_JIANGJIANG_HU);
				
				isWin = true;
			}
			// 一条龙
			if (has_rule(GameConstants_YiYang.GAME_YI_TIAO_LONG) && is_yi_tiao_long(cbCardIndexTemp)) {
				chiHuRight.opr_or(GameConstants_YiYang.CHR_YI_TIAO_LONG);
				chiHuRight.da_hu_count++;
				isWin = true;
			}
		}else{
			if (GameDescUtil.has_rule(gameRuleIndexEx, GameConstants_YiYang.GAME_RULE_HUNAN_M_Q_J_J_HU)) {
				if (_logic.is_men_qing(weaveItems, weaveCount) == GameConstants.CHR_HUNAN_MEN_QING
						&& _logic.is_jiangjiang_hu(cards_index, weaveItems, weaveCount, cur_card)) {
					chiHuRight.opr_or(GameConstants.CHR_HUNAN_MEN_QING);
					chiHuRight.opr_or(GameConstants.CHR_HUNAN_JIANGJIANG_HU);
					
					isWin = true;
				}
			}
		}


		// 清一色
		if (_logic.is_qing_yi_se(cbCardIndexTemp, weaveItems, weaveCount, cur_card)) {
			chiHuRight.opr_or(GameConstants.CHR_HUNAN_QING_YI_SE);
		}

		// 七对
		int qiXiaoDui = is_qi_xiao_dui(cards_index, weaveItems, weaveCount, cur_card);
		if (qiXiaoDui != GameConstants.WIK_NULL) {
			chiHuRight.opr_or(qiXiaoDui);
			isWin = true;
		}

		// 全求人
		if (_logic.is_dan_diao(cards_index, cur_card)) {
			chiHuRight.opr_or(GameConstants.CHR_HUNAN_QUAN_QIU_REN);
			isWin = true;
		}

		List<AnalyseItem> analyseItemArray = new ArrayList<AnalyseItem>();
		// 分析扑克
		boolean bValue = _logic.analyse_card(cbCardIndexTemp, weaveItems, weaveCount, analyseItemArray, false);
		if (!bValue) {
			if (isWin) {
				return GameConstants.WIK_CHI_HU;
			}
			chiHuRight.set_empty();
			return GameConstants.WIK_NULL;
		}
		// 胡牌分析
		if (analyseItemArray.size() > 0) {
			// 牌型分析
			for (int i = 0; i < analyseItemArray.size(); i++) {
				// 变量定义
				AnalyseItem analyseItem = analyseItemArray.get(i);
				// 碰碰和
				if (_logic.is_pengpeng_hu(analyseItem))
					chiHuRight.opr_or(GameConstants.CHR_HUNAN_PENGPENG_HU);
			}
		}
		if(is_mj_type(GameConstants.GAME_TYPE_MJ_YI_YANG_QIN_YOU)){
			if (chiHuRight.is_empty() && !_playerStatus[_seat_index].is_bao_ting()) {
				return GameConstants.WIK_NULL;
			}
		}else{
			if (chiHuRight.is_empty()) {
				return GameConstants.WIK_NULL;
			}
		}
		return GameConstants.WIK_CHI_HU;
	}

	public int get_ting_card(int[] cards, int cards_index[], WeaveItem weaveItems[], int cbWeaveCount, int seatIndex) {
		// 复制数据
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		ChiHuRight chr = new ChiHuRight();
		int cbCurrentCard;
		int count = 0;
		// 遍历所有的牌去判断能不能胡牌
		for (int i = 0; i < GameConstants.MAX_INDEX - GameConstants.CARD_FENG_COUNT - GameConstants.CARD_HUA_COUNT; i++) {
			if (this._logic.is_magic_index(i))
				continue;
			cbCurrentCard = _logic.switch_to_card_data(i);
			chr.set_empty();
			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card(cards_index, weaveItems, cbWeaveCount, cbCurrentCard, chr,
					GameConstants.HU_CARD_TYPE_ZIMO, seatIndex)) {
				cards[count++] = cbCurrentCard;
			}
		}
		return count;
	}

	@Override
	public void process_chi_hu_player_score(int seat_index, int provide_index, int operate_card, boolean zimo) {
		GRR._chi_hu_card[seat_index][0] = operate_card;

		GRR._win_order[seat_index] = 1;
		// 引用权位
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];

		int wFanShu = 2;// 番数
		countCardType(chr, seat_index);

		/////////////////////////////////////////////// 算分//////////////////////////
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
			GRR._lost_fan_shu[provide_index][seat_index] = wFanShu;
		}

		float lChiHuScore = wFanShu;

		////////////////////////////////////////////////////// 自摸 算分
		if (zimo) {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index) {
					continue;
				}
				int niao = GRR._count_pick_niao + GRR._player_niao_count_fei[seat_index] + GRR._player_niao_count_fei[i];
				if(is_mj_type(GameConstants.GAME_TYPE_MJ_YI_YANG_QIN_YOU)){
					niao = GRR._player_niao_count[i] + GRR._player_niao_count[seat_index];
				}
				float s = getScore(chr, lChiHuScore);
				
				pai_xing_fen[i] -= s;
				pai_xing_fen[seat_index] += s;
				
				s *= 1 << niao;
				// 胡牌分
				GRR._game_score[i] -= s;
				GRR._game_score[seat_index] += s;
			}
		}
		////////////////////////////////////////////////////// 点炮 算分
		else {
			float s = getScore(chr, lChiHuScore);
			
			pai_xing_fen[provide_index] -= s;
			pai_xing_fen[seat_index] += s;

			int niao = GRR._count_pick_niao + GRR._player_niao_count_fei[seat_index] + GRR._player_niao_count_fei[provide_index];
			if(is_mj_type(GameConstants.GAME_TYPE_MJ_YI_YANG_QIN_YOU)){
				niao = GRR._player_niao_count[provide_index] + GRR._player_niao_count[seat_index];
			}
			s *= 1 << niao;
			if(!is_mj_type(GameConstants.GAME_TYPE_MJ_YI_YANG_QIN_YOU))
				s = s * 2; // 出分数量是积分*2 放炮
			// 胡牌分
			GRR._game_score[provide_index] -= s;
			GRR._game_score[seat_index] += s;

		}

		// 设置变量
		GRR._provider[seat_index] = provide_index;
		_status_gang = false;
		_status_gang_hou_pao = false;

		change_player_status(seat_index, GameConstants.INVALID_VALUE);
	}

	private float getScore(ChiHuRight chr, float lChiHuScore) {

		float s = lChiHuScore;
		chr.opr_or(chr.qi_shou_bao_ting);

		int da_hu_count = chr.da_hu_count;
		if (!(chr.opr_and(GameConstants.CHR_HUNAN_MEN_QING)).is_empty()) {
			da_hu_count--;
		}
		if (da_hu_count > 0) {
			int count = 0;
			if (!(chr.opr_and(GameConstants.CHR_HUNAN_TIAN_HU)).is_empty()) {
				count++;
			}
			if (!(chr.opr_and(GameConstants.CHR_HUNAN_DI_HU)).is_empty()) {
				count++;
			}
			if (!(chr.opr_and(GameConstants.CHR_HUNAN_PENGPENG_HU)).is_empty()) {
				count++;
			}
			if (!(chr.opr_and(GameConstants.CHR_HUNAN_JIANGJIANG_HU)).is_empty()) {
				count++;
			}
			if (!(chr.opr_and(GameConstants.CHR_HUNAN_QING_YI_SE)).is_empty()) {
				count++;
			}
			if (!(chr.opr_and(GameConstants.CHR_HUNAN_QI_XIAO_DUI)).is_empty()) {
				count++;
			}
			if (!(chr.opr_and(GameConstants_YiYang.CHR_YI_TIAO_LONG)).is_empty()) {
				count++;
			}
			if (!(chr.opr_and(GameConstants.CHR_HUNAN_HAOHUA_QI_XIAO_DUI)).is_empty()) {
				if(is_mj_type(GameConstants.GAME_TYPE_MJ_YI_YANG_QIN_YOU)){
					count += 2;
				}else
					count++;
			}
			if (!(chr.opr_and(GameConstants.CHR_HUNAN_SHUANG_HAO_HUA_QI_XIAO_DUI)).is_empty()) {
				if(is_mj_type(GameConstants.GAME_TYPE_MJ_YI_YANG_QIN_YOU)){
					count += 3;
				}else
					count++;
			}
			if (!(chr.opr_and(GameConstants.CHR_HUNAN_THREE_HAOHUA_QI_YI_SE)).is_empty()) {
				if(is_mj_type(GameConstants.GAME_TYPE_MJ_YI_YANG_QIN_YOU)){
					count += 4;
				}else
					count++;
			}
			if (!(chr.opr_and(GameConstants.CHR_HUNAN_HAI_DI_LAO)).is_empty()) {
				count++;
			}
			if (!(chr.opr_and(GameConstants.CHR_HUNAN_QUAN_QIU_REN)).is_empty()) {
				count++;
			}
			if (!(chr.opr_and(GameConstants.CHR_HUNAN_QIANG_GANG_HU)).is_empty()) {
				count++;
			}
			if (!(chr.opr_and(GameConstants.CHR_HUNAN_GANG_KAI)).is_empty()) {
				count++;
			}
			if(is_mj_type(GameConstants.GAME_TYPE_MJ_YI_YANG_QIN_YOU)){
				if (count > 0) {
					s = 4 * (1 << (count - 1));
				}
				if (!(chr.opr_and(GameConstants.CHR_HUNAN_MEN_QING)).is_empty()) {
					s *= 2;
				}
			}else{
				if (count > 0) {
					s = 3 * (1 << (count - 1));
				}
				if (!(chr.opr_and(GameConstants.CHR_HUNAN_MEN_QING)).is_empty()) {
					s += 2;
				}
			}
		} else {
			if (!(chr.opr_and(GameConstants.CHR_HUNAN_MEN_QING)).is_empty()) {
				s = 2;
			} else {
				s = 1;
			}
		}

		if (!(chr.opr_and(GameConstants.CHR_HUNAN_QISHOU_BAO_TING)).is_empty()) {
			s = s * 2;
		}
		return s;
	}

	@Override
	protected void set_result_describe() {
		int chrTypes;
		long type = 0;
		for (int player = 0; player < getTablePlayerNumber(); player++) {
			StringBuilder gameDesc = new StringBuilder("");

			GRR._chi_hu_rights[player].opr_or(GRR._chi_hu_rights[player].qi_shou_bao_ting);

			chrTypes = GRR._chi_hu_rights[player].type_count;

			for (int typeIndex = 0; typeIndex < chrTypes; typeIndex++) {
				type = GRR._chi_hu_rights[player].type_list[typeIndex];

				if (GRR._chi_hu_rights[player].is_valid()) {

					if (type == GameConstants.CHR_ZI_MO) {
						gameDesc.append(" 自摸");
					}
					if (type == GameConstants.CHR_SHU_FAN) {
						gameDesc.append(" 接炮");
					}
					if (type == GameConstants.CHR_HUNAN_MEN_QING) {
						gameDesc.append(" 门清");
					}
					if (type == GameConstants.CHR_HUNAN_TIAN_HU) {
						gameDesc.append(" 天胡");
					}
					if (type == GameConstants.CHR_HUNAN_DI_HU) {
						gameDesc.append(" 地胡");
					}
					if (type == GameConstants.CHR_HUNAN_QISHOU_BAO_TING) {
						gameDesc.append(" 起手报听");
					}
					if (type == GameConstants.CHR_HUNAN_PENGPENG_HU) {
						gameDesc.append(" 碰碰胡");
					}
					if (type == GameConstants.CHR_HUNAN_JIANGJIANG_HU) {
						gameDesc.append(" 将将胡");
					}
					if (type == GameConstants.CHR_HUNAN_QING_YI_SE) {
						gameDesc.append(" 清一色");
					}
					if (type == GameConstants.CHR_HUNAN_QI_XIAO_DUI) {
						gameDesc.append(" 七小对");
					}
					if (type == GameConstants.CHR_HUNAN_HAOHUA_QI_XIAO_DUI) {
						gameDesc.append(" 豪华七小对");
					}
					if (type == GameConstants.CHR_HUNAN_SHUANG_HAO_HUA_QI_XIAO_DUI) {
						gameDesc.append(" 双豪华七小对");
					}
					if (type == GameConstants.CHR_HUNAN_THREE_HAOHUA_QI_YI_SE) {
						gameDesc.append(" 三豪华七小对");
					}
					if (type == GameConstants.CHR_HUNAN_HAI_DI_LAO) {
						gameDesc.append(" 海底胡");
					}
					if (type == GameConstants.CHR_HUNAN_QUAN_QIU_REN) {
						gameDesc.append(" 全求人");
					}
					if (type == GameConstants_YiYang.CHR_YI_TIAO_LONG) {
						gameDesc.append(" 一条龙");
					}
					if (type == GameConstants.CHR_HUNAN_QIANG_GANG_HU) {
						gameDesc.append(" 抢杠胡");
					}
					if (type == GameConstants.CHR_HUNAN_GANG_KAI) {
						gameDesc.append(" 杠上开花");
					}
				} else if (type == GameConstants.CHR_FANG_PAO) {
					gameDesc.append(" 放炮");
				}
			}

			int jie_gang = 0, fang_gang = 0, ming_gang = 0, an_gang = 0;

			if (GRR != null) {
				for (int tmpPlayer = 0; tmpPlayer < getTablePlayerNumber(); tmpPlayer++) {
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
				gameDesc.append(" 暗杠X" + an_gang);
			}
			if (ming_gang > 0) {
				gameDesc.append(" 明杠X" + ming_gang);
			}
			if (fang_gang > 0) {
				gameDesc.append(" 放杠X" + fang_gang);
			}
			if (jie_gang > 0) {
				gameDesc.append(" 接杠X" + jie_gang);
			}
			
			if(yi_zi_qiao[player])
				gameDesc.append(" 一字撬有喜");
			if(is_mj_type(GameConstants.GAME_TYPE_MJ_YI_YANG_QIN_YOU)){
				if (GRR._player_niao_count[player] > 0) {
					gameDesc.append(" 中鸟x" + GRR._player_niao_count[player]);
				}
				
				if (GRR._player_niao_count_fei[player] > 0) {
					gameDesc.append(" 飞鸟x" + GRR._player_niao_count_fei[player]);
				}
			}

			GRR._result_des[player] = gameDesc.toString();
		}
	}

	@Override
	public boolean handler_requst_pao_qiang(Player player, int pao, int qiang) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean handler_requst_nao_zhuang(Player player, int nao) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean handler_requst_message_deal(Player player, int seat_index, RoomRequest room_rq, int type) {
		return true;
	}

	public boolean estimate_player_out_card_respond(int seat_index, int card) {
		// 变量定义
		boolean bAroseAction = false;// 出现(是否)有

		// 用户状态
		for (int i = 0; i < getTablePlayerNumber(); i++) {

			_playerStatus[i].clean_action();
			_playerStatus[i].clean_weave();
		}

		int llcard = 0;
		PlayerStatus playerStatus = null;

		int action = GameConstants.WIK_NULL;

		// 动作判断
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			// 用户过滤
			if (seat_index == i)
				continue;

			playerStatus = _playerStatus[i];

			// if (!GameDescUtil.has_rule(gameRuleIndexEx,
			// GameConstants.GAME_RULE_HUNAN_MEN_QING)) {
			//// 碰牌判断
			action = _logic.check_peng(GRR._cards_index[i], card);
			if (action != 0 && !_playerStatus[i].is_bao_ting()) {
				playerStatus.add_action(action);
				playerStatus.add_peng(card, seat_index);
				bAroseAction = true;
			}
			if (GRR._left_card_count > llcard && !_playerStatus[i].is_bao_ting()) {
				// 杠牌判断
				action = _logic.estimate_gang_card_out_card(GRR._cards_index[i], card);
				if (action != 0) {
					playerStatus.add_action(GameConstants.WIK_GANG);
					playerStatus.add_gang(card, seat_index, 1);// 加上杠
					bAroseAction = true;
				}
			}
			// }

			// 可以胡的情况 判断
			if (_playerStatus[i].is_chi_hu_round()) {
				// 吃胡判断
				ChiHuRight chr = GRR._chi_hu_rights[i];// playerStatus._chiHuRight;
				chr.set_empty();
				if (_out_card_count == 1 && seat_index == _cur_banker) {
					chr.opr_or(GameConstants.CHR_HUNAN_DI_HU);
				}
				int cbWeaveCount = GRR._weave_count[i];
				action = analyse_chi_hu_card_chi_hu(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr,
						GameConstants.HU_CARD_TYPE_PAOHU, i);

				// 结果判断
				if (action != 0) {
					_playerStatus[i].add_action(GameConstants.WIK_CHI_HU);
					_playerStatus[i].add_chi_hu(card, seat_index);// 吃胡的组合
					chr.opr_or(GameConstants.CHR_SHU_FAN);
					bAroseAction = true;
				}
			}
		}

		if (bAroseAction) {
			_resume_player = _current_player;// 保存当前轮到的玩家
			_current_player = GameConstants.INVALID_SEAT;// 有需要操作的玩家。当前玩家为空
			_provide_player = seat_index;

		} else {

			return false;
		}

		return true;
	}

	public boolean exe_dispatch_last_card(int seat_index, int type, int delay_time) {

		if (delay_time > 0) {
			GameSchedule.put(new DispatchLastCardRunnable(this.getRoom_id(), seat_index, type, false), delay_time, TimeUnit.MILLISECONDS);// MJGameConstants.GANG_LAST_CARD_DELAY
		} else {
			// 发牌
			if (_handler_dispath_last_card != null) {
				this.set_handler(this._handler_dispath_last_card);
				this._handler_dispath_last_card.reset_status(seat_index, type);
				this._handler.exe(this);
			}
		}

		return true;
	}

	/**
	 * 调度 发最后4张牌
	 * 
	 * @param cur_player
	 * @param type
	 * @param tail
	 * @return
	 */
	@Override
	public boolean runnable_dispatch_last_card_data(int cur_player, int type, boolean tail) {
		// 牌局未开 或者等待状态 调度不需要执行 add by zain 2017/6/1
		if ((_game_status == GameConstants.GS_MJ_FREE || _game_status == GameConstants.GS_MJ_WAIT) && is_sys())
			return false;

		if (_handler_dispath_last_card != null) {
			// 发牌
			this.set_handler(this._handler_dispath_last_card);
			this._handler_dispath_last_card.reset_status(cur_player, type);
			this._handler.exe(this);
		}

		return true;
	}

	/**
	 * 获取飞鸟 数量
	 * 
	 * @return
	 */
	public int getFeiNiaoNum() {
		int num = 0;
		if (GameDescUtil.has_rule(gameRuleIndexEx, GameConstants_YiYang.GAME_RULE_HUNAN_CS_FEI_NIAO1)) {
			return GameConstants.FEINIAO_1;
		} else if (GameDescUtil.has_rule(gameRuleIndexEx, GameConstants_YiYang.GAME_RULE_HUNAN_CS_FEI_NIAO2)) {
			return GameConstants.FEINIAO_2;
		} else if (GameDescUtil.has_rule(gameRuleIndexEx, GameConstants_YiYang.GAME_RULE_HUNAN_CS_FEI_NIAO3)) {
			return GameConstants.FEINIAO_3;
		}
		return num;
	}

	/**
	 * 
	 * @param seat_index
	 * @param card
	 *            固定的鸟
	 * @param show
	 * @param add_niao
	 *            额外奖鸟
	 */
	public void set_niao_card_fei(int seat_index, int card, boolean show, int add_niao) {
		if (!GameDescUtil.has_rule(gameRuleIndexEx, GameConstants_YiYang.GAME_RULE_HUNAN_FEI_NIAO)) {
			return;
		}

		for (int i = 0; i < GameConstants.MAX_NIAO_CARD; i++) {
			GRR._cards_data_niao_fei[i] = GameConstants.INVALID_VALUE;
		}

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			GRR._player_niao_count_fei[i] = 0;
			for (int j = 0; j < GameConstants.MAX_NIAO_CARD; j++) {
				GRR._player_niao_cards_fei[i][j] = GameConstants.INVALID_VALUE;
			}
		}

		GRR._show_bird_effect = show;
		GRR._count_niao_fei = getFeiNiaoNum();

		if (GRR._count_niao_fei > GameConstants.ZHANIAO_0) {
			if (card == GameConstants.INVALID_VALUE) {
				int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
				_logic.switch_to_cards_index(_repertory_card, _all_card_len - GRR._left_card_count, GRR._count_niao_fei, cbCardIndexTemp);
				GRR._left_card_count -= GRR._count_niao_fei;
				_logic.switch_to_cards_data(cbCardIndexTemp, GRR._cards_data_niao_fei);
			}
		}
		// 中鸟个数
		GRR._count_pick_niao_fei = _logic.get_pick_niao_count(GRR._cards_data_niao_fei, GRR._count_niao_fei);

		for (int i = 0; i < GRR._count_niao_fei; i++) {
			int nValue = _logic.get_card_value(GRR._cards_data_niao_fei[i]);
			int seat = 0;
			seat = (seat_index + (nValue - 1) % 4) % 4;
			GRR._player_niao_cards_fei[seat][GRR._player_niao_count_fei[seat]] = GRR._cards_data_niao_fei[i];
			GRR._player_niao_count_fei[seat]++;
		}

		// 飞鸟
		GRR._count_pick_niao_fei = 0;
		for (int i = 0; i < 4; i++) {
			for (int j = 0; j < GRR._player_niao_count_fei[i]; j++) {
				//////////////////////////////////////////////// 长沙麻将抓鸟//////////////////
				if (is_mj_type(GameConstants.GAME_TYPE_THJ_YI_YANG)) {
					GRR._player_niao_cards_fei[i][j] = this.set_fei_niao_valid(GRR._player_niao_cards_fei[i][j], true);
				} else {
					if (seat_index == i) {// 自己有效
						GRR._count_pick_niao_fei++;
						GRR._player_niao_cards_fei[i][j] = this.set_fei_niao_valid(GRR._player_niao_cards_fei[i][j], true);// 胡牌的鸟生效
					} else {
						GRR._player_niao_cards_fei[i][j] = this.set_fei_niao_valid(GRR._player_niao_cards_fei[i][j], false);
					}
				}
			}
		}
	}

	/**
	 * 抓鸟 个数
	 * 
	 * @return
	 */
	public int getCsDingNiaoNum() {
		int nNum = GameConstants.ZHANIAO_0;
		if (GameDescUtil.has_rule(gameRuleIndexEx, GameConstants_YiYang.GAME_RULE_HUNAN_ZHANIAO1)) {
			return GameConstants.ZHANIAO_1;
		} else if (GameDescUtil.has_rule(gameRuleIndexEx, GameConstants_YiYang.GAME_RULE_HUNAN_ZHANIAO2)) {
			return GameConstants.ZHANIAO_2;
		} else if (GameDescUtil.has_rule(gameRuleIndexEx, GameConstants_YiYang.GAME_RULE_HUNAN_ZHANIAO3)) {
			return GameConstants.ZHANIAO_3;
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
		if(is_mj_type(GameConstants.GAME_TYPE_MJ_YI_YANG_QIN_YOU)){
			if(has_rule(GameConstants_YiYang.GAME_BU_ZHUA_NIAO))
				return;
		}else{
			if (!GameDescUtil.has_rule(gameRuleIndexEx, GameConstants_YiYang.GAME_RULE_HUNAN_ZHANIAO)) {
				return;
			}
		}

		for (int i = 0; i < GameConstants.MAX_NIAO_CARD; i++) {
			GRR._cards_data_niao[i] = GameConstants.INVALID_VALUE;
		}

		for (int i = 0; i < getTablePlayerNumber(); i++) {
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
		if(is_mj_type(GameConstants.GAME_TYPE_MJ_YI_YANG_QIN_YOU)){
			set_niao_card(seat_index);
			return;
		}
		// 中鸟个数
		GRR._count_pick_niao = _logic.get_pick_niao_count(GRR._cards_data_niao, GRR._count_niao);

		int[] player_niao_count = new int[getTablePlayerNumber()];
		int[][] player_niao_cards = new int[getTablePlayerNumber()][GameConstants.MAX_NIAO_CARD];
		
		for (int j = 0; j < GRR._count_niao; j++) {
			int data = GRR._cards_data_niao[j];
			int value = _logic.get_card_value(data);
			if (value == 1 || value == 5 || value == 9) {
				GRR._count_pick_niao++;
				player_niao_cards[seat_index][j] = GRR._cards_data_niao[j] + 1000;
			} else {
				player_niao_cards[seat_index][j] = GRR._cards_data_niao[j];
			}
			player_niao_count[seat_index]++;
		}
		
		GRR._player_niao_cards = player_niao_cards;
		GRR._player_niao_count = player_niao_count;
	}
	
	//////////////////
	public void set_pick_direction_niao_cards(int cards_data[], int card_num, int seat_index, int half_index) {
		int seat = (seat_index - half_index + getTablePlayerNumber()) % getTablePlayerNumber();
		for (int i = 0; i < card_num; i++) {
			int nValue = _logic.get_card_value(cards_data[i]);
			boolean flag = false;
			if(getTablePlayerNumber() == 2){
				if(seat_index == half_index && nValue % 2 == 1)
					flag = true;
			}else if(nValue % 4 == 1){
				if(GRR._chi_hu_rights[seat_index].is_valid())
					flag = true;
			}else if((seat + 1) % 4 == nValue % 4){
				flag = true;
			}
			if (flag) {
				GRR._player_niao_cards[seat_index][i] = GRR._cards_data_niao[i] + GameConstants.DING_NIAO_VALID;
				GRR._player_niao_count[seat_index]++;
			}else{
				GRR._player_niao_cards[seat_index][i] = GRR._cards_data_niao[i];
			}

		}
	}
	
	/**
	 * 设置鸟
	 */
	public void set_niao_card(int seat_index) {
		ChiHuRight chiHuRight = GRR._chi_hu_rights[seat_index];
		boolean is_zimo = false;
		boolean is_dian = false;
		boolean is_duo = false;
		if(chiHuRight.is_valid()){
			if(!chiHuRight.opr_and_long(GameConstants.CHR_ZI_MO).is_empty() || 
					!chiHuRight.opr_and_long(GameConstants.CHR_HUNAN_GANG_KAI).is_empty()){
				is_zimo = true;
			}else{
				is_dian = true;
			}
		}else{
			is_duo = true;
		}
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if(is_zimo)
				set_pick_direction_niao_cards(GRR._cards_data_niao, GRR._count_niao, i, seat_index);
			if(is_dian && (i == seat_index || !GRR._chi_hu_rights[i].opr_and(GameConstants.CHR_FANG_PAO).is_empty()))
				set_pick_direction_niao_cards(GRR._cards_data_niao, GRR._count_niao, i, seat_index);
			if(is_duo && (i == seat_index || GRR._chi_hu_rights[i].is_valid()))
				set_pick_direction_niao_cards(GRR._cards_data_niao, GRR._count_niao, i, seat_index);
		}
	}
		
	/////////////////////////
	
	
	

	@Override
	public boolean exe_out_card_bao_ting(int seat_index, int card, int type) {
		// 出牌
		this.set_handler(this._handler_out_card_bao_ting);
		this._handler_out_card_bao_ting.reset_status(seat_index, card, type);
		this._handler.exe(this);

		return true;
	}

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
			// 王牌过滤
			// if( i == get_magic_card_index() ) continue;
			//
			// //单牌统计
			// if( cbCardCount == 1 || cbCardCount == 3 ) cbReplaceCount++;
			//
			// if (cbCardCount == 4 )
			// {
			// nGenCount++;
			// }
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
				// 双豪华七小对
				return GameConstants.CHR_HUNAN_SHUANG_HAO_HUA_QI_XIAO_DUI;
			}
			if (nGenCount == 3) {
				return GameConstants.CHR_HUNAN_THREE_HAOHUA_QI_YI_SE;
			}
			return GameConstants.CHR_HUNAN_HAOHUA_QI_XIAO_DUI;
		} else {
			return GameConstants.CHR_HUNAN_QI_XIAO_DUI;
		}

	}

	@Override
	public boolean trustee_timer(int operate_id, int seat_index) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	protected void test_cards() {

		// 黑摸
		// int cards[] = new int[] { 0x06, 0x07, 0x08, 0x14, 0x15,0x16, 0x16,
		// 0x24, 0x25, 0x26, 0x27, 0x28, 0x29 };

		// 晃晃不能胡
		// int cards[] = new int[] { 0x12, 0x13, 0x16, 0x17, 0x18,0x22, 0x22,
		// 0x24, 0x25, 0x26, 0x28, 0x28, 0x28 };

		// 晃晃没显示听癞子
		// int cards[] = new int[] { 0x02, 0x04, 0x06, 0x06, 0x06,0x14, 0x15,
		// 0x16, 0x21, 0x21, 0x21, 0x22, 0x22 };

		// 晃晃没显示听癞子
		// int cards[] = new int[] { 0x22, 0x06, 0x06, 0x06, 0x06, 0x14, 0x15,
		// 0x16, 0x21, 0x21, 0x21, 0x21, 0x22 };

		// 双鬼没显示听鬼
		// int cards[] = new int[] { 0x01, 0x02, 0x15, 0x15, 0x16,0x17, 0x18,
		// 0x26, 0x26, 0x26, 0x27, 0x28, 0x29 };

		// int cards[] = new int[] { 0x01, 0x01, 0x01, 0x05, 0x06, 0x07,0x08,
		// 0x12, 0x13, 0x14, 0x17, 0x18, 0x19 };
		// int cards[] = new int[] { 0x01, 0x01, 0x01, 0x05, 0x06, 0x07,0x08,
		// 0x12, 0x13, 0x14, 0x17, 0x18, 0x19 };
		// int cards[] = new int[] { 0x01, 0x01, 0x01, 0x05, 0x06, 0x07,0x08,
		// 0x12, 0x13, 0x14, 0x17, 0x18, 0x19 };

		// 红中显示听多牌
		// int cards[] = new int[]
		// {0x02,0x03,0x04,0x07,0x08,0x09,0x11,0x11,0x11,0x18,0x18,0x25,0x26};
		// 杠5饼出错
		// int cards[] = new int[]
		// {0x02,0x02,0x06,0x07,0x22,0x22,0x22,0x25,0x25,0x02,0x27,0x28,0x29};

		// 五鬼不能胡
		// int cards[] = new int[]
		// {0x01,0x01,0x03,0x03,0x06,0x06,0x07,0x15,0x16,0x18,0x18,0x25,0x26};

		// 起手四红中

		// int cards[] = new int[]
		// {0x11,0x11,0x12,0x12,0x13,0x13,0x14,0x14,0x19,0x19,0x19,0x19,0x16};

		// 红中不能胡1
		// int cards[] = new int[]
		// {0x11,0x12,0x12,0x13,0x14,0x16,0x16,0x17,0x18,0x21,0x22,0x23,0x35};
		// 红中不能胡2
		// int cards[] = new int[]
		// {0x01,0x02,0x03,0x17,0x17,0x21,0x22,0x22,0x23,0x24,0x26,0x28,0x35};
		// 杠牌重复/
		// int cards[] = new int[] { 0x09, 0x09, 0x18, 0x18, 0x19, 0x19, 0x19,
		// 0x26, 0x27, 0x28, 0x29, 0x29, 0x29 };
		// int cards[] = new int[] { 0x11, 0x11, 0x11, 0x21, 0x22, 0x23, 0x12,
		// 0x13, 0x14, 0x15, 0x16, 0x17, 0x18 };

		// 花牌
		// int cards[] = new int[]
		// {0x09,0x09,0x18,0x18,0x19,0x19,0x19,0x26,0x27,0x28,0x29,0x29,0x38};
		// int cards[] = new int[]
		// {0x09,0x09,0x18,0x18,0x19,0x19,0x19,0x26,0x27,0x28,0x29,0x29,0x38};

		// int cards[] = new int[]
		// {0x05,0x05,0x07,0x07,0x08,0x09,0x09,0x17,0x17,0x18,0x18,0x19,0x19};
		// int cards[] = new int[]
		// {0x01,0x02,0x03,0x04,0x05,0x06,0x16,0x16,0x21,0x21,0x21,0x15,0x15};

		// int cards[] = new int[]
		// {0x01,0x02,0x03,0x04,0x05,0x06,0x16,0x16,0x21,0x21,0x21,0x15,0x15};
		// int cards[] = new int[]
		// {0x03,0x03,0x05,0x05,0x06,0x07,0x08,0x08,0x08,0x12,0x12,0x12,0x22};
		// int cards[] = new int[]
		// {0x09,0x09,0x18,0x18,0x19,0x19,0x19,0x26,0x27,0x28,0x29,0x29,0x38};
		// int cards[] = new int[]
		// {0x09,0x09,0x18,0x18,0x19,0x19,0x19,0x26,0x27,0x28,0x29,0x29,0x38};
		// //int cards[] = new int[]
		// {0x09,0x09,0x18,0x18,0x19,0x19,0x19,0x26,0x27,0x28,0x29,0x29,0x38};
		// int cards[] = new int[]
		// {0x09,0x09,0x18,0x18,0x19,0x19,0x19,0x26,0x27,0x28,0x29,0x29,0x38};
		// int cards[] = new int[]
		// {0x09,0x09,0x18,0x18,0x19,0x19,0x19,0x26,0x27,0x28,0x29,0x29,0x38};

		// 单吊
		// int cards[] = new int[]
		// {0x01,0x01,0x01,0x02,0x02,0x02,0x03,0x03,0x03,0x04,0x04,0x04,0x06};
		// int cards[] = new int[]
		// {0x01,0x01,0x01,0x02,0x02,0x02,0x03,0x03,0x03,0x04,0x04,0x04,0x06};
		// int cards[] = new int[]
		// {0x01,0x01,0x01,0x02,0x02,0x02,0x03,0x03,0x03,0x04,0x04,0x04,0x06};

		// 全球人
		// int cards[] = new int[] { 0x13, 0x14, 0x15, 0x16, 0x17, 0x17, 0x17,
		// 0x18, 0x18, 0x18, 0x19, 0x19, 0x19 };

		// int cards[] = new int[] { 0x23, 0x24, 0x25, 0x16, 0x17, 0x17, 0x17,
		// 0x08, 0x08, 0x08, 0x19, 0x19, 0x19 };
		// 河南麻将七小对
		// int cards[] = new int[]
		// {0x02,0x02,0x05,0x05,0x09,0x09,0x12,0x12,0x14,0x14,0x18,0x27,0x27};

		// int cards[] = new int[] { 0x21, 0x21, 0x21, 0x03, 0x03, 0x03, 0x06,
		// 0x06, 0x06, 0x07, 0x07, 0x07, 0x09 };

		// 吃
		// int cards[] = new int[]
		// {0x01,0x02,0x05,0x05,0x21,0x23,0x24,0x26,0x26,0x27,0x28,0x29,0x29};

		// 板板胡
		// int cards[] = new int[]
		// {0x03,0x14,0x16,0x16,0x17,0x17,0x17,0x21,0x21,0x23,0x23,0x19,0x19};
		// 红中中2鸟算分不对
		// int cards[] = new int[]
		// {0x07,0x09,0x12,0x12,0x15,0x17,0x21,0x21,0x21,0x23,0x23,0x23,0x35};
		// 杠牌胡两张

		// int cards[] = new int[] { 0x01, 0x01, 0x1, 0x4, 0x2, 0x3, 0x13, 0x14,
		// 0x12, 0x21, 0x23, 0x23, 0x23 };
		// int cards[] = new int[] { 0x01, 0x06, 0x06, 0x06, 0x06, 0x04, 0x05,
		// 0x08, 0x08, 0x08, 0x09, 0x09, 0x09 };
		// int cards[] = new int[]
		// {0x01,0x06,0x06,0x06,0x03,0x04,0x05,0x08,0x08,0x08,0x09,0x09,0x09};
		// int cards[] = new int[]
		// {0x01,0x06,0x06,0x06,0x03,0x04,0x05,0x08,0x08,0x08,0x09,0x09,0x09};

		// int cards[] = new int[]
		// {0x01,0x02,0x04,0x04,0x05,0x07,0x07,0x08,0x14,0x19,0x21,0x23,0x29};
		// 四喜

		// int cards[] = new int[] { 0x11, 0x11, 0x11, 0x11, 0x14, 0x14, 0x16,
		// 0x16, 0x17, 0x17, 0x19, 0x19, 0x19 };

		// int cards[] = new int[] { 0x11, 0x11, 0x11, 0x11, 0x14, 0x14, 0x15,
		// 0x15, 0x17, 0x17, 0x19, 0x19, 0x19 };

		// int cards[] = new int[] { 0x11, 0x11, 0x11, 0x11, 0x14, 0x14, 0x15,
		// 0x15, 0x17, 0x17, 0x19, 0x19, 0x19 };
		// int cards[] = new int[] { 0x11, 0x11, 0x11, 0x11, 0x14, 0x14, 0x15,
		// 0x15, 0x17, 0x17, 0x19,
		//
		// 0x19, 0x19 };
		// int cards[] = new int[] { 0x15, 0x19, 0x13, 0x02, 0x04, 0x03, 0x15,
		// 0x15, 0x17, 0x17, 0x19,0x17, 0x19 };
		// int cards[] = new int[] { 0x11, 0x11, 0x11, 0x11, 0x14, 0x14, 0x15,
		// 0x15, 0x17, 0x17, 0x19, 0x19, 0x19 };
		// int cards[] = new int[] { 0x15, 0x19, 0x13, 0x02, 0x04, 0x03, 0x15,
		// 0x15, 0x17, 0x17, 0x19,0x17, 0x19 };
		// //碰碰胡
		// int cards[] = new int[] { 0x02, 0x02, 0x02, 0x03, 0x03,0x11, 0x11,
		// 0x11, 0x15, 0x15, 0x21, 0x21, 0x21 };
		// 清一色碰碰胡
		// int cards[] = new int[] { 0x06, 0x06, 0x06, 0x06, 0x17, 0x17, 0x17,
		// 0x23, 0x33, 0x33, 0x33, 0x23, 0x23 };

		// 将将胡
		// int cards[] = new int[] { 0x02, 0x02, 0x02, 0x05, 0x05, 0x05, 0x08,
		// 0x08, 0x08, 0x12, 0x12, 0x12, 0x15 };

		// 十三幺
		// int[] cards = new int[] { 0x01, 0x09, 0x11, 0x19, 0x21, 0x29, 0x31,
		// 0x32, 0x33, 0x34, 0x35, 0x36, 0x37 };
		// 十三烂
		// int[] cards = new int[] { 0x01, 0x03, 0x11, 0x19, 0x21, 0x29, 0x31,
		// 0x32, 0x33, 0x34, 0x35, 0x36, 0x37 };
		// 起手胡
		// int[] cards = new int[] { 0x01, 0x09, 0x11, 0x19, 0x21, 0x29, 0x31,
		// 0x32, 0x33, 0x34, 0x35, 0x36, 0x37 };
		// 七对
		// int cards[] = new int[] { 0x11, 0x11, 0x11, 0x11, 0x14, 0x14, 0x15,
		// 0x15, 0x17, 0x17, 0x19, 0x19, 0x04 };2
		// 王闯王
		// int cards[] = new int[] { 0x06, 0x07, 0x35, 0x13, 0x14, 0x15, 0x13,
		// 0x14, 0x15, 0x17, 0x17, 0x17, 0x36 };
		int cards[] = new int[] { 0x01,0x02,0x03,0x04,0x05,0x06,0x08,0x08,0x08,0x21,0x22,0x22,0x23};
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			for (int j = 0; j < GameConstants.MAX_INDEX; j++) {
				GRR._cards_index[i][j] = 0;
			}
		}
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			for (int j = 0; j < 13; j++) {
				GRR._cards_index[i][_logic.switch_to_card_index(cards[j])] += 1;
			}
		}

		/*************** 如果要测试线上实际牌 把下面代码取消注释 放入实际牌型即可 ***********************/
		// int[] realyCards = new int[] { 34, 19, 25, 8, 9, 6, 4, 3, 24, 41, 20,
		// 35, 2, 7, 1, 18, 17, 7, 34, 8, 9, 6, 41,
		// 35, 21, 2, 9, 21, 1, 2, 38, 34, 5, 39, 40, 21, 39, 33, 18, 38, 23,
		// 38, 37, 3, 33, 19, 24, 20, 22, 4, 39,
		// 3, 7, 5, 8, 18, 35, 36, 22, 2, 1, 22, 24, 23, 40, 35, 17, 4, 25, 36,
		// 19, 8, 5, 41, 6, 33, 20, 24, 40,
		// 38, 40, 25, 17, 6, 17, 34, 4, 20, 37, 37, 7, 37, 36, 25, 3, 41, 23,
		// 33, 39, 36, 21, 18, 9, 1, 22, 23, 5,
		// 19 };
		// testRealyCard(realyCards);

		/********
		 * 下面这个给测试用的 如果开发用 把上面的注释去掉 realyCards
		 **************************/

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

	protected boolean on_handler_game_finish(int seat_index, int reason) {
		int real_reason = reason;

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_player_ready[i] = 0;
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_GAME_END);
		GameEndResponse.Builder game_end = GameEndResponse.newBuilder();
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
			//一字撬奖喜
			if(is_mj_type(GameConstants.GAME_TYPE_MJ_YI_YANG_QIN_YOU) && has_rule(GameConstants_YiYang.GAME_YI_ZI_QIAO)){
				int cards[] = new int[GameConstants.MAX_COUNT];
				for (int i = 0; i < getTablePlayerNumber(); i++) {
					int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[i], cards);
					if(hand_card_count == 1){
						yi_zi_qiao[i] = true;
						for (int j = 0; j < getTablePlayerNumber(); j++) {
							if(j == i)
								continue;
							GRR._game_score[i] += 2;
							GRR._game_score[j] -= 2;
						}
					}
				}
			}
			
			game_end.setRoundOverType(1);
			game_end.setStartTime(GRR._start_time);

			game_end.setGameTypeIndex(GRR._game_type_index);
			roomResponse.setLeftCardCount(GRR._left_card_count);
			
			int left_card_count = GRR._left_card_count;
			for (int i = _all_card_len - GRR._left_card_count; i < _all_card_len; i++) {
				game_end.addCardsList(_repertory_card[_all_card_len - left_card_count]);
				left_card_count--;
			}

			// 特别显示的牌
			if (this.is_mj_type(GameConstants.GAME_TYPE_HUNAN_XIANG_TAN) || this.is_mj_type(GameConstants.GAME_TYPE_HUNAN_XIANG_TAN_DT)) {
				for (int i = 0; i < GRR._especial_card_count; i++) {
					game_end.addEspecialShowCards(GRR._especial_show_cards[i] + GameConstants.CARD_ESPECIAL_TYPE_DING_GUI);
				}
			} else {
				for (int i = 0; i < GRR._especial_card_count; i++) {
					game_end.addEspecialShowCards(GRR._especial_show_cards[i]);
				}
			}

			GRR._end_type = reason;

			// 杠牌，每个人的分数
			float lGangScore[] = new float[getTablePlayerNumber()];

			if (this.is_mj_type(GameConstants.GAME_TYPE_HU_NAN_CHANG_DE) || this.is_mj_type(GameConstants.GAME_TYPE_HUNAN_MJ_CD_DT)) {
				if (GameDescUtil.has_rule(gameRuleIndexEx, GameConstants.GAME_RULE_HUNAN_HUANG_ZHUANG_HUANG_GANG)
						&& (reason == GameConstants.Game_End_DRAW || reason == GameConstants.Game_End_RELEASE_PLAY)) { // 流局并且荒庄荒杠
					for (int i = 0; i < getTablePlayerNumber(); i++) {
						for (int j = 0; j < getTablePlayerNumber(); j++) {
							_player_result.lost_fan_shu[i][j] += GRR._lost_fan_shu[i][j];
						}
					}
				} else {
					for (int i = 0; i < getTablePlayerNumber(); i++) {

						for (int j = 0; j < GRR._gang_score[i].gang_count; j++) {
							for (int k = 0; k < getTablePlayerNumber(); k++) {
								lGangScore[k] += GRR._gang_score[i].scores[j][k];// 杠牌，每个人的分数
							}
						}

						// 记录
						for (int j = 0; j < getTablePlayerNumber(); j++) {
							_player_result.lost_fan_shu[i][j] += GRR._lost_fan_shu[i][j];
						}

					}
				}
			} else {
				for (int i = 0; i < getTablePlayerNumber(); i++) {

					for (int j = 0; j < GRR._gang_score[i].gang_count; j++) {
						for (int k = 0; k < getTablePlayerNumber(); k++) {
							lGangScore[k] += GRR._gang_score[i].scores[j][k];// 杠牌，每个人的分数
						}
					}

					// 记录
					for (int j = 0; j < getTablePlayerNumber(); j++) {
						_player_result.lost_fan_shu[i][j] += GRR._lost_fan_shu[i][j];
					}

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

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				Int32ArrayResponse.Builder pnc = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GRR._count_niao; j++) {
					pnc.addItem(GRR._player_niao_cards[i][j]);
				}
				for (int j = 0; j < GRR._player_niao_count_fei[i]; j++) {
					pnc.addItem(GRR._player_niao_cards_fei[i][j]);
				}
				game_end.addPlayerNiaoCards(pnc);
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
				
				game_end.addStartHuScore(pai_xing_fen[i]);
				
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
		} else if ((!is_sys()) && (reason == GameConstants.Game_End_RELEASE_PLAY || reason == GameConstants.Game_End_RELEASE_NO_BEGIN
				|| reason == GameConstants.Game_End_RELEASE_RESULT || reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT || reason == GameConstants.Game_End_RELEASE_SYSTEM)) {
			end = true;
			real_reason = GameConstants.Game_End_DRAW;// 刘局
			game_end.setRoomOverType(1);
			game_end.setPlayerResult(this.process_player_result(reason));
		}
		game_end.setEndType(real_reason);

		////////////////////////////////////////////////////////////////////// 得分总的
		roomResponse.setGameEnd(game_end);

		this.send_response_to_room(roomResponse);

		record_game_round(game_end);

		// 结束后刷新玩家
		// RoomResponse.Builder roomResponse2 = RoomResponse.newBuilder();
		// roomResponse2.setGameStatus(_game_status);
		// roomResponse2.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);// 刷新玩家
		// this.load_player_info_data(roomResponse2);
		// this.send_response_to_room(roomResponse2);

		// 超时解散
		if (reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT || reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT) {
			for (int j = 0; j < getTablePlayerNumber(); j++) {
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
}
