/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */

import com.cai.common.define.EServerStatus;
import com.cai.common.define.EServerType;
import com.cai.common.rmi.ICenterRMIServer;
import com.cai.common.util.AbstractServer;
import com.cai.common.util.GlobalExecutor;
import com.cai.common.util.PerformanceTimer;
import com.cai.common.util.PropertiesUtil;
import com.cai.common.util.ServerListenUtil;
import com.cai.common.util.SpringService;
import com.cai.config.SystemConfig;
import com.cai.dictionary.CityDict;
import com.cai.dictionary.ClubWelfareRewardDict;
import com.cai.dictionary.ClubWelfareSwitchModelDict;
import com.cai.dictionary.DirtyWordDict;
import com.cai.dictionary.GameGroupRuleDict;
import com.cai.dictionary.ItemDict;
import com.cai.dictionary.ServerDict;
import com.cai.dictionary.SysGameTypeDict;
import com.cai.dictionary.SysParamDict;
import com.cai.dictionary.SysParamServerDict;
import com.cai.net.core.ClubAcceptorListener;
import com.xianyi.framework.core.service.ServiceManager;
import com.xianyi.framework.core.transport.Acceptor;
import com.xianyi.framework.core.transport.event.IOEventListener;
import com.xianyi.framework.core.transport.netty.NettySocketAcceptor;
import com.xianyi.framework.core.transport.netty.session.C2SSession;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.webapp.WebAppContext;

/**
 * @author wu date: 2017年8月29日 下午1:29:04 <br/>
 */
public final class GoClubServer extends AbstractServer {

	public static void main(String[] args) {
		GoClubServer server = new GoClubServer();
		server.setServerType(EServerType.CLUB);
		try {
			ServerListenUtil.sysInfo();
			// 1基础组件启动
			server.start();

			//			List<ClubModel> list = SpringService.getBean(ClubDao.class).getClubList();
			//			System.out.println("数据库测试:" + list.size());

			// 2注册到中心服
			boolean s = SpringService.getBean(ICenterRMIServer.class)
					.serverStatusUpdate(EServerType.CLUB, EServerStatus.ACTIVE, SystemConfig.club_index);
			if (!s) {
				server.log.error("#########=====注册到中心服失败，请检查 ============#########");
				System.exit(1);
			}

			// 3网络启动
			server.startAcceptor(SystemConfig.game_socket_port);

			// 4启动jetty服务器
			server.startJettyServer();

		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	/**
	 * @throws Exception
	 */
	private void startJettyServer() throws Exception {
		Server jettyServer = new Server(SystemConfig.webPort);
		// 关联一个已经存在的上下文
		WebAppContext context = new WebAppContext();
		// 设置描述，作为hander加载使用
		context.setDescriptor("./webapp/WEB-INF/web.xml");
		// 设置Web内容上下文路径
		context.setResourceBase("./webapp");
		// 设置上下文路径既访问路径的根路径
		context.setContextPath("/gameClub");
		context.setClassLoader(Thread.currentThread().getContextClassLoader());
		context.setConfigurationDiscovered(true);
		context.setParentLoaderPriority(true);
		jettyServer.setHandler(context);
		jettyServer.start();
		jettyServer.join();
	}

	@Override
	public void stop() throws Exception {
		PerformanceTimer timerShudown = new PerformanceTimer();
		System.out.println("GoClubServer shutdown doing.......");

		try {
			// 通知当前服务器下线
			SpringService.getBean(ICenterRMIServer.class).serverStatusUpdate(EServerType.CLUB, EServerStatus.CLOSE, SystemConfig.club_index);
		} catch (Exception e) {
			log.error("", e);
		}

		ServiceManager.getInstance().stop();
		GlobalExecutor.shutdownGracefully();
		System.out.println("关闭服务器耗时:" + timerShudown.getStr());
	}

	@Override
	protected void config(PropertiesUtil prop) {
		SystemConfig.init(prop);
	}

	@Override
	protected Class<? extends IOEventListener<C2SSession>> acceptorListener() {
		return ClubAcceptorListener.class;
	}

	@Override
	protected void startService() throws Exception {
		SysGameTypeDict.getInstance().load();
		SysParamServerDict.getInstance().load();
		SysParamDict.getInstance().load();
		// MatchDict.getInstance().load();
		ServerDict.getInstance().load();
		GameGroupRuleDict.getInstance().load();

		DirtyWordDict.getInstance().load();
		ItemDict.getInstance().load();
		CityDict.getInstance().load();
		ClubWelfareSwitchModelDict.getInstance().load(false);
		ClubWelfareRewardDict.getInstance().load();

		ServiceManager.getInstance().loadServices(new String[] { "com.cai.service" });
		ServiceManager.getInstance().start();
	}

	@Override
	protected void initAcceptor(Acceptor acceptor) {
		((NettySocketAcceptor) acceptor).setIsClientAcceptor(Boolean.FALSE);
	}
}
