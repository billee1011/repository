package com.cai.game.mj.chenchuang.ningxinag;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import protobuf.clazz.Protocol.GameEndResponse;
import protobuf.clazz.Protocol.GameStartResponse;
import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomInfo;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.mj.Constants_MJ_NING_XIANG;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.GangCardResult;
import com.cai.common.domain.Player;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.WeaveItem;
import com.cai.common.util.RandomUtil;
import com.cai.future.GameSchedule;
import com.cai.game.mj.AbstractMJTable;
import com.cai.game.mj.MJType;
import com.cai.game.util.AnalyseCardUtil;
import com.cai.service.PlayerServiceImpl;

/**
 * chenchuang
 * 宁乡麻将
 */
@SuppressWarnings("unchecked")
public class Table_NING_XIANG extends AbstractMJTable {

	private static final long serialVersionUID = 1L;
	public HandlerPao_NING_XIANG _handler_pao;
	
	public int[][] qsh_cards_index = new int[this.getTablePlayerNumber()][GameConstants.MAX_INDEX];
	
	public Map<Long, Integer>[] qi_shou_hu_type = new HashMap[this.getTablePlayerNumber()];
	public Map<Long, Integer>[] ztqi_shou_hu_type = new HashMap[this.getTablePlayerNumber()];
	public Map<Long, Integer>[] has_ztqi_shou_hu_type = new HashMap[this.getTablePlayerNumber()];
	public Set<Integer>[] sx_card = new HashSet[this.getTablePlayerNumber()];//四喜
	public Set<Integer>[] lls_card = new HashSet[this.getTablePlayerNumber()];//六六顺的牌
	public List<Integer>[] qssx_card = new ArrayList[this.getTablePlayerNumber()];//起手四喜
	public List<Integer>[] qslls_card = new ArrayList[this.getTablePlayerNumber()];//起手六六顺的牌
	{for (int i = 0; i < getTablePlayerNumber(); i++){
		qi_shou_hu_type[i] = new HashMap<Long, Integer>();
		ztqi_shou_hu_type[i] = new HashMap<Long, Integer>();
		has_ztqi_shou_hu_type[i] = new HashMap<Long, Integer>();
		sx_card[i] = new HashSet<Integer>();
		lls_card[i] = new HashSet<Integer>();
		qssx_card[i] = new ArrayList<Integer>();
		qslls_card[i] = new ArrayList<Integer>();
	}}
	public boolean[] has_qi_shou_hu = new boolean[getTablePlayerNumber()];		
	public boolean[] is_gang = new boolean[getTablePlayerNumber()];
	public boolean[] is_jia_jiang_gang = new boolean[getTablePlayerNumber()];
	public boolean[] is_can_jia_jiang_jiang = new boolean[getTablePlayerNumber()];
	public int[] gang_mo_cards = new int[getKaiGangNum()];
	public boolean is_judge;
	
	// 有接炮时，不胡，再次存储一下，胡牌时的牌型分，和score_when_abandoned_win进行比较
	public int[] score_when_abandoned_jie_pao = new int[getTablePlayerNumber()]; // 可以胡没有胡记录可以胡的牌型翻数
	public int last_dispatch_card_player;
	
	public int[] fa_pai_count = new int[getTablePlayerNumber()];
	
	public boolean[] is_dao_pai = new boolean[getTablePlayerNumber()];//起手胡牌倒下去了
	
	public int _send_card_data;

	public Table_NING_XIANG() {
		super(MJType.GAME_TYPE_MJ_NING_XIANG);
	}
	@Override
	protected void onInitTable() {
		_handler_chi_peng = new HandlerChiPeng_NING_XIANG();
		_handler_dispath_card = new HandlerDispatchCard_NING_XIANG();
		_handler_gang = new HandlerGang_NING_XIANG();
		_handler_out_card_operate = new HandlerOutCardOperate_NING_XIANG();
		_handler_pao = new HandlerPao_NING_XIANG();
	}
	
	/**第一局建房者为庄家，若为代开房/俱乐部开房（即开房者自己未进入牌局），则随机一位为庄家;*/
	@Override
	protected void initBanker() {
		long id = getCreate_player().getAccount_id();
		if(get_player(id) == null)
			_cur_banker = RandomUtil.getRandomNumber(getTablePlayerNumber());
	}


	/**
	 * 洗完牌执行开始
	 */
	@Override
	protected boolean on_game_start() {
		_send_card_data = 0;
		last_dispatch_card_player = 0;
		gang_mo_cards = new int[getKaiGangNum()];
		score_when_abandoned_jie_pao = new int[getTablePlayerNumber()];
		qi_shou_hu_type = new HashMap[this.getTablePlayerNumber()];
		is_gang = new boolean[getTablePlayerNumber()];
		is_jia_jiang_gang = new boolean[getTablePlayerNumber()];
		is_can_jia_jiang_jiang = new boolean[getTablePlayerNumber()];
		is_judge = false;
		fa_pai_count = new int[getTablePlayerNumber()];
		has_qi_shou_hu = new boolean[getTablePlayerNumber()];
		qsh_cards_index = new int[this.getTablePlayerNumber()][GameConstants.MAX_INDEX];
		is_dao_pai = new boolean[getTablePlayerNumber()];
		for (int i = 0; i < qi_shou_hu_type.length; i++){
			qi_shou_hu_type[i] = new HashMap<Long, Integer>();
			ztqi_shou_hu_type[i] = new HashMap<Long, Integer>();
			has_ztqi_shou_hu_type[i] = new HashMap<Long, Integer>();
			sx_card[i] = new HashSet<Integer>();
			lls_card[i] = new HashSet<Integer>();
			qssx_card[i] = new ArrayList<Integer>();
			qslls_card[i] = new ArrayList<Integer>();
			_player_result.biaoyan[i] = 0;
			_player_result.haspiao[i] = 0;
		}
		//二人模式下，系统每局开始时随机去掉40张牌
		if(getTablePlayerNumber() == 2 && getRuleValue(Constants_MJ_NING_XIANG.GAME_RULE_QU_DIAO_40_CARD) == 1){
			GRR._left_card_count = GRR._left_card_count - 40;
		}
		int paoZi = getPaoZi();
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_player_result.qiang[i] = paoZi;// 清掉 默认是-1
		}
		//飘
		if (has_rule(Constants_MJ_NING_XIANG.GAME_RULE_PIAO_NIAO)) {
			if(_cur_round == 1){
				this.set_handler(this._handler_pao);
				this._handler_pao.exe(this);
				return true;
			}
		}else{
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				_player_result.pao[i] = 0;// 清掉 默认是-1
			}
		}
		show_tou_zi(0);		
		GameStartResponse.Builder gameStartResponse = GameStartResponse.newBuilder();
		gameStartResponse.setBankerPlayer(GRR._banker_player);
		gameStartResponse.setCurrentPlayer(_current_player);
		gameStartResponse.setLeftCardCount(GRR._left_card_count);

		int hand_cards[][] = new int[getTablePlayerNumber()][GameConstants.MAX_COUNT];

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[i], hand_cards[i]);
			gameStartResponse.addCardsCount(hand_card_count);
		}
		//发送给玩家手牌
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

			gameStartResponse.clearCardData();
			for (int j = 0; j < GameConstants.MAX_COUNT; j++) {
				if (_logic.is_magic_card(hand_cards[i][j])) {
					hand_cards[i][j] += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
                }
				gameStartResponse.addCardData(hand_cards[i][j]);
			}

			GRR._video_recode.addHandCards(cards);

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			load_room_info_data(roomResponse);
			load_common_status(roomResponse);
			roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
			roomResponse.setGameStart(gameStartResponse);
			roomResponse.setCurrentPlayer(_current_player == GameConstants.INVALID_SEAT ? _resume_player : _current_player);
			roomResponse.setLeftCardCount(GRR._left_card_count);
			roomResponse.setGameStatus(_game_status);
			roomResponse.setLeftCardCount(GRR._left_card_count);
			send_response_to_player(i, roomResponse);
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
		load_room_info_data(roomResponse);
		load_common_status(roomResponse);
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

		// 检测听牌
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_playerStatus[i]._hu_card_count = get_ting_card(_playerStatus[i]._hu_cards, GRR._cards_index[i], GRR._weave_items[i], GRR._weave_count[i],
					i);
			if (_playerStatus[i]._hu_card_count > 0) {
				if(i != _cur_banker && getRuleValue(Constants_MJ_NING_XIANG.GAME_RULE_BAO_TING) == 1)
					_player_result.biaoyan[i] = 2;
				operate_chi_hu_cards(i, _playerStatus[i]._hu_card_count, _playerStatus[i]._hu_cards);
			}
		}

		//发第一张牌
		exe_dispatch_card(_current_player, GameConstants.GANG_TYPE_HONG_ZHONG, GameConstants.DELAY_SEND_CARD_DELAY);

		return true;
	}
	
	@Override
	public boolean handler_requst_pao_qiang(Player player, int pao, int qiang) {
		if (has_rule(Constants_MJ_NING_XIANG.GAME_RULE_PIAO_NIAO))
			_handler_pao.handler_pao_qiang(this, player.get_seat_index(), pao, qiang);
		return false;
	}
	
	public int getPaoZi(){
		int paoZi = 0;
		if(has_rule(Constants_MJ_NING_XIANG.GAME_RULE_PIAO_1))
			paoZi = 1;
		if(has_rule(Constants_MJ_NING_XIANG.GAME_RULE_PIAO_2))
			paoZi = 2;
		if(has_rule(Constants_MJ_NING_XIANG.GAME_RULE_PIAO_4))
			paoZi = 4;
		return paoZi;
	}
	
	public int getZhongBirdAddScore(){
		int paoZi = 0;
		if(getRuleValue(Constants_MJ_NING_XIANG.GAME_RULE_ZHONG_NIAO_JIA_1) == 1)
			paoZi = 1;
		if(getRuleValue(Constants_MJ_NING_XIANG.GAME_RULE_ZHONG_NIAO_JIA_2) == 1)
			paoZi = 2;
		if(getRuleValue(Constants_MJ_NING_XIANG.GAME_RULE_ZHONG_NIAO_JIA_3) == 1)
			paoZi = 3;
		return paoZi;
	}
	
	public int getBottomScore(){
		if(getRuleValue(Constants_MJ_NING_XIANG.GAME_RULE_BOTTOM_SCORE_2) == 1)
			return 2;
		else 
			return 1;
	}
	
	public int getPaiXingScore(ChiHuRight chr, int seat_index){
		int score = 0;
		if(!chr.opr_and_long(Constants_MJ_NING_XIANG.CHR_PING_HU).is_empty())
			score += getBottomScore() * 2;
		if(!chr.opr_and_long(Constants_MJ_NING_XIANG.CHR_PENG_PENG_HU).is_empty())
			score += 8;
		if(!chr.opr_and_long(Constants_MJ_NING_XIANG.CHR_QING_YI_SE).is_empty())
			score += 8;
		if(!chr.opr_and_long(Constants_MJ_NING_XIANG.CHR_JIANG_JIANG_HU).is_empty())
			score += 8;
		if(!chr.opr_and_long(Constants_MJ_NING_XIANG.CHR_QI_XIAO_DUI).is_empty())
			score += 8;
		if(!chr.opr_and_long(Constants_MJ_NING_XIANG.CHR_HH_QI_XIAO_DUI).is_empty())
			score += 16;
		if(!chr.opr_and_long(Constants_MJ_NING_XIANG.CHR_2HH_QI_XIAO_DUI).is_empty())
			score += 24;
		if(!chr.opr_and_long(Constants_MJ_NING_XIANG.CHR_3HH_QI_XIAO_DUI).is_empty())
			score += 32;
		if(!chr.opr_and_long(Constants_MJ_NING_XIANG.CHR_HAI_DI_LAO_YUE).is_empty())
			score += 8;
		if(!chr.opr_and_long(Constants_MJ_NING_XIANG.CHR_HAI_DI_PAO).is_empty())
			score += 8;
		if(!chr.opr_and_long(Constants_MJ_NING_XIANG.CHR_GANG_SHANG_KAI_HUA).is_empty())
			score += 8;
		if(!chr.opr_and_long(Constants_MJ_NING_XIANG.CHR_GANG_SHANG_PAO).is_empty())
			score += 8;
		if(!chr.opr_and_long(Constants_MJ_NING_XIANG.CHR_QIANG_GANG_HU).is_empty())
			score += 8;
		if(!chr.opr_and_long(Constants_MJ_NING_XIANG.CHR_QUAN_QIU_REN).is_empty())
			score += 8;
		if(!chr.opr_and_long(Constants_MJ_NING_XIANG.CHR_MEN_PING).is_empty())
			score += 8;
		if(!chr.opr_and_long(Constants_MJ_NING_XIANG.CHR_TIAN_HU).is_empty())
			score += 8;
		if(!chr.opr_and_long(Constants_MJ_NING_XIANG.CHR_DI_HU).is_empty())
			score += 8;
		if(_player_result.biaoyan[seat_index] == 1)
			score += 8;
		return score;
	}
	
	public int getPaiXingScore1(ChiHuRight chr, int seat_index, int provide){
		boolean is_zhuang = seat_index == GRR._banker_player || provide == GRR._banker_player;
		int score = 0;
		if(!chr.opr_and_long(Constants_MJ_NING_XIANG.CHR_PING_HU).is_empty())
			score += is_zhuang ? getBottomScore() * 2 : getBottomScore();
		if(!chr.opr_and_long(Constants_MJ_NING_XIANG.CHR_PENG_PENG_HU).is_empty())
			score += is_zhuang ? 7 : 6;
		if(!chr.opr_and_long(Constants_MJ_NING_XIANG.CHR_QING_YI_SE).is_empty())
			score += is_zhuang ? 7 : 6;
		if(!chr.opr_and_long(Constants_MJ_NING_XIANG.CHR_JIANG_JIANG_HU).is_empty())
			score += is_zhuang ? 7 : 6;
		if(!chr.opr_and_long(Constants_MJ_NING_XIANG.CHR_QI_XIAO_DUI).is_empty())
			score += is_zhuang ? 7 : 6;
		if(!chr.opr_and_long(Constants_MJ_NING_XIANG.CHR_HH_QI_XIAO_DUI).is_empty())
			score += is_zhuang ? 14 : 12;
		if(!chr.opr_and_long(Constants_MJ_NING_XIANG.CHR_2HH_QI_XIAO_DUI).is_empty())
			score += is_zhuang ? 21 : 18;
		if(!chr.opr_and_long(Constants_MJ_NING_XIANG.CHR_3HH_QI_XIAO_DUI).is_empty())
			score += is_zhuang ? 28 : 24;
		if(!chr.opr_and_long(Constants_MJ_NING_XIANG.CHR_HAI_DI_LAO_YUE).is_empty())
			score += is_zhuang ? 7 : 6;
		if(!chr.opr_and_long(Constants_MJ_NING_XIANG.CHR_HAI_DI_PAO).is_empty())
			score += is_zhuang ? 7 : 6;
		if(!chr.opr_and_long(Constants_MJ_NING_XIANG.CHR_GANG_SHANG_KAI_HUA).is_empty())
			score += is_zhuang ? 7 : 6;
		if(!chr.opr_and_long(Constants_MJ_NING_XIANG.CHR_GANG_SHANG_PAO).is_empty())
			score += is_zhuang ? 7 : 6;
		if(!chr.opr_and_long(Constants_MJ_NING_XIANG.CHR_QIANG_GANG_HU).is_empty())
			score += is_zhuang ? 7 : 6;
		if(!chr.opr_and_long(Constants_MJ_NING_XIANG.CHR_QUAN_QIU_REN).is_empty())
			score += is_zhuang ? 7 : 6;
		if(!chr.opr_and_long(Constants_MJ_NING_XIANG.CHR_MEN_PING).is_empty())
			score += is_zhuang ? 7 : 6;
		if(!chr.opr_and_long(Constants_MJ_NING_XIANG.CHR_TIAN_HU).is_empty())
			score += is_zhuang ? 7 : 6;
		if(!chr.opr_and_long(Constants_MJ_NING_XIANG.CHR_DI_HU).is_empty())
			score += is_zhuang ? 7 : 6;
		if(_player_result.biaoyan[seat_index] == 1)
			score += is_zhuang ? 7 : 6;
		return score;
	}
	/**
	 * （1）四喜：起牌后，玩家手上已有四张一样的牌，即可胡牌。
	（2）板板胡：起牌后，玩家手上没有一张2、5、8，即可胡牌。
	（3）缺一色：起牌后，玩家手上筒、索、万任缺一门，即可胡牌。
	（4）六六顺：起牌后，玩家手上已有两坎牌，即可胡牌。
	（5）步步高：起牌后，玩家手中同一花色内有三个连着的对子，比如，一对1万，一对2万，一对3万，即可胡牌。
	（6）金童玉女：起牌后，玩家手中有一对2筒和一对2条，即可胡牌。。
	（7）一枝花：起牌后，玩家手中同一花色只有1张牌，且这张牌是5条5筒或者5万，即可胡牌。
	（8）三同：起牌后，玩家手中每个花色都有一对数字相同的牌，比如，一对1万，一对1筒，一对1条，即可胡牌。
	（9）一点红：起牌后，玩家手中只有一张258将牌，即可胡牌。
	（10）后八轮：起牌后，玩家手中有3张8筒，即可起手胡。
	（11）梅花三弄：起牌后，玩家手中有3张5筒，即可起手胡。
	 */
	public boolean is_qi_shou_hu(int[] cards_index ,int seat_index){
		boolean is_qi_shou_hu = false;
		int[] temp_cards_index = Arrays.copyOf(cards_index, cards_index.length);
		if(has_rule(Constants_MJ_NING_XIANG.GAME_RULE_SI_XI)){
			int SI_XI = 0;
			for (int i = 0; i < GameConstants.MAX_ZI; i++) {
				if(temp_cards_index[i] == 4){
					qsh_cards_index[seat_index][i] = 4;
					qssx_card[seat_index].add(_logic.switch_to_card_data(i));
					is_qi_shou_hu = true;
					SI_XI++;
				}
			}
			if(SI_XI > 0)
				qi_shou_hu_type[seat_index].put(Constants_MJ_NING_XIANG.CHR_SI_XI, SI_XI);
		}
		
		if(has_rule(Constants_MJ_NING_XIANG.GAME_RULE_BAN_BAN_HU)){
			if(is_ban_ban_hu(temp_cards_index, null, 0)){
				qsh_cards_index[seat_index] = Arrays.copyOf(cards_index, cards_index.length);
				is_qi_shou_hu = true;
				qi_shou_hu_type[seat_index].put(Constants_MJ_NING_XIANG.CHR_BAN_BAN_HU, 1);
			}
		}
		
		if(has_rule(Constants_MJ_NING_XIANG.GAME_RULE_QUE_YI_SE)){
			int get_se_count = _logic.get_se_count(temp_cards_index, null, 0);
			if(get_se_count != 3){
				qsh_cards_index[seat_index] = Arrays.copyOf(cards_index, cards_index.length);
				is_qi_shou_hu = true;
				qi_shou_hu_type[seat_index].put(Constants_MJ_NING_XIANG.CHR_QUE_YI_SE, 3 - get_se_count);
			}
		}
		
		if(has_rule(Constants_MJ_NING_XIANG.GAME_RULE_LIU_LIU_SHUN)){
			int kanCount = 0;
			int lastdata = 0;
			int lastdataOldcount = 0;
			for (int i = 0; i < GameConstants.MAX_ZI; i++) {
				if(temp_cards_index[i] >= 3){
					lastdataOldcount = qsh_cards_index[seat_index][i];
					if(qsh_cards_index[seat_index][i] < 3)
						qsh_cards_index[seat_index][i] = 3;
					int data = _logic.switch_to_card_data(i);
					lastdata = data;
					qslls_card[seat_index].add(data);
					kanCount++;
				}
			}
			if(kanCount % 2 == 1){
				qsh_cards_index[seat_index][_logic.switch_to_card_index(lastdata)] = lastdataOldcount;
				qslls_card[seat_index].remove(Integer.valueOf(lastdata));
			}
			if(kanCount >= 2){
				is_qi_shou_hu = true;
				qi_shou_hu_type[seat_index].put(Constants_MJ_NING_XIANG.CHR_LIU_LIU_SHUN, kanCount/2);
			}
		}
		
		if(has_rule(Constants_MJ_NING_XIANG.GAME_RULE_BU_BU_GAO)){
			int BU_BU_GAO = 0;
			for (int i = 0; i < GameConstants.MAX_ZI; i++) {
				int value = _logic.get_card_value(_logic.switch_to_card_data(i));
				if(value >= 2 && value <= 8 ){
					if(temp_cards_index[i] >= 2 && temp_cards_index[i-1] >= 2 && temp_cards_index[i+1] >= 2){
						temp_cards_index[i] -= 2;
						temp_cards_index[i-1] -= 2;
						temp_cards_index[i+1] -= 2;
						if(qsh_cards_index[seat_index][i] < 2)
							qsh_cards_index[seat_index][i] = 2;
						if(qsh_cards_index[seat_index][i-1] < 2)
							qsh_cards_index[seat_index][i-1] = 2;
						if(qsh_cards_index[seat_index][i+1] < 2)
							qsh_cards_index[seat_index][i+1] = 2;
						BU_BU_GAO++;
						if(temp_cards_index[i] >= 2 && temp_cards_index[i-1] >= 2 && temp_cards_index[i+1] >= 2){
							temp_cards_index[i] -= 2;
							temp_cards_index[i-1] -= 2;
							temp_cards_index[i+1] -= 2;
							if(qsh_cards_index[seat_index][i] < 4)
								qsh_cards_index[seat_index][i] = 4;
							if(qsh_cards_index[seat_index][i-1] < 4)
								qsh_cards_index[seat_index][i-1] = 4;
							if(qsh_cards_index[seat_index][i+1] < 4)
								qsh_cards_index[seat_index][i+1] = 4;
							BU_BU_GAO++;
						}
					}
				}
			}
			if(BU_BU_GAO > 0){
				is_qi_shou_hu = true;
				qi_shou_hu_type[seat_index].put(Constants_MJ_NING_XIANG.CHR_BU_BU_GAO, BU_BU_GAO);
			}
		}
		
		if(has_rule(Constants_MJ_NING_XIANG.GAME_RULE_JIN_TONG_YU_NV)){
			temp_cards_index = Arrays.copyOf(cards_index, cards_index.length);
			if(temp_cards_index[10] == 4 && temp_cards_index[19] == 4){
				if(qsh_cards_index[seat_index][10] < 4)
					qsh_cards_index[seat_index][10] = 4;
				if(qsh_cards_index[seat_index][19] < 4)
					qsh_cards_index[seat_index][19] = 4;
				is_qi_shou_hu = true;
				qi_shou_hu_type[seat_index].put(Constants_MJ_NING_XIANG.CHR_JIN_TONG_YU_NV, 2);
			}else if(temp_cards_index[10] >= 2 && temp_cards_index[19] >= 2){
				if(qsh_cards_index[seat_index][10] < 2)
					qsh_cards_index[seat_index][10] = 2;
				if(qsh_cards_index[seat_index][19] < 2)
					qsh_cards_index[seat_index][19] = 2;
				is_qi_shou_hu = true;
				qi_shou_hu_type[seat_index].put(Constants_MJ_NING_XIANG.CHR_JIN_TONG_YU_NV, 1);
			}
		}
		
		int[] colorCardCount = new int[3];
		int count258 = 0;
		int index = -1;
		for (int i = 0; i < GameConstants.MAX_ZI; i++) {
			if(temp_cards_index[i] > 0){
				int data = _logic.switch_to_card_data(i);
				int value = _logic.get_card_value(data);
				if(value == 2 || value == 5 || value == 8){
					index = i;
					count258 += temp_cards_index[i];
				}
				int color = _logic.get_card_color(data);
				colorCardCount[color] += temp_cards_index[i];
			}
		}
		
		if(has_rule(Constants_MJ_NING_XIANG.GAME_RULE_YI_ZHI_HUA)){
			int YI_ZHI_HUA = 0;
			for (int i = 0; i < colorCardCount.length; i++) {
				if(colorCardCount[i] == 1 && temp_cards_index[i * 9 + 4] == 1){
					if(qsh_cards_index[seat_index][i * 9 + 4] < 1)
						qsh_cards_index[seat_index][i * 9 + 4] = 1;
					YI_ZHI_HUA++;
				}
			}
			if(YI_ZHI_HUA > 0){
				is_qi_shou_hu = true;
				qi_shou_hu_type[seat_index].put(Constants_MJ_NING_XIANG.CHR_YI_ZHI_HUA, YI_ZHI_HUA);
			}
		}
		
		if(has_rule(Constants_MJ_NING_XIANG.GAME_RULE_SAN_TONG)){
			int SAN_TONG = 0;
			for (int i = 0; i < 9; i++) {
				if(temp_cards_index[i] >= 2 && temp_cards_index[i + 9] >= 2 && temp_cards_index[i + 18] >= 2){
					temp_cards_index[i] -= 2;
					temp_cards_index[i + 9] -= 2;
					temp_cards_index[i + 18] -= 2;
					if(qsh_cards_index[seat_index][i] < 2)
						qsh_cards_index[seat_index][i] = 2;
					if(qsh_cards_index[seat_index][i+9] < 2)
						qsh_cards_index[seat_index][i+9] = 2;
					if(qsh_cards_index[seat_index][i+18] < 2)
						qsh_cards_index[seat_index][i+18] = 2;
					SAN_TONG++;
					if(temp_cards_index[i] >= 2 && temp_cards_index[i + 9] >= 2 && temp_cards_index[i + 18] >= 2){
						temp_cards_index[i] -= 2;
						temp_cards_index[i + 9] -= 2;
						temp_cards_index[i + 18] -= 2;
						if(qsh_cards_index[seat_index][i] < 4)
							qsh_cards_index[seat_index][i] = 4;
						if(qsh_cards_index[seat_index][i+9] < 4)
							qsh_cards_index[seat_index][i+9] = 4;
						if(qsh_cards_index[seat_index][i+18] < 4)
							qsh_cards_index[seat_index][i+18] = 4;
						SAN_TONG++;
					}
				}
			}
			if(SAN_TONG > 0){
				is_qi_shou_hu = true;
				qi_shou_hu_type[seat_index].put(Constants_MJ_NING_XIANG.CHR_SAN_TONG, SAN_TONG);
			}
		}
		
		if(has_rule(Constants_MJ_NING_XIANG.GAME_RULE_YI_DIAN_HONG)){
			if(count258 == 1){
				if(qsh_cards_index[seat_index][index] < 1)
					qsh_cards_index[seat_index][index] = 1;
				is_qi_shou_hu = true;
				qi_shou_hu_type[seat_index].put(Constants_MJ_NING_XIANG.CHR_YI_DIAN_HONG, 1);
			}
		}
		if(getRuleValue(Constants_MJ_NING_XIANG.GAME_RULE_HOU_BA_LUN) == 1){
			if(temp_cards_index[25] >= 3){
				if(qsh_cards_index[seat_index][25] < 3)
					qsh_cards_index[seat_index][25] = 3;
				is_qi_shou_hu = true;
				qi_shou_hu_type[seat_index].put(Constants_MJ_NING_XIANG.CHR_HOU_BA_LUN, 1);
			}
		}
		if(getRuleValue(Constants_MJ_NING_XIANG.GAME_RULE_MEI_HUA_SAN_NONG) == 1){
			if(temp_cards_index[22] >= 3){
				if(qsh_cards_index[seat_index][22] < 3)
					qsh_cards_index[seat_index][22] = 3;
				is_qi_shou_hu = true;
				qi_shou_hu_type[seat_index].put(Constants_MJ_NING_XIANG.CHR_MEI_HUA_SAN_NONG, 1);
			}
		}
		return is_qi_shou_hu;
	}
	
	public void zt_qsh_score(int seat_index,
			int qi_shou_hu_score, int qi_shou_hu_count, long wik_action) {
		for (int j = 0; j < getTablePlayerNumber(); j++) {
			if(j == seat_index)
				continue;
			int qi_shou_hu_score1 = qi_shou_hu_score;
			if(getRuleValue(Constants_MJ_NING_XIANG.GAME_RULE_HU_PAI_WEI_ZHUANG) == 1 || j == _cur_banker || seat_index == _cur_banker)
				qi_shou_hu_score1 *= 2;
			if(getRuleValue(Constants_MJ_NING_XIANG.GAME_RULE_QSH_ADD_BIRD) == 1){
				qi_shou_hu_score1 += (_player_result.pao[j] +_player_result.pao[seat_index] + _player_result.qiang[j] + _player_result.qiang[seat_index]) * qi_shou_hu_count;
			}
			GRR._start_hu_score[j] -= qi_shou_hu_score1;
			_player_result.game_score[j] -= qi_shou_hu_score1;
			GRR._start_hu_score[seat_index] += qi_shou_hu_score1;
			_player_result.game_score[seat_index] += qi_shou_hu_score1;
		}
		
		operate_player_info();
		ztqi_shou_hu_player_operate(seat_index, wik_action);
	}
	
	public boolean is_zt_qi_shou_hu(int[] cards_index, int seat_index, boolean is_gong_shi_card){
		if(is_gang[seat_index] && getRuleValue(Constants_MJ_NING_XIANG.GAME_RULE_ONLY_GNAG_MENY) != 1)
			return false;
		ztqi_shou_hu_type[seat_index].clear();
		qssx_card[seat_index].clear();
		qslls_card[seat_index].clear();
		qsh_cards_index = new int[this.getTablePlayerNumber()][GameConstants.MAX_INDEX];
		int[] temp_cards_index = Arrays.copyOf(cards_index, cards_index.length);
		if(has_rule(Constants_MJ_NING_XIANG.GAME_RULE_ZT_SI_XI)){
			int SI_XI = 0;
			for (int i = 0; i < GameConstants.MAX_ZI; i++) {
				if(temp_cards_index[i] == 4){
					int data = _logic.switch_to_card_data(i);
					if(!sx_card[seat_index].contains(data)){
						qsh_cards_index[seat_index][i] = 4;
						qssx_card[seat_index].add(data);
						SI_XI++;
					}
				}
			}
			if(SI_XI > 0){
				ztqi_shou_hu_type[seat_index].put(Constants_MJ_NING_XIANG.CHR_ZT_SI_XI, SI_XI);
				_playerStatus[seat_index].add_action(Constants_MJ_NING_XIANG.WIK_ZT_SI_XI);
			}
		}
		
		if(has_rule(Constants_MJ_NING_XIANG.GAME_RULE_ZT_LIU_LIU_SHUN)){
			LinkedList<Integer> llscard = new LinkedList<Integer>();
			for (int i = 0; i < GameConstants.MAX_ZI; i++) {
				if(temp_cards_index[i] >= 3){
					int data = _logic.switch_to_card_data(i);
					if(!lls_card[seat_index].contains(data)){
						if(is_gong_shi_card){
							temp_cards_index[i] -= 3;
							if(GameConstants.WIK_CHI_HU == analyse_chi_hu_card(temp_cards_index, GRR._weave_items[seat_index],
									GRR._weave_count[seat_index], -1, GRR._chi_hu_rights[seat_index], GameConstants.WIK_ZI_MO, seat_index))
								llscard.add(data);
							temp_cards_index[i] += 3;
						}else{
							llscard.add(data);
						}
					}
				}
			}
			if(llscard.size() >= 2){
				if(llscard.size() % 2 == 1){
					llscard.removeLast();
				}
				for (Integer card : llscard) {
					int i = _logic.switch_to_card_index(card);
					if(qsh_cards_index[seat_index][i] < 3)
						qsh_cards_index[seat_index][i] = 3;
				}
				qslls_card[seat_index].addAll(llscard);
				ztqi_shou_hu_type[seat_index].put(Constants_MJ_NING_XIANG.CHR_ZT_LIU_LIU_SHUN, llscard.size()/2);
				_playerStatus[seat_index].add_action(Constants_MJ_NING_XIANG.WIK_ZT_LIU_LIU_SHUN);
			}
		}
		return !ztqi_shou_hu_type[seat_index].isEmpty();
	}

	@Override
	public int analyse_chi_hu_card(int[] cards_index, WeaveItem[] weaveItems, int weave_count, int cur_card, ChiHuRight chiHuRight, int card_type,
			int _seat_index) {
		if (card_type == Constants_MJ_NING_XIANG.HU_CARD_TYPE_ZI_MO) {
			chiHuRight.opr_or_long(Constants_MJ_NING_XIANG.CHR_ZI_MO);//自摸
		} else if (card_type == Constants_MJ_NING_XIANG.HU_CARD_TYPE_QIANG_GANG) {
			chiHuRight.opr_or_long(Constants_MJ_NING_XIANG.CHR_QIANG_GANG_HU);//抢杠胡
		}else if (card_type == Constants_MJ_NING_XIANG.HU_CARD_TYPE_DIAN_PAO) {
			chiHuRight.opr_or_long(Constants_MJ_NING_XIANG.CHR_DIAN_PAO_HU);//点炮胡
		}
		if(is_jia_jiang_gang[_seat_index])
			return GameConstants.WIK_NULL;
		//拷贝把当前牌加入手牌
		int cur_card_index = cur_card > 0 ? _logic.switch_to_card_index(cur_card) : -1;
		int[] temp_cards_index = Arrays.copyOf(cards_index, cards_index.length);
		if(cur_card > 0)
			temp_cards_index[cur_card_index]++;
		//门清
		boolean is_men_qing = has_rule(Constants_MJ_NING_XIANG.GAME_RULE_MEN_QING) && card_type == Constants_MJ_NING_XIANG.HU_CARD_TYPE_ZI_MO && is_men_qing1(weaveItems, weave_count, chiHuRight);
		if(is_men_qing){
			chiHuRight.opr_or_long(Constants_MJ_NING_XIANG.CHR_MEN_PING);
		}
		//七对
		int is_xiao_qi_dui = is_xiao_qi_dui(temp_cards_index, weave_count);
		if(is_xiao_qi_dui == 3){
			chiHuRight.opr_or_long(Constants_MJ_NING_XIANG.CHR_3HH_QI_XIAO_DUI);
		}else if(is_xiao_qi_dui == 2){
			chiHuRight.opr_or_long(Constants_MJ_NING_XIANG.CHR_2HH_QI_XIAO_DUI);
		}else if(is_xiao_qi_dui == 1){
			chiHuRight.opr_or_long(Constants_MJ_NING_XIANG.CHR_HH_QI_XIAO_DUI);
		}else if(is_xiao_qi_dui == 0){
			chiHuRight.opr_or_long(Constants_MJ_NING_XIANG.CHR_QI_XIAO_DUI);
		}
		
		boolean is_jiang_jiang_hu = is_jiang_jiang_hu(temp_cards_index, weaveItems, weave_count);
		if(is_jiang_jiang_hu)
			chiHuRight.opr_or_long(Constants_MJ_NING_XIANG.CHR_JIANG_JIANG_HU);
		//清一色2倍
		boolean is_qing_yi_se = _logic.is_qing_yi_se(cards_index, weaveItems, weave_count, cur_card);
		if(is_qing_yi_se)
			chiHuRight.opr_or_long(Constants_MJ_NING_XIANG.CHR_QING_YI_SE);
        //是否胡牌牌型
  		boolean analyse_win_by_cards_index = AnalyseCardUtil.analyse_win_by_cards_index(cards_index, cur_card_index, null, 0);
  		if(!(analyse_win_by_cards_index || is_xiao_qi_dui != -1 || is_jiang_jiang_hu)){
  			return GameConstants.WIK_NULL;
  		}
  		boolean is_all_pg = is_all_pg(weaveItems, weave_count);//全部是碰杠
  		//碰碰胡
		boolean is_peng_peng_hu = !_logic.exist_eat(weaveItems, weave_count) && AnalyseCardUtil.analyse_peng_hu_by_cards_index(cards_index, cur_card_index, null, 0);
		if(is_peng_peng_hu){
			chiHuRight.opr_or_long(Constants_MJ_NING_XIANG.CHR_PENG_PENG_HU);
		}
		
		if(weave_count == 4 && is_all_pg){
			chiHuRight.opr_or_long(Constants_MJ_NING_XIANG.CHR_QUAN_QIU_REN);
		}
		if(!(is_peng_peng_hu || is_qing_yi_se || is_jiang_jiang_hu || is_xiao_qi_dui != -1 ||
				!chiHuRight.opr_and_long(Constants_MJ_NING_XIANG.CHR_HAI_DI_LAO_YUE).is_empty()||
				!chiHuRight.opr_and_long(Constants_MJ_NING_XIANG.CHR_HAI_DI_PAO).is_empty()||
				!chiHuRight.opr_and_long(Constants_MJ_NING_XIANG.CHR_GANG_SHANG_KAI_HUA).is_empty()||
				!chiHuRight.opr_and_long(Constants_MJ_NING_XIANG.CHR_GANG_SHANG_PAO).is_empty()||
				!chiHuRight.opr_and_long(Constants_MJ_NING_XIANG.CHR_QIANG_GANG_HU).is_empty()||
				!chiHuRight.opr_and_long(Constants_MJ_NING_XIANG.CHR_QUAN_QIU_REN).is_empty()||
				_player_result.biaoyan[_seat_index] == 1)){
			boolean analyse_258_by_cards_index = AnalyseCardUtil.analyse_258_by_cards_index(cards_index, cur_card_index, null, 0);
			if(card_type == 100 && getRuleValue(Constants_MJ_NING_XIANG.GAME_RULE_JIA_JIANG_HU) == 1){
				if(!analyse_258_by_cards_index)
					chiHuRight.opr_or_long(Constants_MJ_NING_XIANG.CHR_JIA_JIANG_HU);//假将胡
			}else{
				if(!analyse_258_by_cards_index)
					return GameConstants.WIK_NULL;
			}
		}
		if(!(is_peng_peng_hu || is_qing_yi_se || is_jiang_jiang_hu || is_xiao_qi_dui != -1 ||
				!chiHuRight.opr_and_long(Constants_MJ_NING_XIANG.CHR_HAI_DI_LAO_YUE).is_empty()||
				!chiHuRight.opr_and_long(Constants_MJ_NING_XIANG.CHR_HAI_DI_PAO).is_empty()||
				!chiHuRight.opr_and_long(Constants_MJ_NING_XIANG.CHR_GANG_SHANG_KAI_HUA).is_empty()||
				!chiHuRight.opr_and_long(Constants_MJ_NING_XIANG.CHR_GANG_SHANG_PAO).is_empty()||
				!chiHuRight.opr_and_long(Constants_MJ_NING_XIANG.CHR_QIANG_GANG_HU).is_empty()||
				!chiHuRight.opr_and_long(Constants_MJ_NING_XIANG.CHR_TIAN_HU).is_empty()||
				!chiHuRight.opr_and_long(Constants_MJ_NING_XIANG.CHR_DI_HU).is_empty()||
				!chiHuRight.opr_and_long(Constants_MJ_NING_XIANG.CHR_MEN_PING).is_empty()||
				!chiHuRight.opr_and_long(Constants_MJ_NING_XIANG.CHR_QUAN_QIU_REN).is_empty()||
				_player_result.biaoyan[_seat_index] == 1)){
			chiHuRight.opr_or_long(Constants_MJ_NING_XIANG.CHR_PING_HU);//平胡
		}
		if(has_rule(Constants_MJ_NING_XIANG.GAME_RULE_PING_HU_BU_KE_JIE_PAO) && card_type == Constants_MJ_NING_XIANG.HU_CARD_TYPE_DIAN_PAO &&
				!chiHuRight.opr_and_long(Constants_MJ_NING_XIANG.CHR_PING_HU).is_empty()){
			return GameConstants.WIK_NULL;
		}
		return GameConstants.WIK_CHI_HU;
	}

	private boolean is_jiang_jiang_hu(int[] temp_cards_index,
			WeaveItem[] weaveItems, int weave_count) {
		for(int i = 0; i < weave_count; i++){
			int value = _logic.get_card_value(weaveItems[i].center_card);
			if(value != 2 && value != 5 && value != 8)
				return false;
		}
		for(int i = 0; i < GameConstants.MAX_ZI; i++){
			if(temp_cards_index[i] != 0){
				int value = _logic.get_card_value(_logic.switch_to_card_data(i));
				if(value != 2 && value != 5 && value != 8)
					return false;
			}
		}
		return true;
	}

	private boolean is_ban_ban_hu(int[] temp_cards_index,
			WeaveItem[] weaveItems, int weave_count) {
		for(int i = 0; i < weave_count; i++){
			int value = _logic.get_card_value(weaveItems[i].center_card);
			if(value == 2 || value == 5 || value == 8)
				return false;
		}
		for(int i = 0; i < GameConstants.MAX_ZI; i++){
			if(temp_cards_index[i] != 0){
				int value = _logic.get_card_value(_logic.switch_to_card_data(i));
				if(value == 2 || value == 5 || value == 8)
					return false;
			}
		}
		return true;
	}

	/**
	 * 判断抢杠胡
	 * @return
	 */
	public boolean estimate_gang_respond(int seat_index, int card, boolean is_qiang_gang) {
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

			// 吃胡判断
			ChiHuRight chr = GRR._chi_hu_rights[i];
			chr.set_empty();
			int cbWeaveCount = GRR._weave_count[i];
			int type = is_qiang_gang ? Constants_MJ_NING_XIANG.HU_CARD_TYPE_QIANG_GANG : Constants_MJ_NING_XIANG.HU_CARD_TYPE_DIAN_PAO;
			action = analyse_chi_hu_card(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr,
					type, i);
			// 结果判断
			if (action != 0) {
			// 可以胡的情况 判断 ,强杠胡不用判断是否过圈
				if (_playerStatus[i].is_chi_hu_round() || getPaiXingScore(chr, seat_index) > score_when_abandoned_jie_pao[seat_index]) {
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
	

	@Override
	public void process_chi_hu_player_score(int seat_index, int provide_index, int operate_card, boolean zimo) {
		this.operate_show_card(seat_index, GameConstants.Show_Card_XiaoHU, 0, null, GameConstants.INVALID_SEAT);
		GRR._chi_hu_card[seat_index][0] = operate_card;

		GRR._win_order[seat_index] = 1;
		// 引用权位
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];


		countCardType(chr, seat_index);
		

		int ma = GRR._player_niao_count[seat_index];//中马的个数
		int piao = _player_result.pao[seat_index];//飘鸟
		int qiang = _player_result.qiang[seat_index];//坐鸟
		int addScore = getZhongBirdAddScore();//中鸟加分
		/////////////////////////////////////////////// 算分//////////////////////////
		/*int lChiHuScore = 0;
		if(has_rule(Constants_MJ_NING_XIANG.GAME_RULE_HIT_BIRD_ADD_DOUBLE))
			lChiHuScore = wFanShu * (1 << ma);
		else
			lChiHuScore = wFanShu + ma;*/
		////////////////////////////////////////////////////// 自摸 算分
		if(getRuleValue(Constants_MJ_NING_XIANG.GAME_RULE_HU_PAI_WEI_ZHUANG) == 1){
			int wFanShu = getPaiXingScore(chr, seat_index);// 番数
			if (zimo) {
				for (int i = 0; i < getTablePlayerNumber(); i++) {
					if (i == seat_index) {
						continue;
					}
					int bt_score = _player_result.biaoyan[i] == 1 ? 8 : 0;
					int score = 0;
					if(has_rule(Constants_MJ_NING_XIANG.GAME_RULE_HIT_BIRD_ADD_DOUBLE)){
						score = (wFanShu + bt_score) * (1 + (GRR._player_niao_count[i] + ma)) + _player_result.pao[i] +piao + _player_result.qiang[i] + qiang;
					}else{
						score = (wFanShu + bt_score) + ((GRR._player_niao_count[i] + ma) * addScore) + _player_result.pao[i] +piao + _player_result.qiang[i] + qiang;
					}
					GRR._game_score[i] -= score;
					GRR._game_score[seat_index] += score;
				}
			}else {
				int score = 0;
				int bt_score = _player_result.biaoyan[provide_index] == 1 ? 8 : 0;
				if(has_rule(Constants_MJ_NING_XIANG.GAME_RULE_HIT_BIRD_ADD_DOUBLE)){
					score = (wFanShu + bt_score) * (1 + (GRR._player_niao_count[provide_index] + ma)) + _player_result.pao[provide_index] + piao + _player_result.qiang[provide_index] + qiang;
				}else{
					score = (wFanShu + bt_score) + ((GRR._player_niao_count[provide_index] + ma) * addScore) + _player_result.pao[provide_index] + piao + _player_result.qiang[provide_index] + qiang;
				}
				GRR._game_score[provide_index] -= score;
				GRR._game_score[seat_index] += score;
			}
		}else{
			if (zimo) {
				for (int i = 0; i < getTablePlayerNumber(); i++) {
					if (i == seat_index) {
						continue;
					}
					int wFanShu = getPaiXingScore1(chr, seat_index, i);
					int bt_score = _player_result.biaoyan[i] == 1 ? 7 : 0;
					int score = 0;
					if(has_rule(Constants_MJ_NING_XIANG.GAME_RULE_HIT_BIRD_ADD_DOUBLE)){
						score = (wFanShu + bt_score) * (1 + (GRR._player_niao_count[i] + ma)) + _player_result.pao[i] +piao + _player_result.qiang[i] + qiang;
					}else{
						score = (wFanShu + bt_score) + ((GRR._player_niao_count[i] + ma) * addScore) + _player_result.pao[i] +piao + _player_result.qiang[i] + qiang;
					}
					GRR._game_score[i] -= score;
					GRR._game_score[seat_index] += score;
				}
			}else {
				int wFanShu = getPaiXingScore1(chr, seat_index, provide_index);
				int score = 0;
				int bt_score = _player_result.biaoyan[provide_index] == 1 ? 7 : 0;
				if(has_rule(Constants_MJ_NING_XIANG.GAME_RULE_HIT_BIRD_ADD_DOUBLE)){
					score = (wFanShu + bt_score) * (1 + (GRR._player_niao_count[provide_index] + ma)) + _player_result.pao[provide_index] + piao + _player_result.qiang[provide_index] + qiang;
				}else{
					score = (wFanShu + bt_score) + ((GRR._player_niao_count[provide_index] + ma) * addScore) + _player_result.pao[provide_index] + piao + _player_result.qiang[provide_index] + qiang;
				}
				GRR._game_score[provide_index] -= score;
				GRR._game_score[seat_index] += score;
			}
		}
		
		// 设置变量
		GRR._provider[seat_index] = provide_index;
		_status_gang = false;
		_status_gang_hou_pao = false;

		change_player_status(seat_index, GameConstants.INVALID_VALUE);
	}

	@Override
	protected void set_result_describe() {
		int chrTypes;
		long type = 0;
		for (int player = 0; player < this.getTablePlayerNumber(); player++) {
			StringBuilder result = new StringBuilder("");

			chrTypes = GRR._chi_hu_rights[player].type_count;

			for (int typeIndex = 0; typeIndex < chrTypes; typeIndex++) {
				type = GRR._chi_hu_rights[player].type_list[typeIndex];

				if (GRR._chi_hu_rights[player].is_valid()) {
					if (type == Constants_MJ_NING_XIANG.CHR_DIAN_PAO_HU) {
						result.append(" 点炮");
					}
					if (type == Constants_MJ_NING_XIANG.CHR_ZI_MO) {
						result.append(" 自摸");
					}
					if (type == Constants_MJ_NING_XIANG.CHR_PING_HU) {
						result.append(" 平胡");
					}
					if (type == Constants_MJ_NING_XIANG.CHR_PENG_PENG_HU) {
						result.append(" 碰碰胡");
					}
					if (type == Constants_MJ_NING_XIANG.CHR_JIANG_JIANG_HU) {
						result.append(" 将将胡");
					}
					if (type == Constants_MJ_NING_XIANG.CHR_QIANG_GANG_HU) {
						result.append(" 抢杠胡");
					}
					if (type == Constants_MJ_NING_XIANG.CHR_QI_XIAO_DUI) {
						result.append(" 七小对");
					}
					if (type == Constants_MJ_NING_XIANG.CHR_HH_QI_XIAO_DUI) {
						result.append(" 豪华七小对");
					}
					if (type == Constants_MJ_NING_XIANG.CHR_2HH_QI_XIAO_DUI) {
						result.append(" 双豪华七小对");
					}
					if (type == Constants_MJ_NING_XIANG.CHR_3HH_QI_XIAO_DUI) {
						result.append(" 三豪华七小对");
					}
					if (type == Constants_MJ_NING_XIANG.CHR_HAI_DI_LAO_YUE) {
						result.append(" 海底捞月");
					}
					if (type == Constants_MJ_NING_XIANG.CHR_HAI_DI_PAO) {
						result.append(" 海底炮");
					}
					if (type == Constants_MJ_NING_XIANG.CHR_QING_YI_SE) {
						result.append(" 清一色");
					}
					if (type == Constants_MJ_NING_XIANG.CHR_QUAN_QIU_REN) {
						result.append(" 全求人");
					}
					if (type == Constants_MJ_NING_XIANG.CHR_MEN_PING) {
						result.append(" 门清");
					}
					if (type == Constants_MJ_NING_XIANG.CHR_TIAN_HU) {
						result.append(" 天胡");
					}
					if (type == Constants_MJ_NING_XIANG.CHR_DI_HU) {
						result.append(" 地胡");
					}
					if (type == Constants_MJ_NING_XIANG.CHR_GANG_SHANG_KAI_HUA) {
						result.append(" 杠上开花");
					}
					if (type == Constants_MJ_NING_XIANG.CHR_GANG_SHANG_PAO) {
						result.append(" 杠上炮");
					}
				} else{
					if (type == Constants_MJ_NING_XIANG.CHR_FANG_PAO)
						result.append(" 放炮");
					if (type == Constants_MJ_NING_XIANG.CHR_BEI_QIANG_GANG)
						result.append(" 被抢杠");
				}
			}

			
			if(GRR._player_niao_count[player] > 0){
				result.append(" 中鸟X"+GRR._player_niao_count[player]);
			}

			if(has_qi_shou_hu[player]){
				/*（1）四喜：起牌后，玩家手上已有四张一样的牌，即可胡牌。
				（2）板板胡：起牌后，玩家手上没有一张2、5、8，即可胡牌。
				（3）缺一色：起牌后，玩家手上筒、索、万任缺一门，即可胡牌。
				（4）六六顺：起牌后，玩家手上已有两坎牌，即可胡牌。
				（5）步步高：起牌后，玩家手中同一花色内有三个连着的对子，比如，一对1万，一对2万，一对3万，即可胡牌。
				（6）金童玉女：起牌后，玩家手中有一对2筒和一对2条，即可胡牌。。
				（7）一枝花：起牌后，玩家手中同一花色只有1张牌，且这张牌是5条5筒或者5万，即可胡牌。
				（8）三同：起牌后，玩家手中每个花色都有一对数字相同的牌，比如，一对1万，一对1筒，一对1条，即可胡牌。
				（9）一点红：起牌后，玩家手中只有一张258将牌，即可胡牌。*/
				for(Entry<Long, Integer> entry : qi_shou_hu_type[player].entrySet()){
					long key = entry.getKey();
					if(key == Constants_MJ_NING_XIANG.CHR_SI_XI){
						result.append(" 四喜");
						if(entry.getValue() > 1)
							result.append("x"+entry.getValue());
					}
					if(key == Constants_MJ_NING_XIANG.CHR_BAN_BAN_HU){
						result.append(" 板板胡");
						if(entry.getValue() > 1)
							result.append("x"+entry.getValue());
					}
					if(key == Constants_MJ_NING_XIANG.CHR_QUE_YI_SE){
						result.append(" 缺一色");
						if(entry.getValue() > 1)
							result.append("x"+entry.getValue());
					}
					if(key == Constants_MJ_NING_XIANG.CHR_LIU_LIU_SHUN){
						result.append(" 六六顺");
						if(entry.getValue() > 1)
							result.append("x"+entry.getValue());
					}
					if(key == Constants_MJ_NING_XIANG.CHR_BU_BU_GAO){
						result.append(" 步步高");
						if(entry.getValue() > 1)
							result.append("x"+entry.getValue());
					}
					if(key == Constants_MJ_NING_XIANG.CHR_JIN_TONG_YU_NV){
						result.append(" 金童玉女");
						if(entry.getValue() > 1)
							result.append("x"+entry.getValue());
					}
					if(key == Constants_MJ_NING_XIANG.CHR_YI_ZHI_HUA){
						result.append(" 一枝花");
						if(entry.getValue() > 1)
							result.append("x"+entry.getValue());
					}
					if(key == Constants_MJ_NING_XIANG.CHR_SAN_TONG){
						result.append(" 三同");
						if(entry.getValue() > 1)
							result.append("x"+entry.getValue());
					}
					if(key == Constants_MJ_NING_XIANG.CHR_YI_DIAN_HONG){
						result.append(" 一点红");
						if(entry.getValue() > 1)
							result.append("x"+entry.getValue());
					}
					if(key == Constants_MJ_NING_XIANG.CHR_HOU_BA_LUN){
						result.append(" 后八轮");
					}
					if(key == Constants_MJ_NING_XIANG.CHR_MEI_HUA_SAN_NONG){
						result.append(" 梅花三弄");
					}
				}
				
			}
			for(Entry<Long, Integer> entry : has_ztqi_shou_hu_type[player].entrySet()){
				Long key = entry.getKey();
				if(key == Constants_MJ_NING_XIANG.CHR_ZT_SI_XI){
					result.append(" 中途四喜");
					if(entry.getValue() > 1)
						result.append("x"+entry.getValue());
				}
				if(key == Constants_MJ_NING_XIANG.CHR_ZT_LIU_LIU_SHUN){
					result.append(" 中途六六顺");
					if(entry.getValue() > 1)
						result.append("x"+entry.getValue());
				}
			}
			if(_player_result.biaoyan[player] == 1)
				result.append(" 报听");
			if(_player_result.pao[player] > 0)
				result.append(" 飘x" + _player_result.pao[player]);
			if(getPaoZi() > 0)
				result.append(" 坐鸟x" + getPaoZi());
			GRR._result_des[player] = result.toString();
		}
	}
	
	
	//private static final int[][] birdVaule = new int[][]{{1,5,9},{2,6},{3,7},{4,8}};
	public void set_pick_direction_niao_cards(int cards_data[], int card_num, int seat_index, int half_index) {
		if(getRuleValue(Constants_MJ_NING_XIANG.GAME_RULE_HU_PAI_WEI_ZHUANG) != 1)
			half_index = GRR._banker_player;
		int seat = (seat_index - half_index + getTablePlayerNumber()) % getTablePlayerNumber();
		for (int i = 0; i < card_num; i++) {
			int nValue = _logic.get_card_value(cards_data[i]);
			boolean flag = false;
			if((seat + 1) % 4 == nValue % 4)
				flag = true;
			/*for(int v = 0; v < birdVaule[seat].length; v++){
				if(nValue == birdVaule[seat][v]){
					flag = true;
					break;
				}
			}*/
			if (flag) {
				GRR._player_niao_cards[seat_index][i] = GRR._cards_data_niao[i] + GameConstants.DING_NIAO_VALID;
				GRR._player_niao_count[seat_index]++;
				_player_result.ming_gang_count[seat_index]++;
			}else{
				GRR._player_niao_cards[seat_index][i] = GRR._cards_data_niao[i];
			}

		}
	}
	
	/**
	 * 设置鸟
	 */
	public void set_niao_card(int seat_index, boolean is_hai_di_hu) {

		for (int i = 0; i < GameConstants.MAX_NIAO_CARD; i++) {
			GRR._cards_data_niao[i] = GameConstants.INVALID_VALUE;
		}

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			GRR._player_niao_count[i] = 0;
			for (int j = 0; j < GameConstants.MAX_NIAO_CARD; j++) {
				GRR._player_niao_cards[i][j] = GameConstants.INVALID_VALUE;
			}
		}

		GRR._show_bird_effect = true;
		int bird_num = get_bird_num();
		ChiHuRight chiHuRight = GRR._chi_hu_rights[seat_index];
		boolean is_zimo = false;
		boolean is_dian = false;
		boolean is_duo = false;
		if(chiHuRight.is_valid()){
			if(!chiHuRight.opr_and_long(Constants_MJ_NING_XIANG.CHR_ZI_MO).is_empty() || 
					!chiHuRight.opr_and_long(Constants_MJ_NING_XIANG.CHR_GANG_SHANG_KAI_HUA).is_empty()){
				is_zimo = true;
			}else{
				is_dian = true;
			}
		}else{
			is_duo = true;
		}
			
		if(is_hai_di_hu){
			GRR._count_niao = bird_num;
			for (int i = 0; i < GRR._count_niao; i++) {
				GRR._cards_data_niao[i] = _repertory_card[_all_card_len - 1];
			}
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if(is_zimo)
					set_pick_direction_niao_cards(GRR._cards_data_niao, GRR._count_niao, i, seat_index);
				if(is_dian && (i == seat_index || !GRR._chi_hu_rights[i].opr_and_long(Constants_MJ_NING_XIANG.CHR_FANG_PAO).is_empty()))
					set_pick_direction_niao_cards(GRR._cards_data_niao, GRR._count_niao, i, seat_index);
				if(is_duo && (i == seat_index || GRR._chi_hu_rights[i].is_valid()))
					set_pick_direction_niao_cards(GRR._cards_data_niao, GRR._count_niao, i, seat_index);
			}
		}else{
			GRR._count_niao = bird_num > GRR._left_card_count ? GRR._left_card_count : bird_num;
			int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
			_logic.switch_to_cards_index(_repertory_card, _all_card_len - GRR._left_card_count, GRR._count_niao, cbCardIndexTemp);
			GRR._left_card_count -= GRR._count_niao;
			_logic.switch_to_cards_data(cbCardIndexTemp, GRR._cards_data_niao);
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if(is_zimo)
					set_pick_direction_niao_cards(GRR._cards_data_niao, GRR._count_niao, i, seat_index);
				if(is_dian && (i == seat_index || !GRR._chi_hu_rights[i].opr_and_long(Constants_MJ_NING_XIANG.CHR_FANG_PAO).is_empty()))
					set_pick_direction_niao_cards(GRR._cards_data_niao, GRR._count_niao, i, seat_index);
				if(is_duo && (i == seat_index || GRR._chi_hu_rights[i].is_valid()))
					set_pick_direction_niao_cards(GRR._cards_data_niao, GRR._count_niao, i, seat_index);
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
	public void qi_shou_hu_player_operate(int seat_index, int last) {
		int size = qi_shou_hu_type[seat_index].size();
		long[] arr = new long[size];
		int l = 0;
		for(long a : qi_shou_hu_type[seat_index].keySet()){
			arr[l] = a;
			l++;
		}
		this.operate_effect_action(seat_index, 3, size, arr, 1, GameConstants.INVALID_SEAT);

		// 手牌删掉
		//qsh_cards_index
		int[] cards_index = Arrays.copyOf(GRR._cards_index[seat_index], GRR._cards_index[seat_index].length);
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			if (qsh_cards_index[seat_index][i] > 0) {
				cards_index[i] -= qsh_cards_index[seat_index][i];
			}
		}
		int cards_data[] = new int[GameConstants.MAX_COUNT];
		int cards_index_count = _logic.switch_to_cards_data(cards_index, cards_data);
		this.operate_player_cards(seat_index, cards_index_count, cards_data, 0, null);

		// 显示胡牌
		int cards[] = new int[GameConstants.MAX_COUNT];
		int hand_card_count = _logic.switch_to_cards_data(qsh_cards_index[seat_index], cards);
		this.operate_show_card(seat_index, GameConstants.Show_Card_XiaoHU, hand_card_count, cards, GameConstants.INVALID_SEAT);
		if(seat_index == last){
			GameSchedule.put(() -> {
			if(!checkBaoTing())
				((HandlerDispatchCard_NING_XIANG) _handler_dispath_card).checkGang(this);
			}, 4, TimeUnit.SECONDS);
		}
		/*GameSchedule.put(() -> {
			int out_ting_count = 0;
			int show_send_card = _provide_card;
			if(hand_card_count == 14){
				boolean flag = false;
				for (int j = 0; j < 13; j++) {
					if(cards[j] == show_send_card){
						flag = true;
					}
					if(flag){
						cards[j] = cards[j+1];
					}
				}
				cards[13] = 0;
				// 出任意一张牌时，能胡哪些牌 -- Begin
				out_ting_count = _playerStatus[seat_index]._hu_out_card_count;
				if (out_ting_count > 0) {
					for (int j = 0; j < 13; j++) {
						for (int k = 0; k < out_ting_count; k++) {
							if (cards[j] == _playerStatus[seat_index]._hu_out_card_ting[k]) {
								cards[j] += GameConstants.CARD_ESPECIAL_TYPE_TING;
								break;
							}
						}
					}
				}
			}
			this.operate_player_cards(seat_index, 13, cards, 0, null);
			if(hand_card_count == 14){
				for (int k = 0; k < out_ting_count; k++){ 
					if (show_send_card == _playerStatus[seat_index]._hu_out_card_ting[k]){
						show_send_card += GameConstants.CARD_ESPECIAL_TYPE_TING;
						break;
					}
				}
				operate_player_get_card(seat_index, 1, new int[] { show_send_card }, GameConstants.INVALID_SEAT);
			}
			this.operate_show_card(seat_index, GameConstants.Show_Card_XiaoHU, 0, null, GameConstants.INVALID_SEAT);
			if(seat_index == last){
				if(!checkBaoTing())
					((HandlerDispatchCard_NING_XIANG) _handler_dispath_card).checkGang(this);
			}
		}, 4, TimeUnit.SECONDS);*/
		return;
	}
	
	public void ztqi_shou_hu_player_operate(int seat_index, long wik_action) {
		this.operate_effect_action(seat_index, 3, 1, new long[]{wik_action}, 1, GameConstants.INVALID_SEAT);

		// 手牌删掉
		int[] cards_index = Arrays.copyOf(GRR._cards_index[seat_index], GRR._cards_index[seat_index].length);
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			if (qsh_cards_index[seat_index][i] > 0) {
				cards_index[i] -= qsh_cards_index[seat_index][i];
			}
		}
		int cards_data[] = new int[GameConstants.MAX_COUNT];
		int cards_index_count = _logic.switch_to_cards_data(cards_index, cards_data);
		this.operate_player_cards(seat_index, cards_index_count, cards_data, 0, null);

		// 显示胡牌
		int cards[] = new int[GameConstants.MAX_COUNT];
		int hand_card_count = _logic.switch_to_cards_data(qsh_cards_index[seat_index], cards);
		this.operate_show_card(seat_index, GameConstants.Show_Card_XiaoHU, hand_card_count, cards, GameConstants.INVALID_SEAT);
		GameSchedule.put(() -> {
			exe_qi_pai(seat_index);
			if(_playerStatus[seat_index]._action_count > 0){
				//boolean is_zm = _playerStatus[seat_index].has_action_by_code(GameConstants.WIK_ZI_MO);
				//_playerStatus[seat_index].clean_action(Constants_MJ_NING_XIANG.WIK_ZT_QI_SHOU_HU);
				_playerStatus[seat_index]._response = false;
//				int action = is_zm ? GameConstants.WIK_ZI_MO : GameConstants.WIK_CHI_HU;
//				_playerStatus[seat_index].add_action(action);
//				_playerStatus[seat_index].add_action_card(is_zm?1:2, _provide_card, action, seat_index);
				change_player_status(seat_index, GameConstants.Player_Status_OPR_CARD);
				operate_player_action(seat_index, false);
			}else{
				if(_player_result.biaoyan[seat_index] == 1 || is_gang[seat_index]){
					GameSchedule.put(()->{
						_handler.handler_player_out_card(this, seat_index, _provide_card);
					},1, TimeUnit.SECONDS);
				}else{
					change_player_status(seat_index, GameConstants.Player_Status_OUT_CARD);
					operate_player_status();
				}
			}
		}, 3, TimeUnit.SECONDS);
	}
	
	public void exe_qi_pai(int seat_index){
		is_dao_pai[seat_index] = false;
		int cards[] = new int[GameConstants.MAX_COUNT];
		int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[seat_index], cards);
		int out_ting_count = 0;
		int show_send_card = _provide_card;
		if(hand_card_count == 14){
			boolean flag = false;
			for (int j = 0; j < 13; j++) {
				if(cards[j] == show_send_card){
					flag = true;
				}
				if(flag){
					cards[j] = cards[j+1];
				}
			}
			cards[13] = 0;
			// 出任意一张牌时，能胡哪些牌 -- Begin
			out_ting_count = _playerStatus[seat_index]._hu_out_card_count;
			if (out_ting_count > 0) {
				for (int j = 0; j < 13; j++) {
					for (int k = 0; k < out_ting_count; k++) {
						if (cards[j] == _playerStatus[seat_index]._hu_out_card_ting[k]) {
							cards[j] += GameConstants.CARD_ESPECIAL_TYPE_TING;
							break;
						}
					}
				}
			}
		}
		this.operate_player_cards(seat_index, 13, cards, 0, null);
		if(hand_card_count == 14){
			for (int k = 0; k < out_ting_count; k++){ 
				if (show_send_card == _playerStatus[seat_index]._hu_out_card_ting[k]){
					show_send_card += GameConstants.CARD_ESPECIAL_TYPE_TING;
					break;
				}
			}
			operate_player_get_card(seat_index, 1, new int[] { show_send_card }, GameConstants.INVALID_SEAT);
		}
		this.operate_show_card(seat_index, GameConstants.Show_Card_XiaoHU, 0, null, GameConstants.INVALID_SEAT);
	}
	
	public boolean reconnectionQiShouHu(){
		boolean flag = true;
		for (int p = 0; p < getTablePlayerNumber(); p++) {
			if(is_dao_pai[p]){
				if(_current_player == p)
					flag = false;
				int[] cards_index = Arrays.copyOf(GRR._cards_index[p], GRR._cards_index[p].length);
				for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
					if (qsh_cards_index[p][i] > 0) {
						cards_index[i] -= qsh_cards_index[p][i];
					}
				}
				int cards_data[] = new int[GameConstants.MAX_COUNT];
				int cards_index_count = _logic.switch_to_cards_data(cards_index, cards_data);
				this.operate_player_cards(p, cards_index_count, cards_data, 0, null);
				
				// 显示胡牌
				int cards[] = new int[GameConstants.MAX_COUNT];
				int hand_card_count = _logic.switch_to_cards_data(qsh_cards_index[p], cards);
				this.operate_show_card(p, GameConstants.Show_Card_XiaoHU, hand_card_count, cards, GameConstants.INVALID_SEAT);
			}
		}
		return flag;
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
			boolean is_zd = is_gang[i] || _player_result.biaoyan[i] == 1;//不能再吃碰杠
			playerStatus = _playerStatus[i];
			
			if (i == get_banker_next_seat(seat_index) && getRuleValue(Constants_MJ_NING_XIANG.GAME_RULE_ONLY_PENG) != 1 && !is_zd) {
				action = _logic.check_chi(GRR._cards_index[i], card);

				if ((action & GameConstants.WIK_RIGHT) != 0) {
					_playerStatus[i].add_action(GameConstants.WIK_RIGHT);
					_playerStatus[i].add_chi(card, GameConstants.WIK_RIGHT, seat_index);
				}
				if ((action & GameConstants.WIK_CENTER) != 0) {
					_playerStatus[i].add_action(GameConstants.WIK_CENTER);
					_playerStatus[i].add_chi(card, GameConstants.WIK_CENTER, seat_index);
				}
				if ((action & GameConstants.WIK_LEFT) != 0) {
					_playerStatus[i].add_action(GameConstants.WIK_LEFT);
					_playerStatus[i].add_chi(card, GameConstants.WIK_LEFT, seat_index);
				}

				if (_playerStatus[i].has_action()) {
					bAroseAction = true;
				}
			}
			

			/*boolean can_peng = true;
			int[] tmp_cards_data = _playerStatus[i].get_cards_abandoned_peng();
			for (int x = 0; x < GameConstants.MAX_ABANDONED_CARDS_COUNT; x++) {
				if (tmp_cards_data[x] == card) {
					can_peng = false;
					break;
				}
			}*/
			
			if (GRR._left_card_count > 0 && !is_zd) {//如果牌堆已经没有牌，出的最后一张牌了不能碰，杠
				action = _logic.check_peng(GRR._cards_index[i], card);
				if (action != 0) {
					playerStatus.add_action(action);
					playerStatus.add_peng(card, seat_index);
					bAroseAction = true;
				}
			}

			if (GRR._left_card_count > 0 && _player_result.biaoyan[i] != 1) {
				action = _logic.estimate_gang_card_out_card(GRR._cards_index[i], card);
				if (action != 0) {
					if(!is_zd){
						playerStatus.add_action(Constants_MJ_NING_XIANG.WIK_BU_ZHANG);
						add_bu_zhang(playerStatus, card, seat_index, 1);
						bAroseAction = true;
					}
					if(is_can_gang_card(card, GRR._cards_index[i], GRR._weave_items[i],
							GRR._weave_count[i], i)){
						playerStatus.add_action(GameConstants.WIK_GANG);
						playerStatus.add_gang(card, seat_index, 1);
						bAroseAction = true;
					}
				}
			}
			
			ChiHuRight chr = GRR._chi_hu_rights[i];
			chr.set_empty();
			if(_handler_out_card_operate._type == GameConstants.GANG_TYPE_HONG_ZHONG)
				chr.opr_or_long(Constants_MJ_NING_XIANG.CHR_DI_HU);
			int cbWeaveCount = GRR._weave_count[i];
			int card_type = Constants_MJ_NING_XIANG.HU_CARD_TYPE_DIAN_PAO;
			action = analyse_chi_hu_card(GRR._cards_index[i],
					GRR._weave_items[i], cbWeaveCount, card, chr,
					card_type, i);
			if (action != 0) {
				if (_playerStatus[i].is_chi_hu_round() || getPaiXingScore(chr, i) > score_when_abandoned_jie_pao[i]) {
						_playerStatus[i].add_action(GameConstants.WIK_CHI_HU);
						_playerStatus[i].add_chi_hu(card, seat_index);
						bAroseAction = true;
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
	
	public boolean estimate_player_gang_card_respond(int seat_index, int card) {
		boolean bAroseAction = false;

		PlayerStatus playerStatus = _playerStatus[seat_index];

		int action = GameConstants.WIK_NULL;
		
		if(0 != _logic.estimate_gang_card_out_card(GRR._cards_index[seat_index], card) && is_can_gang_card(card, GRR._cards_index[seat_index], GRR._weave_items[seat_index],
				GRR._weave_count[seat_index], seat_index)){
			playerStatus.add_action(GameConstants.WIK_GANG);
			playerStatus.add_gang(card, seat_index, 1);
			bAroseAction = true;
		}

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (seat_index == i) {
				continue;
			}
			boolean is_zd = is_gang[i] || _player_result.biaoyan[i] == 1;//不能再吃碰杠
			playerStatus = _playerStatus[i];
			
			if (i == get_banker_next_seat(seat_index) && getRuleValue(Constants_MJ_NING_XIANG.GAME_RULE_ONLY_PENG) != 1 && !is_zd) {
				action = _logic.check_chi(GRR._cards_index[i], card);

				if ((action & GameConstants.WIK_RIGHT) != 0) {
					_playerStatus[i].add_action(GameConstants.WIK_RIGHT);
					_playerStatus[i].add_chi(card, GameConstants.WIK_RIGHT, seat_index);
				}
				if ((action & GameConstants.WIK_CENTER) != 0) {
					_playerStatus[i].add_action(GameConstants.WIK_CENTER);
					_playerStatus[i].add_chi(card, GameConstants.WIK_CENTER, seat_index);
				}
				if ((action & GameConstants.WIK_LEFT) != 0) {
					_playerStatus[i].add_action(GameConstants.WIK_LEFT);
					_playerStatus[i].add_chi(card, GameConstants.WIK_LEFT, seat_index);
				}

				if (_playerStatus[i].has_action()) {
					bAroseAction = true;
				}
			}
			
			if (GRR._left_card_count > 0 && !is_zd) {//如果牌堆已经没有牌，出的最后一张牌了不能碰，杠
				action = _logic.check_peng(GRR._cards_index[i], card);
				if (action != 0) {
					playerStatus.add_action(action);
					playerStatus.add_peng(card, seat_index);
					bAroseAction = true;
				}
			}

			if (GRR._left_card_count > 0 && _player_result.biaoyan[i] != 1) {
				action = _logic.estimate_gang_card_out_card(GRR._cards_index[i], card);
				if (action != 0) {
					if(!is_zd){
						playerStatus.add_action(Constants_MJ_NING_XIANG.WIK_BU_ZHANG);
						add_bu_zhang(playerStatus, card, seat_index, 1);
						bAroseAction = true;
					}
					if(is_can_gang_card(card, GRR._cards_index[i], GRR._weave_items[i],
							GRR._weave_count[i], i)){
						playerStatus.add_action(GameConstants.WIK_GANG);
						playerStatus.add_gang(card, seat_index, 1);
						bAroseAction = true;
					}
				}
			}
		}

		return bAroseAction;
	}
	
	public void add_bu_zhang(PlayerStatus curPlayerStatus, int card, int provider, int p) {
		for (int i = 0; i < curPlayerStatus._weave_count; i++) {
			if ((curPlayerStatus._action_weaves[i].center_card == card) && (curPlayerStatus._action_weaves[i].weave_kind == Constants_MJ_NING_XIANG.WIK_BU_ZHANG)) {
				return;
			}
		}
		curPlayerStatus._action_weaves[curPlayerStatus._weave_count].public_card = p;
		curPlayerStatus._action_weaves[curPlayerStatus._weave_count].center_card = card;
		curPlayerStatus._action_weaves[curPlayerStatus._weave_count].weave_kind = Constants_MJ_NING_XIANG.WIK_BU_ZHANG;
		curPlayerStatus._action_weaves[curPlayerStatus._weave_count].provide_player = provider;
		curPlayerStatus._weave_count++;
	}

	public int get_ting_card(int[] cards, int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int seat_index) {
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		ChiHuRight chr = new ChiHuRight();

		int count = 0;
		int cbCurrentCard;

		for (int i = 0; i < GameConstants.MAX_ZI; i++) {
			cbCurrentCard = _logic.switch_to_card_data(i);
			chr.set_empty();

			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card(cbCardIndexTemp, weaveItem, cbWeaveCount, cbCurrentCard, chr,
					GameConstants.CHR_ZI_MO, seat_index)) {
				cards[count++] = cbCurrentCard;
			}
		}
		
		//全听
		if (count == GameConstants.MAX_ZI) {
			count = 1;
			cards[0] = -1;
		}

		return count;
	}
	
	public int getKaiGangNum(){
		if(getRuleValue(Constants_MJ_NING_XIANG.GAME_RULE_KAI_4_ZHANG) == 1)
			return 4;
		return 2;
	}
	
	public boolean is_can_gang_card(int card, int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int seat_index) {
		if(!is_start())
			return false;
		
		if(GRR._left_card_count < getKaiGangNum() + 1 || (getRuleValue(Constants_MJ_NING_XIANG.GAME_RULE_ONLY_GNAG_MENY) != 1 && is_gang[seat_index]))
			return false;
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}
		cbCardIndexTemp[_logic.switch_to_card_index(card)] = 0;
		
		WeaveItem weaves[] = new WeaveItem[GameConstants.MAX_WEAVE];
		int weave_count = GRR._weave_count[seat_index];
		for (int i = 0; i < weave_count; i++) {
			weaves[i] = new WeaveItem();
			weaves[i].weave_kind = GRR._weave_items[seat_index][i].weave_kind;
			weaves[i].center_card = GRR._weave_items[seat_index][i].center_card;
			weaves[i].public_card = GRR._weave_items[seat_index][i].public_card;
			weaves[i].provide_player = GRR._weave_items[seat_index][i].provide_player;
		}
		if(weave_count < 4){//防止下标越界
			weaves[weave_count] = new WeaveItem();
			weaves[weave_count].weave_kind = GameConstants.WIK_GANG;
			weaves[weave_count].center_card = card;
			weaves[weave_count].public_card = 0;
			weaves[weave_count].provide_player = seat_index;
			weave_count++;
		}
		
		ChiHuRight chr = new ChiHuRight();

		int count = 0;
		int cbCurrentCard;
		is_can_jia_jiang_jiang[seat_index] = false;
		boolean flag = true;
		for (int i = 0; i < GameConstants.MAX_ZI; i++) {
			cbCurrentCard = _logic.switch_to_card_data(i);
			chr.set_empty();
			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card(cbCardIndexTemp, weaves, weave_count, cbCurrentCard, chr,
					100, seat_index)) {
				if(chr.opr_and_long(Constants_MJ_NING_XIANG.CHR_JIA_JIANG_HU).is_empty())
					flag = false;
				count++;
			}
		}
		if(flag)
			is_can_jia_jiang_jiang[seat_index] = true;
		if(count > 0)
			return true;
			
		return false;
	}
	
	public boolean is_start(){
		return is_judge;
	}

	@Override
	public boolean trustee_timer(int operate_id, int seat_index) {
		return false;
	}
	
	
	
	/**
	 * 混一色判断
	 * 
	 * @return
	 */
	public boolean is_hun_yi_se(int cards_index[], WeaveItem weaveItem[], int weaveCount) {
		if (_logic.get_se_count(cards_index, weaveItem, weaveCount) != 1) {
			return false;
		}

		if (_logic.has_feng_pai(cards_index, weaveItem, weaveCount) == false) {
			return false;
		}
		return true;
	}
	
	
	public boolean is_men_qing(WeaveItem weaveItem[], int weaveCount) {
    	if(weaveCount == 0)
    		return true;
    	
    	// 都是暗杠
        for (int i = 0; i < weaveCount; i++) {
            if(!(weaveItem[i].weave_kind == GameConstants.WIK_GANG && weaveItem[i].public_card == 0))
            	return false;
        }
        return true;
    }
	public boolean is_men_qing1(WeaveItem weaveItem[], int weaveCount,ChiHuRight chiHuRight) {
    	if(weaveCount == 0)
    		return true;
    	if(weaveCount > 1)
    		return false;
    	
        if(weaveItem[0].weave_kind == GameConstants.WIK_GANG && weaveItem[0].public_card == 0
        		&& !chiHuRight.opr_and_long(Constants_MJ_NING_XIANG.CHR_GANG_SHANG_KAI_HUA).is_empty())
        	return true;
            
        return false;
    }
	
	public boolean is_all_pg(WeaveItem weaveItem[], int weaveCount) {
    	if(weaveCount == 0)
    		return true;
    	
    	// 都是暗杠
        for (int i = 0; i < weaveCount; i++) {
            if(weaveItem[i].weave_kind == GameConstants.WIK_GANG && weaveItem[i].public_card == 0)
            	return false;
        }
        return true;
    }
	
	// -1 不是小七对 0是 1豪华 2双豪华 3三豪华
 	public int is_xiao_qi_dui(int cards_index[], int cbWeaveCount) {

 		// 组合判断
 		if (cbWeaveCount != 0)
 			return -1;

 		// 单牌数目
 		int cbReplaceCount = 0;
 		int nGenCount = 0;

 		// 计算单牌
 		for (int i = 0; i < GameConstants.MAX_ZI_FENG; i++) {
 			int cbCardCount = cards_index[i];
 				// 单牌统计
 				if (cbCardCount == 1 || cbCardCount == 3)
 					cbReplaceCount++;

 				if (cbCardCount == 4) {
 					nGenCount++;
 				}
 		}

		if (cbReplaceCount > 0)
			return -1;

 		if (nGenCount == 1)
 			return 1;
 		if (nGenCount == 2)
 			return 2;
 		if (nGenCount == 3)
 			return 3;
 			
 		return 0;
 	}
    // 杠牌分析 包括补杠 check_weave检查补杠
    public int analyse_gang(int cards_index[], WeaveItem WeaveItem[], int cbWeaveCount,
            GangCardResult gangCardResult, boolean check_weave, int cards_abandoned_gang[], int seat_index) {
        // 设置变量
        int cbActionMask = GameConstants.WIK_NULL;
        // 手上杠牌
        for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
            if (cards_index[i] == 4) {
                cbActionMask |= GameConstants.WIK_GANG;
                int index = gangCardResult.cbCardCount++;
                gangCardResult.cbCardData[index] = _logic.switch_to_card_data(i);
                gangCardResult.isPublic[index] = 0;// 暗刚
                gangCardResult.type[index] = GameConstants.GANG_TYPE_AN_GANG;
            }
        }

        if (check_weave) {
            // 组合杠牌，包括以前能杠，但是不杠，发牌之后而选择补杠的
            for (int i = 0; i < cbWeaveCount; i++) {
                if (WeaveItem[i].weave_kind == GameConstants.WIK_PENG) {
                    for (int j = 0; j < GameConstants.MAX_INDEX; j++) {
                        if (cards_index[j] != 1) {
                            continue;
                        } else{
                            if (WeaveItem[i].center_card == _logic.switch_to_card_data(j)) {
                                cbActionMask |= GameConstants.WIK_GANG;
                                int index = gangCardResult.cbCardCount++;
                                gangCardResult.cbCardData[index] = WeaveItem[i].center_card;
                                gangCardResult.isPublic[index] = 1;// 明刚
                                gangCardResult.type[index] = GameConstants.GANG_TYPE_ADD_GANG;
                                break;
                            }
                        }
                    }
                }
            }
        }

        return cbActionMask;
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
        if (GRR != null) {// reason == MJGameConstants.Game_End_NORMAL || reason
                          // == MJGameConstants.Game_End_DRAW ||
            // (reason ==MJGameConstants.Game_End_RELEASE_PLAY && GRR!=null)
			
            game_end.setRoundOverType(1);
            game_end.setStartTime(GRR._start_time);

            game_end.setGameTypeIndex(GRR._game_type_index);
            roomResponse.setLeftCardCount(GRR._left_card_count);

            // 特别显示的牌
            if (this.is_mj_type(GameConstants.GAME_TYPE_HUNAN_XIANG_TAN)
                    || this.is_mj_type(GameConstants.GAME_TYPE_HUNAN_XIANG_TAN_DT)) {
                for (int i = 0; i < GRR._especial_card_count; i++) {
                    game_end.addEspecialShowCards(
                            GRR._especial_show_cards[i] + GameConstants.CARD_ESPECIAL_TYPE_DING_GUI);
                }
            } else {
                for (int i = 0; i < GRR._especial_card_count; i++) {
                    game_end.addEspecialShowCards(GRR._especial_show_cards[i]);
                }
            }

            GRR._end_type = reason;

            // 杠牌，每个人的分数
            float lGangScore[] = new float[getTablePlayerNumber()];
            //boolean liu_ju = reason == GameConstants.Game_End_DRAW || reason == GameConstants.Game_End_RELEASE_PLAY;
            for (int i = 0; i < getTablePlayerNumber(); i++) {
                // 记录
                for (int j = 0; j < getTablePlayerNumber(); j++) {
                    _player_result.lost_fan_shu[i][j] += GRR._lost_fan_shu[i][j];
                }

            }

            for (int i = 0; i < getTablePlayerNumber(); i++) {
                GRR._game_score[i] += lGangScore[i];
                // 记录
                _player_result.game_score[i] += GRR._game_score[i];

            }
            for (int i = 0; i < getTablePlayerNumber(); i++) {
            	GRR._game_score[i] += lGangScore[i];
            	// 记录
            	GRR._game_score[i] += GRR._start_hu_score[i]; // 起手胡分数
            	
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
                	if (_logic.is_magic_card(GRR._chi_hu_card[i][j])) {
						hc.addItem(GRR._chi_hu_card[i][j]
								+ GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI);
					} else {
						hc.addItem(GRR._chi_hu_card[i][j]);
					}
                }
                
				if (_logic.is_magic_card(GRR._chi_hu_card[i][0])) {
					game_end.addHuCardData(GRR._chi_hu_card[i][0]
							+ GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI);
				} else {
					game_end.addHuCardData(GRR._chi_hu_card[i][0]);
				}

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
                	if (_logic.is_magic_card(GRR._cards_data[i][j])) {
						cs.addItem(GRR._cards_data[i][j]
								+ GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI);
					} else {
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

        // 结束后刷新玩家
        // RoomResponse.Builder roomResponse2 = RoomResponse.newBuilder();
        // roomResponse2.setGameStatus(_game_status);
        // roomResponse2.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);// 刷新玩家
        // this.load_player_info_data(roomResponse2);
        // this.send_response_to_room(roomResponse2);

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
        }else{
        	operate_player_info();
        }

        if (!is_sys()) {
            GRR = null;
        }

        if (is_sys()) {
            clear_score_in_gold_room();
        }
        
        for (int i = 0; i < getTablePlayerNumber(); i++) {
            _player_result.biaoyan[i] = 0;
			_player_result.haspiao[i] = 0;
        }
        // 错误断言
        return false;
    }
    
	public int get_bird_num() {
		int num = 1;
		if(has_rule(Constants_MJ_NING_XIANG.GAME_RULE_CATCH_BIRD_2))
			num = 2;
		if(has_rule(Constants_MJ_NING_XIANG.GAME_RULE_CATCH_BIRD_4))
			num = 4;
		if(has_rule(Constants_MJ_NING_XIANG.GAME_RULE_CATCH_BIRD_6))
			num = 6;
		return num;
	}
	
	public boolean operate_player_info() {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);
		roomResponse.setGameStatus(_game_status);

		load_player_info_data(roomResponse);

		if (GRR != null)
			GRR.add_room_response(roomResponse);

		send_response_to_room(roomResponse);

		return true;
	}
	/**
	 * 检查暗杠，补杠，报听
	 * @param m_gangCardResult
	 * @param _seat_index
	 * @param is_first_card是否发的第一张牌
	 */
	public void check_an_add_gang(GangCardResult m_gangCardResult, int _seat_index) {
		m_gangCardResult.cbCardCount = 0;
		PlayerStatus curPlayerStatus = _playerStatus[_seat_index];
		int cbActionMask = analyse_gang(GRR._cards_index[_seat_index], GRR._weave_items[_seat_index],
				GRR._weave_count[_seat_index], m_gangCardResult, true, GRR._cards_abandoned_gang[_seat_index], _seat_index);

		if (cbActionMask != GameConstants.WIK_NULL) {
			if(!is_gang[_seat_index])
				curPlayerStatus.add_action(Constants_MJ_NING_XIANG.WIK_BU_ZHANG);
			for (int i = 0; i < m_gangCardResult.cbCardCount; i++) {
				if(!is_gang[_seat_index])
					add_bu_zhang(curPlayerStatus, m_gangCardResult.cbCardData[i], _seat_index, m_gangCardResult.isPublic[i]);
				if(is_can_gang_card(m_gangCardResult.cbCardData[i], GRR._cards_index[_seat_index], GRR._weave_items[_seat_index],
						GRR._weave_count[_seat_index], _seat_index)){
					if(!curPlayerStatus.has_action_by_code(GameConstants.WIK_GANG))
						curPlayerStatus.add_action(GameConstants.WIK_GANG);
					curPlayerStatus.add_gang(m_gangCardResult.cbCardData[i], _seat_index, m_gangCardResult.isPublic[i]);
				}
			}
		}
	}
	
	public boolean checkBaoTing(){
		boolean is_bao_ting = false;
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if(_player_result.biaoyan[i] == 2){
				PlayerStatus playerStatus = _playerStatus[i];
				playerStatus.add_action(GameConstants.WIK_BAO_TING);
				change_player_status(i, GameConstants.Player_Status_OPR_CARD);
				playerStatus._response = false;
				operate_player_action(i, false);
				is_bao_ting = true;
			}
		}
		if(!is_bao_ting)
			is_judge = true;
		return is_bao_ting;
	}
	
	/**
	 * 获取操作的优先等级
	 * 
	 **/
	// 获取动作等级
	public int get_action_rank(int player_action) {
		// 自摸牌等级
		if (player_action == GameConstants.WIK_ZI_MO) {
			return 50;
		}

		// 吃胡牌等级
		if (player_action == GameConstants.WIK_CHI_HU) {
			return 40;
		}

		// 杠牌等级
		if (player_action == GameConstants.WIK_GANG) {
			return 30;
		}

		// 补
		if (player_action == Constants_MJ_NING_XIANG.WIK_BU_ZHANG) {
			return 25;
		}


		// 碰牌等级
		if (player_action == GameConstants.WIK_PENG) {
			return 20;
		}

		if (player_action == GameConstants.WIK_BAO_TING) {
			return 15;
		}

		// 上牌等级
		if (player_action == GameConstants.WIK_RIGHT || player_action == GameConstants.WIK_CENTER
				|| player_action == GameConstants.WIK_LEFT) {
			return 10;
		}

		return 0;
	}
	
	public int get_action_list_rank(int action_count, int action[]) {
		int max_index = 0;

		for (int i = 0; i < action_count; i++) {
			int index = get_action_rank(action[i]);
			if (max_index < index) {
				max_index = index;
			}
		}

		return max_index;
	}
    
    @Override
	public void test_cards() {
		int[] cards_of_player0 = new int[] { 0x01,0x02,0x03,0x12,0x12,0x15,0x13,0x13,0x13,0x25,0x25,0x26,0x26 };
		int[] cards_of_player1 = new int[] { 0x21,0x21,0x22,0x22,0x22,0x23,0x23,0x23,0x24,0x24,0x25,0x25,0x26 };
		int[] cards_of_player2 = new int[] { 0x21,0x21,0x22,0x22,0x22,0x23,0x23,0x23,0x24,0x24,0x25,0x25,0x26 };
		int[] cards_of_player3 = new int[] { 0x11,0x12,0x13,0x21,0x22,0x23,0x07,0x08,0x09,0x14,0x14,0x25,0x25 };
		
//		int[] cards_of_player0 = new int[] { 0x11,0x11,0x11,0x12,0x12,0x12,0x13,0x13,0x13,0x22,0x22,0x23,0x23 };
//		int[] cards_of_player1 = new int[] { 0x11,0x11,0x11,0x12,0x12,0x12,0x13,0x13,0x13,0x22,0x22,0x23,0x23 };
//		int[] cards_of_player2 = new int[] { 0x21,0x22,0x23,0x12,0x12,0x12,0x13,0x13,0x13,0x28,0x28,0x03,0x03 };
//		int[] cards_of_player3 = new int[] { 0x21,0x22,0x23,0x12,0x12,0x12,0x13,0x13,0x13,0x28,0x28,0x28,0x23 };

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			for (int j = 0; j < GameConstants.MAX_INDEX; j++) {
				GRR._cards_index[i][j] = 0;
			}
		}

		for (int j = 0; j < 13; j++) {
			if (getTablePlayerNumber() == 4) {
				GRR._cards_index[0][_logic
						.switch_to_card_index(cards_of_player0[j])] += 1;
				GRR._cards_index[1][_logic
						.switch_to_card_index(cards_of_player1[j])] += 1;
				GRR._cards_index[2][_logic
						.switch_to_card_index(cards_of_player2[j])] += 1;
				GRR._cards_index[3][_logic
						.switch_to_card_index(cards_of_player3[j])] += 1;
			} else if (getTablePlayerNumber() == 3) {
				GRR._cards_index[0][_logic
						.switch_to_card_index(cards_of_player0[j])] += 1;
				GRR._cards_index[1][_logic
						.switch_to_card_index(cards_of_player1[j])] += 1;
				GRR._cards_index[2][_logic
						.switch_to_card_index(cards_of_player2[j])] += 1;
			} else if (getTablePlayerNumber() == 2) {
				GRR._cards_index[0][_logic
						.switch_to_card_index(cards_of_player0[j])] += 1;
				GRR._cards_index[1][_logic
						.switch_to_card_index(cards_of_player1[j])] += 1;
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
	
}
