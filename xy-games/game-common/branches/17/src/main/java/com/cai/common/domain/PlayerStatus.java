package com.cai.common.domain;

import com.cai.common.constant.MJGameConstants;

public class PlayerStatus {

	private int _status; //状态
	
	public int _action[];
	
	public int _action_count;
	
	public int _action_cards[];
	
	public int _perfrom_action;
	
	public int _target;
	
	public boolean _response;
	
	public WeaveItem _action_weaves[];
	
	public int _weave_count;
	
	//public ChiHuRight _chiHuRight;
	
	public int _ready;
	
	public int _operate_card;
	
	//public int _provide_player;
	//public int _provide_card;
	
	public int _gang_card_data;
	
	
	public boolean _chi_hu_round;
	
	public int _card_status;//牌型状态
	
	public int _hu_card_count;
	
	public int _hu_cards[];
	
	public boolean ting_check;
	
	public PlayerStatus(){
		_status =MJGameConstants.INVALID_VALUE;
		_action = new int[MJGameConstants.MAX_COUNT];
		
		_action_cards = new int[MJGameConstants.MAX_COUNT];
		_perfrom_action = MJGameConstants.INVALID_VALUE;
		_response=false;
		_action_count =MJGameConstants.INVALID_VALUE;
		_target=MJGameConstants.INVALID_SEAT;
		
		_action_weaves = new WeaveItem[MJGameConstants.MAX_COUNT];
		for(int i=0; i < MJGameConstants.MAX_COUNT;i++){
			_action_weaves[i] = new WeaveItem();
		}
		_weave_count=0;
		_action_count=0;
		
		_ready = 0;
		
		
		_operate_card = MJGameConstants.INVALID_VALUE;
		
		//_chiHuRight = new ChiHuRight();
		
		_chi_hu_round = true;
		
		_card_status = MJGameConstants.CARD_STATUS_NORMAL;
		
		
		_hu_cards = new int[MJGameConstants.MAX_INDEX];
		
		ting_check= false;
	}
	
	
	public void reset(){
		_status =MJGameConstants.INVALID_VALUE;
		
		_action_cards = new int[MJGameConstants.MAX_COUNT];
		_perfrom_action = MJGameConstants.INVALID_VALUE;
		_response=false;
		
		_target=MJGameConstants.INVALID_SEAT;
		_weave_count=0;
		_action_count=0;
		_ready = 0;
		
		//_chiHuRight.set_empty();
		
		for(int i=0; i < MJGameConstants.MAX_COUNT; i++){
			
			_action_cards[i] = MJGameConstants.INVALID_VALUE;
			
			_action_weaves[i].public_card = 0;
			_action_weaves[i].center_card = MJGameConstants.INVALID_VALUE;
			_action_weaves[i].weave_kind = MJGameConstants.INVALID_VALUE;
			_action_weaves[i].provide_player = MJGameConstants.INVALID_SEAT;
			
			_action[i] = MJGameConstants.INVALID_VALUE;
			
		}
	}
	
	public void add_gang(int card, int provider,int p){
		for(int i=0; i < _weave_count; i++){
			if((_action_weaves[i].center_card == card)&&(_action_weaves[i].weave_kind == MJGameConstants.WIK_GANG)){
				return;
			}
		}
		_action_weaves[_weave_count].public_card = p;
		_action_weaves[_weave_count].center_card = card;
		_action_weaves[_weave_count].weave_kind = MJGameConstants.WIK_GANG;
		_action_weaves[_weave_count].provide_player = provider;
		_weave_count++;
	}
	
	public void add_xiao(int card,int type,int provider,int p){
		for(int i=0; i < _weave_count; i++){
			if((_action_weaves[i].center_card == card)&&(_action_weaves[i].weave_kind == type)){
				return;
			}
		}
		_action_weaves[_weave_count].public_card = p;
		_action_weaves[_weave_count].center_card = card;
		_action_weaves[_weave_count].weave_kind = type;
		_action_weaves[_weave_count].provide_player = provider;
		_weave_count++;
	}
	
	
	
	public void add_bu_zhang(int card, int provider,int p){
		for(int i=0; i < _weave_count; i++){
			if((_action_weaves[i].center_card == card)&&(_action_weaves[i].weave_kind == MJGameConstants.WIK_BU_ZHNAG)){
				return;
			}
		}
		_action_weaves[_weave_count].public_card =p;
		_action_weaves[_weave_count].center_card = card;
		_action_weaves[_weave_count].weave_kind = MJGameConstants.WIK_BU_ZHNAG;
		_action_weaves[_weave_count].provide_player = provider;
		_weave_count++;
	}
	
	public void add_yao_hai_di(){
		_action_weaves[_weave_count].public_card =0;
		_action_weaves[_weave_count].center_card = 0;
		_action_weaves[_weave_count].weave_kind = MJGameConstants.WIK_YAO_HAI_DI;
		_action_weaves[_weave_count].provide_player = -1;
		_weave_count++;
	}
	
	public void add_chi(int card, int type,int provider){
		for(int i=0; i < _weave_count; i++){
			if((_action_weaves[i].center_card == card)&&(_action_weaves[i].weave_kind == type)){
				return;
			}
		}
		_action_weaves[_weave_count].public_card =1;
		_action_weaves[_weave_count].center_card = card;
		_action_weaves[_weave_count].weave_kind = type;
		_action_weaves[_weave_count].provide_player = provider;
		_weave_count++;
	}
	
	public void add_chi_hu(int card,int provider){
		for(int i=0; i < _weave_count; i++){
			if((_action_weaves[i].center_card == card)&&(_action_weaves[i].weave_kind == MJGameConstants.WIK_CHI_HU)){
				return;
			}
		}
		_action_weaves[_weave_count].public_card =1;
		_action_weaves[_weave_count].center_card = card;
		_action_weaves[_weave_count].weave_kind = MJGameConstants.WIK_CHI_HU;
		_action_weaves[_weave_count].provide_player = provider;
		_weave_count++;
	}
	
	
	
	public void add_peng(int card,int provider){
		for(int i=0; i < _weave_count; i++){
			if((_action_weaves[i].center_card == card)&&(_action_weaves[i].weave_kind == MJGameConstants.WIK_PENG)){
				return;
			}
		}
		_action_weaves[_weave_count].public_card =1;
		_action_weaves[_weave_count].center_card = card;
		_action_weaves[_weave_count].weave_kind = MJGameConstants.WIK_PENG;
		_action_weaves[_weave_count].provide_player = provider;
		_weave_count++;
	}
	
	public void add_zi_mo(int card,int provider){
		for(int i=0; i < _weave_count; i++){
			if((_action_weaves[i].center_card == card)&&(_action_weaves[i].weave_kind == MJGameConstants.WIK_ZI_MO)){
				return;
			}
		}
		_action_weaves[_weave_count].public_card =1;
		_action_weaves[_weave_count].center_card = card;
		_action_weaves[_weave_count].weave_kind = MJGameConstants.WIK_ZI_MO;
		_action_weaves[_weave_count].provide_player = provider;
		_weave_count++;
	}
	
	public void clean_weave(){
		_weave_count=0;
	}
	
	//////////////////////////////////////////胡牌屏蔽///////////////////////////
	/////玩家点击不胡的时候，这一轮就不能胡了
	public void chi_hu_round_invalid(){
		
		_chi_hu_round = false;
	}
	
	public void chi_hu_round_valid(){
		
		_chi_hu_round = true;
	}
	
	//////////////////////////////////////////动作///////////////////////////////
	public void add_action(int action){
		if(action==0)return;
		for(int i=0; i < _action_count; i++){
			if(_action[i] == action){
				return;
			}
		}
		_action[_action_count] = action;
		_action_count++;
		
	}
	
	public void clean_action(){
		_action_count = 0;
		_perfrom_action = MJGameConstants.INVALID_VALUE;
		_response=false;
		_operate_card=0;
		_weave_count=0;
	}
	
	public boolean has_xiao_hu(){
		for(int i=0; i < _action_count; i++){
			if(_action[i] == MJGameConstants.WIK_XIAO_HU){
				return true;
			}
		}
		return false;
	}
	
	public boolean has_chi_hu(){
		for(int i=0; i < _action_count; i++){
			if(_action[i] == MJGameConstants.WIK_CHI_HU || _action[i] == MJGameConstants.WIK_ZI_MO){
				return true;
			}
		}
		return false;
	}
	
	public boolean has_zi_mo(){
		for(int i=0; i < _action_count; i++){
			if(_action[i] == MJGameConstants.WIK_ZI_MO){
				return true;
			}
		}
		return false;
	}
	
	
	public boolean has_action(){
		return _action_count>0;
	}
	
	public boolean has_action_by_code(int code){
		for(int i=0; i < _action_count; i++){
			if(_action[i] == code){
				return true;
			}
		}
		return false;
	}
	
	////////////////////////////////////////////////状态/////////////////////////
	public void set_status(int st){
		_status =st;
	}
	public void clean_status(){
		_status =MJGameConstants.INVALID_VALUE;
	}
//
	public int get_status() {
		if(lock_huan_zhang()==true && _status == MJGameConstants.Player_Status_OUT_CARD){
			return MJGameConstants.Player_Status_NULL;//不能出牌
			
		}
		return _status;
	}
//	public void status_cs_gang(){
//		_cs_gang = true;
//	}
	
	public boolean lock_huan_zhang(){
		return _card_status==MJGameConstants.CARD_STATUS_CS_GANG;
	}
	
	public int get_card_status(){
		return _card_status;
	}
	
	public void set_card_status(int st){
		_card_status =st;
	}
////////////////////////////////////////////////操作/////////////////////////
	public boolean is_respone(){
		return _response;
	}
	
	public void set_perform(int action){
		_perfrom_action = action;
		_response = true;
	}
	
	public int get_perform(){
		return _perfrom_action;
	}
	
	public void set_operate_card(int card){
		_operate_card = card;
	}
	
	/**
	 * 记录玩家当前操作 以及操作的牌
	 * @param operate_code
	 * @param operate_card
	 */
	public void operate(int operate_code,int operate_card){
		_response = true;
		_perfrom_action = operate_code;// 执行动作
		_operate_card = operate_card;
	}
	
	public int get_max_operate(){
		int action = 0;
		for(int i =0; i < _action_count; i++){
			int rank=0;
			if(_action[i] == MJGameConstants.WIK_CHI_HU){
				rank = 4;
			}else if(_action[i] == MJGameConstants.WIK_GANG){
				rank = 3;
			}else if(_action[i] == MJGameConstants.WIK_BU_ZHNAG){
				rank = 3;
			}else if(_action[i] == MJGameConstants.WIK_PENG){
				rank = 2;
			}else if(_action[i] == MJGameConstants.WIK_RIGHT ||
					_action[i] == MJGameConstants.WIK_CENTER||
					_action[i] == MJGameConstants.WIK_LEFT){
				rank = 1;
			}
			if(rank>action){
				action = rank;
			}
		}
		
		 
		return (_response == false) ? action : _perfrom_action;
	}


	public boolean is_chi_hu_round() {
		return _chi_hu_round;
	}


	public void set_chi_hu_round(boolean _chi_hu_round) {
		this._chi_hu_round = _chi_hu_round;
	}


	
	
	
	
	
	
	
}
