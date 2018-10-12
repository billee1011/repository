package com.cai.game.mj.hainan.hainanmj;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.mj.GameConstants_HaiNan;
import com.cai.common.define.ERoomStatus;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.Player;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.WeaveItem;
import com.cai.common.util.GameDescUtil;
import com.cai.common.util.PerformanceTimer;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.DispatchCardHuaRunnable;
import com.cai.game.mj.AbstractMJTable;
import com.cai.game.mj.MJGameLogic.AnalyseItem;
import com.cai.game.mj.MJType;
import com.cai.game.util.AnalyseCardUtil;
import com.cai.game.util.GameUtilConstants;
import com.cai.service.PlayerServiceImpl;
import com.cai.util.ClubMsgSender;

import protobuf.clazz.Protocol.GameEndResponse;
import protobuf.clazz.Protocol.GameStartResponse;
import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomInfo;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class MJTable_HaiNan extends AbstractMJTable {

	private int[] dispatchcardNum; // 摸牌次数
	private boolean isCanGenZhuang; // 可以跟庄
	private int genZhuangCount; // 跟庄次数
	private int genZhuangCard; // 跟庄牌
	private int next_seat_index;
	private int shang_ju_zhuang;
	private MJHandlerGa_HaiNan ga_HaiNan; // 上噶Handler
	private int[] sanDaoPai; // 三道牌
	// protected MJHandlerYaoHaiDi_ND _handler_yao_hai_di_nd;
	private int lian_zhuang_count; // 连庄次数
	public int liu_ju_count; // 留局牌数
	public int don_ling_index; // 东令位置
	public int hu_index; //胡牌玩家
	public boolean isJieHu[];

	/**
	 * 跟庄操作
	 * 
	 * @param seat_index
	 * @param card
	 * @param isZhuang
	 */
	public void addGenZhuangCard(int seat_index, int card, boolean isZhuang) {
		if (isZhuang) {
			genZhuangCard = card;
			next_seat_index = (seat_index + getTablePlayerNumber() + 1) % getTablePlayerNumber();
			// if (!isDispatchcardNum(seat_index)) {
			// setGenZhuangCount();
			// }
		} else {
			if (genZhuangCard != card || seat_index != next_seat_index) {
				isCanGenZhuang = false;
			} else {
				next_seat_index = (seat_index + getTablePlayerNumber() + 1) % getTablePlayerNumber();
				if (next_seat_index == _cur_banker) {
					//WalkerGeek 二人场跟庄BUG
					setGenZhuangCount(seat_index);
				}
			}
		}
	}

	/**
	 * 跟庄状态
	 * 
	 * @return the isCanGenZhuang
	 */
	public boolean isCanGenZhuang() {
		return isCanGenZhuang;
	}

	/**
	 * 跟庄次数
	 * 
	 * @return the genZhuangCount
	 */
	public int getGenZhuangCount() {
		return genZhuangCount;
	}

	/**
	 * 跟庄次数累计
	 * 
	 * @param genZhuangCount
	 *            the genZhuangCount to set
	 */
	public void setGenZhuangCount(int seat_index) {
		operate_effect_action(seat_index, GameConstants.Effect_Action_Other, 1, new long[]{GameConstants_HaiNan.EFFECT_ACTION_OTHER_GENGPAI}, 1000, GameConstants.INVALID_SEAT);
		
		this.genZhuangCount++;
	}

	/** 摸牌数累计 */
	public void addDispatchcardNum(int seat_index) {
		dispatchcardNum[seat_index]++;
	}

	/**
	 * 是否第一次摸牌
	 * 
	 * @param seat_index
	 * @return
	 */
	public boolean isDispatchcardNum(int seat_index) {
		return dispatchcardNum[seat_index] == 1;
	}

	/**
	* 
	*/
	public MJTable_HaiNan() {
		super(MJType.GAME_TYPE_MJ_HAI_NAN);
	}

	private static final long serialVersionUID = 1L;

	@Override
	public int getTablePlayerNumber() {
		if (has_rule(GameConstants_HaiNan.GAME_RULE_HAINAN_PLAYER_2)) {
			return GameConstants.GAME_PLAYER - 2;
		} else if (has_rule(GameConstants_HaiNan.GAME_RULE_HAINAN_PLAYER_3)) {
			return GameConstants.GAME_PLAYER - 1;
		} else {
			return GameConstants.GAME_PLAYER;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.cai.game.mj.AbstractMJTable#onInitTable()
	 */
	@Override
	protected void onInitTable() {
		_handler_dispath_card = new MJHandlerDispatchCard_HaiNan();
		_handler_out_card_operate = new MJHandlerOutCardOperate_HaiNan();
		_handler_gang = new MJHandlerGang_HaiNan();
		_handler_chi_peng = new MJHandlerChiPeng_HaiNan();
		ga_HaiNan = new MJHandlerGa_HaiNan();
		sanDaoPai = new int[getTablePlayerNumber()];
		lian_zhuang_count = 0;

		// _handler_yao_hai_di_nd = new MJHandlerYaoHaiDi_ND();
	}

	/**
	 * 参数初始化
	 */
	protected void onInitParam() {
		isJieHu = new boolean[getTablePlayerNumber()];
		hu_index = GameConstants.INVALID_SEAT;
		dispatchcardNum = new int[GameConstants.GAME_PLAYER];
		isCanGenZhuang = true;
		genZhuangCount = 0;
		sanDaoPai = new int[getTablePlayerNumber()];
		if (_cur_round == 1) {
			// shang_ju_zhuang = 0;
			lian_zhuang_count = 0;
		}
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			isJieHu[i] = false;
		}
		liu_ju_count = 0;

	}

	public void progress_banker_select() {
		// if (_cur_banker == GameConstants.INVALID_SEAT) {
		// _cur_banker = getOpenRoomIndex();// 创建者的玩家为专家
		// _shang_zhuang_player = GameConstants.INVALID_SEAT;
		// _lian_zhuang_player = GameConstants.INVALID_SEAT;
		// }

		if (_cur_round < 2) {// 金币场 随机庄家
			Random random = new Random();//
			int rand = random.nextInt(6) + 1 + random.nextInt(6) + 1;
			_cur_banker = rand % getTablePlayerNumber();//

			_shang_zhuang_player = GameConstants.INVALID_SEAT;
			_lian_zhuang_player = GameConstants.INVALID_SEAT;
			shang_ju_zhuang = 0;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.cai.game.mj.AbstractMJTable#on_game_start()
	 */
	@Override
	protected boolean on_game_start() {
		changPlayerIndex();
		// 首局东令位置
		don_ling_index = _cur_banker;
		// 噶
		if (has_rule(GameConstants_HaiNan.GAME_RULE_HAINAN_SHANG_GA)) {
			set_handler(ga_HaiNan);
			ga_HaiNan.exe(this);
			return true;
		}
		return on_game_start_real();
	}
	
	/**
	 * 开始处理换位置
	 */
	public void changPlayerIndex(){
		Player[] players_temp = new Player[this.getTablePlayerNumber()];
		PlayerStatus status_temp[] = new PlayerStatus[this.getTablePlayerNumber()];
		int gamePlayerIndex[] = new int[this.getTablePlayerNumber()];
		int gamePlayerIndexRlayerStatus[] = new int[this.getTablePlayerNumber()];
		//int card_index_temp[][] = new int[this.getTablePlayerNumber()][GameConstants.MAX_INDEX];
		
		for(int i=0; i < this.getTablePlayerNumber(); i++){
			int index = (_cur_banker+i+this.getTablePlayerNumber()) %this.getTablePlayerNumber();
			if(_cur_round > 1 &&shang_ju_zhuang == i){
				shang_ju_zhuang = index;
			}
			gamePlayerIndex[i] = index;
			
		}
		
		//数据切换
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			int index = gamePlayerIndex[i];
			players_temp[i] = this.get_players()[index];
			status_temp[i] = _playerStatus[index];
			gamePlayerIndexRlayerStatus[i] = _gameRoomRecord.release_players[index];
		}

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			this.get_players()[i] = players_temp[i];
			_playerStatus[i] = status_temp[i];
			_gameRoomRecord.release_players[i] = gamePlayerIndexRlayerStatus[i]  ;
		}
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			int index = gamePlayerIndex[i];
			this.get_players()[index].set_seat_index(gamePlayerIndex[i]);
		}
		
		GRR._banker_player = 0;
		_current_player = 0;
		_cur_banker = 0;
		_player_result.changIndexMJ(gamePlayerIndex, this.getTablePlayerNumber());
		operate_player_info();
		
	}
	
	public boolean operate_player_info() {
		ClubMsgSender.roomPlayerStatusUpdate(ERoomStatus.TABLE_REFRESH, this); // 换位置后同步到中心服
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);
		roomResponse.setGameStatus(_game_status);

		load_player_info_data(roomResponse);

		if (GRR != null)
			GRR.add_room_response(roomResponse);

		send_response_to_room(roomResponse);

		return true;
	}

	protected void init_shuffle() {
		if (has_rule(GameConstants_HaiNan.GAME_RULE_HAINAN_WU_ZI_PAI)) {
			_repertory_card = new int[GameConstants_HaiNan.CARD_DATA_MAX1.length];
			shuffle(_repertory_card, GameConstants_HaiNan.CARD_DATA_MAX1);
		} else {
			_repertory_card = new int[mjType.getCardLength()];
			shuffle(_repertory_card, mjType.getCards());
		}
	};

	public boolean on_game_start_real() {
		onInitParam();
		GameStartResponse.Builder gameStartResponse = GameStartResponse.newBuilder();
		gameStartResponse.setBankerPlayer(GRR._banker_player);
		gameStartResponse.setCurrentPlayer(_current_player);
		gameStartResponse.setLeftCardCount(GRR._left_card_count);

		int hand_cards[][] = new int[getTablePlayerNumber()][GameConstants.MAX_COUNT];
		// 发送数据
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[i], hand_cards[i]);
			gameStartResponse.addCardsCount(hand_card_count);
		}
		_game_status = GameConstants.GS_MJ_PLAY;
		// 发送数据
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

			// 只发自己的牌
			gameStartResponse.clearCardData();
			for (int j = 0; j < GameConstants.MAX_COUNT; j++) {
				gameStartResponse.addCardData(hand_cards[i][j]);
			}

			// 回放数据
			GRR._video_recode.addHandCards(cards);

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			this.load_room_info_data(roomResponse);
			this.load_common_status(roomResponse);
			roomResponse.setType(MsgConstants.RESPONSE_GAME_START);

			if (GameDescUtil.has_rule(gameRuleIndexEx, GameConstants.GAME_RULE_ALL_RANDOM_INDEX)) {
				this.load_room_info_data(roomResponse);
				this.load_common_status(roomResponse);

				if (this._cur_round == 1) {
					this.load_player_info_data(roomResponse);
				}
			}

			// 记录
			roomResponse.setGameStart(gameStartResponse);
			roomResponse
					.setCurrentPlayer(_current_player == GameConstants.INVALID_SEAT ? _resume_player : _current_player);
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

			for (int j = 0; j < GameConstants.MAX_COUNT; j++) {
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
		if (is_qishou_hu == false) {
			this.exe_dispatch_card(_current_player, GameConstants.WIK_NULL, GameConstants.DELAY_SEND_CARD_DELAY);
		}

		return false;
	}

	@Override
	public int analyse_chi_hu_card(int[] cards_index, WeaveItem[] weaveItems, int weave_count, int cur_card,
			ChiHuRight chiHuRight, int card_type, int _seat_index) {

		if (cur_card == 0) {
			return GameConstants.WIK_NULL;
		}
		if(has_rule(GameConstants_HaiNan.GAME_RULE_HAINAN_BU_KE_DIAN_PAO)){
			if(card_type == GameConstants.HU_CARD_TYPE_QIANGGANG && !has_rule(GameConstants_HaiNan.GAME_RULE_HAINAN_KE_QIANG_GANG_HU)){
				return GameConstants.WIK_NULL;
			}
		}

		int cbChiHuKind = GameConstants.WIK_NULL;
		// 构造扑克
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}
		
		
		// 插入扑克
		if (cur_card != GameConstants.INVALID_VALUE) {
			cbCardIndexTemp[_logic.switch_to_card_index(cur_card)]++;
		}
		
		boolean you_fan = false;
		//带一个杠算有番
		for (int i = 0; i < weave_count; i++) {
			if (weaveItems[i].weave_kind == GameConstants.WIK_GANG) {
				you_fan = true;
			}
		}
		//花胡算有番
		if(chiHuRight.hua_count >= 7){
			you_fan = true;
		}
		
		// 七小对
		long qxd = _logic.is_qi_xiao_dui(cards_index, weaveItems, weave_count, cur_card);
		if (qxd != GameConstants.WIK_NULL) {
			you_fan = true;
			if (qxd == GameConstants.CHR_HUNAN_SHUANG_HAO_HUA_QI_XIAO_DUI) {
				chiHuRight.opr_or(GameConstants.CHR_HUNAN_HAOHUA_QI_XIAO_DUI);
			} else {
				chiHuRight.opr_or(qxd);
			}
			cbChiHuKind = GameConstants.WIK_CHI_HU;

		}

		// 十三幺
		boolean shiSanYao = _logic.isShiSanYao(cbCardIndexTemp, weaveItems, weave_count);
		if (shiSanYao) {
			you_fan = true;
			chiHuRight.opr_or(GameConstants.CHR_HUNAN_SHI_SAN_YAO);
			cbChiHuKind = GameConstants.WIK_CHI_HU;
		}

		if (chiHuRight.is_empty() == false) {
			// 清一色
			boolean qingyise = _logic.is_qing_yi_se(cards_index, weaveItems, weave_count, cur_card);
			if (qingyise) {
				you_fan = true;
				chiHuRight.opr_or(GameConstants.CHR_HUNAN_QING_YI_SE);
			}
			// 天胡地胡
			if(is_tian_hu(_seat_index, card_type, chiHuRight)){
				you_fan = true;
			}
			if(buildChiHuRight(card_type, chiHuRight)){
				you_fan = true;
			}
			if (card_type == GameConstants.HU_CARD_TYPE_QIANGGANG /*&& card_type != GameConstants.HU_CARD_TYPE_ZIMO*/) {
				/*if (has_rule(GameConstants_HaiNan.GAME_RULE_HAINAN_YOU_FAN)) {
					if (!has_fang(cards_index, weaveItems, weave_count, cur_card, chiHuRight, card_type, _seat_index)) {
						chiHuRight.set_empty();
						return GameConstants.WIK_NULL;
					}
				}*/
				has_fang(cards_index, weaveItems, weave_count, cur_card, chiHuRight, card_type, _seat_index,you_fan);
			} 
//			else {
//			}
			return cbChiHuKind;
		}

		// 天胡地胡
		if(is_tian_hu(_seat_index, card_type, chiHuRight)){
			you_fan = true;
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
			return GameConstants.WIK_NULL;
		}

		// 清一色
		boolean qingyise = _logic.is_qing_yi_se(cards_index, weaveItems, weave_count, cur_card);
		if (qingyise) {
			you_fan = true;
			chiHuRight.opr_or(GameConstants.CHR_HUNAN_QING_YI_SE);
		}

		// 碰碰胡
		boolean is_peng_hu = AnalyseCardUtil.analyse_peng_hu_by_cards_index(cards_index,
				_logic.switch_to_card_index(cur_card), magic_cards_index, magic_card_count);

		if (is_peng_hu) {
			boolean exist_eat = exist_eat(weaveItems, weave_count);
			if (!exist_eat) {
				you_fan = true;
				chiHuRight.opr_or(GameConstants.CHR_HUNAN_PENGPENG_HU);
			}
		}

		cbChiHuKind = GameConstants.WIK_CHI_HU;
		if(buildChiHuRight(card_type, chiHuRight)){
			you_fan = true;
		}
		if (card_type != GameConstants.HU_CARD_TYPE_QIANGGANG && card_type != GameConstants.HU_CARD_TYPE_ZIMO) {
			if (has_rule(GameConstants_HaiNan.GAME_RULE_HAINAN_YOU_FAN)) {
				if(has_fang(cards_index, weaveItems, weave_count, cur_card, chiHuRight, card_type, _seat_index,you_fan)){
					you_fan = true;
				}
				if (!you_fan) {
					chiHuRight.set_empty();
					return GameConstants.WIK_NULL;
				}
			}
		} else {
			has_fang(cards_index, weaveItems, weave_count, cur_card, chiHuRight, card_type, _seat_index,you_fan);
		}

		return cbChiHuKind;
	}

	/**
	 * 有番判断
	 * 
	 * @param cards_index
	 * @param weaveItems
	 * @param weave_count
	 * @param cur_card
	 * @param chiHuRight
	 * @param card_type
	 * @param _seat_index
	 * @return
	 */
	public boolean has_fang(int[] cards_index, WeaveItem[] weaveItems, int weave_count, int cur_card,
			ChiHuRight chiHuRight, int card_type, int _seat_index,boolean has_fang) {
		// boolean has_eyes = false;
		// 构造扑克
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		// 插入扑克
		if (cur_card != GameConstants.INVALID_VALUE) {
			cbCardIndexTemp[_logic.switch_to_card_index(cur_card)]++;
		}

		// 2 门清
		if (_logic.is_men_qing_hainan(weaveItems, weave_count) != GameConstants.WIK_NULL) {
			has_fang = true;
		}

		// 3 有眼
		if (AnalyseCardUtil.analyse_258_by_cards_index(cards_index, _logic.switch_to_card_index(cur_card), new int[0],
				0) && _logic.is_qi_xiao_dui(cards_index, weaveItems, weave_count, cur_card) == GameConstants.WIK_NULL) {
			has_fang = true;
		}

		// 4箭刻牌 - 5 风刻牌
		int card_fen = GameConstants.INVALID_CARD;
		int index = _seat_index;
		if(has_rule(GameConstants_HaiNan.GAME_RULE_HAINAN_PLAYER_2)){
			if(_seat_index == _cur_banker){
				index = 0;
			}else{
				index = 1;
			}
		}
		switch (index) {
		case 0:
			card_fen = 0x31;
			break;
		case 1:
			if(has_rule(GameConstants_HaiNan.GAME_RULE_HAINAN_PLAYER_2)){
				card_fen = 0x33;
			}else{
				card_fen = 0x32;
			}
			break;
		case 2:
			card_fen = 0x33;
			break;
		case 3:
			card_fen = 0x34;
			break;

		}
		// 组合中遍历
		for (int i = 0; i < weave_count; i++) {
			if (weaveItems[i].weave_kind == GameConstants.WIK_PENG) {
				if (weaveItems[i].center_card == 0x35 || weaveItems[i].center_card == 0x36
						|| weaveItems[i].center_card == 0x37) {
					has_fang = true;
				}
				if (weaveItems[i].center_card == card_fen) {
					has_fang = true;
				}
			} else if (weaveItems[i].weave_kind == GameConstants.WIK_GANG) {
				if (weaveItems[i].center_card == card_fen) {
					has_fang = true;
				}
			}
		}
		// 手牌中
		if (cbCardIndexTemp[_logic.switch_to_card_index(0x35)] == 3
				|| cbCardIndexTemp[_logic.switch_to_card_index(0x36)] == 3
				|| cbCardIndexTemp[_logic.switch_to_card_index(0x37)] == 3) {
			has_fang = true;
		}
		if (cbCardIndexTemp[_logic.switch_to_card_index(card_fen)] >= 3) {
			has_fang = true;
		}

		// 6令牌
		if(has_rule(GameConstants_HaiNan.GAME_RULE_HAINAN_JIAO_LING)){
			int card_index = getLingPaiIndex(_seat_index);
			if (card_index != -1) {
				if (cbCardIndexTemp[card_index] >= 3) {
					has_fang = true;
				}
				for (int i = 0; i < weave_count; i++) {
					if (weaveItems[i].weave_kind == GameConstants.WIK_PENG) {
						if (weaveItems[i].center_card == _logic.switch_to_card_data(card_index)) {
							has_fang = true;
						}
					}
				}
			}
		}
		
		// 8 番花对位
		if (getFanHuaPaiIndex(_seat_index,  _logic.switch_to_card_data(getFanHua(_seat_index)))) {
			has_fang = true;
		}

		// 7 混一色
		int pai_se_count = _logic.get_se_count(cbCardIndexTemp, weaveItems, weave_count);
		boolean has_feng_pai = has_feng_pai(cbCardIndexTemp, weaveItems, weave_count);
		boolean can_hun_yi_se = pai_se_count == 1 && has_feng_pai;
		if (can_hun_yi_se) {
			has_fang = true;
		}

		// 1只吃不碰
		if (exist_eat_has_fan(weaveItems, weave_count)) {
			if (_logic.analyse_card_zhi_chi(cbCardIndexTemp, weaveItems, weave_count, new ArrayList<AnalyseItem>(),
					card_fen)) {
				return true;
			}
		}

		// 抢杠胡包赔
		if (!has_fang && has_rule(GameConstants_HaiNan.GAME_RULE_HAINAN_YOU_FAN)) {
			chiHuRight.can_bao_si_peng = true;
		}
		return has_fang;
	}

	public boolean has_feng_pai(int[] cards_index, WeaveItem[] weaveItems, int weaveCount) {
		for (int i = 0; i < weaveCount; i++) {
			int card = weaveItems[i].center_card;
			int color = _logic.get_card_color(card);
			if (color == 3)
				return true;
		}
		for (int i = 27; i < 34 && i < cards_index.length; i++) {
			if (cards_index[i] > 0)
				return true;
		}
		return false;
	}

	/**
	 * 天胡地胡
	 * 
	 * @param seat_index
	 * @param card_type
	 * @param chiHuRight
	 */
	public boolean is_tian_hu(int seat_index, int card_type, ChiHuRight chiHuRight) {
		boolean flag = false;
		if (card_type != GameConstants.HU_CARD_TYPE_QIANGGANG || card_type != GameConstants.HU_CARD_TYPE_PAOHU) {

			if (isDispatchcardNum(_cur_banker)) {
				if (seat_index == _cur_banker && _out_card_count == 0) {
					chiHuRight.opr_or(GameConstants.CHR_HUNAN_TIAN_HU);
					flag = true;
				} else {
					boolean can = true;
					for (int i = 0; i < getTablePlayerNumber(); i++) {
						if (i == _cur_banker) {
							continue;
						}
						if (isDispatchcardNum(i)) {
							can = false;
						}
					}
					if (can) {
						if (_out_card_count == 1) {
							chiHuRight.opr_or(GameConstants.CHR_HUNAN_DI_HU);
							flag = true;
						}
					}
				}
			}
		}
		return flag;
	}

	public boolean exist_eat(WeaveItem weaveItem[], int cbWeaveCount) {
		for (int i = 0; i < cbWeaveCount; i++) {
			if (weaveItem[i].weave_kind == GameConstants.WIK_LEFT || weaveItem[i].weave_kind == GameConstants.WIK_RIGHT
					|| weaveItem[i].weave_kind == GameConstants.WIK_CENTER)
				return true;
		}


		return false;
	}
	
	public boolean exist_eat_has_fan(WeaveItem weaveItem[], int cbWeaveCount) {
		for (int i = 0; i < cbWeaveCount; i++) {
			if (weaveItem[i].weave_kind != GameConstants.WIK_LEFT && weaveItem[i].weave_kind != GameConstants.WIK_RIGHT
					&& weaveItem[i].weave_kind != GameConstants.WIK_CENTER)
				return false;
		}

		return true;
	}

	public boolean buildChiHuRight(int card_type, ChiHuRight chiHuRight) {
		boolean flag = false;
		// 海底包牌
		if (has_rule(GameConstants_HaiNan.GAME_RULE_HAINAN_HAI_DI_BAO_PAI)) {
			if (GRR._left_card_count > 15 && GRR._left_card_count <= getTablePlayerNumber() + 15
					&& (card_type == GameConstants.HU_CARD_TYPE_QIANGGANG
							|| card_type == GameConstants.HU_CARD_TYPE_PAOHU)) {
				chiHuRight.opr_or(GameConstants.CHR_HUNAN_HAI_DI_PAO);
				chiHuRight.can_bao_si_peng = true;
			}
		}
		if (card_type == GameConstants.HU_CARD_TYPE_GANG_KAI) {
			chiHuRight.opr_or(GameConstants.CHR_HUNAN_GANG_KAI);
			flag = true;
		} else if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
			chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
		} else if (card_type == GameConstants.HU_CARD_TYPE_QIANGGANG) {
			chiHuRight.opr_or(GameConstants.CHR_HUNAN_QIANG_GANG_HU);// 抢杠胡
		} else if (card_type == GameConstants_HaiNan.HU_CARD_TYPE_BU_HUA) {
			flag = true;
			chiHuRight.opr_or(GameConstants_HaiNan.CHR_MJ_HAI_NAN_HU_BU_HUA);// 花上添花
		} else {
			chiHuRight.opr_or(GameConstants.CHR_SHU_FAN);
		}
		return flag;
	}

	@Override
	public void process_chi_hu_player_score(int seat_index, int provide_index, int operate_card, boolean zimo) {
		GRR._chi_hu_card[seat_index][0] = operate_card;

		GRR._win_order[seat_index] = 1;
		// 引用权位
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];

		int wFanShu = get_chi_hu_fen(chr, zimo,seat_index);
		
		countCardType(chr, seat_index);
		int lChiHuScore = wFanShu * GameConstants.CELL_SCORE;

		// 统计
		if (zimo) {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index) {
					continue;
				}
				GRR._lost_fan_shu[i][seat_index] = wFanShu;
			}
		} else {
			GRR._lost_fan_shu[provide_index][seat_index] = wFanShu;
		}

		if (zimo) {
			int bao_pei_index = -1;
			if (sanDaoPai[seat_index] == 4) {
				bao_pei_index = GRR._weave_items[seat_index][0].provide_player;
				hu_index = bao_pei_index;
				
			}
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index) {
					continue;
				}

				int s = lChiHuScore;
				int gaScore = getGaCount(seat_index, i);
				int zhuangXian = calcuZhuangXian(seat_index, i);
				int di = gaScore + zhuangXian;
				if (lian_zhuang_count > 1 && has_rule(GameConstants_HaiNan.GAME_RULE_HAINAN_LIAN_ZHUANG)) {
					di += (lian_zhuang_count - 1);
				}
				s = s * di;
				int index = i;
				if (bao_pei_index != -1) {
					index = bao_pei_index;
				}
				// 胡牌分
				GRR._game_score[index] -= s;
				GRR._game_score[seat_index] += s;
			}
		} else {
			int s = lChiHuScore;
//			int gaScore = getGaCount(seat_index, provide_index);
//			int zhuangXian = calcuZhuangXian(seat_index, provide_index);
//			int di = gaScore + zhuangXian;
//			if (lian_zhuang_count > 1 && has_rule(GameConstants_HaiNan.GAME_RULE_HAINAN_LIAN_ZHUANG)) {
//				di += (lian_zhuang_count - 1);
//			}

			boolean can_bao_pei = false;
			if (sanDaoPai[seat_index] >= 3) {
				int count = 0;
				for (int k = 0; k < 4; k++) {
					if (GRR._weave_items[seat_index][k].provide_player == provide_index) {
						count++;
					}

				}
				if (count >= 3) {
					can_bao_pei = true;
				}
			}
			if (has_rule(GameConstants_HaiNan.GAME_RULE_HAINAN_HAI_DI_BAO_PAI)) {
				if (GRR._left_card_count <= (15 + getTablePlayerNumber())) {
					can_bao_pei = true;
				}
			}
			if (can_bao_pei || chr.can_bao_si_peng || has_rule(GameConstants_HaiNan.GAME_RULE_HAINAN_FANG_GOU_JIAO)) {
				int score_temp = s;
				hu_index = provide_index;
				s = 0;
				for (int u = 0; u < getTablePlayerNumber(); u++) {
					if (u == seat_index) {
						continue;
					}
					int zhuangXian1 = calcuZhuangXian(u, seat_index);
					int di1 = getGaCount(seat_index, u) + zhuangXian1;
					if (lian_zhuang_count > 1 && has_rule(GameConstants_HaiNan.GAME_RULE_HAINAN_LIAN_ZHUANG)) {
						di1 += (lian_zhuang_count - 1);
					}
					s += score_temp * di1;
				}
				
				s++;
				GRR._game_score[provide_index] -= s;
				GRR._game_score[seat_index] += s;
			} else {
				for (int u = 0; u < getTablePlayerNumber(); u++) {
					int score_temp = s;
					if (u == seat_index) {
						continue;
					}
					int zhuangXian1 = calcuZhuangXian(u, seat_index);
					int di1 = getGaCount(seat_index, u) + zhuangXian1;
					if (lian_zhuang_count > 1 && has_rule(GameConstants_HaiNan.GAME_RULE_HAINAN_LIAN_ZHUANG)) {
						di1 += (lian_zhuang_count - 1);
					}
					int score = score_temp * di1;
					if(provide_index == u){
						score++;
					}
					GRR._game_score[u] -= score;
					GRR._game_score[seat_index] += score;
				}
				
			}
			// 算完分后+1分炮分
			
			GRR._chi_hu_rights[provide_index].opr_or(GameConstants.CHR_FANG_PAO);
			GRR._provider[seat_index] = provide_index;
		}

		// 设置变量
		_status_gang = false;
		_status_gang_hou_pao = false;

		change_player_status(seat_index, GameConstants.INVALID_VALUE);
		return;

	}

	public int getLiuJuCount() {
		return 15 - liu_ju_count;
	}

	/**
	 * 噶数
	 * 
	 * @param seat_index
	 * @param provide_index
	 * @return
	 */
	public int getGaCount(int seat_index, int provide_index) {
		int ga = 0;
		if (has_rule(GameConstants_HaiNan.GAME_RULE_HAINAN_SHANG_GA)) {
			ga = _player_result.pao[seat_index] + _player_result.pao[provide_index];
		}
		// if(ga > 0){
		ga++;
		// }
		return ga;
	}

	// 胡牌底分
	public int get_chi_hu_fen(ChiHuRight chiHuRight, boolean zimo,int seat_index) {
		int wFanShu = 1;

		if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_DI_HU)).is_empty()) {
			wFanShu *= 3;
		}
		if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_TIAN_HU)).is_empty()) {
			wFanShu *= 3;
		}
		/*
		 * if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_QIANG_GANG_HU)).
		 * is_empty()) { wFanShu = 12; }
		 */
		if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_GANG_KAI)).is_empty()) {
			zimo = false;
			wFanShu *= 3;
		}
		if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_QING_YI_SE)).is_empty()) {
			wFanShu *= 2;
		}
		if (!(chiHuRight.opr_and(GameConstants_HaiNan.CHR_MJ_HAI_NAN_HU_BU_HUA)).is_empty()) {
			zimo = false;
			wFanShu *= 3;
		}
		if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_SHI_SAN_YAO)).is_empty()) {
			wFanShu *= 13;
		}

		if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_PENGPENG_HU)).is_empty()) {
			wFanShu *= 2;
		}
		if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_HAOHUA_QI_XIAO_DUI)).is_empty()) {
			wFanShu *= 3;
		}
		if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_QI_XIAO_DUI)).is_empty()) {
			wFanShu *= 2;
		}

		// if (wFanShu == 0) {
		// wFanShu = 1;
		// }
		
		if (zimo) {
			wFanShu *= 2;
		}
//		//杠番
//		wFanShu += getGanFan(seat_index);
		
		if (wFanShu > getMaxFan()) {
			wFanShu = getMaxFan();
		}

		return wFanShu;
	}

	public int getMaxFan() {
		int fan = 8;
		if (has_rule(GameConstants_HaiNan.GAME_RULE_HAINAN_BEI_12)) {
			fan = 12;
		} else if (has_rule(GameConstants_HaiNan.GAME_RULE_HAINAN_BEI_16)) {
			fan = 16;
		}
		return fan;
	}

	@Override
	protected void set_result_describe() {
		int l;
		long type = 0;
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			String des = "";
			if (has_rule(GameConstants_HaiNan.GAME_RULE_HAINAN_SHANG_GA)) {
				int ga = _player_result.pao[i];
				if (ga > 0) {
					des = "噶" + ga;
				} else {
					des = "无噶";
				}
			}
			l = GRR._chi_hu_rights[i].type_count;
			boolean isPengHU = true;
			boolean isSanDaoPai = true;
			for (int j = 0; j < l; j++) {
				type = GRR._chi_hu_rights[i].type_list[j];
				if (GRR._chi_hu_rights[i].is_valid()) {
					if (type == GameConstants.CHR_ZI_MO) {
						des += " 自摸Ｘ2";
					}
					if (type == GameConstants.CHR_HUNAN_DI_HU) {
						des += " 地胡Ｘ3";
						isPengHU = false;
					}
					if (type == GameConstants.CHR_HUNAN_TIAN_HU) {
						des += " 天胡Ｘ3";
						isPengHU = false;
					}

					if (type == GameConstants.CHR_SHU_FAN) {
						des += " 接炮Ｘ1";
						isPengHU = false;
					}
					if (type == GameConstants.CHR_HUNAN_QIANG_GANG_HU) {
						des += " 抢杠胡Ｘ1";
						isPengHU = false;
					}

					if (has_rule(GameConstants_HaiNan.GAME_RULE_HAINAN_HAI_DI_BAO_PAI)
							&& type == GameConstants.CHR_HUNAN_HAI_DI_PAO && GRR._chi_hu_rights[i].can_bao_si_peng) {
						des += " 海底包牌Ｘ1";
					}

					if (type == GameConstants.CHR_HUNAN_GANG_KAI) {
						des += " 杠开Ｘ3";
						isPengHU = false;
					}
					if (type == GameConstants.CHR_HUNAN_QING_YI_SE) {
						des += " 清一色Ｘ２";
						isPengHU = false;
					}
					if (type == GameConstants_HaiNan.CHR_MJ_HAI_NAN_HU_BU_HUA) {
						des += " 花上添花Ｘ3";
						isPengHU = false;
					}
					if (type == GameConstants.CHR_HUNAN_SHI_SAN_YAO) {
						des += " 十三幺Ｘ13";
						isPengHU = false;
					}

					if (type == GameConstants.CHR_HUNAN_PENGPENG_HU) {
						des += " 碰碰胡Ｘ２";
						isPengHU = false;
					}

					if (type == GameConstants.CHR_HUNAN_HAOHUA_QI_XIAO_DUI) {
						des += " 豪华七小对Ｘ3";
						isPengHU = false;
					}

					if (type == GameConstants.CHR_HUNAN_QI_XIAO_DUI) {
						des += " 七小对Ｘ2";
						isPengHU = false;
					}

					if (sanDaoPai[i] == 3 && isSanDaoPai) {
						des += " 三道牌";
						isSanDaoPai = false;
					} else if (sanDaoPai[i] == 4 && isSanDaoPai) {
						des += " 四道牌";
						isSanDaoPai = false;
					}
					if(isJieHu[i]){
						des += " 截胡";
						isJieHu[i] = false;
					}
				} else {
					if (type == GameConstants.CHR_FANG_PAO) {
						des += " 放炮";
					}
				}
			}

			if (isPengHU && GRR._chi_hu_rights[i].is_valid()) {
				des += " 平胡Ｘ１";
			}
			if (GRR._banker_player == i && genZhuangCount > 0) {
				des += " 被跟X" + genZhuangCount;
			}
			if (has_rule(GameConstants_HaiNan.GAME_RULE_HAINAN_LIAN_ZHUANG) && _cur_banker == i
					&& lian_zhuang_count > 1) {
				des += " 连庄X" + (lian_zhuang_count - 1);
			}
			if (has_rule(GameConstants_HaiNan.GAME_RULE_HAINAN_HUA_HU)) {
				if (GRR._chi_hu_rights[i].hua_count >= 7) {
					des += " 花胡(" + GRR._chi_hu_rights[i].hua_count + ")";
				}
			}

			int jie_gang = 0, fang_gang = 0, ming_gang = 0, an_gang = 0;
			if (GRR != null) {
				for (int p = 0; p < GameConstants.GAME_PLAYER; p++) {
					for (int w = 0; w < GRR._weave_count[p]; w++) {
						if (GRR._weave_items[p][w].weave_kind != GameConstants.WIK_GANG) {
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

	@Override
	public boolean trustee_timer(int operate_id, int seat_index) {
		return false;
	}

	public int get_ting_card(int[] cards, int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, boolean dai_feng,
			int seat_index) {
		PerformanceTimer timer = new PerformanceTimer();

		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		ChiHuRight chr = new ChiHuRight();

		int count = 0;
		int cbCurrentCard;

		int l = GameConstants.MAX_ZI_FENG;

		for (int i = 0; i < l; i++) {
			if (this._logic.is_magic_index(i))
				continue;
			cbCurrentCard = _logic.switch_to_card_data(i);
			chr.set_empty();
			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card(cbCardIndexTemp, weaveItem, cbWeaveCount, cbCurrentCard,
					chr, GameConstants.HU_CARD_TYPE_ZIMO, seat_index)) {
				cards[count] = cbCurrentCard;
				count++;
			}
		}

		if (count == 0) {
		} else if (count > 0 && count < l) {
		} else if (count == l) {
			count = 1;
			cards[0] = -1;
		}

		if (timer.get() > 50) {
			logger.warn("cost time too long " + Arrays.toString(cards_index) + ", cost time = " + timer.duration());
		}

		return count;
	}

	/**
	 * 玩家动作--通知玩家弹出/关闭操作
	 * 
	 * @param seat_index
	 * @param close
	 * @return
	 */
	public boolean operate_player_action(int seat_index, boolean close, boolean isNotWait) {
		PlayerStatus curPlayerStatus = _playerStatus[seat_index];

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_PLAYER_ACTION);
		roomResponse.setTarget(seat_index);
		roomResponse.setIsGoldRoom(isNotWait);// 暂时用金币场这个字段
		this.load_common_status(roomResponse);

		if (close == true) {
			GRR.add_room_response(roomResponse);
			// 通知玩家关闭
			this.send_response_to_player(seat_index, roomResponse);
			return true;
		}
		for (int i = 0; i < curPlayerStatus._action_count; i++) {
			roomResponse.addActions(curPlayerStatus._action[i]);
		}
		// 组合数据
		for (int i = 0; i < curPlayerStatus._weave_count; i++) {
			WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
			weaveItem_item.setCenterCard(curPlayerStatus._action_weaves[i].center_card);
			weaveItem_item.setProvidePlayer(curPlayerStatus._action_weaves[i].provide_player);
			weaveItem_item.setPublicCard(curPlayerStatus._action_weaves[i].public_card);
			weaveItem_item.setWeaveKind(curPlayerStatus._action_weaves[i].weave_kind);
			roomResponse.addWeaveItems(weaveItem_item);
		}
		GRR.add_room_response(roomResponse);
		this.send_response_to_player(seat_index, roomResponse);
		return true;
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

		PlayerStatus playerStatus = null;

		int action = GameConstants.WIK_NULL;

		// 动作判断
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			// 用户过滤
			if (seat_index == i)
				continue;

			playerStatus = _playerStatus[i];

			//// 碰牌判断
			action = _logic.check_peng(GRR._cards_index[i], card);
			if (action != 0 && _playerStatus[i].card != card) {
				playerStatus.add_action(action);
				playerStatus.add_peng(card, seat_index);
				bAroseAction = true;
			}

			if (GRR._left_card_count > 0) {
				// 杠牌判断
				action = _logic.estimate_gang_card_out_card(GRR._cards_index[i], card);
				if (action != 0) {
					playerStatus.add_action(GameConstants.WIK_GANG);
					playerStatus.add_gang(card, seat_index, 1);// 加上杠
					bAroseAction = true;
				}
			}

			// 可以胡的情况 判断
			if (!has_rule(GameConstants_HaiNan.GAME_RULE_HAINAN_BU_KE_DIAN_PAO) && _playerStatus[i].is_chi_hu_round()) {
				// 吃胡判断
				ChiHuRight chr = GRR._chi_hu_rights[i];// playerStatus._chiHuRight;
				chr.set_empty();
				int cbWeaveCount = GRR._weave_count[i];
				action = analyse_chi_hu_card(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr,
						GameConstants.HU_CARD_TYPE_PAOHU, i);

				// 结果判断
				if (action != 0) {
					_playerStatus[i].add_action(GameConstants.WIK_CHI_HU);
					_playerStatus[i].add_chi_hu(card, seat_index);// 吃胡的组合
					bAroseAction = true;
				}
			}
		}

		// 吃牌
		if (!has_rule(GameConstants_HaiNan.GAME_RULE_HAINAN_BU_KE_CHI)) {
			int chi_seat_index = (seat_index + 1) % getTablePlayerNumber();
			action = _logic.check_chi(GRR._cards_index[chi_seat_index], card);
			if ((action & GameConstants.WIK_LEFT) != 0) {
				_playerStatus[chi_seat_index].add_action(GameConstants.WIK_LEFT);
				_playerStatus[chi_seat_index].add_chi(card, GameConstants.WIK_LEFT, seat_index);
			}
			if ((action & GameConstants.WIK_CENTER) != 0) {
				_playerStatus[chi_seat_index].add_action(GameConstants.WIK_CENTER);
				_playerStatus[chi_seat_index].add_chi(card, GameConstants.WIK_CENTER, seat_index);
			}
			if ((action & GameConstants.WIK_RIGHT) != 0) {
				_playerStatus[chi_seat_index].add_action(GameConstants.WIK_RIGHT);
				_playerStatus[chi_seat_index].add_chi(card, GameConstants.WIK_RIGHT, seat_index);
			}

			// 结果判断
			if (_playerStatus[chi_seat_index].has_action()) {
				bAroseAction = true;
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

	/**
	 * 
	 * @return
	 */
	public boolean estimate_gang_respond(int seat_index, int card) {
		// 变量定义
		boolean bAroseAction = false;// 出现(是否)有

		PlayerStatus playerStatus = null;

		int action = GameConstants.WIK_NULL;

		// 动作判断
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			// 用户过滤
			if (seat_index == i)
				continue;

			playerStatus = _playerStatus[i];

			if (playerStatus.isAbandoned()) // 已经弃胡
				continue;

			// 可以胡的情况 判断
			if (playerStatus.is_chi_hu_round()) {
				// 吃胡判断
				ChiHuRight chr = GRR._chi_hu_rights[i];
				chr.set_empty();
				int cbWeaveCount = GRR._weave_count[i];
				action = analyse_chi_hu_card(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr,
						GameConstants.HU_CARD_TYPE_QIANGGANG, i);

				// 结果判断
				if (action != 0) {
					_playerStatus[i].add_action(GameConstants.WIK_CHI_HU);
					_playerStatus[i].add_chi_hu(card, seat_index);
					bAroseAction = true;
				}
			}
		}

		if (bAroseAction == true) {
			_provide_player = seat_index;// 谁打的牌
			_provide_card = card;// 打的什么牌
			_resume_player = _current_player;// 保存当前轮到的玩家
			_current_player = GameConstants.INVALID_SEAT;// 有需要操作的玩家。当前玩家为空
		}

		return bAroseAction;
	}

	/**
	 * 抓鸟 1 5 9
	 * 
	 * @param cards_data
	 * @param card_num
	 * @return
	 */
	public int get_pick_niao_count(int cards_data[], int card_num) {
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

	/**
	 * 获取玩家的令牌
	 * 
	 * @return
	 */
	public int getLingPaiIndex(int seat_index) {
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			int index = (don_ling_index + i + getTablePlayerNumber()) % getTablePlayerNumber();
			if (index == seat_index) {
				int rt_index = -1;
				switch (i) {
				case 0:
					rt_index = _logic.switch_to_card_index(0x31);
					break;
				case 1:
					//二人场番花对位
					if(has_rule(GameConstants_HaiNan.GAME_RULE_HAINAN_PLAYER_2)){
						rt_index = _logic.switch_to_card_index(0x33);
					}else{
						rt_index = _logic.switch_to_card_index(0x32);
					}
					break;
				case 2:
					rt_index = _logic.switch_to_card_index(0x33);
					break;
				case 3:
					rt_index = _logic.switch_to_card_index(0x34);
					break;
				}
				return rt_index;
			}
		}
		return -1;
	}
	
	
	/**
	 * 获取玩家的令牌
	 * 
	 * @return
	 */
	public int getFanHua(int seat_index) {
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			int index = (_cur_banker + i + getTablePlayerNumber()) % getTablePlayerNumber();
			if (index == seat_index) {
				int rt_index = -1;
				switch (i) {
				case 0:
					rt_index = _logic.switch_to_card_index(0x31);
					break;
				case 1:
					//二人场番花对位
					if(has_rule(GameConstants_HaiNan.GAME_RULE_HAINAN_PLAYER_2)){
						rt_index = _logic.switch_to_card_index(0x33);
					}else{
						rt_index = _logic.switch_to_card_index(0x32);
					}
					break;
				case 2:
					rt_index = _logic.switch_to_card_index(0x33);
					break;
				case 3:
					rt_index = _logic.switch_to_card_index(0x34);
					break;
				}
				return rt_index;
			}
		}
		return -1;
	}

	/**
	 * 获取番花对位判断
	 * 
	 * @return
	 */
	public boolean getFanHuaPaiIndex(int seat_index, int card) {
		int card1 = GameConstants.INVALID_CARD;
		int card2 = GameConstants.INVALID_CARD;
		if (card == 0x31) {
			card1 = 0x38;
			card2 = 0x43;
		} else if (card == 0x32) {
			card1 = 0x39;
			card2 = 0x44;
		} else if (card == 0x33) {
			card1 = 0x41;
			card2 = 0x45;
		} else if (card == 0x34) {
			card1 = 0x42;
			card2 = 0x46;
		}
		if (card1 != GameConstants.INVALID_CARD && card2 != GameConstants.INVALID_CARD) {
			for (int j = 0; j < 55; j++) {
				int card3 = GRR._discard_cards[seat_index][j];
				if (card1 == card3 || card2 == card3) {
					return true;
				}
			}
		}
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
		if(reason == GameConstants.Game_End_RELEASE_PLAY
				|| reason == GameConstants.Game_End_RELEASE_NO_BEGIN || reason == GameConstants.Game_End_RELEASE_RESULT
				|| reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_SYSTEM){
			if(_game_status != GameConstants.GS_MJ_PLAY){
				Arrays.fill(_player_result.pao, 0);
			}
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_GAME_END); // 宁都麻将结算类型
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
			game_end.setRoundOverType(1);
			game_end.setStartTime(GRR._start_time);

			game_end.setGameTypeIndex(GRR._game_type_index);
			roomResponse.setLeftCardCount(GRR._left_card_count);

			for (int i = 0; i < GRR._especial_card_count; i++) {
				game_end.addEspecialShowCards(GRR._especial_show_cards[i]);
			}

			GRR._end_type = reason;

			// int hupaiscore[] = new int[getTablePlayerNumber()];// 胡牌分
			int totalScore[] = new int[getTablePlayerNumber()];// 总分=胡牌分+杠分+跟庄

			// 杠牌，每个人的分数
			float lGangScore[] = new float[getTablePlayerNumber()];
			int mingGangScore[] = new int[getTablePlayerNumber()]; // 明杆得分
			int zhiGangScore[] = new int[getTablePlayerNumber()]; // 直杆得分
			int anGangScore[] = new int[getTablePlayerNumber()]; // 暗杠得分

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				// 流局算分
				if (reason != GameConstants.Game_End_DRAW
						|| has_rule(GameConstants_HaiNan.GAME_RULE_HAINAN_LIU_JU_SHUAN_FEN)) {
//					for (int j = 0; j < GRR._weave_count[i]; j++) {
//						if (GRR._weave_items[i][j].weave_kind == GameConstants.WIK_GANG) {
//							if (GRR._weave_items[i][j].type == GameConstants.GANG_TYPE_JIE_GANG) {
//								int zhuangXian = calcuZhuangXian(GRR._weave_items[i][j].provide_player, i);
//								int score = 0;
//
//								if (has_rule(GameConstants_HaiNan.GAME_RULE_HAINAN_FANG_GOU_JIAO)) {
//									for (int u = 0; u < getTablePlayerNumber(); u++) {
//										if (u == i) {
//											continue;
//										}
//										int zhuangXian1 = calcuZhuangXian(u, i);
//										score += GRR._weave_items[i][j].weave_score * (getGaCount(i, u) + zhuangXian1);
//									}
//								} else {
//									score = GRR._weave_items[i][j].weave_score
//											* (getGaCount(i, GRR._weave_items[i][j].provide_player) + zhuangXian);
//								}
//								zhiGangScore[GRR._weave_items[i][j].provide_player] -= score;
//								zhiGangScore[i] += score;
//							} else {
//								for (int k = 0; k < getTablePlayerNumber(); k++) {
//									int zhuangXian = calcuZhuangXian(k, i);
//									int score = GRR._weave_items[i][j].weave_score * (getGaCount(i, k) + zhuangXian);
//									if (k == i) {
//										continue;
//									}
//									if (GRR._weave_items[i][j].type == GameConstants.GANG_TYPE_AN_GANG) {
//										anGangScore[k] -= score;
//										anGangScore[i] += score;
//									} else {
//										mingGangScore[k] -= score;
//										mingGangScore[i] += score;
//									}
//								}
//							}
//						}
//					}
					//包杠分
//					boolean other =false;
//					int jie_gang_index = i;
//					if (has_rule(GameConstants_HaiNan.GAME_RULE_HAINAN_FANG_GOU_JIAO)) {
//						for (int j = 0; j < GRR._weave_count[i]; j++) {
//							if (GRR._weave_items[i][j].weave_kind == GameConstants.WIK_GANG) {
//								if (GRR._weave_items[i][j].type == GameConstants.GANG_TYPE_JIE_GANG) {
//									//other = true;
//									jie_gang_index = GRR._weave_items[i][j].provide_player;
//								}
//							}
//						}
//					}
					int fan = getGanFan(i);
//					if (other) {
//						int score = 0;
//						for (int u = 0; u < getTablePlayerNumber(); u++) {
//							if (u == i) {
//								continue;
//							}
//							int zhuangXian1 = calcuZhuangXian(u, i);
//							score += fan * (getGaCount(i, u) + zhuangXian1);
//						}
//						zhiGangScore[jie_gang_index] -= score;
//						zhiGangScore[i] += score;
//					}else{
						for (int j = 0; j < GRR._weave_count[i]; j++) {
							if (GRR._weave_items[i][j].weave_kind == GameConstants.WIK_GANG) {
								if (GRR._weave_items[i][j].type == GameConstants.GANG_TYPE_JIE_GANG) {
									//int zhuangXian = calcuZhuangXian(GRR._weave_items[i][j].provide_player, i);
									int score = 0;
	
									if (has_rule(GameConstants_HaiNan.GAME_RULE_HAINAN_FANG_GOU_JIAO) ||GRR._weave_items[i][j].is_lao_gang ) {
										for (int u = 0; u < getTablePlayerNumber(); u++) {
											if (u == i) {
												continue;
											}
											int zhuangXian1 = calcuZhuangXian(u, i);
											if ((i == GRR._banker_player || u ==GRR._banker_player ) &&lian_zhuang_count > 1 && has_rule(GameConstants_HaiNan.GAME_RULE_HAINAN_LIAN_ZHUANG)) {
												zhuangXian1 += (lian_zhuang_count - 1);
											}
											score += GRR._weave_items[i][j].weave_score * (getGaCount(i, u) + zhuangXian1);
										}
										zhiGangScore[GRR._weave_items[i][j].provide_player] -= score;
										zhiGangScore[i] += score;
									} else {
										for (int k = 0; k < getTablePlayerNumber(); k++) {
											int index = k;
										
											if (index == i) {
												continue;
											}
											int zhuangXian2= calcuZhuangXian(index, i);
											if ((i == GRR._banker_player || k ==GRR._banker_player ) &&lian_zhuang_count > 1 && has_rule(GameConstants_HaiNan.GAME_RULE_HAINAN_LIAN_ZHUANG)) {
												zhuangXian2 += (lian_zhuang_count - 1);
											}
											int score2 = GRR._weave_items[i][j].weave_score * (getGaCount(i, index) + zhuangXian2);
											zhiGangScore[index] -= score2;
											zhiGangScore[i] += score2;
										}
									}
								} 
								else {
//									if(hu_index == i){
//										continue;
//									}
									for (int k = 0; k < getTablePlayerNumber(); k++) {
										int index = k;
										if(hu_index != GameConstants.INVALID_SEAT){
											//index = hu_index;
										}
										int zhuangXian = calcuZhuangXian(index, i);
										if ((i == GRR._banker_player || k ==GRR._banker_player ) &&lian_zhuang_count > 1 && has_rule(GameConstants_HaiNan.GAME_RULE_HAINAN_LIAN_ZHUANG)) {
											zhuangXian += (lian_zhuang_count - 1);
										}
										int score = GRR._weave_items[i][j].weave_score * (getGaCount(i, index) + zhuangXian);
										if (index == i) {
											continue;
										}
										if (GRR._weave_items[i][j].type == GameConstants.GANG_TYPE_AN_GANG) {
											anGangScore[index] -= score;
											anGangScore[i] += score;
										} else {
											mingGangScore[index] -= score;
											mingGangScore[i] += score;
										}
									}
								}
							}
						}
//					}
					
					// 花胡
					if (has_rule(GameConstants_HaiNan.GAME_RULE_HAINAN_HUA_HU) ) {
						for (int k = 0; k < getTablePlayerNumber(); k++) {
							if (k == i) {
								continue;
							}
							int zhuangXian = calcuZhuangXian(i, k);
							int bei = getMaxFan() - fan >= 0? getMaxFan() - fan:0;
							if ((i == GRR._banker_player || k ==GRR._banker_player ) &&lian_zhuang_count > 1 && has_rule(GameConstants_HaiNan.GAME_RULE_HAINAN_LIAN_ZHUANG)) {
								zhuangXian += (lian_zhuang_count - 1);
							}
							
							if (GRR._chi_hu_rights[i].hua_count == 7) {
								if(bei>0){
									bei =1;
								}
								int score =  (getGaCount(i, k) + zhuangXian)*bei;
								mingGangScore[k] -= score;
								mingGangScore[i] += score;
							} else if (GRR._chi_hu_rights[i].hua_count == 8) {
								if(bei>0){
									bei =2;
								}
								int score =  (getGaCount(i, k) + zhuangXian)*bei;
								anGangScore[k] -= score;
								anGangScore[i] += score;
							}
						}
						// 记录
						if (GRR._chi_hu_rights[i].hua_count == 7) {
							_player_result.ming_gang_count[i]++;
						} else if (GRR._chi_hu_rights[i].hua_count == 8) {
							_player_result.an_gang_count[i]++;
						}
					}

				}

				// 记录
				for (int j = 0; j < getTablePlayerNumber(); j++) {
					_player_result.lost_fan_shu[i][j] += GRR._lost_fan_shu[i][j];
				}

			}
			// 杠分计算
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				lGangScore[i] = mingGangScore[i] + anGangScore[i] + zhiGangScore[i];
			}

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				GRR._game_score[i] += lGangScore[i];
				GRR._game_score[i] += GRR._start_hu_score[i]; // 起手胡分数

				if (has_rule(GameConstants_HaiNan.GAME_RULE_HAINAN_LIU_JU_SHUAN_FEN) || reason != GameConstants.Game_End_DRAW) {

					if (i != shang_ju_zhuang) {

						int zhuangXian = calcuZhuangXian(shang_ju_zhuang, i);
						// 跟庄分数
						int genScore = genZhuangCount * (getGaCount(shang_ju_zhuang, i) + zhuangXian);
						GRR._game_score[i] += genScore;
						GRR._game_score[shang_ju_zhuang] -= genScore;
					}
				}
			}

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				totalScore[i] = (int) GRR._game_score[i];
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
			// 花牌显示
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				Int32ArrayResponse.Builder pnc = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GRR._discard_count[i]; j++) {
					if (GRR._discard_cards[i][j] < 0x38) {
						continue;
					}
					pnc.addItem(GRR._discard_cards[i][j]);
				}
				// for (int j = 0; j < GRR._player_niao_count_fei[i]; j++) {
				// pnc.addItem(GRR._player_niao_cards_fei[i][j]);
				// }
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

			shang_ju_zhuang = _cur_banker;
			genZhuangCount = 0;
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
		roomResponse.setGameEnd(game_end);
		this.send_response_to_room(roomResponse);
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
	
	
	public int getGanFan(int i){
		int fan = 0;
		for (int j = 0; j < GRR._weave_count[i]; j++) {
			if (GRR._weave_items[i][j].weave_kind == GameConstants.WIK_GANG) {
				fan += GRR._weave_items[i][j].weave_score;
			}
		}
		// 花胡
		if (has_rule(GameConstants_HaiNan.GAME_RULE_HAINAN_HUA_HU)) {
			for (int k = 0; k < getTablePlayerNumber(); k++) {
				if (k == i) {
					continue;
				}
				if (GRR._chi_hu_rights[i].hua_count == 7) {
					fan+=1;
				} else if (GRR._chi_hu_rights[i].hua_count == 8) {
					fan+=2;
				}
			}
		}

		return fan;
	}

	public void changerLiangZhuang() {
		if (shang_ju_zhuang == _cur_banker) {
			lian_zhuang_count++;
			//don_ling_index = (_cur_banker  + getTablePlayerNumber()-lian_zhuang_count) % getTablePlayerNumber();
		} else {
			don_ling_index = _cur_banker;
			lian_zhuang_count = 0;
		}
	}

	/**
	 * 计算庄闲
	 * 
	 * @param seat_index
	 * @param provide_index
	 * @return
	 */
	public int calcuZhuangXian(int seat_index, int provide_index) {
		int zhuangXian = 0;
		if (has_rule(GameConstants_HaiNan.GAME_RULE_HAINAN_ZHUANG_XIAN)) {
			if (seat_index == GRR._banker_player || provide_index == GRR._banker_player) {
				zhuangXian = 1;
			}
		}
		return zhuangXian;
	}

	@Override
	public boolean handler_player_ready(int seat_index, boolean is_cancel) {

		if (this.get_players()[seat_index] == null) {
			return false;
		}
		if (GameConstants.GS_MJ_XIAOHU != _game_status && GameConstants.GS_MJ_FREE != _game_status
				&& GameConstants.GS_MJ_WAIT != _game_status) {
			return false;
		}

		if (is_cancel) {//
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

		_player_ready[seat_index] = 1;

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

		handler_game_start();

		this.refresh_room_redis_data(GameConstants.PROXY_ROOM_UPDATE, nt);

		return true;
	}

	/**
	 * //执行发牌 是否延迟
	 * 
	 * @param seat_index
	 * @param delay
	 * @return
	 */
	public boolean exe_dispatch_card(int seat_index, int type, int delay, int card_num) {

		if (delay > 0) {
			GameSchedule.put(new DispatchCardHuaRunnable(this.getRoom_id(), seat_index, type, false, card_num), delay,
					TimeUnit.MILLISECONDS);
		} else {
			// 发牌
			this.set_handler(this._handler_dispath_card);
			this._handler_dispath_card.reset_card_count_hua(seat_index, type, card_num);
			this._handler.exe(this);
		}

		return true;
	}

	// 摸起来的牌 派发牌
	public boolean dispatch_card_data(int cur_player, int type, boolean tail, int card_num) {
		// 牌局未开 或者等待状态 调度不需要执行 add by zain 2017/6/1
		if ((_game_status == GameConstants.GS_MJ_FREE || _game_status == GameConstants.GS_MJ_WAIT) && is_sys())
			return false;
		// add end 发牌
		this.set_handler(this._handler_dispath_card);
		this._handler_dispath_card.reset_card_count_hua(cur_player, type, card_num);
		this._handler.exe(this);

		return true;
	}

	public int getWeaveItemCount(int seat_index,int num) {
		int count = 0;
	
		WeaveItem[] items = GRR.getWeaveItemsForOut(seat_index, new WeaveItem[GameConstants.MAX_WEAVE]);
		int items_count = GRR._weave_count[seat_index];
		if (items_count < 3) {
			return count;
		}
		for (int i = 0; i < GRR._weave_count[seat_index]; i++) {
			int index = items[i].provide_player;
			if (index >= 1000) {
				index -= 1000;
			}
			if (index == seat_index) {
				continue;
			}

			int count1 = 1;
			for (int j = 0; j < GRR._weave_count[seat_index]; j++) {
				int index1 = items[j].provide_player;
				if (index1 >= 1000) {
					index1 -= 1000;
				}
				if (index1 == seat_index || i == j) {
					continue;
				}
				if (index == index1) {
					count1++;
				}
			}
			if (count1 > count) {
				count = count1;
			}
			
		}
		if (count >= 3) {
			if(sanDaoPai[seat_index] >= count && count != num){
				return GameConstants.INVALID_SEAT;
			}
			sanDaoPai[seat_index] = count;
		}
		return count;
	}

	@Override
	public void test_cards() {
		int[] cards_of_player0 = new int[] { 0x22,0x23,0x24,0x26,0x27,0x16,0x16,0x11,0x12,0x32,0x32,0x32,0x39 };
		int[] cards_of_player1 = new int[] { 0x22,0x23,0x24,0x26,0x27,0x16,0x16,0x11,0x12,0x32,0x32,0x32,0x39  };
		int[] cards_of_player3 = new int[] {0x22,0x23,0x24,0x26,0x27,0x16,0x16,0x11,0x12,0x32,0x32,0x32,0x39 };
		int[] cards_of_player2 = new int[] { 0x22,0x23,0x24,0x26,0x27,0x16,0x16,0x11,0x12,0x32,0x32,0x32,0x39};

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
				GRR._cards_index[1][_logic.switch_to_card_index(cards_of_player0[j])] += 1;
				GRR._cards_index[2][_logic.switch_to_card_index(cards_of_player0[j])] += 1;
			} else if (this.getTablePlayerNumber() == 2) {
				GRR._cards_index[0][_logic.switch_to_card_index(cards_of_player0[j])] += 1;
				GRR._cards_index[1][_logic.switch_to_card_index(cards_of_player1[j])] += 1;
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
	public boolean operate_player_cards(int seat_index, int card_count, int cards[], int weave_count,
			WeaveItem weaveitems[]) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYER_CARDS);// 12更新玩家手牌数据
		roomResponse.setGameStatus(_game_status);
		roomResponse.setTarget(seat_index);
		roomResponse.setCardType(1);

		this.load_common_status(roomResponse);

		// 手牌数量
		roomResponse.setCardCount(card_count);
		roomResponse.setWeaveCount(weave_count);
		// 组合牌
		if (weave_count > 0) {
			for (int j = 0; j < weave_count; j++) {
				WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
				weaveItem_item.setProvidePlayer(weaveitems[j].provide_player);
				weaveItem_item.setPublicCard(weaveitems[j].public_card);
				weaveItem_item.setWeaveKind(weaveitems[j].weave_kind);
				weaveItem_item.setCenterCard(weaveitems[j].center_card);
				roomResponse.addWeaveItems(weaveItem_item);
			}
		}

		this.send_response_to_other(seat_index, roomResponse);

		// 手牌--将自己的手牌数据发给自己
		for (int j = 0; j < card_count; j++) {
			roomResponse.addCardData(cards[j]);
		}
		GRR.add_room_response(roomResponse);
		// 自己才有牌数据
		this.send_response_to_player(seat_index, roomResponse);

		return true;
	}

	@Override
	public boolean handler_requst_pao_qiang(Player player, int pao, int qiang) {
		return ga_HaiNan.handler_pao_qiang(this, player.get_seat_index(), pao, qiang);

	}

}
