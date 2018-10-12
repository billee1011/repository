package com.cai.game.mj.jiangxi.yudu;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.Player;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.SysParamModel;
import com.cai.common.domain.WeaveItem;
import com.cai.common.util.GameDescUtil;
import com.cai.common.util.PBUtil;
import com.cai.common.util.PerformanceTimer;
import com.cai.dictionary.SysParamDict;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.game.mj.AbstractMJTable;
import com.cai.game.mj.MJType;
import com.cai.game.util.AnalyseCardUtil;
import com.cai.game.util.GameUtilConstants;
import com.cai.service.PlayerServiceImpl;

import protobuf.clazz.Protocol.GameEndResponse;
import protobuf.clazz.Protocol.GameStartResponse;
import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomInfo;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;
import protobuf.clazz.gzcg.GZCGRsp.EveryJingInfo_GZCG;
import protobuf.clazz.gzcg.GZCGRsp.GameEndResponse_GZCG;

public class MJTable_YD extends AbstractMJTable {

	protected int[] dispatchcardNum; // // 庄家摸牌次数 如果庄家摸牌次数==1说明是第一圈
	protected boolean ifCanGenZhuang = true; // 是否能跟庄（三人时不跟庄）
	protected boolean ifGenZhuang = true; // 是否跟了庄（第一圈打完时确定）
	private int genZhuangCard; // 跟庄牌(庄家打出的第一张牌)
	protected int baoPai;// 宝牌牌值
	protected int baseScore;// 房间底分(1/2/5)
	protected int baoPaiCount[] = new int[getTablePlayerNumber()]; // 上宝数量
	protected int baoPaiScore[] = new int[getTablePlayerNumber()]; // 上宝分（打出一张5
																	// 2张15 3张45
																	// 4张135
																	// 4张流局时也算30
																	// 其他不算）
	protected int gangBaoScore[] = new int[getTablePlayerNumber()];
	private int shang_ju_zhuang;
	private int next_seat_index;

	public MJHandlerBao_YD handlerBao_YD; // 选宝
	protected MJHandlerYaoHaiDi_YD _handler_yao_hai_di_yd;

	/**
	 * 跟庄操作
	 * 
	 * @param seat_index
	 * @param card
	 * @param isZhuang
	 */
	public void addGenZhuangCard(int seat_index, int card, boolean isZhuang) {
		if (dispatchcardNum[seat_index] != 1 || dispatchcardNum[_cur_banker] != 1) {
			return;
		}
		if (isZhuang) {
			genZhuangCard = card;
			next_seat_index = (seat_index + getTablePlayerNumber() + 1) % getTablePlayerNumber();
		} else {
			if (genZhuangCard != card || seat_index != next_seat_index) {
				ifGenZhuang = false;
			} else {
				next_seat_index = (seat_index + getTablePlayerNumber() + 1) % getTablePlayerNumber();
			}
		}
		/*
		 * if(ifGenZhuang==false){ System.out.println("!!!!!!!!!"); }
		 */
	}

	/**
	 * 跟庄状态
	 * 
	 * @return the ifCanGenZhuang
	 */
	public boolean ifCanGenZhuang() {
		return ifCanGenZhuang;
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
	/*
	 * public boolean isDispatchcardNum(int seat_index) { return
	 * dispatchcardNum[seat_index] <= 1; }
	 */

	/**
	* 
	*/
	public MJTable_YD() {
		super(MJType.GAME_TYPE_JX_YUDU);
	}

	private static final long serialVersionUID = 1L;

	@Override
	public int getTablePlayerNumber() {
		if (has_rule(GameConstants_YD.GAME_RULE_PLAYER_3)) {
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
		_handler_dispath_card = new MJHandlerDispatchCard_YD();
		_handler_out_card_operate = new MJHandlerOutCardOperate_YD();
		_handler_gang = new MJHandlerGang_YD();
		_handler_chi_peng = new MJHandlerChiPeng_YD();
		handlerBao_YD = new MJHandlerBao_YD();
		_handler_yao_hai_di_yd = new MJHandlerYaoHaiDi_YD();
	}

	/**
	 * 参数初始化
	 */
	protected void onInitParam() {
		dispatchcardNum = new int[GameConstants.GAME_PLAYER];
		genZhuangCard = 0x00;
		baoPai = -1;
		// 确定房间底分
		baseScore = 1;
		if (has_rule(GameConstants_YD.GAME_RULE_BASE2)) {
			baseScore = 2;
		}
		if (has_rule(GameConstants_YD.GAME_RULE_BASE5)) {
			baseScore = 5;
		}
		ifCanGenZhuang = true;
		ifGenZhuang = true;
		baoPaiScore = new int[getTablePlayerNumber()];
		if (_cur_round == 1) {
			shang_ju_zhuang = 0;
		}
		this.baoPaiCount = new int[getTablePlayerNumber()];
		this.baoPaiScore = new int[getTablePlayerNumber()];
		this.gangBaoScore = new int[getTablePlayerNumber()];

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.cai.game.mj.AbstractMJTable#on_game_start()
	 */
	@Override
	protected boolean on_game_start() {
		onInitParam();
		_logic.clean_magic_cards();
		GameStartResponse.Builder gameStartResponse = GameStartResponse.newBuilder();
		// gameStartResponse.setSiceIndex(rand);
		gameStartResponse.setBankerPlayer(GRR._banker_player);
		gameStartResponse.setCurrentPlayer(_current_player);
		gameStartResponse.setLeftCardCount(GRR._left_card_count);

		int hand_cards[][] = new int[getTablePlayerNumber()][GameConstants.MAX_COUNT];
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

			roomResponse.setGameStart(gameStartResponse);
			roomResponse.setCurrentPlayer(_current_player == GameConstants.INVALID_SEAT ? _resume_player : _current_player);
			roomResponse.setLeftCardCount(GRR._left_card_count);
			roomResponse.setGameStatus(_game_status);
			roomResponse.setLeftCardCount(GRR._left_card_count);
			this.send_response_to_player(i, roomResponse);
		}
		// //////////////////////////////////////////////// 回放
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
		// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////

		// 检测听牌
		/*
		 * for (int i = 0; i < getTablePlayerNumber(); i++) {
		 * _playerStatus[i]._hu_card_count =
		 * this.get_ting_card(_playerStatus[i]._hu_cards, GRR._cards_index[i],
		 * GRR._weave_items[i], GRR._weave_count[i], true,
		 * i,GameConstants_YD.HU_CARD_TYPE_ZI_MO); if
		 * (_playerStatus[i]._hu_card_count > 0) { this.operate_chi_hu_cards(i,
		 * _playerStatus[i]._hu_card_count, _playerStatus[i]._hu_cards); } }
		 */

		// 此处需要判断庄家是否能天胡，代码暂时省略
		/*
		 * boolean is_tian_hu = false; if (is_tian_hu == false) {
		 * //exe_bao(this.GRR._banker_player);
		 * //this.exe_dispatch_card(_current_player, GameConstants.WIK_NULL,
		 * GameConstants.DELAY_SEND_CARD_DELAY); }
		 */
		exe_bao(this.GRR._banker_player);

		return false;
	}

	public boolean exe_bao(int seat_index) {
		// 出牌
		this.set_handler(this.handlerBao_YD);
		this.handlerBao_YD.reset_status(seat_index);
		this.handlerBao_YD.exe(this);

		return true;
	}

	/**
	 * 显示中间出的牌
	 * 
	 * @param seat_index
	 */
	public void runnable_remove_bao_middle_cards(int seat_index) {
		if ((_game_status == GameConstants.GS_MJ_FREE || _game_status == GameConstants.GS_MJ_WAIT) && is_sys()) {
			return;
		}

		// 去掉
		this.operate_show_card(seat_index, GameConstants.Show_Card_Center, 0, null, GameConstants.INVALID_SEAT);

		// 刷新有癞子的牌
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			boolean has_lai_zi = false;
			for (int j = 0; j < GameConstants.MAX_INDEX; j++) {
				if (GRR._cards_index[i][j] > 0 && _logic.switch_to_card_data(j) == this.baoPai) {
					has_lai_zi = true;
					break;
				}
			}
			if (has_lai_zi) {
				// 刷新自己手牌
				int cards[] = new int[GameConstants.MAX_COUNT];
				int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[i], cards);
				changeCard(cards);
				this.operate_player_cards(i, hand_card_count, cards, 0, null);
			}
		}
		// 检测听牌
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			this._playerStatus[i]._hu_card_count = this.get_ting_card(this._playerStatus[i]._hu_cards, this.GRR._cards_index[i],
					this.GRR._weave_items[i], this.GRR._weave_count[i], true, i, GameConstants_YD.HU_CARD_TYPE_ZI_MO);
			if (this._playerStatus[i]._hu_card_count > 0) {
				this.operate_chi_hu_cards(i, this._playerStatus[i]._hu_card_count, this._playerStatus[i]._hu_cards);
			}
		}

		this.exe_dispatch_card(this._current_player, GameConstants.WIK_NULL, GameConstants.DELAY_SEND_CARD_DELAY);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.cai.game.mj.AbstractMJTable#analyse_chi_hu_card(int[],
	 * com.cai.common.domain.WeaveItem[], int, int,
	 * com.cai.common.domain.ChiHuRight, int, int)
	 */
	@Override
	public int analyse_chi_hu_card(int[] cards_index, WeaveItem[] weaveItems, int weave_count, int cur_card, ChiHuRight chiHuRight, int card_type,
			int _seat_index) {

		boolean big_hu = false;
		// 炮胡并且其他人打出宝牌 这个时候需要对这张宝牌进行过滤
		boolean cut_laizi = false;
		boolean is_zimo = false;

		if (cur_card == 0)
			return GameConstants_YD.WIK_NULL;

		int[] magic_cards_index = new int[GameUtilConstants.MAX_MAGIC_INDEX_COUNT];
		int magic_card_count = _logic.get_magic_card_count();

		if (magic_card_count > 2) { // 一般只有两种癞子牌存在
			magic_card_count = 2;
		}

		if (cur_card == baoPai && card_type != GameConstants_YD.HU_CARD_TYPE_ZI_MO && card_type != GameConstants_YD.HU_CARD_TYPE_GANG_SHANG_KAI_HUA) {
			cut_laizi = true;
		}
		if (card_type == GameConstants_YD.HU_CARD_TYPE_ZI_MO || card_type == GameConstants_YD.HU_CARD_TYPE_GANG_SHANG_KAI_HUA) {
			is_zimo = true;
		}

		for (int i = 0; i < magic_card_count; i++) {
			magic_cards_index[i] = _logic.get_magic_card_index(i);
		}

		int cbChiHuKind = GameConstants_YD.WIK_NULL;

		// 构造扑克
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		// 插入扑克
		if (cur_card != GameConstants.INVALID_VALUE) {
			cbCardIndexTemp[_logic.switch_to_card_index(cur_card)]++;
		}

		// 七对
		if (!cut_laizi) {
			int is_qi_xiao_dui = is_qi_xiao_dui(cards_index, weaveItems, weave_count, cur_card, magic_cards_index, magic_card_count);
			if (GameConstants.WIK_NULL != is_qi_xiao_dui) {
				big_hu = true;
				chiHuRight.opr_or(GameConstants_YD.CHR_QI_DUI);
				cbChiHuKind = GameConstants_YD.WIK_CHI_HU;
			}
		} else {
			int is_qi_xiao_dui = is_qi_xiao_dui_cutlaizi(cards_index, weaveItems, weave_count, cur_card, magic_cards_index, magic_card_count);
			if (GameConstants.WIK_NULL != is_qi_xiao_dui) {
				big_hu = true;
				chiHuRight.opr_or(GameConstants_YD.CHR_QI_DUI);
				cbChiHuKind = GameConstants_YD.WIK_CHI_HU;
			}
		}

		if (!cut_laizi) {
			// 全求人
			boolean is_quan_qiu_ren = isQuanQiuRen(cards_index, weaveItems, weave_count, cur_card);
			if (is_quan_qiu_ren && card_type != GameConstants_YD.HU_CARD_TYPE_ZI_MO
					&& card_type != GameConstants_YD.HU_CARD_TYPE_GANG_SHANG_KAI_HUA) {
				big_hu = true;
				chiHuRight.opr_or(GameConstants_YD.CHR_QUAN_QIU_REN);
				cbChiHuKind = GameConstants_YD.WIK_CHI_HU;
			} else {
				// 大七对的胡牌规则和碰碰胡一样
				boolean only_gang = only_gang(weaveItems, weave_count);
				boolean is_da_qi_dui = AnalyseCardUtil.analyse_peng_hu_by_cards_index(cards_index, _logic.switch_to_card_index(cur_card),
						magic_cards_index, magic_card_count) && only_gang; // 不能吃不能碰
				if (is_da_qi_dui) {
					big_hu = true;
					chiHuRight.opr_or(GameConstants_YD.CHR_DA_QI_DUI);
					cbChiHuKind = GameConstants_YD.WIK_CHI_HU;
				}
			}
		} else {
			// 全求人
			boolean is_quan_qiu_ren = isQuanQiuRen_cutlaizi(cards_index, weaveItems, weave_count, cur_card);
			if (is_quan_qiu_ren && card_type != GameConstants_YD.HU_CARD_TYPE_ZI_MO
					&& card_type != GameConstants_YD.HU_CARD_TYPE_GANG_SHANG_KAI_HUA) {
				big_hu = true;
				chiHuRight.opr_or(GameConstants_YD.CHR_QUAN_QIU_REN);
				cbChiHuKind = GameConstants_YD.WIK_CHI_HU;
			} else {
				// 大七对的胡牌规则和碰碰胡一样
				boolean only_gang = only_gang(weaveItems, weave_count);
				boolean is_da_qi_dui = AnalyseCardUtil.analyse_peng_hu_by_cards_index_ydcutlaizi(cards_index, _logic.switch_to_card_index(cur_card),
						magic_cards_index, magic_card_count) && only_gang; // 不能吃不能碰
				if (is_da_qi_dui) {
					big_hu = true;
					chiHuRight.opr_or(GameConstants_YD.CHR_DA_QI_DUI);
					cbChiHuKind = GameConstants_YD.WIK_CHI_HU;
				}
			}
		}

		if (!cut_laizi) {
			// 七星十三烂
			boolean qxshisanlan = _logic.isQXShiSanLan(cbCardIndexTemp, weaveItems, weave_count);
			if (qxshisanlan) {
				big_hu = true;
				chiHuRight.opr_or(GameConstants_YD.CHR_QI_XING_SHI_SAN_LAN);
				cbChiHuKind = GameConstants_YD.WIK_CHI_HU;
			} else {
				// 十三烂
				boolean shisanlan = _logic.isShiSanLanYD(cbCardIndexTemp, weaveItems, weave_count);
				if (shisanlan) {
					big_hu = true;
					chiHuRight.opr_or(GameConstants_YD.CHR_SHI_SAN_LAN);
					cbChiHuKind = GameConstants_YD.WIK_CHI_HU;
				}
			}
		} else {
			// 七星十三烂
			boolean qxshisanlan = isQXShiSanLan_cutlaizi(cbCardIndexTemp, weaveItems, weave_count);
			if (qxshisanlan) {
				big_hu = true;
				chiHuRight.opr_or(GameConstants_YD.CHR_QI_XING_SHI_SAN_LAN);
				cbChiHuKind = GameConstants_YD.WIK_CHI_HU;
			} else {
				// 十三烂
				boolean shisanlan = isShiSanLanYD_cutlaizi(cbCardIndexTemp, weaveItems, weave_count);
				if (shisanlan) {
					big_hu = true;
					chiHuRight.opr_or(GameConstants_YD.CHR_SHI_SAN_LAN);
					cbChiHuKind = GameConstants_YD.WIK_CHI_HU;
				}
			}
		}

		// int[] magic_cards_index = new
		// int[GameUtilConstants.MAX_MAGIC_INDEX_COUNT];

		// 判断能否胡
		boolean can_win = AnalyseCardUtil.analyse_feng_chi_by_cards_index_yd(cards_index, _logic.switch_to_card_index(cur_card), magic_cards_index,
				magic_card_count, is_zimo);

		if (!can_win && !big_hu) {
			chiHuRight.set_empty();
			return GameConstants_YD.WIK_NULL;
		}

		if (card_type != GameConstants_YD.HU_CARD_TYPE_QIANG_GANG) {
			if (dispatchcardNum[_seat_index] <= 1) {
				if (_seat_index == _cur_banker) {
					if (card_type == GameConstants_YD.HU_CARD_TYPE_ZI_MO) {
						big_hu = true;
						chiHuRight.opr_or(GameConstants_YD.CHR_TIAN_HU);
					}
				}
			}
			if (weave_count == 0 && _out_card_count == 1 && _out_card_player == GRR._banker_player && _seat_index != GRR._banker_player
					&& card_type != GameConstants_YD.HU_CARD_TYPE_ZI_MO && card_type != GameConstants_YD.HU_CARD_TYPE_GANG_SHANG_KAI_HUA) {
				big_hu = true;
				chiHuRight.opr_or(GameConstants_YD.CHR_DI_HU);
			}
		}
		cbChiHuKind = GameConstants_YD.WIK_CHI_HU;
		if (card_type == GameConstants_YD.HU_CARD_TYPE_GANG_SHANG_KAI_HUA) {
			chiHuRight.opr_or(GameConstants_YD.CHR_GANG_SHANG_KAI_HUA);
		} else if (card_type == GameConstants_YD.HU_CARD_TYPE_ZI_MO) {
			chiHuRight.opr_or(GameConstants_YD.CHR_ZI_MO);
		} else if (card_type == GameConstants_YD.HU_CARD_TYPE_QIANG_GANG) {
			chiHuRight.opr_or(GameConstants_YD.CHR_QIANG_GANG);// 抢杠胡
		} else if (card_type == GameConstants_YD.HU_CARD_TYPE_GAME_GANG_SHANG_PAO) {
			chiHuRight.opr_or(GameConstants_YD.CHR_GANG_SHANG_PAO);// 杠上炮
		} else if (card_type == GameConstants_YD.HU_CARD_TYPE_JIE_PAO) {
			chiHuRight.opr_or(GameConstants_YD.CHR_JIE_PAO);// 炮胡
		}
		if (!big_hu) {
			chiHuRight.opr_or(GameConstants_YD.CHR_PING_HU);
		}
		return cbChiHuKind;

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.cai.game.mj.AbstractMJTable#process_chi_hu_player_score(int,int,
	 * int, boolean)
	 */
	@Override
	public void process_chi_hu_player_score(int seat_index, int provide_index, int operate_card, boolean zimo) {
		// 如果胡的那个人上宝了
		/*
		 * for(int i=0;i<getTablePlayerNumber();i++){
		 * if(baoPaiScore[seat_index]!=0){ if(i==seat_index){
		 * GRR._game_score[seat_index] += baoPaiScore[seat_index]; }else{
		 * GRR._game_score[i] -= baoPaiScore[seat_index]; } } }
		 */

		GRR._chi_hu_card[seat_index][0] = operate_card;

		GRR._win_order[seat_index] = 1;
		// 引用权位
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];

		int wFanShu = get_chi_hu_fen(chr);

		countCardType(chr, seat_index);

		int lChiHuScore = wFanShu;// wFanShu*m_pGameServiceOption->lCellScore;

		// 分数=胡牌分+上宝分+杠分+跟庄分
		// 流局=杠分+跟庄分+上宝分

		// 统计
		// _lost_fan_shu就是胡牌牌型番数
		if (zimo) {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index) {
					continue;
				}
				GRR._lost_fan_shu[i][seat_index] = lChiHuScore;
			}
		} else {
			GRR._lost_fan_shu[provide_index][seat_index] = lChiHuScore;
		}

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (i == seat_index) {
				continue;
			}

			int s = lChiHuScore;
			GRR._game_score[i] -= s;
			GRR._game_score[seat_index] += s;

		}
		if (zimo) {
			GRR._chi_hu_rights[provide_index].opr_or(GameConstants_YD.CHR_ZI_MO);
		} else {
			GRR._chi_hu_rights[provide_index].opr_or(GameConstants_YD.CHR_FANG_PAO);
		}

		if (provide_index == GameConstants.INVALID_SEAT) {
			GRR._provider[seat_index] = provide_index;
		} else {
			GRR._provider[seat_index] = provide_index;
		}
		// 设置变量
		_status_gang = false;
		_status_gang_hou_pao = false;

		change_player_status(seat_index, GameConstants.INVALID_VALUE);
		return;

	}

	// 胡牌底分
	public int get_chi_hu_fen(ChiHuRight chiHuRight) {

		int wFanShu = 1;

		// 确定房间底分
		int baseScore = 1;
		if (has_rule(GameConstants_YD.GAME_RULE_BASE2)) {
			baseScore = 2;
		}
		if (has_rule(GameConstants_YD.GAME_RULE_BASE5)) {
			baseScore = 5;
		}

		// 七对
		if (!(chiHuRight.opr_and(GameConstants_YD.CHR_QI_DUI)).is_empty()) {
			wFanShu = 2 * baseScore;
		}
		// 大七对判断
		else if (!(chiHuRight.opr_and(GameConstants_YD.CHR_DA_QI_DUI)).is_empty()) {
			wFanShu = 4 * baseScore;
		}

		// 全求人
		else if (!(chiHuRight.opr_and(GameConstants_YD.CHR_QUAN_QIU_REN)).is_empty()) {
			wFanShu = 10 * baseScore;
		}
		// 十三烂
		else if (!(chiHuRight.opr_and(GameConstants_YD.CHR_SHI_SAN_LAN)).is_empty()) {
			wFanShu = 2 * baseScore;
		}
		// 七星十三烂
		else if (!(chiHuRight.opr_and(GameConstants_YD.CHR_QI_XING_SHI_SAN_LAN)).is_empty()) {
			wFanShu = 4 * baseScore;
		} else {
			wFanShu = 1 * baseScore;
		}
		boolean gangKai = false;
		boolean gangHu = false;
		// 杠开
		if (!(chiHuRight.opr_and(GameConstants_YD.CHR_GANG_SHANG_KAI_HUA)).is_empty()) {
			gangKai = true;
		}

		// 抢杠胡
		if (!(chiHuRight.opr_and(GameConstants_YD.CHR_QIANG_GANG)).is_empty()) {
			gangHu = true;
		}
		if (gangKai || gangHu) {
			wFanShu = wFanShu * 2;
		}

		// 天胡
		if (!(chiHuRight.opr_and(GameConstants_YD.CHR_TIAN_HU)).is_empty()) {
			wFanShu = 40;
		}

		// 地胡
		if (!(chiHuRight.opr_and(GameConstants_YD.CHR_DI_HU)).is_empty()) {
			wFanShu = 20;
		}
		return wFanShu;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.cai.game.mj.AbstractMJTable#set_result_describe()
	 */
	@Override
	protected void set_result_describe() {
		int l;
		long type = 0;
		int pao_seat = -1;
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			String des = "";
			l = GRR._chi_hu_rights[i].type_count;

			for (int j = 0; j < l; j++) {
				type = GRR._chi_hu_rights[i].type_list[j];
				if (GRR._chi_hu_rights[i].is_valid()) {
					if (type == GameConstants_YD.CHR_PING_HU) {
						des += " 平胡";
					}
					if (type == GameConstants_YD.CHR_ZI_MO) {
						des += " 自摸";
					}
					if (type == GameConstants_YD.CHR_DI_HU) {
						des += " 地胡";
					}
					if (type == GameConstants_YD.CHR_TIAN_HU) {
						des += " 天胡";
					}
					if (type == GameConstants_YD.CHR_JIE_PAO) {
						des += " 炮胡";
					}
					if (type == GameConstants_YD.CHR_QIANG_GANG) {
						des += " 抢杠胡";
					}

					if (type == GameConstants_YD.CHR_GANG_SHANG_PAO) {
						des += " 杠上炮";
					}

					if (type == GameConstants_YD.CHR_GANG_SHANG_KAI_HUA) {
						des += " 杠上开花";
					}

					if (type == GameConstants_YD.CHR_DA_QI_DUI) {
						des += " 大七对";
					}

					if (type == GameConstants_YD.CHR_QI_DUI) {
						des += " 小七对";
					}

					if (type == GameConstants_YD.CHR_QUAN_QIU_REN) {
						des += " 全求人";
					}

					if (type == GameConstants_YD.CHR_SHI_SAN_LAN) {
						des += " 基本十三烂";
					}

					if (type == GameConstants_YD.CHR_QI_XING_SHI_SAN_LAN) {
						des += " 七星十三烂";
					}
				} else {
					if (type == GameConstants_YD.CHR_FANG_PAO) {
						pao_seat = i;
					}
				}
			}

			/*
			 * if (_cur_banker == i && ifGenZhuang == true) { des += " 跟庄"; }
			 */

			/*
			 * int jie_gang = 0, fang_gang = 0, ming_gang = 0, an_gang = 0; if
			 * (GRR != null) { for (int p = 0; p < GameConstants.GAME_PLAYER;
			 * p++) { for (int w = 0; w < GRR._weave_count[p]; w++) { if
			 * (GRR._weave_items[p][w].weave_kind != GameConstants.WIK_GANG) {
			 * continue; } if (p == i) {// 自己 // 接杠 if
			 * (GRR._weave_items[p][w].provide_player != p) { jie_gang++; } else
			 * { if (GRR._weave_items[p][w].public_card == 1) {// 弯杠
			 * ming_gang++; } else { an_gang++; } } } else { // 放杠 if
			 * (GRR._weave_items[p][w].provide_player == i) { fang_gang++; } } }
			 * } }
			 * 
			 * if (an_gang > 0) { des += " 暗杠X" + an_gang; } // 弯杠就是碰杠 if
			 * (ming_gang > 0) { des += " 弯杠X" + ming_gang; } if (fang_gang > 0)
			 * { des += " 放杠X" + fang_gang; } if (jie_gang > 0) { des += " 接杠X"
			 * + jie_gang; }
			 */
			if (!"".equals(des)) {
				for (int j = 0; j < getTablePlayerNumber(); j++) {
					GRR._result_des[j] = des;
				}
			}
		}
		if (pao_seat != -1) {
			GRR._result_des[pao_seat] += " 点炮";
		}
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

	public int get_ting_card(int[] cards, int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, boolean dai_feng, int seat_index,
			int card_type) {
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

			/*
			 * if(card_type==GameConstants_YD.HU_CARD_TYPE_ZI_MO||card_type==
			 * GameConstants_YD.HU_CARD_TYPE_GANG_SHANG_KAI_HUA){ if
			 * (this._logic.is_magic_index(i)){ continue; } }
			 */
			cbCurrentCard = _logic.switch_to_card_data(i);
			chr.set_empty();
			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card(cbCardIndexTemp, weaveItem, cbWeaveCount, cbCurrentCard, chr, card_type,
					seat_index)) {
				cards[count] = cbCurrentCard;
				count++;
			}
		}

		if (count == 0) {
		} else if (count == 1) {
			if (cards[0] == baoPai || cards[0] == baoPai + GameConstants.CARD_ESPECIAL_TYPE_BAO) {
				cards[0] = baoPai + GameConstants.CARD_ESPECIAL_TYPE_BAO;
			} else {
				for (int i = 0; i < _logic.get_magic_card_count(); i++) {
					cards[count] = _logic.switch_to_card_data(_logic.get_magic_card_index(i)) + GameConstants.CARD_ESPECIAL_TYPE_BAO;
					count++;
				}
			}

		} else if (count > 0 && count < l) {
			boolean ting_laizi = false;
			for (int i = 0; i < cards.length; i++) {
				if (cards[i] == baoPai) {
					cards[i] = baoPai + GameConstants.CARD_ESPECIAL_TYPE_BAO;
					ting_laizi = true;
				} else {
					if (cards[i] == baoPai + GameConstants.CARD_ESPECIAL_TYPE_BAO) {
						ting_laizi = true;
					}
				}
			}
			if (!ting_laizi) {
				for (int i = 0; i < _logic.get_magic_card_count(); i++) {
					cards[count] = baoPai + GameConstants.CARD_ESPECIAL_TYPE_BAO;
					count++;
				}
				if (count == l) {
					count = 1;
					cards[0] = -1;
				}
			}

		} else if (count == l) {
			count = 1;
			cards[0] = -1;
		}

		if (timer.get() > 50) {
			logger.warn("cost time too long " + Arrays.toString(cards_index) + ", cost time = " + timer.duration());
		}

		return count;
	}

	@Override
	public int get_zhong_seat_by_value_three(int nValue, int banker_seat) {
		int seat = 0;
		if (getTablePlayerNumber() >= 3) {
			seat = (banker_seat + (nValue - 1) % getTablePlayerNumber()) % getTablePlayerNumber();
		} else if (getTablePlayerNumber() == 2) {
			int v = (nValue - 1) % 4;
			switch (v) {
			case 0:
				seat = banker_seat;
				break;
			case 1:
				seat = get_null_seat_for_two_player(banker_seat);
				break;
			case 2:
				seat = get_banker_next_seat(banker_seat);
				break;
			default:
				seat = get_null_seat_for_two_player((banker_seat + 1) / getTablePlayerNumber());
				break;
			}
		}
		return seat;
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

			// // 碰牌判断
			if (_playerStatus[i].is_chi_peng_round()) {
				action = _logic.check_peng(GRR._cards_index[i], card);
				if (action != 0) {
					playerStatus.add_action(action);
					playerStatus.add_peng(card, seat_index);
					bAroseAction = true;
				}
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
			if (_playerStatus[i].is_chi_hu_round()) {
				// 吃胡判断
				ChiHuRight chr = GRR._chi_hu_rights[i];
				// playerStatus._chiHuRight;
				chr.set_empty();
				int cbWeaveCount = GRR._weave_count[i];
				action = analyse_chi_hu_card(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr, GameConstants_YD.HU_CARD_TYPE_JIE_PAO,
						i);

				// 结果判断
				if (action != 0) {
					_playerStatus[i].add_action(GameConstants.WIK_CHI_HU);
					_playerStatus[i].add_chi_hu(card, seat_index);// 吃胡的组合
					bAroseAction = true;
				}
			}
			// 吃牌判断
			int next = (seat_index + 1) % getTablePlayerNumber();
			if (i == next) {
				action = _logic.check_chi_gzcg(GRR._cards_index[next], card);
				int number = 0;
				int cardChanged = card;
				int cardValue = _logic.get_card_value(card);
				if (3 == _logic.get_card_color(card)) {
					if (cardValue > 4) {
						switch (cardValue) {
						case 5:
							cardChanged |= 0x67000;
							break;
						case 6:
							cardChanged |= 0x57000;
							break;
						case 7:
							cardChanged |= 0x56000;
							break;
						}
					} else {
						int begin = _logic.switch_to_card_index(0x31);
						int c = _logic.switch_to_card_index(card);
						for (int k = begin; k < begin + 4; k++) {
							if (k == c) {
								continue;
							}
							if (GRR._cards_index[next][k] > 0) {
								number++;
							}
						}
						if (number == 2) {
							int value = 16 * 16 * 16;
							for (int k = begin; k < begin + 4; k++) {
								if (k == c) {
									continue;
								}
								if (GRR._cards_index[i][k] > 0) {
									if (number == 1) {
										value *= 16;
									}
									cardChanged += value * _logic.get_card_value(_logic.switch_to_card_data(k));
									number--;
								}
							}
						}
					}
				}
				if (number == 3) {
					switch (cardValue) {
					case 1: // 东
						_playerStatus[next].add_action(GameConstants.WIK_RIGHT);
						_playerStatus[next].add_chi_gzcg(cardChanged, GameConstants.WIK_RIGHT, seat_index);
						_playerStatus[next].add_action(GameConstants.WIK_RIGHT);
						_playerStatus[next].add_chi_gzcg(cardChanged, GameConstants.WIK_RIGHT, seat_index);
						_playerStatus[next].add_action(GameConstants.WIK_RIGHT);
						_playerStatus[next].add_chi_gzcg(cardChanged, GameConstants.WIK_RIGHT, seat_index);
						break;
					case 2: // 南
						_playerStatus[next].add_action(GameConstants.WIK_RIGHT);
						_playerStatus[next].add_chi_gzcg(cardChanged, GameConstants.WIK_RIGHT, seat_index);
						_playerStatus[next].add_action(GameConstants.WIK_CENTER);
						_playerStatus[next].add_chi_gzcg(cardChanged, GameConstants.WIK_CENTER, seat_index);
						_playerStatus[next].add_action(GameConstants.WIK_CENTER);
						_playerStatus[next].add_chi_gzcg(cardChanged, GameConstants.WIK_CENTER, seat_index);
						break;
					case 3: // 西
						_playerStatus[next].add_action(GameConstants.WIK_LEFT);
						_playerStatus[next].add_chi_gzcg(cardChanged, GameConstants.WIK_LEFT, seat_index);
						_playerStatus[next].add_action(GameConstants.WIK_CENTER);
						_playerStatus[next].add_chi_gzcg(cardChanged, GameConstants.WIK_CENTER, seat_index);
						_playerStatus[next].add_action(GameConstants.WIK_CENTER);
						_playerStatus[next].add_chi_gzcg(cardChanged, GameConstants.WIK_CENTER, seat_index);
						break;
					case 4: // 北
						_playerStatus[next].add_action(GameConstants.WIK_LEFT);
						_playerStatus[next].add_chi_gzcg(cardChanged, GameConstants.WIK_LEFT, seat_index);
						_playerStatus[next].add_action(GameConstants.WIK_LEFT);
						_playerStatus[next].add_chi_gzcg(cardChanged, GameConstants.WIK_LEFT, seat_index);
						_playerStatus[next].add_action(GameConstants.WIK_LEFT);
						_playerStatus[next].add_chi_gzcg(cardChanged, GameConstants.WIK_LEFT, seat_index);
						break;
					}
				} else {
					if ((action & GameConstants.WIK_LEFT) != 0) {
						_playerStatus[next].add_action(GameConstants.WIK_LEFT);
						_playerStatus[next].add_chi(cardChanged, GameConstants.WIK_LEFT, seat_index);
					}
					if ((action & GameConstants.WIK_CENTER) != 0) {
						_playerStatus[next].add_action(GameConstants.WIK_CENTER);
						_playerStatus[next].add_chi(cardChanged, GameConstants.WIK_CENTER, seat_index);
					}
					if ((action & GameConstants.WIK_RIGHT) != 0) {
						_playerStatus[next].add_action(GameConstants.WIK_RIGHT);
						_playerStatus[next].add_chi(cardChanged, GameConstants.WIK_RIGHT, seat_index);
					}
				}

			}
			// 结果判断
			if (_playerStatus[next].has_action()) {
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
	@Override
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
				ChiHuRight chr = GRR._chi_hu_rights[i];// playerStatus._chiHuRight;
				chr.set_empty();
				int cbWeaveCount = GRR._weave_count[i];
				action = analyse_chi_hu_card(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr,
						GameConstants_YD.HU_CARD_TYPE_QIANG_GANG, i);

				// 结果判断
				if (action != 0) {
					_playerStatus[i].add_action(GameConstants.WIK_CHI_HU);
					_playerStatus[i].add_chi_hu(card, seat_index);// 吃胡的组合
					chr.opr_or(GameConstants_YD.CHR_QIANG_GANG);// 抢杠胡
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
	 * 处理牌桌结束逻辑
	 * 
	 * @param seat_index
	 * @param reason
	 */
	@Override
	protected boolean on_handler_game_finish(int seat_index, int reason) {

		// 庄家摸牌少于2张 说明第一圈就胡牌了 跟庄失败
		if (dispatchcardNum[_cur_banker] <= 1) {
			ifGenZhuang = false;
		}
		int real_reason = reason;

		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			_player_ready[i] = 0;
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		// 结算类型和普通麻将的不一样 普通麻将发15 我发1013 和客户端约好的
		roomResponse.setType(1013);
		GameEndResponse.Builder game_end = GameEndResponse.newBuilder();
		GameEndResponse_GZCG.Builder gameEndGzcg = GameEndResponse_GZCG.newBuilder();

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
			// 总分=胡牌分+杠分+跟庄+上宝分
			// int totalScore[] = new int[getTablePlayerNumber()];

			int genZhuangScore[] = new int[getTablePlayerNumber()];// 跟庄分
			int jingScore[] = new int[getTablePlayerNumber()];// 上宝分

			int mingGangCard[][] = new int[getTablePlayerNumber()][4]; // 弯杠牌
			int zhiGangCard[][] = new int[getTablePlayerNumber()][4]; // 直杠牌
			int anGangCard[][] = new int[getTablePlayerNumber()][4]; // 暗杆牌

			// 杠牌，每个人的分数
			float lGangScore[] = new float[getTablePlayerNumber()];

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				// 如果不是正常结束
				if (reason != GameConstants.Game_End_NORMAL) {
					// 判断上宝分
					if (baoPaiScore[i] == 135) {
						baoPaiScore[i] = 30;
					} else {
						baoPaiScore[i] = 0;
					}
					if (baoPaiScore[i] != 0) {
						for (int j = 0; j < getTablePlayerNumber(); j++) {
							if (i == j) {
								jingScore[j] += baoPaiScore[i];
								GRR._game_score[i] += baoPaiScore[i];
							} else {
								jingScore[j] -= baoPaiScore[i];
								GRR._game_score[i] -= baoPaiScore[i];
							}
						}
					}
				}

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

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				hupaiscore[i] = (int) GRR._game_score[i];
			}
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				GRR._game_score[i] += lGangScore[i];
				// 跟庄分数,当最后一个玩家摸牌数=0时说明是第一轮胡牌，不能算跟庄
				if (GRR._banker_player != i && ifGenZhuang && dispatchcardNum[getTablePlayerNumber() - 1] > 0) {
					genZhuangScore[i] += 2;
					GRR._game_score[i] += 2;
					genZhuangScore[GRR._banker_player] -= 2;
					GRR._game_score[GRR._banker_player] -= 2;
				}
				// 记录
				_player_result.game_score[i] += GRR._game_score[i];

			}

			// 结算统计
			boolean shangbao_and_hu = false;
			boolean shangsibao_and_no_hu = false;
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {

				gameEndGzcg.addHuPaiScore(hupaiscore[i]);// 胡牌分
				gameEndGzcg.addMingGang(0); // 杠分
				gameEndGzcg.addAnGang((int) lGangScore[i]);// 杠分
				gameEndGzcg.addChaoZhuang(genZhuangScore[i]);// 跟庄分
				gameEndGzcg.addGangJing(gangBaoScore[i]);// 杠宝牌分

				if (hupaiscore[i] > 0 && baoPaiScore[i] > 0) {
					shangbao_and_hu = true;
				}
				if (hupaiscore[i] <= 0 && baoPaiCount[i] == 4) {
					shangsibao_and_no_hu = true;
				}

				EveryJingInfo_GZCG.Builder gangPai = EveryJingInfo_GZCG.newBuilder();
				for (int k = 0; k < mingGangCard.length; k++) {
					if (mingGangCard[i][k] == 0) {
						continue;
					}
					gangPai.addFuJingCount(mingGangCard[i][k]);
				}
				for (int k = 0; k < anGangCard.length; k++) {
					if (anGangCard[i][k] == 0) {
						continue;
					}
					gangPai.addZhengJingCount(anGangCard[i][k]);
				}
				for (int k = 0; k < zhiGangCard.length; k++) {
					if (zhiGangCard[i][k] == 0) {
						continue;
					}
					gangPai.addEveryJingScore(zhiGangCard[i][k]);
				}
				gameEndGzcg.addJingInfo(gangPai);
			}
			int flag = -1;// 存储胡牌玩家
			int flag2 = -1;// 存储没胡牌但是上四宝玩家:一人给他30分
			for (int j = 0; j < this.getTablePlayerNumber(); j++) {
				if (hupaiscore[j] > 0) {
					flag = j;
				} else if (hupaiscore[j] <= 0 && baoPaiCount[j] == 4) {
					flag2 = j;
				}
			}
			if (shangbao_and_hu) {
				if (shangsibao_and_no_hu) {
					for (int j = 0; j < this.getTablePlayerNumber(); j++) {
						if (flag == j) {
							gameEndGzcg.addJingScore(baoPaiScore[flag] * (getTablePlayerNumber() - 1) - 30);// 上宝分
							GRR._game_score[flag] = GRR._game_score[flag] + baoPaiScore[flag] * (getTablePlayerNumber() - 1) - 30;
							gameEndGzcg.addTotalScore(hupaiscore[j] + (int) lGangScore[j] + genZhuangScore[j] + gangBaoScore[j]
									+ baoPaiScore[flag] * (getTablePlayerNumber() - 1) - 30);
						} else {
							if (flag2 == j) {
								gameEndGzcg.addJingScore(-baoPaiScore[flag] + (this.getTablePlayerNumber() - 1) * 30);// 上宝分
								GRR._game_score[j] = GRR._game_score[j] - baoPaiScore[flag] + (this.getTablePlayerNumber() - 1) * 30;
								gameEndGzcg.addTotalScore(hupaiscore[j] + (int) lGangScore[j] + genZhuangScore[j] + gangBaoScore[j]
										- baoPaiScore[flag] + (this.getTablePlayerNumber() - 1) * 30);
							} else {
								gameEndGzcg.addJingScore(-baoPaiScore[flag] - 30);// 上宝分
								GRR._game_score[j] = GRR._game_score[j] - baoPaiScore[flag] - 30;
								gameEndGzcg.addTotalScore(
										hupaiscore[j] + (int) lGangScore[j] + genZhuangScore[j] + gangBaoScore[j] - baoPaiScore[flag] - 30);
							}

						}
					}
				} else {
					for (int j = 0; j < this.getTablePlayerNumber(); j++) {
						if (flag == j) {
							gameEndGzcg.addJingScore(baoPaiScore[flag] * (getTablePlayerNumber() - 1));// 上宝分
							GRR._game_score[flag] += baoPaiScore[flag] * (getTablePlayerNumber() - 1);
							gameEndGzcg.addTotalScore(hupaiscore[j] + (int) lGangScore[j] + genZhuangScore[j] + gangBaoScore[j]
									+ baoPaiScore[flag] * (getTablePlayerNumber() - 1));
						} else {
							gameEndGzcg.addJingScore(-baoPaiScore[flag]);// 上宝分
							GRR._game_score[j] -= baoPaiScore[flag];
							gameEndGzcg.addTotalScore(hupaiscore[j] + (int) lGangScore[j] + genZhuangScore[j] + gangBaoScore[j] - baoPaiScore[flag]);
						}
					}
				}
			} else {
				if (shangsibao_and_no_hu) {
					for (int j = 0; j < this.getTablePlayerNumber(); j++) {
						if (flag2 == j) {
							gameEndGzcg.addJingScore((this.getTablePlayerNumber() - 1) * 30);// 上宝分
							GRR._game_score[j] = GRR._game_score[j] + (this.getTablePlayerNumber() - 1) * 30;
							gameEndGzcg.addTotalScore(hupaiscore[j] + (int) lGangScore[j] + genZhuangScore[j] + gangBaoScore[j]
									+ (this.getTablePlayerNumber() - 1) * 30);
						} else {
							gameEndGzcg.addJingScore(-30);// 上宝分
							GRR._game_score[j] = GRR._game_score[j] - 30;
							gameEndGzcg.addTotalScore(hupaiscore[j] + (int) lGangScore[j] + genZhuangScore[j] + gangBaoScore[j] - 30);
						}

						// gameEndGzcg.addTotalScore(totalScore[j]);// 总成绩
					}
				} else {
					gameEndGzcg.addJingScore(0);// 上宝分
					for (int j = 0; j < this.getTablePlayerNumber(); j++) {
						gameEndGzcg.addTotalScore(hupaiscore[j] + (int) lGangScore[j] + genZhuangScore[j] + gangBaoScore[j]);
					}
				}
			}
			shang_ju_zhuang = _cur_banker;

			this.load_player_info_data(roomResponse);

			game_end.setGameRound(_game_round);
			game_end.setCurRound(_cur_round);

			game_end.setCellScore(baseScore);

			if (reason != GameConstants.Game_End_NORMAL) {
				// 流局时设立庄家下家为庄家
				int next_player = (GRR._banker_player + getTablePlayerNumber() + 1) % getTablePlayerNumber();
				game_end.setBankerPlayer(next_player);// 庄家
				game_end.setLeftCardCount(GRR._left_card_count);// 剩余牌
			} else {
				game_end.setBankerPlayer(GRR._banker_player);// 庄家
				game_end.setLeftCardCount(GRR._left_card_count);// 剩余牌
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
				game_end.addGameScore(GRR._game_score[i]);// 放炮的人？大于-1代表是放炮者
															// 于都根据胡牌描述判断谁是放炮者
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
		} else if ((!is_sys()) && (reason == GameConstants.Game_End_RELEASE_PLAY || reason == GameConstants.Game_End_RELEASE_NO_BEGIN
				|| reason == GameConstants.Game_End_RELEASE_RESULT || reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT || reason == GameConstants.Game_End_RELEASE_SYSTEM)) {
			end = true;
			real_reason = GameConstants.Game_End_DRAW;// 流局
			game_end.setRoomOverType(1);
			game_end.setPlayerResult(this.process_player_result(reason));
		}
		game_end.setEndType(real_reason);

		// ////////////////////////////////////////////////////////////////////
		// 得分总的
		gameEndGzcg.setGameEnd(game_end);
		roomResponse.setGameEnd(game_end);
		roomResponse.setCommResponse(PBUtil.toByteString(gameEndGzcg));
		this.send_response_to_room(roomResponse);
		game_end.setCommResponse(PBUtil.toByteString(gameEndGzcg));
		record_game_round(game_end);

		// 结束后刷新玩家
		// RoomResponse.Builder roomResponse2 = RoomResponse.newBuilder();
		// roomResponse2.setGameStatus(_game_status);
		// roomResponse2.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);// 刷新玩家
		// this.load_player_info_data(roomResponse2);
		// this.send_response_to_room(roomResponse2);

		// 超时解散
		if (reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT || reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT) {
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
	 * 要海底
	 * 
	 * @param seat_index
	 * @return
	 */

	@Override
	public boolean exe_yao_hai_di(int seat_index) {
		this.set_handler(this._handler_yao_hai_di_yd);
		this._handler_yao_hai_di_yd.reset_status(seat_index);
		this._handler_yao_hai_di_yd.exe(this);
		return true;
	}

	@Override
	public void test_cards() {
		int[] cards_of_player0 = new int[] { 0x01, 0x07, 0x14, 0x17, 0x21, 0x24, 0x35, 0x31, 0x32, 0x33, 0x34, 0x36, 0x37 };
		int[] cards_of_player1 = new int[] { 0x01, 0x04, 0x07, 0x11, 0x14, 0x17, 0x21, 0x24, 0x27, 0x31, 0x32, 0x33, 0x34 };
		int[] cards_of_player2 = new int[] { 0x01, 0x01, 0x02, 0x02, 0x03, 0x03, 0x04, 0x04, 0x09, 0x21, 0x21, 0x23, 0x23 };
		int[] cards_of_player3 = new int[] { 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x11, 0x12, 0x13, 0x11 };

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
			} else if (this.getTablePlayerNumber() == 2) {
				GRR._cards_index[0][_logic.switch_to_card_index(cards_of_player0[j])] += 1;
				GRR._cards_index[1][_logic.switch_to_card_index(cards_of_player1[j])] += 1;
			}
		}

		// this.BACK_DEBUG_CARDS_MODE=true;
		// debug_my_cards=new int[] {52, 53, 35, 55, 9, 1, 6, 51, 35, 2, 52, 24,
		// 24, 35, 8, 1, 20, 9, 24, 49, 7, 49, 25, 4, 38, 53, 39, 40, 38, 50,
		// 34, 41, 39, 18, 23, 22, 23, 39, 54, 22, 36, 1, 40, 37, 21, 7, 34, 33,
		// 55, 52, 55, 9, 7, 55, 25, 41, 8, 2, 9, 50, 41, 20, 21, 8, 22, 3, 34,
		// 18, 2, 4, 22, 33, 19, 51, 5, 17, 6, 6, 53, 49, 4, 21, 34, 18, 19, 50,
		// 23, 19, 36, 37, 38, 3, 17, 18, 4, 23, 37, 51, 1, 54, 7, 39, 2, 35,
		// 36, 20, 41, 53, 38, 52, 3, 54, 25, 17, 17, 25, 5, 33, 3, 37, 33, 54,
		// 36, 5, 40, 49, 19, 50, 40, 5, 20, 21, 24, 8, 6, 51};

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
	@Override
	public boolean operate_player_cards(int seat_index, int card_count, int cards[], int weave_count, WeaveItem weaveitems[]) {
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
	public boolean handler_release_room(Player player, int opr_code) {
		if (is_sys()) {
			return handler_release_room_in_gold(player, opr_code);
		}
		int seat_index = 0;
		if (player != null) {
			seat_index = player.get_seat_index();
		}

		SysParamModel sysParamModel3007 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(313).get(3007);
		int delay = 60;
		if (sysParamModel3007 != null) {
			delay = sysParamModel3007.getVal1();
		}

		switch (opr_code) {
		case GameConstants.Release_Room_Type_SEND: {
			// 发起解散
			if (GameConstants.GS_MJ_FREE == _game_status) {
				// 游戏还没开始,不能发起解散
				return false;
			}
			// 有人申请解散了
			if (_gameRoomRecord.request_player_seat != GameConstants.INVALID_SEAT) {
				return false;
			}

			// 取消之前的调度
			if (_release_scheduled != null)
				_release_scheduled.cancel(false);
			_request_release_time = System.currentTimeMillis() + delay * 1000;
			if (GRR == null) {
				_release_scheduled = GameSchedule.put(
						new GameFinishRunnable(this.getRoom_id(), seat_index, GameConstants.Game_End_RELEASE_WAIT_TIME_OUT), delay, TimeUnit.SECONDS);
			} else {
				_release_scheduled = GameSchedule.put(
						new GameFinishRunnable(this.getRoom_id(), seat_index, GameConstants.Game_End_RELEASE_PLAY_TIME_OUT), delay, TimeUnit.SECONDS);
			}

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				_gameRoomRecord.release_players[i] = 0;
			}

			_gameRoomRecord.request_player_seat = seat_index;
			_gameRoomRecord.release_players[seat_index] = 1;// 同意

			// 不在线的玩家默认同意
			// for (int i = 0; i < MJGameConstants.GAME_PLAYER; i++) {
			// Player pl = this.get_players()[i];
			// if(pl==null || pl.isOnline()==false){
			// _gameRoomRecord.release_players[i] = 1;//同意
			// }
			// }
			int count = 0;
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (_gameRoomRecord.release_players[i] == 1) {// 都同意了

					count++;
				}
			}
			if (count == getTablePlayerNumber()) {
				if (GRR == null) {
					this.handler_game_finish(GameConstants.INVALID_SEAT, GameConstants.Game_End_RELEASE_RESULT);
				} else {
					this.handler_game_finish(GameConstants.INVALID_SEAT, GameConstants.Game_End_RELEASE_PLAY);
				}

				for (int j = 0; j < getTablePlayerNumber(); j++) {
					player = this.get_players()[j];
					if (player == null)
						continue;
					send_error_notify(j, 1, "游戏解散成功!");

				}
				return true;
			}

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_PLAYER_RELEASE);
			roomResponse.setReleaseTime(delay);
			roomResponse.setOperateCode(0);
			roomResponse.setRequestPlayerSeat(_gameRoomRecord.request_player_seat);
			roomResponse.setLeftTime(delay);
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				roomResponse.addReleasePlayers(_gameRoomRecord.release_players[i]);
			}
			this.send_response_to_room(roomResponse);

			return false;
		}

		case GameConstants.Release_Room_Type_AGREE: {
			if (GameConstants.GS_MJ_FREE == _game_status) {
				// 游戏还没开始,不能同意解散
				return false;
			}
			// 没人发起解散
			if (_gameRoomRecord.request_player_seat == GameConstants.INVALID_SEAT) {
				return false;
			}
			if (_gameRoomRecord.release_players[seat_index] == 1)
				return false;

			_gameRoomRecord.release_players[seat_index] = 1;

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_PLAYER_RELEASE);
			roomResponse.setReleaseTime(delay);
			roomResponse.setRequestPlayerSeat(_gameRoomRecord.request_player_seat);
			roomResponse.setOperateCode(1);
			roomResponse.setLeftTime((_request_release_time - System.currentTimeMillis()) / 1000);
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				roomResponse.addReleasePlayers(_gameRoomRecord.release_players[i]);
			}

			this.send_response_to_room(roomResponse);

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (get_players()[i] == null)
					continue;
				if (_gameRoomRecord.release_players[i] != 1) {
					return false;// 有一个不同意
				}
			}

			for (int j = 0; j < getTablePlayerNumber(); j++) {
				player = this.get_players()[j];
				if (player == null)
					continue;
				send_error_notify(j, 1, "游戏解散成功!");

			}

			if (_release_scheduled != null)
				_release_scheduled.cancel(false);
			_release_scheduled = null;
			if (GRR == null) {
				this.handler_game_finish(GameConstants.INVALID_SEAT, GameConstants.Game_End_RELEASE_RESULT);
			} else {
				this.handler_game_finish(GameConstants.INVALID_SEAT, GameConstants.Game_End_RELEASE_PLAY);
			}

			return false;
		}

		case GameConstants.Release_Room_Type_DONT_AGREE: {
			if (GameConstants.GS_MJ_FREE == _game_status) {
				// 游戏还没开始,不能同意解散
				return false;
			}
			if (_gameRoomRecord.request_player_seat == GameConstants.INVALID_SEAT) {
				return false;
			}
			_gameRoomRecord.release_players[seat_index] = 2;
			_gameRoomRecord.request_player_seat = GameConstants.INVALID_SEAT;

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setReleaseTime(delay);
			roomResponse.setType(MsgConstants.RESPONSE_PLAYER_RELEASE);
			roomResponse.setRequestPlayerSeat(_gameRoomRecord.request_player_seat);
			roomResponse.setOperateCode(1);
			int l = (int) (_request_release_time - System.currentTimeMillis() / 1000);
			if (l <= 0) {
				l = 1;
			}
			roomResponse.setLeftTime(l);
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				roomResponse.addReleasePlayers(_gameRoomRecord.release_players[i]);
			}
			this.send_response_to_room(roomResponse);

			_request_release_time = 0;
			_gameRoomRecord.request_player_seat = GameConstants.INVALID_SEAT;
			if (_release_scheduled != null)
				_release_scheduled.cancel(false);
			_release_scheduled = null;
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				_gameRoomRecord.release_players[i] = 0;
			}

			for (int j = 0; j < getTablePlayerNumber(); j++) {
				player = this.get_players()[j];
				if (player == null)
					continue;
				send_error_notify(j, 1, "游戏解散失败!玩家[" + this.get_players()[seat_index].getNick_name() + "]不同意解散");

			}

			return false;
		}

		case GameConstants.Release_Room_Type_CANCEL: {// 房主未开始游戏 解散
			if (GameConstants.GS_MJ_FREE != _game_status) {
				// 游戏已经来时
				return false;
			}
			if (_cur_round == 0) {
				// _gameRoomRecord.request_player_seat =
				// MJGameConstants.INVALID_SEAT;

				RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
				roomResponse.setType(MsgConstants.RESPONSE_FORCE_EXIT);// 直接拉出游戏
				this.send_response_to_room(roomResponse);

				for (int i = 0; i < getTablePlayerNumber(); i++) {
					Player p = this.get_players()[i];
					if (p == null)
						continue;
					if (i == seat_index) {
						send_error_notify(i, 2, "游戏已解散");
					} else {
						send_error_notify(i, 2, "游戏已被创建者解散");
					}

				}
				this.huan_dou(GameConstants.Game_End_RELEASE_NO_BEGIN);
				// 删除房间
				PlayerServiceImpl.getInstance().delRoomId(this.getRoom_id());
			} else {
				return false;

			}
		}
			break;
		case GameConstants.Release_Room_Type_QUIT: {// 玩家未开始游戏 退出
			if (GameConstants.GS_MJ_FREE != _game_status) {
				// 游戏已经开始
				return false;
			}
			RoomResponse.Builder quit_roomResponse = RoomResponse.newBuilder();
			quit_roomResponse.setType(MsgConstants.RESPONSE_FORCE_EXIT);// 直接拉出游戏
			send_response_to_player(seat_index, quit_roomResponse);

			send_error_notify(seat_index, 2, "您已退出该游戏");
			this.get_players()[seat_index] = null;
			_player_ready[seat_index] = 0;
			// WalkerGeek 允许少人模式
			super.init_less_param();

			if (player != null) {
				PlayerServiceImpl.getInstance().quitRoomId(this.getRoom_id(), player.getAccount_id());
			}

			if (_kick_schedule != null) {
				_kick_schedule.cancel(false);
				_kick_schedule = null;
			}
			// 刷新玩家
			RoomResponse.Builder refreshroomResponse = RoomResponse.newBuilder();
			refreshroomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);
			this.load_player_info_data(refreshroomResponse);
			//
			send_response_to_other(seat_index, refreshroomResponse);

			// 通知代理
			this.refresh_room_redis_data(GameConstants.PROXY_ROOM_PLAYER, false);
		}
			break;
		case GameConstants.Release_Room_Type_PROXY: {
			// 游戏还没开始,不能解散
			if (GameConstants.GS_MJ_FREE != _game_status) {
				return false;
			}

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_FORCE_EXIT);// 直接拉出游戏
			this.send_response_to_room(roomResponse);

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				Player p = this.get_players()[i];
				if (p == null)
					continue;
				send_error_notify(i, 2, "游戏已被创建者解散");

			}
			this.huan_dou(GameConstants.Game_End_RELEASE_NO_BEGIN);
			// 删除房间
			PlayerServiceImpl.getInstance().delRoomId(this.getRoom_id());

		}
			break;
		}

		return true;

	}

	public boolean is_bao_pai(int card_data) {
		if (card_data == baoPai) {
			return true;
		} else {
			return false;
		}

	}

	/**
	 * 添加宝牌参数
	 * 
	 * @param cards
	 */
	public void changeCard(int cards[]) {
		for (int i = 0; i < GameConstants.MAX_COUNT; i++) {
			if (cards[i] == 0) {
				continue;
			}
			if (cards[i] == baoPai) {
				// 牌值+15000作为标识tag
				cards[i] += GameConstants.CARD_ESPECIAL_TYPE_BAO;
			}
		}
	}

	public void process_chi_hu_player_operate_hy(int seat_index, int operate_card[], int card_count, boolean rm) {
		// 引用权位
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];

		// 效果
		this.operate_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_HU, chr.type_count, chr.type_list, 1, GameConstants.INVALID_SEAT);

		// 手牌删掉
		this.operate_player_cards(seat_index, 0, null, 0, null);

		if (rm) {
			// 把摸的牌从手牌删掉,结算的时候不显示这张牌的
			for (int i = 0; i < card_count; i++) {
				GRR._cards_index[seat_index][_logic.switch_to_card_index(operate_card[i])]--;
			}
		}

		// 显示胡牌
		int cards[] = new int[GameConstants.MAX_INDEX];
		int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[seat_index], cards);
		for (int i = 0; i < card_count; i++) {
			cards[hand_card_count++] = operate_card[i] + GameConstants.CARD_ESPECIAL_TYPE_HU;
		}
		this.operate_show_card(seat_index, GameConstants.Show_Card_HU, hand_card_count, cards, GameConstants.INVALID_SEAT);

		return;
	}

	@Override
	protected int get_banker_next_seat(int banker_seat) {
		int count = 0;
		int seat = banker_seat;
		do {
			count++;
			seat = (seat + 1) % 4;
		} while (get_players()[seat] == null && count <= 5);
		return seat;
	}

	public boolean only_gang(WeaveItem weaveItem[], int cbWeaveCount) {
		for (int i = 0; i < cbWeaveCount; i++) {
			if (weaveItem[i].weave_kind != GameConstants.WIK_GANG)
				return false;
		}
		return true;
	}

	// 七小对牌 七小对：胡牌时，手上任意七对牌。
	public int is_qi_xiao_dui(int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int cur_card, int[] get_magic_card_index,
			int _magic_card_count) {

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

			if (_magic_card_count > 0) {
				for (int m = 0; m < _magic_card_count; m++) {
					// 王牌过滤
					if (i == get_magic_card_index[m])
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
		if (_magic_card_count > 0) {
			int count = 0;
			for (int m = 0; m < _magic_card_count; m++) {
				count += cbCardIndexTemp[get_magic_card_index[m]];
			}

			if (cbReplaceCount > count) {
				return GameConstants.WIK_NULL;
			}
		} else {
			if (cbReplaceCount > 0)
				return GameConstants.WIK_NULL;
		}

		return GameConstants_YD.CHR_QI_DUI;

	}

	// 七小对牌 七小对：胡牌时，手上任意七对牌。
	public int is_qi_xiao_dui_cutlaizi(int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int cur_card, int[] get_magic_card_index,
			int _magic_card_count) {

		// 组合判断
		if (cbWeaveCount != 0)
			return GameConstants.WIK_NULL;

		// 单牌数目
		int cbReplaceCount = 0;

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

			if (_magic_card_count > 0) {
				for (int m = 0; m < _magic_card_count; m++) {
					// 王牌过滤
					if (i == get_magic_card_index[m])
						continue;

					// 单牌统计
					if (cbCardCount == 1 || cbCardCount == 3)
						cbReplaceCount++;
				}
			} else {
				// 单牌统计
				if (cbCardCount == 1 || cbCardCount == 3)
					cbReplaceCount++;
			}

		}

		// 王牌不够
		if (_magic_card_count > 0) {
			int count = 0;
			for (int m = 0; m < _magic_card_count; m++) {
				count += cbCardIndexTemp[get_magic_card_index[m]];
			}
			// 别家打出了癞子 需要转换成实际牌值计算 这里要减掉一个癞子
			count--;

			if (cbReplaceCount > count) {
				return GameConstants.WIK_NULL;
			}
		} else {
			if (cbReplaceCount > 0)
				return GameConstants.WIK_NULL;
		}

		return GameConstants_YD.CHR_QI_DUI;

	}

	/**
	 * 刷新玩家的牌
	 * 
	 * @param seat_index
	 * @param card_count
	 * @param cards
	 * @param weave_count
	 * @param weaveitems
	 * @return
	 */
	@Override
	public boolean operate_player_cards_with_ting(int seat_index, int card_count, int cards[], int weave_count, WeaveItem weaveitems[]) {
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

		// 手牌
		for (int j = 0; j < card_count; j++) {
			roomResponse.addCardData(cards[j]);
		}

		int ting_count = _playerStatus[seat_index]._hu_out_card_count;

		// 打出去可以听牌的个数
		roomResponse.setOutCardCount(ting_count);

		for (int i = 0; i < ting_count; i++) {
			int ting_card_cout = _playerStatus[seat_index]._hu_out_card_ting_count[i];
			roomResponse.addOutCardTingCount(ting_card_cout);
			// if (_playerStatus[seat_index]._hu_out_card_ting[i]!=baoPai) {
			roomResponse.addOutCardTing(_playerStatus[seat_index]._hu_out_card_ting[i] + GameConstants.CARD_ESPECIAL_TYPE_TING);

			Int32ArrayResponse.Builder int_array = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < ting_card_cout; j++) {
				int_array.addItem(_playerStatus[seat_index]._hu_out_cards[i][j]);
			}
			roomResponse.addOutCardTingCards(int_array);
		}
		/*
		 * for(int i=0;i<cards.length;i++){ if(){
		 * roomResponse.addOutCardTing(_playerStatus
		 * [seat_index]._hu_out_card_ting[i] +
		 * GameConstants.CARD_ESPECIAL_TYPE_BAO); } }
		 */

		// 自己才有牌数据
		this.send_response_to_player(seat_index, roomResponse);

		GRR.add_room_response(roomResponse);

		return true;
	}

	// 全求人
	public boolean isQuanQiuRen(int[] cards_index, WeaveItem[] weaveItems, int weaveCount, int cur_card) {
		int card_index = _logic.switch_to_card_index(baoPai);
		// 如果手上有癞子牌 不能算全求人
		if (cards_index[card_index] != 0) {
			return false;
		}

		for (int i = 0; i < weaveCount; i++) {
			if (weaveItems[i].type == GameConstants.GANG_TYPE_AN_GANG) {
				return false;
			}

		}
		if (!_logic.is_dan_diao_lai(cards_index, cur_card)) {
			return false;
		}
		return true;
	}

	// 全求人
	public boolean isQuanQiuRen_cutlaizi(int[] cards_index, WeaveItem[] weaveItems, int weaveCount, int cur_card) {
		int card_index = _logic.switch_to_card_index(baoPai);
		// 如果手上有癞子牌 不能算全求人
		if (cards_index[card_index] != 0) {
			return false;
		}

		for (int i = 0; i < weaveCount; i++) {
			if (weaveItems[i].type == GameConstants.GANG_TYPE_AN_GANG) {
				return false;
			}

		}
		if (!_logic.is_dan_diao(cards_index, cur_card)) {
			return false;
		}
		return true;
	}

	// 江西于都麻将十三烂
	public boolean isShiSanLanYD_cutlaizi(int[] cards_index, WeaveItem[] weaveItems, int weaveCount) {
		if (weaveCount != 0) {
			return false;
		}

		// 癞子总数
		int magicCountAll = _logic.magic_count(cards_index);

		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}
		cbCardIndexTemp[_logic.get_magic_card_index(0)] = magicCountAll--;

		for (int i = 0; i < 27; i += 9) {
			for (int j = i; j < i + 9; j++) {
				if (cbCardIndexTemp[j] == 0) {
					continue;
				}
				if (_logic.is_magic_index(j)) {
					magicCountAll--;
					if (magicCountAll > 0) {
						continue;
					} else {
						if (cbCardIndexTemp[j] > 1) {
							return false;
						}
						int limitIndex = i + 9;
						if (j + 1 < limitIndex && cbCardIndexTemp[j + 1] != 0) {
							return false;
						}
						if (j + 2 < limitIndex && cbCardIndexTemp[j + 2] != 0) {
							return false;
						}
					}
				}
				if (cbCardIndexTemp[j] > 1) {
					return false;
				}

				int limitIndex = i + 9;
				if (j + 1 < limitIndex && cbCardIndexTemp[j + 1] != 0) {
					return false;
				}
				if (j + 2 < limitIndex && cbCardIndexTemp[j + 2] != 0) {
					return false;
				}
			}
		}

		for (int i = GameConstants.MAX_ZI; i < GameConstants.MAX_ZI_FENG; i++) {
			if (cbCardIndexTemp[i] == 0) {
				continue;
			}
			if (cbCardIndexTemp[i] > 1) {
				return false;
			}
		}

		return true;
	}

	// 江西于都麻将七星十三烂 兼容癞子 十三烂的基础上必须东南西北中发白都有一张
	public boolean isQXShiSanLan_cutlaizi(int[] cards_index, WeaveItem[] weaveItems, int weaveCount) {
		// 癞子总数
		int magicCountAll = _logic.magic_count(cards_index);

		if (weaveCount != 0) {
			return false;
		}
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}
		cbCardIndexTemp[_logic.get_magic_card_index(0)] = magicCountAll--;

		int need_laizi = 0;

		for (int i = 0; i < 7; i++) {
			int index = _logic.switch_to_card_index(0x31) + i;
			if (cbCardIndexTemp[index] != 1) {
				need_laizi++;
			}

		}

		for (int i = 0; i < 27; i += 9) {
			for (int j = i; j < i + 9; j++) {
				if (cbCardIndexTemp[j] == 0) {
					continue;
				}
				if (_logic.is_magic_index(j)) {
					magicCountAll--;
					if (magicCountAll > 0) {
						continue;
					} else {
						if (cbCardIndexTemp[j] > 1) {
							return false;
						}
						int limitIndex = i + 9;
						if (j + 1 < limitIndex && cbCardIndexTemp[j + 1] != 0) {
							return false;
						}
						if (j + 2 < limitIndex && cbCardIndexTemp[j + 2] != 0) {
							return false;
						}
					}
				}
				if (cbCardIndexTemp[j] > 1) {
					return false;
				}

				int limitIndex = i + 9;
				if (j + 1 < limitIndex && cbCardIndexTemp[j + 1] != 0) {
					return false;
				}
				if (j + 2 < limitIndex && cbCardIndexTemp[j + 2] != 0) {
					return false;
				}
			}
		}

		for (int i = GameConstants.MAX_ZI; i < GameConstants.MAX_ZI_FENG; i++) {
			if (cbCardIndexTemp[i] == 0 || _logic.is_magic_index(i)) {
				continue;
			}
			if (cbCardIndexTemp[i] > 1) {
				return false;
			}
		}
		if (_logic.magic_count(cbCardIndexTemp) >= need_laizi) {
			return true;
		} else {
			return false;
		}
	}

}
