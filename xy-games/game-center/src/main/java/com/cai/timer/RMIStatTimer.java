package com.cai.timer;

import java.util.Map;
import java.util.TimerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.define.EServerStatus;
import com.cai.common.domain.LogicGameServerModel;
import com.cai.common.domain.ProxyGameServerModel;
import com.cai.common.rmi.ILogicRMIServer;
import com.cai.common.rmi.IProxyRMIServer;
import com.cai.common.util.RMIUtil;
import com.cai.dictionary.ServerDict;
import com.cai.service.RMIServiceImpl;

/**
 * RMI状态检测
 * 
 * @author run
 *
 */
public class RMIStatTimer extends TimerTask {

	private static Logger logger = LoggerFactory.getLogger(RMIStatTimer.class);

	@Override
	public void run() {
		try {

			// 1代理服检查
			ProxyRMIStatChecker proxyChecker = new ProxyRMIStatChecker();
			proxyChecker.start();

			// 2逻辑服检查
			LogicRMIStatChecker logicChecker = new LogicRMIStatChecker();
			logicChecker.start();

			// StringBuilder buf = new StringBuilder();
			// buf.append("检测RMI状态:代理服:").append(proxySuccessCount).append("/").append(proxyCount).append(",逻辑服:").append(logicSuccessCount).append("/")
			// .append(logicCoun);
			//
			// // ===========================
			// MongoDBServiceImpl.getInstance().systemLog(ELogType.rmiTest,
			// buf.toString(), failCount, null, ESysLogLevelType.NONE);

		} catch (Exception e) {
			logger.error("error", e);
		}

	}

	/**
	 * 
	 * 
	 *
	 * @author wu_hc date: 2017年8月7日 下午2:43:34 <br/>
	 */
	private final class ProxyRMIStatChecker extends Thread {

		@Override
		public void run() {

			logger.debug("[center<->proxy] 中心服开始检测代理服的RMI");

			long currentMs = System.currentTimeMillis();

			Map<Integer, ProxyGameServerModel> proxyMap = ServerDict.getInstance().getProxyGameServerModelDict();

			for (final Map.Entry<Integer, IProxyRMIServer> entry : RMIServiceImpl.getInstance().getProxyRMIServerMap().entrySet()) {
				final ProxyGameServerModel serverModel = proxyMap.get(entry.getKey());
				if (currentMs - serverModel.getLastPingTime() > RMIUtil.RMI_PING_TIME_OUT) { // 超时了
					logger.error("IProxyRMIServer[{},{},{}] 和中心服rmi连接超时!", entry.getKey(), entry.getValue(), serverModel);
					try {
						logger.info("中心服主动尝试重连代理服[{}]开始！", entry.getKey());
						entry.getValue().test();
						logger.info("中心服主动尝试重连代理服[{}]成功！", entry.getKey());
						serverModel.setLastPingTime(currentMs);
					} catch (Exception e) {
						logger.info("中心服主动尝试重连代理服[{}]失败！", entry.getKey());
						serverModel.setStatus(EServerStatus.CLOSE);
					}
				} /*
					 * else { // 如果不是正在维护状态，直接设置为活跃 if (EServerStatus.REPAIR !=
					 * serverModel.getStatus()) {
					 * serverModel.setStatus(EServerStatus.ACTIVE); } }
					 */
			}
		}

	}

	/**
	 * 
	 * 
	 *
	 * @author wu_hc date: 2017年8月7日 下午2:43:48 <br/>
	 */
	private final class LogicRMIStatChecker extends Thread {

		@Override
		public void run() {

			logger.debug("[center<->proxy] 中心服开始检测逻辑服的RMI");
			Map<Integer, LogicGameServerModel> logicMap = ServerDict.getInstance().getLogicGameServerModelDict();

			long currentMs = System.currentTimeMillis();

			for (final Map.Entry<Integer, ILogicRMIServer> entry : RMIServiceImpl.getInstance().getLogicRMIServerMap().entrySet()) {
				final LogicGameServerModel serverModel = logicMap.get(entry.getKey());
				if (currentMs - serverModel.getLastPingTime() > RMIUtil.RMI_PING_TIME_OUT) { // 超时了
					logger.error("ILogicRMIServer[{},{}] 和中心服rmi连接超时!", entry.getKey(), serverModel);
					try {
						logger.info("中心服主动尝试重连逻辑服[{}]开始！", entry.getKey());
						entry.getValue().test();
						logger.info("中心服主动尝试重连逻辑服[{}]成功！", entry.getKey());
						serverModel.setLastPingTime(currentMs);

					} catch (Exception e) {
						logger.info("中心服主动尝试重连逻辑服[{}]失败！", entry.getKey());
						serverModel.setStatus(EServerStatus.CLOSE);
					}
				} /*
					 * else { // 如果不是正在维护状态，直接设置为活跃 if (EServerStatus.REPAIR !=
					 * serverModel.getStatus()) {
					 * serverModel.setStatus(EServerStatus.ACTIVE); } }
					 */
			}
		}
	}

}
