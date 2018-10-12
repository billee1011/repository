package com.cai.game.mj.shanxi.sxkdd;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.mj.GameConstants_SXKDD;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.Player;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.SysParamModel;
import com.cai.common.domain.WeaveItem;
import com.cai.dictionary.SysParamDict;
import com.cai.game.mj.AbstractMJTable;
import com.cai.game.mj.MJGameLogic.AnalyseItem;
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

public class MJTable_SXKDD extends AbstractMJTable {

	/** 选金handler */
	private MJHandlerHaoZi_SXKDD handlerJing_XN;

	/** 报听handler */
	private MJHandlerOutCardBaoTing_SXKDD baoTing_XN;

	public int tingCardCount; // 听牌数量

	public boolean outTingCardIsMagic; // 出牌报听的牌是不是金牌

	public MJTable_SXKDD() {
		super(MJType.GAME_TYPE_SXKDD);
	}

	private static final long serialVersionUID = 1L;

	@Override
	public int getTablePlayerNumber() {
		if (has_rule(GameConstants_SXKDD.GAME_RULE_MJ_SXKDD_PLAYER_NUMBER_3)) {
			return GameConstants.GAME_PLAYER - 1;
		} else {
			return GameConstants.GAME_PLAYER;
		}
	}

	@Override
	protected void onInitTable() {
		_handler_dispath_card = new MJHandlerDispatchCard_SXKDD();
		_handler_out_card_operate = new MJHandlerOutCardOperate_SXKDD();
		_handler_gang = new MJHandlerGang_SXKDD();
		_handler_chi_peng = new MJHandlerChiPeng_SXKDD();
		handlerJing_XN = new MJHandlerHaoZi_SXKDD();
		baoTing_XN = new MJHandlerOutCardBaoTing_SXKDD();
		outTingCardIsMagic = false;
	}

	/**
	 * 参数初始化
	 */
	protected void onInitParam() {
		// 牌局开始清理上局癞子牌
		_logic.clean_magic_cards();
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_player_result.biaoyan[i] = 0;
		}
		tingCardCount = 0;
		outTingCardIsMagic = false;
	}

	/**
	 * 庄家选择
	 */
	public void progress_banker_select() {
		if (_cur_banker == GameConstants.INVALID_SEAT) {
			_cur_banker = getOpenRoomIndex();// 创建者的玩家为专家
			_shang_zhuang_player = GameConstants.INVALID_SEAT;
			_lian_zhuang_player = GameConstants.INVALID_SEAT;
		}

		if (_cur_round < 2 && !isOpenPlayerInRoom()) {// 金币场 随机庄家
			Random random = new Random();//
			int rand = random.nextInt(6) + 1 + random.nextInt(6) + 1;
			_cur_banker = rand % getTablePlayerNumber();//

			_shang_zhuang_player = GameConstants.INVALID_SEAT;
			_lian_zhuang_player = GameConstants.INVALID_SEAT;
		}
	}

	/**
	 * 游戏开始
	 */
	@Override
	protected boolean on_game_start() {
		onInitParam();
		show_tou_zi(GRR._banker_player);

		GameStartResponse.Builder gameStartResponse = GameStartResponse.newBuilder();
		gameStartResponse.setBankerPlayer(GRR._banker_player);
		gameStartResponse.setCurrentPlayer(_current_player);
		gameStartResponse.setLeftCardCount(GRR._left_card_count);

		int hand_cards[][] = new int[getTablePlayerNumber()][GameConstants.MAX_COUNT];
		// 发送数据
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[i], hand_cards[i]);
			changeCard(hand_cards[i]);
			gameStartResponse.addCardsCount(hand_card_count);
		}
		// 发送数据
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

			// 自己的牌
			gameStartResponse.clearCardData();
			for (int j = 0; j < GameConstants.MAX_COUNT; j++) {
				gameStartResponse.addCardData(hand_cards[i][j]);
			}

			// 回放数据
			GRR._video_recode.addHandCards(cards);

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			this.load_room_info_data(roomResponse);
			this.load_common_status(roomResponse);
			this.load_player_info_data(roomResponse);
			roomResponse.setType(MsgConstants.RESPONSE_GAME_START);

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

		if (has_rule(GameConstants_SXKDD.GAME_RULE_MJ_SXKDD_CHANG_GUI)) {
			// 发牌
			exe_dispatch_card(_cur_banker, GameConstants.WIK_NULL, GameConstants.DELAY_SEND_CARD_DELAY);
		} else {
			// 选金
			this.set_handler(this.handlerJing_XN);
			this.handlerJing_XN.reset_status(_cur_banker);
			this._handler.exe(this);
		}

		return false;
	}

	/**
	 * 切换到报听handler
	 * 
	 * @param seat_index
	 * @param card
	 * @param type
	 */
	public void exe_bao_ting(int seat_index, int card, int type) {
		this.set_handler(this.baoTing_XN);
		this.baoTing_XN.reset_status(seat_index, card, type);
		this._handler.exe(this);
	}

	@Override
	public int analyse_chi_hu_card(int[] cards_index, WeaveItem[] weaveItems, int weave_count, int cur_card,
			ChiHuRight chiHuRight, int card_type, int _seat_index) {

		if (cur_card == 0) {
			return GameConstants.WIK_NULL;
		}

		// 构造数据
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}
		if (cur_card != GameConstants.INVALID_VALUE) {
			cbCardIndexTemp[_logic.switch_to_card_index(cur_card)]++;
		}
		int cbChiHuKind = GameConstants.WIK_NULL;

		long qxd = _logic.is_qi_xiao_dui_hy(cards_index, weaveItems, weave_count, cur_card);
		if (qxd != GameConstants.WIK_NULL) {
			if (qxd == GameConstants.CHR_HUNAN_SHUANG_HAO_HUA_QI_XIAO_DUI) {
				qxd = GameConstants.CHR_HUNAN_HAOHUA_QI_XIAO_DUI;
			}
			chiHuRight.opr_or(qxd);
			cbChiHuKind = GameConstants.WIK_CHI_HU;
		}

		int[] magic_cards_index = new int[GameUtilConstants.MAX_MAGIC_INDEX_COUNT];
		int magic_card_count = _logic.get_magic_card_count();

		if (magic_card_count > 2) { // 一般只有两种癞子牌存在
			magic_card_count = 2;
		}

		for (int i = 0; i < magic_card_count; i++) {
			magic_cards_index[i] = _logic.get_magic_card_index(i);
		}

		boolean can_win = AnalyseCardUtil.analyse_feng_chi_by_cards_index(cards_index,
				_logic.switch_to_card_index(cur_card), magic_cards_index, magic_card_count);

		if (_logic.is_qing_yi_se(cards_index, weaveItems, weave_count, cur_card)) {
			chiHuRight.opr_or(GameConstants.CHR_HUNAN_QING_YI_SE);
		}

		if (_logic.is_yi_tiao_long(cbCardIndexTemp, weave_count)) {
			chiHuRight.opr_or(GameConstants.CHR_HUNAN_YI_TIAO_LONG);
		}

		cbChiHuKind = GameConstants.WIK_CHI_HU;
		boolean zimo = true;
		if (card_type == GameConstants.HU_CARD_TYPE_GANG_KAI) {
			chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
		} else if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
			chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
		} else if (card_type == GameConstants.HU_CARD_TYPE_QIANGGANG) {
			zimo = false;
			chiHuRight.opr_or(GameConstants.CHR_HUNAN_QIANG_GANG_HU);// 抢杠胡
		} else if (card_type == GameConstants.HU_CARD_TYPE_PAOHU) {
			zimo = false;
			chiHuRight.opr_or(GameConstants.CHR_HUNAN_GANG_SHANG_PAO);// 抢杠胡
		} else if (card_type == GameConstants.HU_CARD_TYPE_PAOHU) {
			zimo = false;
			chiHuRight.opr_or(GameConstants.CHR_SHU_FAN);
		}
		boolean magicEyes = false;
		if (can_win && !zimo) {
			List<AnalyseItem> analyseItemArray = new ArrayList<AnalyseItem>();
			// 分析扑克
			_logic.analyse_card(cbCardIndexTemp, weaveItems, weave_count, analyseItemArray, true);
			for (AnalyseItem analyseItem : analyseItemArray) {
				if (analyseItem.bMagicEye) {
					magicEyes = true;
				}
			}
		}
		int number = getCardNumber(cur_card);

		if (zimo) {
			if (number < 3 && !_logic.is_magic_card(cur_card)) {
				chiHuRight.set_empty();
				return GameConstants.WIK_NULL;
			}
		} else {
			if (number < 6) {
				chiHuRight.set_empty();
				return GameConstants.WIK_NULL;
			}
		}
		// 癞子牌做牌眼不能点炮
		if (!zimo) {
			if (magicEyes) {
				chiHuRight.set_empty();
				return GameConstants.WIK_NULL;
			}
		}

		if (!can_win) {
			if (qxd != GameConstants.WIK_NULL) {
				return cbChiHuKind;
			} else {
				chiHuRight.set_empty();
				return GameConstants.WIK_NULL;
			}
		}

		return cbChiHuKind;
	}

	public int getCardNumber(int card) {
		int color = _logic.get_card_color(card);

		if (color > 2) {
			return 10;
		} else {
			return _logic.get_card_value(card);
		}

	}

	public boolean operate_out_card_bao_ting(int seat_index, int count, int cards[], int type, int to_player) {
		if (seat_index == GameConstants.INVALID_SEAT) {
			return false;
		}
		//// 发送玩家出牌 201 =玩家出牌(CMD_OutCard: operate_player,operate_card)
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		this.load_common_status(roomResponse);
		roomResponse.setType(MsgConstants.RESPONSE_OUT_CARD);
		roomResponse.setTarget(seat_index);
		roomResponse.setCardType(type);// 出牌
		roomResponse.setCardCount(count);
		int flashTime = 60;
		int standTime = 60;
		int gameId = this.getGame_id() == 0 ? 1 : this.getGame_id();
		SysParamModel sysParamModel105 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(gameId)
				.get(1105);
		if (sysParamModel105 != null && sysParamModel105.getVal3() > 0 && sysParamModel105.getVal3() < 1000) {
			flashTime = sysParamModel105.getVal3();
			standTime = sysParamModel105.getVal4();
		}
		roomResponse.setFlashTime(flashTime);
		roomResponse.setStandTime(standTime);

		if (to_player == GameConstants.INVALID_SEAT) {
			for (int i = 0; i < count; i++) {

//				roomResponse.addCardData(cards[i]);
				roomResponse.addCardData(GameConstants.BLACK_CARD);
			}
			this.send_response_to_player(seat_index, roomResponse);// 自己有值

			GRR.add_room_response(roomResponse);

			roomResponse.clearCardData();

			for (int i = 0; i < count; i++) {
				roomResponse.addCardData(GameConstants.BLACK_CARD);
			}
			this.send_response_to_other(seat_index, roomResponse);// 别人是背着的
		} else {
			if (to_player == seat_index) {
				for (int i = 0; i < count; i++) {
					roomResponse.addCardData(cards[i]);
				}
			} else {
				for (int i = 0; i < count; i++) {
					roomResponse.addCardData(GameConstants.BLACK_CARD);
				}
			}

			this.send_response_to_player(to_player, roomResponse);
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

		int wFanShu = get_chi_hu_fen(chr, seat_index, zimo);

		countCardType(chr, seat_index);

		int lChiHuScore = wFanShu;// wFanShu*m_pGameServiceOption->lCellScore;
		int scoreNumber = getCardNumber(operate_card);
		//如果是胡癞子牌，算最大牌型
		if(_logic.is_magic_card(operate_card)){
			Set<Integer> set = new HashSet<Integer>();
			for(int i= 0; i < _playerStatus[seat_index]._hu_cards.length; i++){
				int card = _playerStatus[seat_index]._hu_cards[i];
				if(card <3000 && card != 0){
					set.add(getCardNumber(card));
				}
			}
			int count = 0;
			for (Integer integer : set) {
				if(integer == null ){
					continue;
				}
				if(count == 0 ){
					scoreNumber = integer;
				}
				if(scoreNumber < integer){
					scoreNumber = integer;
				}
			}
		}
		
		
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
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index) {
					continue;
				}

				int s = lChiHuScore * scoreNumber;
				// 胡牌分
				GRR._game_score[i] -= s;
				GRR._game_score[seat_index] += s;
			}
		} else {
			int s = lChiHuScore * scoreNumber;
			// 胡牌分
			GRR._game_score[provide_index] -= s;
			GRR._game_score[seat_index] += s;

			GRR._chi_hu_rights[provide_index].opr_or(GameConstants.CHR_FANG_PAO);

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
	public int get_chi_hu_fen(ChiHuRight chiHuRight, int seat_index, boolean zimo) {
		int wFanShu = 1;
		// 地胡
		if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_HAOHUA_QI_XIAO_DUI)).is_empty()) {
			if (zimo) {
				wFanShu = 8;
			} else {
				wFanShu = 4;
			}
		} else if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_YI_TIAO_LONG)).is_empty()
				|| !(chiHuRight.opr_and(GameConstants.CHR_HUNAN_QING_YI_SE)).is_empty()
				|| !(chiHuRight.opr_and(GameConstants.CHR_HUNAN_QI_XIAO_DUI)).is_empty()) {
			if (zimo) {
				wFanShu = 4;
			} else {
				wFanShu = 2;
			}

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
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			String des = "";
			boolean pinghu = true;
			l = GRR._chi_hu_rights[i].type_count;

			for (int j = 0; j < l; j++) {
				type = GRR._chi_hu_rights[i].type_list[j];
				if (GRR._chi_hu_rights[i].is_valid()) {
					if (type == GameConstants.CHR_ZI_MO) {
						des += " 自摸";
					}

					if (type == GameConstants.CHR_SHU_FAN) {
						des += " 接炮";
					}
					if (type == GameConstants.CHR_HUNAN_QIANG_GANG_HU) {
						des += " 抢杠胡";
					}
					if (type == GameConstants.CHR_HUNAN_GANG_KAI) {
						des += " 杠开";
					}
					if (type == GameConstants.CHR_HUNAN_YI_TIAO_LONG) {
						des += " 一条龙";
						pinghu = false;
					}
					if (type == GameConstants.CHR_HUNAN_QING_YI_SE) {
						des += " 清一色";
						pinghu = false;
					}

					if (type == GameConstants.CHR_HUNAN_QI_XIAO_DUI) {
						des += " 七小对";
						pinghu = false;
					}
					if (type == GameConstants.CHR_HUNAN_HAOHUA_QI_XIAO_DUI) {
						des += " 豪华七小对";
						pinghu = false;
					}

				} else {
					if (type == GameConstants.CHR_FANG_PAO) {
						des += " 放炮";
					}
				}
			}

			if (GRR._chi_hu_rights[i].is_valid() && pinghu) {
				des += " 平胡";
			}

			int jie_gang = 0, fang_gang = 0, ming_gang = 0, an_gang = 0;
			if (GRR != null) {
				for (int p = 0; p < getTablePlayerNumber(); p++) {
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
				des += " 补杠X" + ming_gang;
			}
			if (fang_gang > 0) {
				des += " 放杠X" + fang_gang;
			}
			if (jie_gang > 0) {
				des += " 明杠X" + jie_gang;
			}

			GRR._result_des[i] = des;
		}

	}

	public int get_ting_card(int[] cards, int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, boolean dai_feng,
			int seat_index) {
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		ChiHuRight chr = new ChiHuRight();

		int count = 0;
		int l = GameConstants.MAX_ZI_FENG;
		for (int i = 0; i < l; i++) {
			if (this._logic.is_magic_index(i))
				continue;
			int cbCurrentCard = _logic.switch_to_card_data(i);
			chr.set_empty();
			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card(cbCardIndexTemp, weaveItem, cbWeaveCount, cbCurrentCard,
					chr, GameConstants.HU_CARD_TYPE_ZIMO, seat_index)) {
				
				cards[count] = cbCurrentCard;
				count++;
			}
		}
		

		if (count == 0 && getCardNumber( _logic.switch_to_card_data(_logic.get_magic_card_index(0))) >= 3) {
			int[] magic_cards_index = new int[GameUtilConstants.MAX_MAGIC_INDEX_COUNT];
			int magic_card_count = _logic.get_magic_card_count();

			if (magic_card_count > 2) { // 一般只有两种癞子牌存在
				magic_card_count = 2;
			}

			for (int i = 0; i < magic_card_count; i++) {
				magic_cards_index[i] = _logic.get_magic_card_index(i);
			}
			
			if(AnalyseCardUtil.analyse_feng_chi_by_cards_index_yd(cbCardIndexTemp, _logic.get_magic_card_index(0), magic_cards_index, magic_card_count, false)){
				cards[count] = _logic.switch_to_card_data(_logic.get_magic_card_index(0))
						+ GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
				count++;
			}
		} else if (count > 0 && count < l) {
			if (!has_rule(GameConstants_SXKDD.GAME_RULE_MJ_SXKDD_CHANG_GUI)) {
				cards[count] = _logic.switch_to_card_data(_logic.get_magic_card_index(0))
						+ GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
				count++;
			}
		} else if (count == l) {
			count = 1;
			cards[0] = -1;
		}
		if(AnalyseCardUtil.analyse_feng_chi_by_cards_index(cbCardIndexTemp,
				_logic.get_magic_card_index(0), new int[]{}, 0)){
			
		}
		outTingCardIsMagic = false;
		return count;
	}

	public int get_ting_card_hu(int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, boolean dai_feng,
			int seat_index) {
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		ChiHuRight chr = new ChiHuRight();

		int count = 0;
		int l = GameConstants.MAX_ZI_FENG;
		for (int i = 0; i < l; i++) {
			if (this._logic.is_magic_index(i))
				continue;
			int cbCurrentCard = _logic.switch_to_card_data(i);
			chr.set_empty();
			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card(cbCardIndexTemp, weaveItem, cbWeaveCount, cbCurrentCard,
					chr, GameConstants.HU_CARD_TYPE_ZIMO, seat_index)) {
				count++;
			}
		}
		int[] magic_cards_index = new int[GameUtilConstants.MAX_MAGIC_INDEX_COUNT];
		int magic_card_count = _logic.get_magic_card_count();

		if (magic_card_count > 2) { // 一般只有两种癞子牌存在
			magic_card_count = 2;
		}

		for (int i = 0; i < magic_card_count; i++) {
			magic_cards_index[i] = _logic.get_magic_card_index(i);
		}

		int cbCurrentCardIndex = _logic.get_magic_card_index(0);
		if (AnalyseCardUtil.analyse_win_by_cards_index_taojiang(cbCardIndexTemp, cbCurrentCardIndex, magic_cards_index,
				magic_card_count)) {
			count++;
		}
		return count;
	}

	public int getTingCardCount(int seat_index) {
		return get_ting_card_hu(GRR._cards_index[seat_index], GRR._weave_items[seat_index],
				GRR._weave_count[seat_index], true, seat_index);
	}

	/**
	 * 处理吃胡的玩家
	 * 
	 * @param seat_index
	 * @param operate_card
	 * @param rm
	 */
	public void process_chi_hu_player_operate(int seat_index, int operate_card, boolean rm) {
		// 引用权位
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];
		// 效果
		this.operate_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_HU, chr.type_count, chr.type_list, 1,
				GameConstants.INVALID_SEAT);

		// 手牌删掉
		this.operate_player_cards(seat_index, 0, null, 0, null);

		if (rm) {
			// 把摸的牌从手牌删掉,结算的时候不显示这张牌的
			GRR._cards_index[seat_index][_logic.switch_to_card_index(operate_card)]--;
		}

		// 显示胡牌
		int cards[] = new int[GameConstants.MAX_COUNT];
		int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[seat_index], cards);
		changeCard(cards);
		int real_card = operate_card;
		if (_logic.is_magic_card(operate_card)) {
			real_card += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
		}
		cards[hand_card_count] = real_card;
		hand_card_count++;
		this.operate_show_card(seat_index, GameConstants.Show_Card_HU, hand_card_count, cards,
				GameConstants.INVALID_SEAT);

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (i == seat_index)
				continue;

			operate_player_cards(i, 0, null, 0, null);

			hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[i], cards);
			changeCard(cards);
			operate_show_card(i, GameConstants.Show_Card_HU, hand_card_count, cards, GameConstants.INVALID_SEAT);
		}

		return;
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
	public boolean estimate_player_out_card_respond(int seat_index, int card, int type) {
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
			int cards[] = new int[GameConstants.MAX_COUNT];
			_logic.switch_to_cards_data(GRR._cards_index[i], cards);

			playerStatus = _playerStatus[i];

			//// 碰牌判断
			action = _logic.check_peng(GRR._cards_index[i], card);
			if (action != 0 && !_playerStatus[i].is_bao_ting() && _playerStatus[i].is_chi_peng_round()) {
				playerStatus.add_action(action);
				playerStatus.add_peng(card, seat_index);
				bAroseAction = true;
			}

			// 杠牌判断
			if (GRR._left_card_count > 1) {
				action = _logic.estimate_gang_card_out_card(GRR._cards_index[i], card);
				if (action != 0 ) {
					if(!_playerStatus[i].is_bao_ting()){
						playerStatus.add_action(GameConstants.WIK_GANG);
						playerStatus.add_gang(card, seat_index, 1);// 加上杠
						bAroseAction = true;
					}else if(!check_gang_huan_zhang(i, card)){
						playerStatus.add_action(GameConstants.WIK_GANG);
						playerStatus.add_gang(card, seat_index, 1);// 加上杠
						bAroseAction = true;
					}
				}
			}

			if (type == GameConstants.INVALID_SEAT) {
				type = GameConstants.HU_CARD_TYPE_PAOHU;
			}
			if (_playerStatus[i].is_bao_ting()) {
				if (_playerStatus[i].is_chi_hu_round()) {
					// 吃胡判断
					ChiHuRight chr = GRR._chi_hu_rights[i];
					chr.set_empty();
					int cbWeaveCount = GRR._weave_count[i];
					action = analyse_chi_hu_card(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr,
							type, i);

					// 结果判断
					if (action != 0) {
						_playerStatus[i].add_action(GameConstants.WIK_CHI_HU);
						_playerStatus[i].add_chi_hu(card, seat_index);// 吃胡的组合
						bAroseAction = true;
					}
				}
			}
		}

		if (bAroseAction)

		{
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
				ChiHuRight chr = GRR._chi_hu_rights[i];// playerStatus._chiHuRight;
				chr.set_empty();
				int cbWeaveCount = GRR._weave_count[i];
				action = analyse_chi_hu_card(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr,
						GameConstants.HU_CARD_TYPE_QIANGGANG, i);

				// 结果判断
				if (action != 0) {
					_playerStatus[i].add_action(GameConstants.WIK_CHI_HU);
					_playerStatus[i].add_chi_hu(card, seat_index);// 吃胡的组合
					chr.opr_or(GameConstants.CHR_HUNAN_QIANG_GANG_HU);// 抢杠胡
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
	protected boolean on_handler_game_finish(int seat_index, int reason) {
		int real_reason = reason;

		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
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
			game_end.setRoundOverType(1);
			game_end.setStartTime(GRR._start_time);

			game_end.setGameTypeIndex(GRR._game_type_index);
			roomResponse.setLeftCardCount(GRR._left_card_count);

			for (int i = 0; i < GRR._especial_card_count; i++) {
				game_end.addEspecialShowCards(GRR._especial_show_cards[i]);
			}

			GRR._end_type = reason;

			// 杠牌，每个人的分数
			float lGangScore[] = new float[getTablePlayerNumber()];
			int mingGangScore[] = new int[getTablePlayerNumber()]; // 明杆得分
			int zhiGangScore[] = new int[getTablePlayerNumber()]; // 直杆得分
			int anGangScore[] = new int[getTablePlayerNumber()]; // 暗杠得分
			int isBaoGangSeat = GameConstants.INVALID_SEAT;
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (has_rule(GameConstants_SXKDD.GAME_RULE_MJ_SXKDD_DIAN_PAO_BAO_GANG)
						&& !(GRR._chi_hu_rights[i].opr_and(GameConstants.CHR_FANG_PAO)).is_empty()
						&& !_playerStatus[i].is_bao_ting()) {
					isBaoGangSeat = i;
				}
			}

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				// 荒张荒杠
				if (reason != GameConstants.Game_End_DRAW && reason != GameConstants.Game_End_RELEASE_PLAY
						&& reason != GameConstants.Game_End_RELEASE_NO_BEGIN
						&& reason != GameConstants.Game_End_RELEASE_RESULT
						&& reason != GameConstants.Game_End_RELEASE_PLAY_TIME_OUT
						&& reason != GameConstants.Game_End_RELEASE_WAIT_TIME_OUT
						&& reason != GameConstants.Game_End_RELEASE_SYSTEM) {
					boolean needGangScore = true;
					if (has_rule(GameConstants_SXKDD.GAME_RULE_MJ_SXKDD_DIAN_GANG_BAO_GANG)
							&& !(GRR._chi_hu_rights[i].opr_and(GameConstants.CHR_FANG_PAO)).is_empty()
							&& !_playerStatus[i].is_bao_ting()) {
						needGangScore = false;
					}

					if (needGangScore) {
						for (int j = 0; j < GRR._weave_count[i]; j++) {
							if (GRR._weave_items[i][j].weave_kind == GameConstants.WIK_GANG) {
								if (GRR._weave_items[i][j].type == GameConstants.GANG_TYPE_JIE_GANG) {
									int score = GRR._weave_items[i][j].weave_score
											* getCardNumber(GRR._weave_items[i][j].center_card);
									if (has_rule(GameConstants_SXKDD.GAME_RULE_MJ_SXKDD_DIAN_GANG_BAO_GANG)) {
										if (!GRR._weave_items[i][j].can_add_gang) { // 报听
											for (int u = 0; u < getTablePlayerNumber(); u++) {
												if (u == i) {
													continue;
												}
												zhiGangScore[u] -= score;
												zhiGangScore[i] += score;
											}
										} else {
											zhiGangScore[GRR._weave_items[i][j].provide_player] -= score;
											zhiGangScore[i] += score;
										}
									} else {
										if (isBaoGangSeat != GameConstants.INVALID_SEAT) {
											zhiGangScore[isBaoGangSeat] -= score;
											zhiGangScore[i] += score;
										} else {
											zhiGangScore[GRR._weave_items[i][j].provide_player] -= score;
											zhiGangScore[i] += score;
										}
									}
								} else {
									for (int k = 0; k < getTablePlayerNumber(); k++) {
										int score = GRR._weave_items[i][j].weave_score
												* getCardNumber(GRR._weave_items[i][j].center_card);
										if (k == i) {
											continue;
										}
										int index = k;
										if (isBaoGangSeat != GameConstants.INVALID_SEAT) {
											index = isBaoGangSeat;
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
					}
				}

				// 记录
				for (int j = 0; j < getTablePlayerNumber(); j++) {
					_player_result.lost_fan_shu[i][j] += GRR._lost_fan_shu[i][j];
				}

			}
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				lGangScore[i] = mingGangScore[i] + zhiGangScore[i] + anGangScore[i]; // 暗杠得分
			}
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				GRR._game_score[i] += lGangScore[i];

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
					hc.addItem(changeCard(GRR._chi_hu_card[i][j]));
				}

				game_end.addHuCardData(changeCard(GRR._chi_hu_card[i][0]));
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

					cs.addItem(changeCard(GRR._cards_data[i][j]));
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

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_player_result.biaoyan[i] = 0;
		}

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

	public boolean operate_player_info(int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);
		roomResponse.setGameStatus(_game_status);

		if (GRR != null)
			GRR.add_room_response(roomResponse);

		if (seat_index == GameConstants.INVALID_SEAT) {

			send_response_to_room(roomResponse);
		} else {
			send_response_to_player(seat_index, roomResponse);
		}

		return true;
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
	public boolean operate_player_cards_with_ting(int seat_index, int card_count, int cards[], int weave_count,
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
			if (_logic.is_magic_card(_playerStatus[seat_index]._hu_out_card_ting[i])) {
				roomResponse.addOutCardTing(
						_playerStatus[seat_index]._hu_out_card_ting[i] + GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI);
			} else {
				roomResponse.addOutCardTing(
						_playerStatus[seat_index]._hu_out_card_ting[i] + GameConstants.CARD_ESPECIAL_TYPE_TING_XN);
			}

			Int32ArrayResponse.Builder int_array = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < ting_card_cout; j++) {
				int_array.addItem(_playerStatus[seat_index]._hu_out_cards[i][j]);
			}
			roomResponse.addOutCardTingCards(int_array);
		}

		// 自己才有牌数据
		this.send_response_to_player(seat_index, roomResponse);

		GRR.add_room_response(roomResponse);

		return true;
	}

	@Override
	public boolean runnable_remove_middle_cards_general(int seat_index) {
		if ((_game_status == GameConstants.GS_MJ_FREE || _game_status == GameConstants.GS_MJ_WAIT) && is_sys())
			return false;
		// // 去掉
		// this.operate_show_card(seat_index, GameConstants.Show_Card_Center, 0,
		// null, GameConstants.INVALID_SEAT);

		// 刷新有癞子的牌
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			boolean has_lai_zi = false;
			for (int j = 0; j < GameConstants.MAX_INDEX; j++) {
				if (GRR._cards_index[i][j] > 0 && _logic.is_magic_index(j)) {
					has_lai_zi = true;
					break;
				}
			}
			if (has_lai_zi) {
				// 刷新自己手牌
				int cards[] = new int[GameConstants.MAX_COUNT];
				int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[i], cards);
				this.changeCard(cards);
				this.operate_player_cards(i, hand_card_count, cards, 0, null);
			}
		}
		// 检测听牌
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			this._playerStatus[i]._hu_card_count = get_ting_card(this._playerStatus[i]._hu_cards,
					this.GRR._cards_index[i], this.GRR._weave_items[i], this.GRR._weave_count[i], true, i);
			if (this._playerStatus[i]._hu_card_count > 0) {
				this.operate_chi_hu_cards(i, this._playerStatus[i]._hu_card_count, this._playerStatus[i]._hu_cards);
			}
		}

		return this.exe_dispatch_card(seat_index, GameConstants.WIK_NULL, 0);
	}

	/**
	 * 检查杠牌后是否换章
	 * 
	 * @param seat_index
	 * @param card
	 * @return
	 */
	public boolean check_gang_huan_zhang(int seat_index, int card) {
		// 不能换章，需要检测是否改变了听牌
		int gang_card_index = _logic.switch_to_card_index(card);
		int gang_card_count = GRR._cards_index[seat_index][gang_card_index];
		// 假如杠了
		GRR._cards_index[seat_index][gang_card_index] = 0;

		int hu_cards[] = new int[GameConstants.MAX_INDEX];

		// 检查听牌
		int hu_card_count = get_ting_card(hu_cards, GRR._cards_index[seat_index], GRR._weave_items[seat_index],
				GRR._weave_count[seat_index], true, seat_index);

		// 还原手牌
		GRR._cards_index[seat_index][gang_card_index] = gang_card_count;

		if (hu_card_count != _playerStatus[seat_index]._hu_card_count) {
			return true;
		} else {
			for (int j = 0; j < hu_card_count; j++) {
				int real_card = get_real_card(_playerStatus[seat_index]._hu_cards[j]);
				if (real_card != get_real_card(hu_cards[j])) {
					return true;
				}
			}
		}

		return false;
	}

	/**
	 * 手中癞子牌角标
	 * 
	 * @param cards
	 */
	public void changeCard(int cards[]) {
		for (int j = 0; j < cards.length; j++) {
			if (cards[j] == 0) {
				continue;
			}
			if (_logic.is_magic_card(cards[j])) {
				cards[j] += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
			}
		}
	}

	/**
	 * 手中癞子牌角标
	 * 
	 * @param cards
	 */
	public int changeCard(int cards) {
		if (_logic.is_magic_card(cards)) {
			cards += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
		}

		return cards;
	}

	public void changeCard(int cards[], int tingCards[], int tingCardCount) {
		for (int j = 0; j < cards.length; j++) {
			if (cards[j] == 0) {
				continue;
			}
			if (_logic.is_magic_card(cards[j])) {
				cards[j] += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
			} else {
				for (int k = 0; k < tingCardCount; k++) {
					if (tingCards[k] == cards[j]) {
						cards[j] += GameConstants.CARD_ESPECIAL_TYPE_TING;
					}
				}
			}
		}
	}

	@Override
	public void test_cards() {
		int[] cards_of_player0 = new int[] { 0x06,0x06,0x06,0x07,0x07,0x07,0x08,0x35,0x35,0x35,0x05,0x08,0x08 };
		int[] cards_of_player1 = new int[] {0x06,0x06,0x06,0x07,0x07,0x07,0x08,0x35,0x35,0x35,0x05,0x08,0x08};
		int[] cards_of_player3 = new int[] {0x06,0x06,0x06,0x07,0x07,0x07,0x08,0x35,0x35,0x35,0x05,0x08,0x08};
		int[] cards_of_player2 = new int[] { 0x06,0x06,0x06,0x07,0x07,0x07,0x08,0x35,0x35,0x35,0x05,0x08,0x08};

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			for (int j = 0; j < GameConstants.MAX_INDEX; j++) {
				GRR._cards_index[i][j] = 0;
			}
		}

		for (int j = 0; j < cards_of_player0.length; j++) {
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
	 * 取出实际牌数据
	 * 
	 * @param card
	 * @return
	 */
	public int get_real_card(int card) {
		// 错误断言
		if (card > GameConstants.CARD_ESPECIAL_TYPE_GUI && card < GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI) {
			card -= GameConstants.CARD_ESPECIAL_TYPE_GUI;
		} else if (card > GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI && card < GameConstants.CARD_ESPECIAL_TYPE_DING_GUI) {
			card -= GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;

		} else if (card > GameConstants.CARD_ESPECIAL_TYPE_DING_GUI
				&& card < GameConstants.CARD_ESPECIAL_TYPE_LAI_GEN) {
			card -= GameConstants.CARD_ESPECIAL_TYPE_DING_GUI;

		} else if (card > GameConstants.CARD_ESPECIAL_TYPE_LAI_GEN && card < GameConstants.CARD_ESPECIAL_TYPE_TING) {
			card -= GameConstants.CARD_ESPECIAL_TYPE_LAI_GEN;

		} else if (card > GameConstants.CARD_ESPECIAL_TYPE_TING && card < GameConstants.CARD_ESPECIAL_TYPE_BAO_TING) {
			card -= GameConstants.CARD_ESPECIAL_TYPE_TING;

		} else if (card > GameConstants.CARD_ESPECIAL_TYPE_BAO_TING && card < GameConstants.CARD_ESPECIAL_TYPE_HUN) {
			card -= GameConstants.CARD_ESPECIAL_TYPE_BAO_TING;
		} else if (card > GameConstants.CARD_ESPECIAL_TYPE_HUN && card < GameConstants.CARD_ESPECIAL_TYPE_CI) {
			card -= GameConstants.CARD_ESPECIAL_TYPE_HUN;
		} else if (card == (GameConstants.CARD_ESPECIAL_TYPE_TING + GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI
				+ GameConstants.HZ_MAGIC_CARD)) {// 滑水麻将
													// 特殊听牌和癞子重合叠加
			card -= (GameConstants.CARD_ESPECIAL_TYPE_TING + GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI);
		} else if (card > GameConstants.CARD_ESPECIAL_TYPE_CI && card < GameConstants.CARD_ESPECIAL_TYPE_WANG_BA) {
			card -= GameConstants.CARD_ESPECIAL_TYPE_CI;
		} else if (card > GameConstants.CARD_ESPECIAL_TYPE_WANG_BA
				&& card < GameConstants.CARD_ESPECIAL_TYPE_LIANG_ZHANG) {
			card -= GameConstants.CARD_ESPECIAL_TYPE_WANG_BA;
		} else if (card > GameConstants.CARD_ESPECIAL_TYPE_LIANG_ZHANG
				&& card < GameConstants.CARD_ESPECIAL_TYPE_LIANG_ZHANG_BIAO_TING) {
			card -= GameConstants.CARD_ESPECIAL_TYPE_LIANG_ZHANG;
		} else if (card > GameConstants.CARD_ESPECIAL_TYPE_LIANG_ZHANG_BIAO_TING
				&& card < GameConstants.CARD_ESPECIAL_TYPE_YAOJIU) {
			card -= GameConstants.CARD_ESPECIAL_TYPE_LIANG_ZHANG_BIAO_TING;
		} else if (card > GameConstants.CARD_ESPECIAL_TYPE_YAOJIU && card < GameConstants.CARD_ESPECIAL_TYPE_SHENGPAI) {
			card -= GameConstants.CARD_ESPECIAL_TYPE_YAOJIU;
		} else if (card > GameConstants.CARD_ESPECIAL_TYPE_SHENGPAI
				&& card < GameConstants.CARD_ESPECIAL_TYPE_SHENGPAI_TING) { // 窟窿带神神牌
			card -= GameConstants.CARD_ESPECIAL_TYPE_SHENGPAI;
		} else if (card > GameConstants.CARD_ESPECIAL_TYPE_SHENGPAI_TING
				&& card < GameConstants.CARD_ESPECIAL_TYPE_BAO) { // 窟窿带神神牌
			card -= GameConstants.CARD_ESPECIAL_TYPE_SHENGPAI_TING;
		} else if (card > GameConstants.CARD_ESPECIAL_TYPE_BAO && card < GameConstants.CARD_ESPECIAL_TYPE_JING) { // 瑞金麻将宝牌
			card -= GameConstants.CARD_ESPECIAL_TYPE_BAO;
		} else if (card > GameConstants.CARD_ESPECIAL_TYPE_JING
				&& card < (GameConstants.CARD_ESPECIAL_TYPE_JING + GameConstants.CARD_ESPECIAL_TYPE_TING)) { // 山西乡宁金牌
			card -= GameConstants.CARD_ESPECIAL_TYPE_JING;
		} else if (card > (GameConstants.CARD_ESPECIAL_TYPE_JING + GameConstants.CARD_ESPECIAL_TYPE_TING)) {
			card -= (GameConstants.CARD_ESPECIAL_TYPE_JING + GameConstants.CARD_ESPECIAL_TYPE_TING);
		}

		return card;
	}

}
