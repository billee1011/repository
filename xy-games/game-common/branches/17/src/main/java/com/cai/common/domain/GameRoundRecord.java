package com.cai.common.domain;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.annotation.JSONField;
import com.cai.common.constant.MJGameConstants;
import com.cai.common.constant.MsgConstants;

import protobuf.clazz.Protocol.Record_RoomResponse;
import protobuf.clazz.Protocol.RoomInfo;
import protobuf.clazz.Protocol.RoomPlayerResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.Video_Record;
import protobuf.clazz.Protocol.Video_Record_Item;
import protobuf.clazz.Protocol.Video_Record_Item_List;
import protobuf.clazz.Protocol.WeaveItemResponse;

public class GameRoundRecord {
	//参数定义
	public static int INVALID_VALUE	=0;						//无效椅子
	public static int INVALID_SEAT	=-1;						//无效椅子
		
	public int _end_type;
	
	public int _cur_round;
	
	public int _card_count[]; 		//
	public int _cards_data[][]; 	//
	//结束信息
	public int _provide_player[];	//供应用户
	public int _chi_hu_right[];		//胡牌类型
	public ChiHuRight _start_hu_right[];   //起手胡牌类型
	public int _start_hu_score[];	//起手胡牌分数
	
	//积分信息
	public float _game_score[];		//游戏积分
	public int _game_tax[];			//抽水？？？
	
	public String _result_des[];

	public int _win_order[];		//胡牌排名
	
	//详细得分
	
	public int _lost_fan_shu[][];
	
	
	/**
	 * 组合扑克--组合数目
	 */
	public int	_weave_count[];					//
	/**
	 * 组合扑克
	 */
	public WeaveItem					_weave_items[][];		
	public int							_cards_data_niao[];	// 鸟牌
	public int							_count_niao;	//鸟牌个数
	public int							_count_pick_niao;	//中鸟个数
	public int							_player_niao_cards[][];					//中鸟个数
	public int							_player_niao_count[];
	public int							_player_niao_invalid[];
	
	public int 							_banker_player;
	public int 							_left_card_count;
	public int 							_chi_hu_card[];
	public int							_chi_hu_kind[];				//吃胡结果
	public ChiHuRight					_chi_hu_rights[];				//
	public int							_provider[];				//放炮的人？？？？？
	
	public int							_discard_count[];			//丢弃数目
	public int							_discard_cards[][];		    //丢弃记录
	
	
	public int	_cards_index[][]; //用户扑克 = new int [GAME_PLAYER][MAX_INDEX];	
	public GangScore _gang_score[];				//杠牌得分
					//游戏得分

	//解散房间
	public int							_release_room[];		//解散房间
	
	public int 							_game_type_index;
	public long 							_start_time;
	public int							_version;
	public int 							_hu_result[];

	@JSONField(serialize = false)
	public Video_Record.Builder _video_recode;
	@JSONField(serialize = false)
	public RoomInfo.Builder _room_info;
	//@JSONField(serialize = false)
	//public RoomPlayerResponse.Builder _players;
	@JSONField(serialize = false)
	public boolean _show_bird_effect;
	
	public byte _record_bytes[];
	
	@JSONField(serialize = false)
	public int _especial_show_cards[];
	@JSONField(serialize = false)
	public int _especial_card_count;
	
	@JSONField(serialize = false)
	public int _piao_lai_count;
	@JSONField(serialize = false)
	public int[] _piao_lai_seat;
	//@JSONField(serialize = false)
	//public int _bei_shu;
	
	@JSONField(serialize = false)
	public String _especial_txt;
	
	@JSONField(serialize = false)
	public int _especial_txt_type;
	
	public GameRoundRecord(){
		//游戏变量
		_cards_index = new int [MJGameConstants.GAME_PLAYER][MJGameConstants.MAX_INDEX];	
		_gang_score = new GangScore[MJGameConstants.GAME_PLAYER];
		
		//_allGRR._gang_score = new GangScore[MJGameConstants.GAME_PLAYER];
		
		for(int i = 0 ;  i < MJGameConstants.GAME_PLAYER;i++){
			_gang_score[i] = new GangScore();
		}
		
		_game_score = new float [MJGameConstants.GAME_PLAYER];
		_lost_fan_shu = new int [MJGameConstants.GAME_PLAYER][MJGameConstants.GAME_PLAYER];
		
		_win_order = new int [MJGameConstants.GAME_PLAYER];
		//出牌信息
		_discard_count = new int [MJGameConstants.GAME_PLAYER];
		_discard_cards = new int [MJGameConstants.GAME_PLAYER][55];	
		
		//结束信息
		_chi_hu_kind = new int[MJGameConstants.GAME_PLAYER];
		_chi_hu_rights = new ChiHuRight[MJGameConstants.GAME_PLAYER];
		
		_start_hu_right = new ChiHuRight[MJGameConstants.GAME_PLAYER];
		for(int i = 0 ;  i < MJGameConstants.GAME_PLAYER;i++){
			_chi_hu_rights[i] = new ChiHuRight();
			_start_hu_right[i] = new ChiHuRight();
		}
		
		_provider = new int[MJGameConstants.GAME_PLAYER];
		_cards_data_niao = new int [MJGameConstants.MAX_NIAO_CARD];
		
		//用户状态
		_start_hu_score = new int[MJGameConstants.GAME_PLAYER];
		//_all_start_hu_score = new int[MJGameConstants.GAME_PLAYER];
		
		//组合扑克
		_weave_count = new int[MJGameConstants.GAME_PLAYER];
		_weave_items = new WeaveItem[MJGameConstants.GAME_PLAYER][MJGameConstants.MAX_WEAVE];
		_weave_items= new WeaveItem[MJGameConstants.GAME_PLAYER][MJGameConstants.MAX_WEAVE];
		for(int i = 0 ;  i < MJGameConstants.GAME_PLAYER;i++){
			for(int j = 0 ;  j < MJGameConstants.MAX_WEAVE;j++){
				_weave_items[i][j] = new WeaveItem();
			}
		}
		
		_player_niao_cards = new int[MJGameConstants.GAME_PLAYER][MJGameConstants.MAX_NIAO_CARD];
		_player_niao_count=new int[MJGameConstants.GAME_PLAYER];
		_player_niao_invalid=new int[MJGameConstants.GAME_PLAYER];
		_release_room =new int[MJGameConstants.GAME_PLAYER];
		
		_result_des = new String[MJGameConstants.GAME_PLAYER];
		_hu_result = new int[MJGameConstants.GAME_PLAYER];
		_chi_hu_card = new int[MJGameConstants.GAME_PLAYER];
		
		for(int i = 0 ;  i < MJGameConstants.GAME_PLAYER;i++){
			_release_room[i] = INVALID_VALUE;
			_win_order[i] =0;
			
			_result_des[i]="";
			//_player_niao_invalid[i] = INVALID_VALUE;
			_chi_hu_card[i]= 0;
		}
		_card_count = new int[MJGameConstants.GAME_PLAYER];
		_cards_data = new int[MJGameConstants.GAME_PLAYER][MJGameConstants.MAX_COUNT];
		
		_video_recode = Video_Record.newBuilder();
		
		_room_info = RoomInfo.newBuilder();
		//_players = RoomPlayerResponse.newBuilder();
		//_records = Video_Record_Item.newBuilder();
		
		_show_bird_effect = false;
		
		
		_especial_show_cards = new int[MJGameConstants.MAX_COUNT];
		_especial_card_count = 0;
		
		
		//飘赖
		_piao_lai_count = 0;
		_piao_lai_seat =new int[MJGameConstants.MAX_COUNT];
		
		//_bei_shu=1;
		_especial_txt="";
		_especial_txt_type = MJGameConstants.Especial_Txt_Type_NULL;
	}
	
	public String to_json(){
		try{
			return JSON.toJSONString(this);
		}catch(Exception e){
			e.printStackTrace();
		}
		return null;
	}
	
	public Video_Record.Builder get_video_record(){
		//Video_Record vr = _video_recode.build();
		_video_recode.setRoomInfo(_room_info);
		return _video_recode;
	}
	
	public static GameRoundRecord to_object(String str){
		try{
			return JSON.parseObject(str, GameRoundRecord.class);
		}catch(Exception e){
			e.printStackTrace();
		}
		return null;
		
	}
	
	public void add_room_response(RoomResponse.Builder roomResponse){
		Record_RoomResponse.Builder rrr = Record_RoomResponse.newBuilder();
		long ddd = System.currentTimeMillis()/1000L - _start_time;
		rrr.setDelta(ddd);
		rrr.setRr(roomResponse);
		_video_recode.addRecordList(rrr);
	}
	
	
//	public void add_mo_pai(int seat_index,int card){
//		Video_Record_Item_List.Builder vril = Video_Record_Item_List.newBuilder();
//		vril.setLeftCardCount(this._left_card_count);
//		vril.setCurrentPlayer(seat_index);
//		
//		Video_Record_Item.Builder record = Video_Record_Item.newBuilder();
//		record.setOperatePlayer(seat_index);
//		record.setType(1);//摸牌
//		record.setCardCount(1);
//		record.addCardData(card);
//
//		vril.addRecords(record);
//		
//		_video_recode.addRecords(vril);
//	}
	
//	public void add_chu_pai(int seat_index,int card,int refresh_card_count,int refresh_card[],int weave_count,WeaveItem weaveitems[]){
//		Video_Record_Item_List.Builder vril = Video_Record_Item_List.newBuilder();
//		vril.setLeftCardCount(this._left_card_count);
//		vril.setCurrentPlayer(seat_index);
//		
//		Video_Record_Item.Builder record = Video_Record_Item.newBuilder();
//		record.setOperatePlayer(seat_index);
//		record.setType(2);//摸牌
//		record.setCardCount(1);
//		record.addCardData(card);
//
//		vril.addRecords(record);
//		
//		
//		/////////////////////////////////////////刷新手牌
//		Video_Record_Item.Builder refresh_record = Video_Record_Item.newBuilder();
//		record.setType(3);//刷新牌
//		refresh_record.setOperatePlayer(seat_index);
//		//手牌数量
//		refresh_record.setCardCount(refresh_card_count);
//		refresh_record.setWeaveCount(weave_count);
//			
//		// 手牌
//		for (int j = 0; j < refresh_card_count; j++) {
//			refresh_record.addCardData(refresh_card[j]);
//		}
//		//组合牌
//		if (weave_count>0) {
//			for (int j = 0; j < weave_count; j++) {
//				WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
//				weaveItem_item.setProvidePlayer(weaveitems[j].provide_player);
//				weaveItem_item.setPublicCard(weaveitems[j].public_card);
//				weaveItem_item.setWeaveKind(weaveitems[j].weave_kind);
//				weaveItem_item.setCenterCard(weaveitems[j].center_card);
//				refresh_record.addWeaveItems(weaveItem_item);
//			}
//		}
//		
//		vril.addRecords(refresh_record);
//		
//		_video_recode.addRecords(vril);
//	}
//	
//	public void add_pai_dui(int seat_index,int count,int cards[]){
//		Video_Record_Item_List.Builder vril = Video_Record_Item_List.newBuilder();
//		vril.setLeftCardCount(this._left_card_count);
//		vril.setCurrentPlayer(seat_index);
//		
//		Video_Record_Item.Builder record = Video_Record_Item.newBuilder();
//		record.setOperatePlayer(seat_index);
//		record.setType(4);//加到牌堆
//		record.setCardCount(count);
//		for(int i=0; i < count; i++){
//			record.addCardData(cards[i]);
//		}
//
//		vril.addRecords(record);
//		
//		_video_recode.addRecords(vril);
//	}
//	
//	
//	public void refresh_player_card(int seat_index, int card_count, int cards[], int weave_count, WeaveItem weaveitems[]){
////		Video_Record_Item.Builder record = Video_Record_Item.newBuilder();
////		record.setOperatePlayer(seat_index);
////		record.setActions(2);
////	
//// 		record.setCurrentPlayer(seat_index);
////		record.setLeftCardCount(this._left_card_count);
////		
////		record.setCardCount(card_count);
////		record.setWeaveCount(weave_count);
////		//组合牌
////		if (weave_count>0) {
////			for (int j = 0; j < weave_count; j++) {
////				WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
////				weaveItem_item.setProvidePlayer(weaveitems[j].provide_player);
////				weaveItem_item.setPublicCard(weaveitems[j].public_card);
////				weaveItem_item.setWeaveKind(weaveitems[j].weave_kind);
////				weaveItem_item.setCenterCard(weaveitems[j].center_card);
////				record.addWeaveItems(weaveItem_item);
////			}
////		}
////
////		// 手牌
////		for (int j = 0; j < card_count; j++) {
////			record.addCardData(cards[j]);
////		}
//	}

	public int get_end_type() {
		return _end_type;
	}

	public void set_end_type(int _end_type) {
		this._end_type = _end_type;
	}

	public int get_cur_round() {
		return _cur_round;
	}

	public void set_cur_round(int _cur_round) {
		this._cur_round = _cur_round;
	}

	public int[] get_card_count() {
		return _card_count;
	}

	public void set_card_count(int[] _card_count) {
		this._card_count = _card_count;
	}

	public int[][] get_cards_data() {
		return _cards_data;
	}

	public void set_cards_data(int[][] _cards_data) {
		this._cards_data = _cards_data;
	}

	public int[] get_provide_player() {
		return _provide_player;
	}

	public void set_provide_player(int[] _provide_player) {
		this._provide_player = _provide_player;
	}

	public int[] get_chi_hu_right() {
		return _chi_hu_right;
	}

	public void set_chi_hu_right(int[] _chi_hu_right) {
		this._chi_hu_right = _chi_hu_right;
	}


	public int[] get_start_hu_score() {
		return _start_hu_score;
	}

	public void set_start_hu_score(int[] _start_hu_score) {
		this._start_hu_score = _start_hu_score;
	}

	public float[] get_game_score() {
		return _game_score;
	}

	public void set_game_score(float[] _game_score) {
		this._game_score = _game_score;
	}

	public int[] get_game_tax() {
		return _game_tax;
	}

	public void set_game_tax(int[] _game_tax) {
		this._game_tax = _game_tax;
	}

	public int[] get_win_order() {
		return _win_order;
	}

	public void set_win_order(int[] _win_order) {
		this._win_order = _win_order;
	}


	public int[][] get_lost_fan_shu() {
		return _lost_fan_shu;
	}

	public void set_lost_fan_shu(int[][] _lost_fan_shu) {
		this._lost_fan_shu = _lost_fan_shu;
	}

	public int[] get_weave_count() {
		return _weave_count;
	}

	public void set_weave_count(int[] _weave_count) {
		this._weave_count = _weave_count;
	}

	public WeaveItem[][] get_weave_items() {
		return _weave_items;
	}

	public void set_weave_items(WeaveItem[][] _weave_items) {
		this._weave_items = _weave_items;
	}

	public int[] get_cards_data_niao() {
		return _cards_data_niao;
	}

	public void set_cards_data_niao(int[] _cards_data_niao) {
		this._cards_data_niao = _cards_data_niao;
	}

	public int get_count_niao() {
		return _count_niao;
	}

	public void set_count_niao(int _count_niao) {
		this._count_niao = _count_niao;
	}

	public int get_count_pick_niao() {
		return _count_pick_niao;
	}

	public void set_count_pick_niao(int _count_pick_niao) {
		this._count_pick_niao = _count_pick_niao;
	}

	public int[][] get_player_niao_cards() {
		return _player_niao_cards;
	}

	public void set_player_niao_cards(int[][] _player_niao_cards) {
		this._player_niao_cards = _player_niao_cards;
	}

	public int[] get_player_niao_count() {
		return _player_niao_count;
	}

	public void set_player_niao_count(int[] _player_niao_count) {
		this._player_niao_count = _player_niao_count;
	}

	public int get_banker_player() {
		return _banker_player;
	}

	public void set_banker_player(int _banker_player) {
		this._banker_player = _banker_player;
	}

	public int get_left_card_count() {
		return _left_card_count;
	}

	public void set_left_card_count(int _left_card_count) {
		this._left_card_count = _left_card_count;
	}


	public int[] get_chi_hu_kind() {
		return _chi_hu_kind;
	}

	public void set_chi_hu_kind(int[] _chi_hu_kind) {
		this._chi_hu_kind = _chi_hu_kind;
	}

	public ChiHuRight[] get_chi_hu_rights() {
		return _chi_hu_rights;
	}

	public void set_chi_hu_rights(ChiHuRight[] _chi_hu_rights) {
		this._chi_hu_rights = _chi_hu_rights;
	}

	public int[] get_provider() {
		return _provider;
	}

	public void set_provider(int[] _provider) {
		this._provider = _provider;
	}

	public int[] get_discard_count() {
		return _discard_count;
	}

	public void set_discard_count(int[] _discard_count) {
		this._discard_count = _discard_count;
	}

	public int[][] get_discard_cards() {
		return _discard_cards;
	}

	public void set_discard_cards(int[][] _discard_cards) {
		this._discard_cards = _discard_cards;
	}

	public ChiHuRight[] get_start_hu_right() {
		return _start_hu_right;
	}

	public void set_start_hu_right(ChiHuRight[] _start_hu_right) {
		this._start_hu_right = _start_hu_right;
	}

	public int[][] get_cards_index() {
		return _cards_index;
	}

	public void set_cards_index(int[][] _cards_index) {
		this._cards_index = _cards_index;
	}

	public GangScore[] get_gang_score() {
		return _gang_score;
	}

	public void set_gang_score(GangScore[] _gang_score) {
		this._gang_score = _gang_score;
	}

	public int[] get_release_room() {
		return _release_room;
	}

	public void set_release_room(int[] _release_room) {
		this._release_room = _release_room;
	}

	public String[] get_result_des() {
		return _result_des;
	}

	public void set_result_des(String[] _result_des) {
		this._result_des = _result_des;
	}

	public int get_game_type_index() {
		return _game_type_index;
	}

	public void set_game_type_index(int _game_type_index) {
		this._game_type_index = _game_type_index;
	}

	public long get_start_time() {
		return _start_time;
	}

	public void set_start_time(long _start_time) {
		this._start_time = _start_time;
	}

	public int get_version() {
		return _version;
	}

	public void set_version(int _version) {
		this._version = _version;
	}

	public int[] get_player_niao_invalid() {
		return _player_niao_invalid;
	}

	public void set_player_niao_invalid(int[] _player_niao_invalid) {
		this._player_niao_invalid = _player_niao_invalid;
	}

	public int[] get_chi_hu_card() {
		return _chi_hu_card;
	}

	public void set_chi_hu_card(int[] _chi_hu_card) {
		this._chi_hu_card = _chi_hu_card;
	}

	public int[] get_hu_result() {
		return _hu_result;
	}

	public void set_hu_result(int[] _hu_result) {
		this._hu_result = _hu_result;
	}

	public byte[] getRecord_bytes() {
		return _record_bytes;
	}

	public void setRecord_bytes(byte[] record_bytes) {
		this._record_bytes = record_bytes;
	}

	
}
