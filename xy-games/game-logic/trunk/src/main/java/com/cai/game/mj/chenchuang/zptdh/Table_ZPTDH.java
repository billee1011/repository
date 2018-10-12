package com.cai.game.mj.chenchuang.zptdh;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import protobuf.clazz.Protocol.GameEndResponse;
import protobuf.clazz.Protocol.GameStartResponse;
import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomInfo;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;
import protobuf.clazz.mj.GdtdhPro.GDTDHGameEndResponse;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.mj.Constants_MJ_ZPTDH;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.GangCardResult;
import com.cai.common.domain.Player;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.WeaveItem;
import com.cai.common.util.PBUtil;
import com.cai.common.util.RandomUtil;
import com.cai.game.mj.AbstractMJTable;
import com.cai.game.mj.MJType;
import com.cai.game.mj.ThreeDimension;
import com.cai.game.util.AnalyseCardUtil;
import com.cai.game.util.GameUtilConstants;
import com.cai.service.PlayerServiceImpl;

/**
 * chenchuang
 * 欢乐扣点点
 */
@ThreeDimension
public class Table_ZPTDH extends AbstractMJTable {

	private static final long serialVersionUID = 1L;
	public int banker_out_first_card;
	public boolean is_gen_zhuang_valid;
	public boolean is_gen_zhuang;
	public Map<Integer, Integer> cBMap = new HashMap<Integer, Integer>();
	public int[] send_card_count = new int[getTablePlayerNumber()];
	public int[] hu_dec_type = new int[getTablePlayerNumber()];
	
	public Table_ZPTDH() {
		
		super(MJType.GAME_TYPE_MJ_ZPTDH);
	}

	@Override
	protected void onInitTable() {
		_handler_chi_peng = new HandlerChiPeng_ZPTDH();
		_handler_dispath_card = new HandlerDispatchCard_ZPTDH();
		_handler_gang = new HandlerGang_ZPTDH();
		_handler_out_card_operate = new HandlerOutCardOperate_ZPTDH();
	}

	public void exe_select_magic(){
		if(hasRule(Constants_MJ_ZPTDH.GAME_RULE_NO_LAI_ZI))
			return;
		int _da_dian_card = 0;
		int card_next = 0;
		if(hasRule(Constants_MJ_ZPTDH.GAME_RULE_FAN_LAI_ZI)){
			_da_dian_card = _repertory_card[_all_card_len - GRR._left_card_count];
			
			if (DEBUG_CARDS_MODE){
				_da_dian_card = 0x12;
			}
			if(DEBUG_MAGIC_CARD)
				_da_dian_card = magic_card_decidor;
			
	        
	        /*GameSchedule.put(new Runnable() {
	            @Override
	            public void run() {
	                // 将翻出来的牌从牌桌的正中央移除
	                operate_show_card(_cur_banker, GameConstants.Show_Card_Center, 0, null, GameConstants.INVALID_SEAT);
	            }
	        }, 3000, TimeUnit.MILLISECONDS);*/

	        int cur_value = _logic.get_card_value(_da_dian_card);
	        int cur_color = _logic.get_card_color(_da_dian_card);

	        if (cur_color == 3) {
	    		if(cur_value == 7)
	    			card_next = _da_dian_card - 6;
	    		else
	    			card_next = _da_dian_card + 1;
	        } else {
	            if (cur_value == 9) {
	                card_next = _da_dian_card - 8;
	            } else {
	                card_next = _da_dian_card + 1;
	            }
	        }
		}else{
			if(hasRule(Constants_MJ_ZPTDH.GAME_RULE_HONG_LAI_ZI))
				card_next = 0x35;	
			if(hasRule(Constants_MJ_ZPTDH.GAME_RULE_BAI_LAI_ZI))
				card_next = 0x37;
		}
		
        

        int magic_card_index = _logic.switch_to_card_index(card_next);

        // 添加鬼
        _logic.add_magic_card_index(magic_card_index);
        GRR._especial_card_count = 2;
        GRR._especial_show_cards[0] = card_next + GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
        GRR._especial_show_cards[1] = _da_dian_card;
        
        // 将翻出来的牌显示在牌桌的正中央
        operate_show_card(_cur_banker, GameConstants.Show_Card_Center, 1, new int[] { _da_dian_card },
                GameConstants.INVALID_SEAT);

        // 处理每个玩家手上的牌，如果有王牌，处理一下
        for (int i = 0; i < getTablePlayerNumber(); i++) {
            int[] hand_cards = new int[GameConstants.MAX_COUNT];
            int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[i], hand_cards);
            for (int j = 0; j < hand_card_count; j++) {
                if (_logic.is_magic_card(hand_cards[j])) {
                    hand_cards[j] += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
                }
            }
            // 玩家客户端刷新一下手牌
            operate_player_cards(i, hand_card_count, hand_cards, 0, null);
        }

	}
	
	/**第一局建房者为庄家，若为代开房/俱乐部开房（即开房者自己未进入牌局），则随机一位为庄家;*/
	@Override
	protected void initBanker() {
		long id = getCreate_player().getAccount_id();
		if(get_player(id) == null)
			_cur_banker = RandomUtil.getRandomNumber(getTablePlayerNumber());
	}


	@Override
	public int getTablePlayerNumber() {
		if(playerNumber > 0) 
			return playerNumber;
		if(getRuleValue(GameConstants.GAME_RULE_ALL_GAME_THREE_PLAYER) == 1) 
			return 3;
		return 4;
	}
	
	public boolean hasRule(int rule){
		if(rule == 11 || rule == 12 || rule == 13 || rule == 14)
			return ruleMap.containsKey(11) && ruleMap.get(11) == rule;
		if(rule == 15 || rule == 16 || rule == 17)
			return ruleMap.containsKey(15) && ruleMap.get(15) == rule;
		if(rule == 18 || rule == 19 || rule == 20 || rule == 21 || rule == 41)
			return ruleMap.containsKey(18) && ruleMap.get(18) == rule;
		if(rule == 22 || rule == 23 || rule == 24)
			return ruleMap.containsKey(22) && ruleMap.get(22) == rule;
		if(rule == 25 || rule == 26 || rule == 27 || rule == 28)
			return ruleMap.containsKey(25) && ruleMap.get(25) == rule;
		return getRuleValue(rule) == 1;
	}
	
	@Override
	protected void init_shuffle() {
		int[] card = Constants_MJ_ZPTDH.CARD_DATA_DAI_FENG;
		if(hasRule(Constants_MJ_ZPTDH.GAME_RULE_WU_ZI_PAI)){
			card = Constants_MJ_ZPTDH.CARD_DATA_BU_DAI_FENG;
			if(hasRule(Constants_MJ_ZPTDH.GAME_RULE_HONG_LAI_ZI))
				card = Constants_MJ_ZPTDH.CARD_DATA_HONG_ZHONG;
			if(hasRule(Constants_MJ_ZPTDH.GAME_RULE_BAI_LAI_ZI))
				card = Constants_MJ_ZPTDH.CARD_DATA_BAI_BAN;
		}
		_repertory_card = new int[card.length];
		shuffle(_repertory_card, card);
	}
	/**
	 * 洗完牌执行开始
	 */
	@Override
	protected boolean on_game_start() {
		//初始化常量
		is_gen_zhuang_valid = getTablePlayerNumber() == 4 && hasRule(Constants_MJ_ZPTDH.GAME_RULE_GEN_ZHUANG);
		banker_out_first_card = 0;
		is_gen_zhuang = false;
		cBMap.clear();
		send_card_count = new int[getTablePlayerNumber()];
		hu_dec_type = new int[getTablePlayerNumber()];
		//选鬼
		_logic.clean_magic_cards();
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
		//选鬼
		exe_select_magic();
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
	
	public int getHorseCount(){
		int horse = 0;
		if(hasRule(Constants_MJ_ZPTDH.GAME_RULE_MA_2))
			horse = 2;
		if(hasRule(Constants_MJ_ZPTDH.GAME_RULE_MA_4))
			horse = 4;
		if(hasRule(Constants_MJ_ZPTDH.GAME_RULE_MA_6))
			horse = 6;
		if(hasRule(Constants_MJ_ZPTDH.GAME_RULE_MA_8))
			horse = 8;
		return horse;
	}

	@Override
	public int analyse_chi_hu_card(int[] cards_index, WeaveItem[] weaveItems, int weave_count, int cur_card, ChiHuRight chiHuRight, int card_type,
			int _seat_index) {
		//拷贝把当前牌加入手牌
		int cur_card_index = _logic.switch_to_card_index(cur_card);
		int[] temp_cards_index = Arrays.copyOf(cards_index, cards_index.length);
		temp_cards_index[cur_card_index]++;
		
		boolean is_magic_dian_pao = _logic.is_magic_card(cur_card) &&( card_type == Constants_MJ_ZPTDH.HU_CARD_TYPE_QIANG_GANG_HU || card_type == Constants_MJ_ZPTDH.HU_CARD_TYPE_DIAN_PAO);
		
		//七小对
		int qi_dui = is_qi_xiao_dui(cards_index, weaveItems, weave_count, cur_card, is_magic_dian_pao);
		if(qi_dui == 2 && !hasRule(Constants_MJ_ZPTDH.GAME_RULE_HH_QI_XIAO_DUI_ADD_5)){
			qi_dui = 1;
		}
		if(qi_dui == 1 && !hasRule(Constants_MJ_ZPTDH.GAME_RULE_QI_XIAO_DUI_ADD_3)){
			qi_dui = 0;
		}
		
			
		//十三幺
		boolean shiSanYao = hasRule(Constants_MJ_ZPTDH.GAME_RULE_SHI_SAN_YAO_ADD_13) && isShiSanYao(temp_cards_index, is_magic_dian_pao);
		
		//得到癞子牌的个数，和索引
		int[] magic_cards_index = new int[GameUtilConstants.MAX_MAGIC_INDEX_COUNT];
        int magic_card_count = _logic.get_magic_card_count();
        if (magic_card_count > 2) { // 一般只有两种癞子牌存在
            magic_card_count = 2;
        }
        for (int i = 0; i < magic_card_count; i++) {
            magic_cards_index[i] = _logic.get_magic_card_index(i);
        }
        //是否胡牌牌型
  		boolean analyse_win_by_cards_index = AnalyseCardUtil.analyse_win_by_cards_index(cards_index, cur_card_index, magic_cards_index, magic_card_count);
  		if(is_magic_dian_pao)
  			analyse_win_by_cards_index = AnalyseCardUtil.analyse_win_by_cards_index_taojiang(cards_index, cur_card_index, magic_cards_index, magic_card_count);
  		
  		if(!(analyse_win_by_cards_index || qi_dui > 0 || shiSanYao)){
  			return GameConstants.WIK_NULL;
  		}
  		
		//0字一色1清一色2混一色
		int pai_xing_se = get_pai_xing_se(temp_cards_index, weaveItems, weave_count, is_magic_dian_pao);
		if(pai_xing_se == 0 && !hasRule(Constants_MJ_ZPTDH.GAME_RULE_ZI_YI_SE_ADD_9))
			pai_xing_se = -1;
		else if(pai_xing_se == 1 && !hasRule(Constants_MJ_ZPTDH.GAME_RULE_QING_YI_SE_ADD_5))
			pai_xing_se = -1;
		else if(pai_xing_se == 2 && !hasRule(Constants_MJ_ZPTDH.GAME_RULE_HUN_YI_SE_ADD_2))
			pai_xing_se = -1;
		
  		//碰碰胡
  		boolean is_peng_peng_hu = hasRule(Constants_MJ_ZPTDH.GAME_RULE_PENG_PENG_HU_ADD_2) && 
  				!_logic.exist_eat(weaveItems, weave_count) && 
  				AnalyseCardUtil.analyse_peng_hu_by_cards_index(cards_index, cur_card_index, magic_cards_index, magic_card_count);
  		//大三元
  		boolean is_da_san_yuan = hasRule(Constants_MJ_ZPTDH.GAME_RULE_DA_SAN_YUAN_ADD_10) && is_da_san_yuan(temp_cards_index, weaveItems, weave_count, is_magic_dian_pao);
  		//大四喜
  		boolean is_da_si_xi = hasRule(Constants_MJ_ZPTDH.GAME_RULE_DA_SI_XI_ADD_10) && is_da_si_xi(temp_cards_index, weaveItems, weave_count, is_magic_dian_pao);
  		
  		//1混、2清幺九，0不是
  		int yao_jiu = yao_jiu(temp_cards_index, weaveItems, weave_count, is_magic_dian_pao);
  		if(yao_jiu == 2 && !hasRule(Constants_MJ_ZPTDH.GAME_RULE_HUN_YAO_JIU_ADD_7))
  			yao_jiu = 0;
  		else if(yao_jiu == 1 && !hasRule(Constants_MJ_ZPTDH.GAME_RULE_QING_YAO_JIU_ADD_9))
  			yao_jiu = 0;
  		
		if(shiSanYao)
			chiHuRight.opr_or(Constants_MJ_ZPTDH.CHR_SHI_SAN_YAO);
		else if(is_da_si_xi)
			chiHuRight.opr_or(Constants_MJ_ZPTDH.CHR_DA_SI_XI);
		else if(is_da_san_yuan)
			chiHuRight.opr_or(Constants_MJ_ZPTDH.CHR_DA_SAN_YUAN);
		else if(pai_xing_se == 1 && qi_dui == 2)
			chiHuRight.opr_or(Constants_MJ_ZPTDH.CHR_QHH_QI_XIAO_DUI);
		else if(pai_xing_se == 0)
			chiHuRight.opr_or(Constants_MJ_ZPTDH.CHR_ZI_YI_SE);
		else if(yao_jiu == 2)
			chiHuRight.opr_or(Constants_MJ_ZPTDH.CHR_QING_YOU_JIU);
		else if(pai_xing_se == 1 && qi_dui == 1)
			chiHuRight.opr_or(Constants_MJ_ZPTDH.CHR_QING_DUI);
		else if(yao_jiu == 1)
			chiHuRight.opr_or(Constants_MJ_ZPTDH.CHR_HUN_YAO_JIU);
		else if(pai_xing_se == 1 && is_peng_peng_hu)
			chiHuRight.opr_or(Constants_MJ_ZPTDH.CHR_QING_PENG);
		else if(pai_xing_se == 2 && qi_dui == 2)
			chiHuRight.opr_or(Constants_MJ_ZPTDH.CHR_HYS_HH_QI_XIAO_DUI);
		else if(pai_xing_se == 1)
			chiHuRight.opr_or(Constants_MJ_ZPTDH.CHR_QING_YI_SE);
		else if(qi_dui == 2)
			chiHuRight.opr_or(Constants_MJ_ZPTDH.CHR_HH_QI_XIAO_DUI);
		else if(pai_xing_se == 2 && qi_dui == 1)
			chiHuRight.opr_or(Constants_MJ_ZPTDH.CHR_HYS_QI_XIAO_DUI);
		else if(pai_xing_se == 2 && is_peng_peng_hu)
			chiHuRight.opr_or(Constants_MJ_ZPTDH.CHR_HUN_PENG);
		else if(qi_dui == 1)
			chiHuRight.opr_or(Constants_MJ_ZPTDH.CHR_QI_XIAO_DUI);
		else if(pai_xing_se == 2)
			chiHuRight.opr_or(Constants_MJ_ZPTDH.CHR_HUN_YI_SE);
		else if(is_peng_peng_hu)
			chiHuRight.opr_or(Constants_MJ_ZPTDH.CHR_PENG_PENG_HU);
		else{
			if(hasRule(Constants_MJ_ZPTDH.GAME_RULE_BU_NENG_JI_HU))
				return GameConstants.WIK_NULL;
			if(card_type == Constants_MJ_ZPTDH.HU_CARD_TYPE_DIAN_PAO
					&& hasRule(Constants_MJ_ZPTDH.GAME_RULE_JI_HU_BU_KE_HU))
				return GameConstants.WIK_NULL;
			chiHuRight.opr_or(Constants_MJ_ZPTDH.CHR_PING_HU);
		}
		
		if (card_type == Constants_MJ_ZPTDH.HU_CARD_TYPE_ZI_MO) {
			chiHuRight.opr_or(Constants_MJ_ZPTDH.CHR_ZI_MO);//自摸
		} else if (card_type == Constants_MJ_ZPTDH.HU_CARD_TYPE_QIANG_GANG_HU) {
			chiHuRight.opr_or(Constants_MJ_ZPTDH.CHR_QIANG_GANG);//抢杠胡
		}else if (card_type == Constants_MJ_ZPTDH.HU_CARD_TYPE_DIAN_PAO) {
			chiHuRight.opr_or(Constants_MJ_ZPTDH.CHR_DIAN_PAO);//点炮胡
		}else if (card_type == Constants_MJ_ZPTDH.HU_CARD_TYPE_FANG_GANG_KAI_HU) {
			chiHuRight.opr_or(Constants_MJ_ZPTDH.CHR_FANG_GANG_KAI);//放杠杠开
		}
		
		return GameConstants.WIK_CHI_HU;
	}
	
	private int yao_jiu(int[] temp_cards_index, WeaveItem[] weaveItems,
			int weave_count, boolean is_magic_dian_pao) {
		boolean has_feng = false;
		// 一九判断
		for (int i = 0; i < GameConstants.MAX_ZI_FENG; i ++) {
			if(temp_cards_index[i] == 0)
				continue;
			if(_logic.is_magic_index(i) && !is_magic_dian_pao)
				continue;
			if(i >= GameConstants.MAX_ZI){
				has_feng = true;
				continue;
			}
			// 无效判断
			int value = _logic.get_card_value(_logic.switch_to_card_data(i));
			if (value != 1 && value != 9)
				return 0;
		}
    	
		// 落地牌都是19
        for (int i = 0; i < weave_count; i++) {
        	if(_logic.switch_to_card_index(weaveItems[i].center_card) >= GameConstants.MAX_ZI){
        		has_feng = true;
        		continue;
        	}
            int value = _logic.get_card_value(weaveItems[i].center_card);
            if (value != 1 && value != 9)
            	return 0;
        }
        return has_feng ? 1 : 2;
	}

	private boolean is_da_san_yuan(int[] temp_cards_index, WeaveItem[] weaveItems,
			int weave_count, boolean is_magic_dian_pao) {
		int[] copyOf = Arrays.copyOf(temp_cards_index, temp_cards_index.length);
		int magic_card_index = _logic.get_magic_card_index(0);
		int magic_card_count = is_magic_dian_pao ? copyOf[magic_card_index] - 1 : copyOf[magic_card_index];
		HashSet<Integer> set = new HashSet<Integer>();
		for (int i = 0; i < weave_count; i++) {
			if(weaveItems[i].weave_kind == GameConstants.WIK_GANG &&
					weaveItems[i].center_card > 0x34)
				set.add(weaveItems[i].center_card);
		}
		if(set.size() == 3)
			return true;
		
		for (int i = 31; i < 34; i++) {
			if(!set.contains(_logic.switch_to_card_data(i))){
				if(i != magic_card_index){
					if(copyOf[i] >= 3){
						copyOf[i] -= 3;
					}else if(copyOf[i] + magic_card_count >= 3){
						int count = copyOf[i] >= 3 ? 0 : 3 - copyOf[i];
						magic_card_count -= count;
						copyOf[magic_card_index] -= count;
						copyOf[i] = 0;
					}else{
						return false;
					}
				}else{
					if(copyOf[i] >= 3){
						copyOf[i] -= 3;
						magic_card_count -= 3;
					}else{
						return false;
					}
				}
			}
		}
		
		boolean analyse_win_by_cards_index = AnalyseCardUtil.analyse_win_by_cards_index(copyOf, -1, new int[]{magic_card_index}, 1);
  		if(is_magic_dian_pao && copyOf[magic_card_index] > 0)
  			analyse_win_by_cards_index = AnalyseCardUtil.analyse_win_by_cards_index_taojiang(copyOf, -1, new int[]{magic_card_index}, 1);
		return analyse_win_by_cards_index;
	}

	private boolean is_da_si_xi(int[] temp_cards_index,
			WeaveItem[] weaveItems, int weave_count, boolean is_magic_dian_pao) {
		int[] copyOf = Arrays.copyOf(temp_cards_index, temp_cards_index.length);
		int magic_card_index = _logic.get_magic_card_index(0);
		int magic_card_count = is_magic_dian_pao ? copyOf[magic_card_index] - 1 : copyOf[magic_card_index];
		HashSet<Integer> set = new HashSet<Integer>();
		for (int i = 0; i < weave_count; i++) {
			if(weaveItems[i].weave_kind == GameConstants.WIK_GANG &&
					weaveItems[i].center_card >= 0x31 && weaveItems[i].center_card <= 0x34)
				set.add(weaveItems[i].center_card);
		}
		if(set.size() == 4)
			return true;
		
		for (int i = 27; i < 31; i++) {
			if(!set.contains(_logic.switch_to_card_data(i))){
				if(i != magic_card_index){
					if(copyOf[i] >= 3){
						copyOf[i] -= 3;
					}else if(copyOf[i] + magic_card_count >= 3){
						int count = copyOf[i] >= 3 ? 0 : 3 - copyOf[i];
						magic_card_count -= count;
						copyOf[magic_card_index] -= count;
						copyOf[i] = 0;
					}else{
						return false;
					}
				}else{
					if(copyOf[i] >= 3){
						copyOf[i] -= 3;
						magic_card_count -= 3;
					}else{
						return false;
					}
				}
			}
		}
		
		boolean analyse_win_by_cards_index = AnalyseCardUtil.analyse_win_by_cards_index(copyOf, -1, new int[]{magic_card_index}, 1);
  		if(is_magic_dian_pao && copyOf[magic_card_index] > 0)
  			analyse_win_by_cards_index = AnalyseCardUtil.analyse_win_by_cards_index_taojiang(copyOf, -1, new int[]{magic_card_index}, 1);
		return analyse_win_by_cards_index;
	}

	private boolean isShiSanYao(int cards_index[], boolean is_magic_dian_pao) {
		//不能有非19的牌
		for (int i = 0; i < GameConstants.MAX_ZI; i++) {
			if(_logic.is_magic_index(i) && !is_magic_dian_pao)
				continue;
			if(!(i % 9 == 0 || (i + 1) % 9 == 0) && cards_index[i] > 0)
				return false;
		}
		int que = 0;//十三幺缺的个数
		// 一九判断
		for (int i = 0; i < GameConstants.MAX_ZI; i += 9) {
			// 无效判断
			if (cards_index[i] == 0 || (_logic.is_magic_index(i) && !is_magic_dian_pao))
				que++;
			if (cards_index[i + 8] == 0 || (_logic.is_magic_index(i + 8) && !is_magic_dian_pao))
				que++;
		}
		// 风牌判断
		for (int i = GameConstants.MAX_ZI; i < GameConstants.MAX_ZI_FENG; i++) {
		    if (cards_index[i] == 0 || (_logic.is_magic_index(i) && !is_magic_dian_pao))
		    	que++;
		}
		int magic_count = cards_index[_logic.get_magic_card_index(0)];
		if(is_magic_dian_pao)
			magic_count--;
		if(magic_count < que){
			return false;
		}
		return true;
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
			if ( _playerStatus[i].is_chi_hu_round() && hasRule(Constants_MJ_ZPTDH.GAME_RULE_KE_QIANG_GANG_HU)) {
				// 吃胡判断
				ChiHuRight chr = GRR._chi_hu_rights[i];
				chr.set_empty();
				int cbWeaveCount = GRR._weave_count[i];
				action = analyse_chi_hu_card(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr,
						Constants_MJ_ZPTDH.HU_CARD_TYPE_QIANG_GANG_HU, i);

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
			
		int score = getPaiXingScore(chr, seat_index, zimo);
		int horse = GRR._player_niao_count[seat_index];
		score = horse > 0 ? (horse + 1) * score : score;
		
		
		int p = getCBPlayer(chr, seat_index, provide_index, zimo);
		////////////////////////////////////////////////////// 自摸 算分
		
		if (zimo || !chr.opr_and(Constants_MJ_ZPTDH.CHR_QIANG_GANG).is_empty()) {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index) {
					continue;
				}
				if(p > -1)
					GRR._game_score[p] -= score;
				else
					GRR._game_score[i] -= score;
				GRR._game_score[seat_index] += score;
			}
		}else {
			int s = score;
			if(p > -1){
				s = score * (getTablePlayerNumber() - 1);
				GRR._game_score[p] -= s;
			}else
				GRR._game_score[provide_index] -= s;
			GRR._game_score[seat_index] += s;
		}
		// 设置变量
		GRR._provider[seat_index] = provide_index;
		_status_gang = false;
		_status_gang_hou_pao = false;

		change_player_status(seat_index, GameConstants.INVALID_VALUE);
	}


	private int getCBPlayer(ChiHuRight chr, int seat_index, int provide_index, boolean zimo) {
		int p = -1;
		if(hasRule(Constants_MJ_ZPTDH.GAME_RULE_12_LD_CB) && cBMap.containsKey(seat_index)){
			p = cBMap.get(seat_index);
			hu_dec_type[p] = zimo ? 11 : 10;
			return p;
		}
		if(!chr.opr_and(Constants_MJ_ZPTDH.CHR_FANG_GANG_KAI).is_empty()){
			p = GRR._weave_items[seat_index][GRR._weave_count[seat_index] - 1].provide_player;
			hu_dec_type[p] = 11;
			return p;
		}
		if(hasRule(Constants_MJ_ZPTDH.GAME_RULE_QG_CB) && !chr.opr_and(Constants_MJ_ZPTDH.CHR_QIANG_GANG).is_empty()){
			p = provide_index;
			hu_dec_type[p] = 10;
			return p;
		}
		return p;
	}

	private int getPaiXingScore(ChiHuRight chr, int seat_index, boolean zimo) {
		int score = 1;
		if(!chr.opr_and(Constants_MJ_ZPTDH.CHR_SHI_SAN_YAO).is_empty()
				|| !chr.opr_and(Constants_MJ_ZPTDH.CHR_TIAN_HU).is_empty()
				|| !chr.opr_and(Constants_MJ_ZPTDH.CHR_DI_HU).is_empty())
			score = 13;
		if(!chr.opr_and(Constants_MJ_ZPTDH.CHR_QHH_QI_XIAO_DUI).is_empty()
				|| !chr.opr_and(Constants_MJ_ZPTDH.CHR_DA_SAN_YUAN).is_empty()
				|| !chr.opr_and(Constants_MJ_ZPTDH.CHR_DA_SI_XI).is_empty())
			score = 10;
		if(!chr.opr_and(Constants_MJ_ZPTDH.CHR_QHH_QI_XIAO_DUI).is_empty()
				|| !chr.opr_and(Constants_MJ_ZPTDH.CHR_ZI_YI_SE).is_empty())
			score = 9;
		if(!chr.opr_and(Constants_MJ_ZPTDH.CHR_QING_DUI).is_empty())
			score = 8;
		if(!chr.opr_and(Constants_MJ_ZPTDH.CHR_HYS_HH_QI_XIAO_DUI).is_empty()
				|| !chr.opr_and(Constants_MJ_ZPTDH.CHR_QING_PENG).is_empty()
				|| !chr.opr_and(Constants_MJ_ZPTDH.CHR_HUN_YAO_JIU).is_empty())
			score = 7;
		if(!chr.opr_and(Constants_MJ_ZPTDH.CHR_HYS_QI_XIAO_DUI).is_empty()
				|| !chr.opr_and(Constants_MJ_ZPTDH.CHR_HH_QI_XIAO_DUI).is_empty()
				|| !chr.opr_and(Constants_MJ_ZPTDH.CHR_QING_YI_SE).is_empty())
			score = 5;
		if(!chr.opr_and(Constants_MJ_ZPTDH.CHR_HUN_PENG).is_empty())
			score = 4;
		if(!chr.opr_and(Constants_MJ_ZPTDH.CHR_QI_XIAO_DUI).is_empty())
			score = 3;
		if(!chr.opr_and(Constants_MJ_ZPTDH.CHR_PENG_PENG_HU).is_empty()
				|| !chr.opr_and(Constants_MJ_ZPTDH.CHR_HUN_YI_SE).is_empty())
			score = 2;
		
		if(zimo){
			if(hasRule(Constants_MJ_ZPTDH.GAME_RULE_ZI_MO_x2))
				score *= 2;
			if(hasRule(Constants_MJ_ZPTDH.GAME_RULE_ZI_MO_ADD_1))
				score += 1;
			if(hasRule(Constants_MJ_ZPTDH.GAME_RULE_ZI_MO_ADD_2))
				score += 2;
		}
		
		if(!chr.opr_and(Constants_MJ_ZPTDH.CHR_FANG_GANG_KAI).is_empty())
			score *= 2;
		if(!chr.opr_and(Constants_MJ_ZPTDH.CHR_QIANG_GANG).is_empty() &&
				hasRule(Constants_MJ_ZPTDH.GAME_RULE_QIANG_GANG_HU_x2))
			score *= 2;
		
		return score;
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
					if (type == Constants_MJ_ZPTDH.CHR_DIAN_PAO) {
						result.append(" 接炮");
					}
					if (type == Constants_MJ_ZPTDH.CHR_ZI_MO) {
						result.append(" 自摸");
					}
					if (type == Constants_MJ_ZPTDH.CHR_PING_HU) {
						result.append(" 鸡胡");
					}
					if (type == Constants_MJ_ZPTDH.CHR_QIANG_GANG) {
						result.append(" 抢杠胡");
					}
					if (type == Constants_MJ_ZPTDH.CHR_FANG_GANG_KAI) {
						result.append(" 放杠杠开");
					}
					if (type == Constants_MJ_ZPTDH.CHR_PENG_PENG_HU) {
						result.append(" 碰碰胡");
					}
					if (type == Constants_MJ_ZPTDH.CHR_HUN_YI_SE) {
						result.append(" 混一色");
					}
					if (type == Constants_MJ_ZPTDH.CHR_QI_XIAO_DUI) {
						result.append(" 七小对");
					}
					if (type == Constants_MJ_ZPTDH.CHR_HUN_PENG) {
						result.append(" 混碰");
					}
					if (type == Constants_MJ_ZPTDH.CHR_HYS_QI_XIAO_DUI) {
						result.append(" 混一色七小对");
					}
					if (type == Constants_MJ_ZPTDH.CHR_HH_QI_XIAO_DUI) {
						result.append(" 豪华七小对");
					}
					if (type == Constants_MJ_ZPTDH.CHR_QING_YI_SE) {
						result.append(" 清一色");
					}
					if (type == Constants_MJ_ZPTDH.CHR_HYS_HH_QI_XIAO_DUI) {
						result.append(" 混一色豪华七小对");
					}
					if (type == Constants_MJ_ZPTDH.CHR_QING_PENG) {
						result.append(" 清碰");
					}
					if (type == Constants_MJ_ZPTDH.CHR_HUN_YAO_JIU) {
						result.append(" 混幺九");
					}
					if (type == Constants_MJ_ZPTDH.CHR_QING_DUI) {
						result.append(" 清对");
					}
					if (type == Constants_MJ_ZPTDH.CHR_QING_YOU_JIU) {
						result.append(" 清幺九");
					}
					if (type == Constants_MJ_ZPTDH.CHR_ZI_YI_SE) {
						result.append(" 字一色");
					}
					if (type == Constants_MJ_ZPTDH.CHR_QHH_QI_XIAO_DUI) {
						result.append(" 清豪华七小对");
					}
					if (type == Constants_MJ_ZPTDH.CHR_DA_SAN_YUAN) {
						result.append(" 大三元");
					}
					if (type == Constants_MJ_ZPTDH.CHR_DA_SI_XI) {
						result.append(" 大四喜");
					}
					if (type == Constants_MJ_ZPTDH.CHR_SHI_SAN_YAO) {
						result.append(" 十三幺");
					}
					if (type == Constants_MJ_ZPTDH.CHR_TIAN_HU) {
						result.append(" 天胡");
					}
					if (type == Constants_MJ_ZPTDH.CHR_DI_HU) {
						result.append(" 地胡");
					}
				} else if (type == Constants_MJ_ZPTDH.CHR_FANG_PAO) {
					result.append(" 点炮");
				} else if (type == Constants_MJ_ZPTDH.CHR_BEI_QIANG_GANG) {
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
				result.append(" 公杠X" + ming_gang);
			}
			if (fang_gang > 0) {
				result.append(" 放杠X" + fang_gang);
			}
			if (jie_gang > 0) {
				result.append(" 直杠X" + jie_gang);
			}
			if (GRR._player_niao_count[player] > 0) {
				result.append(" 中马X" + GRR._player_niao_count[player]);
			}
			if(is_gen_zhuang && player == GRR._banker_player)
				result.append(" 被跟庄");
			if(hu_dec_type[player] == 10)
				result.append(" 包牌");
			if(hu_dec_type[player] == 11)
				result.append(" 包自摸");
			GRR._result_des[player] = result.toString();
		}
	}
	
	
	/**
	 * 设置鸟
	 */
	public void set_niao_card(int seat_index) {
		GRR._show_bird_effect = true;
		int bird_num = getHorseCount();
		GRR._count_niao = bird_num > GRR._left_card_count ? GRR._left_card_count : bird_num;

		if (GRR._count_niao > GameConstants.ZHANIAO_0) {
			int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
			_logic.switch_to_cards_index(_repertory_card, _all_card_len - GRR._left_card_count, GRR._count_niao, cbCardIndexTemp);
			GRR._left_card_count -= GRR._count_niao;
			_logic.switch_to_cards_data(cbCardIndexTemp, GRR._cards_data_niao);
			for (int i = 0; i < GRR._count_niao; i++) {
				int nValue = _logic.get_card_value(GRR._cards_data_niao[i]);
				boolean flag = false;
				if (hasRule(Constants_MJ_ZPTDH.GAME_RULE_ZHUANG_JIA_MAI_MA)){
					if(nValue % 4 == 1)
						flag = true;
				}else{
					if((seat_index - _cur_banker + 1 + getTablePlayerNumber()) % getTablePlayerNumber() == nValue % getTablePlayerNumber())
						flag = true;
				}
					
				if (flag) {
					GRR._player_niao_cards[seat_index][i] = GRR._cards_data_niao[i] + GameConstants.DING_NIAO_VALID;
					GRR._player_niao_count[seat_index]++;
				}else{
					GRR._player_niao_cards[seat_index][i] = GRR._cards_data_niao[i] + GameConstants.DING_NIAO_INVALID;
				}

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
			if (GRR._left_card_count > getHorseCount()) {
				if (i == get_banker_next_seat(seat_index) && hasRule(Constants_MJ_ZPTDH.GAME_RULE_KE_CHI_PAI)) {
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
				action = _logic.check_peng(GRR._cards_index[i], card);
				if (action != 0) {
					playerStatus.add_action(action);
					playerStatus.add_peng(card, seat_index);
					bAroseAction = true;
				}

			}
			
			action = _logic.estimate_gang_card_out_card(GRR._cards_index[i], card);
			if (action != 0) {
				playerStatus.add_action(GameConstants.WIK_GANG);
				playerStatus.add_gang(card, i, 1);
				bAroseAction = true;
			}
			
			if (_playerStatus[i].is_chi_hu_round() && !hasRule(Constants_MJ_ZPTDH.GAME_RULE_ZI_MO_HU)) {
				ChiHuRight chr = GRR._chi_hu_rights[i];
				chr.set_empty();
				int cbWeaveCount = GRR._weave_count[i];
				int card_type = Constants_MJ_ZPTDH.HU_CARD_TYPE_DIAN_PAO;
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
		int cbCurrentCard = 0;

		for (int i = 0; i < GameConstants.MAX_ZI; i++) {
			cbCurrentCard = _logic.switch_to_card_data(i);
			chr.set_empty();

			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card(cbCardIndexTemp, weaveItem, cbWeaveCount, cbCurrentCard, chr,
					GameConstants.CHR_ZI_MO, seat_index)) {
				cards[count++] = cbCurrentCard;
			}
		}
		
		if(hasRule(Constants_MJ_ZPTDH.GAME_RULE_WU_ZI_PAI)){
			if(hasRule(Constants_MJ_ZPTDH.GAME_RULE_HONG_LAI_ZI)
					|| hasRule(Constants_MJ_ZPTDH.GAME_RULE_BAI_LAI_ZI)){
				if(hasRule(Constants_MJ_ZPTDH.GAME_RULE_HONG_LAI_ZI))
					cbCurrentCard = 0x35;
				if(hasRule(Constants_MJ_ZPTDH.GAME_RULE_BAI_LAI_ZI))
					cbCurrentCard = 0x37;
				chr.set_empty();
				if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card(cbCardIndexTemp, weaveItem, cbWeaveCount, cbCurrentCard, chr, GameConstants.CHR_ZI_MO,
						seat_index)) {
					cards[count++] = cbCurrentCard;
				}
			}
		}else{
			for (int i = GameConstants.MAX_ZI; i < GameConstants.MAX_ZI_FENG; i++) {
				cbCurrentCard = _logic.switch_to_card_data(i);
				chr.set_empty();

				if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card(cbCardIndexTemp, weaveItem, cbWeaveCount, cbCurrentCard, chr,
						GameConstants.CHR_ZI_MO, seat_index)) {
					cards[count++] = cbCurrentCard;
				}
			}
		}
		
		//全听
		if (count == 27 || count == 28 || count == 34) {
			count = 1;
			cards[0] = -1;
		}

		return count;
	}

	@Override
	public boolean trustee_timer(int operate_id, int seat_index) {
		return false;
	}
	
	
	
	public int get_pai_xing_se(int cards_index[], WeaveItem weaveItem[], int weaveCount, boolean is_magic_dian_pao) {
		int cbQueYiMenColor[] = new int[] { 1, 1, 1 };// 缺一色
		boolean has_feng = false;
		// 计算单牌
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			int cbCardCount = cards_index[i];// 数量
			if (cbCardCount == 0 || (_logic.is_magic_index(i) && !is_magic_dian_pao)) {
				continue;
			}
			int card = _logic.switch_to_card_data(i);
			
			// 缺一色：起完牌后，玩家手上筒、索、万任缺一门，即可胡牌。（等同小胡自摸）
			int cbCardColor = _logic.get_card_color(card);
			if (cbCardColor > 2){
				has_feng = true;
				continue;
			}
			cbQueYiMenColor[cbCardColor] = 0;
		}
		for (int i = 0; i < weaveCount; i++) {
			int cbCardColor = _logic.get_card_color(weaveItem[i].center_card);
			// 字牌过滤
			if (cbCardColor > 2){
				has_feng = true;
				continue;
			}
			cbQueYiMenColor[cbCardColor] = 0;
		}
		int count = 0;
		for (int i = 0; i < 3; i++) {
			if (cbQueYiMenColor[i] == 0) {
				count += 1;
			}
		}
		if(has_feng && count == 0)
			return 0;
		if(!has_feng && count == 1)
			return 1;
		if(has_feng && count == 1)
			return 2;
		return -1;
	}
	
	
    // 杠牌分析 包括补杠 check_weave检查补杠
    public int analyse_gang(int cards_index[], WeaveItem WeaveItem[], int cbWeaveCount,
            GangCardResult gangCardResult, int send_card_data) {
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

        // 组合杠牌，包括以前能杠，但是不杠，发牌之后而选择补杠的
        for (int i = 0; i < cbWeaveCount; i++) {
            if (WeaveItem[i].weave_kind == GameConstants.WIK_PENG) {
                for (int j = 0; j < GameConstants.MAX_INDEX; j++) {
                    if (WeaveItem[i].center_card == send_card_data) {
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
            for (int i = 0; i < getTablePlayerNumber(); i++) {
            	//荒庄荒杠
                for (int j = 0; j < GRR._gang_score[i].gang_count && reason == GameConstants.Game_End_NORMAL; j++) {
            		for (int k = 0; k < getTablePlayerNumber(); k++) {
            			lGangScore[k] += GRR._gang_score[i].scores[j][k];// 杠牌，每个人的分数
            		}
            	}
            }

            for (int i = 0; i < getTablePlayerNumber(); i++) {
            	//杠分
                GRR._game_score[i] += lGangScore[i];
                //跟庄分
                if(is_gen_zhuang){
                	if(GRR._banker_player == i)
                		GRR._game_score[i] -= getTablePlayerNumber() - 1;
                	else
                		GRR._game_score[i] += 1;
                }
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
                for (int j = 0; j < getHorseCount(); j++) {
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
                game_end.addGameScore(GRR._game_score[i]);
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
        handler_game_end(game_end);
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
    
    private void handler_game_end(GameEndResponse.Builder game_end) {
		GDTDHGameEndResponse.Builder gameEndBuilder = GDTDHGameEndResponse.newBuilder();

		Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();
		for (int p = 0; p < getTablePlayerNumber(); p++) {
			cards.clear();
			gameEndBuilder.addHuDes(String.valueOf(hu_dec_type[p]));
		}
		game_end.setCommResponse(PBUtil.toByteString(gameEndBuilder));
	}
    
	
	// 七小对牌 七小对：胡牌时，手上任意七对牌。
	public int is_qi_xiao_dui(int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int cur_card, boolean is_magic_dian_pao) {

		// 组合判断
		if (cbWeaveCount != 0)
			return GameConstants.WIK_NULL;

		// 单牌数目
		int cbReplaceCount = 0;
		int cbReplaceCount3 = 0;
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
					if (cbCardCount == 1)
						cbReplaceCount++;
					if (cbCardCount == 3)
						cbReplaceCount3++;

					if (cbCardCount == 4) {
						nGenCount++;
					}
				}
			} else {
				// 单牌统计
				if (cbCardCount == 1)
					cbReplaceCount++;
				if (cbCardCount == 3)
					cbReplaceCount3++;

				if (cbCardCount == 4) {
					nGenCount++;
				}
			}
		}

		// 王牌不够
		int count = 0;
		if (_logic.get_magic_card_count() > 0) {
			for (int m = 0; m < _logic.get_magic_card_count(); m++) {
				count += cbCardIndexTemp[_logic.get_magic_card_index(m)];
			}
			
			if(is_magic_dian_pao){
				count--;
				cbReplaceCount++;
			}
			
			if (cbReplaceCount + cbReplaceCount3 > count) {
				return GameConstants.WIK_NULL;
			}
		} else {
			if (cbReplaceCount + cbReplaceCount3 > 0)
				return GameConstants.WIK_NULL;
		}

		if (nGenCount > 0 || (count - cbReplaceCount -cbReplaceCount3) > 1 || (count > 0 && cbReplaceCount3 > 0)) {
			return 2;
		} else {
			return 1;
		}

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
	
	public boolean is_yi_tiao_long(int cards_index[], WeaveItem weaveItem[], int weaveCount) {
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
		
		// 落地一条龙判断
		/*if(weaveCount == 0)
			return false;
		for (int i = 0; i < weaveCount; i++) {
			if(weaveItem[i].center_card < 0x31){
				int value = _logic.get_card_value(weaveItem[i].center_card);
				int color = _logic.get_card_color(weaveItem[i].center_card);
				if((value == 1 && weaveItem[i].weave_kind == GameConstants.WIK_LEFT) ||
						(value == 2 && weaveItem[i].weave_kind == GameConstants.WIK_CENTER) ||
						(value == 3 && weaveItem[i].weave_kind == GameConstants.WIK_RIGHT)){
					int[] copyOf = Arrays.copyOf(cards_index, cards_index.length);
					boolean flag = true;
					for(int c = color * 9 + 3; c < color * 9 + 9; c++){
						if(copyOf[c] == 0){
							flag = false;
							break;
						}
						copyOf[c]--;
					}
					boolean hu = flag && AnalyseCardUtil.analyse_feng_chi_by_cards_index(copyOf, -1, null, 0);
					if(hu)
						return hu;
				}
				if((value == 4 && weaveItem[i].weave_kind == GameConstants.WIK_LEFT) ||
						(value == 5 && weaveItem[i].weave_kind == GameConstants.WIK_CENTER) ||
						(value == 6 && weaveItem[i].weave_kind == GameConstants.WIK_RIGHT)){
					int[] copyOf = Arrays.copyOf(cards_index, cards_index.length);
					boolean flag = true;
					for(int c = color * 9 + 0; c < color * 9 + 3; c++){
						if(copyOf[c] == 0){
							flag = false;
							break;
						}
						copyOf[c]--;
					}
					for(int c = color * 9 + 6; c < color * 9 + 9; c++){
						if(copyOf[c] == 0){
							flag = false;
							break;
						}
						copyOf[c]--;
					}
					boolean hu = flag && AnalyseCardUtil.analyse_feng_chi_by_cards_index(copyOf, -1, null, 0);
					if(hu)
						return hu;
				}
				if((value == 7 && weaveItem[i].weave_kind == GameConstants.WIK_LEFT) ||
						(value == 8 && weaveItem[i].weave_kind == GameConstants.WIK_CENTER) ||
						(value == 9 && weaveItem[i].weave_kind == GameConstants.WIK_RIGHT)){
					int[] copyOf = Arrays.copyOf(cards_index, cards_index.length);
					boolean flag = true;
					for(int c = color * 9 + 0; c < color * 9 + 6; c++){
						if(copyOf[c] == 0){
							flag = false;
							break;
						}
						copyOf[c]--;
					}
					boolean hu = flag && AnalyseCardUtil.analyse_feng_chi_by_cards_index(copyOf, -1, null, 0);
					if(hu)
						return hu;
				}
			}
		}*/
		
		return false;
	}
	
    @Override
	public void test_cards() {
		int[] cards_of_player0 = new int[] { 0x11,0x11,0x11,0x15,0x15,0x15,0x22,0x22,0x22,0x03,0x03,0x15,0x15 };
		int[] cards_of_player1 = new int[] { 0x01,0x02,0x03,0x04,0x05,0x06,0x07,0x08,0x09,0x09,0x03,0x15,0x01 };
		int[] cards_of_player2 = new int[] { 0x21,0x21,0x21,0x11,0x11,0x11,0x19,0x19,0x19,0x29,0x29,0x29,0x35 };
		int[] cards_of_player3 = new int[] { 0x21,0x21,0x21,0x11,0x11,0x11,0x19,0x19,0x19,0x29,0x29,0x29,0x35 };

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
