package com.cai.common.domain;

import org.apache.log4j.Logger;

import com.cai.common.constant.GameConstants;

public class PlayerStatus {
	
	private static Logger logger = Logger.getLogger(PlayerStatus.class);

	private int _status; //玩家状态
	
	/**
	 * 吃碰杆的 动作
	 */
	public int _action[];
	
	public int _action_count;
	
	public int _action_cards[];
	
	public int _perfrom_action;
	
	public int _target;
	
	public boolean _response;
	public int _lou_pai_kind;
	
	/**
	 * 牌的组合
	 */
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
	
	
	//二维数组 打出的牌，可以胡的牌
	public int _hu_out_cards[][];
	public int _hu_out_card_count;
	public int _hu_out_card_ting_count[];
	public int _hu_out_card_ting[];
	
	public boolean ting_check;
	
	public boolean _is_pao_qiang;
	
	public boolean _is_pao;
	
	public boolean _is_pass_action;
	
	public boolean _is_nao_zhuang;
	public int	_biaoyan_count;
	
	private int max_count;
	
	//////////////////////斗地主
	public int _call_banker;
	public int _qiang_banker;
	
	public PlayerStatus(){
		this(GameConstants.MAX_COUNT);
	}
	
	
	public PlayerStatus(int max_count){
		this.max_count = max_count;
		
		_status =GameConstants.INVALID_VALUE;
		_action = new int[max_count];
		
		_action_cards = new int[max_count];
		_perfrom_action = GameConstants.INVALID_VALUE;
		_response=false;
		_lou_pai_kind = -1;
		_action_count =GameConstants.INVALID_VALUE;
		_target=GameConstants.INVALID_SEAT;
		
		_action_weaves = new WeaveItem[max_count];
		for(int i=0; i < max_count;i++){
			_action_weaves[i] = new WeaveItem();
		}
		_weave_count=0;
		_action_count=0;
		
		_ready = 0;
		
		
		_operate_card = GameConstants.INVALID_VALUE;
		
		//_chiHuRight = new ChiHuRight();
		
		_chi_hu_round = true;
		
		_card_status = GameConstants.CARD_STATUS_NORMAL;
		
		
		_hu_cards = new int[GameConstants.MAX_INDEX];
		
		ting_check= false;
		
		_hu_out_cards = new int[max_count][GameConstants.MAX_INDEX];
		
		_hu_out_card_ting_count = new int[max_count];
		
		_hu_out_card_ting  = new int[max_count]; 
		
		_is_pao_qiang=false;
		_is_pass_action = false;
		_is_nao_zhuang=false;
		
		_biaoyan_count = 0;
		
		
		_call_banker=-1;
		_qiang_banker=-1;
	}
	
	
	public void reset(){
		_status =GameConstants.INVALID_VALUE;
		
		_action_cards = new int[max_count];
		_perfrom_action = GameConstants.INVALID_VALUE;
		_response=false;
		_lou_pai_kind = -1;
		_target=GameConstants.INVALID_SEAT;
		_weave_count=0;
		_action_count=0;
		_ready = 0;
		_is_pass_action = false;
		_is_nao_zhuang=false;
		
		_biaoyan_count=0;
		//_chiHuRight.set_empty();
		
		for(int i=0; i < max_count; i++){
			
			_action_cards[i] = GameConstants.INVALID_VALUE;
			
			_action_weaves[i].public_card = 0;
			_action_weaves[i].center_card = GameConstants.INVALID_VALUE;
			_action_weaves[i].weave_kind = GameConstants.INVALID_VALUE;
			_action_weaves[i].provide_player = GameConstants.INVALID_SEAT;
			_action_weaves[i].lou_qi_count = 0;
			for(int j = 0; j<50;j++)
			{
				for(int k = 0; k<2;k++)
					_action_weaves[i].lou_qi_weave[j][k] = 0;
			}
			
			_action[i] = GameConstants.INVALID_VALUE;
			
		}
	}

	public void add_pao(int card, int provider,int p){
		for(int i=0; i < _weave_count; i++){
			if((_action_weaves[i].center_card == card)&&(_action_weaves[i].weave_kind == GameConstants.WIK_PAO)){
				return;
			}
		}
		_action_weaves[_weave_count].public_card = p;
		_action_weaves[_weave_count].center_card = card;
		_action_weaves[_weave_count].weave_kind = GameConstants.WIK_PAO;
		_action_weaves[_weave_count].provide_player = provider;
		_weave_count++;
	}
	public void add_sao(int card, int provider){
		for(int i=0; i < _weave_count; i++){
			if((_action_weaves[i].center_card == card)&&(_action_weaves[i].weave_kind == GameConstants.WIK_SAO)){
				return;
			}
		}
		_action_weaves[_weave_count].public_card = 1;
		_action_weaves[_weave_count].center_card = card;
		_action_weaves[_weave_count].weave_kind = GameConstants.WIK_SAO;
		_action_weaves[_weave_count].provide_player = provider;
		_weave_count++;
	}
	public void add_gang(int card, int provider,int p){
		for(int i=0; i < _weave_count; i++){
			if((_action_weaves[i].center_card == card)&&(_action_weaves[i].weave_kind == GameConstants.WIK_GANG)){
				return;
			}
		}
		_action_weaves[_weave_count].public_card = p;
		_action_weaves[_weave_count].center_card = card;
		_action_weaves[_weave_count].weave_kind = GameConstants.WIK_GANG;
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
	
	public void add_XiaZi(int card,int provider,int p){
		for(int i=0; i < _weave_count; i++){
			if((_action_weaves[i].center_card == card)&&(_action_weaves[i].weave_kind == GameConstants.WIK_XIA_ZI_BU)){
				return;
			}
		}
		_action_weaves[_weave_count].public_card = p;
		_action_weaves[_weave_count].center_card = card;
		_action_weaves[_weave_count].weave_kind = GameConstants.WIK_XIA_ZI_BU;
		_action_weaves[_weave_count].provide_player = provider;
		_weave_count++;
	}
	
	
	
	public void add_bu_zhang(int card, int provider,int p){
		for(int i=0; i < _weave_count; i++){
			if((_action_weaves[i].center_card == card)&&(_action_weaves[i].weave_kind == GameConstants.WIK_BU_ZHNAG)){
				return;
			}
		}
		_action_weaves[_weave_count].public_card =p;
		_action_weaves[_weave_count].center_card = card;
		_action_weaves[_weave_count].weave_kind = GameConstants.WIK_BU_ZHNAG;
		_action_weaves[_weave_count].provide_player = provider;
		_weave_count++;
	}

	public void add_zhao(int card, int provider,int p){
		for(int i=0; i < _weave_count; i++){
			if((_action_weaves[i].center_card == card)&&(_action_weaves[i].weave_kind == GameConstants.WIK_ZHAO)){
				return;
			}
		}
		_action_weaves[_weave_count].public_card =p;
		_action_weaves[_weave_count].center_card = card;
		_action_weaves[_weave_count].weave_kind = GameConstants.WIK_ZHAO;
		_action_weaves[_weave_count].provide_player = provider;
		_weave_count++;
	}
	
	public void add_yao_hai_di(){
		_action_weaves[_weave_count].public_card =0;
		_action_weaves[_weave_count].center_card = 0;
		_action_weaves[_weave_count].weave_kind = GameConstants.WIK_YAO_HAI_DI;
		_action_weaves[_weave_count].provide_player = -1;
		_weave_count++;
	}
	
	public void add_chi_hh(int card, int type,int provider,int count,int lou_qi_kind[][]){
		for(int i=0; i < _weave_count; i++){
			if((_action_weaves[i].center_card == card)&&(_action_weaves[i].weave_kind == type)){
				return;
			}
		}
		_action_weaves[_weave_count].public_card =1;
		_action_weaves[_weave_count].center_card = card;
		_action_weaves[_weave_count].weave_kind = type;
		_action_weaves[_weave_count].provide_player = provider;
		_action_weaves[_weave_count].lou_qi_count = count;
		for(int i = 0; i< count;i++){
			_action_weaves[_weave_count].lou_qi_weave[i][0] = lou_qi_kind[i][0];
			_action_weaves[_weave_count].lou_qi_weave[i][1] = lou_qi_kind[i][1];
			
		}
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
			if((_action_weaves[i].center_card == card)&&(_action_weaves[i].weave_kind == GameConstants.WIK_CHI_HU)){
				return;
			}
		}
		_action_weaves[_weave_count].public_card =1;
		_action_weaves[_weave_count].center_card = card;
		_action_weaves[_weave_count].weave_kind = GameConstants.WIK_CHI_HU;
		_action_weaves[_weave_count].provide_player = provider;
		_weave_count++;
	}
	
	public void add_pao(int card, int provider){
		for(int i=0; i < _weave_count; i++){
			if((_action_weaves[i].center_card == card)&&(_action_weaves[i].weave_kind == GameConstants.WIK_NULL)){
				return;
			}
		}
		_action_weaves[_weave_count].public_card =1;
		_action_weaves[_weave_count].center_card = card;
		_action_weaves[_weave_count].weave_kind = GameConstants.WIK_PAO;
		_action_weaves[_weave_count].provide_player = provider;
		_weave_count++;
	}
	public void add_pass(int card, int provider){
		for(int i=0; i < _weave_count; i++){
			if((_action_weaves[i].center_card == card)&&(_action_weaves[i].weave_kind == GameConstants.WIK_NULL)){
				return;
			}
		}
		_action_weaves[_weave_count].public_card =1;
		_action_weaves[_weave_count].center_card = card;
		_action_weaves[_weave_count].weave_kind = GameConstants.WIK_NULL;
		_action_weaves[_weave_count].provide_player = provider;
		_weave_count++;
	}
	public void add_peng(int card,int provider){
		for(int i=0; i < _weave_count; i++){
			if((_action_weaves[i].center_card == card)&&(_action_weaves[i].weave_kind == GameConstants.WIK_PENG)){
				return;
			}
		}
		_action_weaves[_weave_count].public_card =1;
		_action_weaves[_weave_count].center_card = card;
		_action_weaves[_weave_count].weave_kind = GameConstants.WIK_PENG;
		_action_weaves[_weave_count].provide_player = provider;
		_weave_count++;
	}
	
	public void add_zi_mo(int card,int provider){
		for(int i=0; i < _weave_count; i++){
			if((_action_weaves[i].center_card == card)&&(_action_weaves[i].weave_kind == GameConstants.WIK_ZI_MO)){
				return;
			}
		}
		_action_weaves[_weave_count].public_card =1;
		_action_weaves[_weave_count].center_card = card;
		_action_weaves[_weave_count].weave_kind = GameConstants.WIK_ZI_MO;
		_action_weaves[_weave_count].provide_player = provider;
		_weave_count++;
	}
	
	public void add_lygc_ci(int card,int provider,int weave_kind){
		for(int i=0; i < _weave_count; i++){
			if((_action_weaves[i].center_card == card)&&((_action_weaves[i].weave_kind == GameConstants.WIK_LYGC_CI)
					||(_action_weaves[i].weave_kind == GameConstants.WIK_LYGC_PI_CI))){
				return;
			}
		}
		_action_weaves[_weave_count].public_card =1;
		_action_weaves[_weave_count].center_card = card;
		_action_weaves[_weave_count].weave_kind = weave_kind;
		_action_weaves[_weave_count].provide_player = provider;
		_weave_count++;
	}
	
	//添加 提 跑 扫
	public void add_pao_hu(int card,int seat_index, int provider,int weave_kind){
		for(int i=0; i < _weave_count; i++){
			if((_action_weaves[i].center_card == card)&&(_action_weaves[i].weave_kind == GameConstants.WIK_ZI_MO)){
				return;
			}
		}
		_action_weaves[_weave_count].public_card =1;
		_action_weaves[_weave_count].center_card = card;
		_action_weaves[_weave_count].weave_kind = weave_kind;
		_action_weaves[_weave_count].provide_player = provider;
		_weave_count++;
	}
	
	public void clean_weave(){
		_weave_count=0;
	}
	
	public void reset_pao_qiang(){
		_is_pao_qiang=false;
	}
	
	/**
	 * 清除指定Action
	 * @param action
	 */
	public void clean_action(int action){
		int[] copy_action = new int[_action.length];
		int copy_action_count = 0;
		
		for(int i=0; i < _action.length; i++){
			if(_action[i] == 0)
				continue;
			if(_action[i] != action){
				copy_action[copy_action_count] = _action[i];
				copy_action_count++;
			}
		}
		_action = copy_action;
		_action_count = copy_action_count;
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
	/**
	 * 添加吃碰杆 胡的动作
	 * @param action
	 */
	public void add_action(int action){
		//if(action==0)return;
		for(int i=0; i < _action_count; i++){
			if(_action[i] == action){
				return;
			}
		}
		_action[_action_count] = action;
		_action_count++;
		
	}
	
	/**
	 * 清除玩家动作 临时组合
	 */
	public void clean_action(){
		_action_count = 0;
		_perfrom_action = GameConstants.INVALID_VALUE;
		_response=false;
		_lou_pai_kind = -1;
		_operate_card=0;
		_weave_count=0;
		_is_pass_action = false;
	}
	
	public boolean has_xiao_hu(){
		for(int i=0; i < _action_count; i++){
			if(_action[i] == GameConstants.WIK_XIAO_HU){
				return true;
			}
		}
		return false;
	}
	
	public boolean has_chi_hu(){
		for(int i=0; i < _action_count; i++){
			if(_action[i] == GameConstants.WIK_CHI_HU || _action[i] == GameConstants.WIK_ZI_MO){
				return true;
			}
		}
		return false;
	}
	
	public boolean has_zi_mo(){
		for(int i=0; i < _action_count; i++){
			if(_action[i] == GameConstants.WIK_ZI_MO){
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
//		if(st!=_status) {
//			logger.warn("status=="+st+ThreadUtil.getStack());
//		}
		_status =st;
	}
	public void clean_status(){
		_status =GameConstants.INVALID_VALUE;
	}
//
	public int get_status() {
		if(lock_huan_zhang()==true && _status == GameConstants.Player_Status_OUT_CARD){
			return GameConstants.Player_Status_NULL;//不能出牌
			
		}
		return _status;
	}
//	public void status_cs_gang(){
//		_cs_gang = true;
//	}
	
	public boolean lock_huan_zhang(){
		return _card_status==GameConstants.CARD_STATUS_CS_GANG;
	}
	
	public boolean is_bao_ting(){
		return _card_status==GameConstants.CARD_STATUS_BAO_TING;
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
	///////////////////////////////////////落牌////////////////////////////////
	public int get_lou_kind(){
		return _lou_pai_kind;
	}
	public void set_lou_pai_kind(int lou_kind){
		_lou_pai_kind = lou_kind;
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
			if(_action[i] == GameConstants.WIK_CHI_HU){
				rank = 4;
			}else if(_action[i] == GameConstants.WIK_GANG){
				rank = 3;
			}else if(_action[i] == GameConstants.WIK_BU_ZHNAG){
				rank = 3;
			}else if(_action[i] == GameConstants.WIK_PENG){
				rank = 2;
			}else if(_action[i] == GameConstants.WIK_RIGHT ||
					_action[i] == GameConstants.WIK_CENTER||
					_action[i] == GameConstants.WIK_LEFT){
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
	
	public void set_exe_pass(boolean is_pass){
		this._is_pass_action = is_pass;
	}
	public boolean get_exe_pass(){
		return this._is_pass_action;
	}
	
}
