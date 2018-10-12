package com.cai.game.mj.chenchuang.dalianqionghu;

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
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.mj.Constants_MJ_DA_LIAN_QH;
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
 * 大连穷胡麻将
 */
public class Table_DaLianQiongHu extends AbstractMJTable {

	private static final long serialVersionUID = 1L;
	
	public boolean[] is_bao_ting = new boolean[getTablePlayerNumber()];//是否报听
	public boolean[] is_bao_ting_gq = new boolean[getTablePlayerNumber()];//是否过圈看宝；
	public int gang_count;//杠的次数
	public int bao_card;//翻的包牌
	public boolean[] is_kai_kou = new boolean[getTablePlayerNumber()];//是否开口
	public int fen_zhang_start_seat_index = -1;
	
	public Table_DaLianQiongHu() {
		super(MJType.GAME_TYPE_NEW_MJ_DLQH);
	}

	@Override
	protected void onInitTable() {
		_handler_chi_peng = new HandlerChiPeng_DaLianQiongHu();
		_handler_dispath_card = new HandlerDispatchCard_DaLianQiongHu();
		_handler_gang = new HandlerGang_DaLianQiongHu();
		_handler_out_card_operate = new HandlerOutCardOperate_DaLianQiongHu();
	}

	public void exe_select_magic(){
		
	}
	
	/**第一局建房者为庄家，若为代开房/俱乐部开房（即开房者自己未进入牌局），则随机一位为庄家;*/
	@Override
	protected void initBanker() {
//		long id = getCreate_player().getAccount_id();
//		if(get_player(id) == null)
			_cur_banker = RandomUtil.getRandomNumber(getTablePlayerNumber());
	}


	/**
	 * 洗完牌执行开始
	 */
	@Override
	protected boolean on_game_start() {
		//初始化常量
		is_bao_ting = new boolean[getTablePlayerNumber()];
		is_bao_ting_gq = new boolean[getTablePlayerNumber()];
		gang_count = 0;
		bao_card = 0;
		is_kai_kou = new boolean[getTablePlayerNumber()];
		fen_zhang_start_seat_index = -1;
		_logic.clean_magic_cards();
		show_tou_zi(_cur_banker);
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
				operate_chi_hu_cards(i, _playerStatus[i]._hu_card_count, _playerStatus[i]._hu_cards);
			}
		}

		//发第一张牌
		exe_dispatch_card(_current_player, GameConstants.GANG_TYPE_HONG_ZHONG, GameConstants.DELAY_SEND_CARD_DELAY);

		return true;
	}

	@Override
	public int analyse_chi_hu_card(int[] cards_index, WeaveItem[] weaveItems, int weave_count, int cur_card, ChiHuRight chiHuRight, int card_type,
			int _seat_index) {
		if (card_type == Constants_MJ_DA_LIAN_QH.HU_CARD_TYPE_ZI_MO) {
			chiHuRight.opr_or(Constants_MJ_DA_LIAN_QH.CHR_ZI_MO);//自摸
		} else if (card_type == Constants_MJ_DA_LIAN_QH.HU_CARD_TYPE_QIANG_GANG) {
			chiHuRight.opr_or(Constants_MJ_DA_LIAN_QH.CHR_QIANG_GANG_HU);//抢杠胡
		}else if (card_type == Constants_MJ_DA_LIAN_QH.HU_CARD_TYPE_JIE_PAO) {
			chiHuRight.opr_or(Constants_MJ_DA_LIAN_QH.CHR_DIAN_PAO_HU);//点炮胡
		}else if (card_type == Constants_MJ_DA_LIAN_QH.HU_CARD_TYPE_GKSH) {
			chiHuRight.opr_or(Constants_MJ_DA_LIAN_QH.CHR_GANG_SHANG_KAI_HUA);//杠上开花
		}
		
		/**
		  	①三色全：手牌中要有万筒条三门花色；
			②有幺牌：手牌中要有1、9或字牌；
			③有刻子：手牌中最少有一副刻子（或杠牌），没有刻子时有中、发、白对子作将也可以；
			④有开口：必须吃、碰、杠过，不允许门清；
			有顺子：手牌最少有一副顺子，飘胡除外；
		 */
		//④有开口：必须吃、碰、杠过，不允许门清；
		if(!is_kai_kou[_seat_index])
			return GameConstants.WIK_NULL;
		//拷贝把当前牌加入手牌
		int cur_card_index = _logic.switch_to_card_index(cur_card);
		int[] temp_cards_index = Arrays.copyOf(cards_index, cards_index.length);
		temp_cards_index[cur_card_index]++;
		//①三色全：手牌中要有万筒条三门花色；
		if(_logic.get_se_count(temp_cards_index, weaveItems, weave_count) != 3)
			return GameConstants.WIK_NULL;
		
		boolean is_bao_card = (card_type == Constants_MJ_DA_LIAN_QH.HU_CARD_TYPE_ZI_MO || card_type == Constants_MJ_DA_LIAN_QH.HU_CARD_TYPE_GKSH) && is_bao_card(_seat_index, cur_card);
  		
        //是否胡牌牌型
  		boolean analyse_win_by_cards_index = AnalyseCardUtil.analyse_win_by_cards_index_da_lian(cards_index, cur_card_index, is_bao_card?1:0, false);
  		if(!analyse_win_by_cards_index){
  			return GameConstants.WIK_NULL;
  		}
  		
  		//②有幺牌：手牌中要有1、9或字牌；
  		if(!has_one_nine(cards_index, cur_card, is_bao_card, weaveItems, weave_count)){
  			return GameConstants.WIK_NULL;
  		}
  		//③有刻子：手牌中最少有一副刻子（或杠牌），没有刻子时有中、发、白对子作将也可以；
  		if(!has_ke_zi(cards_index, cur_card_index, is_bao_card, weaveItems, weave_count)){
  			return GameConstants.WIK_NULL;
  		}
  		boolean peng_hu = !_logic.exist_eat(weaveItems, weave_count) && AnalyseCardUtil.analyse_win_by_cards_index_da_lian(cards_index, cur_card_index, is_bao_card?1:0, true);
  		boolean is_piao_hu = has_rule(Constants_MJ_DA_LIAN_QH.GAME_RULE_PIAO_HU) && peng_hu;
  		if(is_piao_hu)
			chiHuRight.opr_or(Constants_MJ_DA_LIAN_QH.CHR_PIAO_HU);
  		//有顺子：手牌最少有一副顺子，飘胡除外
  		if(!is_piao_hu && peng_hu){
  			return GameConstants.WIK_NULL;
  		}
  		boolean is_jia_hu = has_rule(Constants_MJ_DA_LIAN_QH.GAME_RULE_JIA_HU) && is_bao_ting[_seat_index] && _playerStatus[_seat_index]._hu_card_count == 1 && has_hand_card19(cards_index, weaveItems, weave_count);
		if(is_jia_hu)
			chiHuRight.opr_or(Constants_MJ_DA_LIAN_QH.CHR_JIA_HU);
		if(is_bao_card && chiHuRight.opr_and(Constants_MJ_DA_LIAN_QH.CHR_CHONG_BAO).is_empty())
			chiHuRight.opr_or(Constants_MJ_DA_LIAN_QH.CHR_MO_BAO);
  		if(chiHuRight.m_dwRight[0] < 0x10)
			chiHuRight.opr_or(Constants_MJ_DA_LIAN_QH.CHR_PING_HU);
		
		return GameConstants.WIK_CHI_HU;
	}
	
	private boolean has_ke_zi(int[] temp_cards_index, int cur_card_index,
			boolean is_bao_card, WeaveItem[] weaveItems, int weave_count) {
		for (int i = 0; i < weave_count; i++) {
			 if(weaveItems[i].weave_kind == GameConstants.WIK_GANG ||
					 weaveItems[i].weave_kind == GameConstants.WIK_PENG){
					 return true;
			 }
		}
		if(is_bao_card){
			for (int i = 0; i < GameConstants.MAX_ZI_FENG; i++) {
				if(AnalyseCardUtil.analyse_win_by_cards_index_da_lian(temp_cards_index, i, 0, false))
					if(has_kz_zfbdz(temp_cards_index, i))
						return true;
			}
			
			for (int i = 31; i < 34; i++) {
				if(AnalyseCardUtil.analyse_win_by_cards_index_da_lian(temp_cards_index, i, 0, false))
					if(has_kz_zfbdz(temp_cards_index, i))
						return true;
			}
		}else{
			if(has_kz_zfbdz(temp_cards_index, cur_card_index))
				return true;
		}
		
		
		return false;
	}
	
	private boolean has_kz_zfbdz(int[] temp_cards_index, int cur_card_index){
		int[] cards_index = Arrays.copyOf(temp_cards_index, temp_cards_index.length);
		cards_index[cur_card_index]++;
		for (int i = 0; i < GameConstants.MAX_ZI_FENG; i++) {
			if(cards_index[i] >= 3){
				cards_index[i] -= 3;
				if(AnalyseCardUtil.analyse_win_by_cards_index_da_lian(cards_index, -1, 0, false)){
					return true;
				}else{
					cards_index[i] += 3;
				}
			}
		}
		
		for (int i = 31; i < 34; i++) {
			if(cards_index[i] == 2)
				return true;
		}
		return false;
	}

	private boolean has_one_nine(int[] temp_cards_index, int cur_card, boolean is_bao_card,
			WeaveItem[] weaveItems, int weave_count) {
		if(cur_card >= 0x31){
			return true;
		}
		if(cur_card < 0x31){
			int cur_card_value = _logic.get_card_value(cur_card);
			 if(cur_card_value == 1 || cur_card_value == 9)
				 return true;
		}
			
		if(has_hand_card19(temp_cards_index, weaveItems, weave_count))
				 return true;
		
		if(is_bao_card){
			for (int i = 0; i < GameConstants.MAX_ZI; i++) {
				if(i % 9 == 0 || i % 9 == 8){
					if(AnalyseCardUtil.analyse_win_by_cards_index_da_lian(temp_cards_index, i, 0, false))
						return true;
				}
			}
		}
		return false;
	}

	public boolean has_hand_card19(int[] temp_cards_index,
			WeaveItem[] weaveItems, int weave_count) {
		for (int i = 0; i < weave_count; i++) {
			int center_card = weaveItems[i].center_card; 
			int value = _logic.get_card_value(center_card);
			if(center_card < 0x31){
				 if(value == 1 || value == 9)
					 return true;
				 if(weaveItems[i].weave_kind == GameConstants.WIK_LEFT){
					 if(value + 2 == 9)
						 return true;
				 }
				 if(weaveItems[i].weave_kind == GameConstants.WIK_CENTER){
					 if(value + 1 == 9 || value - 1 == 1)
						 return true;
				 }
				 if(weaveItems[i].weave_kind == GameConstants.WIK_RIGHT){
					 if(value - 2 == 1)
						 return true;
				 }
			}else{
				 return true;
			}
		}
		for (int i = 0; i < GameConstants.MAX_ZI; i++) {
			if(i % 9 == 0 || i % 9 == 8)
				if(temp_cards_index[i] > 0)
					return true;
		}
		
		for (int i = GameConstants.MAX_ZI; i < GameConstants.MAX_ZI_FENG; i++) {
			if(temp_cards_index[i] > 0)
				return true;
		}
		return false;
	}
	
	public List<Integer> getBaoTingOutCard(int _seat_index){
		List<Integer> arr = new ArrayList<Integer>();
		int[] temp_cards_index = Arrays.copyOf(GRR._cards_index[_seat_index], GRR._cards_index[_seat_index].length);
		for(int i = 0; i < _playerStatus[_seat_index]._hu_out_card_count; i++){
			int ting_card = _playerStatus[_seat_index]._hu_out_card_ting[i];
			int index = _logic.switch_to_card_index(ting_card);
			temp_cards_index[index]--;
			if(has_hand_card19(temp_cards_index, GRR._weave_items[_seat_index], GRR._weave_count[_seat_index]))
				arr.add(ting_card);
			temp_cards_index[index]++;
		}
		return arr;
	}

	public boolean is_bao_card(int seat_index, int send_card){
		if(is_bao_ting[seat_index])
			 if(bao_card == send_card || (has_rule(Constants_MJ_DA_LIAN_QH.GAME_RULE_XJMTF) && send_card == 0x11))
					 return true;
		return false;
	}
	
	public boolean can_hand_card_one(int seat_index){
		if(GRR._weave_count[seat_index] < 3)
			return true;
		if(GRR._weave_count[seat_index] == 4)
			return false;
		if(_logic.exist_eat(GRR._weave_items[seat_index], GRR._weave_count[seat_index]) || !has_rule(Constants_MJ_DA_LIAN_QH.GAME_RULE_PIAO_HU))
			return false;
		return true;
	}
	
	public boolean is_jia_zi(int cards_index[], int card) {
		int[] copyOf = Arrays.copyOf(cards_index, cards_index.length);
		if(_logic.get_card_color(card) == 3)
			return false;
		int value = _logic.get_card_value(card);
		if(value == 1 || value == 9)
			return false;
		if(value == 3){
			if(copyOf[_logic.switch_to_card_index(card - 1)] != 0 &&
					copyOf[_logic.switch_to_card_index(card - 2)] != 0){
				copyOf[_logic.switch_to_card_index(card - 1)] -= 1;
				copyOf[_logic.switch_to_card_index(card - 2)] -= 1;
				boolean hu;
					hu = AnalyseCardUtil.analyse_feng_chi_by_cards_index(copyOf, -1, null, 0);
				if(hu)
					return hu;
				copyOf[_logic.switch_to_card_index(card - 1)] += 1;
				copyOf[_logic.switch_to_card_index(card - 2)] += 1;
			}
		}else if(value == 7){
			if(copyOf[_logic.switch_to_card_index(card + 1)] != 0 &&
					copyOf[_logic.switch_to_card_index(card + 2)] != 0){
				copyOf[_logic.switch_to_card_index(card + 1)] -= 1;
				copyOf[_logic.switch_to_card_index(card + 2)] -= 1;
				boolean hu;
					hu = AnalyseCardUtil.analyse_feng_chi_by_cards_index(copyOf, -1, null, 0);
				if(hu)
					return hu;
				copyOf[_logic.switch_to_card_index(card + 1)] += 1;
				copyOf[_logic.switch_to_card_index(card + 2)] += 1;
			}
		}

		if(copyOf[_logic.switch_to_card_index(card - 1)] == 0 ||
				copyOf[_logic.switch_to_card_index(card + 1)] == 0)
			return false;
		copyOf[_logic.switch_to_card_index(card - 1)] -= 1;
		copyOf[_logic.switch_to_card_index(card + 1)] -= 1;
		
		boolean hu;
			hu = AnalyseCardUtil.analyse_feng_chi_by_cards_index(copyOf, -1, null, 0);
		
		return hu;
	}

	/**
	 * 判断抢杠胡
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
			if (_playerStatus[i].is_chi_hu_round()) {
				// 吃胡判断
				ChiHuRight chr = GRR._chi_hu_rights[i];
				chr.set_empty();
				int cbWeaveCount = GRR._weave_count[i];
				action = analyse_chi_hu_card(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr,
						Constants_MJ_DA_LIAN_QH.HU_CARD_TYPE_QIANG_GANG, i);

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

	@Override
	public void process_chi_hu_player_score(int seat_index, int provide_index, int operate_card, boolean zimo) {
		GRR._chi_hu_card[seat_index][0] = operate_card;

		GRR._win_order[seat_index] = 1;
		// 引用权位
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];

		countCardType(chr, seat_index);
		
		int fan = getPaiXingFan(chr, seat_index);
		////////////////////////////////////////////////////// 自摸 算分
		int sealTopScore = getSealTopScore();
		if (zimo || (!chr.opr_and(Constants_MJ_DA_LIAN_QH.CHR_DIAN_PAO_HU).is_empty() && has_rule(Constants_MJ_DA_LIAN_QH.GAME_RULE_DPSJF))) {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index) {
					continue;
				}
				int sfan = (i == GRR._banker_player || seat_index == GRR._banker_player) ? fan + 1 : fan;
				sfan = !is_kai_kou[i] ? sfan + 1 : sfan;
				if(!zimo && i == provide_index)
					sfan++;
				int s = 1 << (sfan - 1);
				if(sealTopScore != 0){
					s = s > sealTopScore ? sealTopScore : s;
				}
				GRR._game_score[i] -= s;
				GRR._game_score[seat_index] += s;
			}
		}else {
			int sfan = (provide_index == GRR._banker_player || seat_index == GRR._banker_player) ? fan + 1 : fan;
			sfan = !is_kai_kou[provide_index] ? sfan + 1 : sfan;
			sfan++;
			int s = 1 << (sfan - 1);
			if(sealTopScore != 0){
				s = s > sealTopScore ? sealTopScore : s;
			}
			GRR._game_score[provide_index] -= s;
			GRR._game_score[seat_index] += s;
		}
		// 设置变量
		GRR._provider[seat_index] = provide_index;
		_status_gang = false;
		_status_gang_hou_pao = false;

		change_player_status(seat_index, GameConstants.INVALID_VALUE);
	}

	private int getSealTopScore() {
		if(has_rule(Constants_MJ_DA_LIAN_QH.GAME_RULE_SCORE_20))
			return 20;
		if(has_rule(Constants_MJ_DA_LIAN_QH.GAME_RULE_SCORE_40))
			return 40;
		if(has_rule(Constants_MJ_DA_LIAN_QH.GAME_RULE_SCORE_80))
			return 80;
		if(has_rule(Constants_MJ_DA_LIAN_QH.GAME_RULE_SCORE_160))
			return 160;
		if(has_rule(Constants_MJ_DA_LIAN_QH.GAME_RULE_SCORE_320))
			return 320;
		return 0;
	}

	private int getPaiXingFan(ChiHuRight chr, int seat_index) {
		int score = 1;
		if(!chr.opr_and(Constants_MJ_DA_LIAN_QH.CHR_ZI_MO).is_empty())
			score += 1;
		if(!chr.opr_and(Constants_MJ_DA_LIAN_QH.CHR_JIA_HU).is_empty())
			score += 1;
		if(!chr.opr_and(Constants_MJ_DA_LIAN_QH.CHR_MO_BAO).is_empty())
			score += 1;
		if(!chr.opr_and(Constants_MJ_DA_LIAN_QH.CHR_CHONG_BAO).is_empty())
			score += 3;
		if(!chr.opr_and(Constants_MJ_DA_LIAN_QH.CHR_GANG_SHANG_KAI_HUA).is_empty())
			score += 2;
		if(!chr.opr_and(Constants_MJ_DA_LIAN_QH.CHR_QIANG_GANG_HU).is_empty() && has_rule(Constants_MJ_DA_LIAN_QH.GAME_RULE_QGHJF))
			score += 2;
		if(!chr.opr_and(Constants_MJ_DA_LIAN_QH.CHR_PIAO_HU).is_empty())
			score += 2;
		return score;
	}
	
	@Override
	public int getTablePlayerNumber() {
		if(getRuleValue(GameConstants.GAME_RULE_ALL_GAME_TWO_PLAYER) == 1) 
			return 2;
		if(getRuleValue(GameConstants.GAME_RULE_ALL_GAME_THREE_PLAYER) == 1) 
			return 3;
		return 4;
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
					if (type == Constants_MJ_DA_LIAN_QH.CHR_DIAN_PAO_HU) {
						result.append(" 点炮胡");
					}
					if (type == Constants_MJ_DA_LIAN_QH.CHR_ZI_MO) {
						result.append(" 自摸");
					}
					if (type == Constants_MJ_DA_LIAN_QH.CHR_PING_HU) {
						result.append(" 平胡");
					}
					if (type == Constants_MJ_DA_LIAN_QH.CHR_QIANG_GANG_HU) {
						result.append(" 抢杠胡");
					}
					if (type == Constants_MJ_DA_LIAN_QH.CHR_GANG_SHANG_KAI_HUA) {
						result.append(" 杠上开花");
					}
					if (type == Constants_MJ_DA_LIAN_QH.CHR_JIA_HU) {
						result.append(" 夹胡");
					}
					if (type == Constants_MJ_DA_LIAN_QH.CHR_MO_BAO) {
						result.append(" 摸宝");
					}
					if (type == Constants_MJ_DA_LIAN_QH.CHR_CHONG_BAO) {
						result.append(" 冲宝");
					}
					if (type == Constants_MJ_DA_LIAN_QH.CHR_PIAO_HU) {
						result.append(" 飘胡");
					}
				} else if (type == Constants_MJ_DA_LIAN_QH.CHR_FANG_PAO) {
					result.append(" 放炮");
				} else if (type == Constants_MJ_DA_LIAN_QH.CHR_BEI_QIANG_GANG) {
					result.append(" 被抢杠");
				}
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
				result.append(" 补杠X" + ming_gang);
			}
			if (fang_gang > 0) {
				result.append(" 放杠X" + fang_gang);
			}
			if (jie_gang > 0) {
				result.append(" 明杠X" + jie_gang);
			}
			
			if(!is_kai_kou[player]){
				result.append(" 未开门");
			}
			GRR._result_des[player] = result.toString();
		}
	}
	
	
	private static final int[][] birdVaule = new int[][]{{1,5,9},{2,6},{3,7},{4,8}};
	private static final int[][] birdVaule3 = new int[][]{{1,4,7},{2,5,8},{3,6,9}};
	public void set_pick_direction_niao_cards(int cards_data[], int card_num, int seat_index) {
		int seat = (seat_index - _cur_banker + getTablePlayerNumber()) % getTablePlayerNumber();
		for (int i = 0; i < card_num; i++) {
			int nValue = _logic.get_card_value(cards_data[i]);
			boolean flag = false;
			if(getTablePlayerNumber() == 4){
				for(int v = 0; v < birdVaule[seat].length; v++){
					if(nValue == birdVaule[seat][v]){
						flag = true;
						break;
					}
				}
			}else{
				for(int v = 0; v < birdVaule3[seat].length; v++){
					if((nValue == birdVaule3[seat][v] && cards_data[i] != 0x35) || (seat_index == _cur_banker && cards_data[i] == 0x35)){
						flag = true;
						break;
					}
				}
			}
			if (flag) {
				GRR._player_niao_cards[seat_index][i] = GRR._cards_data_niao[i] + GameConstants.DING_NIAO_VALID;
				GRR._player_niao_count[seat_index]++;
			}else{
				GRR._player_niao_cards[seat_index][i] = GRR._cards_data_niao[i];
			}

		}
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
			if (GRR._left_card_count > 18 - 4 + getTablePlayerNumber() && !is_bao_ting[i] && GRR._weave_count[i] < 3) {
				if (i == get_banker_next_seat(seat_index)) {
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

				boolean can_peng = true;
				int[] tmp_cards_data = _playerStatus[i].get_cards_abandoned_peng();
				for (int x = 0; x < GameConstants.MAX_ABANDONED_CARDS_COUNT; x++) {
					if (tmp_cards_data[x] == card) {
						can_peng = false;
						break;
					}
				}
				if(!has_rule(Constants_MJ_DA_LIAN_QH.GAME_RULE_ZFBKP) && (card >= 0x35))
					can_peng = false;
				action = _logic.check_peng(GRR._cards_index[i], card);
				if (can_peng && action != 0) {
					playerStatus.add_action(action);
					playerStatus.add_peng(card, seat_index);
					bAroseAction = true;
				}
			}
			if (GRR._left_card_count > 17 - 4 + getTablePlayerNumber() && (gang_count % 2 == 0 || GRR._left_card_count > 18 - 4 + getTablePlayerNumber()) && can_hand_card_one(i)) {
				action = _logic.estimate_gang_card_out_card(GRR._cards_index[i], card);
				if (action != 0 && is_can_gang(i, card)) {
					playerStatus.add_action(GameConstants.WIK_GANG);
					playerStatus.add_gang(card, seat_index, 1);
					bAroseAction = true;
				}
			}
			
			if (_playerStatus[i].is_chi_hu_round()) {
				ChiHuRight chr = GRR._chi_hu_rights[i];
				chr.set_empty();
				int cbWeaveCount = GRR._weave_count[i];
				int card_type = Constants_MJ_DA_LIAN_QH.HU_CARD_TYPE_JIE_PAO;
				action = analyse_chi_hu_card(GRR._cards_index[i],
						GRR._weave_items[i], cbWeaveCount, card, chr,
						card_type, i);
				if (action != 0) {
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

	public int get_ting_card(int[] cards, int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int seat_index) {
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		ChiHuRight chr = new ChiHuRight();

		int count = 0;
		int cbCurrentCard;

		for (int i = 0; i < GameConstants.MAX_ZI_FENG; i++) {
			cbCurrentCard = _logic.switch_to_card_data(i);
			chr.set_empty();

			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card(cbCardIndexTemp, weaveItem, cbWeaveCount, cbCurrentCard, chr,
					GameConstants.CHR_ZI_MO, seat_index)) {
				cards[count++] = cbCurrentCard;
			}
		}
		
		//全听
		if (count == 34) {
			count = 1;
			cards[0] = -1;
		}

		return count;
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
	
	
	public boolean is_yao_jiu(int[] hand_indexs, WeaveItem weaveItem[], int weaveCount) {
    	// 一九判断
		for (int i = 0; i < GameConstants.MAX_ZI_FENG; i ++) {
			if(hand_indexs[i] == 0)
				continue;
			if(_logic.is_magic_index(i))
				continue;
			if(i >= GameConstants.MAX_ZI)
        		return false;
			// 无效判断
			int value = _logic.get_card_value(_logic.switch_to_card_data(i));
			if (value != 1 && value != 9)
				return false;
		}
    	
		// 落地牌都是19
        for (int i = 0; i < weaveCount; i++) {
        	if(_logic.switch_to_card_index(weaveItem[i].center_card) >= GameConstants.MAX_ZI)
        		return false;
            int value = _logic.get_card_value(weaveItem[i].center_card);
            if (value != 1 && value != 9)
				return false;
        }
        return true;
    }
    
    // 杠牌分析 包括补杠 check_weave检查补杠
    public int analyse_gang(int cards_index[], WeaveItem WeaveItem[], int cbWeaveCount,
            GangCardResult gangCardResult, boolean check_weave, int cards_abandoned_gang[], int seat_index) {
        // 设置变量
        int cbActionMask = GameConstants.WIK_NULL;

        // 手上杠牌
        //if(can_hand_card_one(seat_index)){
        	for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
        		if (cards_index[i] == 4 && cards_abandoned_gang[i] != 1 && !_logic.is_magic_index(i)) {
        			cbActionMask |= GameConstants.WIK_GANG;
        			int index = gangCardResult.cbCardCount++;
        			gangCardResult.cbCardData[index] = _logic.switch_to_card_data(i);
        			gangCardResult.isPublic[index] = 0;// 暗刚
        			gangCardResult.type[index] = GameConstants.GANG_TYPE_AN_GANG;
        		}
        	}
        //}

        if (check_weave) {
            // 组合杠牌，包括以前能杠，但是不杠，发牌之后而选择补杠的
            for (int i = 0; i < cbWeaveCount; i++) {
                if (WeaveItem[i].weave_kind == GameConstants.WIK_PENG) {
                    for (int j = 0; j < GameConstants.MAX_INDEX; j++) {
                        if (cards_index[j] != 1) {
                            continue;
                        } else if(WeaveItem[i].is_vavild && cards_abandoned_gang[j] != 1){
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
            if(has_rule(Constants_MJ_DA_LIAN_QH.GAME_RULE_XJMTF))
            	game_end.addEspecialShowCards(0x11);
            if(!has_rule(Constants_MJ_DA_LIAN_QH.GAME_RULE_XJMTF) || bao_card != 0x11)
            	game_end.addEspecialShowCards(bao_card);

            GRR._end_type = reason;

            // 杠牌，每个人的分数
            float lGangScore[] = new float[getTablePlayerNumber()];
            for (int i = 0; i < getTablePlayerNumber(); i++) {
            	for (int j = 0; j < GRR._gang_score[i].gang_count && reason == GameConstants.Game_End_NORMAL; j++) {
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
        }

        if (!is_sys()) {
            GRR = null;
        }

        if (is_sys()) {
            clear_score_in_gold_room();
        }
        for (int i = 0; i < getTablePlayerNumber(); i++) {
        	_player_result.biaoyan[i] = 0;
		}
        // 错误断言
        return false;
    }
    
	
	public boolean is_can_gang(int seat_index, int card) {
		if(!is_bao_ting[seat_index])
			return true;
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = GRR._cards_index[seat_index][i];
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

		Set<Integer> s = new HashSet<Integer>();
		for (int i = 0; i < GameConstants.MAX_ZI_FENG; i++) {
			int cbCurrentCard = _logic.switch_to_card_data(i);
			chr.set_empty();
			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card(cbCardIndexTemp, weaves,
					weave_count, cbCurrentCard, chr,
					GameConstants.CHR_ZI_MO, seat_index)) {
				s.add(cbCurrentCard);
			}
		}
		
		int ting_cards[] = _playerStatus[seat_index]._hu_cards;
		int ting_count = _playerStatus[seat_index]._hu_card_count;
		
		for (int i = 0; i < ting_count; i++) {
			if(ting_cards[i] == -1)
				break;
			if(!s.contains(ting_cards[i]))
				return false;
		}
			
		return true;
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
	
	public boolean is_first_bao_ting(){
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if(is_bao_ting[i])
				return false;
		}
		return true;
	}
	
	public void set_bao_pai(){
		Set<Integer> set = new HashSet<Integer>();
		for (int i = GRR._left_card_count; i > 0; i--) {
			if(set.contains(_repertory_card[_all_card_len - i])){
				bao_card = _repertory_card[_all_card_len - i];
				_repertory_card[_all_card_len - i] = _repertory_card[_all_card_len - GRR._left_card_count];
				GRR._left_card_count--;
				_send_card_count++;
				return;
			}else{
				set.add(_repertory_card[_all_card_len - i]);
			}
		}
		bao_card = _repertory_card[_all_card_len - 1];
		if(DEBUG_MAGIC_CARD)
			bao_card = magic_card_decidor;
		gang_dispatch_count++;
	}
	
	public boolean has_bao(){
		if(bao_card == 0)
			return true;	
		for (int i = GRR._left_card_count; i > 0; i--) {
			if(bao_card == _repertory_card[_all_card_len - i]){
				return true;
			}
		}
		return false;
	}
	
	public boolean is_chong_bao(int _seat_index){
		if (AbstractMJTable.DEBUG_CARDS_MODE) {
			bao_card = 0x02;
		}
		ChiHuRight chr = GRR._chi_hu_rights[_seat_index];
		chr.set_empty();
		if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card(GRR._cards_index[_seat_index], GRR._weave_items[_seat_index],
				GRR._weave_count[_seat_index], bao_card, chr,
				GameConstants.CHR_ZI_MO, _seat_index)) {
			chr.opr_or(Constants_MJ_DA_LIAN_QH.CHR_CHONG_BAO);
			_playerStatus[_seat_index].add_action(GameConstants.WIK_ZI_MO);
			_playerStatus[_seat_index].add_zi_mo(bao_card, _seat_index);
			GameSchedule.put(() -> {
				change_player_status(_seat_index, GameConstants.Player_Status_OPR_CARD);
				operate_player_action(_seat_index, false);
			}, 2, TimeUnit.SECONDS);
			return true;
		} else {
			chr.set_empty();
			return false;
		}
	}
	
	public void execute_fen_zhang(int _seat_index){
		fen_zhang_start_seat_index = fen_zhang_start_seat_index == -1 ? _seat_index :fen_zhang_start_seat_index;
		
		_send_card_count++;
		_send_card_data = _repertory_card[_all_card_len - GRR._left_card_count];
		--GRR._left_card_count;
		_provide_player = _seat_index;

		if (AbstractMJTable.DEBUG_CARDS_MODE) {
			_send_card_data = 0x03;
		}
		
		PlayerStatus curPlayerStatus = _playerStatus[_seat_index];
		ChiHuRight chr = GRR._chi_hu_rights[_seat_index];
		// 判断是否是杠后抓牌,杠开花只算接杠
		int card_type = Constants_MJ_DA_LIAN_QH.HU_CARD_TYPE_ZI_MO;
		// 检查牌型,听牌
		int action = analyse_chi_hu_card(GRR._cards_index[_seat_index], GRR._weave_items[_seat_index],
				GRR._weave_count[_seat_index], _send_card_data, chr, card_type, _seat_index);

		if (action != GameConstants.WIK_NULL) {
			curPlayerStatus.add_action(GameConstants.WIK_ZI_MO);
			curPlayerStatus.add_zi_mo(_send_card_data, _seat_index);
		} else {
			GRR._chi_hu_rights[_seat_index].set_empty();
			chr.set_empty();
		}

		GRR._cards_index[_seat_index][_logic.switch_to_card_index(_send_card_data)]++;
		
		int show_send_card = _send_card_data;
		if (is_bao_card(_seat_index, _send_card_data)) {
			show_send_card += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
		}
		// 显示牌
		operate_player_get_card(_seat_index, 1, new int[] { show_send_card }, GameConstants.INVALID_SEAT);
		if (curPlayerStatus.has_action()) {
			change_player_status(_seat_index, GameConstants.Player_Status_OPR_CARD);
			operate_player_action(_seat_index, false);
		}else{
			operate_effect_action(_seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { Constants_MJ_DA_LIAN_QH.ACTION_MEI_HU_DAO }, 1, GameConstants.INVALID_SEAT);
			exe_dispatch_card((_seat_index + 1) % getTablePlayerNumber(), GameConstants.WIK_NULL, 2000);
		}
	}
	
	
	
	/**
	 * 玩家动作--通知玩家弹出/关闭操作
	 * 
	 * @param seat_index
	 * @param close
	 * @return
	 */
	public boolean operate_player_action(int seat_index, boolean close) {
		PlayerStatus curPlayerStatus = _playerStatus[seat_index];

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_PLAYER_ACTION);
		roomResponse.setTarget(seat_index);
		this.load_common_status(roomResponse);

		if (close == true) {
			GRR.add_room_response(roomResponse);
			// 通知玩家关闭
			this.send_response_to_player(seat_index, roomResponse);
			return true;
		}
		for (int i = 0; i < curPlayerStatus._action_count; i++) {
			roomResponse.addActions(curPlayerStatus._action[i]);
			if(curPlayerStatus._action[i] == GameConstants.WIK_BAO_TING){
				roomResponse.addAllDouliuzi(getBaoTingOutCard(seat_index));
			}
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
	
	public void show_tou_zi(int seat_index) {
		tou_zi_dian_shu[0] = RandomUtil.getRandomNumber(Integer.MAX_VALUE) % 6 + 1;
		tou_zi_dian_shu[1] = RandomUtil.getRandomNumber(Integer.MAX_VALUE) % 6 + 1;

		operate_tou_zi_effect(tou_zi_dian_shu[0], tou_zi_dian_shu[1], time_for_tou_zi_animation, time_for_tou_zi_fade, seat_index);
	}
	
	public boolean operate_tou_zi_effect(int tou_zi_one, int tou_zi_two, int time_for_animate, int time_for_fade, int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_EFFECT_ACTION);
		roomResponse.setEffectType(GameConstants.Effect_Action_SHAI_ZI);
		roomResponse.setTarget(seat_index);
		roomResponse.setEffectCount(2);
		roomResponse.addEffectsIndex(tou_zi_one);
		roomResponse.addEffectsIndex(tou_zi_two);
		roomResponse.setEffectTime(time_for_animate);
		roomResponse.setStandTime(time_for_fade);
		send_response_to_room(roomResponse);
		if (GRR != null)
			GRR.add_room_response(roomResponse);

		return true;
	}
	
    @Override
	public void test_cards() {
		int[] cards_of_player0 = new int[] { 0x01,0x02,0x03,0x14,0x15,0x16,0x22,0x22,0x22,0x28,0x28,0x28,0x28 };
		int[] cards_of_player1 = new int[] { 0x08,0x16,0x17,0x18,0x11,0x11,0x08,0x24,0x25,0x26,0x27,0x28,0x29 };
		int[] cards_of_player2 = new int[] { 0x01,0x02,0x03,0x14,0x15,0x22,0x22,0x22,0x22,0x26,0x26,0x26,0x26 };
		int[] cards_of_player3 = new int[] { 0x01,0x02,0x03,0x14,0x15,0x22,0x22,0x22,0x22,0x26,0x26,0x26,0x26 };

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
