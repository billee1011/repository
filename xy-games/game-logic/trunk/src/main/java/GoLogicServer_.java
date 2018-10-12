
/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
import com.cai.common.define.EIODire;
import com.cai.common.define.ELogType;
import com.cai.common.define.EServerStatus;
import com.cai.common.define.EServerType;
import com.cai.common.define.ESysLogLevelType;
import com.cai.common.rmi.ICenterRMIServer;
import com.cai.common.util.AbstractServer;
import com.cai.common.util.PerformanceTimer;
import com.cai.common.util.PropertiesUtil;
import com.cai.common.util.SpringService;
import com.cai.core.SystemConfig;
import com.cai.net.core.LogicAcceptorListener;
import com.cai.service.MongoDBServiceImpl;
import com.cai.service.ServiceManager;
import com.xianyi.framework.core.transport.event.IOEventListener;
import com.xianyi.framework.core.transport.netty.session.C2SSession;

/**
 * 
 *
 * @author wu_hc date: 2017年10月10日 下午4:21:18 <br/>
 */
public final class GoLogicServer_ extends AbstractServer {

	public static void main(String[] args) {
		PerformanceTimer timer = new PerformanceTimer();
		GoLogicServer_ server = new GoLogicServer_();
		server.setServerType(EServerType.LOGIC);

		try {
			// 1基础组件启动
			server.start();

			// 2网络启动
			server.startAcceptor(SystemConfig.game_socket_port);
			server.getAcceptor().socketIO().setDire(EIODire.NONE);

			System.out.println("启动时间:" + timer.getStr());

			MongoDBServiceImpl.getInstance().systemLog(ELogType.startJvm, "启动服务器", null, null, ESysLogLevelType.NONE);

		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	@Override
	public void stop() throws Exception {
		PerformanceTimer timerShudown = new PerformanceTimer();
		System.out.println("shutdown doing.......");

		MongoDBServiceImpl.getInstance().systemLog(ELogType.stopJvm, "关闭服务器", null, null, ESysLogLevelType.NONE);

		// 通知中心服
		SpringService.getBean(ICenterRMIServer.class).serverStatusUpdate(EServerType.LOGIC, EServerStatus.CLOSE, SystemConfig.logic_index);
		try {
			MongoDBServiceImpl.getInstance().getMogoDBTimer().handle();
		} catch (Exception e) {
			e.printStackTrace();
		}

		System.out.println("关闭服务器耗时:" + timerShudown.getStr());
	}

	@Override
	protected void config(PropertiesUtil prop) {
		SystemConfig.init(prop);
	}

	@Override
	protected void startService() throws Exception {
		ServiceManager.getInstance().load();
	}

	@Override
	protected Class<? extends IOEventListener<C2SSession>> acceptorListener() {
		return LogicAcceptorListener.class;
	}

}
