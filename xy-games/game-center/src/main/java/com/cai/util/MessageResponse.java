package com.cai.util;

import com.cai.common.domain.GlobalModel;

import protobuf.redis.ProtoRedis.RsSystemStopReadyStatusResponse;

public class MessageResponse {
	
	public static RsSystemStopReadyStatusResponse getRsSystemStopReadyStatusResponse(GlobalModel globalModel){
		RsSystemStopReadyStatusResponse.Builder builder = RsSystemStopReadyStatusResponse.newBuilder();
		builder.setSystemStopReady(globalModel.isSystemStopReady());
		if(globalModel.isSystemStopReady()){
			builder.setStopData(globalModel.getStopDate().getTime());
		}
		return builder.build();
	}
	

}
