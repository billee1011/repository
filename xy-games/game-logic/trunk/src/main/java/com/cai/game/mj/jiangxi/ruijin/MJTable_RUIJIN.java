package com.cai.game.mj.jiangxi.ruijin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.mj.GameConstants_ND;
import com.cai.common.constant.game.mj.GameConstants_RUIJIN;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.Player;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.WeaveItem;
import com.cai.common.util.GameDescUtil;
import com.cai.common.util.PerformanceTimer;
import com.cai.game.mj.AbstractMJTable;
import com.cai.game.mj.MJType;
import com.cai.game.util.AnalyseCardUtil;
import com.cai.service.PlayerServiceImpl;

import protobuf.clazz.Protocol.GameEndResponse;
import protobuf.clazz.Protocol.GameStartResponse;
import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomInfo;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class MJTable_RUIJIN extends AbstractMJTable {

	private int[] dispatchcardNum; // 摸牌次数
	private boolean isCanGenZhuang; // 可以跟庄
	private int genZhuangCount; // 跟庄次数
	private int genZhuangCard; // 跟庄牌
	private int next_seat_index;
	private int shang_ju_zhuang; // 上局庄
	private int liang_zhuang_count = 0; // 连庄次数
	private int xia_zhuang_fen = 0; // 下庄分

	public MJHandlerQiShouSiBao handlerQiShouSiBao;// 四宝
	public MJHandlerBao_RUIJIN handlerBao_RUIJIN; // 选宝

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
			/*if (!isDispatchcardNum(seat_index)) {
				setGenZhuangCount();
			}*/
		} else {
			if (genZhuangCard != card || seat_index != next_seat_index) {
				isCanGenZhuang = false;
			} else {
				next_seat_index = (seat_index + getTablePlayerNumber() + 1) % getTablePlayerNumber();
				if(_cur_banker == next_seat_index){
					setGenZhuangCount();
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
	public void setGenZhuangCount() {
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
	 * 是否第一次摸牌
	 * 
	 * @param seat_index
	 * @return
	 */
	public boolean isDispatchcardNumZhuang(int seat_index) {
		return dispatchcardNum[seat_index] == 1;
	}

	/**
	* 
	*/
	public MJTable_RUIJIN() {
		super(MJType.GAME_TYPE_MJ_RUIJIN);
	}

	private static final long serialVersionUID = 1L;

	@Override
	public int getTablePlayerNumber() {
		return GameConstants.GAME_PLAYER;
	}

	/**
	 * 修改连庄次数
	 */
	public void changeLiangZhuangCount() {
		if ( GRR._banker_player == _cur_banker) {
			liang_zhuang_count++;
		} else {
			xia_zhuang_fen = liang_zhuang_count;
			liang_zhuang_count = 1;
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.cai.game.mj.AbstractMJTable#onInitTable()
	 */
	@Override
	protected void onInitTable() {
		_handler_dispath_card = new MJHandlerDispatchCard_RUIJIN();
		_handler_out_card_operate = new MJHandlerOutCardOperate_RUIJIN();
		_handler_gang = new MJHandlerGang_RUIJIN();
		_handler_chi_peng = new MJHandlerChiPeng_RUIJIN();
		handlerBao_RUIJIN = new MJHandlerBao_RUIJIN();
		handlerQiShouSiBao = new MJHandlerQiShouSiBao();
	}

	/**
	 * 参数初始化
	 */
	protected void onInitParam() {
		dispatchcardNum = new int[GameConstants.GAME_PLAYER];
		isCanGenZhuang = true;
		genZhuangCount = 0;
		if (_cur_round == 1) {
			shang_ju_zhuang = 0;
		}
		_logic.clean_magic_cards();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.cai.game.mj.AbstractMJTable#on_game_start()
	 */
	@Override
	protected boolean on_game_start() {
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
			roomResponse
					.setCurrentPlayer(_current_player == GameConstants.INVALID_SEAT ? _resume_player : _current_player);
			roomResponse.setLeftCardCount(GRR._left_card_count);
			roomResponse.setGameStatus(_game_status);
			roomResponse.setLeftCardCount(GRR._left_card_count);
			this.send_response_to_player(i, roomResponse);
		}
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

		exe_bao(this.GRR._banker_player);
		return true;

	}

	/**
	 * @param seat_index
	 * @param card
	 * @return
	 */
	public boolean exe_bao(int seat_index) {
		// 出牌
		this.set_handler(this.handlerBao_RUIJIN);
		this.handlerBao_RUIJIN.reset_status(seat_index);
		this.handlerBao_RUIJIN.exe(this);

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
	public int analyse_chi_hu_card(int[] cards_index, WeaveItem[] weaveItems, int weave_count, int cur_card,
			ChiHuRight chiHuRight, int card_type, int _seat_index) {

		if (cur_card == 0) {
			return GameConstants.WIK_NULL;
		}

		int[] tmp_hand_cards_index = new int[GameConstants.MAX_INDEX];
		int cur_card_index = _logic.switch_to_card_index(cur_card);
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			tmp_hand_cards_index[i] = cards_index[i];
		}

		if (cur_card_index >= 0) {
			tmp_hand_cards_index[cur_card_index]++;
		}

		// 四宝起飞
		boolean bao_si = false;
		if (this._logic.get_has_jia_bao() ) {
			if(tmp_hand_cards_index[this._logic.get_magic_card_index(0)] == 4){
				bao_si = true;
			}
		} else {
			int baoNum = 0;
			for (int j = 0; j < 4; j++) {
				int index = GameConstants.MAX_ZI_FENG+j;
				if (tmp_hand_cards_index[index] > 0) {
					baoNum++;
				}
			}
			if (baoNum == 4) {
				bao_si = true;
			}
		}

		if (bao_si && card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
			chiHuRight.opr_or(GameConstants.CHR_HENAN_QISHOU_HU);
			return GameConstants.WIK_CHI_HU;
		}

		int cbChiHuKind = GameConstants.WIK_NULL;
		
		

		if (isDispatchcardNum(_cur_banker) && _seat_index==_cur_banker) {
			if (card_type == GameConstants.HU_CARD_TYPE_PAOHU) {
				chiHuRight.opr_or(GameConstants.CHR_HUNAN_DI_HU);
			} else {
				chiHuRight.opr_or(GameConstants.CHR_HUNAN_TIAN_HU);
			}
		}
		if(_logic.isShiSanLanRJ(tmp_hand_cards_index, weaveItems, weave_count)){
			chiHuRight.opr_or(GameConstants.CHR_HUNAN_SHI_SAN_LAN);
			return GameConstants.WIK_CHI_HU;
		}

		int magic_card_count = _logic.get_magic_card_count();

		if (!_logic.get_has_jia_bao()) { // 一般只有两种癞子牌存在
			magic_card_count = 4;
		}
		int[] magic_cards_index = new int[magic_card_count];
		if (_logic.get_has_jia_bao()) {
			for (int i = 0; i < magic_card_count; i++) {
				magic_cards_index[i] = _logic.get_magic_card_index(i);
			}
		} else {
			int t = 0;
			for (int i = 0 ; i < 4; i++) {
				magic_cards_index[t] = GameConstants.MAX_ZI_FENG+i;
				t++;
			}
		}

		boolean can_win = AnalyseCardUtil.analyse_feng_chi_by_cards_index_ruijin(cards_index,
				_logic.switch_to_card_index(cur_card), magic_cards_index, magic_card_count, GameConstants.MAX_INDEX,
				_logic.get_has_jia_bao(),card_type == GameConstants.HU_CARD_TYPE_ZIMO);

		if (!can_win) {
			chiHuRight.set_empty();
			return GameConstants.WIK_NULL;
		}

		// 飞宝吊
		boolean dandiao = _logic.is_dan_diao_lai(cards_index, cur_card);
		if (dandiao && card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
			for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
				if (cards_index[i] > 0) {
					if ((_logic.get_has_jia_bao() &&  _logic.is_magic_index(i))
							|| (!_logic.get_has_jia_bao() && i >= GameConstants.MAX_ZI_FENG)) {
						chiHuRight.opr_or(GameConstants_ND.CHR_HENAN_DAN_DIAO);
						break;
					}
				}
			}
		}

		cbChiHuKind = GameConstants.WIK_CHI_HU;
		if (card_type == GameConstants.HU_CARD_TYPE_GANG_KAI) {
			chiHuRight.opr_or(GameConstants.CHR_HUNAN_GANG_KAI);
		} else if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
			chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
		} else if (card_type == GameConstants.HU_CARD_TYPE_QIANGGANG) {
			chiHuRight.opr_or(GameConstants.CHR_HUNAN_QIANG_GANG_HU);// 抢杠胡
		} else {
			chiHuRight.opr_or(GameConstants.CHR_SHU_FAN);
		}

		return cbChiHuKind;
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

		// 牌型分数
		int wFanShu = get_chi_hu_fen(chr);
		countCardType(chr, seat_index);
		
		//底分
		int di = GameConstants.CELL_SCORE;
		if (has_rule(GameConstants_RUIJIN.GAME_RULE_DI_2)) {
			di = 2;
		} else if (has_rule(GameConstants_RUIJIN.GAME_RULE_DI_5)) {
			di = 5;
		}

		int lChiHuScore =wFanShu * di;
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
				
				int s = lChiHuScore;

				if(wFanShu != 10 ){
					s += di; //庄家要加一个房间低分番
				}
				if (i == GRR._banker_player || seat_index == GRR._banker_player) {
					if (_cur_banker != GRR._banker_player) { // 下庄给分
						if(xia_zhuang_fen > 0){
							s += xia_zhuang_fen -1;
						}
					} else { // 连庄给分
						if(liang_zhuang_count > 0){
							s += liang_zhuang_count-1;
						}
					}
				}

				// 胡牌分
				GRR._game_score[i] -= s;
				GRR._game_score[seat_index] += s;
			}
		} else {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index) {
					continue;
				}
				
				int s = lChiHuScore;
				if(has_rule(GameConstants_RUIJIN.GAME_RULE_TONG_ZHUANG)){
					if (provide_index == i || i == GRR._banker_player) {
						if(wFanShu != 10 ){
							s += di; //庄家要加一个房间低分番
						}
					}
				}else{
					if (i == GRR._banker_player) {
						s += di; //庄家要加一个房间低分番
					}
				}
				if (i == GRR._banker_player || seat_index == GRR._banker_player) {
					if (_cur_banker != GRR._banker_player) { // 下庄给分
						if(xia_zhuang_fen > 0){
							s += xia_zhuang_fen -1;
						}
					} else { // 连庄给分
						if(liang_zhuang_count > 0){
							s += liang_zhuang_count-1;
						}
					}
				}
				/*else{
					if(has_rule(GameConstants_RUIJIN.GAME_RULE_FEN_ZHUANG_XIANG)){
						s = di; // 分庄闲玩法下，非点炮闲家只计算房间底分
					}
				}*/
				GRR._game_score[i] -= s;
				GRR._game_score[seat_index] += s;
			}

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
	public int get_chi_hu_fen(ChiHuRight chiHuRight) {

		int wFanShu = 1;
	
		// 天胡
		if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_TIAN_HU)).is_empty()) {
			wFanShu = 10;
		}

		// 地胡
		if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_DI_HU)).is_empty()) {
			wFanShu = 10;
		}

		// 四宝
		if (!(chiHuRight.opr_and(GameConstants.CHR_HENAN_QISHOU_HU)).is_empty()) {
			wFanShu = 10;
		}

		// 飞宝吊
		if (!(chiHuRight.opr_and(GameConstants_ND.CHR_HENAN_DAN_DIAO)).is_empty()) {
			wFanShu = 10;
		}
		
		if(wFanShu == 1){
			wFanShu += 1;
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
			StringBuilder des = new StringBuilder();

			l = GRR._chi_hu_rights[i].type_count;

			for (int j = 0; j < l; j++) {
				type = GRR._chi_hu_rights[i].type_list[j];
				if (GRR._chi_hu_rights[i].is_valid()) {
					if (type == GameConstants.CHR_ZI_MO) {
						des.append(" 自摸");
					}
					if (type == GameConstants.CHR_HUNAN_DI_HU) {
						des.append(" 地胡");
					}
					if (type == GameConstants.CHR_HUNAN_TIAN_HU) {
						des.append(" 天胡");
					}
					if (type == GameConstants.CHR_SHU_FAN) {
						des.append(" 接炮");
					}
					/*if (type == GameConstants.CHR_HUNAN_QIANG_GANG_HU) {
						des.append(" 抢杠胡");
					}*/
					if (type == GameConstants.CHR_HENAN_DAN_DIAO) {
						des.append(" 飞宝吊");
					}

					if (type == GameConstants.CHR_HENAN_QISHOU_HU) {
						des.append(" 四宝");
					}

				} else {
					if (type == GameConstants.CHR_FANG_PAO) {
						des.append(" 放炮");
					}
				}
			}

			if (_cur_banker == i && genZhuangCount > 0) {
				des.append(" 跟庄X").append(genZhuangCount);
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
				des.append(" 暗杠X").append(an_gang);
			}
			if (ming_gang > 0) {
				des.append(" 弯杠X").append(ming_gang);
			}
			if (fang_gang > 0) {
				des.append(" 放杠X").append(fang_gang);
			}
			if (jie_gang > 0) {
				des.append(" 明杠X").append(jie_gang);
			}

			GRR._result_des[i] = des.toString();
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

		int l = GameConstants.MAX_INDEX -4;

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
			for(int i = 0; i < _logic.get_magic_card_count(); i++){
				cards[count] = _logic.switch_to_card_data(_logic.get_magic_card_index(i))+GameConstants.CARD_ESPECIAL_TYPE_BAO;
				count++;
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
		boolean bAroseAction = false;

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
			// 碰牌判断
			List<Integer> peng_list = new ArrayList<Integer>();
			action = _logic.check_peng_hua(GRR._cards_index[i], card, peng_list);
			if (action != 0 && _playerStatus[i].card != card) {
				if (peng_list.size() > 0) {
					for (Integer peng : peng_list) {
						playerStatus.add_action(action);
						playerStatus.add_peng(peng, seat_index);
						bAroseAction = true;
					}
				} else {
					playerStatus.add_action(action);
					playerStatus.add_peng(card, seat_index);
					bAroseAction = true;
				}
			}

			if (GRR._left_card_count > 0) {
				List<Integer> gang_list = new ArrayList<Integer>();
				// 杠牌判断
				action = _logic.estimate_gang_card_out_card_hua(GRR._cards_index[i], card,gang_list);
				if (action != 0) {
					
					if(gang_list.size() >0 ){
						for(Integer gang :gang_list){
							playerStatus.add_action(GameConstants.WIK_GANG);
							playerStatus.add_gang(gang, seat_index, GameConstants.PUBLIC_CARD_OPEN);// 加上杠
							bAroseAction = true;
						}
					}else{
						playerStatus.add_action(GameConstants.WIK_GANG);
						playerStatus.add_gang(card, seat_index, GameConstants.PUBLIC_CARD_OPEN);// 加上杠
						bAroseAction = true;
					}
				}
			}

			// 可以胡的情况 判断
			if (_playerStatus[i].is_chi_hu_round() && has_rule(GameConstants_RUIJIN.GAME_RULE_KE_PAO_HU)) {
				// 吃胡判断
				ChiHuRight chr = GRR._chi_hu_rights[i];//
				chr.set_empty();
				int cbWeaveCount = GRR._weave_count[i];
				action = analyse_chi_hu_card(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr,
						GameConstants.HU_CARD_TYPE_PAOHU, seat_index);

				// 结果判断
				if (action != 0) {
					_playerStatus[i].add_action(GameConstants.WIK_CHI_HU);
					_playerStatus[i].add_chi_hu(card, seat_index);// 吃胡的组合
					bAroseAction = true;
				}
			}
		}

		// 吃牌判断
		int chi_seat_index = (seat_index + 1) % getTablePlayerNumber();
		Map<Integer, List<Integer>> chi_map = _logic.check_chi_ruijin(chi_seat_index, GRR._cards_index[chi_seat_index],
				card, _logic.get_has_jia_bao());
		if (chi_map != null) {
			for (Entry<Integer, List<Integer>> entry : chi_map.entrySet()) {
				for (int j = 0; j < entry.getValue().size(); j++) {
					_playerStatus[chi_seat_index].add_action(entry.getKey());
					_playerStatus[chi_seat_index].add_chi(entry.getValue().get(j),entry.getKey(), seat_index);// 吃胡的组合
					bAroseAction = true;
				}
			}
		}

		// 结果判断
		if (_playerStatus[chi_seat_index].has_action()) {
			bAroseAction = true;
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
				ChiHuRight chr = GRR._chi_hu_rights[i];// playerStatus._chiHuRight;
				chr.set_empty();
				int cbWeaveCount = GRR._weave_count[i];
				action = analyse_chi_hu_card(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr,
						GameConstants.HU_CARD_TYPE_QIANGGANG, seat_index);

				// 结果判断
				if (action != 0) {
					_playerStatus[i].add_action(GameConstants.WIK_CHI_HU);
					_playerStatus[i].add_chi_hu(card, seat_index);// 吃胡的组合
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

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if(this._end_reason != GameConstants.Game_End_DRAW){
					for (int j = 0; j < GRR._gang_score[i].gang_count; j++) {
						for (int k = 0; k < getTablePlayerNumber(); k++) {
							lGangScore[k] += GRR._gang_score[i].scores[j][k];// 杠牌，每个人的分数
						}
					}
				}

				// 记录
				for (int j = 0; j < getTablePlayerNumber(); j++) {
					_player_result.lost_fan_shu[i][j] += GRR._lost_fan_shu[i][j];
				}

			}
		
			int di  = 1;
			if (has_rule(GameConstants_RUIJIN.GAME_RULE_DI_2)) {
				di = 2;
			} else if (has_rule(GameConstants_RUIJIN.GAME_RULE_DI_5)) {
				di = 5;
			}
			for(int i = 0; i < getTablePlayerNumber(); i++){
				lGangScore[i] *= di;
			}

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				GRR._game_score[i] += lGangScore[i];
				GRR._game_score[i] += GRR._start_hu_score[i]; // 起手胡分数
				// 跟庄分数
				if (GRR._banker_player != i && this._end_reason != GameConstants.Game_End_DRAW) {
					GRR._game_score[i] += genZhuangCount*di;
					GRR._game_score[GRR._banker_player] -= genZhuangCount*di;
				}
				// 记录
				_player_result.game_score[i] += GRR._game_score[i];

			}

			shang_ju_zhuang = GRR._banker_player;

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
					if(_logic.is_magic_card(GRR._chi_hu_card[i][j])){
						hc.addItem(GRR._chi_hu_card[i][j]+ GameConstants.CARD_ESPECIAL_TYPE_BAO);
					}else{
						hc.addItem(GRR._chi_hu_card[i][j]);
					}
				}
				int card_hu = GRR._chi_hu_card[i][0];
				if(_logic.is_magic_card(card_hu)){
					card_hu += GameConstants.CARD_ESPECIAL_TYPE_BAO;
				}
				game_end.addHuCardData(card_hu);
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
					if(_logic.is_magic_card(GRR._cards_data[i][j])){
						cs.addItem(GRR._cards_data[i][j] + GameConstants.CARD_ESPECIAL_TYPE_BAO);
					}else{
						cs.addItem(GRR._cards_data[i][j]);
					}
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
				game_end.addStartHuScore((int)_player_result.game_score[i]); //小胡分数代表
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


	@Override
	public void test_cards() {
		int[] cards_of_player0 = new int[] { 0x01,0x02,0x03,0x04,0x05,0x06,0x07,0x08,0x09,0x09,0x08,0x07,0x06 };
		int[] cards_of_player1 = new int[] { 0x01,0x02,0x03,0x04,0x05,0x06,0x07,0x08,0x09,0x09,0x08,0x07,0x06 };
		int[] cards_of_player3 = new int[] { 0x01,0x02,0x03,0x04,0x05,0x06,0x07,0x08,0x09,0x09,0x08,0x07,0x06 };
		int[] cards_of_player2 = new int[] { 0x01,0x02,0x03,0x04,0x05,0x06,0x07,0x08,0x09,0x09,0x08,0x07,0x06};

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
				if (GRR._cards_index[i][j] > 0 && _logic.is_magic_index(j)) {
					has_lai_zi = true;
					break;
				}
			}
			if (has_lai_zi) {
				// 刷新自己手牌
				int cards[] = new int[GameConstants.MAX_COUNT];
				int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[i], cards);
				changCard(cards);
				this.operate_player_cards(i, hand_card_count, cards, 0, null);
			}
		}

		// 检测听牌
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			this._playerStatus[i]._hu_card_count = this.get_ting_card(this._playerStatus[i]._hu_cards,
					this.GRR._cards_index[i], this.GRR._weave_items[i], this.GRR._weave_count[i], true, i);
			if (this._playerStatus[i]._hu_card_count > 0) {
				this.operate_chi_hu_cards(i, this._playerStatus[i]._hu_card_count, this._playerStatus[i]._hu_cards);
			}
		}

		boolean is_qishou_hu = false;
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			boolean can_hu = false; // 可以胡
			// 起手4个红中
			if (this._logic.get_has_jia_bao()) {
				if (this.GRR._cards_index[i][this._logic.get_magic_card_index(0)] == 4) {
					can_hu = true;
				}
			} else {
				int baoNum = 0;
				for (int j = GameConstants.MAX_ZI_FENG; j < 4; j++) {
					if (this.GRR._cards_index[i][j] > 0) {
						baoNum++;
					}
				}
				if (baoNum == 4) {
					can_hu = true;
				}
			}

			if (can_hu) {
				this._playerStatus[i].add_action(GameConstants.WIK_ZI_MO);
				this._playerStatus[i].add_zi_mo(this._logic.switch_to_card_data(this._logic.get_magic_card_index(0)),
						i);
				this.GRR._chi_hu_rights[i].opr_or(GameConstants.CHR_ZI_MO);
				this.GRR._chi_hu_rights[i].opr_or(GameConstants.CHR_HENAN_QISHOU_HU);
				this.exe_qishou_si_bao(i);
				is_qishou_hu = true;
				break;
			}
		}
		if (is_qishou_hu == false) {
			this.exe_dispatch_card(this._current_player, GameConstants.WIK_NULL, GameConstants.DELAY_SEND_CARD_DELAY);
		}

	}

	/**
	 * 起手4宝
	 * 
	 * @param seat_index
	 * @return
	 */
	public boolean exe_qishou_si_bao(int seat_index) {
		this.set_handler(this.handlerQiShouSiBao);
		this.handlerQiShouSiBao.reset_status(seat_index);
		this.handlerQiShouSiBao.exe(this);
		return true;
	}

	
	
	public boolean is_bao_pai(int card_data){
		int card_index = _logic.switch_to_card_index(card_data);
		if(_logic.get_has_jia_bao()){
			if(card_index == _logic.get_magic_card_index(0)){
				return true;
			}
		}else{
			for(int i= _logic.switch_to_card_index(0x38); i<_logic.switch_to_card_index(0x38)+4; i++){
				if(card_index == i){
					return true;
				}
			}
		}
		
		return false;
	}
	
	/**
	 * 添加宝牌参数
	 * @param cards
	 */
	public void changCard(int cards[]){
		for(int i = 0; i < GameConstants.MAX_COUNT; i++){
			if(cards[i] == 0 ){
				continue;
			}
			if(is_bao_pai(cards[i])){
				cards[i] += GameConstants.CARD_ESPECIAL_TYPE_BAO;
			}
		}
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
		this.operate_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_HU, chr.type_count, chr.type_list, 1, GameConstants.INVALID_SEAT);

		// 手牌删掉
		this.operate_player_cards(seat_index, 0, null, 0, null);

		if (rm) {
			// 把摸的牌从手牌删掉,结算的时候不显示这张牌的
			GRR._cards_index[seat_index][_logic.switch_to_card_index(operate_card)]--;
		}

		// 显示胡牌
		int cards[] = new int[GameConstants.MAX_COUNT];
		int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[seat_index], cards);
		changCard(cards);
		int card = operate_card; 
		if(is_bao_pai(card)){
			card +=  GameConstants.CARD_ESPECIAL_TYPE_HU_BAO;
		}else{
			card +=  GameConstants.CARD_ESPECIAL_TYPE_HU;
		}
		cards[hand_card_count] =  card;
		hand_card_count++;
		this.operate_show_card(seat_index, GameConstants.Show_Card_HU, hand_card_count, cards, GameConstants.INVALID_SEAT);

		return;
	}
	
	
	
	public int getTwo(int card) {
		return (card & 0xFF0000) >> 16;
	}

	public int getOne(int card) {
		return (card & 0xFF00) >> 8;
	}
	
	public int getThree(int card) {
		return (card & 0xFF000000) >> 24;
	}
	
	public int getOneZ(int card) {
		return (card & 0xFF);
	}
	
}
