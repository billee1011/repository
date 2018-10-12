package com.cai.common.domain;

import com.cai.common.constant.GameConstants;

public class PlayerResult {
	public long create_player_id;
	public long create_time;
	public int							room_id;									//id
	public int							game_type_index;							//类型 0转转 1长沙
	public int							game_rule_index;							//w玩法
	public String						game_rule_des;							//玩法描述
	public int							game_round;									//游戏局数
	
	public float game_score[];
	public int lost_fan_shu[][];
	public int win_order[];
	
	
	public int zi_mo_count[];
	public int jie_pao_count[];
	public int dian_pao_count[];
	public int an_gang_count[];
	public int ming_gang_count[];
	
	public int da_hu_zi_mo[];
	public int da_hu_jie_pao[];
	public int da_hu_dian_pao[];
	public int xiao_hu_zi_mo[];
	public int xiao_hu_jie_pao[];
	public int xiao_hu_dian_pao[];
	
	public int piao_lai_count[];
	
	public int pao[];
	public int qiang[];
	
	
	public int men_qing[];
	public int gun_gun[];
	public int peng_peng_hu[];
	public int gang_shang_hua[];
	public int hai_di[];
	
	public PlayerResult(){
		
	}
	public PlayerResult(long create_player_id,int room_id,int game_type_index,int game_rule_index,int game_round,String game_rule_des){
		this.create_player_id = create_player_id;
		this.room_id = room_id;
		this.game_type_index = game_type_index;
		this.game_rule_index = game_rule_index;
		this.game_round = game_round;
		this.game_rule_des = game_rule_des;
		
		create_time = System.currentTimeMillis()/1000;
		
		
		game_score = new float[GameConstants.GAME_PLAYER]; 
		lost_fan_shu = new int[GameConstants.GAME_PLAYER][GameConstants.GAME_PLAYER]; 
		win_order= new int[GameConstants.GAME_PLAYER];
		
		zi_mo_count= new int[GameConstants.GAME_PLAYER];
		jie_pao_count= new int[GameConstants.GAME_PLAYER];
		dian_pao_count= new int[GameConstants.GAME_PLAYER];
		an_gang_count= new int[GameConstants.GAME_PLAYER];
		ming_gang_count= new int[GameConstants.GAME_PLAYER];
		
		
		da_hu_zi_mo= new int[GameConstants.GAME_PLAYER];
		da_hu_jie_pao= new int[GameConstants.GAME_PLAYER];
		da_hu_dian_pao= new int[GameConstants.GAME_PLAYER];
		xiao_hu_zi_mo= new int[GameConstants.GAME_PLAYER];
		xiao_hu_jie_pao= new int[GameConstants.GAME_PLAYER];
		xiao_hu_dian_pao= new int[GameConstants.GAME_PLAYER];
		
		piao_lai_count= new int[GameConstants.GAME_PLAYER];
		
		pao= new int[GameConstants.GAME_PLAYER];
		qiang= new int[GameConstants.GAME_PLAYER];
		
		
		
		men_qing = new int[GameConstants.GAME_PLAYER];
		gun_gun = new int[GameConstants.GAME_PLAYER];
		peng_peng_hu = new int[GameConstants.GAME_PLAYER];
		gang_shang_hua = new int[GameConstants.GAME_PLAYER];
		hai_di = new int[GameConstants.GAME_PLAYER];
		
		for(int i=0; i < GameConstants.GAME_PLAYER; i++){
			piao_lai_count[i] = 0;
			
			pao[i] = -1;
			qiang[i] = -1;
		}
	}

	public int[] getZi_mo_count() {
		return zi_mo_count;
	}

	public void setZi_mo_count(int[] zi_mo_count) {
		this.zi_mo_count = zi_mo_count;
	}

	public int[] getJie_pao_count() {
		return jie_pao_count;
	}

	public void setJie_pao_count(int[] jie_pao_count) {
		this.jie_pao_count = jie_pao_count;
	}

	public int[] getDian_pao_count() {
		return dian_pao_count;
	}

	public void setDian_pao_count(int[] dian_pao_count) {
		this.dian_pao_count = dian_pao_count;
	}

	public int[] getAn_gang_count() {
		return an_gang_count;
	}

	public void setAn_gang_count(int[] an_gang_count) {
		this.an_gang_count = an_gang_count;
	}

	public int[] getMing_gang_count() {
		return ming_gang_count;
	}

	public void setMing_gang_count(int[] ming_gang_count) {
		this.ming_gang_count = ming_gang_count;
	}

	public float[] getGame_score() {
		return game_score;
	}

	public void setGame_score(float[] game_score) {
		this.game_score = game_score;
	}

	public int[][] getLost_fan_shu() {
		return lost_fan_shu;
	}

	public void setLost_fan_shu(int[][] lost_fan_shu) {
		this.lost_fan_shu = lost_fan_shu;
	}

	public int[] getWin_order() {
		return win_order;
	}

	public void setWin_order(int[] win_order) {
		this.win_order = win_order;
	}

	public long getCreate_player_id() {
		return create_player_id;
	}

	public void setCreate_player_id(long create_player_id) {
		this.create_player_id = create_player_id;
	}

	public long getCreate_time() {
		return create_time;
	}

	public void setCreate_time(long create_time) {
		this.create_time = create_time;
	}

	public int getRoom_id() {
		return room_id;
	}

	public void setRoom_id(int room_id) {
		this.room_id = room_id;
	}

	public int getGame_type_index() {
		return game_type_index;
	}

	public void setGame_type_index(int game_type_index) {
		this.game_type_index = game_type_index;
	}

	public int getGame_rule_index() {
		return game_rule_index;
	}

	public void setGame_rule_index(int game_rule_index) {
		this.game_rule_index = game_rule_index;
	}

	public String getGame_rule_des() {
		return game_rule_des;
	}

	public void setGame_rule_des(String game_rule_des) {
		this.game_rule_des = game_rule_des;
	}

	public int getGame_round() {
		return game_round;
	}

	public void setGame_round(int game_round) {
		this.game_round = game_round;
	}

	public int[] getDa_hu_zi_mo() {
		return da_hu_zi_mo;
	}

	public void setDa_hu_zi_mo(int[] da_hu_zi_mo) {
		this.da_hu_zi_mo = da_hu_zi_mo;
	}

	public int[] getDa_hu_jie_pao() {
		return da_hu_jie_pao;
	}

	public void setDa_hu_jie_pao(int[] da_hu_jie_pao) {
		this.da_hu_jie_pao = da_hu_jie_pao;
	}

	public int[] getDa_hu_dian_pao() {
		return da_hu_dian_pao;
	}

	public void setDa_hu_dian_pao(int[] da_hu_dian_pao) {
		this.da_hu_dian_pao = da_hu_dian_pao;
	}

	public int[] getXiao_hu_zi_mo() {
		return xiao_hu_zi_mo;
	}

	public void setXiao_hu_zi_mo(int[] xiao_hu_zi_mo) {
		this.xiao_hu_zi_mo = xiao_hu_zi_mo;
	}

	public int[] getXiao_hu_jie_pao() {
		return xiao_hu_jie_pao;
	}

	public void setXiao_hu_jie_pao(int[] xiao_hu_jie_pao) {
		this.xiao_hu_jie_pao = xiao_hu_jie_pao;
	}

	public int[] getXiao_hu_dian_pao() {
		return xiao_hu_dian_pao;
	}

	public void setXiao_hu_dian_pao(int[] xiao_hu_dian_pao) {
		this.xiao_hu_dian_pao = xiao_hu_dian_pao;
	}
}
