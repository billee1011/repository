/**
 * 
 */
package com.cai.game.pdk.handler.srpdk;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.RedisConstant;
import com.cai.common.define.ECardType;
import com.cai.common.define.EGoldOperateType;
import com.cai.common.define.EMoneyOperateType;
import com.cai.common.define.EMsgIdType;
import com.cai.common.define.ERedisTopicType;
import com.cai.common.define.ESysMsgType;
import com.cai.common.domain.AddGoldResultModel;
import com.cai.common.domain.AddMoneyResultModel;
import com.cai.common.domain.BrandLogModel;
import com.cai.common.domain.CardsData;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.GameRoomRecord;
import com.cai.common.domain.GameRoundRecord;
import com.cai.common.domain.Player;
import com.cai.common.domain.PlayerResult;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.Room;
import com.cai.common.domain.RoomRedisModel;
import com.cai.common.domain.SysParamModel;
import com.cai.common.domain.WeaveItem;
import com.cai.common.rmi.ICenterRMIServer;
import com.cai.common.util.FvMask;
import com.cai.common.util.GameDescUtil;
import com.cai.common.util.LocationUtil;
import com.cai.common.util.RandomUtil;
import com.cai.common.util.RoomComonUtil;
import com.cai.common.util.SpringService;
import com.cai.common.util.ZipUtil;
import com.cai.dictionary.BrandIdDict;
import com.cai.dictionary.SysParamDict;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.AddDiscardRunnable;
import com.cai.future.runnable.ChulifirstCardRunnable;
import com.cai.future.runnable.CreateTimeOutRunnable;
import com.cai.future.runnable.DispatchCardRunnable;
import com.cai.future.runnable.DispatchFirstCardRunnable;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.future.runnable.GangCardRunnable;
import com.cai.future.runnable.JianPaoHuRunnable;
import com.cai.game.RoomUtil;
import com.cai.game.pdk.PDKGameLogic;
import com.cai.game.pdk.PDKTable;
import com.cai.game.pdk.handler.PDKHandler;
import com.cai.game.pdk.handler.PDKHandlerFinish;
import com.cai.game.pdk.handler.PDKHandlerOutCardOperate;
import com.cai.game.pdk.handler.jdpdk.PDKHandlerOutCardOperate_JD;
import com.cai.redis.service.RedisService;
import com.cai.service.MongoDBServiceImpl;
import com.cai.service.PlayerServiceImpl;
import com.cai.service.RedisServiceImpl;
import com.cai.util.SysParamUtil;

import protobuf.clazz.Protocol;
import protobuf.clazz.Protocol.ChiGroupCard;
import protobuf.clazz.Protocol.GameEndResponse;
import protobuf.clazz.Protocol.GameStart_PDK;
import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.LocationInfor;
import protobuf.clazz.Protocol.MsgAllResponse;
import protobuf.clazz.Protocol.OutCardData;
import protobuf.clazz.Protocol.PlayerResultResponse;
import protobuf.clazz.Protocol.PukeGameEnd;
import protobuf.clazz.Protocol.RefreshCards;
import protobuf.clazz.Protocol.Response;
import protobuf.clazz.Protocol.Response.ResponseType;
import protobuf.clazz.Protocol.RoomInfo;
import protobuf.clazz.Protocol.RoomPlayerResponse;
import protobuf.clazz.Protocol.RoomRequest;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.RoomResponse_DDZ;
import protobuf.clazz.Protocol.RoomResponse_PDK;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.TableResponseDDZ;
import protobuf.clazz.Protocol.TableResponse_PDK;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;
import protobuf.redis.ProtoRedis.RedisResponse;
import protobuf.redis.ProtoRedis.RedisResponse.RsResponseType;
import protobuf.redis.ProtoRedis.RsAccountResponse;
import protobuf.redis.ProtoRedis.RsCmdResponse;

//效验类型
class EstimatKind {
	public static int EstimatKind_OutCard = 1; // 出牌效验
	public static int EstimatKind_GangCard = 2; // 杠牌效验
}

///////////////////////////////////////////////////////////////////////////////////////////////
public class PDK_SR_Table extends PDKTable {

	public PDK_SR_Table() {
	}

	public void init_table(int game_type_index, int game_rule_index, int game_round) {
		_game_type_index = game_type_index;//
		_game_rule_index = game_rule_index;
		_game_round = game_round;
		_cur_round = 0;
		this._logic._game_rule_index=_game_rule_index;

		_player_result = new PlayerResult(this.getRoom_owner_account_id(), this.getRoom_id(), _game_type_index,
				_game_rule_index, _game_round, this.get_game_des());
		boolean kd = this.kou_dou();
		if (kd == false  ) {
			return;
		}
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_player_result.hu_pai_count[i] = 0;
			_player_result.ming_tang_count[i] = 0;
			_player_result.ying_xi_count[i] = 0;
			_game_score[i]=0;
		}
		// 初始化基础牌局handler
		_handler_out_card_operate = new PDKHandlerOutCardOperate_SR();
		
		_handler_finish = new PDKHandlerFinish();

	}

	@Override
	public void runnable_set_trustee(int _seat_index) {
		// TODO Auto-generated method stub

	}
	// 游戏开始
	public boolean handler_game_start() {

		_game_status = GameConstants.GS_MJ_PLAY;
		this.log_error("gme_status:"+this._game_status);

		reset_init_data();

		// 庄家选择
		this.progress_banker_select();

		GRR._banker_player = _banker_select;
		_current_player = GRR._banker_player;
		
		_turn_out__player = GameConstants.INVALID_SEAT;
		_bao_pei_palyer=GameConstants.INVALID_SEAT;
		_hong_tao_palyer=GameConstants.INVALID_SEAT;
		
		_turn_out_card_count=0;
		for(int i=0;i<this.get_hand_card_count_max();i++){
			_turn_out_card_data[i]=GameConstants.INVALID_CARD;
			
		}
		for(int i=0;i<this.getTablePlayerNumber();i++){
			GRR._cur_card_type[i]=GameConstants.PDK_CT_ERROR;
			GRR._cur_round_pass[i]=0;
			for(int j=0;j<this.get_hand_card_count_max();j++){
				GRR._cur_round_data[i][j]=GameConstants.INVALID_CARD;
				GRR._cur_change_round_data[i][j]=GameConstants.INVALID_CARD;
			}
		}


		//
		
		if(this.has_rule(GameConstants.GAME_RULE_YIFU_COUNT)){
			_repertory_card = new int[GameConstants.CARD_COUNT_PDK_SR_ONE];
			shuffle(_repertory_card, GameConstants.CARD_DATA_PDK_SR_ONE);
		}else{
			_repertory_card = new int[GameConstants.CARD_COUNT_PDK_SR_TWO];
			shuffle(_repertory_card, GameConstants.CARD_DATA_PDK_SR_TWO);
		}
		

	
//		DEBUG_CARDS_MODE = true;
//		BACK_DEBUG_CARDS_MODE = true;
		if (DEBUG_CARDS_MODE || BACK_DEBUG_CARDS_MODE)
			test_cards();

		return game_start_pkd();
	}
	/**
	 * 开始 跑得快经典
	 * 
	 * @return
	 */
	private boolean  game_start_pkd(){
		// 游戏开始
		this._game_status = GameConstants.GS_MJ_PLAY;// 设置状态
		
		int FlashTime = 4000;
		int standTime = 1000;
		
		//初始化游戏变量
		for(int i=0;i<this.getTablePlayerNumber();i++){
			_boom_num[i]=0;
			_out_card_times[i]=0;
		}
		_total_boom=0;
		
		for(int play_index=0;play_index<this.getTablePlayerNumber();play_index++){
			
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			this.load_room_info_data(roomResponse);
			this.load_common_status(roomResponse);
			roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
			roomResponse.setGameStatus(this._game_status);
			// 发送数据
			RoomResponse_PDK.Builder roomResponse_pdk = RoomResponse_PDK.newBuilder();
			GameStart_PDK.Builder gamestart_pdk = GameStart_PDK.newBuilder();
			if (this._cur_round == 1) {
				// shuffle_players();
				 GRR._banker_player = this.find_banker();
				this.load_player_info_data(roomResponse);
			}
			this._current_player = GRR._banker_player;
			gamestart_pdk.setCurBanker(GRR._banker_player);
			
			for(int i=0;i<this.getTablePlayerNumber();i++){
				if(this.has_rule(GameConstants.GAME_RULE_DISPLAY_CARD)){
					gamestart_pdk.addCardCount(GRR._card_count[i]);
				}else{
					if(i == play_index){
						gamestart_pdk.addCardCount(GRR._card_count[i]);
					}
					else{
						gamestart_pdk.addCardCount(-1);
					}
				}
				Int32ArrayResponse.Builder cards_card=Int32ArrayResponse.newBuilder();
				gamestart_pdk.addCardsData(i,cards_card);
			}
			
			// 手牌--将自己的手牌数据发给自己
			Int32ArrayResponse.Builder cards_card=Int32ArrayResponse.newBuilder();
			for(int j=0;j<GRR._card_count[play_index];j++){
				cards_card.addItem(GRR._cards_data[play_index][j]);
			}
			gamestart_pdk.addCardsData(play_index, cards_card);
			gamestart_pdk.setDisplayTime(10);
			gamestart_pdk.setMagicCard(GameConstants.INVALID_CARD);
			roomResponse_pdk.setGameStart(gamestart_pdk);
			roomResponse.setRoomResponsePdk(roomResponse_pdk);   
			 
			
			int gameId = this.getGame_id() == 0 ? 8 : this.getGame_id();
			SysParamModel sysParamModel1104 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(gameId)
					.get(1104);
			if (sysParamModel1104 != null && sysParamModel1104.getVal1() > 0 && sysParamModel1104.getVal1() < 10000) {
				FlashTime = sysParamModel1104.getVal1();
			}
			if (sysParamModel1104 != null && sysParamModel1104.getVal2() > 0 && sysParamModel1104.getVal2() < 10000) {
				standTime = sysParamModel1104.getVal2();
			}
			roomResponse.setFlashTime(FlashTime);
			roomResponse.setStandTime(standTime);
			
			GRR.add_room_response(roomResponse);
			
			// 自己才有牌数据
			this.send_response_to_player(play_index, roomResponse);
		}
		
		PlayerStatus curPlayerStatus = this._playerStatus[this._current_player];
		curPlayerStatus.set_status(GameConstants.Player_Status_OUT_CARD);// 出牌状态
		this.operate_player_status();


		return true;

	}
	public int find_banker(){
		int seat = -1;
		if(this.has_rule(GameConstants.GAME_RULE_YIFU_COUNT)){
			for(int i = 0; i<this.getTablePlayerNumber();i++)
			{
				for(int j = 0; j<GRR._card_count[i];j++)
					if(GRR._cards_data[i][j] == 0x33)
						return i;
			}
		}
		return RandomUtil.getRandomNumber(this.getTablePlayerNumber());
	}
	
	/**
	 * 是否托管
	 * 
	 * @param seat_index
	 * @return
	 */
	public boolean isTrutess(int seat_index) {
		if (seat_index < 0 || seat_index > this.getTablePlayerNumber())
			return false;
		return istrustee[seat_index];
	}

	/// 洗牌
	private void shuffle(int repertory_card[], int mj_cards[]) {

		_logic.random_card_data(repertory_card, mj_cards);

		int count = this.getTablePlayerNumber();
		if(this.has_rule(GameConstants.GAME_RULE_YIFU_COUNT)){
			for (int i = 0; i < count; i++) {
				for (int j = 0; j < GameConstants.MAX_PDK_COUNT_SS; j++) {
					GRR._cards_data[i][j] = repertory_card[i *  GameConstants.MAX_PDK_COUNT_SS + j];
				}
				GRR._card_count[i] = GameConstants.MAX_PDK_COUNT_SS;
				_logic.sort_card_date_list(GRR._cards_data[i], GRR._card_count[i]);
			}
			GRR._left_card_count= 0;
		}else{
			for (int i = 0; i < count; i++) {
				for (int j = 0; j < GameConstants.MAX_PDK_COUNT_EQ; j++) {
					GRR._cards_data[i][j] = repertory_card[i *  GameConstants.MAX_PDK_COUNT_EQ + j];
				}
				GRR._card_count[i] = GameConstants.MAX_PDK_COUNT_EQ;
				_logic.sort_card_date_list(GRR._cards_data[i], GRR._card_count[i]);
			}
			GRR._left_card_count= 0;
		}

		// test_cards();
		// 记录初始牌型
		_recordRoomRecord.setBeginArray(Arrays.toString(repertory_card));

	}

	private void test_cards() {
		int cards[] = new int[] { 0x4E,0x4E,0x4E,0x4E,0x4E,0x4E,0x4E,0x4F,0x4F,0x4F,0x4F,
				0x4F,0x4F};
		
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			for (int j = 0; j < get_hand_card_count_max(); j++) {
				GRR._cards_data[i][j] = 0;
			}
		}
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			for (int j = 0; j < get_hand_card_count_max(); j++) {
				GRR._cards_data[i][j] = cards[j];
			}
		}
		/********
		 * 下面这个给测试用的 如果开发用 把上面的注释去掉 realyCards
		 **************************/

		if (BACK_DEBUG_CARDS_MODE) {
			if (debug_my_cards != null) {
				if(this.is_mj_type(GameConstants.GAME_PLAYER_FPHZ) == false)
				{
					if (debug_my_cards.length > 20) {
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
				else{	
						if (debug_my_cards.length > 15) {
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

	/**
	 * 测试线上 实际牌
	 */
	public void testRealyCard(int[] realyCards) {
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			for (int j = 0; j < GameConstants.MAX_HH_INDEX; j++) {
				GRR._cards_index[i][j] = 0;
			}
		}
		this._repertory_card = realyCards;

		int send_count;
		int have_send_count = 0;
		_banker_select = 0;
		// 分发扑克
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			send_count = (GameConstants.MAX_HH_COUNT - 1);
			GRR._left_card_count -= send_count;

			have_send_count += send_count;
		}
		DEBUG_CARDS_MODE = false;// 把调试模式关闭
		BACK_DEBUG_CARDS_MODE = false;
		System.err.println("=========开始调试线上牌型 调试模式自动关闭*=========");

	}

	/**
	 * 模拟牌型--相同牌
	 */
	public void testSameCard(int[] cards) {
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			for (int j = 0; j < GameConstants.MAX_HH_INDEX; j++) {
				GRR._cards_index[i][j] = 0;
			}
		}
	
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			int send_count = 0;
			if(this.is_mj_type(GameConstants.GAME_PLAYER_FPHZ) == false)
				send_count = (GameConstants.MAX_HH_COUNT - 1);
			else 
				send_count = GameConstants.MAX_FPHZ_COUNT - 1;
		
		}
		DEBUG_CARDS_MODE = false;// 把调试模式关闭
		BACK_DEBUG_CARDS_MODE = false;
	}

	// 摸起来的牌 派发牌
	public boolean dispatch_card_data(int cur_player, int type, boolean tail) {

	

		return true;
	}

	// 游戏结束
	public boolean handler_game_finish(int seat_index, int reason) {
		_game_status = GameConstants.GS_MJ_WAIT;
		boolean ret = false;
		
		if (_cur_round == 1 && (reason == GameConstants.Game_End_NORMAL || reason == GameConstants.Game_End_DRAW)) {
			RoomUtil.realkou_dou(this);
		}
		ret = this.handler_game_finish_pdk(seat_index, reason);

		return ret;
	}

	/**
	 * @return
	 */
	public RoomInfo.Builder getRoomInfo() {
		RoomInfo.Builder room_info = RoomInfo.newBuilder();
		room_info.setRoomId(this.getRoom_id());
		room_info.setGameRuleIndex(_game_rule_index);
		room_info.setGameRuleDes(get_game_des());
		room_info.setGameTypeIndex(_game_type_index);
		room_info.setGameRound(_game_round);
		room_info.setCurRound(_cur_round);
		room_info.setGameStatus(_game_status);
		room_info.setCreatePlayerId(this.getRoom_owner_account_id());
		room_info.setBankerPlayer(this._banker_select);
		room_info.setCreateName(this.getRoom_owner_name());

		int beginLeftCard = GameConstants.CARD_COUNT_HH_YX - 60;
		room_info.setBeginLeftCard(beginLeftCard);
		return room_info;
	}
	public void cal_score_pdk_jd(int end_score[]){
		int win_player=GameConstants.INVALID_SEAT;
		int win_score=0;
		for(int i=0;i<this.getTablePlayerNumber();i++){
			if(this.GRR._card_count[i] == 0){
				win_player=GameConstants.INVALID_SEAT;
				win_player=i;
				GRR._banker_player=win_player;
			}
		}
		
		for(int i=0;i<this.getTablePlayerNumber();i++){
			if(i == win_player){
				continue;
			}
			if(this.GRR._card_count[i]>1){
				if(i == this.GRR._banker_player ){
					//反春天
					if(this._out_card_times[i] == 1 && this.has_rule(GameConstants.GAME_RULE_KE_FAN_DE)){
						end_score[i]-=this.GRR._card_count[i]*2;
					}
					else{
						end_score[i]-=this.GRR._card_count[i];
					}
				}
				else{
					if(this._out_card_times[i] == 0){
						end_score[i]-=this.GRR._card_count[i]*2;
					}
					else{
						end_score[i]-=this.GRR._card_count[i];
					}
				}
				//红桃扎10鸟
				if(this.has_rule(GameConstants.GAME_RULE_HONGTAO10_ZANIAO)){
					if(_hong_tao_palyer == win_player){
						end_score[i]*=2;
					}
					else if(_hong_tao_palyer == i){
						end_score[i]*=2;
					}
				}
				//炸弹分
				end_score[i]-=this._total_boom*10*2;
				win_score+=Math.abs(end_score[i]);
			}
			//放走包赔
			if(this._bao_pei_palyer != GameConstants.INVALID_SEAT){
				if(_bao_pei_palyer == i && end_score[i] < 0){
					this._lose_num[i]++;
				}
			}else{
				if(end_score[i] < 0){
					this._lose_num[i]++;
				}
			}
			
		}
		
		//放走包赔
		if(this._bao_pei_palyer != GameConstants.INVALID_SEAT){
			for(int i=0;i<this.getTablePlayerNumber();i++){
				if(i != _bao_pei_palyer && i != win_player){
					end_score[_bao_pei_palyer]-=Math.abs(end_score[i]);
					end_score[i]=0;
				}
			}
		}
		
		if(win_score > 0){
			this._win_num[win_player]++;
		}
		end_score[win_player]+=win_score;
		
		for(int i=0;i<this.getTablePlayerNumber();i++){
			_game_score[i]+=end_score[i];
		}
	}
	
	public boolean handler_game_finish_pdk(int seat_index, int reason) {
		int real_reason = reason;

		int count = getTablePlayerNumber();
		if (count == 0) {
			count = this.getTablePlayerNumber();
		}
		
		//重制准备
		for (int i = 0; i < count; i++) {
			_player_ready[i] = 0;
		}
		for(int i = 0; i < count; i++){
			if (this._cur_round > 0) {
				// 结束后刷新玩家
				RoomResponse.Builder roomResponse2 = RoomResponse.newBuilder();
				roomResponse2.setGameStatus(_game_status);
				roomResponse2.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);// 刷新玩家
				this.load_player_info_data(roomResponse2);
				this.send_response_to_player(i, roomResponse2);
			}
		}

		
		
		int end_score[]=new int[this.getTablePlayerNumber()];
		Arrays.fill(end_score,0);
		//计算分数
		if(reason == GameConstants.Game_End_NORMAL){
			cal_score_pdk_jd(end_score);
		}
		//最高得分
		for(int i=0;i<this.getTablePlayerNumber();i++){
			if(_game_score[i]>_game_score_max[i]){
				_game_score_max[i]=_game_score[i];
			}
		}
		
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		RoomResponse_PDK.Builder roomResponse_pdk = RoomResponse_PDK.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_GAME_END);
		PukeGameEnd.Builder game_end = PukeGameEnd.newBuilder();

		this.load_common_status(roomResponse);
		this.load_room_info_data(roomResponse);

		RoomInfo.Builder room_info = getRoomInfo();
		game_end.setRoomInfo(room_info);
		if(reason == GameConstants.Game_End_NORMAL){
			game_end.setHongTaoPlayer(_hong_tao_palyer);
			game_end.setBaoPeiPlayer(_bao_pei_palyer);
			game_end.setFanDiPlayer(_fan_di_palyer);
			game_end.setTaoPaoPlayer(_tao_pao_palyer);
			
			//春天玩家
			for(int i=0;i<this.getTablePlayerNumber();i++){
				if(i != GRR._banker_player && this._out_card_times[i]==0){
					if(this._bao_pei_palyer == GameConstants.INVALID_SEAT){
						game_end.addChunTianPlayer(i);
					}else{
						if(this._bao_pei_palyer == i){
							game_end.addChunTianPlayer(i);
						}
					}
					
				}
			}
			
			game_end.setHongTaoPlayer(_hong_tao_palyer);
		}
		this.load_player_info_data(roomResponse);
		game_end.setGameRound(_game_round);
		game_end.setCurRound(_cur_round);
		game_end.setBankerPlayer(GRR._banker_player);// 庄家

		for(int i=0;i<this.getTablePlayerNumber();i++){
			game_end.addCardCount(GRR._card_count[i]);
			game_end.addBoomCardNum(_boom_num[i]);
			game_end.addEndScore(end_score[i]);
			Int32ArrayResponse.Builder cards_card=Int32ArrayResponse.newBuilder();
			for(int j=0;j<GRR._card_count[i];j++){
				cards_card.addItem(GRR._cards_data[i][j]);
			}
			game_end.addCardsData(i,cards_card);
			game_end.addWinNum(this._win_num[i]);
			game_end.addLoseNum(this._lose_num[i]);
		}

		boolean end = false;
		if (reason == GameConstants.Game_End_NORMAL || reason == GameConstants.Game_End_DRAW) {
			if (_cur_round >= _game_round) {// 局数到了
				end = true;
				real_reason = GameConstants.Game_End_ROUND_OVER;
				for(int i=0;i<this.getTablePlayerNumber();i++){
					game_end.addAllBoomCardNum(this._all_boom_num[i]);
					game_end.addAllEndScore(this._game_score[i]);
					game_end.addEndScoreMax(this._game_score_max[i]);
				}
				
			} 
		} else if (reason == GameConstants.Game_End_RELEASE_PLAY || reason == GameConstants.Game_End_RELEASE_NO_BEGIN
				|| reason == GameConstants.Game_End_RELEASE_RESULT
				|| reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_SYSTEM) {
			end = true;
			for(int i=0;i<this.getTablePlayerNumber();i++){
				game_end.addAllBoomCardNum(this._all_boom_num[i]);
				game_end.addAllEndScore(this._game_score[i]);
				game_end.addEndScoreMax(this._game_score_max[i]);
			}
		}
		
		game_end.setReason(real_reason);
		////////////////////////////////////////////////////////////////////// 得分总的
		roomResponse_pdk.setGameEnd(game_end);
		roomResponse.setRoomResponsePdk(roomResponse_pdk);
		this.send_response_to_room(roomResponse);



		// 超时解散
		if (reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT) {
			for (int j = 0; j < this.getTablePlayerNumber(); j++) {
				Player player = this.get_players()[j];
				if (player == null)
					continue;
				send_error_notify(j, 1, "游戏解散成功!");

			}
		}
		
		

		if (end)// 删除
		{
			PlayerServiceImpl.getInstance().delRoomId(this.getRoom_id());
		}


		// 错误断言
		return false;
	}
	/**
	 * 创建房间
	 * 
	 * @return
	 */
	public boolean handler_create_room(Player player, int type, int maxNumber) {
		this.setCreate_type(type);
		this.setCreate_player(player);
		if (type == GameConstants.CREATE_ROOM_PROXY) {
			GameSchedule.put(new CreateTimeOutRunnable(this.getRoom_id()), GameConstants.CREATE_ROOM_PROXY_TIME_GAP,
					TimeUnit.MINUTES);
			// 写入redis
			RedisService redisService = SpringService.getBean(RedisService.class);
			RoomRedisModel roomRedisModel = SpringService.getBean(RedisService.class).hGet(RedisConstant.ROOM,
					getRoom_id() + "", RoomRedisModel.class);
			roomRedisModel.setGameRuleDes(this.get_game_des());
			roomRedisModel.setRoomStatus(this._game_status);
			roomRedisModel.setPlayer_max(maxNumber);
			roomRedisModel.setGame_round(this._game_round);
			redisService.hSet(RedisConstant.ROOM, getRoom_id() + "", roomRedisModel);

			// 发送进入房间
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_CREATE_RROXY_ROOM_SUCCESS);
			load_room_info_data(roomResponse);
			Response.Builder responseBuilder = Response.newBuilder();
			responseBuilder.setResponseType(ResponseType.ROOM);
			responseBuilder.setExtension(Protocol.roomResponse, roomResponse.build());
			PlayerServiceImpl.getInstance().send(player, responseBuilder.build());
			return true;
		}

		// 机器人开房
		if (type == GameConstants.CREATE_ROOM_ROBOT) {
			GameSchedule.put(new CreateTimeOutRunnable(this.getRoom_id()), GameConstants.CREATE_ROOM_PROXY_TIME_GAP,
					TimeUnit.MINUTES);

			// 写入redis
			RedisService redisService = SpringService.getBean(RedisService.class);
			RoomRedisModel roomRedisModel = SpringService.getBean(RedisService.class).hGet(RedisConstant.ROOM,
					getRoom_id() + "", RoomRedisModel.class);
			roomRedisModel.setGameRuleDes(this.get_game_des());
			roomRedisModel.setRoomStatus(this._game_status);
			roomRedisModel.setPlayer_max( RoomComonUtil.getMaxNumber(this));
			roomRedisModel.setGame_round(this._game_round);
			redisService.hSet(RedisConstant.ROOM, getRoom_id() + "", roomRedisModel);

			return true;
		}

		// c成功
		get_players()[0] = player;
		player.set_seat_index(0);

		// 发送进入房间
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_ENTER_ROOM);
		roomResponse.setGameStatus(_game_status);
		roomResponse.setAppId(this.getGame_id());
		//
		this.load_room_info_data(roomResponse);
		this.load_player_info_data(roomResponse);

		return send_response_to_player(player.get_seat_index(), roomResponse);
	}

	// 玩家进入房间
	@Override
	public boolean handler_enter_room(Player player) {

		int seat_index = GameConstants.INVALID_SEAT;

		if (playerNumber == 0) {// 未开始 才分配位置
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				if (get_players()[i] == null) {
					get_players()[i] = player;
					seat_index = i;
					break;
				}
			}
		}

		// if (seat_index == GameConstants.INVALID_SEAT &&
		// player.get_seat_index() != GameConstants.INVALID_SEAT) {
		// Player tarPlayer = get_players()[player.get_seat_index()];
		// if (tarPlayer.getAccount_id() == player.getAccount_id()) {
		// seat_index = player.get_seat_index();
		// }
		// }

		if (seat_index == GameConstants.INVALID_SEAT) {
			player.setRoom_id(0);// 把房间信息清除--
			send_error_notify(player, 1, "游戏已经开始");
			return false;
		}

		player.set_seat_index(seat_index);
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_ENTER_ROOM);
		roomResponse.setGameStatus(_game_status);
		roomResponse.setAppId(this.getGame_id());

		this.load_room_info_data(roomResponse);
		this.load_player_info_data(roomResponse);
		//
		send_response_to_player(player.get_seat_index(), roomResponse);

		roomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);// 进入房间
		send_response_to_other(player.get_seat_index(), roomResponse);

		// 同步数据

		// ========同步到中心========
		RoomRedisModel roomRedisModel = SpringService.getBean(RedisService.class).hGet(RedisConstant.ROOM,
				getRoom_id() + "", RoomRedisModel.class);
		roomRedisModel.getPlayersIdSet().add(player.getAccount_id());
		// 写入redis
		SpringService.getBean(RedisService.class).hSet(RedisConstant.ROOM, getRoom_id() + "", roomRedisModel);

		RedisResponse.Builder redisResponseBuilder = RedisResponse.newBuilder();
		redisResponseBuilder.setRsResponseType(RsResponseType.ACCOUNT_UP);
		//
		RsAccountResponse.Builder rsAccountResponseBuilder = RsAccountResponse.newBuilder();
		rsAccountResponseBuilder.setAccountId(player.getAccount_id());
		rsAccountResponseBuilder.setRoomId(getRoom_id());
		//
		redisResponseBuilder.setRsAccountResponse(rsAccountResponseBuilder);
		RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(), ERedisTopicType.topicAll);

		return true;
	}

	// 玩家进入房间
	@Override
	public boolean handler_reconnect_room(Player player) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_ENTER_ROOM);
		roomResponse.setGameStatus(_game_status);
		roomResponse.setAppId(this.getGame_id());

		//
		this.load_player_info_data(roomResponse);
		this.load_room_info_data(roomResponse);

		send_response_to_player(player.get_seat_index(), roomResponse);

		roomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);// 进入房间
		send_response_to_other(player.get_seat_index(), roomResponse);

		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.cai.common.domain.Room#handler_requst_open_less(com.cai.common.domain
	 * .Player, boolean)
	 */
	@Override
	public boolean handler_requst_open_less(Player player, boolean openThree) {
		return false;
	}

	@Override
	public boolean handler_player_ready_in_gold(int get_seat_index, boolean is_cancel) {
		if (is_cancel) {// 取消准备
			if (this.get_players()[get_seat_index] == null) {
				return false;
			}
			if (GameConstants.GS_MJ_FREE != _game_status && GameConstants.GS_MJ_WAIT != _game_status) {
				return false;
			}
			_player_ready[get_seat_index] = 0;
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_PLAYER_READY);
			roomResponse.setOperatePlayer(get_seat_index);
			roomResponse.setIsCancelReady(is_cancel);
			send_response_to_room(roomResponse);
			if (this._cur_round > 0) {
				// 结束后刷新玩家
				RoomResponse.Builder roomResponse2 = RoomResponse.newBuilder();
				roomResponse2.setGameStatus(_game_status);
				roomResponse2.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);// 刷新玩家
				this.load_player_info_data(roomResponse2);
				this.send_response_to_player(get_seat_index, roomResponse2);
			}
			return false;
		} else {
			return handler_player_ready(get_seat_index,is_cancel);
		}
	}

	@Override
	public boolean handler_player_ready(int seat_index, boolean is_cancel) {
		if (is_cancel) {//
			if (this.get_players()[seat_index] == null) {
				return false;
			}
			if (GameConstants.GS_MJ_FREE != _game_status && GameConstants.GS_MJ_WAIT != _game_status) {
				return false;
			}
			_player_ready[seat_index] = 0;
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_PLAYER_READY);
			roomResponse.setOperatePlayer(seat_index);
			roomResponse.setIsCancelReady(is_cancel);
			send_response_to_room(roomResponse);
			if (this._cur_round > 0) {
				// 结束后刷新玩家
				RoomResponse.Builder roomResponse2 = RoomResponse.newBuilder();
				roomResponse2.setGameStatus(_game_status);
				roomResponse2.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);// 刷新玩家
				this.load_player_info_data(roomResponse2);
				this.send_response_to_player(seat_index, roomResponse2);
			}
			return false;
		}
		if (this.get_players()[seat_index] == null) {
			return false;
		}

		_player_ready[seat_index] = 1;

		if (GameConstants.GS_MJ_FREE != _game_status && GameConstants.GS_MJ_WAIT != _game_status) {
			return false;
		}

		boolean nt = true;
		if (this.get_players()[seat_index].getAccount_id() == this.getRoom_owner_account_id()) {
			nt = false;
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_PLAYER_READY);
		roomResponse.setOperatePlayer(seat_index);
		send_response_to_room(roomResponse);

		if (this._cur_round > 0) {
			// 结束后刷新玩家
			RoomResponse.Builder roomResponse2 = RoomResponse.newBuilder();
			roomResponse2.setGameStatus(_game_status);
			roomResponse2.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);// 刷新玩家
			this.load_player_info_data(roomResponse2);
			this.send_response_to_player(seat_index, roomResponse2);
		}

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (this.get_players()[i] == null) {
				_player_ready[i] = 0;
			}

			if (_player_ready[i] == 0) {
				this.refresh_room_redis_data(GameConstants.PROXY_ROOM_PLAYER, nt);
				return false;
			}
		}
		if (playerNumber == 0) {
			playerNumber = this.getTablePlayerNumber();
		}
		handler_game_start();

		this.refresh_room_redis_data(GameConstants.PROXY_ROOM_UPDATE, nt);
		return false;
	}

	@Override
	public boolean handler_player_be_in_room(int seat_index) {
		this.log_error("gme_status:"+this._game_status+" seat_index:" +seat_index);
		if ((GameConstants.GS_MJ_FREE != _game_status && GameConstants.GS_MJ_WAIT != _game_status)
				&& this.get_players()[seat_index] != null) {
			// this.send_play_data(seat_index);

			this.log_error("gme_status:"+this._game_status+"GS_MJ_WAIT  seat_index:" +seat_index);
			if (this._handler != null){
				this._handler.handler_player_be_in_room(this, seat_index);
			}else{
				RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
				roomResponse.setType(MsgConstants.RESPONSE_RECONNECT_DATA);
				RoomResponse_PDK.Builder roomResponse_pdk = RoomResponse_PDK.newBuilder();
				TableResponse_PDK.Builder tableResponse_pdk = TableResponse_PDK.newBuilder();

				load_room_info_data(roomResponse);
				load_player_info_data(roomResponse);
				load_common_status(roomResponse);
					
				tableResponse_pdk.setBankerPlayer(GRR._banker_player);
				tableResponse_pdk.setCurrentPlayer(_current_player);
				tableResponse_pdk.setPrevPlayer(_prev_palyer);
				
				

				for(int i=0;i<getTablePlayerNumber();i++){
					tableResponse_pdk.addOutCardsCount(GRR._cur_round_count[i]);
					tableResponse_pdk.addPlayerPass(GRR._cur_round_pass[i]);
					Int32ArrayResponse.Builder out_cards = Int32ArrayResponse.newBuilder();
					Int32ArrayResponse.Builder out_change_cards = Int32ArrayResponse.newBuilder();
					for(int j=0;j<GRR._cur_round_count[i];j++){
						out_cards.addItem(GRR._cur_round_data[i][j]);
						out_change_cards.addItem(GRR._cur_change_round_data[i][j]);
					}
					if(this.has_rule(GameConstants.GAME_RULE_DISPLAY_CARD)){
						tableResponse_pdk.addCardCount(GRR._card_count[i]);
					}else{
						if(i == seat_index){
							tableResponse_pdk.addCardCount(GRR._card_count[i]);
						}
						else{
							tableResponse_pdk.addCardCount(-1);
						}
					}
					tableResponse_pdk.addCardType(GRR._cur_card_type[i]);
					tableResponse_pdk.addOutCardsData(i,out_cards);
					Int32ArrayResponse.Builder cards_card=Int32ArrayResponse.newBuilder();
					for(int j=0;j<GRR._card_count[i];j++){
						cards_card.addItem(GameConstants.INVALID_CARD);
					}
					tableResponse_pdk.addCardsData(i,cards_card);
				}

				
				// 手牌--将自己的手牌数据发给自己
				Int32ArrayResponse.Builder cards_card=Int32ArrayResponse.newBuilder();
				for(int j=0;j<GRR._card_count[seat_index];j++){
					cards_card.addItem(GRR._cards_data[seat_index][j]);
				}
				tableResponse_pdk.setCardsData(seat_index,cards_card);
				
				for(int i=0;i<_turn_out_card_count;i++){
					tableResponse_pdk.addPrCardsData(_turn_out_card_data[i]);
				}
				tableResponse_pdk.setPrCardsCount(_turn_out_card_count);
				tableResponse_pdk.setPrOutCardType(_turn_out_card_type);
				tableResponse_pdk.setPrOutCardPlayer(_turn_out__player);

				tableResponse_pdk.setIsFirstOut(2);	
				tableResponse_pdk.setDisplayTime(10);
				tableResponse_pdk.setMagicCard(GameConstants.INVALID_CARD);
					
				roomResponse_pdk.setTableResponse(tableResponse_pdk);
				roomResponse.setRoomResponsePdk(roomResponse_pdk);
				send_response_to_player(seat_index, roomResponse);
			}
				

		}
		if (_gameRoomRecord != null) {
			if (_gameRoomRecord.request_player_seat != GameConstants.INVALID_SEAT) {
				RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
				roomResponse.setType(MsgConstants.RESPONSE_PLAYER_RELEASE);

				SysParamModel sysParamModel3007 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(1)
						.get(3007);
				int delay = 60;
				if (sysParamModel3007 != null) {
					delay = sysParamModel3007.getVal1();
				}

				roomResponse.setReleaseTime(delay);
				roomResponse.setOperateCode(0);
				roomResponse.setRequestPlayerSeat(_gameRoomRecord.request_player_seat);
				roomResponse.setLeftTime((_request_release_time - System.currentTimeMillis()) / 1000);
				for (int i = 0; i < getTablePlayerNumber(); i++) {
					roomResponse.addReleasePlayers(_gameRoomRecord.release_players[i]);
				}
				this.send_response_to_player(seat_index, roomResponse);
			}
		}
		if(this._cur_round > 0 )
			return handler_player_ready(seat_index,false);
		return true;
		

	}

	/**
	 * //用户出牌
	 * 
	 * @param seat_index
	 * @param card
	 * @return
	 */
	@Override
	public boolean handler_player_out_card(int seat_index, int card) {

		return true;
	}
	/**
	 * @param get_seat_index
	 * @param out_cards[]
	 * @param card_count
	 * @param b_out_card
	 * @return
	 */
	public  boolean handler_operate_out_card_mul(int get_seat_index, List<Integer> list,int card_count,int b_out_card){
		if (this._handler_out_card_operate != null) {
			this._handler = this._handler_out_card_operate;
			int out_cards[] = new int [card_count];
			for(int i = 0; i< card_count;i++){
				out_cards[i] = list.get(i);
			}

			this._handler_out_card_operate.reset_status(get_seat_index,out_cards,card_count,b_out_card);
			this._handler.exe(this);
	}
		
		return true;
	}
	
	/***
	 * //用户操作
	 * 
	 * @param seat_index
	 * @param operate_code
	 * @param operate_card
	 * @return
	 */
	@Override
	public boolean handler_operate_card(int seat_index, int operate_code, int operate_card, int luoCode) {


		return true;
	}
	/**
	 * @param get_seat_index
	 * @param operate_code
	 * @return
	 */
	@Override
	public boolean handler_operate_button(int seat_index, int operate_code) {
		return true;
	}
	/**
	 * @param get_seat_index
	 * @param call_banker
	 * @return
	 */
	@Override
	public  boolean handler_call_banker(int seat_index,int call_banker)
	{
		return true;
	}
	/**
	 * @param seat_index
	 * @param jetton
	 * @return
	 */
	@Override
	public  boolean handler_add_jetton( int seat_index, int jetton){
		return true;
	}
	/**
	 * @param seat_index
	 * @param open_flag
	 * @return
	 */
	@Override
	public  boolean handler_open_cards(int seat_index,boolean open_flag){
		return true;
	}
	/**
	 * 释放
	 */
	public boolean process_release_room() {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_FORCE_EXIT);
		this.send_response_to_room(roomResponse);

		if (_cur_round == 0) {
			Player player = null;
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				player = this.get_players()[i];
				if (player == null)
					continue;
				send_error_notify(i, 2, "游戏等待超时解散");

			}
		} else {
			this.handler_game_finish(GameConstants.INVALID_SEAT, GameConstants.Game_End_RELEASE_PLAY);

		}

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			this.get_players()[i] = null;

		}

		if (_table_scheduled != null)
			_table_scheduled.cancel(false);

		// 删除房间
		PlayerServiceImpl.getInstance().delRoomId(this.getRoom_id());

		return true;

	}

	public boolean process_flush_time() {
		long old_flush = super.getLast_flush_time();
		setLast_flush_time(System.currentTimeMillis());
		long new_flush = super.getLast_flush_time();

		// 线程安全，先不要开放，加好锁后再处理
		// //大于10分钟通知redis
		// if(new_flush - old_flush > 1000L*60*10){
		// RoomRedisModel roomRedisModel =
		// SpringService.getBean(RedisService.class).hGet(RedisConstant.ROOM,
		// super.getRoom_id()+"", RoomRedisModel.class);
		// if(roomRedisModel!=null){
		// roomRedisModel.setLast_flush_time(System.currentTimeMillis());
		// //写入redis
		// SpringService.getBean(RedisService.class).hSet(RedisConstant.ROOM,
		// super.getRoom_id()+"", roomRedisModel);
		// }
		// }

		return true;
	}


	@Override
	public Player get_player(long account_id) {
		Player player = null;

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			player = this.get_players()[i];
			if (player != null && player.getAccount_id() == account_id) {
				return player;
			}
		}

		return null;
	}

	@Override
	public boolean handler_audio_chat(Player player, com.google.protobuf.ByteString chat, int l, float audio_len) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_AUDIO_CHAT);
		roomResponse.setOperatePlayer(player.get_seat_index());
		roomResponse.setAudioChat(chat);
		roomResponse.setAudioSize(l);
		roomResponse.setAudioLen(audio_len);
//		this.log_error("nickname = "+player.getNick_name()+"audio_len=" +audio_len + "time="+System.currentTimeMillis());
		this.send_response_to_other(player.get_seat_index(), roomResponse);
		return true;
	}

	@Override
	public boolean handler_emjoy_chat(Player player, int id) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_EMJOY_CHAT);
		roomResponse.setOperatePlayer(player.get_seat_index());
		roomResponse.setEmjoyId(id);
		this.send_response_to_room(roomResponse);

		return true;
	}

	// 18 玩家申请解散房间( operate_code=1发起解散2=同意解散3=不同意解散)
	/*
	 * 
	 * Release_Room_Type_SEND = 1, //发起解散 Release_Room_Type_AGREE, //同意
	 * Release_Room_Type_DONT_AGREE, //不同意 Release_Room_Type_CANCEL,
	 * //还没开始,房主解散房间 Release_Room_Type_QUIT //还没开始,普通玩家退出房间
	 */
	@Override
	public boolean handler_release_room(Player player, int opr_code) {
		int seat_index = 0;
		if (player != null) {
			seat_index = player.get_seat_index();
		}

		SysParamModel sysParamModel3007 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(1).get(3007);
		int delay = 150;
		if (sysParamModel3007 != null) {
			delay = sysParamModel3007.getVal1();
		}
		if (DEBUG_CARDS_MODE)
			delay = 10;
		int playerNumber = getTablePlayerNumber();
		if (playerNumber == 0) {
			playerNumber = this.getTablePlayerNumber();
		}

		switch (opr_code) {
		case GameConstants.Release_Room_Type_SEND: {
			// 发起解散
			if (GameConstants.GS_MJ_FREE == _game_status) {
				// 游戏还没开始,不能发起解散
				return false;
			}
			// 有人申请解散了
			if (_gameRoomRecord.request_player_seat != GameConstants.INVALID_SEAT) {
				return false;
			}

			// 取消之前的调度
			if (_release_scheduled != null)
				_release_scheduled.cancel(false);
			_request_release_time = System.currentTimeMillis() + delay * 1000;
			if (GRR == null) {
				_release_scheduled = GameSchedule.put(new GameFinishRunnable(this.getRoom_id(), seat_index,
						GameConstants.Game_End_RELEASE_WAIT_TIME_OUT), delay, TimeUnit.SECONDS);
			} else {
				_release_scheduled = GameSchedule.put(new GameFinishRunnable(this.getRoom_id(), seat_index,
						GameConstants.Game_End_RELEASE_PLAY_TIME_OUT), delay, TimeUnit.SECONDS);
			}

			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				_gameRoomRecord.release_players[i] = 0;
			}

			_gameRoomRecord.request_player_seat = seat_index;
			_gameRoomRecord.release_players[seat_index] = 1;// 同意

			// 不在线的玩家默认同意
			// for (int i = 0; i < MJGameConstants.GAME_PLAYER; i++) {
			// Player pl = this.get_players()[i];
			// if(pl==null || pl.isOnline()==false){
			// _gameRoomRecord.release_players[i] = 1;//同意
			// }
			// }
			int count = 0;
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				if (_gameRoomRecord.release_players[i] == 1) {// 都同意了

					count++;
				}
			}
			if (count == this.getTablePlayerNumber()) {
				if (_game_status == GameConstants.GS_MJ_WAIT) {
					this.handler_game_finish(GameConstants.INVALID_SEAT, GameConstants.Game_End_RELEASE_PLAY);
				} else {
					this.handler_game_finish(GameConstants.INVALID_SEAT, GameConstants.Game_End_RELEASE_PLAY);
				}

				for (int j = 0; j < this.getTablePlayerNumber(); j++) {
					player = this.get_players()[j];
					if (player == null)
						continue;
					send_error_notify(j, 1, "游戏解散成功!");

				}
				return true;
			}
 
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_PLAYER_RELEASE);
			roomResponse.setReleaseTime(delay);
			roomResponse.setOperateCode(0);
			roomResponse.setRequestPlayerSeat(_gameRoomRecord.request_player_seat);
			roomResponse.setLeftTime(delay);
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				roomResponse.addReleasePlayers(_gameRoomRecord.release_players[i]);
			}
			this.send_response_to_room(roomResponse);

			return false;
		}

		case GameConstants.Release_Room_Type_AGREE: {
			if (GameConstants.GS_MJ_FREE == _game_status) {
				// 游戏还没开始,不能同意解散
				return false;
			}
			// 没人发起解散
			if (_gameRoomRecord.request_player_seat == GameConstants.INVALID_SEAT) {
				return false;
			}
			if (_gameRoomRecord.release_players[seat_index] == 1)
				return false;

			_gameRoomRecord.release_players[seat_index] = 1;

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_PLAYER_RELEASE);
			roomResponse.setReleaseTime(delay);
			roomResponse.setRequestPlayerSeat(_gameRoomRecord.request_player_seat);
			roomResponse.setOperateCode(1);
			roomResponse.setLeftTime((_request_release_time - System.currentTimeMillis()) / 1000);
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				roomResponse.addReleasePlayers(_gameRoomRecord.release_players[i]);
			}

			this.send_response_to_room(roomResponse);

			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				if (_gameRoomRecord.release_players[i] != 1) {
					return false;// 有一个不同意
				}
			}

			for (int j = 0; j < this.getTablePlayerNumber(); j++) {
				player = this.get_players()[j];
				if (player == null)
					continue;
				send_error_notify(j, 1, "游戏解散成功!");

			}

			if (_release_scheduled != null)
				_release_scheduled.cancel(false);
			_release_scheduled = null;
			if (_game_status == GameConstants.GS_MJ_WAIT) {
				this.handler_game_finish(GameConstants.INVALID_SEAT, GameConstants.Game_End_RELEASE_PLAY);
			} else {
				this.handler_game_finish(GameConstants.INVALID_SEAT, GameConstants.Game_End_RELEASE_PLAY);
			}

			return false;
		}

		case GameConstants.Release_Room_Type_DONT_AGREE: {
			if (GameConstants.GS_MJ_FREE == _game_status) {
				// 游戏还没开始,不能同意解散
				return false;
			}
			if (_gameRoomRecord.request_player_seat == GameConstants.INVALID_SEAT) {
				return false;
			}
			_gameRoomRecord.release_players[seat_index] = 2;
			_gameRoomRecord.request_player_seat = GameConstants.INVALID_SEAT;

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setReleaseTime(delay);
			roomResponse.setType(MsgConstants.RESPONSE_PLAYER_RELEASE);
			roomResponse.setRequestPlayerSeat(_gameRoomRecord.request_player_seat);
			roomResponse.setOperateCode(1);
			int l = (int) (_request_release_time - System.currentTimeMillis() / 1000);
			if (l <= 0) {
				l = 1;
			}
			roomResponse.setLeftTime(l);
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				roomResponse.addReleasePlayers(_gameRoomRecord.release_players[i]);
			}
			this.send_response_to_room(roomResponse);

			_request_release_time = 0;
			_gameRoomRecord.request_player_seat = GameConstants.INVALID_SEAT;
			if (_release_scheduled != null)
				_release_scheduled.cancel(false);
			_release_scheduled = null;
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				_gameRoomRecord.release_players[i] = 0;
			}

			for (int j = 0; j < this.getTablePlayerNumber(); j++) {
				player = this.get_players()[j];
				if (player == null)
					continue;
				send_error_notify(j, 1, "游戏解散失败!玩家[" + this.get_players()[seat_index].getNick_name() + "]不同意解散");

			}

			return false;
		}

		case GameConstants.Release_Room_Type_CANCEL: {// 房主未开始游戏 解散
			if (GameConstants.GS_MJ_FREE != _game_status) {
				// 游戏已经来时
				return false;
			}
			if (_cur_round == 0) {
				// _gameRoomRecord.request_player_seat =
				// MJGameConstants.INVALID_SEAT;

				RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
				roomResponse.setType(MsgConstants.RESPONSE_FORCE_EXIT);// 直接拉出游戏
				this.send_response_to_room(roomResponse);

				for (int i = 0; i < this.getTablePlayerNumber(); i++) {
					Player p = this.get_players()[i];
					if (p == null)
						continue;
					if (i == seat_index) {
						send_error_notify(i, 2, "游戏已解散");
					} else {
						send_error_notify(i, 2, "游戏已被创建者解散");
					}

				}
				this.huan_dou(GameConstants.Game_End_RELEASE_NO_BEGIN);
				// 删除房间
				PlayerServiceImpl.getInstance().delRoomId(this.getRoom_id());
			} else {
				return false;

			}
		}
			break;
		case GameConstants.Release_Room_Type_QUIT: {// 玩家未开始游戏 退出
			if (GameConstants.GS_MJ_FREE != _game_status) {
				// 游戏已经开始
				return false;
			}
			RoomResponse.Builder quit_roomResponse = RoomResponse.newBuilder();
			quit_roomResponse.setType(MsgConstants.RESPONSE_FORCE_EXIT);// 直接拉出游戏
			send_response_to_player(seat_index, quit_roomResponse);

			send_error_notify(seat_index, 2, "您已退出该游戏");
			this.get_players()[seat_index] = null;
			_player_ready[seat_index] = 0;

			if (player != null) {
				PlayerServiceImpl.getInstance().quitRoomId(this.getRoom_id(), player.getAccount_id());
			}
			if (_kick_schedule != null) {
				_kick_schedule.cancel(false);
				_kick_schedule = null;
			}
			// 刷新玩家
			RoomResponse.Builder refreshroomResponse = RoomResponse.newBuilder();
			refreshroomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);
			this.load_player_info_data(refreshroomResponse);
			//
			send_response_to_other(seat_index, refreshroomResponse);

			// 通知代理
			this.refresh_room_redis_data(GameConstants.PROXY_ROOM_PLAYER, false);
		}
			break;
		case GameConstants.Release_Room_Type_PROXY: {
			// 游戏还没开始,不能解散
			if (GameConstants.GS_MJ_FREE != _game_status) {
				return false;
			}

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_FORCE_EXIT);// 直接拉出游戏
			this.send_response_to_room(roomResponse);

			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				Player p = this.get_players()[i];
				if (p == null)
					continue;
				send_error_notify(i, 2, "游戏已被创建者解散");

			}
			this.huan_dou(GameConstants.Game_End_RELEASE_NO_BEGIN);
			// 删除房间
			PlayerServiceImpl.getInstance().delRoomId(this.getRoom_id());

		}
			break;
		}

		return true;

	}

	@Override
	public boolean handler_requst_pao_qiang(Player player, int pao, int qiang) {

		return false;

	}
	@Override
	public boolean handler_requst_nao_zhuang(Player player, int nao) {

		return true;
	}
	/////////////////////////////////////////////////////// send///////////////////////////////////////////
	/////
	/**
	 * 基础状态
	 * 
	 * @return
	 */
	public boolean operate_player_status() {
		//// 发送玩家出牌 201 =玩家出牌(CMD_OutCard: operate_player,operate_card)
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_PLAYER_STATUS);//29
		this.load_common_status(roomResponse);
		return this.send_response_to_room(roomResponse);
	}

	/**
	 * 刷新玩家信息
	 * 
	 * @return
	 */
	public boolean operate_player_data() {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);
		this.load_player_info_data(roomResponse);

		GRR.add_room_response(roomResponse);

		return this.send_response_to_room(roomResponse);
	}

	/**
	 * 在玩家的前面显示出的牌 --- 发送玩家出牌
	 * 
	 * @param seat_index
	 * @param count
	 * @param cards
	 * @param type
	 * @param to_player
	 * @return
	 */
	//
	public boolean operate_out_card(int seat_index, int count, int cards_data[], int type, int to_player) {
		if (seat_index == GameConstants.INVALID_SEAT) {
			return false;
		}
		//// 发送玩家出牌 201 =玩家出牌(CMD_OutCard: operate_player,operate_card)
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		RoomResponse_PDK.Builder roomResponse_pdk = RoomResponse_PDK.newBuilder();
		OutCardData.Builder outcarddata=OutCardData.newBuilder();
		Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_OUT_CARD);//201
		roomResponse.setTarget(seat_index);

		for (int j = 0; j < count; j++) {
			outcarddata.addCardsData(cards_data[j]);
		}
		//上一出牌数据
		outcarddata.setPrCardsCount(this._turn_out_card_count);
		for(int i=0;i<this._turn_out_card_count;i++){
			outcarddata.addPrCardsData(this._turn_out_card_data[i]);
		}
		outcarddata.setCardsCount(count);
		outcarddata.setOutCardPlayer(seat_index);
		outcarddata.setCardType(type);
		outcarddata.setCurPlayer(this._current_player);
		outcarddata.setPrOutCardType(this._turn_out_card_type);
		if(_turn_out_card_count == 0){
			outcarddata.setIsFirstOut(true);
		}
		else{
			outcarddata.setIsFirstOut(false);
		}

		roomResponse_pdk.setOutCard(outcarddata);
		roomResponse.setRoomResponsePdk(roomResponse_pdk);
		if (to_player == GameConstants.INVALID_SEAT) {
			GRR.add_room_response(roomResponse);
			return this.send_response_to_room(roomResponse);
		} else {
			return this.send_response_to_player(to_player, roomResponse);
		}

	}

	/**
	 * 效果 (通知玩家弹出 吃碰杆 胡牌==效果)
	 * 
	 * @param seat_index
	 * @param effect_type
	 * @param effect_count
	 * @param effect_indexs
	 * @param time
	 * @param to_player
	 * @return
	 */
	public boolean operate_effect_action(int seat_index, int effect_type, int effect_count, long effect_indexs[],
			int time, int to_player) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_EFFECT_ACTION);
		roomResponse.setEffectType(effect_type);
		roomResponse.setTarget(seat_index);
		roomResponse.setEffectCount(effect_count);
		for (int i = 0; i < effect_count; i++) {
			roomResponse.addEffectsIndex(effect_indexs[i]);
		}

		roomResponse.setEffectTime(time);

		if (to_player == GameConstants.INVALID_SEAT) {
			GRR.add_room_response(roomResponse);
			this.send_response_to_room(roomResponse);
		} else {
			this.send_response_to_player(to_player, roomResponse);
		}

		return true;
	}

	public boolean record_effect_action(int seat_index, int effect_type, int effect_count, long effect_indexs[],
			int time) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_EFFECT_ACTION);
		roomResponse.setEffectType(effect_type);
		roomResponse.setTarget(seat_index);
		roomResponse.setEffectCount(effect_count);
		for (int i = 0; i < effect_count; i++) {
			roomResponse.addEffectsIndex(effect_indexs[i]);
		}

		roomResponse.setEffectTime(time);

		GRR.add_room_response(roomResponse);

		return true;
	}

	/**
	 * 刷新玩家的牌--手牌 和 牌堆上的牌
	 * 
	 * @param seat_index
	 * @param card_count
	 * @param cards
	 *            实际的牌数据(手上的牌)
	 * @param weave_count
	 * @param weaveitems
	 *            牌堆中的组合(落地的牌)
	 * @return
	 */
	public boolean operate_player_cards() {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYER_CARDS);// 12更新玩家手牌数据
		roomResponse.setGameStatus(_game_status);
		roomResponse.setCardType(1);
		
		RoomResponse_PDK.Builder roomResponse_pdk = RoomResponse_PDK.newBuilder();
		for(int play_index=0;play_index<this.getTablePlayerNumber();play_index++){
			//刷新玩家手牌数量
			RefreshCards.Builder refreshcards=RefreshCards.newBuilder();
			for(int i=0;i<this.getTablePlayerNumber();i++){
				refreshcards.addCardCount(GRR._card_count[i]);
				
				Int32ArrayResponse.Builder cards_card=Int32ArrayResponse.newBuilder();
				for(int j=0;j<GRR._card_count[i];j++){
					cards_card.addItem(GameConstants.INVALID_CARD);
				}
				refreshcards.addCardsData(i,cards_card);
			}
			
			// 手牌--将自己的手牌数据发给自己
			Int32ArrayResponse.Builder cards_card=Int32ArrayResponse.newBuilder();
			for(int j=0;j<GRR._card_count[play_index];j++){
				cards_card.addItem(GRR._cards_data[play_index][j]);
			}
			refreshcards.setCardsData(play_index,cards_card);
			roomResponse_pdk.setRefreshCards(refreshcards);
			roomResponse.setRoomResponsePdk(roomResponse_pdk);
			GRR.add_room_response(roomResponse);
			
			// 自己才有牌数据
			this.send_response_to_player(play_index, roomResponse);
		}
		return true;
	}
	/***
	 * 刷新特殊描述
	 * 
	 * @param txt
	 * @param type
	 * @return
	 */
	public boolean operate_especial_txt(String txt, int type) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_GAME_ESPECIAL_TXT);
		roomResponse.setEspecialTxtType(type);
		roomResponse.setEspecialTxt(txt);
		this.send_response_to_room(roomResponse);
		return true;
	}

	public boolean handler_player_offline(Player player) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setGameStatus(_game_status);
		roomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);// 刷新玩家
		this.load_player_info_data(roomResponse);

		send_response_to_other(player.get_seat_index(), roomResponse);
		return true;
	}

	public boolean send_play_data(int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_RECONNECT_DATA);

		TableResponse.Builder tableResponse = TableResponse.newBuilder();

		load_room_info_data(roomResponse);
		load_player_info_data(roomResponse);

		this.load_common_status(roomResponse);
		// 游戏变量
		tableResponse.setBankerPlayer(GRR._banker_player);
		tableResponse.setCurrentPlayer(_current_player);
		tableResponse.setCellScore(0);

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			tableResponse.addTrustee(false);// 是否托管
			// 剩余牌数
			tableResponse.addDiscardCount(GRR._discard_count[i]);
			Int32ArrayResponse.Builder int_array = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < 55; j++) {
				int_array.addItem(GRR._discard_cards[i][j]);
			}
			tableResponse.addDiscardCards(int_array);

		}


		roomResponse.setTable(tableResponse);

		this.send_response_to_player(seat_index, roomResponse);

		return true;

	}

	private PlayerResultResponse.Builder process_player_result(int reason) {
		this.huan_dou(reason);
		// 大赢家
		int count = getTablePlayerNumber();
		if (count == 0) {
			count = this.getTablePlayerNumber();
		}
		for (int i = 0; i < count; i++) {
			_player_result.win_order[i] = -1;
		}
		int win_idx = 0;
		float max_score = 0;
		for (int i = 0; i < count; i++) {
			int winner = -1;
			float s = -999999;
			for (int j = 0; j < count; j++) {
				if (_player_result.win_order[j] != -1) {
					continue;
				}
				if (_player_result.game_score[j] > s) {
					s = _player_result.game_score[j];
					winner = j;
				}
			}
			if (s >= max_score) {
				max_score = s;
			} else {
				win_idx++;
			}
			if (winner != -1) {
				_player_result.win_order[winner] = win_idx;
				// win_idx++;
			}
		}

		PlayerResultResponse.Builder player_result = PlayerResultResponse.newBuilder();

		for (int i = 0; i < count; i++) {
			player_result.addGameScore(_player_result.game_score[i]);
			player_result.addWinOrder(_player_result.win_order[i]);

			Int32ArrayResponse.Builder lfs = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < count; j++) {
				lfs.addItem(_player_result.lost_fan_shu[i][j]);
			}

			player_result.addLostFanShu(lfs);

			player_result.addHuPaiCount(_player_result.hu_pai_count[i]);
			player_result.addMingTangCount(_player_result.ming_tang_count[i]);
			player_result.addYingXiCount(_player_result.ying_xi_count[i]);

			player_result.addPlayersId(i);
			// }
		}

		player_result.setRoomId(this.getRoom_id());
		player_result.setRoomOwnerAccountId(this.getRoom_owner_account_id());
		player_result.setRoomOwnerName(this.getRoom_owner_name());
		player_result.setCreateTime(this.getCreate_time());
		player_result.setRecordId(this.get_record_id());
		player_result.setGameRound(_game_round);
		player_result.setGameRuleDes(get_game_des());
		player_result.setGameRuleIndex(_game_rule_index);
		player_result.setGameTypeIndex(_game_type_index);
		return player_result;
	}

	/**
	 * 加载基础状态 癞子 定鬼 牌型状态 玩家状态
	 * 
	 * @param roomResponse
	 */
	public void load_common_status(RoomResponse.Builder roomResponse) {
		roomResponse.setCurrentPlayer(_current_player);
		roomResponse.setGameStatus(_game_status);

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			roomResponse.addCardStatus(_playerStatus[i]._card_status);
			roomResponse.addPlayerStatus(_playerStatus[i].get_status());
		}
	}




	/***
	 * 加载房间的玩法 状态信息
	 * 
	 * @param roomResponse
	 */
	public void load_room_info_data(RoomResponse.Builder roomResponse) {
		RoomInfo.Builder room_info = getRoomInfo();
		roomResponse.setRoomInfo(room_info);
	}

	/**
	 * 加载房间里的玩家信息
	 * 
	 * @param roomResponse
	 */
	public void load_player_info_data(RoomResponse.Builder roomResponse) {
		Player rplayer;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			rplayer = this.get_players()[i];
			if (rplayer == null)
				continue;
			RoomPlayerResponse.Builder room_player = RoomPlayerResponse.newBuilder();
			room_player.setAccountId(rplayer.getAccount_id());
			room_player.setHeadImgUrl(rplayer.getAccount_icon());
			room_player.setIp(rplayer.getAccount_ip());
			room_player.setUserName(rplayer.getNick_name());
			room_player.setSeatIndex(rplayer.get_seat_index());
			room_player.setOnline(rplayer.isOnline() ? 1 : 0);
			room_player.setIpAddr(rplayer.getAccount_ip_addr());
			room_player.setSex(rplayer.getSex());
			room_player.setScore(_player_result.game_score[i]);
			room_player.setReady(_player_ready[i]);
			room_player.setPao(_player_result.pao[i] < 0 ? 0 : _player_result.pao[i]);
			room_player.setQiang(_player_result.qiang[i]);
			room_player.setMoney(rplayer.getMoney());
			room_player.setGold(rplayer.getGold());
			if (rplayer.locationInfor != null) {
				room_player.setLocationInfor(rplayer.locationInfor);
			}
			roomResponse.addPlayers(room_player);
		}
	}

	public boolean send_response_to_player(int seat_index, RoomResponse.Builder roomResponse) {

		roomResponse.setRoomId(super.getRoom_id());// 日志用的
		if (this.get_players()[seat_index] == null) {
			return false;
		}
		Response.Builder responseBuilder = Response.newBuilder();
		responseBuilder.setResponseType(ResponseType.ROOM);
		responseBuilder.setExtension(Protocol.roomResponse, roomResponse.build());
		PlayerServiceImpl.getInstance().send(this.get_players()[seat_index], responseBuilder.build());
		return true;
	}

	public boolean send_sys_response_to_player(int seat_index, String msg) {
		if (this.get_players()[seat_index] == null) {
			return false;
		}

		Response.Builder responseBuilder = Response.newBuilder();
		responseBuilder.setResponseType(ResponseType.MSG);
		MsgAllResponse.Builder msgBuilder = MsgAllResponse.newBuilder();
		msgBuilder.setType(ESysMsgType.NONE.getId());
		msgBuilder.setMsg(msg);
		msgBuilder.setErrorId(EMsgIdType.ROOM_ERROR.getId());
		responseBuilder.setExtension(Protocol.msgAllResponse, msgBuilder.build());
		PlayerServiceImpl.getInstance().send(this.get_players()[seat_index], responseBuilder.build());
		return true;
	}

	public boolean send_response_to_room(RoomResponse.Builder roomResponse) {

		roomResponse.setRoomId(super.getRoom_id());// 日志用的
		Player player = null;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			player = this.get_players()[i];
			if (player == null)
				continue;
			Response.Builder responseBuilder = Response.newBuilder();
			responseBuilder.setResponseType(ResponseType.ROOM);
			responseBuilder.setExtension(Protocol.roomResponse, roomResponse.build());

			PlayerServiceImpl.getInstance().send(this.get_players()[i], responseBuilder.build());
		}

		return true;
	}

	public boolean send_error_notify(int seat_index, int type, String msg) {
		MsgAllResponse.Builder e = MsgAllResponse.newBuilder();
		e.setType(type);
		e.setMsg(msg);
		Response.Builder responseBuilder = Response.newBuilder();
		responseBuilder.setResponseType(ResponseType.MSG);
		responseBuilder.setExtension(Protocol.msgAllResponse, e.build());
		PlayerServiceImpl.getInstance().send(this.get_players()[seat_index], responseBuilder.build());

		return false;

	}

	public boolean send_error_notify(Player player, int type, String msg) {
		MsgAllResponse.Builder e = MsgAllResponse.newBuilder();
		e.setType(type);
		e.setMsg(msg);
		Response.Builder responseBuilder = Response.newBuilder();
		responseBuilder.setResponseType(ResponseType.MSG);
		responseBuilder.setExtension(Protocol.msgAllResponse, e.build());
		PlayerServiceImpl.getInstance().send(player, responseBuilder.build());

		return false;

	}

	public boolean send_response_to_other(int seat_index, RoomResponse.Builder roomResponse) {

		roomResponse.setRoomId(super.getRoom_id());// 日志用的
		Player player = null;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			player = this.get_players()[i];
			if (player == null)
				continue;
			if (i == seat_index)
				continue;
			Response.Builder responseBuilder = Response.newBuilder();
			responseBuilder.setResponseType(ResponseType.ROOM);
			responseBuilder.setExtension(Protocol.roomResponse, roomResponse.build());
//			this.log_error("nickname = "+this.get_players()[seat_index].getNick_name() + "nickname =  "+this.get_players()[i].getNick_name() +roomResponse.getAudioLen()+ "time=" + System.currentTimeMillis());
			PlayerServiceImpl.getInstance().send(this.get_players()[i], responseBuilder.build());
		}

		return true;
	}

	public boolean send_response_to_special(int seat_index, int to_player, RoomResponse.Builder roomResponse) {

		roomResponse.setRoomId(super.getRoom_id());// 日志用的
		Player player = null;

		player = this.get_players()[to_player];
		if (player == null)
			return true;
		if (to_player == seat_index)
			return true;
		Response.Builder responseBuilder = Response.newBuilder();
		responseBuilder.setResponseType(ResponseType.ROOM);
		responseBuilder.setExtension(Protocol.roomResponse, roomResponse.build());

		PlayerServiceImpl.getInstance().send(this.get_players()[to_player], responseBuilder.build());

		return true;
	}

	private void progress_banker_select() {
		if (_banker_select == GameConstants.INVALID_SEAT) {
			_banker_select = 0;// 创建者的玩家为专家
		}
	}

	private void record_game_room() {
		// 第一局开始
		_gameRoomRecord = new GameRoomRecord();

		this.set_record_id(BrandIdDict.getInstance().getId());

		_gameRoomRecord.set_record_id(this.get_record_id());
		_gameRoomRecord.setRoom_id(this.getRoom_id());
		_gameRoomRecord.setRoom_owner_account_id(this.getRoom_owner_account_id());
		_gameRoomRecord.setCreate_time(this.getCreate_time());
		_gameRoomRecord.setRoom_owner_name(this.getRoom_owner_name());
		_gameRoomRecord.set_player(_player_result);
		_gameRoomRecord.setPlayers(this.get_players());

		_gameRoomRecord.request_player_seat = GameConstants.INVALID_SEAT;

		// 设置战绩游戏ID
		_gameRoomRecord.setGame_id(GameConstants.GAME_ID_PDK);
		_recordRoomRecord = MongoDBServiceImpl.getInstance().parentBrand(_gameRoomRecord.getGame_id(),
				this.get_record_id(), "", _gameRoomRecord.to_json(), (long) this._game_round,
				(long) this._game_type_index, this.getRoom_id() + "", getRoom_owner_account_id());

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			MongoDBServiceImpl.getInstance().accountBrand(_gameRoomRecord.getGame_id(),
					this.get_players()[i].getAccount_id(), this.get_record_id(), getRoom_owner_account_id());
		}

	}

	public boolean is_mj_type(int type) {
		return _game_type_index == type;
	}

	private void record_game_round(GameEndResponse.Builder game_end) {
		if (_recordRoomRecord != null) {
			_recordRoomRecord.setMsg(_gameRoomRecord.to_json());
			MongoDBServiceImpl.getInstance().updateParenBrand(_recordRoomRecord);
		}

		if (GRR != null) {
			game_end.setRecord(GRR.get_video_record());
			long id = BrandIdDict.getInstance().getId();
			String stl = String.valueOf(id);
			game_end.setBrandIdStr(stl);
			game_end.setBrandId(id);

			GameEndResponse ge = game_end.build();

			byte[] gzipByte = ZipUtil.gZip(ge.toByteArray());

			// 记录 to mangodb
			MongoDBServiceImpl.getInstance().childBrand(_gameRoomRecord.getGame_id(), id, this.get_record_id(), "",
					null, null, gzipByte, this.getRoom_id() + "",
					_recordRoomRecord == null ? "" : _recordRoomRecord.getBeginArray(), getRoom_owner_account_id());

		}

		if ((cost_dou > 0) && (this._cur_round == 1)) {
			// 不是正常结束的
			if ((game_end.getEndType() != GameConstants.Game_End_NORMAL)
					&& (game_end.getEndType() != GameConstants.Game_End_DRAW)) {
				// 还豆
				StringBuilder buf = new StringBuilder();
				buf.append("开局失败[" + game_end.getEndType() + "]" + ":" + this.getRoom_id())
						.append("game_id:" + this.getGame_id()).append(",game_type_index:" + _game_type_index)
						.append(",game_round:" + _game_round).append(",房主:" + this.getRoom_owner_account_id())
						.append(",豆+:" + cost_dou);
				// 把豆还给玩家
				AddGoldResultModel result = PlayerServiceImpl.getInstance().addGold(this.getRoom_owner_account_id(),
						cost_dou, false, buf.toString(), EGoldOperateType.FAILED_ROOM);
				if (result.isSuccess() == false) {
					logger.error("房间[" + this.getRoom_id() + "]" + "玩家[" + this.getRoom_owner_account_id() + "]还豆失败");
				}

			}
		}

	}

	public void log_error(String error) {

		logger.error("房间[" + this.getRoom_id() + "]" + error);

	}

	public void log_player_error(int seat_index, String error) {

		logger.error("房间[" + this.getRoom_id() + "]" + " 玩家[" + seat_index + "]" + error);

	}

 

	public String get_game_des() {
		return GameDescUtil.getGameDesc(_game_type_index, _game_rule_index);
	}



	/**
	 * 取出实际牌数据
	 * 
	 * @param card
	 * @return
	 */
	public int get_real_card(int card) {
		return card;
	}

	/**
	 * 结束调度
	 * 
	 * @return
	 */
	public boolean exe_finish() {

		this._handler = this._handler_finish;
//		this._handler_finish.exe(this);
		return true;
	}

	public boolean exe_add_discard(int seat_index, int card_count, int card_data[], boolean send_client, int delay) {
		if (delay == 0) {
			this.runnable_add_discard(seat_index, card_count, card_data, send_client);
			return true;
		}
		GameSchedule.put(
				new AddDiscardRunnable(getRoom_id(), seat_index, card_count, card_data, send_client, getMaxCount()),
				delay, TimeUnit.MILLISECONDS);

		return true;

	}



	@Override
	public boolean runnable_gang_card_data(int seat_index, int provide_player, int center_card, int action, int type,
			boolean depatch, boolean self, boolean d) {
	
		return true;

	}

	/**
	 * 
	 * @param seat_index
	 * @param provide_player
	 * @param center_card
	 * @param action
	 * @param type
	 *            //共杠还是明杠
	 * @param self
	 *            自己摸的
	 * @param d
	 *            双杠
	 * @return
	 */

	/**
	 * 切换到出牌处理器
	 * 
	 * @param seat_index
	 * @param card
	 * @return
	 */
	public boolean exe_out_card(int seat_index, int card, int type) {
		

		return true;
	}

	public boolean exe_chi_peng(int seat_index, int provider, int action, int card, int type,int lou_operate) {
	

		return true;
	}

	public boolean exe_jian_pao_hu(int seat_index, int action, int card) {

		GameSchedule.put(new JianPaoHuRunnable(this.getRoom_id(), seat_index, action, card),
				GameConstants.DELAY_AUTO_OUT_CARD_TRUTESS, TimeUnit.MILLISECONDS);

		return true;
	}

	/**
	 * 调度,加入牌堆
	 **/
	public void runnable_add_discard(int seat_index, int card_count, int card_data[], boolean send_client) {
	}

	/**
	 * 显示中间出的牌
	 * 
	 * @param seat_index
	 */
	public void runnable_remove_middle_cards(int seat_index) {
		
	}

	/**
	 * 
	 * @param seat_index
	 * @param type
	 */
	public void runnable_remove_out_cards(int seat_index, int type) {
		// 删掉出来的那张牌
		operate_out_card(seat_index, 0, null, type, GameConstants.INVALID_SEAT);
	}

	public void runnable_create_time_out() {
		// 已经开始
		if (this._game_status != GameConstants.GS_MJ_FREE) {
			return;
		}

		// 把豆还给创建的人
		this.huan_dou(GameConstants.Game_End_RELEASE_SYSTEM);

		process_release_room();
	}

	/***
	 * 强制解散
	 */
	public boolean force_account() {
		if (this._cur_round == 0) {
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_FORCE_EXIT);// 直接拉出游戏
			this.send_response_to_room(roomResponse);

			Player player = null;
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				player = this.get_players()[i];
				if (player == null)
					continue;
				send_error_notify(i, 2, "游戏已被系统解散");
			}
			// 删除房间
			PlayerServiceImpl.getInstance().delRoomId(this.getRoom_id());

		} else {
			this.handler_game_finish(GameConstants.INVALID_SEAT, GameConstants.Game_End_RELEASE_SYSTEM);
		}

		return false;
	}

	private void shuffle_players() {
		List<Player> pl = new ArrayList<Player>();
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			pl.add(this.get_players()[i]);
		}

		Collections.shuffle(pl);
		pl.toArray(this.get_players());

		// 重新排下位置
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			get_players()[i].set_seat_index(i);
		}
	}

	public static void main(String[] args) {
		int value = 0x00000200;
		int value1 = 0x200;

		System.out.println(value);
		System.out.println(value1);

	}

	public boolean handler_requst_location(Player player, LocationInfor locationInfor) {
		// LocationUtil.LantitudeLongitudeDist(lon1, lat1, lon2, lat2);
		// 发送数据
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			this.load_player_info_data(roomResponse);
			roomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);
			this.send_response_to_player(i, roomResponse);
		}

		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.cai.common.domain.Room#handler_request_trustee(int, boolean)
	 */
	@Override
	public boolean handler_request_trustee(int get_seat_index, boolean isTrustee) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_IS_TRUSTEE);
		roomResponse.setOperatePlayer(get_seat_index);
		roomResponse.setIstrustee(isTrustee);
		boolean isTing = _playerStatus[get_seat_index]._hu_card_count <= 0 ? false : true;
		roomResponse.setIsTing(isTing);
		if (isTrustee && !isTing) {
			roomResponse.setIstrustee(false);
			send_response_to_player(get_seat_index, roomResponse);
			return false;
		}
		if (isTrustee && SysParamUtil.is_auto(GameConstants.GAME_ID_FLS_LX)) {
			istrustee[get_seat_index] = isTrustee;
		}
		this.send_response_to_room(roomResponse);
		GRR.add_room_response(roomResponse);
		return false;
	}

	@Override
	public boolean runnable_dispatch_last_card_data(int cur_player, int type, boolean tail) {

		return true;
	}

	@Override
	public boolean runnable_chuli_first_card_data(int _seat_index, int _type, boolean _tail) {

		return true;
	}

	@Override
	public boolean runnable_dispatch_first_card_data(int _seat_index, int _type, boolean _tail) {

		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.cai.common.domain.Room#getTablePlayerNumber()
	 */
	@Override
	public int getTablePlayerNumber() {
		if(this.has_rule(GameConstants.GAME_RULE_THREE_PLAY)){
			return 3;
		}
		else if(this.has_rule(GameConstants.GAME_RULE_TWO_PLAY)){
			return 2;
		}
		return GameConstants.GAME_PLAYER;
	}
	


	public boolean refresh_room_redis_data(int type, boolean notifyRedis) {

		if (type == GameConstants.PROXY_ROOM_UPDATE) {
			int cur_player_num = 0;
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				if (this.get_players()[i] != null) {
					cur_player_num++;
				}
			}

			// 写入redis
			RedisService redisService = SpringService.getBean(RedisService.class);
			RoomRedisModel roomRedisModel = SpringService.getBean(RedisService.class).hGet(RedisConstant.ROOM,
					getRoom_id() + "", RoomRedisModel.class);
			roomRedisModel.setGameRuleDes(this.get_game_des());
			roomRedisModel.setRoomStatus(this._game_status);
			roomRedisModel.setPlayer_max( RoomComonUtil.getMaxNumber(this));
			roomRedisModel.setCur_player_num(cur_player_num);
			roomRedisModel.setGame_round(this._game_round);
			// roomRedisModel.setCreate_time(System.currentTimeMillis());
			redisService.hSet(RedisConstant.ROOM, getRoom_id() + "", roomRedisModel);

		} else if (type == GameConstants.PROXY_ROOM_RELEASE) {

		}
		if (notifyRedis) {
			// 通知redis消息队列
			RedisResponse.Builder redisResponseBuilder = RedisResponse.newBuilder();
			redisResponseBuilder.setRsResponseType(RsResponseType.CMD);
			RsCmdResponse.Builder rsCmdResponseBuilder = RsCmdResponse.newBuilder();
			rsCmdResponseBuilder.setType(3);
			rsCmdResponseBuilder.setAccountId(this.getRoom_owner_account_id());
			redisResponseBuilder.setRsCmdResponse(rsCmdResponseBuilder);
			RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(),
					ERedisTopicType.topicProxy);

		}
		return true;
	}

	private boolean kou_dou() {
		return RoomUtil.kou_dou(this);
	}

	private boolean huan_dou(int result) {
		if (cost_dou == 0)
			return false;
		boolean huan = false;
		if (result == GameConstants.Game_End_NORMAL || result == GameConstants.Game_End_DRAW
				|| result == GameConstants.Game_End_ROUND_OVER) {
			return false;
		} else if (result == GameConstants.Game_End_RELEASE_NO_BEGIN) {
			// 还没开始
			huan = true;
		} else if (result == GameConstants.Game_End_RELEASE_RESULT || result == GameConstants.Game_End_RELEASE_PLAY
				|| result == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT
				|| result == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT) {
			// 开始了 没打完
			if ((this._cur_round <= 1) && (GRR != null)) {
				huan = true;
			}
		} else if (result == GameConstants.Game_End_RELEASE_SYSTEM) {
			if (this._cur_round <= 1) {
				huan = true;
			}
		} else {
			return false;
		}

		if (huan) {
			// 不是正常结束的
			// if((game_end.getEndType()!=GameConstants.Game_End_NORMAL) &&
			// (game_end.getEndType()!=GameConstants.Game_End_DRAW)){
			// 还豆
			StringBuilder buf = new StringBuilder();
			buf.append("开局失败" + ":" + this.getRoom_id()).append("game_id:" + this.getGame_id())
					.append(",game_type_index:" + _game_type_index).append(",game_round:" + _game_round)
					.append(",房主:" + this.getRoom_owner_account_id()).append(",豆+:" + cost_dou);
			// 把豆还给玩家
			AddGoldResultModel addresult = PlayerServiceImpl.getInstance().addGold(this.getRoom_owner_account_id(),
					cost_dou, false, buf.toString(), EGoldOperateType.FAILED_ROOM);
			if (addresult.isSuccess() == false) {
				logger.error("房间[" + this.getRoom_id() + "]" + "玩家[" + this.getRoom_owner_account_id() + "]还豆失败");
			}

			// }
		}
		return true;
	}

	@Override
	public boolean exe_finish(int reason) {
		// TODO Auto-generated method stub
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.cai.common.domain.Room#handler_request_goods(int,
	 * protobuf.clazz.Protocol.RoomRequest)
	 */
	@Override
	public boolean handler_request_goods(int get_seat_index, RoomRequest room_rq) {
		long targetID = room_rq.getTargetAccountId();
		int goodsID = room_rq.getGoodsID();
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_GOODS);
		roomResponse.setOperatePlayer(get_seat_index);
		roomResponse.setGoodID(goodsID);
		roomResponse.setTargetID(targetID);
		this.send_response_to_room(roomResponse);
		return false;
	}


	@Override
	public void clear_score_in_gold_room() {
		ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
		int score = 0;

		// 门槛判断
		SysParamModel sysParamModel = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(getGame_id())
				.get(5006);
		int beilv = sysParamModel.getVal2().intValue();// 倍率
		float[] scores = _player_result.getGame_score();
		// GameConstants.GAME_PLAYER
		for (int i = 0; i < scores.length; i++) {
			score = (int) (scores[i] * beilv);

			StringBuilder buf = new StringBuilder();
			buf.append("牌局消耗:" + scores[i]).append("game_id:" + getGame_id());
			AddMoneyResultModel addGoldResultModel = centerRMIServer.addAccountMoney(
					this.get_players()[i].getAccount_id(), score, false, buf.toString(), EMoneyOperateType.ROOM_COST);
			if (addGoldResultModel.isSuccess() == false) {
				// 扣费失败
				logger.error("玩家:" + this.get_players()[i].getAccount_id() + ",扣费:" + score + "失败");
			}
			// 结算后 清0
			scores[i] = 0;
		}
	}

	@Override
	public boolean handler_refresh_player_data(int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);// 刷新玩家
		this.load_player_info_data(roomResponse);
		this.send_response_to_player(seat_index, roomResponse);
		// this.send_response_to_room(roomResponse);
		return true;
	}
	
	@Override
	public boolean kickout_not_ready_player() {
		return true;
	}
	public  boolean open_card_timer(){
		return false;
	}
	
	public  boolean robot_banker_timer(){
		return false;
	}
	
	public  boolean ready_timer(){
		return false;
	}
	
	public  boolean add_jetton_timer(){
		return false;
	}

	@Override
	public void init_other_param(Object... objects) {
		// WalkerGeek Auto-generated method stub
		
	}
	/**
	 * @param seat_index
	 * @param call_banker
	 * @param qiang_banker
	 * @return
	 */
	@Override
	public  boolean handler_requst_call_qiang_zhuang(int seat_index,int call_banker,int qiang_banker)
	{
		return true;
	}
}
