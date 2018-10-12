package com.cai.handler;

import java.util.Date;
import java.util.List;

import com.cai.common.domain.Account;
import com.cai.common.domain.GameDescModel;
import com.cai.common.domain.MainUiNoticeModel;
import com.cai.common.domain.SysNoticeModel;
import com.cai.common.domain.SysParamModel;
import com.cai.common.util.MyDateUtil;
import com.cai.dictionary.GameDescDict;
import com.cai.dictionary.MainUiNoticeDict;
import com.cai.dictionary.SysNoticeDict;
import com.cai.dictionary.SysParamDict;
import com.cai.net.core.ClientHandler;
import com.cai.util.MessageResponse;
import com.google.common.collect.Lists;

import javolution.util.FastMap;
import protobuf.clazz.Protocol;
import protobuf.clazz.Protocol.GaemDescItemResponse;
import protobuf.clazz.Protocol.MainUiNoticeItemResponse;
import protobuf.clazz.Protocol.OtherSystemRequest;
import protobuf.clazz.Protocol.OtherSystemResponse;
import protobuf.clazz.Protocol.Response;
import protobuf.clazz.Protocol.Response.ResponseType;

/**
 * 其它系统功能
 * @author run
 *
 */
public class OtherSystemHandler extends ClientHandler<OtherSystemRequest>{

	/**
	 * 系统公告
	 */
	private static final int SYS_NOTICE = 1;
	
	/**
	 * 玩法说明
	 */
	private static final int GAME_DESC = 2;
	
	/**
	 * 主界面公告
	 */
	private static final int MAIN_UI_NOTICE = 3;
	
	@Override
	public void onRequest() throws Exception {
		
		int type = request.getType();
		
		if(!session.isCanRequest("OtherSystemHandler_"+type, 300L)){
			return;
		}
		
		
		if(session.getAccount()==null)
			return;
		
		Account account = session.getAccount();	
		
		int game_id = account.getGame_id();
		
		if(type == SYS_NOTICE){
			
			
			SysNoticeModel sysNoticeModel = SysNoticeDict.getInstance().getSysNoticeModelDictionary().get(game_id).get(1);
			
			OtherSystemResponse.Builder otherSystemResponseBuilder = OtherSystemResponse.newBuilder();
			otherSystemResponseBuilder.setType(1);
			otherSystemResponseBuilder.setContent(sysNoticeModel.getContent());
			
			Response.Builder responseBuilder = Response.newBuilder();
			responseBuilder.setResponseType(ResponseType.OTHER_SYS);
			responseBuilder.setExtension(Protocol.otherSystemResponse, otherSystemResponseBuilder.build());
			send(responseBuilder.build());
			
			
//			List<RsAccountParamModelResponse> rsAccountParamModelResponseList = Lists.newArrayList();
//			//标记为看过的
//			AccountParamModel accountParamModel = account.getAccountParamModelMap().get(EPropertyType.RED_SYS_NOTIC.getId());
//			if(accountParamModel==null){
//				accountParamModel = new AccountParamModel();
//				accountParamModel.setAccount_id(account.getAccount_id());
//				accountParamModel.setType(EPropertyType.RED_SYS_NOTIC.getId());
//				accountParamModel.setLong1(sysNoticeModel.getCreate_time().getTime());
//				accountParamModel.setNeedDB(true);
//				rsAccountParamModelResponseList.add(MessageResponse.getRsAccountParamModelResponse(accountParamModel).build());
//			}else{
//				if(accountParamModel.getLong1()==null || accountParamModel.getLong1()!=sysNoticeModel.getCreate_time().getTime()){
//					accountParamModel.setLong1(sysNoticeModel.getCreate_time().getTime());
//					accountParamModel.setNeedDB(true);
//					rsAccountParamModelResponseList.add(MessageResponse.getRsAccountParamModelResponse(accountParamModel).build());
//				}
//			}
//			
//			if(rsAccountParamModelResponseList.size()>0){
//				//========同步到中心========
//				RedisResponse.Builder redisResponseBuilder = RedisResponse.newBuilder();
//				redisResponseBuilder.setRsResponseType(RsResponseType.ACCOUNT_UP);
//				RsAccountResponse.Builder rsAccountResponseBuilder = RsAccountResponse.newBuilder();
//				rsAccountResponseBuilder.setAccountId(account.getAccount_id());
//				rsAccountResponseBuilder.addAllRsAccountParamModelResponseList(rsAccountParamModelResponseList);
//				RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(), ERedisTopicType.topicCenter);
//			}
			
			
		}
		
		else if(type == GAME_DESC){
			OtherSystemResponse.Builder otherSystemResponseBuilder = OtherSystemResponse.newBuilder();
			otherSystemResponseBuilder.setType(2);
			List<GaemDescItemResponse> gaemDescItemResponseList = Lists.newArrayList();
			
			for(GameDescModel m : GameDescDict.getInstance().getGameDescModelDictionary().get(game_id).values()){
				gaemDescItemResponseList.add(MessageResponse.getGaemDescItemResponse(m).build());
			}
			otherSystemResponseBuilder.addAllGaemDescItemResponseList(gaemDescItemResponseList);
			Response.Builder responseBuilder = Response.newBuilder();
			responseBuilder.setResponseType(ResponseType.OTHER_SYS);
			responseBuilder.setExtension(Protocol.otherSystemResponse, otherSystemResponseBuilder.build());
			send(responseBuilder.build());
		}
		
		
		else if(type == MAIN_UI_NOTICE){
			List<MainUiNoticeItemResponse> mainUiNoticeItemResponseList = Lists.newArrayList();
			//是否开放
			SysParamModel sysParamModel1016 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(game_id).get(1016);
			if(sysParamModel1016.getVal1()==1){
				FastMap<Integer, MainUiNoticeModel> map = MainUiNoticeDict.getInstance().getMainUiNoticeDictionary().get(game_id);
				if(map!=null){
					Date now = MyDateUtil.getNow();
					for(MainUiNoticeModel m : map.values()){
						if(m.getEnd_time().after(now)){
							mainUiNoticeItemResponseList.add(MessageResponse.getMainUiNoticeItemResponse(m).build());
						}
					}
				}
			}
			
			OtherSystemResponse.Builder otherSystemResponseBuilder = OtherSystemResponse.newBuilder();
			otherSystemResponseBuilder.setType(3);
			otherSystemResponseBuilder.addAllMainUiNoticeItemResponseList(mainUiNoticeItemResponseList);
			Response.Builder responseBuilder = Response.newBuilder();
			responseBuilder.setResponseType(ResponseType.OTHER_SYS);
			responseBuilder.setExtension(Protocol.otherSystemResponse, otherSystemResponseBuilder.build());
			send(responseBuilder.build());
		}
		
		
	}
	

}
