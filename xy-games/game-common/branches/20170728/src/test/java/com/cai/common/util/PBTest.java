/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.common.util;

import java.util.List;

import com.google.common.collect.Lists;

import protobuf.clazz.Protocol.RoomRequest;
import protobuf.clazz.ddz.DdzRsp.DdzCallReq;

/**
 * 
 *
 * @author wu date: 2017年7月17日 下午1:08:01 <br/>
 */
public final class PBTest {

	public static void main(String[] args) {
		// encode

		RoomRequest.Builder roomReqBuilder = RoomRequest.newBuilder();
		roomReqBuilder.setType(1001);
		DdzCallReq.Builder builder = DdzCallReq.newBuilder();
		builder.setId(324234);
		builder.setResult(5444444);
		roomReqBuilder.setCommRequet(PBUtil.toByteString(builder));

		DdzCallReq req = PBUtil.toObject(roomReqBuilder.build(), DdzCallReq.class);
		System.out.println(req.getId() + "\t" + req.getResult());

		List<Integer> k = Lists.newArrayList();
		k.add(1, 344);
		k.add(5, 23);
		System.out.println(k);
	}

}
