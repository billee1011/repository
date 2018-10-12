package com.cai.game.mj.handler.yifeng;

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.Constants_HuaXian;
import com.cai.common.constant.game.mj.GameConstants_ND;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.Player;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.Room;
import com.cai.common.domain.WeaveItem;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.AutoHuPaiYiFengRunnable;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.game.mj.AbstractMJTable;
import com.cai.game.mj.FengKanUtil;
import com.cai.game.mj.MJType;
import com.cai.game.mj.jiangxi.yudu.GameConstants_YD;
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

public class Table_YiFeng extends AbstractMJTable {
    private static final long serialVersionUID = 2570006934151976528L;

	private int[] dispatchcardNum; // 摸牌次数
	private boolean isCanGenZhuang; // 可以跟庄
	private int genZhuangCount; // 跟庄次数
	private int genZhuangCard; // 跟庄牌
	private int next_seat_index;
	
    private int feng_kan = 0;
	public boolean bHaveAnGang = false;
	public static final int HAIDIPAI = 15;//海底牌

    protected HandlerPao_YiFeng _handler_pao;

    public Table_YiFeng() {
        super(MJType.GAME_TYPE_YIFENG);
    }
    
	public int getTablePlayerNumber() {
		if(this.getRuleValue(GameConstants.GAME_RULE_CAN_LESS)==1){
			if (playerNumber > 0) {
				return playerNumber;
			}
		}
		if(this.getRuleValue(GameConstants.GAME_RULE_YF_FOUR)==1)
			return 4;
		if(this.getRuleValue(GameConstants.GAME_RULE_YF_THREE) == 1)
			return 3;
		if(this.getRuleValue(GameConstants.GAME_RULE_YF_TWO) == 1)
			return 2;
		return GameConstants.GAME_PLAYER;
	}
	
	/**
	 * 参数初始化
	 */
	protected void onInitParam() {
		dispatchcardNum = new int[GameConstants.GAME_PLAYER];
		isCanGenZhuang = true;
		bHaveAnGang = false;
		genZhuangCount = 0;
        feng_kan = 0;
	}
	
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
		} else {
			if (genZhuangCard != card || seat_index != next_seat_index) {
				isCanGenZhuang = false;
			} else {
				next_seat_index = (seat_index + getTablePlayerNumber() + 1) % getTablePlayerNumber();
				if(next_seat_index == _cur_banker){
					setGenZhuangCount();
					operate_zhui_zhuang(time_for_tou_zi_animation, time_for_tou_zi_fade);
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
	 * 追庄的效果
	 * 
	 * @param time_for_animate
	 *            动画时间
	 * @param time_for_fade
	 *            动画保留时间
	 * @return
	 */
	public boolean operate_zhui_zhuang(int time_for_animate, int time_for_fade) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_EFFECT_ACTION);
		roomResponse.setEffectType(GameConstants.Effect_Action_Other);
		if (GRR != null)
			roomResponse.setTarget(GRR._banker_player);
		else
			roomResponse.setTarget(0);
		roomResponse.setEffectCount(1);
		roomResponse.addEffectsIndex(genZhuangCount);
		roomResponse.setEffectTime(time_for_animate);
		roomResponse.setStandTime(time_for_fade);

		send_response_to_room(roomResponse);

		if (GRR != null)
			GRR.add_room_response(roomResponse);

		return true;
	}

    @Override
    public int analyse_chi_hu_card(int[] cards_index, WeaveItem[] weaveItems, int weave_count, int cur_card,
            ChiHuRight chiHuRight, int card_type, int _seat_index) {
        if (!_logic.is_valid_card(cur_card)) {
            return GameConstants.WIK_NULL;
        }

		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}
		if (cur_card != GameConstants.INVALID_VALUE) {
			cbCardIndexTemp[_logic.switch_to_card_index(cur_card)]++;
		}
		
        int cbChiHuKind = GameConstants.WIK_NULL;

        //天胡、地胡
		if (isDispatchcardNum(_cur_banker)) {
			if (_seat_index == _cur_banker && card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
				chiHuRight.opr_or(GameConstants.CHR_TIAN_HU_YF);
			} else if(weave_count == 0 && _seat_index != _cur_banker && _out_card_player == GRR._banker_player 
					&& dispatchcardNum[_seat_index] == 0 && card_type != GameConstants_YD.CHR_GANG_SHANG_KAI_YF) {
				chiHuRight.opr_or(GameConstants.CHR_DI_HU_YF);
			}
		}
		
    	//海底捞月
    	if(card_type == Constants_HuaXian.HU_CARD_TYPE_ZI_MO && is_hai_di_lao()){
    		chiHuRight.opr_or(GameConstants.CHR_HAI_DI_LAO_YF);
    	}
    	
    	if (card_type == GameConstants.HU_CARD_TYPE_GANG_KAI) {
			chiHuRight.opr_or(GameConstants.CHR_GANG_SHANG_KAI_YF);
        }else if(card_type == GameConstants.HU_CARD_TYPE_QIANGGANG){
        	chiHuRight.opr_or(GameConstants.CHR_QIANG_GANG_HU_YF);
        }else if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
            chiHuRight.opr_or(GameConstants.CHR_ZI_MO_YF);
        } else if (card_type == GameConstants.HU_CARD_TYPE_PAOHU) {
            chiHuRight.opr_or(GameConstants.CHR_PNGG_HU_YF);
        }
		
        //十三烂判断
        boolean shisanlan = _logic.isShiSanYF(cbCardIndexTemp, weaveItems, weave_count);
        if (shisanlan) {
        	 if(_logic.isQXShiSanLanYF(cbCardIndexTemp, weaveItems, weave_count)){
        		 chiHuRight.opr_or(GameConstants.CHR_QIXING_SHI_SAN_LAN_YF);
        	 }
        	 else{
        		 chiHuRight.opr_or(GameConstants.CHR_SHI_SAN_LAN_YF);
        	 }
        	 cbChiHuKind = GameConstants.WIK_CHI_HU;
        	 
        	 return cbChiHuKind;
        }
        
        //七对
        long qi_xiao_dui = GameConstants.WIK_NULL;
        qi_xiao_dui = _logic.is_qi_xiao_dui_yifeng(cbCardIndexTemp, weaveItems, weave_count); 
        if(qi_xiao_dui != GameConstants.WIK_NULL){
        	//chiHuRight.opr_or(qi_xiao_dui);
        	cbChiHuKind = GameConstants.WIK_CHI_HU;
        	if(_logic.is_qing_yi_se(cbCardIndexTemp, weaveItems, weave_count, cur_card)){
        		if(qi_xiao_dui == GameConstants.CHR_QI_XIAO_DUI_YF){
        			chiHuRight.opr_or(GameConstants.CHR_QING_QI_YF);
        		}
        		else if(qi_xiao_dui == GameConstants.CHR_HAOHUA_QI_XIAO_YF){
        			chiHuRight.opr_or(GameConstants.CHR_QING_HH_QI_YF);
        		}
        		else if(qi_xiao_dui == GameConstants.CHR_SHUANGHAO_QI_XIAO_YF){
        			chiHuRight.opr_or(GameConstants.CHR_QING_THH_QI_YF);
        		}
        		else if(qi_xiao_dui == GameConstants.CHR_SANHAO_QI_XIAO_YF){
        			chiHuRight.opr_or(GameConstants.CHR_QING_SHH_QI_YF);
        		}
        	}
        	else if(_logic.is_zi_yi_se(cbCardIndexTemp, weaveItems, weave_count, cur_card)){
        		if(qi_xiao_dui == GameConstants.CHR_QI_XIAO_DUI_YF){
        			chiHuRight.opr_or(GameConstants.CHR_ZI_QI_YF);
        		}
        		else if(qi_xiao_dui == GameConstants.CHR_HAOHUA_QI_XIAO_YF){
        			chiHuRight.opr_or(GameConstants.CHR_ZI_HH_QI_YF);
        		}
        		else if(qi_xiao_dui == GameConstants.CHR_SHUANGHAO_QI_XIAO_YF){
        			chiHuRight.opr_or(GameConstants.CHR_ZI_THH_QI_YF);
        		}
        		else if(qi_xiao_dui == GameConstants.CHR_SANHAO_QI_XIAO_YF){
        			chiHuRight.opr_or(GameConstants.CHR_ZI_SHH_QI_YF);
        		}
        	}
        	else{
        		chiHuRight.opr_or(qi_xiao_dui);
        	}
        	
        	return cbChiHuKind;
        }
        
        
		//平胡
        int[] magic_cards_index = new int[GameUtilConstants.MAX_MAGIC_INDEX_COUNT];
        int magic_card_count = 0;
        boolean can_win = false;
        can_win = AnalyseCardUtil.analyse_feng_chi_by_cards_index(cards_index,_logic.switch_to_card_index(cur_card), magic_cards_index, magic_card_count);
        //大七对
        boolean exist_eat = exist_eat(weaveItems, weave_count);
		boolean is_da_qi_dui =  AnalyseCardUtil.analyse_peng_hu_by_cards_index(cards_index,
				_logic.switch_to_card_index(cur_card), new int[2], 0) && !exist_eat; // 硬碰碰胡
		if(is_da_qi_dui && can_win){
			cbChiHuKind = GameConstants.WIK_CHI_HU;
			if(_logic.is_qing_yi_se(cbCardIndexTemp, weaveItems, weave_count, cur_card) && is_dan_diao(_seat_index)){
				chiHuRight.opr_or(GameConstants.CHR_QING_DA_QI_DAN_YF);
			}
			else if(_logic.is_qing_yi_se(cbCardIndexTemp, weaveItems, weave_count, cur_card)){
				chiHuRight.opr_or(GameConstants.CHR_QING_DA_QI_YF);
			}
			else if(_logic.is_zi_yi_se(cbCardIndexTemp, weaveItems, weave_count, cur_card) && is_zi_dan_diao(_seat_index)){
				chiHuRight.opr_or(GameConstants.CHR_ZI_DA_QI_DAN_YF);
			}
			else if(_logic.is_zi_yi_se(cbCardIndexTemp, weaveItems, weave_count, cur_card)){
				chiHuRight.opr_or(GameConstants.CHR_ZI_DA_QI_YF);
			}
			else if(is_dan_diao(_seat_index)){
				chiHuRight.opr_or(GameConstants.CHR_DA_QI_DAN_YF);
			}
			else{
				chiHuRight.opr_or(GameConstants.CHR_DA_QI_DUI_YF);
			}
			  
			return cbChiHuKind;
		}
        
        //字一色
        if(_logic.is_zi_yi_se(cbCardIndexTemp, weaveItems, weave_count, cur_card)){
    		//chiHuRight.opr_or(GameConstants.CHR_ZI_YI_SE_YF);
    		cbChiHuKind = GameConstants.WIK_CHI_HU;
    		if(is_zi_dan_diao(_seat_index)){
        		chiHuRight.opr_or(GameConstants.CHR_Zi_DIAO_YF);
        	}
    		else{
    			chiHuRight.opr_or(GameConstants.CHR_ZI_YI_SE_YF);
        	}
    		return cbChiHuKind;
    	}
        
        //平胡
        if (can_win == true  ) {
        	cbChiHuKind = GameConstants.WIK_CHI_HU;
        	if(_logic.is_qing_yi_se(cbCardIndexTemp, weaveItems, weave_count, cur_card) && is_dan_diao(_seat_index)){
        		chiHuRight.opr_or(GameConstants.CHR_QING_DAN_YF);
        	}
        	else if(_logic.is_qing_yi_se(cbCardIndexTemp, weaveItems, weave_count, cur_card)){
        		chiHuRight.opr_or(GameConstants.CHR_QING_YI_SE_YF);
        	}
        	else if(is_dan_diao(_seat_index)){
        		chiHuRight.opr_or(GameConstants.CHR_DIAN_DIAO_YF);
        	}
        }else {
        	chiHuRight.set_empty();
        }

        return cbChiHuKind;
    }
    
    boolean is_dan_diao(int seat_index){
    	return _playerStatus[seat_index]._hu_card_count == 1;
    }
    
    boolean is_hai_di_lao(){
    	return GRR._left_card_count == 14;
    }
    
    boolean is_zi_dan_diao(int seat_index){
    	int cards[] = new int[GameConstants.MAX_COUNT];
        int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[seat_index], cards);
    	return hand_card_count == 2;
    }

    @Override
    public boolean estimate_gang_respond(int seat_index, int card) {
        boolean bAroseAction = false;

        PlayerStatus playerStatus = null;

        int action = GameConstants.WIK_NULL;

        for (int i = 0; i < getTablePlayerNumber(); i++) {
            if (seat_index == i)
                continue;

            playerStatus = _playerStatus[i];

            if (playerStatus.is_chi_hu_round()) {
                ChiHuRight chr = GRR._chi_hu_rights[i];
                chr.set_empty();

                action = analyse_chi_hu_card(GRR._cards_index[i], GRR._weave_items[i], GRR._weave_count[i], card, chr,
                		GameConstants.HU_CARD_TYPE_QIANGGANG, i);

                if (action != 0) {
                	GRR._chi_hu_rights[seat_index].opr_or(GameConstants.CHR_HUNAN_GANG_SHANG_PAO);
                    _playerStatus[i].add_action(GameConstants.WIK_CHI_HU);
                    _playerStatus[i].add_chi_hu(card, seat_index);
                    bAroseAction = true;
                }
            }
        }

        if (bAroseAction == true) {
            _provide_player = seat_index;
            _provide_card = card;
            _resume_player = _current_player;
            _current_player = GameConstants.INVALID_SEAT;
        }

        return bAroseAction;
    }

    public boolean estimate_player_out_card_respond(int seat_index, int card, int type) {
        boolean bAroseAction = false;

        for (int i = 0; i < getTablePlayerNumber(); i++) {
            _playerStatus[i].clean_action();
            _playerStatus[i].clean_weave();
        }

        PlayerStatus playerStatus = null;

        int action = GameConstants.WIK_NULL;

        int hu_count = 0;
        int hu_player[] = new int[4];
        for (int i = 0; i < getTablePlayerNumber(); i++) {
            if (seat_index == i) {
                continue;
            }

            playerStatus = _playerStatus[i];

            if (GRR._left_card_count > 0) {
                action = _logic.check_peng(GRR._cards_index[i], card);
                if (action != 0) {
                    playerStatus.add_action(action);
                    playerStatus.add_peng(card, seat_index);
                    bAroseAction = true;
                }

                if (GRR._left_card_count > 0) {
                    action = _logic.estimate_gang_card_out_card(GRR._cards_index[i], card);
                    if (action != 0) {
                        playerStatus.add_action(GameConstants.WIK_GANG);
                        playerStatus.add_gang(card, seat_index, 1);
                        bAroseAction = true;
                    }
                }
            }
            //有暗杆不能炮胡
            if (_playerStatus[i].is_chi_hu_round() && !bHaveAnGang) {
            	
                ChiHuRight chr = GRR._chi_hu_rights[i];
                chr.set_empty();
                int cbWeaveCount = GRR._weave_count[i];

                int card_type = Constants_HuaXian.HU_CARD_TYPE_JIE_PAO;

                action = analyse_chi_hu_card(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr,
                        card_type, i);

                if (action != 0) {
                    _playerStatus[i].add_action(GameConstants.WIK_CHI_HU);
                    _playerStatus[i].add_chi_hu(card, seat_index);
                    bAroseAction = true;
                    hu_player[hu_count] = i;
                    hu_count ++;
                }
            }
        }
        
        if (bAroseAction) {
            _resume_player = _current_player;
            _current_player = GameConstants.INVALID_SEAT;
            _provide_player = seat_index;
        }
        
        if(hu_count > 1){
        	 GameSchedule.put(
                     new AutoHuPaiYiFengRunnable(getRoom_id(), seat_index,hu_count, hu_player,card,hu_count),0, TimeUnit.SECONDS);
        }
        return bAroseAction;
    }
    
    //一炮多响自动胡牌
    public void runnable_auto_hu_pai(int seat_index, int hu_count, int hu_player[], int hu_card ){
    	for(int i = 0;i < hu_count;i++){
    		this._handler_out_card_operate.handler_operate_card(this,hu_player[i],GameConstants.WIK_CHI_HU,hu_card);
		}
    }

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
                    chr, Constants_HuaXian.HU_CARD_TYPE_ZI_MO, seat_index)) {
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

    @Override
    protected void onInitTable() {
        _handler_chi_peng = new HandlerChiPeng_YiFeng();
        _handler_dispath_card = new HandlerDispatchCard_YiFeng();
        _handler_gang = new HandlerGang_YiFeng();
        _handler_out_card_operate = new HandlerOutCardOperate_YiFeng();
        _handler_pao = new HandlerPao_YiFeng();
    }

    @Override
    protected boolean on_game_start() {
    	
    	set_handler(_handler_pao);
        _handler_pao.exe(this);
   
        return true;
    }

    protected boolean on_game_start_real() {
        onInitParam();
        
        show_tou_zi(GRR._banker_player);
        
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

    @Override
    protected boolean on_handler_game_finish(int seat_index, int reason) {
        int real_reason = reason;

        for (int i = 0; i < getTablePlayerNumber(); i++) {
            _player_ready[i] = 0;
        }

        RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
        roomResponse.setType(MsgConstants.RESPONSE_GAME_END);
        GameEndResponse.Builder game_end = GameEndResponse.newBuilder();

        roomResponse.setLeftCardCount(0);

        load_common_status(roomResponse);
        load_room_info_data(roomResponse);

        RoomInfo.Builder room_info = getRoomInfo();
        game_end.setRoomInfo(room_info);
        game_end.setRunPlayerId(_run_player_id);
        game_end.setRoundOverType(0);
        game_end.setGamePlayerNumber(getTablePlayerNumber());
        game_end.setEndTime(System.currentTimeMillis() / 1000L);
        
        setGameEndBasicPrama(game_end);
        
        if (GRR != null) {
            game_end.setRoundOverType(1);
            game_end.setStartTime(GRR._start_time);

            game_end.setGameTypeIndex(GRR._game_type_index);
            roomResponse.setLeftCardCount(GRR._left_card_count);

            for (int i = 0; i < GRR._especial_card_count; i++) {
                game_end.addEspecialShowCards(GRR._especial_show_cards[i]);
            }

            GRR._end_type = reason;

            float lGangScore[] = new float[getTablePlayerNumber()];

            for (int i = 0; i < getTablePlayerNumber(); i++) {
                // TODO: 荒庄荒杠
                //if (GRR._end_type != GameConstants.Game_End_DRAW) {
            	if (GRR._end_type == GameConstants.Game_End_NORMAL) {
                    for (int j = 0; j < GRR._gang_score[i].gang_count; j++) {
                        for (int k = 0; k < getTablePlayerNumber(); k++) {
                            lGangScore[k] += GRR._gang_score[i].scores[j][k];
                        }
                    }
                }

                for (int j = 0; j < getTablePlayerNumber(); j++) {
                    _player_result.lost_fan_shu[i][j] += GRR._lost_fan_shu[i][j];
                }
            }
            //跟庄分
            for(int i = 0; i < getTablePlayerNumber();i++){
            	if(i == this.GRR._banker_player)
            		continue;
            	GRR._game_score[i] += genZhuangCount;
            	GRR._game_score[GRR._banker_player] -= genZhuangCount;
            }

            for (int i = 0; i < getTablePlayerNumber(); i++) {
                GRR._game_score[i] += lGangScore[i];
                GRR._game_score[i] += GRR._start_hu_score[i];

                _player_result.game_score[i] += GRR._game_score[i];
            }

            load_player_info_data(roomResponse);

            game_end.setGameRound(_game_round);
            game_end.setCurRound(_cur_round);

            game_end.setCellScore(GameConstants.CELL_SCORE);

            game_end.setBankerPlayer(GRR._banker_player);
            game_end.setLeftCardCount(GRR._left_card_count);
            game_end.setShowBirdEffect(GRR._show_bird_effect == false ? 0 : 1);

            for (int i = 0; i < GameConstants.MAX_NIAO_CARD && i < GRR._count_niao; i++) {
                game_end.addCardsDataNiao(GRR._cards_data_niao[i]);
            }
            for (int i = 0; i < GameConstants.MAX_NIAO_CARD && i < GRR._count_niao_fei; i++) {
                game_end.addCardsDataNiao(GRR._cards_data_niao_fei[i]);
            }
            game_end.setCountPickNiao(GRR._count_pick_niao + GRR._count_pick_niao_fei);// 中鸟个数

            for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
                // 鸟牌，必须按四人计算
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
                Int32ArrayResponse.Builder hc = Int32ArrayResponse.newBuilder();
                for (int j = 0; j < GameConstants.MAX_COUNT; j++) {
                    hc.addItem(GRR._chi_hu_card[i][j]);
                }

                for (int h = 0; h < GRR._chi_hu_card[i].length; h++) {
                    game_end.addHuCardData(GRR._chi_hu_card[i][h]);
                }

                game_end.addHuCardArray(hc);
            }

            // 现在权值只有一位
            long rv[] = new long[GameConstants.MAX_RIGHT_COUNT];

            set_result_describe();

            for (int i = 0; i < getTablePlayerNumber(); i++) {
                GRR._card_count[i] = _logic.switch_to_cards_data(GRR._cards_index[i], GRR._cards_data[i]);

                Int32ArrayResponse.Builder cs = Int32ArrayResponse.newBuilder();
                for (int j = 0; j < GRR._card_count[i]; j++) {
                    cs.addItem(GRR._cards_data[i][j]);
                }
                game_end.addCardsData(cs);

                WeaveItemResponseArrayResponse.Builder weaveItem_array = WeaveItemResponseArrayResponse.newBuilder();
                for (int j = 0; j < GRR._weave_count[i]; j++) {
                    WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
                    weaveItem_item.setCenterCard(GRR._weave_items[i][j].center_card);
                    weaveItem_item.setProvidePlayer(GRR._weave_items[i][j].provide_player);
                    weaveItem_item.setPublicCard(GRR._weave_items[i][j].public_card);
                    weaveItem_item.setWeaveKind(GRR._weave_items[i][j].weave_kind);
                    weaveItem_array.addWeaveItem(weaveItem_item);
                }
                game_end.addWeaveItemArray(weaveItem_array);

                GRR._chi_hu_rights[i].get_right_data(rv);
                game_end.addChiHuRight(rv[0]);

                GRR._start_hu_right[i].get_right_data(rv);
                game_end.addStartHuRight(rv[0]);

                game_end.addProvidePlayer(GRR._provider[i]);
                game_end.addGameScore(GRR._game_score[i]);
                game_end.addGangScore(lGangScore[i]);
                game_end.addStartHuScore(GRR._start_hu_score[i]);
                game_end.addResultDes(GRR._result_des[i]);

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
            if (_cur_round >= _game_round && (!is_sys())) {
                end = true;
                game_end.setRoomOverType(1);
                game_end.setPlayerResult(process_player_result(reason));
            } else {
            }
        } else if ((!is_sys()) && (reason == GameConstants.Game_End_RELEASE_PLAY
                || reason == GameConstants.Game_End_RELEASE_NO_BEGIN || reason == GameConstants.Game_End_RELEASE_RESULT
                || reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT
                || reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT
                || reason == GameConstants.Game_End_RELEASE_SYSTEM)) {
            end = true;
            real_reason = GameConstants.Game_End_DRAW;
            game_end.setRoomOverType(1);
            game_end.setPlayerResult(process_player_result(reason));
        }
        game_end.setEndType(real_reason);

        roomResponse.setGameEnd(game_end);

        send_response_to_room(roomResponse);

        record_game_round(game_end);

        if (reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT
                || reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT) {
            for (int j = 0; j < getTablePlayerNumber(); j++) {
                Player player = get_players()[j];
                if (player == null)
                    continue;
                send_error_notify(j, 1, "游戏解散成功!");

            }
        }

        if (end && (!is_sys())) {
            PlayerServiceImpl.getInstance().delRoomId(getRoom_id());
        }

        if (!is_sys()) {
            GRR = null;
        }

        if (is_sys()) {
            clear_score_in_gold_room();
        }

        return false;
    }

    @Override
    public void process_chi_hu_player_operate(int seat_index, int operate_card, boolean rm) {
        ChiHuRight chr = GRR._chi_hu_rights[seat_index];
        int effect_count = chr.type_count;
        long effect_indexs[] = new long[effect_count];
        for (int i = 0; i < effect_count; i++) {
            effect_indexs[i] = chr.type_list[i];
        }

    	/*int effect_count = 1;
        long effect_indexs[] = new long[effect_count];
        effect_indexs[0] = get_hu_type(seat_index);*/
        operate_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_HU, effect_count, effect_indexs, 1,
                GameConstants.INVALID_SEAT);

        operate_player_cards(seat_index, 0, null, 0, null);

        if (rm) {
            GRR._cards_index[seat_index][_logic.switch_to_card_index(operate_card)]--;
        }

        int cards[] = new int[GameConstants.MAX_COUNT];
        int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[seat_index], cards);

        cards[hand_card_count] = operate_card + GameConstants.CARD_ESPECIAL_TYPE_HU;
        hand_card_count++;

        operate_show_card(seat_index, GameConstants.Show_Card_HU, hand_card_count, cards, GameConstants.INVALID_SEAT);

        return;
    }
    
    public long get_hu_type(int seat_index){
    	ChiHuRight chiHuRight = GRR._chi_hu_rights[seat_index];
		// 天胡
		if (!(chiHuRight.opr_and(GameConstants.CHR_TIAN_HU_YF)).is_empty()) {
			return GameConstants.CHR_TIAN_HU_YF;
		}
		// 地胡
		if (!(chiHuRight.opr_and(GameConstants.CHR_DI_HU_YF)).is_empty()) {
			return GameConstants.CHR_DI_HU_YF;
		}
		//字一色大七对单吊
		if(!(chiHuRight.opr_and(GameConstants.CHR_ZI_DA_QI_DAN_YF)).is_empty()){
			return GameConstants.CHR_ZI_DA_QI_DAN_YF;
		}
		//清一色大七对单吊
		if(!(chiHuRight.opr_and(GameConstants.CHR_QING_DA_QI_DAN_YF)).is_empty()){
			return GameConstants.CHR_QING_DA_QI_DAN_YF;
		}
		//字一色大七对
		if(!(chiHuRight.opr_and(GameConstants.CHR_ZI_DA_QI_YF)).is_empty()){
			return GameConstants.CHR_ZI_DA_QI_YF;
		}
		// 清一色大七对
		if(!(chiHuRight.opr_and(GameConstants.CHR_QING_DA_QI_YF)).is_empty()){
			return GameConstants.CHR_QING_DA_QI_YF;
		}
		// 七星十三烂
		if(!(chiHuRight.opr_and(GameConstants_YD.CHR_QIXING_SHI_SAN_LAN_YF)).is_empty()){
			return GameConstants.CHR_QIXING_SHI_SAN_LAN_YF;
		}
		//十三烂
		if(!(chiHuRight.opr_and(GameConstants_YD.CHR_SHI_SAN_LAN_YF)).is_empty()){
			return GameConstants.CHR_SHI_SAN_LAN_YF;
		}
		//清一色三豪华小七对
		if(!(chiHuRight.opr_and(GameConstants.CHR_QING_SHH_QI_YF)).is_empty()){
			return GameConstants.CHR_QING_SHH_QI_YF;
		}
		//清一色双豪华小七对
		if(!(chiHuRight.opr_and(GameConstants.CHR_QING_THH_QI_YF)).is_empty()){
			return GameConstants.CHR_QING_THH_QI_YF;
		}
		//清一色豪华小七对
		if(!(chiHuRight.opr_and(GameConstants.CHR_QING_HH_QI_YF)).is_empty()){
			return GameConstants.CHR_QING_HH_QI_YF;
		}
		//清一色小七对
		if(!(chiHuRight.opr_and(GameConstants.CHR_QING_QI_YF)).is_empty()){
			return GameConstants.CHR_QING_QI_YF;
		}
		// 字一色三豪华小七对
		if(!(chiHuRight.opr_and(GameConstants.CHR_ZI_SHH_QI_YF)).is_empty()){
			return GameConstants.CHR_ZI_SHH_QI_YF;
		}
		// 字一色双豪华小七对
		if(!(chiHuRight.opr_and(GameConstants.CHR_ZI_THH_QI_YF)).is_empty()){
			return GameConstants.CHR_ZI_THH_QI_YF;
		}
		// 字一色豪华小七对
		if(!(chiHuRight.opr_and(GameConstants.CHR_ZI_HH_QI_YF)).is_empty()){
			return GameConstants.CHR_ZI_HH_QI_YF;
		}
		//三豪华小七对
		if(!(chiHuRight.opr_and(GameConstants.CHR_SANHAO_QI_XIAO_YF)).is_empty()){
			return GameConstants.CHR_SANHAO_QI_XIAO_YF;
		}
		// 双豪华七小对
		if(!(chiHuRight.opr_and(GameConstants.CHR_SHUANGHAO_QI_XIAO_YF)).is_empty()){
			return GameConstants.CHR_SHUANGHAO_QI_XIAO_YF;
		}
		// 豪华七小对
		if(!(chiHuRight.opr_and(GameConstants.CHR_HAOHUA_QI_XIAO_YF)).is_empty()){
			return GameConstants.CHR_HAOHUA_QI_XIAO_YF;
		}
		// 七小对
		if(!(chiHuRight.opr_and(GameConstants.CHR_QI_XIAO_DUI_YF)).is_empty()){
			return GameConstants.CHR_QI_XIAO_DUI_YF;
		}
		//大七对单吊
		if(!(chiHuRight.opr_and(GameConstants.CHR_DA_QI_DAN_YF)).is_empty()){
			return GameConstants.CHR_DA_QI_DAN_YF;
		}
		//大七对
		if(!(chiHuRight.opr_and(GameConstants.CHR_DA_QI_DUI_YF)).is_empty()){
			return GameConstants.CHR_DA_QI_DUI_YF;
		}
		//清一色
		if(!(chiHuRight.opr_and(GameConstants.CHR_QING_YI_SE_YF)).is_empty()){
			return GameConstants.CHR_QING_YI_SE_YF;
		}
		//字一色
		if(!(chiHuRight.opr_and(GameConstants.CHR_ZI_YI_SE_YF)).is_empty()){
			return GameConstants.CHR_ZI_YI_SE_YF;
		}
		// 杠开
		if (!(chiHuRight.opr_and(GameConstants.CHR_GANG_SHANG_KAI_YF)).is_empty()) {
			return GameConstants.CHR_GANG_SHANG_KAI_YF;
		}
		//抢杠胡
		if(!(chiHuRight.opr_and(GameConstants.CHR_QIANG_GANG_HU_YF)).is_empty()){
			return GameConstants.CHR_QIANG_GANG_HU_YF;
		}
		//海底捞
		if (!(chiHuRight.opr_and(GameConstants.CHR_HAI_DI_LAO_YF)).is_empty()) {
			return GameConstants.CHR_HAI_DI_LAO_YF;
		}
		//单吊
		if(!(chiHuRight.opr_and(GameConstants.CHR_DIAN_DIAO_YF)).is_empty()){
			return GameConstants.CHR_DIAN_DIAO_YF;
		}
    	return 0;
    }
    

    @Override
    public void process_chi_hu_player_score(int seat_index, int provide_index, int operate_card, boolean zimo) {
        GRR._chi_hu_card[seat_index][0] = operate_card;

        GRR._win_order[seat_index] = 1;

        ChiHuRight chr = GRR._chi_hu_rights[seat_index];

        countCardType(chr, seat_index);

        int di_fen = get_di_fen_ex(provide_index, seat_index);

        int lChiHuScore = di_fen * GameConstants.CELL_SCORE;

        if (zimo) {
            for (int i = 0; i < getTablePlayerNumber(); i++) {
                if (i == seat_index)
                    continue;

                GRR._lost_fan_shu[i][seat_index] = di_fen;
            }
        } else {
            GRR._lost_fan_shu[provide_index][seat_index] = di_fen;
        }

        // TODO: 总分=（基础牌型分*另加牌型+杠分）*上火+杠分+飘
        if (zimo) {
            for (int i = 0; i < getTablePlayerNumber(); i++) {
                if (i == seat_index)
                    continue;

                int s = lChiHuScore;
                
                //对火
                if(this.has_rule(GameConstants.GAME_RULE_GUI_HUO)){
                	if(this._player_result.qiang[seat_index] == 1){
                		s *= 2;
                	}
                	if(this._player_result.qiang[i] == 1){
                		s *= 2;
                	}
                }
                //不对火
                if(this.has_rule(GameConstants.GAME_RULE_BU_DUI_HUO)){
                	if(this._player_result.qiang[seat_index] == 1 || this._player_result.qiang[i] == 1){
                		s *= 2;
                	}
                }
                //对飘
                if(this.has_rule(GameConstants.GAME_RULE_DUI_PIAO)){
                	int ipiao = this._player_result.pao[seat_index] + this._player_result.pao[i];
                	s += ipiao;
                }
                //不对飘
                if(this.has_rule(GameConstants.GAME_RULE_BU_DUI_PIAO)){
                	int ipiao = this._player_result.pao[seat_index] > this._player_result.pao[i] ? this._player_result.pao[seat_index] : this._player_result.pao[i];
                	s += ipiao;
                }
                GRR._game_score[i] -= s;
                GRR._game_score[seat_index] += s;
            }
        } else {
        	
            int s = lChiHuScore;
            //对火
            if(this.has_rule(GameConstants.GAME_RULE_GUI_HUO)){
            	if(this._player_result.qiang[seat_index] == 1){
            		s *= 2;
            	}
            	if(this._player_result.qiang[provide_index] == 1){
            		s *= 2;
            	}
            }
            //不对火
            if(this.has_rule(GameConstants.GAME_RULE_BU_DUI_HUO)){
            	if(this._player_result.qiang[seat_index] == 1 || this._player_result.qiang[provide_index] == 1){
            		s *= 2;
            	}
            }
            //对飘
            if(this.has_rule(GameConstants.GAME_RULE_DUI_PIAO)){
            	int ipiao = this._player_result.pao[seat_index] + this._player_result.pao[provide_index];
            	s += ipiao;
            }
            //不对飘
            if(this.has_rule(GameConstants.GAME_RULE_BU_DUI_PIAO)){
            	int ipiao = this._player_result.pao[seat_index] > this._player_result.pao[provide_index] ? this._player_result.pao[seat_index] : this._player_result.pao[provide_index];
            	s += ipiao;
            }
            
            GRR._game_score[provide_index] -= s;
            GRR._game_score[seat_index] += s;

            //if (!GRR._chi_hu_rights[seat_index].opr_and(GameConstants.CHR_SHU_FAN).is_empty())
            GRR._chi_hu_rights[provide_index].opr_or(Constants_HuaXian.CHR_FANG_PAO);
        }
    }

    private int get_di_fen( int provide_index, int seat_index) {
    	
    	ChiHuRight chiHuRight = GRR._chi_hu_rights[seat_index];
    	ChiHuRight chiprovideHuRight = GRR._chi_hu_rights[provide_index];
    	// 胡牌底分
		int wFanShu = 1;

		boolean isTianHu = false;
		boolean isDiHu = false;
		// 天胡
		if (!(chiHuRight.opr_and(GameConstants.CHR_TIAN_HU_YF)).is_empty()) {
			wFanShu = 10;
			isTianHu = true;
		}

		// 地胡
		if (!(chiHuRight.opr_and(GameConstants.CHR_DI_HU_YF)).is_empty()) {
			wFanShu = 10;
			isDiHu = true;
		}

		//杠上炮
		if(!(chiprovideHuRight.opr_and(GameConstants.CHR_GANG_SHANG_PAO_YF)).is_empty()){
			wFanShu *= 2;
		}
		//海底捞
		if (!(chiHuRight.opr_and(GameConstants.CHR_HAI_DI_LAO_YF)).is_empty()) {
			wFanShu *= 2;
		}
		// 杠开
		if (!(chiHuRight.opr_and(GameConstants.CHR_GANG_SHANG_KAI_YF)).is_empty()) {
			wFanShu *= 2;
		}
		//抢杠胡
		if(!(chiHuRight.opr_and(GameConstants.CHR_QIANG_GANG_HU_YF)).is_empty()){
			wFanShu *= 2;
		}
		//单吊
		if(!(chiHuRight.opr_and(GameConstants.CHR_DIAN_DIAO_YF)).is_empty()){
			wFanShu *= 2;
		}
		//大七对
		if(!(chiHuRight.opr_and(GameConstants.CHR_DA_QI_DUI_YF)).is_empty()){
			wFanShu *= 3;
		}
		//大七对单吊
		if(!(chiHuRight.opr_and(GameConstants.CHR_DA_QI_DAN_YF)).is_empty()){
			wFanShu *= 6;
		}
		//清一色
		if(!(chiHuRight.opr_and(GameConstants.CHR_QING_YI_SE_YF)).is_empty()){
			wFanShu *= 5;
		}
		//字一色
		if(!(chiHuRight.opr_and(GameConstants.CHR_ZI_YI_SE_YF)).is_empty()){
			wFanShu *= 5;
		}
		//十三烂
		if(!(chiHuRight.opr_and(GameConstants_YD.CHR_SHI_SAN_LAN_YF)).is_empty()){
			wFanShu *= 2;
		}else if(!(chiHuRight.opr_and(GameConstants_YD.CHR_QIXING_SHI_SAN_LAN_YF)).is_empty()){
			wFanShu *= 4;
		}
		
		// 七对
		if (!(chiHuRight.opr_and(GameConstants.CHR_QI_XIAO_DUI_YF)).is_empty()) {
			wFanShu *= 5;
		}else if (!(chiHuRight.opr_and(GameConstants.CHR_HAOHUA_QI_XIAO_YF)).is_empty()) {
			wFanShu *= 10;
		}else if(!(chiHuRight.opr_and(GameConstants.CHR_SHUANGHAO_QI_XIAO_YF)).is_empty()){
			wFanShu *= 20;
		}else if(!(chiHuRight.opr_and(GameConstants.CHR_SANHAO_QI_XIAO_YF)).is_empty()){
			wFanShu *= 30;
		}
		
		//组合牌型
		if(!(chiHuRight.opr_and(GameConstants.CHR_QING_DAN_YF)).is_empty()){
			wFanShu *= 10;
		}
		
		if(!(chiHuRight.opr_and(GameConstants.CHR_QING_QI_YF)).is_empty()){
			wFanShu *= 25;
		}
	
		if(!(chiHuRight.opr_and(GameConstants.CHR_QING_HH_QI_YF)).is_empty()){
			wFanShu *= 50;
		}
		if(!(chiHuRight.opr_and(GameConstants.CHR_QING_THH_QI_YF)).is_empty()){
			wFanShu *= 100;
		}
		if(!(chiHuRight.opr_and(GameConstants.CHR_QING_SHH_QI_YF)).is_empty()){
			wFanShu *= 150;
		}
		if(!(chiHuRight.opr_and(GameConstants.CHR_QING_DA_QI_YF)).is_empty()){
			wFanShu *= 15;
		}
		if(!(chiHuRight.opr_and(GameConstants.CHR_QING_DA_QI_DAN_YF)).is_empty() && !(chiHuRight.opr_and(GameConstants.CHR_DI_HU_YF)).is_empty()){
			wFanShu *= 30;
		}
		
		if(!(chiHuRight.opr_and(GameConstants.CHR_ZI_QI_YF)).is_empty()){
			wFanShu *= 25;
		}
		if(!(chiHuRight.opr_and(GameConstants.CHR_ZI_HH_QI_YF)).is_empty()){
			wFanShu *= 50;
		}
		if(!(chiHuRight.opr_and(GameConstants.CHR_ZI_THH_QI_YF)).is_empty()){
			wFanShu *= 100;
		}
		if(!(chiHuRight.opr_and(GameConstants.CHR_ZI_SHH_QI_YF)).is_empty()){
			wFanShu *= 150;
		}
		if(!(chiHuRight.opr_and(GameConstants.CHR_ZI_DA_QI_YF)).is_empty()&& !(chiHuRight.opr_and(GameConstants.CHR_TIAN_HU_YF)).is_empty()){
			wFanShu *= 15;
		}
		if(!(chiHuRight.opr_and(GameConstants.CHR_ZI_DA_QI_DAN_YF)).is_empty() && !(chiHuRight.opr_and(GameConstants.CHR_PING_HU_YF)).is_empty()){
			wFanShu *= 30;
		}
		
		//天胡地胡
		if(isTianHu || isDiHu){
			int iTemp = wFanShu/10;
			if(iTemp > 10){
				wFanShu = iTemp;
			}else{
				wFanShu = 10;
			}
		}
		return wFanShu;
    	
    }
    
    private int get_di_fen_ex( int provide_index, int seat_index) {
    	
    	// 胡牌底分
		int wFanShu = 1;
		long type = 0;
		long chrTypes = 0;

		boolean isTianHu = false;
		boolean isDiHu = false;

        chrTypes = GRR._chi_hu_rights[seat_index].type_count;

        for (int typeIndex = 0; typeIndex < chrTypes; typeIndex++) {
            type = GRR._chi_hu_rights[seat_index].type_list[typeIndex];

            if (GRR._chi_hu_rights[seat_index].is_valid()) {
				if (type == GameConstants.CHR_TIAN_HU_YF) {
					isTianHu = true;
					wFanShu = 10;
					//result.append(" 天胡");
				}
				if (type == GameConstants.CHR_DI_HU_YF) {
					isDiHu = true;
					wFanShu = 10;
					//result.append(" 地胡");
				}
                if (type == GameConstants.CHR_QING_YI_SE_YF) {
                	wFanShu *= 5;
                	//result.append(" 清一色");
				}
                if (type == GameConstants.CHR_ZI_YI_SE_YF) {
                	wFanShu *= 5;
                	//result.append(" 字一色");
				}
                if (type == GameConstants.CHR_QI_XIAO_DUI_YF) {
                	wFanShu *= 5;
                	//result.append(" 七小对");
				}
                if (type == GameConstants.CHR_HAOHUA_QI_XIAO_YF) {
                	wFanShu *= 10;
                	//result.append(" 豪华七小对");
				}
                if (type == GameConstants.CHR_SHUANGHAO_QI_XIAO_YF) {
                	wFanShu *= 20;
                	//result.append(" 双豪华七小对");
				}
                if (type == GameConstants.CHR_SANHAO_QI_XIAO_YF) {
                	wFanShu *= 30;
                	//result.append(" 三豪华七小对");
				}
                if (type == GameConstants.CHR_DA_QI_DUI_YF) {
                	wFanShu *= 3;
                	//result.append(" 大七对");
				}
                if (type == GameConstants.CHR_HAI_DI_LAO_YF) {
                	wFanShu *= 2;
                	//result.append(" 海底捞");
				}
                if (type == GameConstants.CHR_SHI_SAN_LAN_YF) {
                	wFanShu *= 2;
                	//result.append(" 十三烂");
				}
                if (type == GameConstants.CHR_QIXING_SHI_SAN_LAN_YF) {
                	wFanShu *= 4;
                	//result.append(" 七星十三烂");
				}
                if (type == GameConstants.CHR_QIANG_GANG_HU_YF) {
                	wFanShu *= 2;
                	//result.append(" 抢杠胡");
				}
                if (type == GameConstants.CHR_GANG_SHANG_KAI_YF) {
                	wFanShu *= 2;
                	//result.append(" 杠上开");
				}
                if (type == GameConstants.CHR_GANG_SHANG_PAO_YF) {
                	wFanShu *= 2;
                	//result.append(" 杠上炮");
				}
                if(type == GameConstants.CHR_DIAN_DIAO_YF){
                	wFanShu *= 2;
                	//result.append(" 平胡单吊");
                }
                if(type == GameConstants.CHR_DA_QI_DAN_YF){
                	wFanShu *= 6;
                	//result.append(" 大七对单吊");
                }
                if(type == GameConstants.CHR_QING_DAN_YF){
                	wFanShu *= 10;
                	//result.append(" 清一色单吊");
                }
                if(type == GameConstants.CHR_QING_QI_YF){
                	wFanShu *= 25;
                	//result.append(" 清一色七小对");
                }
                if(type == GameConstants.CHR_QING_HH_QI_YF){
                	wFanShu *= 50;
                	//result.append(" 清一色豪华七小对");
                }
                if(type == GameConstants.CHR_QING_THH_QI_YF){
                	wFanShu *= 100;
                	//result.append(" 清一色双豪华七小对");
                }
                if(type == GameConstants.CHR_QING_SHH_QI_YF){
                	wFanShu *= 150;
                	//result.append(" 清一色三豪华七小对");
                }
                if(type == GameConstants.CHR_QING_DA_QI_YF){
                	wFanShu *= 15;
                	//result.append(" 清一色大七小对");
                }
                if(type == GameConstants.CHR_ZI_QI_YF){
                	wFanShu *= 25;
                	//result.append(" 字一色大七小对");
                }
                if(type == GameConstants.CHR_ZI_HH_QI_YF){
                	wFanShu *= 50;
                	//result.append(" 字一色豪华七小对");
                }
                if(type == GameConstants.CHR_ZI_THH_QI_YF){
                	wFanShu *= 100;
                	//result.append(" 字一色双豪华七小对");
                }
                if(type == GameConstants.CHR_ZI_SHH_QI_YF){
                	wFanShu *= 150;
                	//result.append(" 字一色三豪华七小对");
                }
                if(type == GameConstants.CHR_ZI_DA_QI_YF){
                	wFanShu *= 15;
                	//result.append(" 字一色大七对");
                }
                if(type == GameConstants.CHR_QING_DA_QI_DAN_YF){
                	wFanShu *= 30;
                	//result.append(" 清一色大七对单吊");
                }
                if(type == GameConstants.CHR_ZI_DA_QI_DAN_YF){
                	wFanShu *= 30;
                	//result.append(" 字一色大七对单吊");
                }
            } 
        }
            
		//天胡地胡
		if(isTianHu || isDiHu){
			int iTemp = wFanShu/10;
			if(iTemp > 10){
				wFanShu = iTemp;
			}else{
				wFanShu = 10;
			}
		}
		return wFanShu;
    	
    }


    @Override
    public boolean operate_player_cards_with_ting(int seat_index, int card_count, int cards[], int weave_count,
            WeaveItem weaveitems[]) {
        RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
        roomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYER_CARDS);
        roomResponse.setGameStatus(_game_status);
        roomResponse.setTarget(seat_index);
        roomResponse.setCardType(1);

        this.load_common_status(roomResponse);

        roomResponse.setCardCount(card_count);
        roomResponse.setWeaveCount(weave_count);
       
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

        send_response_to_other(seat_index, roomResponse);

        for (int j = 0; j < card_count; j++) {
            roomResponse.addCardData(cards[j]);
        }

        int ting_count = _playerStatus[seat_index]._hu_out_card_count;

        roomResponse.setOutCardCount(ting_count);

        for (int i = 0; i < ting_count; i++) {
            int ting_card_cout = _playerStatus[seat_index]._hu_out_card_ting_count[i];
            roomResponse.addOutCardTingCount(ting_card_cout);
            
            roomResponse.addOutCardTing(
                    _playerStatus[seat_index]._hu_out_card_ting[i] + GameConstants.CARD_ESPECIAL_TYPE_TING);

            Int32ArrayResponse.Builder int_array = Int32ArrayResponse.newBuilder();
            for (int j = 0; j < ting_card_cout; j++) {
                int_array.addItem(_playerStatus[seat_index]._hu_out_cards[i][j]);
            }
            roomResponse.addOutCardTingCards(int_array);
        }

        this.send_response_to_player(seat_index, roomResponse);

        GRR.add_room_response(roomResponse);

        return true;
    }

    @Override
    protected void set_result_describe() {
        int chrTypes;
        long type = 0;

        int[] jie_gang = new int[getTablePlayerNumber()];
        int[] fang_gang = new int[getTablePlayerNumber()];
        int[] an_gang = new int[getTablePlayerNumber()];
        int[] ming_gang = new int[getTablePlayerNumber()];
        // TODO: 荒庄荒杠
        //if (GRR._end_type != GameConstants.Game_End_DRAW) {
        if (GRR._end_type == GameConstants.Game_End_NORMAL) {
            for (int player = 0; player < getTablePlayerNumber(); player++) {
                if (GRR != null) {
                    for (int w = 0; w < GRR._weave_count[player]; w++) {
                        if (GRR._weave_items[player][w].weave_kind != GameConstants.WIK_GANG) {
                            continue;
                        }

                        if (GRR._weave_items[player][w].type == GameConstants.GANG_TYPE_AN_GANG) {
                            an_gang[player]++;
                        } 
                        else if(GRR._weave_items[player][w].type == GameConstants.GANG_TYPE_ADD_GANG){
                        	ming_gang[player]++;
                            
                        }
                        else if(GRR._weave_items[player][w].type == GameConstants.GANG_TYPE_JIE_GANG){
                        	jie_gang[player]++;
                            fang_gang[GRR._weave_items[player][w].provide_player]++;
                        }
                    }
                }
            }
        }

        for (int player = 0; player < getTablePlayerNumber(); player++) {
            StringBuilder result = new StringBuilder("");

            chrTypes = GRR._chi_hu_rights[player].type_count;

            for (int typeIndex = 0; typeIndex < chrTypes; typeIndex++) {
                type = GRR._chi_hu_rights[player].type_list[typeIndex];

                if (GRR._chi_hu_rights[player].is_valid()) {
                    if (type == GameConstants.CHR_ZI_MO_YF)
                        result.append(" 自摸");
                    if (type == GameConstants.CHR_PNGG_HU_YF)
                        result.append(" 接炮");
					if (type == GameConstants.CHR_TIAN_HU_YF) {
						result.append(" 天胡");
					}
					if (type == GameConstants.CHR_DI_HU_YF) {
						result.append(" 地胡");
					}
                    if (type == GameConstants.CHR_QING_YI_SE_YF) {
                    	result.append(" 清一色");
					}
                    if (type == GameConstants.CHR_ZI_YI_SE_YF) {
                    	result.append(" 字一色");
					}
                    if (type == GameConstants.CHR_QI_XIAO_DUI_YF) {
                    	result.append(" 小七对");
					}
                    if (type == GameConstants.CHR_HAOHUA_QI_XIAO_YF) {
                    	result.append(" 豪华小七对");
					}
                    if (type == GameConstants.CHR_SHUANGHAO_QI_XIAO_YF) {
                    	result.append(" 双豪华小七对");
					}
                    if (type == GameConstants.CHR_SANHAO_QI_XIAO_YF) {
                    	result.append(" 三豪华小七对");
					}
                    if (type == GameConstants.CHR_DA_QI_DUI_YF) {
                    	result.append(" 大七对");
					}
                    if (type == GameConstants.CHR_HAI_DI_LAO_YF) {
                    	result.append(" 海底捞月");
					}
                    if (type == GameConstants.CHR_SHI_SAN_LAN_YF) {
                    	result.append(" 十三烂");
					}
                    if (type == GameConstants.CHR_QIXING_SHI_SAN_LAN_YF) {
                    	result.append(" 七星十三烂");
					}
                    if (type == GameConstants.CHR_QIANG_GANG_HU_YF) {
                    	result.append(" 抢杠胡");
					}
                    if (type == GameConstants.CHR_GANG_SHANG_KAI_YF) {
                    	result.append(" 杠开");
					}
                    if (type == GameConstants.CHR_GANG_SHANG_PAO_YF) {
                    	result.append(" 杠上炮");
					}
                    if(type == GameConstants.CHR_DIAN_DIAO_YF){
                    	result.append(" 平胡单吊");
                    }
                    if(type == GameConstants.CHR_DA_QI_DAN_YF){
                    	result.append(" 大七对单吊");
                    }
                    if(type == GameConstants.CHR_QING_DAN_YF){
                    	result.append(" 清一色单吊");
                    }
                    if(type == GameConstants.CHR_QING_QI_YF){
                    	result.append(" 清一色七小对");
                    }
                    if(type == GameConstants.CHR_QING_HH_QI_YF){
                    	result.append(" 清一色豪华七小对");
                    }
                    if(type == GameConstants.CHR_QING_THH_QI_YF){
                    	result.append(" 清一色双豪华七小对");
                    }
                    if(type == GameConstants.CHR_QING_SHH_QI_YF){
                    	result.append(" 清一色三豪华七小对");
                    }
                    if(type == GameConstants.CHR_QING_DA_QI_YF){
                    	result.append(" 清一色大七小对");
                    }
                    if(type == GameConstants.CHR_ZI_QI_YF){
                    	result.append(" 字一色大七小对");
                    }
                    if(type == GameConstants.CHR_ZI_HH_QI_YF){
                    	result.append(" 字一色豪华七小对");
                    }
                    if(type == GameConstants.CHR_ZI_THH_QI_YF){
                    	result.append(" 字一色双豪华七小对");
                    }
                    if(type == GameConstants.CHR_ZI_SHH_QI_YF){
                    	result.append(" 字一色三豪华七小对");
                    }
                    if(type == GameConstants.CHR_ZI_DA_QI_YF){
                    	result.append(" 字一色大七对");
                    }
                    if(type == GameConstants.CHR_QING_DA_QI_DAN_YF){
                    	result.append(" 清一色大七对单吊");
                    }
                    if(type == GameConstants.CHR_ZI_DA_QI_DAN_YF){
                    	result.append(" 字一色大七对单吊");
                    }
                } else if (type == Constants_HuaXian.CHR_FANG_PAO) {
                    result.append(" 放炮");
                }
            }
            
            //if (_player_result.pao[player] > 0)
            //    result.append(" 飘" + _player_result.pao[player]);
            
            //if (_player_result.qiang[player] > 0)
            //    result.append(" 上火");
            
			if (GRR._banker_player == player && genZhuangCount > 0) {
				result.append(" 追庄X" + genZhuangCount);
			}

            // TODO: 荒庄荒杠
            //if (GRR._end_type != GameConstants.Game_End_DRAW) {
			if (GRR._end_type == GameConstants.Game_End_NORMAL) {
                if (an_gang[player] > 0) {
                    result.append(" 暗杠X" + an_gang[player]);
                }
                if (ming_gang[player] > 0) {
                    result.append(" 弯杠X" + ming_gang[player]);
                }
                if (fang_gang[player] > 0) {
                    result.append(" 放杠X" + fang_gang[player]);
                }
                if (jie_gang[player] > 0) {
                    result.append(" 接杠X" + jie_gang[player]);
                }
            }

            int total_an_gang_score = 0;
            for (int i = 0; i < getTablePlayerNumber(); i++) {
                if (i == player)
                    continue;
                total_an_gang_score += an_gang[i] * 2;
            }

            //int gang_fen = an_gang[player] * 6 + jie_gang[player] * 3 - fang_gang[player] * 3 - total_an_gang_score;
            //result.append(" 杠分 " + gang_fen);

            GRR._result_des[player] = result.toString();
        }
    }

    @Override
    public boolean handler_requst_pao_qiang(Player player, int pao, int qiang) {
        return _handler_pao.handler_pao_qiang(this, player.get_seat_index(), pao, qiang);
    }

    @Override
    public boolean trustee_timer(int operate_id, int seat_index) {
        return true;
    }

    @Override
    public void test_cards() {
        int[] cards_of_player0 = new int[] { 0x01, 0x02, 0x03, 0x11, 0x11, 0x11, 0x22, 0x22, 0x22, 0x35, 0x35, 0x35,
        		0x07 };
        int[] cards_of_player1 = new int[] { 0x01, 0x02, 0x03, 0x17, 0x02, 0x08, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36,
        		0x07 };
        int[] cards_of_player3 = new int[] { 0x01, 0x02, 0x03, 0x22, 0x33, 0x23, 0x34, 0x24, 0x15, 0x15, 0x36, 0x16,
        		0x07 };
        int[] cards_of_player2 = new int[] { 0x01, 0x02, 0x03, 0x22, 0x33, 0x23, 0x34, 0x24, 0x15, 0x15, 0x36, 0x16,
        		0x07 };

        for (int i = 0; i < getTablePlayerNumber(); i++) {
            for (int j = 0; j < GameConstants.MAX_INDEX; j++) {
                GRR._cards_index[i][j] = 0;
            }
        }

        for (int j = 0; j < 13; j++) {
            if (getTablePlayerNumber() == 4) {
                GRR._cards_index[0][_logic.switch_to_card_index(cards_of_player0[j])] += 1;
                GRR._cards_index[1][_logic.switch_to_card_index(cards_of_player1[j])] += 1;
                GRR._cards_index[2][_logic.switch_to_card_index(cards_of_player2[j])] += 1;
                GRR._cards_index[3][_logic.switch_to_card_index(cards_of_player3[j])] += 1;
            } else if (getTablePlayerNumber() == 3) {
                GRR._cards_index[0][_logic.switch_to_card_index(cards_of_player0[j])] += 1;
                GRR._cards_index[1][_logic.switch_to_card_index(cards_of_player1[j])] += 1;
                GRR._cards_index[2][_logic.switch_to_card_index(cards_of_player2[j])] += 1;
            }else if (getTablePlayerNumber() == 2) {
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

}
