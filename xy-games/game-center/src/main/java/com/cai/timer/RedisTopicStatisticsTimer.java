package com.cai.timer;

import java.util.TimerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;

import com.cai.common.define.ELogType;
import com.cai.common.define.ESysLogLevelType;
import com.cai.common.util.SpringService;
import com.cai.common.util.ThreadUtil;
import com.cai.redis.listener.TopicCenterMessageDelegate;
import com.cai.service.MongoDBServiceImpl;

/**
 * redis主题消息统计
 * 
 * @author run
 *
 */
public class RedisTopicStatisticsTimer extends TimerTask {

	private static Logger logger = LoggerFactory.getLogger(RedisTopicStatisticsTimer.class);

	/**
	 * 上一次的topicCenter消息记录
	 */
	private long lastTopicCenter = 0;

	/**
	 * 上一次的topAll消息记录
	 */
	private long lastTopicAll = 0;

	@Override
	public void run() {

		try {

			MessageListenerAdapter messageListenerAdapter = (MessageListenerAdapter) SpringService.getBean("messageListenerTopicCenter");
			TopicCenterMessageDelegate topicCenterMessageDelegate = (TopicCenterMessageDelegate) messageListenerAdapter.getDelegate();
			long centerCount = topicCenterMessageDelegate.getMesCount().get();

			// 本次增加的记录
			long topicCenterChange = centerCount - lastTopicCenter;

			//
			lastTopicCenter = centerCount;
			
			MongoDBServiceImpl.getInstance().systemLog(ELogType.redisTopicCenter, null, topicCenterChange, null, ESysLogLevelType.NONE);
			
		} catch (Exception e) {
			logger.error("error", e);
			MongoDBServiceImpl.getInstance().server_error_log(0,ELogType.unkownError, ThreadUtil.getStack(e), 0L, null);
		}

	}

}
