package com.cai.common.util;

import com.cai.common.domain.AccountWeixinModel;
import com.cai.common.domain.GameNoticeModel;

import protobuf.redis.ProtoRedis.RsAccountWeixinModelResponse;
import protobuf.redis.ProtoRedis.RsGameNoticeModelResponse;

/**
 * model转protobuf
 * @author run
 *
 */
public class ModelToRedisUtil {

	
	public static RsAccountWeixinModelResponse.Builder getRsAccountWeixinModelResponse(AccountWeixinModel accountWeixinModel){
		RsAccountWeixinModelResponse.Builder builder = RsAccountWeixinModelResponse.newBuilder();
		builder.setAccountId(accountWeixinModel.getAccount_id());
		builder.setAccessToken(accountWeixinModel.getAccess_token());
		builder.setRefreshToken(accountWeixinModel.getRefresh_token());
		builder.setOpenid(accountWeixinModel.getOpenid());
		builder.setScope(accountWeixinModel.getScope());
		builder.setUnionid(accountWeixinModel.getUnionid());
		builder.setNickname(accountWeixinModel.getNickname());
		builder.setSex(accountWeixinModel.getSex());
		builder.setProvince(accountWeixinModel.getProvince());
		builder.setCity(accountWeixinModel.getCity());
		builder.setCountry(accountWeixinModel.getCountry());
		builder.setHeadimgurl(accountWeixinModel.getHeadimgurl());
		builder.setPrivilege(accountWeixinModel.getPrivilege());
		builder.setLastFlushTime(accountWeixinModel.getLast_flush_time().getTime());
		builder.setSelfToken(accountWeixinModel.getSelf_token());
		builder.setLastFalseSelfToken(accountWeixinModel.getLast_false_self_token().getTime());
		return builder;
	}

	
	public static RsGameNoticeModelResponse.Builder getRsGameNoticeModelResponse(GameNoticeModel model){
		RsGameNoticeModelResponse.Builder builder = RsGameNoticeModelResponse.newBuilder();
		builder.setId(model.getId());
		builder.setContent(model.getContent());
		builder.setGameType(model.getGame_type());
		builder.setDelay(model.getDelay());
		builder.setCreateTime(model.getCreate_time().getTime());
		builder.setEndTime(model.getEnd_time().getTime());
		builder.setGameId(1);//TODO
		builder.setPayType(model.getPlay_type());
		return builder;
	}
	
}
