package com.cai.game.mj.chenchuang.hlkdd;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
import com.cai.common.constant.game.mj.Constants_MJ_HLKDD;
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
import com.cai.game.util.GameUtilConstants;
import com.cai.service.PlayerServiceImpl;

/**
 * chenchuang
 * 欢乐扣点点
 */
public class Table_HLKDD extends AbstractMJTable {

	private static final long serialVersionUID = 1L;
	
	public boolean[] is_bao_ting = new boolean[getTablePlayerNumber()];
	
	public int[][] jie_gang_provide = new int[getTablePlayerNumber()][4];
	
	public float[][] jie_gang_value = new float[getTablePlayerNumber()][4];
	
	public boolean[][] is_jie_gang_ting = new boolean[getTablePlayerNumber()][4];
	
	public int[] jie_gang_count = new int[getTablePlayerNumber()];
	
	public boolean[] is_zi_mo_hu = new boolean[getTablePlayerNumber()];
	public boolean[] is_si_mouse = new boolean[getTablePlayerNumber()];
	public boolean[] magic_own_hu = new boolean[getTablePlayerNumber()];
	public ArrayList<Integer> baoTingOutCards = new ArrayList<Integer>();
	
	public int hu_card_value;
	public int paiXingScore;
	
	public int[] baoTingDiscard = new int[getTablePlayerNumber()];
	
	public Table_HLKDD() {
		super(MJType.GAME_TYPE_MJ_HLKDD);
	}

	@Override
	protected void onInitTable() {
		_handler_chi_peng = new HandlerChiPeng_HLKDD();
		_handler_dispath_card = new HandlerDispatchCard_HLKDD();
		_handler_gang = new HandlerGang_HLKDD();
		_handler_out_card_operate = new HandlerOutCardOperate_HLKDD();
	}

	public void exe_select_magic(){
		if(has_rule(Constants_MJ_HLKDD.GAME_RULE_NO_MOUSE))
			return;
		int _da_dian_card = 0;
		if(has_rule(Constants_MJ_HLKDD.GAME_RULE_CATCH_MOUSE)){
			_da_dian_card = _repertory_card[_all_card_len - GRR._left_card_count];
		}else{
			for (int i = GRR._left_card_count; i > 0; i--) {
				_da_dian_card = _repertory_card[_all_card_len - i];
				if(_da_dian_card >= 0x31){
					_repertory_card[_all_card_len - i] = _repertory_card[_all_card_len - GRR._left_card_count];
					break;
				}
			}
		}
		
		_send_card_count++;
        GRR._left_card_count--;

        if (DEBUG_CARDS_MODE){
			_da_dian_card = 0x28;
		}
		if(DEBUG_MAGIC_CARD)
			_da_dian_card = magic_card_decidor;
		
        // 将翻出来的牌显示在牌桌的正中央
        operate_show_card(_cur_banker, GameConstants.Show_Card_Center, 1, new int[] { _da_dian_card },
                GameConstants.INVALID_SEAT);
        
        int card_next = 0;

        int cur_value = _logic.get_card_value(_da_dian_card);
        int cur_color = _logic.get_card_color(_da_dian_card);

        if (cur_color == 3) {
    		if(cur_value == 4)
    			card_next = _da_dian_card - 3;
    		if(cur_value == 7)
    			card_next = _da_dian_card - 2;
    		else
    			card_next = _da_dian_card + 1;
        } else {
            if (cur_value == 9) {
                card_next = _da_dian_card - 8;
            } else {
                card_next = _da_dian_card + 1;
            }
        }

        int magic_card_index = _logic.switch_to_card_index(card_next);

        // 添加鬼
        _logic.add_magic_card_index(magic_card_index);
        GRR._especial_card_count = 1;
        GRR._especial_show_cards[0] = card_next + GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;

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

        GameSchedule.put(new Runnable() {
            @Override
            public void run() {
                // 将翻出来的牌从牌桌的正中央移除
                operate_show_card(_cur_banker, GameConstants.Show_Card_Center, 0, null, GameConstants.INVALID_SEAT);
            }
        }, 3000, TimeUnit.MILLISECONDS);
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
		if(getRuleValue(GameConstants.GAME_RULE_ALL_GAME_TWO_PLAYER) == 1) 
			return 2;
		if(getRuleValue(GameConstants.GAME_RULE_ALL_GAME_THREE_PLAYER) == 1) 
			return 3;
		return 4;
	}
	
	/**
	 * 初始化洗牌数据:默认(如不能满足请重写此方法)
	 * 
	 * @return
	 */
	@Override
	protected void init_shuffle() {
		int[] card = Constants_MJ_HLKDD.CARD_DATA_DAI_FENG;
		if(has_rule(Constants_MJ_HLKDD.GAME_RULE_BU_DAI_FENG))
			card = Constants_MJ_HLKDD.CARD_DATA_NOT_DAI_FENG;
		_repertory_card = new int[card.length];
		shuffle(_repertory_card, card);
	}
	
	/**
	 * 洗完牌执行开始
	 */
	@Override
	protected boolean on_game_start() {
		//初始化常量
		is_bao_ting = new boolean[getTablePlayerNumber()];
		jie_gang_provide = new int[getTablePlayerNumber()][4];
		jie_gang_value = new float[getTablePlayerNumber()][4];
		is_jie_gang_ting = new boolean[getTablePlayerNumber()][4];
		jie_gang_count = new int[getTablePlayerNumber()];
		is_zi_mo_hu = new boolean[getTablePlayerNumber()];
		is_si_mouse = new boolean[getTablePlayerNumber()];
		hu_card_value = 0;
		paiXingScore = 0;
		magic_own_hu = new boolean[getTablePlayerNumber()];
		baoTingOutCards.clear();
		Arrays.fill(baoTingDiscard, -1);
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
	
	public float getBottomScore(){
		if(has_rule(Constants_MJ_HLKDD.GAME_RULE_BOTTOM_SCORE_02))
			return 0.2f;
		if(has_rule(Constants_MJ_HLKDD.GAME_RULE_BOTTOM_SCORE_05))
			return 0.5f;
		return 1;
	}
	public int getFourMouseScore(){
		if(has_rule(Constants_MJ_HLKDD.GAME_RULE_FOUR_MOUSE_HAS_PRIZE_5))
			return 5;
		if(has_rule(Constants_MJ_HLKDD.GAME_RULE_FOUR_MOUSE_HAS_PRIZE_10))
			return 10;
		return 20;
	}
	
	public int getCardValueScore(int cur_card){
		return cur_card > 0x29 ? 10 : _logic.get_card_value(cur_card);
	}
	
	@Override
	public int analyse_chi_hu_card(int[] cards_index, WeaveItem[] weaveItems, int weave_count, int cur_card, ChiHuRight chiHuRight, int card_type,
			int _seat_index) {
		//胡的是癞子需要特殊处理判断
		int ting_count = _playerStatus[_seat_index]._hu_card_count;
		if(ting_count == 0)
			return GameConstants.WIK_NULL;
		boolean is_magic_card = _logic.is_magic_card(cur_card);
		if(is_magic_card){
			int ting_cards[] = _playerStatus[_seat_index]._hu_cards;
			ArrayList<Integer> arr = new ArrayList<Integer>();
			for (int i = 0; i < ting_count; i++) {
				arr.add(getCardValueScore(ting_cards[i]));
			}
			if(arr.size() > 1 && !magic_own_hu[_seat_index])//如果只有胡癞子肯定是还原自己
				arr.remove(Integer.valueOf(getCardValueScore(cur_card)));
			Collections.sort(arr);
			hu_card_value = arr.get(arr.size() - 1);
		}else
			hu_card_value = getCardValueScore(cur_card);
		if(hu_card_value < 3)
			return GameConstants.WIK_NULL;
		if(card_type == Constants_MJ_HLKDD.HU_CARD_TYPE_DIAN_PAO || card_type == Constants_MJ_HLKDD.HU_CARD_TYPE_QIANG_GANG_HU){
			if(hu_card_value < 6)
				return GameConstants.WIK_NULL;
		}
		
		if(is_zi_mo_hu[_seat_index]){
			if(card_type == Constants_MJ_HLKDD.HU_CARD_TYPE_DIAN_PAO || card_type == Constants_MJ_HLKDD.HU_CARD_TYPE_QIANG_GANG_HU)
				return GameConstants.WIK_NULL;
		}
		
		
		
		if (card_type == Constants_MJ_HLKDD.HU_CARD_TYPE_ZI_MO) {
			chiHuRight.opr_or(Constants_MJ_HLKDD.CHR_ZI_MO);//自摸
		} else if (card_type == Constants_MJ_HLKDD.HU_CARD_TYPE_QIANG_GANG_HU) {
			chiHuRight.opr_or(Constants_MJ_HLKDD.CHR_QIANG_GANG);//抢杠胡
		}else if (card_type == Constants_MJ_HLKDD.HU_CARD_TYPE_DIAN_PAO) {
			chiHuRight.opr_or(Constants_MJ_HLKDD.CHR_DIAN_PAO);//点炮胡
		}
		boolean is_you_da_hu = true;
		//拷贝把当前牌加入手牌
		int cur_card_index = _logic.switch_to_card_index(cur_card);
		int[] temp_cards_index = Arrays.copyOf(cards_index, cards_index.length);
		temp_cards_index[cur_card_index]++;
		
		
		//七对4倍
  		boolean xiao_qi_dui = false;
  		int qi_dui_value = 0;
		int hu = is_qi_xiao_dui(cards_index, weave_count, cur_card);
		if (is_you_da_hu && hu != GameConstants.WIK_NULL) {
			xiao_qi_dui =true;
			qi_dui_value = hu;
  		}
		
		boolean shiSanYao = is_you_da_hu && isShiSanYao(temp_cards_index, is_magic_card);
  		
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
  		boolean analyse_win_by_cards_index = false;
  		if(!has_rule(Constants_MJ_HLKDD.GAME_RULE_FENG_ZUI_ZI))
  			analyse_win_by_cards_index = AnalyseCardUtil.analyse_win_by_cards_index(cards_index, cur_card_index, magic_cards_index, magic_card_count);
  		else
  			analyse_win_by_cards_index = AnalyseCardUtil.analyse_feng_chi_by_cards_index(cards_index, cur_card_index, magic_cards_index, magic_card_count);
  			
//  		if(_logic.is_magic_card(cur_card) &&( card_type == Constants_MJ_HLKDD.HU_CARD_TYPE_QIANG_GANG_HU || card_type == Constants_MJ_HLKDD.HU_CARD_TYPE_DIAN_PAO))
//  			analyse_win_by_cards_index = AnalyseCardUtil.analyse_win_by_cards_index_taojiang(cards_index, cur_card_index, magic_cards_index, magic_card_count);
  		
  		if(!(analyse_win_by_cards_index || xiao_qi_dui || shiSanYao)){
  			return GameConstants.WIK_NULL;
  		}
  	  	boolean has_magic = magic_card_count > 0 && temp_cards_index[_logic.get_magic_card_index(0)] > 0;
  	  	boolean is_mouse_diao_card = false;
  	  	if(has_magic){
  	  		is_you_da_hu = false;
  	  		qi_dui_value = 0;
  	  		shiSanYao = false;
  	  		is_mouse_diao_card = is_mouse_diao_card(cards_index, weave_count);
  	  		if(is_mouse_diao_card && has_rule(Constants_MJ_HLKDD.GAME_RULE_JIAN_ZI_HU_ONLY_ZI_MO) && (card_type == Constants_MJ_HLKDD.HU_CARD_TYPE_DIAN_PAO || card_type == Constants_MJ_HLKDD.HU_CARD_TYPE_QIANG_GANG_HU))
  	  			return GameConstants.WIK_NULL;
  	  	}
		//清一色2倍
		boolean is_qing_yi_se = has_rule(Constants_MJ_HLKDD.GAME_RULE_QING_YI_SE) && is_you_da_hu && _logic.is_qing_yi_se(cards_index, weaveItems, weave_count, cur_card);
		
		boolean is_yi_tiao_long = has_rule(Constants_MJ_HLKDD.GAME_RULE_YI_TIAO_LONG) && is_you_da_hu && is_yi_tiao_long(temp_cards_index, weaveItems, weave_count);
		if(is_mouse_diao_card){
			chiHuRight.opr_or(Constants_MJ_HLKDD.CHR_MOUSE_DIAO_CARD);
		}else if(shiSanYao)
			chiHuRight.opr_or(Constants_MJ_HLKDD.CHR_SHI_SAN_YAO);
		else if(qi_dui_value > 0x20)
			chiHuRight.opr_or(Constants_MJ_HLKDD.CHR_HAO_HUA_QI_XIAO_DUI);
		else if(is_yi_tiao_long)
			chiHuRight.opr_or(Constants_MJ_HLKDD.CHR_YI_TIAO_LONG);
		else if(qi_dui_value > 0)
			chiHuRight.opr_or(Constants_MJ_HLKDD.CHR_QI_XIAO_DUI);
		else if(is_qing_yi_se)
			chiHuRight.opr_or(Constants_MJ_HLKDD.CHR_QING_YI_SE);
		else
			chiHuRight.opr_or(Constants_MJ_HLKDD.CHR_PING_HU);
		
		return GameConstants.WIK_CHI_HU;
	}
	
	public int analyse_chi_hu_card_ting(int[] cards_index, WeaveItem[] weaveItems, int weave_count, int cur_card, ChiHuRight chiHuRight, int card_type,
			int _seat_index) {
		//胡的是癞子需要特殊处理判断
		boolean is_magic_card = _logic.is_magic_card(cur_card);
		int card_value = getCardValueScore(cur_card);
		if(!is_magic_card){
			if(card_value < 3)
				return GameConstants.WIK_NULL;
		}
		
		//七对4倍
  		boolean xiao_qi_dui = false;
		int hu = is_qi_xiao_dui(cards_index, weave_count, cur_card);
		if (hu != GameConstants.WIK_NULL) {
			xiao_qi_dui =true;
  		}
		
		if(xiao_qi_dui){
			if(card_value > 2)
				return GameConstants.WIK_CHI_HU;
		}
		
		//拷贝把当前牌加入手牌
		int cur_card_index = _logic.switch_to_card_index(cur_card);
		int[] temp_cards_index = Arrays.copyOf(cards_index, cards_index.length);
		temp_cards_index[cur_card_index]++;
				
		boolean shiSanYao = isShiSanYao(temp_cards_index, is_magic_card);
		if(shiSanYao)
  			return GameConstants.WIK_CHI_HU;
		
		
		//得到癞子牌的个数，和索引
		int[] magic_cards_index = new int[GameUtilConstants.MAX_MAGIC_INDEX_COUNT];
        int magic_card_count = _logic.get_magic_card_count();
        if (magic_card_count > 2) { // 一般只有两种癞子牌存在
            magic_card_count = 2;
        }
        for (int i = 0; i < magic_card_count; i++) {
            magic_cards_index[i] = _logic.get_magic_card_index(i);
        }
		
		
		boolean feng_chi = has_rule(Constants_MJ_HLKDD.GAME_RULE_FENG_ZUI_ZI);
		boolean can_hu = AnalyseCardUtil.analyse_win_by_cards_index_hlkdd(cards_index, cur_card_index, magic_cards_index, magic_card_count, feng_chi, false);
		if(!can_hu)
			return GameConstants.WIK_NULL;
		if(is_magic_card){
			boolean is_hu = false;
			for (int i = 0; i < GameConstants.MAX_ZI_FENG; i++) {
				int card = _logic.switch_to_card_data(i);
				boolean cardhu = false;
				if(card == cur_card){
					cardhu = AnalyseCardUtil.analyse_win_by_cards_index_hlkdd(cards_index, i, magic_cards_index, magic_card_count, feng_chi, true);
					if(cardhu)
						magic_own_hu[_seat_index] = true;
				}else{
					cardhu = AnalyseCardUtil.analyse_win_by_cards_index_hlkdd(cards_index, i, magic_cards_index, magic_card_count, feng_chi, false);
				}
				if(cardhu){
					int value = getCardValueScore(card);
					if(value > 2)
						is_hu = true;
				}
			}
			return is_hu ? GameConstants.WIK_CHI_HU : GameConstants.WIK_NULL;
		}
		return can_hu ? GameConstants.WIK_CHI_HU : GameConstants.WIK_NULL;
	}
	
	public void exe_ting_card(int _out_card_player) {
		_playerStatus[_out_card_player]._hu_card_count = get_ting_card(_playerStatus[_out_card_player]._hu_cards,
				GRR._cards_index[_out_card_player], GRR._weave_items[_out_card_player], GRR._weave_count[_out_card_player],
				_out_card_player);
		int ting_cards[] = _playerStatus[_out_card_player]._hu_cards;
		int ting_count = _playerStatus[_out_card_player]._hu_card_count;

		if (ting_count > 0) {//显示听牌
			operate_chi_hu_cards(_out_card_player, ting_count, ting_cards);
		} else {
			ting_cards[0] = 0;
			operate_chi_hu_cards(_out_card_player, 1, ting_cards);
		}
	}
	
	public void check_bao_ting(int _seat_index, int ting_count, int i) {
		if(!is_bao_ting[_seat_index]){
			int[] js = _playerStatus[_seat_index]._hu_out_cards[ting_count];
			ArrayList<Integer> arr = new ArrayList<Integer>();
			for (int c = 0; c < _playerStatus[_seat_index]._hu_out_card_ting_count[ting_count]; c++) {
				arr.add(getCardValueScore(js[c]));
			}
			if(arr.size() > 1 && !magic_own_hu[_seat_index]){
				int card_data = _logic.switch_to_card_data(_logic.get_magic_card_index(0));
				arr.remove(Integer.valueOf(getCardValueScore(card_data)));
			}
			Collections.sort(arr);
			if(arr.get(arr.size() - 1) > 5)
				baoTingOutCards.add(_logic.switch_to_card_data(i));
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
				roomResponse.addAllDouliuzi(baoTingOutCards);
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
	
	
	
	private boolean is_mouse_diao_card(int[] cards_index, int weave_count) {
		int magic_card_index = _logic.get_magic_card_index(0);
		if(cards_index[magic_card_index] < 1)
			return false;
		if(weave_count == 4){
			return true;
		}
		int[] temp_cards_index = Arrays.copyOf(cards_index, cards_index.length);
		temp_cards_index[magic_card_index]--;
		
		boolean flag = true;
		for (int j = 0; j < 10; j++) {
			if (temp_cards_index[j] >= 3)
				continue;
			temp_cards_index[j]++;
			boolean hu = AnalyseCardUtil.analyse_feng_chi_by_cards_index(temp_cards_index, j, new int[]{_logic.get_magic_card_index(0)}, 1);
			temp_cards_index[j]--;
			if (!hu){
				flag = false;
				break;
			}
		}
		boolean flag1 = weave_count == 0 ? true : false;
		for (int j = 0; j < 10 && weave_count == 0 && !flag; j++) {
			if (temp_cards_index[j] >= 3)
				continue;
			temp_cards_index[j]++;
			int hu = is_qi_xiao_dui(temp_cards_index, weave_count, _logic.switch_to_card_data(j));
			temp_cards_index[j]--;
			if (hu == 0){
				flag1 = false;
				break;
			}
		}
		return flag || flag1;
	}
	
	private boolean isShiSanYao(int cards_index[], boolean dispatch_magic_card) {
		//不能有非19的牌
		for (int i = 0; i < GameConstants.MAX_ZI; i++) {
			if(!(i % 9 == 0 || (i + 1) % 9 == 0) && cards_index[i] > 0)
				return false;
		}
		List<Integer> que = ssyQueCard(cards_index);
		int magic_count = cards_index[_logic.get_magic_card_index(0)];
		if(magic_count < que.size()){
			return false;
		}
		
		if(dispatch_magic_card){
			if(magic_count - 1 >= que.size())
				return true;
			for (Integer card : que) {
				int value = getCardValueScore(card);
				if(value > 2)//此处牌值只有1,9,10因此没有判断不报听的情况
					return true;
			}
			return false;
		}else{
			return true;
		}
		
	}
	//十三幺缺的牌
	private List<Integer> ssyQueCard(int[] cards_index) {
		List<Integer> arr = new ArrayList<Integer>();//十三幺缺的个数
		// 一九判断
		for (int i = 0; i < GameConstants.MAX_ZI; i += 9) {
			// 无效判断
			if (cards_index[i] == 0 || _logic.is_magic_index(i))
				arr.add(_logic.switch_to_card_data(i));
			if (cards_index[i + 8] == 0 || _logic.is_magic_index(i + 8))
				arr.add(_logic.switch_to_card_data(i + 8));
		}
		// 风牌判断
		for (int i = GameConstants.MAX_ZI; i < GameConstants.MAX_ZI_FENG; i++) {
		    if (cards_index[i] == 0 || _logic.is_magic_index(i))
		    	arr.add(_logic.switch_to_card_data(i));
		}
		return arr;
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
			if ( _playerStatus[i].is_chi_hu_round() && is_bao_ting[i]) {
				// 吃胡判断
				ChiHuRight chr = GRR._chi_hu_rights[i];
				chr.set_empty();
				int cbWeaveCount = GRR._weave_count[i];
				action = analyse_chi_hu_card(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr,
						Constants_MJ_HLKDD.HU_CARD_TYPE_QIANG_GANG_HU, i);

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
		
		//四喜
		setSiMouseScore(seat_index, operate_card, zimo);
		
		//包杠
		setBaoGangScore(seat_index, provide_index, chr);
		
		paiXingScore = getPaiXingScore(chr, seat_index, zimo);	
		float score = paiXingScore * hu_card_value;
		if(!chr.opr_and(Constants_MJ_HLKDD.CHR_MOUSE_DIAO_CARD).is_empty()){
				score = zimo ? 20 : 10;
			if(has_rule(Constants_MJ_HLKDD.GAME_RULE_JIAN_ZI_HU_60))
				score = 60;
		}
		score *= getBottomScore();
		////////////////////////////////////////////////////// 自摸 算分
		
		if (zimo || (!zimo && is_bao_ting[provide_index])) {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index) {
					continue;
				}
				float s = (i == GRR._banker_player || seat_index == GRR._banker_player) && has_rule(Constants_MJ_HLKDD.GAME_RULE_DAI_ZHUANG) ? score + 5 : score;
				GRR._game_score[i] -= s;
				GRR._game_score[seat_index] += s;
			}
		}else {
			float s = score * (getTablePlayerNumber() - 1);
			s = (provide_index == GRR._banker_player || seat_index == GRR._banker_player) && has_rule(Constants_MJ_HLKDD.GAME_RULE_DAI_ZHUANG) ? s + 5 : s;
			GRR._game_score[provide_index] -= s;
			GRR._game_score[seat_index] += s;
		}
		// 设置变量
		GRR._provider[seat_index] = provide_index;
		_status_gang = false;
		_status_gang_hou_pao = false;

		change_player_status(seat_index, GameConstants.INVALID_VALUE);
	}

	private void setSiMouseScore(int seat_index, int operate_card, boolean zimo) {
		if(_logic.get_magic_card_count() > 0){
			int magic_card_index = _logic.get_magic_card_index(0);
			int magic_card_data = _logic.switch_to_card_data(magic_card_index);
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				int magic_card_count = GRR._cards_index[i][magic_card_index];
				if(zimo && magic_card_data == operate_card && seat_index == i)
					magic_card_count++;
				for (int w = 0; w < GRR._weave_count[i]; w++) {
					if(GRR._weave_items[i][w].center_card == magic_card_data
							&& GRR._weave_items[i][w].weave_kind == GameConstants.WIK_GANG){
						magic_card_count = 4;
						break;
					}
					if(GRR._weave_items[i][w].center_card == magic_card_data
							&& GRR._weave_items[i][w].weave_kind == GameConstants.WIK_PENG){
						magic_card_count += 3;
						break;
					}
				}
				if(magic_card_count == 4){
					is_si_mouse[i] = true;
					int s = getFourMouseScore();
					for (int p = 0; p < getTablePlayerNumber(); p++) {
						if (p == i) {
							continue;
						}
						GRR._game_score[p] -= s;
						GRR._game_score[i] += s;
					}
					break;
				}
			}
		}
	}

	private void setBaoGangScore(int seat_index, int provide_index, ChiHuRight chr) {
		if(chr.opr_and(Constants_MJ_HLKDD.CHR_QIANG_GANG).is_empty())
			return;
		for(int g = 0; g < jie_gang_count[seat_index]; g++){
			float score = jie_gang_value[seat_index][g];
			if(is_jie_gang_ting[seat_index][g]){
				for (int i = 0; i < getTablePlayerNumber(); i++) {
					if(i == seat_index)
						continue;
					GRR._game_score[i] += score;
				}
				GRR._game_score[provide_index] -= score * (getTablePlayerNumber() - 1);
			}else{
				int p = jie_gang_provide[seat_index][g] - 1;
				GRR._game_score[p] += score * (getTablePlayerNumber() - 1);
				GRR._game_score[provide_index] -= score * (getTablePlayerNumber() - 1);
			}
		}
	}

	private int getPaiXingScore(ChiHuRight chr, int seat_index, boolean zimo) {
		if(!chr.opr_and(Constants_MJ_HLKDD.CHR_SHI_SAN_YAO).is_empty()
				|| !chr.opr_and(Constants_MJ_HLKDD.CHR_HAO_HUA_QI_XIAO_DUI).is_empty())
			return zimo ? 8 :4;
		if(!chr.opr_and(Constants_MJ_HLKDD.CHR_QING_YI_SE).is_empty() ||
				!chr.opr_and(Constants_MJ_HLKDD.CHR_YI_TIAO_LONG).is_empty() ||
				!chr.opr_and(Constants_MJ_HLKDD.CHR_QI_XIAO_DUI).is_empty())
			return zimo ? 4 :2;
		return zimo ? 2 :1;
	}

	@Override
	protected void set_result_describe() {
		int chrTypes;
		long type = 0;
		float bs = getBottomScore();
		for (int player = 0; player < this.getTablePlayerNumber(); player++) {
			StringBuilder result = new StringBuilder("");

			chrTypes = GRR._chi_hu_rights[player].type_count;

			for (int typeIndex = 0; typeIndex < chrTypes; typeIndex++) {
				type = GRR._chi_hu_rights[player].type_list[typeIndex];

				if (GRR._chi_hu_rights[player].is_valid()) {
					if (type == Constants_MJ_HLKDD.CHR_DIAN_PAO) {
						result.append(" 接炮");
					}
					if (type == Constants_MJ_HLKDD.CHR_ZI_MO) {
						result.append(" 自摸");
					}
					if (type == Constants_MJ_HLKDD.CHR_QIANG_GANG) {
						result.append(" 抢杠胡");
					}
					if (type == Constants_MJ_HLKDD.CHR_QI_XIAO_DUI) {
						result.append(" 七小对");
					}
					if (type == Constants_MJ_HLKDD.CHR_HAO_HUA_QI_XIAO_DUI) {
						result.append(" 豪华七小对");
					}
					if (type == Constants_MJ_HLKDD.CHR_QING_YI_SE) {
						result.append(" 清一色");
					}
					if (type == Constants_MJ_HLKDD.CHR_YI_TIAO_LONG) {
						result.append(" 一条龙");
					}
					if (type == Constants_MJ_HLKDD.CHR_SHI_SAN_YAO) {
						result.append(" 十三幺");
					}
					if (type == Constants_MJ_HLKDD.CHR_MOUSE_DIAO_CARD) {
						result.append(" 耗子吊牌");
					}
				} else if (type == Constants_MJ_HLKDD.CHR_FANG_PAO) {
					result.append(" 放炮");
				} else if (type == Constants_MJ_HLKDD.CHR_BEI_QIANG_GANG) {
					result.append(" 被抢杠");
				}	
			}
			
			if(GRR._chi_hu_rights[player].is_valid() && hu_card_value > 0){
				if(!GRR._chi_hu_rights[player].opr_and(Constants_MJ_HLKDD.CHR_MOUSE_DIAO_CARD).is_empty()){
					if(has_rule(Constants_MJ_HLKDD.GAME_RULE_JIAN_ZI_HU_60))
						result.append("60");
					else if(!GRR._chi_hu_rights[player].opr_and(Constants_MJ_HLKDD.CHR_ZI_MO).is_empty())
						result.append("20");
					else
						result.append("10");
				}else{
					float s = paiXingScore * hu_card_value * bs;
					s= (float) (Math.round(s * 10) / 10.0);
					result.append(hu_card_value + "x" + paiXingScore + "x" + (bs == (int)bs ? ""+((int)bs) : bs+"") + "=" + (s == (int)s ? ""+((int)s) : s+""));
				}
			}

			int /*jie_gang = 0,*/ fang_gang = 0/*, ming_gang = 0, an_gang = 0*/;

			if (GRR != null) {
				for (int tmpPlayer = 0; tmpPlayer < this.getTablePlayerNumber(); tmpPlayer++) {
					for (int w = 0; w < GRR._weave_count[tmpPlayer]; w++) {
						if (GRR._weave_items[tmpPlayer][w].weave_kind != GameConstants.WIK_GANG) {
							continue;
						}
						if (tmpPlayer == player) {
							int value = getCardValueScore(GRR._weave_items[tmpPlayer][w].center_card);
							value = _logic.is_magic_card(GRR._weave_items[tmpPlayer][w].center_card) ? 20 : value;
							if (GRR._weave_items[tmpPlayer][w].provide_player != tmpPlayer) {
								//jie_gang++;
								float s = value * bs;
								s= (float) (Math.round(s * 10) / 10.0);
								result.append(" 明杠" + value + "x1x" + (bs == (int)bs ? ""+((int)bs) : bs+"") + "=" + (s == (int)s ? ""+((int)s) : s+""));
							} else {
								if (GRR._weave_items[tmpPlayer][w].public_card == 1) {
									//ming_gang++;
									float s = value * bs;
									s= (float) (Math.round(s * 10) / 10.0);
									result.append(" 补杠" + value + "x1x" + (bs == (int)bs ? ""+((int)bs) : bs+"") + "=" + (s == (int)s ? ""+((int)s) : s+""));
								} else {
									//an_gang++;
									float s = value * bs * 2;
									s= (float) (Math.round(s * 10) / 10.0);
									result.append(" 暗杠" + value + "x2x" + (bs == (int)bs ? ""+((int)bs) : bs+"") + "=" + (s == (int)s ? ""+((int)s) : s+""));
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

			/*if (an_gang > 0) {
				result.append(" 暗杠X" + an_gang);
			}
			if (ming_gang > 0) {
				result.append(" 补杠X" + ming_gang);
			}*/
			if (fang_gang > 0) {
				result.append(" 放杠X" + fang_gang);
			}
			/*if (jie_gang > 0) {
				result.append(" 明杠X" + jie_gang);
			}*/
			
			if (is_si_mouse[player]) {
				result.append(" 四耗子");
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

			boolean can_peng = true;
			int[] tmp_cards_data = _playerStatus[i].get_cards_abandoned_peng();
			for (int x = 0; x < GameConstants.MAX_ABANDONED_CARDS_COUNT; x++) {
				if (tmp_cards_data[x] == card) {
					can_peng = false;
					break;
				}
			}
			if (GRR._left_card_count >= 0) {//如果牌堆已经没有牌，出的最后一张牌了不能碰，杠
				action = _logic.check_peng(GRR._cards_index[i], card);
				if (can_peng && action != 0 && !is_bao_ting[i]) {
					playerStatus.add_action(action);
					playerStatus.add_peng(card, seat_index);
					bAroseAction = true;
				}

				action = _logic.estimate_gang_card_out_card(GRR._cards_index[i], card);
				if (action != 0 && is_can_gang(i, card)) {
					playerStatus.add_action(GameConstants.WIK_GANG);
					playerStatus.add_gang(card, i, 1);
					bAroseAction = true;
				}
			}
			
			if (_playerStatus[i].is_chi_hu_round() && !_logic.is_magic_card(card) && is_bao_ting[i]) {
				ChiHuRight chr = GRR._chi_hu_rights[i];
				chr.set_empty();
				int cbWeaveCount = GRR._weave_count[i];
				int card_type = Constants_MJ_HLKDD.HU_CARD_TYPE_DIAN_PAO;
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
		magic_own_hu[seat_index] = false;
		for (int i = 0; i < GameConstants.MAX_ZI_FENG; i++) {
			cbCurrentCard = _logic.switch_to_card_data(i);
			chr.set_empty();

			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card_ting(cbCardIndexTemp, weaveItem, cbWeaveCount, cbCurrentCard, chr,
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
            GangCardResult gangCardResult, boolean check_weave, int cards_abandoned_gang[]) {
        // 设置变量
        int cbActionMask = GameConstants.WIK_NULL;

        // 手上杠牌
        for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
            if (cards_index[i] == 4 && cards_abandoned_gang[i] != 1) {
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
        	if(reason == GameConstants.Game_End_DRAW){
        		for (int i = 0; i < getTablePlayerNumber(); i++)
        			if(GRR._gang_score[i].gang_count > 0){
        				_cur_banker = (_cur_banker + 1) % getTablePlayerNumber();
        				break;
        			}
        	}
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
                // 记录
                for (int j = 0; j < getTablePlayerNumber(); j++) {
                    _player_result.lost_fan_shu[i][j] += GRR._lost_fan_shu[i][j];
                }
                for (int j = 0; j < GRR._gang_score[i].gang_count && reason == GameConstants.Game_End_NORMAL; j++) {
            		for (int k = 0; k < getTablePlayerNumber(); k++) {
            			lGangScore[k] += GRR._gang_score[i].scores[j][k];// 杠牌，每个人的分数
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
			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card_ting(cbCardIndexTemp, weaves,
					weave_count, cbCurrentCard, chr,
					GameConstants.CHR_ZI_MO, seat_index)) {
				s.add(cbCurrentCard);
			}
		}
		
		int ting_cards[] = _playerStatus[seat_index]._hu_cards;
		int ting_count = _playerStatus[seat_index]._hu_card_count;
		
		Set<Integer> sh = new HashSet<Integer>();
		for (int i = 0; i < ting_count; i++) {
			if(ting_cards[i] == -1)
				return true;
			sh.add(ting_cards[i]);
		}
		return !s.isEmpty() && sh.containsAll(s);
	}
	
	// 七小对牌 七小对：胡牌时，手上任意七对牌。
	public int is_qi_xiao_dui(int cards_index[], int cbWeaveCount, int cur_card) {
		//boolean is_magic_card = _logic.is_magic_card(cur_card);
		// 组合判断
		if (cbWeaveCount != 0)
			return GameConstants.WIK_NULL;

		// 单牌数目
		int cbReplaceCount = 0;
		int cbReplaceCount3 = 0;
		int nGenCount = 0;
		List<Integer> arr = new ArrayList<Integer>();//单的牌
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
					if (cbCardCount == 1){
						arr.add(_logic.switch_to_card_data(i));
						cbReplaceCount++;
					}
					if (cbCardCount == 3){
						arr.add(_logic.switch_to_card_data(i));
						cbReplaceCount3++;
					}
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

			if (cbReplaceCount + cbReplaceCount3 > count) {
				return GameConstants.WIK_NULL;
			}
			// //王牌不够
			// if( get_magic_card_index() != MJGameConstants.MAX_INDEX &&
			// cbReplaceCount > cbCardIndexTemp[get_magic_card_index()] ||
			// get_magic_card_index() == MJGameConstants.MAX_INDEX &&
			// cbReplaceCount > 0 )
			// return MJGameConstants.WIK_NULL;
		} else {
			if (cbReplaceCount + cbReplaceCount3 > 0)
				return GameConstants.WIK_NULL;
		}
		
		if (nGenCount > 0 || (count - cbReplaceCount -cbReplaceCount3) > 1 || (count > 0 && cbReplaceCount3 > 0)) {
			return GameConstants.CHR_HUNAN_HAOHUA_QI_XIAO_DUI;
		} else {
			return GameConstants.CHR_HUNAN_QI_XIAO_DUI;
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
		int[] cards_of_player0 = new int[] { 0x12,0x13,0x14,0x15,0x01,0x02,0x03,0x04,0x04,0x04,0x05,0x06,0x07 };
		int[] cards_of_player1 = new int[] { 0x12,0x12,0x27,0x28,0x01,0x02,0x03,0x04,0x04,0x04,0x05,0x06,0x07 };
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
