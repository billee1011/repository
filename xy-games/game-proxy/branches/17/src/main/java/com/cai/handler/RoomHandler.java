package com.cai.handler;

import java.util.List;

import com.cai.common.constant.MJGameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.RedisConstant;
import com.cai.common.define.ELogType;
import com.cai.common.define.ERedisTopicType;
import com.cai.common.define.ESysLogLevelType;
import com.cai.common.domain.Account;
import com.cai.common.domain.BrandLogModel;
import com.cai.common.domain.GameRoomRecord;
import com.cai.common.domain.GameRoundRecord;
import com.cai.common.domain.Player;
import com.cai.common.domain.PlayerResult;
import com.cai.common.domain.RoomRedisModel;
import com.cai.common.domain.SysParamModel;
import com.cai.common.rmi.ICenterRMIServer;
import com.cai.common.util.SpringService;
import com.cai.common.util.ZipUtil;
import com.cai.dictionary.SysParamDict;
import com.cai.net.core.ClientHandler;
import com.cai.redis.service.RedisService;
import com.cai.service.ClientServiceImpl;
import com.cai.service.MongoDBServiceImpl;
import com.cai.service.PublicServiceImpl;
import com.cai.service.RedisServiceImpl;
import com.cai.util.MessageResponse;

import protobuf.clazz.Protocol;
import protobuf.clazz.Protocol.GameEndResponse;
import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.LogicRoomRequest;
import protobuf.clazz.Protocol.PlayerResultResponse;
import protobuf.clazz.Protocol.Request;
import protobuf.clazz.Protocol.Request.RequestType;
import protobuf.clazz.Protocol.Response;
import protobuf.clazz.Protocol.Response.ResponseType;
import protobuf.clazz.Protocol.RoomPlayerResponse;
import protobuf.clazz.Protocol.RoomRequest;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.redis.ProtoRedis.RedisResponse;
import protobuf.redis.ProtoRedis.RedisResponse.RsResponseType;
import protobuf.redis.ProtoRedis.RsAccountResponse;
import protobuf.redis.ProtoRedis.RsSystemStopReadyStatusResponse;

/**
 * 房间处理
 * @author run
 *
 */
public class RoomHandler extends ClientHandler<RoomRequest>{
	
	/**
	 * 创建房间
	 */
	private static final int CREATE_ROOM = 1;
	
	/**
	 * 加入房间
	 */
	private static final int JOIN_ROOM = 2;
	
	/**
	 * 重连
	 */
	private static final int RESET_CONNECT = 3;
	
	/**
	 * 请求牌局记录
	 */
	private static final int REQUEST_GAME_ROOM_RECORD = 4;
	
	/**
	 * 小局
	 */
	private static final int REQUEST_GAME_ROUND_RECORD = 5;
	
	/**
	 * 小局回放
	 */
	private static final int ROUND_RECORD_VIDEO = 6;
	
	

	@Override
	public void onRequest() throws Exception {
		
		int type = request.getType();
		//操作频率控制
		if(!session.isCanRequest("RoomHandler_"+type, 300L)){
			return;
		}
		
		
		if(session.getAccount()==null)
			return;
		
		Account account = session.getAccount();	
		
		int game_id = account.getGame_id();
		if(type == CREATE_ROOM){
			
			if(!request.hasGameRound())
				return;
			if(!request.hasGameRuleIndex())
				return;
			if(!request.hasGameTypeIndex())
				return;
			
			//麻将类型
			int game_type_index = request.getGameTypeIndex();
			//玩法
			int game_rule_index = request.getGameRuleIndex();
			///局数
			int game_round = request.getGameRound();
			
			
			if(!(game_round==8 || game_round==16)){
				return;
			}
				
			
			
			//TODO 从redis查看是否有进入其它房间
			
			if(account.getRoom_id()!=0){
				//验证一下
				send(MessageResponse.getMsgAllResponse("调试:已进入其它房间:"+account.getRoom_id()).build());
				return;
			}
			
			
			
			//开放判断
			SysParamModel sysParamModel = null;
			if(game_type_index==MJGameConstants.GAME_TYPE_ZZ){
				sysParamModel = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(1).get(1201);
			}else if(game_type_index==MJGameConstants.GAME_TYPE_CS){
				sysParamModel = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(1).get(1202);
			}else if(game_type_index==MJGameConstants.GAME_TYPE_HZ){
				sysParamModel = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(1).get(1203);
			}
			if(sysParamModel!=null && sysParamModel.getVal1()!=1){
				send(MessageResponse.getMsgAllResponse("即将开放,敬请期待!").build());
				return;
			}
			
			//判断房卡是否免费
			if(sysParamModel!=null && sysParamModel.getVal2()==1){
				SysParamModel sysParamModel1011 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(1).get(1011);
				SysParamModel sysParamModel1012 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(1).get(1012);
				long gold = account.getAccountModel().getGold();
				if(game_round==8){
					if(gold<sysParamModel1011.getVal1()){
						send(MessageResponse.getMsgAllResponse("房卡不足").build());
						return;
					}
				}else if(game_round==16){
					if(gold<sysParamModel1012.getVal1()){
						send(MessageResponse.getMsgAllResponse("房卡不足").build());
						return;
					}
				}
			}
			
			
			//TODO 创建房间号,写入redis 记录是哪个逻辑计算服的
			ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
			int room_id = centerRMIServer.randomRoomId(1);//随机房间号
			if(room_id==-1){
				send(MessageResponse.getMsgAllResponse("创建房间失败!").build());
				return;
			}
			if(room_id==-2){
				send(MessageResponse.getMsgAllResponse("服务器进入停服倒计时,不能创建房间,请等待服务器停机维护完成再登录!").build());
				return;
			}
			
			//redis房间记录
			RoomRedisModel roomRedisModel = new RoomRedisModel();
			roomRedisModel.setRoom_id(room_id);
			roomRedisModel.setLogic_index(1);//TODO 临时
			roomRedisModel.getPlayersIdSet().add(session.getAccountID());
			roomRedisModel.setCreate_time(System.currentTimeMillis());
			roomRedisModel.setGame_round(game_round);
			roomRedisModel.setGame_rule_index(game_rule_index);
			roomRedisModel.setGame_type_index(game_type_index);
			SpringService.getBean(RedisService.class).hSet(RedisConstant.ROOM, room_id+"", roomRedisModel);
			
			//玩家最后的房间号记录
			account.setRoom_id(room_id);
			
			//========同步到中心========
			RedisResponse.Builder redisResponseBuilder = RedisResponse.newBuilder();
			redisResponseBuilder.setRsResponseType(RsResponseType.ACCOUNT_UP);
			//
			RsAccountResponse.Builder rsAccountResponseBuilder = RsAccountResponse.newBuilder();
			rsAccountResponseBuilder.setAccountId(account.getAccount_id());
			rsAccountResponseBuilder.setRoomId(account.getRoom_id());
			//
			redisResponseBuilder.setRsAccountResponse(rsAccountResponseBuilder);
			RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(), ERedisTopicType.topicAll);
			//=======================
			
			
			
			//通知逻辑服
			Request.Builder requestBuider = MessageResponse.getLogicRequest(RequestType.LOGIC_ROOM, session);
			LogicRoomRequest.Builder logicRoomRequestBuilder = LogicRoomRequest.newBuilder();
			logicRoomRequestBuilder.setType(1);
			logicRoomRequestBuilder.setRoomRequest(request);
			logicRoomRequestBuilder.setRoomId(room_id);
			logicRoomRequestBuilder.setLogicRoomAccountItemRequest(MessageResponse.getLogicRoomAccountItemRequest(session));
			requestBuider.setExtension(Protocol.logicRoomRequest, logicRoomRequestBuilder.build());
			boolean flag = ClientServiceImpl.getInstance().sendMsg(requestBuider.build());
			
			if(!flag){
				send(MessageResponse.getMsgAllResponse("逻辑服务器链接失败").build());
				return;
			}
			
		}
		
		else if(type == JOIN_ROOM){
			
			if(!request.hasRoomId())
				return;
			int room_id = request.getRoomId();
			
			//如果他以前有房间的
			int source_room_id = account.getRoom_id();
			if(source_room_id!=0){
				RoomRedisModel roomRedisModel = SpringService.getBean(RedisService.class).hGet(RedisConstant.ROOM, source_room_id+"", RoomRedisModel.class);
				if(roomRedisModel!=null){
					int loginc_index = roomRedisModel.getLogic_index();
					Request.Builder requestBuider = MessageResponse.getLogicRequest(RequestType.LOGIC_ROOM, session);
					LogicRoomRequest.Builder logicRoomRequestBuilder = LogicRoomRequest.newBuilder();
					logicRoomRequestBuilder.setType(3);
					logicRoomRequestBuilder.setRoomRequest(request);
					logicRoomRequestBuilder.setRoomId(source_room_id);
					logicRoomRequestBuilder.setLogicRoomAccountItemRequest(MessageResponse.getLogicRoomAccountItemRequest(session));
					requestBuider.setExtension(Protocol.logicRoomRequest, logicRoomRequestBuilder.build());
					boolean flag = ClientServiceImpl.getInstance().sendMsg(requestBuider.build());
					if(!flag){
						send(MessageResponse.getMsgAllResponse("逻辑服务器链接失败").build());
					}
					return;
				}
			}

			
			//TODO 从redis取出,判断出是哪个逻辑计算服的
			RoomRedisModel roomRedisModel = SpringService.getBean(RedisService.class).hGet(RedisConstant.ROOM, room_id+"", RoomRedisModel.class);
			if(roomRedisModel==null){
				send(MessageResponse.getMsgAllResponse("房间不存在!").build());
				return;
			}
			
			ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
			RsSystemStopReadyStatusResponse rsSystemStopReadyStatusResponse = centerRMIServer.systemStopReadyStatus();
			if(rsSystemStopReadyStatusResponse.getSystemStopReady()){
				send(MessageResponse.getMsgAllResponse("服务器进入停服倒计时,不能进入房间,请等待服务器停机维护完成再登录!").build());
				return;
			}
			
			
			if(roomRedisModel.getPlayersIdSet().contains(account.getAccount_id())){
				send(MessageResponse.getMsgAllResponse("已经在房间里了!").build());
				return;
			}
			if(roomRedisModel.getPlayersIdSet().size()>=4){
				send(MessageResponse.getMsgAllResponse("房间人数已满!").build());
				return;
			}
			roomRedisModel.getPlayersIdSet().add(account.getAccount_id());
			//写入redis
			SpringService.getBean(RedisService.class).hSet(RedisConstant.ROOM, room_id+"", roomRedisModel);
			
			
			
			//TODO 选择逻辑计算服
			int loginc_index = roomRedisModel.getLogic_index();
			Request.Builder requestBuider = MessageResponse.getLogicRequest(RequestType.LOGIC_ROOM, session);
			LogicRoomRequest.Builder logicRoomRequestBuilder = LogicRoomRequest.newBuilder();
			logicRoomRequestBuilder.setType(2);
			logicRoomRequestBuilder.setRoomRequest(request);
			logicRoomRequestBuilder.setRoomId(room_id);
			logicRoomRequestBuilder.setLogicRoomAccountItemRequest(MessageResponse.getLogicRoomAccountItemRequest(session));
			requestBuider.setExtension(Protocol.logicRoomRequest, logicRoomRequestBuilder.build());
			ClientServiceImpl.getInstance().sendMsg(requestBuider.build());
			
			//记录
			account.setRoom_id(room_id);
			
			//========同步到中心========
			RedisResponse.Builder redisResponseBuilder = RedisResponse.newBuilder();
			redisResponseBuilder.setRsResponseType(RsResponseType.ACCOUNT_UP);
			//
			RsAccountResponse.Builder rsAccountResponseBuilder = RsAccountResponse.newBuilder();
			rsAccountResponseBuilder.setAccountId(account.getAccount_id());
			rsAccountResponseBuilder.setRoomId(account.getRoom_id());
			//
			redisResponseBuilder.setRsAccountResponse(rsAccountResponseBuilder);
			RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(), ERedisTopicType.topicAll);
			//=======================
			
			//TODO 数据统计-从哪里进入房间的
			if(request.hasInRoomWay()){
				int in_room_way = request.getInRoomWay();
				MongoDBServiceImpl.getInstance().systemLog(ELogType.inRoomWay, null, (long)in_room_way, null, ESysLogLevelType.NONE);
			}
			
			
		}
		
		else if(type == RESET_CONNECT){
			//判断是否上次在牌桌上
			RoomRedisModel roomRedisModel = null;
			int room_id = account.getRoom_id();
			if(room_id!=0){
				roomRedisModel = SpringService.getBean(RedisService.class).hGet(RedisConstant.ROOM, room_id+"", RoomRedisModel.class);
			}
			
			if(roomRedisModel==null){
				
				account.setRoom_id(0);
				
				//========同步到中心========
				RedisResponse.Builder redisResponseBuilder = RedisResponse.newBuilder();
				redisResponseBuilder.setRsResponseType(RsResponseType.ACCOUNT_UP);
				//
				RsAccountResponse.Builder rsAccountResponseBuilder = RsAccountResponse.newBuilder();
				rsAccountResponseBuilder.setAccountId(account.getAccount_id());
				rsAccountResponseBuilder.setRoomId(account.getRoom_id());
				//
				redisResponseBuilder.setRsAccountResponse(rsAccountResponseBuilder);
				RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(), ERedisTopicType.topicAll);
				//=======================
				
				// 返回消息
				RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
				roomResponse.setType(3);
				Response.Builder responseBuilder = Response.newBuilder();
				responseBuilder.setResponseType(ResponseType.ROOM);
				responseBuilder.setExtension(Protocol.roomResponse, roomResponse.build());
				send(responseBuilder.build());
			}else{
				
				int loginc_index = roomRedisModel.getLogic_index();
				Request.Builder requestBuider = MessageResponse.getLogicRequest(RequestType.LOGIC_ROOM, session);
				LogicRoomRequest.Builder logicRoomRequestBuilder = LogicRoomRequest.newBuilder();
				logicRoomRequestBuilder.setType(3);
				logicRoomRequestBuilder.setRoomRequest(request);
				logicRoomRequestBuilder.setRoomId(room_id);
				logicRoomRequestBuilder.setLogicRoomAccountItemRequest(MessageResponse.getLogicRoomAccountItemRequest(session));
				requestBuider.setExtension(Protocol.logicRoomRequest, logicRoomRequestBuilder.build());
				boolean flag = ClientServiceImpl.getInstance().sendMsg(requestBuider.build());
				if(!flag){
					send(MessageResponse.getMsgAllResponse("逻辑服务器链接失败").build());
					return;
				}
			}
			
			
		}else if(type ==REQUEST_GAME_ROOM_RECORD){
			
			long target_account_id = account.getAccount_id();
			if(request.hasTargetAccountId()){
				if(account.getAccountModel().getIs_agent()!=1){
					send(MessageResponse.getMsgAllResponse("你不是代理没有权限查看其他玩家牌局记录").build());
					return;
				}
				target_account_id = request.getTargetAccountId();
			}
			
			List<BrandLogModel> room_record = MongoDBServiceImpl.getInstance().getParentBrandListByAccountId(target_account_id, game_id);
			int l = room_record.size();
			GameRoomRecord grr = null;
			RoomResponse.Builder game_room_record = RoomResponse.newBuilder();
			game_room_record.setType(MsgConstants.RESPONSE_GAME_ROOM_RECORD_LIST);
			for(int k=0; k < l; k++){
				boolean error_check =false;
				grr = GameRoomRecord.to_Object(room_record.get(k).getMsg());//
				for(int i=0; i <MJGameConstants.GAME_PLAYER; i++){
					if(grr.getPlayers()[i]==null){
						error_check = true;
					}
				}
				if(error_check)continue;
				
				PlayerResult _player_result = grr.get_player();
				PlayerResultResponse.Builder player_result = PlayerResultResponse.newBuilder();
				
				player_result.setRoomId(grr.getRoom_id());
				player_result.setRoomOwnerAccountId(grr.getRoom_owner_account_id());
				player_result.setRoomOwnerName(grr.getRoom_owner_name());
				player_result.setCreateTime(grr.getCreate_time());
				player_result.setRecordId(grr.get_record_id());
				player_result.setGameRound(_player_result.game_round);
				player_result.setGameRuleDes(_player_result.game_rule_des);
				player_result.setGameRuleIndex(_player_result.game_rule_index);
				player_result.setGameTypeIndex(_player_result.game_type_index);
				
				for(int i=0; i <MJGameConstants.GAME_PLAYER; i++){
					player_result.addGameScore(_player_result.game_score[i]);
					player_result.addWinOrder(_player_result.win_order[i]);
					
					//if((_player_result.game_type_index==MJGameConstants.GAME_TYPE_ZZ)||(_player_result.game_type_index==MJGameConstants.GAME_TYPE_HZ)|| (_player_result.game_type_index==MJGameConstants.GAME_TYPE_SHUANGGUI)){
						player_result.addZiMoCount(_player_result.zi_mo_count[i]);
						player_result.addJiePaoCount(_player_result.jie_pao_count[i]);
						player_result.addDianPaoCount(_player_result.dian_pao_count[i]);
						player_result.addAnGangCount(_player_result.an_gang_count[i]);
						player_result.addMingGangCount(_player_result.ming_gang_count[i]);
					//}else if(_player_result.game_type_index==MJGameConstants.GAME_TYPE_CS || (_player_result.game_type_index==MJGameConstants.GAME_TYPE_ZHUZHOU)){
						player_result.addDaHuZiMo(_player_result.da_hu_zi_mo[i]);
						player_result.addDaHuJiePao(_player_result.da_hu_jie_pao[i]);
						player_result.addDaHuDianPao(_player_result.da_hu_dian_pao[i]);
						player_result.addXiaoHuZiMo(_player_result.xiao_hu_zi_mo[i]);
						player_result.addXiaoHuJiePao(_player_result.xiao_hu_jie_pao[i]);
						player_result.addXiaoHuDianPao(_player_result.xiao_hu_dian_pao[i]);
					//}
					
					player_result.addPiaoLaiCount(_player_result.piao_lai_count[i]);
					
					Int32ArrayResponse.Builder lfs = Int32ArrayResponse.newBuilder();
					for(int j=0; j<MJGameConstants.GAME_PLAYER; j++ ){
						lfs.addItem(_player_result.lost_fan_shu[i][j]);
					}
					
					player_result.addLostFanShu(lfs);
					
					Player rplayer = grr.getPlayers()[i];
					
					RoomPlayerResponse.Builder room_player = RoomPlayerResponse.newBuilder();
					room_player.setAccountId(rplayer.getAccount_id());
					room_player.setHeadImgUrl(rplayer.getAccount_icon());
					room_player.setIp(rplayer.getAccount_ip());
					room_player.setUserName(rplayer.getNick_name());
					room_player.setSeatIndex(rplayer.get_seat_index());
					room_player.setOnline(rplayer.isOnline()?1:0);
					//room_player.setIpAddr(rplayer.getAccount_ip_addr());
					
					player_result.addPlayers(room_player);
					
					player_result.addPlayersId(grr.getPlayers()[i].getAccount_id());
					player_result.addPlayersName(grr.getPlayers()[i].getNick_name());
				}
				game_room_record.addGameRoomRecords(player_result);
			}
			
			// 返回消息
			
			Response.Builder responseBuilder = Response.newBuilder();
			responseBuilder.setResponseType(ResponseType.ROOM);
			responseBuilder.setExtension(Protocol.roomResponse, game_room_record.build());
			send(responseBuilder.build());
		}else if(type ==REQUEST_GAME_ROUND_RECORD){
			long record_id  = request.getRecordId();
			
			List<BrandLogModel> round_records = MongoDBServiceImpl.getInstance().getChildBrandList(record_id, game_id);
			int l = round_records.size();
			RoomResponse.Builder game_round_record = RoomResponse.newBuilder();
			game_round_record.setType(MsgConstants.RESPONSE_GAME_ROUND_RECORD_LIST);
			for(int f=0; f < l; f++){
				byte[] gzipByte = ZipUtil.unGZip(round_records.get(f).getVideo_record());
				try{
					GameEndResponse game_end =Protocol.GameEndResponse.parseFrom(gzipByte);
					//不要回放数据
					GameEndResponse.Builder gameEndResponseBuilder = game_end.toBuilder();
					gameEndResponseBuilder.clearRecord();
					game_end = gameEndResponseBuilder.build();
					game_round_record.addGameRoundRecords(game_end);
				}
				catch(Exception e){
					logger.error("error",e);
				}
				
//				Video_Record video_Record = Protocol.Video_Record.parseFrom(video_record_bytes);
//				GRR = GameRoundRecord.to_object(round_records.get(f).getMsg());//
//				byte video_record_bytes[] = round_records.get(f).getVideo_record();
//				int cp = round_records.get(f).getCompress_video();
//				
//				GameEndResponse.Builder  game_end = GameEndResponse.newBuilder();
//				
//				game_end.setStartTime(GRR._start_time);
//				game_end.setGameTypeIndex(GRR._game_type_index);
//				//game_round_record.setGameRound(grr._game_round);
//				game_end.setCurRound(GRR._cur_round);
//				game_end.setEndType(GRR._end_type);
//				game_end.setCellScore(MJGameConstants.CELL_SCORE);
//				
//				try{
//					
//					byte[] gzipByte = ZipUtil.gZip(video_record_bytes);
//					
//					System.out.println("old:"+video_record_bytes.length +", new:"+gzipByte.length);
//					
//					
//					Video_Record video_Record = Protocol.Video_Record.parseFrom(video_record_bytes);
//					game_end.setCompressVideo(cp);
//					game_end.setRecord(video_Record);
//				}catch(Exception e){
//					
//				}
//				
//				
//				
//				
//				game_end.setBankerPlayer(GRR._banker_player);//专家
//				game_end.setLeftCardCount(GRR._left_card_count);//剩余牌
//				
//				int lGangScore[] = new int[MJGameConstants.GAME_PLAYER];
//				for( int i = 0; i < MJGameConstants.GAME_PLAYER; i++ )
//				{
//					for( int j = 0; j < GRR._gang_score[i].gang_count; j++ )
//					{
//						for( int k = 0; k < MJGameConstants.GAME_PLAYER; k++ ){
//							lGangScore[k] += GRR._gang_score[i].scores[j][k];//杠牌，每个人的分数
//						}
//					}
//				}
	//
//				
//				//设置中鸟数据
//				for (int i = 0;i<MJGameConstants.MAX_NIAO_CARD && i< GRR._count_niao;i++)
//				{
//					game_end.addCardsDataNiao(GRR._cards_data_niao[i]);
//				}
//				game_end.setCountPickNiao(GRR._count_pick_niao);//中鸟个数
//				
//				for(int i=0; i<MJGameConstants.GAME_PLAYER;i++){
//					Int32ArrayResponse.Builder pnc = Int32ArrayResponse.newBuilder();
//					for (int j=0;j<GRR._player_niao_count[i];j++)
//					{
//						pnc.addItem(GRR._player_niao_cards[i][j]);
//					}
//					game_end.addPlayerNiaoCards(pnc);
//					game_end.addHuResult(GRR._hu_result[i]);
//					game_end.addHuCardData(GRR._chi_hu_card[i]);
//				}
//				
//				
//				//现在权值只有一位
//				long rv[] = new long[MJGameConstants.MAX_RIGHT_COUNT];
//				
//				for(int i=0; i < MJGameConstants.GAME_PLAYER;i++){
//					Int32ArrayResponse.Builder cs = Int32ArrayResponse.newBuilder();
//					for(int j=0; j<GRR._card_count[i];j++){
//						
//						cs.addItem(GRR._cards_data[i][j]);
//					}
//					game_end.addCardsData(cs);//牌
//					
//					//组合
//					WeaveItemResponseArrayResponse.Builder weaveItem_array= WeaveItemResponseArrayResponse.newBuilder();
//					for(int j=0; j < MJGameConstants.MAX_WEAVE; j++){
//						WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
//						weaveItem_item.setCenterCard(GRR._weave_items[i][j].center_card);
//						weaveItem_item.setProvidePlayer(GRR._weave_items[i][j].provide_player);
//						weaveItem_item.setPublicCard(GRR._weave_items[i][j].public_card);
//						weaveItem_item.setWeaveKind(GRR._weave_items[i][j].weave_kind);
//						weaveItem_array.addWeaveItem(weaveItem_item);
//					}
//					game_end.addWeaveItemArray(weaveItem_array);
//					
//					GRR._chi_hu_rights[i].get_right_data( rv);//获取权位数值
//					game_end.addChiHuRight(rv[0]);
//					
//					GRR._start_hu_right[i].get_right_data(rv);//获取权位数值
//					game_end.addStartHuRight(rv[0]);
//					
//					game_end.addProvidePlayer(GRR._provider[i]);
//					game_end.addGameScore(GRR._game_score[i]);//放炮的人？
//					game_end.addGangScore(lGangScore[i]);//杠牌得分
//					game_end.addStartHuScore(GRR._start_hu_score[i]);
//					game_end.addResultDes(GRR._result_des[i]);
//					
//					//胡牌han
//					game_end.addWinOrder(GRR._win_order[i]);
//					
//					Int32ArrayResponse.Builder lfs = Int32ArrayResponse.newBuilder();
//					for(int j=0; j<MJGameConstants.GAME_PLAYER; j++ ){
//						lfs.addItem(GRR._lost_fan_shu[i][j]);
//					}
//					
//					game_end.addLostFanShu(lfs);
					
//				}
				
			//	game_round_record.addGameRoundRecords(game_end);
			}
			// 返回消息
			
			Response.Builder responseBuilder = Response.newBuilder();
			responseBuilder.setResponseType(ResponseType.ROOM);
			responseBuilder.setExtension(Protocol.roomResponse, game_round_record.build());
			send(responseBuilder.build());
		}
		else if(type == ROUND_RECORD_VIDEO){
			
			if(!request.hasBrandId())
				return;
			long brand_id  =request.getBrandId(); //1612141055370040001L;
			BrandLogModel brandLogModel = MongoDBServiceImpl.getInstance().getChildBrandByBrandId(brand_id, game_id);
			if(brandLogModel==null){
				send(MessageResponse.getMsgAllResponse("记录不存在").build());
				return;
			}
			RoomResponse.Builder roomResponseBuilder = RoomResponse.newBuilder();
			roomResponseBuilder.setType(MsgConstants.RESPONSE_GAME_ROUND_RECORD);
			byte[] gzipByte = ZipUtil.unGZip(brandLogModel.getVideo_record());
			try{
				GameEndResponse game_end =Protocol.GameEndResponse.parseFrom(gzipByte);
				roomResponseBuilder.setGameRoundRecord(game_end);
			}
			catch(Exception e){
				logger.error("error",e);
			}
			// 返回消息
			Response.Builder responseBuilder = Response.newBuilder();
			responseBuilder.setResponseType(ResponseType.ROOM);
			responseBuilder.setExtension(Protocol.roomResponse, roomResponseBuilder.build());
			send(responseBuilder.build());
		}
		else {
			//消息再次封装
			Request logicRequest = MessageResponse.getLogicRequest(topRequest.toBuilder(),session).build();
			ClientServiceImpl.getInstance().sendMsg(logicRequest);
		}
	
	}
	
	

}
