package com.cai.util;

import com.google.protobuf.GeneratedMessage;

import protobuf.clazz.match.MatchClientHeaderRsp.MatchClientResponse;

public class MatchPBButils {
	public static MatchClientResponse.Builder getMatchResponse(int cmd , GeneratedMessage.Builder<?> builder){
		MatchClientResponse.Builder b = MatchClientResponse.newBuilder();
		b.setCmd(cmd);
		b.setData(builder.build().toByteString());
		return b;
	}
}
