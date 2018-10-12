package com.cai.game.mj.henan.kulongdaishen;

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.GameConstants_KLDS;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.Player;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.SysParamModel;
import com.cai.common.domain.WeaveItem;
import com.cai.common.util.PBUtil;
import com.cai.common.util.RandomUtil;
import com.cai.dictionary.SysParamDict;
import com.cai.future.GameSchedule;
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
import protobuf.clazz.mj.Klds.DingSheng;

public class MJTable_KLDS extends AbstractMJTable {
	
	
	@Override
	public int getTablePlayerNumber(){
		return GameConstants.GAME_PLAYER;
	}
	
	protected MJHandlerDingSheng_KLDS handlerDingSheng_KLDS; 
	
	protected MJHandlerPaoQiang_KLDS handlerPaoQiang_KLDS;
	
	protected MJHandlerOutCardBaoTing_KLDS _handler_out_card_bao_ting;
	
	public boolean ding_sheng;    // 是否定神
	
/*	public int[] ding_shen_crad;  //定神出牌
	
	public int out_card_ding_shen; //定神前出牌数量
*/	
	public int[] pao;     //跑
	
	public int[] ziba;	 //自拔
	
	public int[] duanmen;   //断门
	
	public int ding_sheng_count;   //换神次数
	
	
	private static final long serialVersionUID = 1L;

	
	 public MJTable_KLDS() {
        super(MJType.GAME_TYPE_KLDS);
	 }
	
	@Override
	protected void onInitTable() {
		_handler_chi_peng = new MJHandlerChiPeng_KLDS();
        _handler_dispath_card = new MJHandlerDispatchCard_KLDS();
        _handler_gang = new MJHandlerGang_KLDS();
        _handler_out_card_operate = new MJHandlerOutCardOperate_KLDS();
        _handler_out_card_bao_ting = new MJHandlerOutCardBaoTing_KLDS();
        handlerDingSheng_KLDS = new MJHandlerDingSheng_KLDS();
        handlerPaoQiang_KLDS = new MJHandlerPaoQiang_KLDS();
	}
	
	
    public boolean exe_out_card_bao_ting(int seat_index, int card, int type) {
        // 出牌
        this.set_handler(this._handler_out_card_bao_ting);
        this._handler_out_card_bao_ting.reset_status(seat_index, card, type);
        this._handler.exe(this);

        return true;
    }
  
  
	@Override
	protected boolean on_game_start() {
		 //只有第一局需要下拔
		 if (_cur_round > 1 || has_rule(GameConstants_KLDS.GAME_RULE_KLDS_BUPAO)) { 
	        return real_game_start();
	     } else {
	        GRR._banker_player = _current_player = GameConstants.INVALID_SEAT;
	        this.set_handler(this.handlerPaoQiang_KLDS);
	        this.handlerPaoQiang_KLDS.exe(this);
	        return false;
	     }
	}
	
	
	/**
	 * 初始化独有参数
	 */
	public void inint_self_param(){
		ding_sheng = false;
		/*ding_shen_crad = new int[getTablePlayerNumber()];
		out_card_ding_shen = 0;*/
	}
	
	public boolean real_game_start(){
		if(!has_rule(GameConstants_KLDS.GAME_RULE_KLDS_BUPAO)){
			//重新赋值下跑值
			for(int i = 0; i < getTablePlayerNumber(); i++ ){
				_player_result.pao[i] = pao[i];
				_player_result.ziba[i] = ziba[i];
				_player_result.duanmen[i] = duanmen[i];
			}
		}
		
		ding_sheng_count = 0;
		
		operate_player_data();
		
		this.inint_self_param();
		
		_logic.clean_magic_cards();
		_game_status = GameConstants.GS_MJ_PLAY;
        GameStartResponse.Builder gameStartResponse = GameStartResponse.newBuilder();
        gameStartResponse.setBankerPlayer(GRR._banker_player);
        gameStartResponse.setCurrentPlayer(_current_player);
        gameStartResponse.setLeftCardCount(GRR._left_card_count);

        int hand_cards[][] = new int[getTablePlayerNumber()][GameConstants.MAX_COUNT];

        for (int i = 0; i < getTablePlayerNumber(); i++) {
            int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[i], hand_cards[i]);
            gameStartResponse.addCardsCount(hand_card_count);
        }

        for (int i = 0; i < getTablePlayerNumber(); i++) {
            Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

            gameStartResponse.clearCardData();
            for (int j = 0; j < GameConstants.MAX_COUNT; j++) {
                gameStartResponse.addCardData(hand_cards[i][j]);
            }

            GRR._video_recode.addHandCards(cards);

            RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
            load_room_info_data(roomResponse);
            load_common_status(roomResponse);
            roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
            roomResponse.setGameStart(gameStartResponse);
            roomResponse
                    .setCurrentPlayer(_current_player == GameConstants.INVALID_SEAT ? _resume_player : _current_player);
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
            _playerStatus[i]._hu_card_count = get_ting_card(_playerStatus[i]._hu_cards, GRR._cards_index[i],
                    GRR._weave_items[i], GRR._weave_count[i], i);
            if (_playerStatus[i]._hu_card_count > 0) {
                operate_chi_hu_cards(i, _playerStatus[i]._hu_card_count, _playerStatus[i]._hu_cards);
            }
        }

        exe_dispatch_card(_current_player, GameConstants.WIK_NULL, GameConstants.DELAY_SEND_CARD_DELAY);
        return true;
	}
	
	
	/**
	 * 听牌显示
	 * @param cards
	 * @param cards_index
	 * @param weaveItem
	 * @param cbWeaveCount
	 * @param seat_index
	 * @return
	 */
	public int get_ting_card(int[] cards, int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int seat_index) {
        int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
        for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
            cbCardIndexTemp[i] = cards_index[i];
        }

        ChiHuRight chr = new ChiHuRight();

        int count = 0;
        int cbCurrentCard;

        int max_ting_count = GameConstants.MAX_ZI_FENG;

        for (int i = 0; i < max_ting_count; i++) {
            cbCurrentCard = _logic.switch_to_card_data(i);
           
            chr.set_empty();

            if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card(cbCardIndexTemp, weaveItem, cbWeaveCount, cbCurrentCard,
                    chr, GameConstants.HU_CARD_TYPE_ZIMO, seat_index)) {
                cards[count] = cbCurrentCard;
                count++;
            }
        }
        if (count == 0) {
        	
        } else if (count == max_ting_count) {
            count = 1;
            cards[0] = -1;
        } 

        return count;
    }
	
	
	/**
	 * 报飞判断
	 * @param cards
	 * @param cards_index
	 * @param weaveItem
	 * @param cbWeaveCount
	 * @param seat_index
	 * @return
	 */
	public int get_bao_fei_card(int[] cards, int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int seat_index) {
        int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
        for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
            cbCardIndexTemp[i] = cards_index[i];
        }

        int count = 0;
        
        int[] magic_cards_index = new int[GameUtilConstants.MAX_MAGIC_INDEX_COUNT];
        int magic_card_count = _logic.get_magic_card_count();

        if (magic_card_count > 2) { // 一般只有两种癞子牌存在
            magic_card_count = 2;
        }

        for (int i = 0; i < magic_card_count; i++) {
            magic_cards_index[i] = _logic.get_magic_card_index(i);
        }
        
        boolean hu = AnalyseCardUtil.analyse_win_by_cards_index(cbCardIndexTemp, GameConstants.INVALID_CARD, magic_cards_index, magic_card_count);
        if(hu){
        	//报飞状态只听5-8
        	int[] cardArray = new int[]{0x05,0x06,0x07,0x08,0x15,0x16,0x17,0x18,0x25,0x26,0x27,0x28};
        	boolean flag = true;
        	for (int i : cardArray) {
        		if(i == _logic.switch_to_card_data(_logic.get_magic_card_index(0))){
        			flag = false;
        		}
        		cards[count]  = i;
        		count++;
			}
        	if(flag){
        		cards[count] = _logic.switch_to_card_data(_logic.get_magic_card_index(0));
        		count++; 
        	}
        }
        return count;
    }
	
	
	@Override
	public int analyse_chi_hu_card(int[] cards_index, WeaveItem[] weaveItems, int weave_count, int cur_card,
			ChiHuRight chiHuRight, int card_type, int _seat_index) {
		chiHuRight.set_empty();
		if (cur_card == 0) {
			return GameConstants.WIK_NULL;
		}
		
		//不能胡1、2、9不能胡，中发白不能胡，风牌不能胡；
		int val = _logic.get_card_value(cur_card);
		int color = _logic.get_card_color(cur_card);
		if(!_logic.is_magic_card(cur_card)){
			if(val == 1 || val == 9 || val == 2 || color > 2){
				return GameConstants.WIK_NULL;
			}
		}
		
		//报飞状态只能胡 5-8
		if(_playerStatus[_seat_index].is_bao_ting()){
			if(card_type != GameConstants.HU_CARD_TYPE_ZIMO){
				return GameConstants.WIK_NULL;
			}
			if( val != 5 &&  val != 6 && val != 7  &&val != 8  && !_logic.is_magic_card(cur_card)){
				return GameConstants.WIK_NULL;
			}
		}
		//构造数据
		int cbChiHuKind = GameConstants.WIK_NULL;
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}
		
		if (cur_card != GameConstants.INVALID_VALUE) {
			cbCardIndexTemp[_logic.switch_to_card_index(cur_card)]++;
		}
		

        int[] magic_cards_index = new int[GameUtilConstants.MAX_MAGIC_INDEX_COUNT];
        int magic_card_count = _logic.get_magic_card_count();

        if (magic_card_count > 2) { // 一般只有两种癞子牌存在
            magic_card_count = 2;
        }

        for (int i = 0; i < magic_card_count; i++) {
            magic_cards_index[i] = _logic.get_magic_card_index(i);
        }

        int[] bValue = AnalyseCardUtil.analyse_ckeck_ka_bian_diao(cards_index, _logic.switch_to_card_index(cur_card),
                magic_cards_index, magic_card_count,true);

        if(card_type != GameConstants.HU_CARD_TYPE_ZIMO){
        	if(_logic.is_magic_card(cur_card) && bValue[0] > GameConstants.WIK_NULL 
        			&& bValue[1] != _logic.switch_to_card_index(cur_card) ){
        		return GameConstants.WIK_NULL;
        	}
        }
        
        int hu_card_index = _logic.switch_to_card_index(cur_card);
        if(_logic.is_magic_card(cur_card) &&  bValue[0] > GameConstants.WIK_NULL){
        	hu_card_index =  bValue[1];
		}
        
        //三抱一不让胡
		if(cbCardIndexTemp[  hu_card_index+ 1 ] == 3 && hu_card_index < 41){
			return cbChiHuKind;
		}else if(hu_card_index > 1 && cbCardIndexTemp[ hu_card_index - 1 ] == 3){
			return cbChiHuKind;
		}
        
        //缺门
        int colorCount = this._logic.get_se_count(cbCardIndexTemp, weaveItems, weave_count);
        if(colorCount < 3){
        	chiHuRight.opr_or(GameConstants.CHR_HENAN_XY_HUNQUE);
        	chiHuRight.duanmen_count = 3 - colorCount;
        }

        //能胡窟窿
        //if(ka_bian && bValue){
        if( bValue[0] > GameConstants.WIK_NULL){
        	chiHuRight.opr_or(GameConstants.CHR_HENAN_KA_ZHANG);
        	int val_kl =_logic.get_card_value(_logic.switch_to_card_data(bValue[1])); 
        	//报飞牌型胡神牌大窟窿
        	if(_playerStatus[_seat_index].is_bao_ting() && _logic.is_magic_card(cur_card)){
        		chiHuRight.opr_or(GameConstants_KLDS.CHR_HENAN_KLDS_DA);
        	}else{
        		//窟窿大小
        		if(val_kl <= 4){
        			chiHuRight.opr_or(GameConstants_KLDS.CHR_HENAN_KLDS_XIAO);
        		}else if(val_kl > 4 && val_kl <=6){
        			chiHuRight.opr_or(GameConstants_KLDS.CHR_HENAN_KLDS_ZHONG);
        		}else if(val_kl > 6 && val_kl <9){
        			chiHuRight.opr_or(GameConstants_KLDS.CHR_HENAN_KLDS_DA);
        		}
        	}
        	cbChiHuKind = GameConstants.WIK_CHI_HU;
        	//自拔
        	if(card_type == GameConstants.HU_CARD_TYPE_ZIMO){
        		chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
        	}else{
        		chiHuRight.opr_or(GameConstants.CHR_SHU_FAN);
        	}
    		chiHuRight.opr_or(cbChiHuKind);
        }else{
        	chiHuRight.set_empty();
        }

		return cbChiHuKind;
	}
	
	/*@Override
	public int analyse_chi_hu_card(int[] cards_index, WeaveItem[] weaveItems, int weave_count, int cur_card,
			ChiHuRight chiHuRight, int card_type, int _seat_index) {
		chiHuRight.set_empty();
		if (cur_card == 0) {
			return GameConstants.WIK_NULL;
		}
		
		//不能胡1、2、9不能胡，中发白不能胡，风牌不能胡；
		int val = _logic.get_card_value(cur_card);
		int color = _logic.get_card_color(cur_card);
		if(color > 2 || val == 1 || val == 9 || val == 2){
			return GameConstants.WIK_NULL;
		}

		//报飞状态只能胡 5-8
		if(_playerStatus[_seat_index].is_bao_ting()){
			if(card_type != GameConstants.HU_CARD_TYPE_ZIMO){
				return GameConstants.WIK_NULL;
			}
			if( val != 5 &&  val != 6 && val != 7  &&val != 8 ){
				return GameConstants.WIK_NULL;
			}
		}
		
		int cbChiHuKind = GameConstants.WIK_NULL;

		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		int index = _logic.switch_to_card_index(cur_card);
		//三抱一不让胡
		if(cbCardIndexTemp[ index + 1 ] == 3){
			return cbChiHuKind;
		}else if(cbCardIndexTemp[ index - 1 ] == 3){
			return cbChiHuKind;
		}
		
		//剔除一个窟窿 顺子
		if( cbCardIndexTemp[index+1] > 0 
				&& index != _logic.get_magic_card_index(0)-1){
			cbCardIndexTemp[index+1]--;
		} else {
			needMagic++;
		}
		if ( cbCardIndexTemp[index-1] > 0 
				&& index != _logic.get_magic_card_index(0)+1){
			cbCardIndexTemp[index-1]--;
		} else {
			needMagic++;
		}
		if( needMagic > 0 && cbCardIndexTemp[_logic.get_magic_card_index(0)] < needMagic ){
			return GameConstants.WIK_NULL;
		}else{
			cbCardIndexTemp[_logic.get_magic_card_index(0)] -= needMagic;
		}
		
		 // 分析扑克
		int[] magic_cards_index = new int[GameUtilConstants.MAX_MAGIC_INDEX_COUNT];
        int magic_card_count = _logic.get_magic_card_count();

        if (magic_card_count > 2) { // 一般只有两种癞子牌存在
            magic_card_count = 2;
        }

        for (int i = 0; i < magic_card_count; i++) {
            magic_cards_index[i] = _logic.get_magic_card_index(i);
        }

        boolean bValue = AnalyseCardUtil.analyse_win_by_cards_index(cbCardIndexTemp,cur_card,magic_cards_index, magic_card_count);
      
        //缺门
        int colorCount = this._logic.get_se_count(cbCardIndexTemp, weaveItems, weave_count);
        if(colorCount < 3){
        	chiHuRight.opr_or(GameConstants.CHR_HENAN_XY_HUNQUE);
        	chiHuRight.duanmen_count = 3 - colorCount;
        }

        //能胡窟窿
        if( bValue){
        	 chiHuRight.opr_or(GameConstants.CHR_HENAN_KA_ZHANG);
        	//窟窿大小
        	if(val <= 3){
        		chiHuRight.opr_or(GameConstants_KLDS.CHR_HENAN_KLDS_XIAO);
        	}else if(val > 3 && val <=6){
        		chiHuRight.opr_or(GameConstants_KLDS.CHR_HENAN_KLDS_ZHONG);
        	}else if(val > 6 && val <9){
        		chiHuRight.opr_or(GameConstants_KLDS.CHR_HENAN_KLDS_DA);
        	}
        	cbChiHuKind = GameConstants.WIK_CHI_HU;
        	//自拔
        	if(card_type == GameConstants.HU_CARD_TYPE_ZIMO){
        		chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
        	}else{
        		chiHuRight.opr_or(GameConstants.CHR_SHU_FAN);
        	}
    		chiHuRight.opr_or(cbChiHuKind);
        }else{
        	chiHuRight.set_empty();
        }

		return cbChiHuKind;
	}*/

	@Override
	public void process_chi_hu_player_score(int seat_index, int provide_index, int operate_card, boolean zimo) {
		GRR._chi_hu_card[seat_index][0] = operate_card;

        GRR._win_order[seat_index] = 1;
        // 引用权位
        ChiHuRight chr = GRR._chi_hu_rights[seat_index];

        int wFanShu = get_fan(chr,seat_index,provide_index,zimo);// 番数
        countCardType(chr, seat_index);

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
        int fen = 0;
        if(has_rule(GameConstants_KLDS.GAME_RULE_KLDS_DI1)){
        	fen = 1;
        }else if(has_rule(GameConstants_KLDS.GAME_RULE_KLDS_DI5)){
        	fen = 5;
        }else{
        	fen = ruleMap.get(GameConstants_KLDS.GAME_RULE_DI_FENG_KEY);
        }

        ////////////////////////////////////////////////////// 自摸 算分
        if (zimo) {
            for (int i = 0; i < getTablePlayerNumber(); i++) {
                if (i == seat_index) {
                    continue;
                }

                float s = lChiHuScore * fen;

                // 胡牌分
                GRR._game_score[i] -= s;
                GRR._game_score[seat_index] += s;
            }
        }
        ////////////////////////////////////////////////////// 点炮 算分
        else {
            float s = lChiHuScore * fen;

            // 胡牌分
            GRR._game_score[provide_index] -= s;
            GRR._game_score[seat_index] += s;

            GRR._chi_hu_rights[provide_index].opr_or(GameConstants.CHR_FANG_PAO);
        }

        // 设置变量
        GRR._provider[seat_index] = provide_index;
        change_player_status(seat_index, GameConstants.INVALID_VALUE);
		
	}


	/**
	 * 算分计算
	 * @param chr
	 * @param seat_index
	 * @param provide_index
	 * @return
	 */
	public int get_fan(ChiHuRight chr,int seat_index,int provide_index,boolean zimo){
		int fan = 0;
		if(has_rule(GameConstants_KLDS.GAME_RULE_KLDS_XIAPAO)){
			if(zimo){
				fan += _player_result.pao[seat_index];
			}else{
				fan += _player_result.pao[seat_index] + _player_result.pao[provide_index];
			}
		}
		
		//窟窿
		if(!(chr.opr_and(GameConstants_KLDS.CHR_HENAN_KLDS_XIAO)).is_empty()){
			fan += 1;
		}else if(!(chr.opr_and(GameConstants_KLDS.CHR_HENAN_KLDS_ZHONG)).is_empty()){
			fan += 2;
		}else if(!(chr.opr_and(GameConstants_KLDS.CHR_HENAN_KLDS_DA)).is_empty()){
			fan += 3;
		}
		
		//断门
		if(!(chr.opr_and(GameConstants.CHR_HENAN_XY_HUNQUE)).is_empty()){
			fan += _player_result.duanmen[seat_index] * chr.duanmen_count;
		}
		//自拔
		if(!(chr.opr_and(GameConstants.CHR_ZI_MO)).is_empty()){
			fan += _player_result.ziba[seat_index]; 
		}
		
		if(fan == 0){
			fan = 1;
		}
		
		return fan;
	}
	
	
	@Override
	protected void set_result_describe() {
		int l;
        long type = 0;
        for (int i = 0; i < getTablePlayerNumber(); i++) {
            String des = "";
            l = GRR._chi_hu_rights[i].type_count;
            for (int j = 0; j < l; j++) {
                type = GRR._chi_hu_rights[i].type_list[j];
                
        		//窟窿
        		if(type == GameConstants_KLDS.CHR_HENAN_KLDS_XIAO){
        			 des += " 窟窿,"+1+",";
        		}else if(type == GameConstants_KLDS.CHR_HENAN_KLDS_ZHONG){
        			 des += " 窟窿,"+2+",";
        		}else if(type == GameConstants_KLDS.CHR_HENAN_KLDS_DA){
        			 des += " 窟窿,"+3+",";
        		}
        		
        		//断门
        		if(type == GameConstants.CHR_HENAN_XY_HUNQUE){
        			 des += " 断门,"+ _player_result.duanmen[i] * GRR._chi_hu_rights[i].duanmen_count+",";
        		}
        		//自拔
        		if(type == GameConstants.CHR_ZI_MO){
        			des += "自拔,"+ _player_result.ziba[i]+",";; 
        		}
            }
            if(has_rule(GameConstants_KLDS.GAME_RULE_KLDS_XIAPAO)){
            	des += "下跑,"+_player_result.pao[i]+",";
    		}
            GRR._result_des[i] = des;
        }
	}

	

	/**
	 * @param _out_card_player
	 * @param _out_card_data
	 * @return
	 */
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
            if (playerStatus.is_bao_ting() == false) {
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
            		if ((playerStatus.is_bao_ting() == false)) {
						playerStatus.add_action(GameConstants.WIK_GANG);
						playerStatus.add_gang(card, i, 1);// 加上杠
						bAroseAction = true;
					} else {
						if (check_gang_huan_zhang(GameConstants.GAME_TYPE_HENAN_AY, i, card) == false) {
							playerStatus.add_action(GameConstants.WIK_GANG);
							playerStatus.add_gang(card, i, 1);// 加上杠
							bAroseAction = true;
						}
					}
            	}
            }

            // 可以胡的情况 判断
            if (_playerStatus[i].is_chi_hu_round()) {
            	if ( _playerStatus[i].is_bao_ting()) {
					continue;
				}
                // 吃胡判断
                ChiHuRight chr = GRR._chi_hu_rights[i];// playerStatus._chiHuRight;
                chr.set_empty();
                int cbWeaveCount = GRR._weave_count[i];
                action = analyse_chi_hu_card(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr,
                        GameConstants.HU_CARD_TYPE_PAOHU,seat_index);

                // 结果判断
                if (action != 0) {
                    _playerStatus[i].add_action(GameConstants.WIK_CHI_HU);
                    _playerStatus[i].add_chi_hu(card, seat_index);// 吃胡的组合
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
	
	/**
	 * @param _out_card_player
	 * @param _out_card_data
	 * @return
	 */
	public boolean estimate_player_out_card_respond_sheng(int seat_index, int card) {
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

            // 可以胡的情况 判断
            if (_playerStatus[i].is_chi_hu_round()) {
            	if ( _playerStatus[i].is_bao_ting()) {
					continue;
				}
                // 吃胡判断
                ChiHuRight chr = GRR._chi_hu_rights[i];// playerStatus._chiHuRight;
                chr.set_empty();
                int cbWeaveCount = GRR._weave_count[i];
                action = analyse_chi_hu_card(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr,
                        GameConstants.HU_CARD_TYPE_PAOHU,seat_index);

                // 结果判断
                if (action != 0) {
                    _playerStatus[i].add_action(GameConstants.WIK_CHI_HU);
                    _playerStatus[i].add_chi_hu(card, seat_index);// 吃胡的组合
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

	
	
	 /**
     * 处理牌桌结束逻辑
     * 
     * @param seat_index
     * @param reason
     */
    protected boolean on_handler_game_finish(int seat_index, int reason) {
        int real_reason = reason;

        for (int i = 0; i < getTablePlayerNumber(); i++) {
            _player_ready[i] = 0;
        }

        RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
        roomResponse.setType(MsgConstants.RESPONSE_GAME_END);
        GameEndResponse.Builder game_end = GameEndResponse.newBuilder();

        roomResponse.setLeftCardCount(0);

        this.load_common_status(roomResponse);
        this.load_room_info_data(roomResponse);

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

            // 特别显示的牌
            for (int i = 0; i < GRR._especial_card_count; i++) {
                game_end.addEspecialShowCards(GRR._especial_show_cards[i]);
            }

            GRR._end_type = reason;

            // 杠牌，每个人的分数
            float lGangScore[] = new float[getTablePlayerNumber()];
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
                for (int j = 0; j < GRR._player_niao_count[i]; j++) {
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
                	if(AbstractMJTable.DEBUG_CARDS_MODE){
                		logger.info("手牌=="+ j+" ==="+GRR._cards_data[i][j]);
                	}
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
                game_end.addStartHuScore((int)_player_result.game_score[i]);
                game_end.addResultDes(GRR._result_des[i]);
                //game_end.add
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
            real_reason = GameConstants.Game_End_DRAW;
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
	
    
    /* (non-Javadoc)
	 * @see com.cai.common.domain.Room#trustee_timer(int, int)
	 */
	@Override
	public boolean trustee_timer(int operate_id, int seat_index) {
		// WalkerGeek Auto-generated method stub
		return false;
	}
	
	
	@Override
	public boolean handler_requst_xia_ba(Player player, int pao, int ziBa,int duanmen) {
		if (handlerPaoQiang_KLDS != null) {
			return ((MJHandlerPaoQiang_KLDS) handlerPaoQiang_KLDS).handler_pao_qiang(this, player.get_seat_index(), pao, ziBa,duanmen);
		} 
		return false;
	}
	
	
	
	//定神牌显示
    public boolean operate_show_card_ding_sheng(int seat_index, int type, int count, int cards[], int to_player) {
        RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
        this.load_common_status(roomResponse);
        roomResponse.setType(MsgConstants.RESPONSE_DING_SHEN);
        roomResponse.setTarget(seat_index);
        roomResponse.setCardType(type);// 出牌
        roomResponse.setCardCount(count);
        for (int i = 0; i < count; i++) {
            roomResponse.addCardData(cards[i]);
        }
        GRR.add_room_response(roomResponse);
        if (to_player == GameConstants.INVALID_SEAT) {
            return this.send_response_to_room(roomResponse);
        } else {
            return this.send_response_to_player(to_player, roomResponse);
        }
    }
    
    
    /**
     * 通知客户端有神用户
     * @param seat_index
     * @param dingShen
     * @return
     */
    public boolean operate_show_you_sheng(int seat_index, boolean dingShen) {
        RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
        this.load_common_status(roomResponse);
        roomResponse.setType(MsgConstants.RESPONSE_DING_SHEN_YOU_SHENG);
        roomResponse.setTarget(seat_index);
        DingSheng.Builder builder = DingSheng.newBuilder();
        builder.setHasSheng(dingShen);
        roomResponse.setCommResponse(PBUtil.toByteString(builder));
        
        GRR.add_room_response(roomResponse);
        return this.send_response_to_room(roomResponse);
       
    }
	
    
    /**
     * 执行定神
     * @param seat_index
     */
    public void exe_ding_sheng(int seat_index,int delay,MJTable_KLDS table){
    	if(delay > 0){
    		 GameSchedule.put(new Runnable() {
				
				@Override
				public void run() {
					set_handler(handlerDingSheng_KLDS);
		    		handlerDingSheng_KLDS.reset_status(seat_index);
		    		handlerDingSheng_KLDS.exe(table);
				}
			}, 10, TimeUnit.SECONDS);
    	}else{
    		this.set_handler(this.handlerDingSheng_KLDS);
    		this.handlerDingSheng_KLDS.reset_status(seat_index);
    		this.handlerDingSheng_KLDS.exe(this);
    	}
    }
    
    
    public void show_shai_zi( int seat_index){
		int num1 = (int) (RandomUtil.getRandomNumber(Integer.MAX_VALUE) % 6) + 1;
		int num2 = (int) (RandomUtil.getRandomNumber(Integer.MAX_VALUE) % 6) + 1;
		
		_player_result.shaizi[seat_index][0] = num1;
		_player_result.shaizi[seat_index][1] = num2;
		this.operate_shai_zi_effect(num1,num2,1450,1000);
	}
    
    public boolean operate_shai_zi_effect(int num1, int num2, int anim_time, int delay_time) {
        RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
        roomResponse.setType(MsgConstants.RESPONSE_EFFECT_ACTION);
        roomResponse.setEffectType(GameConstants.Effect_Action_SHAI_ZI);
        roomResponse.setEffectCount(2);
        roomResponse.addEffectsIndex(num1);
        roomResponse.addEffectsIndex(num2);
        roomResponse.setEffectTime(anim_time);// anim time//摇骰子动画的时间
        roomResponse.setStandTime(delay_time); // delay time//停留时间
        this.send_response_to_room(roomResponse);
        GRR.add_room_response(roomResponse);
        return true;
    }
    
    
    
    /**
     * 显示中间出的牌
     * 
     * @param seat_index
     */
    public void runnable_remove_ding_shen_middle_cards(int seat_index,int sheng_card) {
    	
		// 牌局未开 或者等待状态 调度不需要执行 add by zain 2017/6/1
        if ((_game_status == GameConstants.GS_MJ_FREE || _game_status == GameConstants.GS_MJ_WAIT) && is_sys())
            return;
        // 去掉
        this.operate_show_card_ding_sheng(seat_index, GameConstants.Show_Card_Center, 0, null, GameConstants.INVALID_SEAT);
        
        //确认定神
        ding_sheng = true;
        //加入白搭牌
        this._logic.add_magic_card_index(this._logic.switch_to_card_index(sheng_card));
        this.GRR._especial_card_count=1;
        this.GRR._especial_show_cards[0] = sheng_card;
        
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
                for (int j = 0; j < hand_card_count; j++) {
                    if (_logic.is_magic_card(cards[j])) {
                        cards[j] += GameConstants.CARD_ESPECIAL_TYPE_SHENGPAI;
                    }
                }
                this.operate_player_cards(i, hand_card_count, cards, 0, null);
            }
        }

        // 检测听牌
        for (int i = 0; i < getTablePlayerNumber(); i++) {
            this._playerStatus[i]._hu_card_count = this.get_ting_card(this._playerStatus[i]._hu_cards,
                    this.GRR._cards_index[i], this.GRR._weave_items[i], this.GRR._weave_count[i],i);
            if (this._playerStatus[i]._hu_card_count > 0) {
                this.operate_chi_hu_cards(i, this._playerStatus[i]._hu_card_count, this._playerStatus[i]._hu_cards);
            }
        }

        this.exe_dispatch_card(this._current_player, GameConstants.WIK_NULL, GameConstants.DELAY_SEND_CARD_DELAY);

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
        int flashTime = 150;
        int standTime = 150;
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

                roomResponse.addCardData(cards[i]);
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
    
    
    /**
     * 玩家动作--通知玩家弹出/关闭操作
     * 
     * @param seat_index
     * @param close
     * @return
     */
    public boolean operate_player_action_type(int seat_index, boolean close) {
        PlayerStatus curPlayerStatus = _playerStatus[seat_index];

        RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
        roomResponse.setType(MsgConstants.RESPONSE_PLAYER_ACTION_DING_SHEN);
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
                        _playerStatus[seat_index]._hu_out_card_ting[i] + GameConstants.CARD_ESPECIAL_TYPE_SHENGPAI_TING);
            } else {
                roomResponse.addOutCardTing(
                        _playerStatus[seat_index]._hu_out_card_ting[i] + GameConstants.CARD_ESPECIAL_TYPE_TING);
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
    
    
    /**
	 * @param gameTypeHenanAy
	 * @param _seat_index
	 * @param i
	 * @return
	 */
	public boolean check_gang_huan_zhang(int mj_type, int seat_index, int card) {
		 // 不能换章，需要检测是否改变了听牌
        int gang_card_index = _logic.switch_to_card_index(card);
        int gang_card_count = GRR._cards_index[seat_index][gang_card_index];
        // 假如杠了
        GRR._cards_index[seat_index][gang_card_index] = 0;

        int hu_cards[] = new int[GameConstants.MAX_INDEX];

        // 检查听牌
        int hu_card_count = get_ting_card( hu_cards, GRR._cards_index[seat_index],
                    GRR._weave_items[seat_index], GRR._weave_count[seat_index],seat_index);

        // 还原手牌
        GRR._cards_index[seat_index][gang_card_index] = gang_card_count;
        if (hu_card_count > 0) {
            return false;
        } else {
            return true;
        }
	}

    
    
    @Override
    public void test_cards() {
        int[] cards_of_player0 = new int[] { 0x11,0x11,0x12,0x13,0x14,0x14,0x22,0x15,0x22,0x22,0x27,0x27,0x27};
        int[] cards_of_player1 = new int[] { 0x06, 0x06, 0x06, 0x36, 0x36, 0x35, 0x36, 0x27, 0x29, 0x37, 0x37, 0x35,
                0x35};
        int[] cards_of_player3 = new int[] { 0x06, 0x06, 0x06, 0x36, 0x36, 0x35, 0x36, 0x27, 0x29, 0x37, 0x37, 0x35,
                0x35 };
        int[] cards_of_player2 = new int[] { 0x06, 0x06, 0x06, 0x36, 0x36, 0x35, 0x36, 0x27, 0x29, 0x37, 0x37, 0x35,
                0x35 };

        for (int i = 0; i < this.getTablePlayerNumber(); i++) {
            for (int j = 0; j < GameConstants.MAX_INDEX; j++) {
                GRR._cards_index[i][j] = 0;
            }
        }

        for (int j = 0; j < 13; j++) {
            if (this.getTablePlayerNumber() == 4) {
                GRR._cards_index[0][_logic.switch_to_card_index(cards_of_player0[j])] += 1;
                GRR._cards_index[1][_logic.switch_to_card_index(cards_of_player0[j])] += 1;
                GRR._cards_index[2][_logic.switch_to_card_index(cards_of_player0[j])] += 1;
                GRR._cards_index[3][_logic.switch_to_card_index(cards_of_player0[j])] += 1;
            } else if (this.getTablePlayerNumber() == 3) {
                GRR._cards_index[0][_logic.switch_to_card_index(cards_of_player0[j])] += 1;
                GRR._cards_index[1][_logic.switch_to_card_index(cards_of_player1[j])] += 1;
                GRR._cards_index[2][_logic.switch_to_card_index(cards_of_player2[j])] += 1;
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
