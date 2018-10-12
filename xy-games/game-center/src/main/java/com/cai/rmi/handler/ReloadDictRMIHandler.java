package com.cai.rmi.handler;

import com.cai.common.constant.RMICmd;
import com.cai.common.define.ERedisTopicType;
import com.cai.common.dictionary.DictType;
import com.cai.common.rmi.IRMIHandler;
import com.cai.common.rmi.IRmi;
import com.cai.dictionary.ActivityMissionDict;
import com.cai.dictionary.ActivityRedpacketPoolDict;
import com.cai.dictionary.CardCategoryDict;
import com.cai.dictionary.CardSecretDict;
import com.cai.dictionary.ChannelModelDict;
import com.cai.dictionary.CityDict;
import com.cai.dictionary.CoinCornucopiaDict;
import com.cai.dictionary.CoinDict;
import com.cai.dictionary.CoinExciteDict;
import com.cai.dictionary.HallGuideDict;
import com.cai.dictionary.InviteActiveDict;
import com.cai.dictionary.InviteFriendsActivityDict;
import com.cai.dictionary.ItemExchangeDict;
import com.cai.dictionary.MatchBroadDict;
import com.cai.dictionary.MatchDict;
import com.cai.dictionary.NoticeDict;
import com.cai.dictionary.PushManagerDict;
import com.cai.dictionary.RecommendLimitDict;
import com.cai.dictionary.SdkAppDict;
import com.cai.dictionary.SdkDiamondShopDict;
import com.cai.dictionary.TurntableDict;
import com.cai.dictionary.WelfareExchangeDict;
import com.cai.dictionary.WelfareGoodsTypeDict;
import com.cai.service.BonusPointsService;
import com.cai.service.ClubExclusiveService;
import com.cai.service.RedisServiceImpl;

import protobuf.redis.ProtoRedis.RedisResponse;
import protobuf.redis.ProtoRedis.RedisResponse.RsResponseType;
import protobuf.redis.ProtoRedis.RsDictUpdateResponse;
import protobuf.redis.ProtoRedis.RsDictUpdateResponse.RsDictType;

/**
 * @author tang date: 2017年11月27日 下午3:41:24 <br/>
 */
@IRmi(cmd = RMICmd.DICT_RELOAD, desc = "加载字典")
public final class ReloadDictRMIHandler extends IRMIHandler<Integer, Integer> {

	@Override
	public Integer execute(Integer type) {
		int result = 0;

		if (type == DictType.CLUB_EXCLUSIVE_ACTIVITY) {
			ClubExclusiveService.getInstance().activityCfgReload(Boolean.TRUE);
		} else if (type == DictType.MATCH_BROAD_LOAD) {
			MatchBroadDict.getInstance().load();
			RedisResponse.Builder redisResponseBuilder = RedisResponse.newBuilder();
			redisResponseBuilder.setRsResponseType(RsResponseType.DICT_UPDATE);
			RsDictUpdateResponse.Builder rsDictUpdateResponseBuilder = RsDictUpdateResponse.newBuilder();
			rsDictUpdateResponseBuilder.setRsDictType(RsDictType.MATCH_BROAD);
			redisResponseBuilder.setRsDictUpdateResponse(rsDictUpdateResponseBuilder);
			RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(), ERedisTopicType.topProxAndLogic);
		} else if (type == DictType.MATCH_DICT_LOAD) {
			MatchDict.getInstance().load();
			RedisResponse.Builder redisResponseBuilder = RedisResponse.newBuilder();
			redisResponseBuilder.setRsResponseType(RsResponseType.DICT_UPDATE);
			RsDictUpdateResponse.Builder rsDictUpdateResponseBuilder = RsDictUpdateResponse.newBuilder();
			rsDictUpdateResponseBuilder.setRsDictType(RsDictType.MATCH_GROUND);
			redisResponseBuilder.setRsDictUpdateResponse(rsDictUpdateResponseBuilder);
			RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(), ERedisTopicType.topicMatch);
		} else if (type == DictType.TURNTABLE_DICT_LOAD) {
			TurntableDict.getInstance().load();
			RedisResponse.Builder redisResponseBuilder = RedisResponse.newBuilder();
			redisResponseBuilder.setRsResponseType(RsResponseType.DICT_UPDATE);
			RsDictUpdateResponse.Builder rsDictUpdateResponseBuilder = RsDictUpdateResponse.newBuilder();
			rsDictUpdateResponseBuilder.setRsDictType(RsDictType.TURNTABLE);
			redisResponseBuilder.setRsDictUpdateResponse(rsDictUpdateResponseBuilder);
			RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(), ERedisTopicType.topProxAndLogic);
			RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(), ERedisTopicType.topicFoundation);
		} else if (type == DictType.INVITE_ACTIVITY) {
			InviteActiveDict.getInstance().load();
			RedisResponse.Builder redisResponseBuilder = RedisResponse.newBuilder();
			redisResponseBuilder.setRsResponseType(RsResponseType.DICT_UPDATE);
			RsDictUpdateResponse.Builder rsDictUpdateResponseBuilder = RsDictUpdateResponse.newBuilder();
			rsDictUpdateResponseBuilder.setRsDictType(RsDictType.INVITE_REDPACKEY);
			redisResponseBuilder.setRsDictUpdateResponse(rsDictUpdateResponseBuilder);
			RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(), ERedisTopicType.topicProxy);
		} else if (type == DictType.COIN_LOAD) {
			CoinDict.getInstance().load();
			RedisResponse.Builder redisResponseBuilder = RedisResponse.newBuilder();
			redisResponseBuilder.setRsResponseType(RsResponseType.DICT_UPDATE);
			RsDictUpdateResponse.Builder rsDictUpdateResponseBuilder = RsDictUpdateResponse.newBuilder();
			rsDictUpdateResponseBuilder.setRsDictType(RsDictType.COIN_CONFIG);
			redisResponseBuilder.setRsDictUpdateResponse(rsDictUpdateResponseBuilder);
			RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(), ERedisTopicType.topProxAndLogic);
		} else if (type == DictType.RECOMMEND_ACCOUNT_LIMIT) {
			// 无需通知其他服务器
			RecommendLimitDict.getInstance().load();
			CityDict.getInstance().load();
		} else if (type == DictType.ITEM_EXCHANGE) {
			ItemExchangeDict.getInstance().load();
			RedisResponse.Builder redisResponseBuilder = RedisResponse.newBuilder();
			redisResponseBuilder.setRsResponseType(RsResponseType.DICT_UPDATE);
			RsDictUpdateResponse.Builder rsDictUpdateResponseBuilder = RsDictUpdateResponse.newBuilder();
			rsDictUpdateResponseBuilder.setRsDictType(RsDictType.ITEM_EXCHANGE);
			redisResponseBuilder.setRsDictUpdateResponse(rsDictUpdateResponseBuilder);
			RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(), ERedisTopicType.topicProxy);
			RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(), ERedisTopicType.topicMatch);
		} else if (type == DictType.WELFARE_EXCHANGE) {
			WelfareExchangeDict.getInstance().load();
			RedisResponse.Builder redisResponseBuilder = RedisResponse.newBuilder();
			redisResponseBuilder.setRsResponseType(RsResponseType.DICT_UPDATE);
			RsDictUpdateResponse.Builder rsDictUpdateResponseBuilder = RsDictUpdateResponse.newBuilder();
			rsDictUpdateResponseBuilder.setRsDictType(RsDictType.WELFARE_EXCHANGE);
			redisResponseBuilder.setRsDictUpdateResponse(rsDictUpdateResponseBuilder);
			RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(), ERedisTopicType.topicProxy);
			RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(), ERedisTopicType.topicMatch);
		} else if (type == DictType.CARD_SECRET) {
			CardSecretDict.getInstance().load();
			RedisResponse.Builder redisResponseBuilder = RedisResponse.newBuilder();
			redisResponseBuilder.setRsResponseType(RsResponseType.DICT_UPDATE);
			RsDictUpdateResponse.Builder rsDictUpdateResponseBuilder = RsDictUpdateResponse.newBuilder();
			rsDictUpdateResponseBuilder.setRsDictType(RsDictType.CARD_SECRET);
			redisResponseBuilder.setRsDictUpdateResponse(rsDictUpdateResponseBuilder);
			RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(), ERedisTopicType.topicMatch);
		} else if (type == DictType.REDPACKET_POOL) {
			ActivityRedpacketPoolDict.getInstance().load();
			RedisResponse.Builder redisResponseBuilder = RedisResponse.newBuilder();
			redisResponseBuilder.setRsResponseType(RsResponseType.DICT_UPDATE);
			RsDictUpdateResponse.Builder rsDictUpdateResponseBuilder = RsDictUpdateResponse.newBuilder();
			rsDictUpdateResponseBuilder.setRsDictType(RsDictType.REDPACKET_POOL);
			redisResponseBuilder.setRsDictUpdateResponse(rsDictUpdateResponseBuilder);
			RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(), ERedisTopicType.topicFoundation);
		} else if (type == DictType.CHANNEL_DICT) {
			ChannelModelDict.getInstance().load();
			RedisResponse.Builder redisResponseBuilder = RedisResponse.newBuilder();
			redisResponseBuilder.setRsResponseType(RsResponseType.DICT_UPDATE);
			RsDictUpdateResponse.Builder rsDictUpdateResponseBuilder = RsDictUpdateResponse.newBuilder();
			rsDictUpdateResponseBuilder.setRsDictType(RsDictType.CHANNEL_DICT);
			redisResponseBuilder.setRsDictUpdateResponse(rsDictUpdateResponseBuilder);
			RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(), ERedisTopicType.topicProxy);
		} else if (type == DictType.PUSH_MANAGER_DICT) {
			PushManagerDict.getInstance().load();
			RedisResponse.Builder redisResponseBuilder = RedisResponse.newBuilder();
			redisResponseBuilder.setRsResponseType(RsResponseType.DICT_UPDATE);
			RsDictUpdateResponse.Builder rsDictUpdateResponseBuilder = RsDictUpdateResponse.newBuilder();
			rsDictUpdateResponseBuilder.setRsDictType(RsDictType.PUSH_MANAGER_DICT);
			redisResponseBuilder.setRsDictUpdateResponse(rsDictUpdateResponseBuilder);
			RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(), ERedisTopicType.topicFoundation);
			RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(), ERedisTopicType.topicMatch);
		} else if (type == DictType.ACTIVITY_MISSION) {
			ActivityMissionDict.getInstance().load();
			RedisResponse.Builder redisResponseBuilder = RedisResponse.newBuilder();
			redisResponseBuilder.setRsResponseType(RsResponseType.DICT_UPDATE);
			RsDictUpdateResponse.Builder rsDictUpdateResponseBuilder = RsDictUpdateResponse.newBuilder();
			rsDictUpdateResponseBuilder.setRsDictType(RsDictType.ACTIVITY_MISSION);
			redisResponseBuilder.setRsDictUpdateResponse(rsDictUpdateResponseBuilder);
			RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(), ERedisTopicType.topicFoundation);
		} else if (type == DictType.GAME_NOTICE_DICT) {
			NoticeDict.INSTANCE().load();
			RedisResponse.Builder redisResponseBuilder = RedisResponse.newBuilder();
			redisResponseBuilder.setRsResponseType(RsResponseType.DICT_UPDATE);
			RsDictUpdateResponse.Builder rsDictUpdateResponseBuilder = RsDictUpdateResponse.newBuilder();
			rsDictUpdateResponseBuilder.setRsDictType(RsDictType.GAME_NOTICE_DICT);
			redisResponseBuilder.setRsDictUpdateResponse(rsDictUpdateResponseBuilder);
			RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(), ERedisTopicType.topProxAndLogic);
		} else if (type == DictType.HALL_GUIDE_DICT) {
			HallGuideDict.getInstance().load();
			RedisResponse.Builder redisResponseBuilder = RedisResponse.newBuilder();
			redisResponseBuilder.setRsResponseType(RsResponseType.DICT_UPDATE);
			RsDictUpdateResponse.Builder rsDictUpdateResponseBuilder = RsDictUpdateResponse.newBuilder();
			rsDictUpdateResponseBuilder.setRsDictType(RsDictType.HALL_GUIDE_DICT);
			redisResponseBuilder.setRsDictUpdateResponse(rsDictUpdateResponseBuilder);
			RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(), ERedisTopicType.topicProxy);
		} else if (type == DictType.BONUS_POINTS_GOODS_DICT) {
			BonusPointsService.getInstance().loadGoods();
		} else if (type == DictType.GAME_RESOURCE_DICT) {
			HallGuideDict.getInstance().loadResource();
			RedisResponse.Builder redisResponseBuilder = RedisResponse.newBuilder();
			redisResponseBuilder.setRsResponseType(RsResponseType.DICT_UPDATE);
			RsDictUpdateResponse.Builder rsDictUpdateResponseBuilder = RsDictUpdateResponse.newBuilder();
			rsDictUpdateResponseBuilder.setRsDictType(RsDictType.GAME_RESOURCE_DICT);
			redisResponseBuilder.setRsDictUpdateResponse(rsDictUpdateResponseBuilder);
			RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(), ERedisTopicType.topicProxy);
		} else if (type == DictType.EXCITE_DICT) {
			CoinExciteDict.getInstance().load();
			RedisResponse.Builder redisResponseBuilder = RedisResponse.newBuilder();
			redisResponseBuilder.setRsResponseType(RsResponseType.DICT_UPDATE);
			RsDictUpdateResponse.Builder rsDictUpdateResponseBuilder = RsDictUpdateResponse.newBuilder();
			rsDictUpdateResponseBuilder.setRsDictType(RsDictType.EXCITE_DICT);
			redisResponseBuilder.setRsDictUpdateResponse(rsDictUpdateResponseBuilder);
			RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(), ERedisTopicType.topProxAndLogic);
		} else if (type == DictType.CARD_CATEGORY) {
			CardCategoryDict.getInstance().load();
			RedisResponse.Builder redisResponseBuilder = RedisResponse.newBuilder();
			redisResponseBuilder.setRsResponseType(RsResponseType.DICT_UPDATE);
			RsDictUpdateResponse.Builder rsDictUpdateResponseBuilder = RsDictUpdateResponse.newBuilder();
			rsDictUpdateResponseBuilder.setRsDictType(RsDictType.CARD_CATEGORY);
			redisResponseBuilder.setRsDictUpdateResponse(rsDictUpdateResponseBuilder);
			RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(), ERedisTopicType.topProxAndLogic);
		} else if (type == DictType.WELFARE_GOODS_TYPE) {
			WelfareGoodsTypeDict.getInstance().load();
			RedisResponse.Builder redisResponseBuilder = RedisResponse.newBuilder();
			redisResponseBuilder.setRsResponseType(RsResponseType.DICT_UPDATE);
			RsDictUpdateResponse.Builder rsDictUpdateResponseBuilder = RsDictUpdateResponse.newBuilder();
			rsDictUpdateResponseBuilder.setRsDictType(RsDictType.WELFARE_GOODS_TYPE);
			redisResponseBuilder.setRsDictUpdateResponse(rsDictUpdateResponseBuilder);
			RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(), ERedisTopicType.topicProxy);
			RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(), ERedisTopicType.topicMatch);
		} else if (type == DictType.INVITE_FRIENDS_ACTIVITY) {
			InviteFriendsActivityDict.getInstance().load();
			RedisResponse.Builder redisResponseBuilder = RedisResponse.newBuilder();
			redisResponseBuilder.setRsResponseType(RsResponseType.DICT_UPDATE);
			RsDictUpdateResponse.Builder rsDictUpdateResponseBuilder = RsDictUpdateResponse.newBuilder();
			rsDictUpdateResponseBuilder.setRsDictType(RsDictType.INVITE_FRIENDS_ACTIVITY);
			redisResponseBuilder.setRsDictUpdateResponse(rsDictUpdateResponseBuilder);
			RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(), ERedisTopicType.topicFoundation);
		} else if (type == DictType.HALL_MAIN_VIEW_BACK_DICT) {
			HallGuideDict.getInstance().loadMainViewBack();
			RedisResponse.Builder redisResponseBuilder = RedisResponse.newBuilder();
			redisResponseBuilder.setRsResponseType(RsResponseType.DICT_UPDATE);
			RsDictUpdateResponse.Builder rsDictUpdateResponseBuilder = RsDictUpdateResponse.newBuilder();
			rsDictUpdateResponseBuilder.setRsDictType(RsDictType.HALL_MAIN_VIEW_BACK);
			redisResponseBuilder.setRsDictUpdateResponse(rsDictUpdateResponseBuilder);
			RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(), ERedisTopicType.topicProxy);
		} else if (type == DictType.SDK_APP_DICT) {
			SdkAppDict.getInstance().load();
			RedisResponse.Builder redisResponseBuilder = RedisResponse.newBuilder();
			redisResponseBuilder.setRsResponseType(RsResponseType.DICT_UPDATE);
			RsDictUpdateResponse.Builder rsDictUpdateResponseBuilder = RsDictUpdateResponse.newBuilder();
			rsDictUpdateResponseBuilder.setRsDictType(RsDictType.SDK_APP);
			redisResponseBuilder.setRsDictUpdateResponse(rsDictUpdateResponseBuilder);
			RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(), ERedisTopicType.topicProxy);

		} else if (type == DictType.CORNUCOPIA) {

			CoinCornucopiaDict.getInstance().load();
			RedisResponse.Builder redisResponseBuilder = RedisResponse.newBuilder();
			redisResponseBuilder.setRsResponseType(RsResponseType.DICT_UPDATE);
			RsDictUpdateResponse.Builder rsDictUpdateResponseBuilder = RsDictUpdateResponse.newBuilder();
			rsDictUpdateResponseBuilder.setRsDictType(RsDictType.CORNUCOPIA);
			redisResponseBuilder.setRsDictUpdateResponse(rsDictUpdateResponseBuilder);
			RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(), ERedisTopicType.topicCoin);
		} else if (type == DictType.SDK_DIAMOND_SHOP) {
			SdkDiamondShopDict.getInstance().load();
			RedisResponse.Builder redisResponseBuilder = RedisResponse.newBuilder();
			redisResponseBuilder.setRsResponseType(RsResponseType.DICT_UPDATE);
			RsDictUpdateResponse.Builder rsDictUpdateResponseBuilder = RsDictUpdateResponse.newBuilder();
			rsDictUpdateResponseBuilder.setRsDictType(RsDictType.SDK_DIAMOND_SHOP);
			redisResponseBuilder.setRsDictUpdateResponse(rsDictUpdateResponseBuilder);
			RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(), ERedisTopicType.topicProxy);
		}

		return result;
	}

}
