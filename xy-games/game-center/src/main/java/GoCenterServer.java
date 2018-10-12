import java.util.List;
import java.util.concurrent.TimeUnit;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.webapp.WebAppContext;

import com.cai.common.define.ELogType;
import com.cai.common.define.EServerType;
import com.cai.common.define.ESysLogLevelType;
import com.cai.common.domain.ProxyGameServerModel;
import com.cai.common.rmi.ICenterRMIServer;
import com.cai.common.util.AbstractServer;
import com.cai.common.util.PerformanceTimer;
import com.cai.common.util.PropertiesUtil;
import com.cai.common.util.SpringService;
import com.cai.core.SystemConfig;
import com.cai.dao.PublicDAO;
import com.cai.dictionary.ServerDict;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.DbBatchRunnable;
import com.cai.future.runnable.GoldGameOpenRunnable;
import com.cai.service.MongoDBServiceImpl;
import com.cai.service.RedisServiceImpl;
import com.cai.service.ServiceManager;
import com.cai.util.TestCMD;
import com.xianyi.framework.core.transport.event.IOEventListener;
import com.xianyi.framework.core.transport.netty.session.C2SSession;

/**
 * 
 * 
 *
 */
public class GoCenterServer extends AbstractServer {

	public static void main(String[] args) throws Exception {

		System.out.println("=====服务器启动===========");
		PerformanceTimer timer = new PerformanceTimer();

		GoCenterServer server = new GoCenterServer();
		try {
			server.setServerType(EServerType.CENTER);
			server.start();

			System.out.println("启动时间:" + timer.getStr());

			// PublicDao
			List<ProxyGameServerModel> list = SpringService.getBean(PublicDAO.class).getProxyGameServerModelList();
			System.out.println("数据库测试:" + list.size());

			// if (SystemConfig.gameDebug == 1) {
			// server.openDebug();
			// }
			MongoDBServiceImpl.getInstance().systemLog(ELogType.startJvm, "启动服务器", null, null, ESysLogLevelType.NONE);
			
			
			GameSchedule.put(new Runnable() {

				@Override
				public void run() {
					SpringService.getBean(ICenterRMIServer.class).reLoadServerDictDictionary();

				}
			}, 10, TimeUnit.SECONDS);
			
			System.out.println("服务器启动完成");

			server.startJettyServer();
			
			
			
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}

	}

	/**
	 * 
	 * @throws Exception
	 */
	private void startJettyServer() throws Exception {
		// //jetty
		// 服务器的监听端口
		Server server = new Server(SystemConfig.webPort);
		// 关联一个已经存在的上下文
		WebAppContext context = new WebAppContext();
		// 设置描述，作为hander加载使用
		context.setDescriptor("./webapp/WEB-INF/web.xml");
		// 设置Web内容上下文路径
		context.setResourceBase("./webapp");
		// 设置上下文路径既访问路径的根路径
		context.setContextPath("/center");
		context.setClassLoader(Thread.currentThread().getContextClassLoader());
		context.setConfigurationDiscovered(true);
		context.setParentLoaderPriority(true);
		server.setHandler(context);
		server.start();
		server.join();
	}

	@Override
	public void stop() throws Exception {
		PerformanceTimer timerShudown = new PerformanceTimer();
		System.out.println("shutdown doing.......");

		try {
			MongoDBServiceImpl.getInstance().systemLog(ELogType.stopJvm, "关闭服务器", null, null, ESysLogLevelType.NONE);
		} catch (Exception e1) {
			e1.printStackTrace();
		}

		// service退出
		ServiceManager.getInstance().stop();
		// mysql入库
		try {
			// PublicServiceImpl.getInstance().getDbSyncTimer().handle();
			DbBatchRunnable dbBatchRunnable = new DbBatchRunnable();
			dbBatchRunnable.run();
		} catch (Exception e) {
			e.printStackTrace();
		}

		// mongodb入库
		try {
			MongoDBServiceImpl.getInstance().getMogoDBTimer().handle();
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (SystemConfig.gameDebug == 0) {
			// 清除数据
			try {
//				RedisServiceImpl.getInstance().clearCache();
			} catch (Exception e) {
				e.printStackTrace();
			}

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
		return null;
	}

	@Override
	protected void debugCmdAccept(String cmd) {
		TestCMD.cmd(cmd);
	}

}
