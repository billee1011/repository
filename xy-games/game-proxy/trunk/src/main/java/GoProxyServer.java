
/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */

import java.util.Timer;

import com.cai.common.define.ELogType;
import com.cai.common.define.EServerStatus;
import com.cai.common.define.EServerType;
import com.cai.common.define.ESysLogLevelType;
import com.cai.common.domain.ProxyGameServerModel;
import com.cai.common.rmi.ICenterRMIServer;
import com.cai.common.util.AbstractServer;
import com.cai.common.util.GlobalExecutor;
import com.cai.common.util.PerformanceTimer;
import com.cai.common.util.PropertiesUtil;
import com.cai.common.util.RuntimeOpt;
import com.cai.common.util.SpringService;
import com.cai.core.Global;
import com.cai.core.SystemConfig;
import com.cai.dictionary.ServerDict;
import com.cai.net.core.ProxyAcceptorListener;
import com.cai.service.MongoDBServiceImpl;
import com.cai.timer.SocketIOTimer;
import com.cai.util.TestCMD;
import com.xianyi.framework.core.transport.event.IOEventListener;
import com.xianyi.framework.core.transport.netty.NettyWebSocketAcceptor;
import com.xianyi.framework.core.transport.netty.session.C2SSession;
import com.xianyi.framework.server.ServiceManager;

/**
 * 
 *
 * @author wu_hc date: 2017年7月13日 下午5:21:18 <br/>
 */
public final class GoProxyServer extends AbstractServer {

	public static void main(String[] args) {
		PerformanceTimer timer = new PerformanceTimer();
		GoProxyServer server = new GoProxyServer();
		server.setServerType(EServerType.PROXY);
		try {
			// 1基础组件启动
			server.start();



			System.out.println("启动时间:" + timer.getStr());

			MongoDBServiceImpl.getInstance().systemLog(ELogType.startJvm, "启动服务器", null, null, ESysLogLevelType.NONE);

//			if (SystemConfig.gameDebug == 1) {
//				 server.openDebug();
//			}

			// 通知中心服
			SpringService.getBean(ICenterRMIServer.class).serverStatusUpdate(EServerType.PROXY, EServerStatus.ACTIVE,
					SystemConfig.proxy_index);

			// 2网络启动
			server.startAcceptor(SystemConfig.game_socket_port);
			server.startWebSocket();
			// 3统计定时器启动
			new Timer("socketClientProxTimer").scheduleAtFixedRate(new SocketIOTimer(server.getAcceptor().socketIO()),
					60000L, 60000L);

		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	@Override
	public void stop() throws Exception {
		PerformanceTimer timerShudown = new PerformanceTimer();
		System.out.println("shutdown doing.......");
		Global.shutdownThreadPool();
		GlobalExecutor.shutdownGracefully();
		MongoDBServiceImpl.getInstance().systemLog(ELogType.stopJvm, "关闭服务器", null, null, ESysLogLevelType.NONE);

		// 通知当前服务器下线
		SpringService.getBean(ICenterRMIServer.class).serverStatusUpdate(EServerType.PROXY, EServerStatus.CLOSE,
				SystemConfig.proxy_index);

		try {
			MongoDBServiceImpl.getInstance().getMogoDBTimer().handle();
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (SystemConfig.gameDebug == 1) {
			// C2SSessionService.getInstance().writeToFile(Boolean.FALSE);
		}
		System.out.println("关闭服务器耗时:" + timerShudown.getStr());
	}

	@Override
	protected void config(PropertiesUtil prop) {
		SystemConfig.init(prop);
	}

	@Override
	protected Class<? extends IOEventListener<C2SSession>> acceptorListener() {
		return ProxyAcceptorListener.class;
	}

	@Override
	protected void startService() throws Exception {
		ServiceManager.getInstance().load();
	}

	@Override
	protected void debugCmdAccept(String cmd) {
		TestCMD.cmd(cmd);
	}

	@Override
	protected void afterInitCfg() throws Exception {
		ProxyGameServerModel serverModel = ServerDict.getInstance().getProxyGameServerModel();
		SystemConfig.game_socket_port = serverModel.getSocket_port();
		SystemConfig.game_websocket_port = serverModel.getWebsocket_port();
	}

	/**
	 * 启动websocket
	 * 
	 * @throws Exception
	 */
	protected void startWebSocket() throws Exception {
		if (0 != SystemConfig.game_websocket_port) {
			NettyWebSocketAcceptor acceptor = new NettyWebSocketAcceptor(SystemConfig.game_websocket_port);
			acceptor.setWorkCount(RuntimeOpt.availableProcessors());
			Class<? extends IOEventListener<C2SSession>> acceptorBehaviour = acceptorListener();
			acceptor.listener().add(acceptorBehaviour.newInstance());
			acceptor.doInit();
			acceptor.start(false);
		}

	}
}
