/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 *
 */
package com.cai.timer;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.TimerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import com.cai.common.define.ERankType;
import com.cai.common.define.ERedisTopicType;
import com.cai.common.domain.AccountModel;
import com.cai.common.domain.AccountSimple;
import com.cai.common.domain.RankModel;
import com.cai.common.util.ModelToRedisUtil;
import com.cai.common.util.SpringService;
import com.cai.service.PublicService;
import com.cai.service.PublicServiceImpl;
import com.cai.service.RankServiceImp;
import com.cai.service.RedisServiceImpl;
import com.google.common.collect.Lists;

import protobuf.redis.ProtoRedis.RedisResponse;
import protobuf.redis.ProtoRedis.RedisResponse.RsResponseType;
import protobuf.redis.ProtoRedis.RsRankResponse;

/**
 * 排行榜定时器
 * 
 * @author wu_hc
 */
public final class RankAndNoticeTimer extends TimerTask {

	/**
	 * 日志
	 */
	private static Logger log = LoggerFactory.getLogger(RankAndNoticeTimer.class);

	@Override
	public void run() {
		long current = System.currentTimeMillis();
//		List<AccountModel> dbList = SpringService.getBean(PublicService.class).getPublicDAO().getAccountList();// getAccountByMoneyDesc(0,10);
//		if (null == dbList || dbList.isEmpty()) {
//			log.warn("rank msg is null or empty!");
//			return;
//		}
//
//		// 1) 土豪榜：豆存量前十名排行榜，每天0点更新排名。
//		List<RankModel> goldRank = doRankAndSync(ERankType.GOLD, dbList);
//		System.out.println("goldRank: " + goldRank);
//
//		// 2) 富豪榜：金币存量前十名排行榜，每天0点更新排名。
//		List<RankModel> moneyRank = doRankAndSync(ERankType.MONEY, dbList);
//		System.out.println("moneyRank: " + moneyRank);
//		System.out.println((System.currentTimeMillis() - current) + "ms");
	}
	/**
	 * 排序
	 * 
	 * @param type
	 *            排行榜类型
	 * @param source
	 *            目标数据
	 * @return
	 */
	private List<RankModel> doRankAndSync(ERankType type, List<AccountModel> source) {

		List<RankModel> target = Lists.newArrayList();

		Collections.sort(source, new Comparator<AccountModel>() {
			@Override
			public int compare(AccountModel o1, AccountModel o2) {
				long tmp = ERankType.GOLD == type ? (o2.getGold() - o1.getGold()) : (o2.getMoney() - o1.getMoney());
				return (tmp == 0L) ? 0 : (tmp > 0L ? 1 : -1);
			}
		});

		RankModel rm = null;
		for (int i = 0; i < source.size() && i < 10; i++) {
			AccountModel m = source.get(i);
			AccountSimple simple = PublicServiceImpl.getInstance().getAccountSimpe(m.getAccount_id());
			if(simple == null){
				continue;
			}
			rm = new RankModel();
			rm.setAccountId(m.getAccount_id());
			rm.setHead(null != simple ? simple.getIcon() : "");
			rm.setNickName(simple.getNick_name());
			rm.setRank(i + 1);
			rm.setValue(type == ERankType.GOLD ? m.getGold() : m.getMoney());
			rm.setSignature(StringUtils.isEmpty(m.getSignature()) ? "该用户很懒，什么都没写" : m.getSignature());
			target.add(rm);
		}

		// 通过redis发布到各个proxy服务器
		RsRankResponse.Builder rankRsp = ModelToRedisUtil.getRankModelResp(type, target);

		RedisResponse.Builder redisResponseBuilder = RedisResponse.newBuilder();
		redisResponseBuilder.setRsResponseType(RsResponseType.RANK);
		redisResponseBuilder.setRsRankRsp(rankRsp);
		RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(),
				ERedisTopicType.topicProxy);

		// 缓存，
		RankServiceImp.getInstance().addRank(type, target);

		return target;
	}
}
