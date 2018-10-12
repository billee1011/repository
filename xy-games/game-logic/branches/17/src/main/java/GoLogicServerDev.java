import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.apache.log4j.PropertyConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import com.cai.common.define.ELogType;
import com.cai.common.define.ESysLogLevelType;
import com.cai.common.util.PerformanceTimer;
import com.cai.common.util.PropertiesUtil;
import com.cai.common.util.ServerListenUtil;
import com.cai.common.util.SpringService;
import com.cai.common.util.WRSystem;
import com.cai.core.SystemConfig;
import com.cai.net.server.GameSocketServer;
import com.cai.service.MongoDBServiceImpl;
import com.cai.service.ServiceManager;
import com.cai.util.TestCMD;

public class GoLogicServerDev {

	private static Logger logger = LoggerFactory.getLogger(GoLogicServerDev.class);

	public static void main(String[] args) throws Exception {

		System.out.println("=====服务器启动===========");
		PerformanceTimer timer = new PerformanceTimer();
		PropertyConfigurator.configureAndWatch(WRSystem.HOME + "config/log4j.properties", 5000);
		PropertiesUtil prop = new PropertiesUtil(WRSystem.HOME + "config/config.properties");
		SystemConfig.init(prop);

		MDC.put("server_name", "logic_" + SystemConfig.logic_index);

		// 系统信息
		ServerListenUtil.sysInfo();

		// 启动spring
		SpringService.start();

		// 开启服务类
		ServiceManager.getInstance().load();

		// 中转与客户端socket
		new GameSocketServer().start();

		// ProcesserManager.reloadRequestHandlerMapping();
		// ProcesserManager.reloadResponseMapping();

		System.out.println("启动时间:" + timer.getStr());

		MongoDBServiceImpl.getInstance().systemLog(ELogType.startJvm, "启动服务器", null, null, ESysLogLevelType.NONE);
		// jvm停止时处理
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				PerformanceTimer timerShudown = new PerformanceTimer();
				System.out.println("shutdown doing.......");

				MongoDBServiceImpl.getInstance().systemLog(ELogType.stopJvm, "关闭服务器", null, null, ESysLogLevelType.NONE);

				try {

					// mongodb入库
					MongoDBServiceImpl.getInstance().getMogoDBTimer().handle();

				} catch (Exception e) {
					e.printStackTrace();
				}

				System.out.println("关闭服务器耗时:" + timerShudown.getStr());

			}
		});

		// FIXME 开发命令
		Thread t2 = new Thread(new Runnable() {
			@Override
			public void run() {
				System.out.println("开发命令输入监听启动");
				while (true) {
					try {
						BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
						String str = br.readLine();
						TestCMD.cmd(str);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		});
		t2.start();
	}
}
