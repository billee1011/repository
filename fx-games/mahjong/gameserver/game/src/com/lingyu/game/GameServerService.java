package com.lingyu.game;

import java.net.InetSocketAddress;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

import com.lingyu.common.config.ServerConfig;
import com.lingyu.common.config.ServerConfigManager;
import com.lingyu.common.constant.SystemConstant;
import com.lingyu.common.core.AbstractService;
import com.lingyu.common.core.ServiceException;
import com.lingyu.common.db.GameRepository;
import com.lingyu.common.http.HttpServerPipelineFactory;
import com.lingyu.common.http.RpcBrokerService;
import com.lingyu.common.io.SessionManager;
import com.lingyu.common.io.TrafficShapingHandler;
import com.lingyu.common.manager.MemoryManager;
import com.lingyu.common.resource.ResourceManager;
import com.lingyu.common.util.LogoUtil;
import com.lingyu.game.service.agent.AgentManager;
import com.lingyu.game.service.back.BackServerManager;
import com.lingyu.game.service.event.EventManager;
import com.lingyu.game.service.id.IdManager;
import com.lingyu.game.service.job.AsyncManager;
import com.lingyu.game.service.mahjong.MahjongManager;
import com.lingyu.game.service.mail.SystemMailManager;
import com.lingyu.game.service.role.RoleManager;
import com.lingyu.game.service.user.UserManager;
import com.lingyu.noark.data.DataManager;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.GlobalEventExecutor;

public class GameServerService extends AbstractService {
	private static final Logger logger = LogManager.getLogger(GameServerService.class);
	private EventLoopGroup bossGroup = new NioEventLoopGroup(1);
	private EventLoopGroup workerGroup = new NioEventLoopGroup();

	public GameServerService(String[] args) {
		super(args);
	}

	@Override
	public String getServiceName() {
		return "game server";
	}

	@Override
	protected void onStart() throws ServiceException {
		int type = SystemConstant.SERVER_TYPE_GAME;
		// 加载配置
		ServerConfigManager serverConfigManager = new ServerConfigManager();
		serverConfigManager.load(type, "game-config.xml");
		ServerConfig appConfig = serverConfigManager.getServerConfig();
		appConfig.setType(type);
		GameServerContext.setAppConfig(appConfig);
		GameRepository gameRepository = new GameRepository();
		gameRepository.init(appConfig);
		// 检测数据库的合法性
		gameRepository.checkDBValid();
		GameServerContext.setGameRepository(gameRepository);

		DataManager dataManager = new DataManager(gameRepository.getDataSource(), appConfig.getSaveInterval(),
		        appConfig.getOfflineInterval(), appConfig.isDebug());
		GameServerContext.setDataManager(dataManager);
		// 暂时不要redis 初始化Redis
		// RedisManager cacheManager = new RedisManager();
		// RedisManager.setSubCallback(new CountrySubScribeCallback()); //
		// 设置放在初始化redis监听之前
		// cacheManager.init(appConfig);
		// GameServerContext.setRedisManager(cacheManager);
		logger.info("加载spring配置开始");
		ApplicationContext applicationContext = new FileSystemXmlApplicationContext(
		        new String[] { "classpath:spring-config.xml", "classpath:config/game-spring-task.xml" });
		// 开始执行spring 内置的initialize的方法
		GameServerContext.setAppContext(applicationContext);
		logger.info("加载spring配置完成");
		// 初始化game repository
		// 消息分发器
		RouteManager routeManager = applicationContext.getBean(RouteManager.class);
		GameServerContext.setRouteManager(routeManager);

		// SystemManager systemManager =
		// applicationContext.getBean(SystemManager.class);
		// GameServerContext.setSystemManager(systemManager);
		// 初始化模板管理系统并加载模板
		ResourceManager resourceManager = applicationContext.getBean(ResourceManager.class);
		String dataVersion = resourceManager.init(appConfig.getLocal());
		appConfig.setDataVersion(dataVersion);

		// 定时器 系统
		AsyncManager asyncManager = applicationContext.getBean(AsyncManager.class);
		asyncManager.init();
		GameServerContext.setAsyncManager(asyncManager);
		// ID生成器初始化
		IdManager idManager = applicationContext.getBean(IdManager.class);
		idManager.init();

		// 把user的数据缓存起来
		UserManager userManager = applicationContext.getBean(UserManager.class);
		userManager.init();
		// 代理数据全部加载
		AgentManager agentManager = applicationContext.getBean(AgentManager.class);
		agentManager.init();
		// 处理role的最大id
		RoleManager roleManager = applicationContext.getBean(RoleManager.class);
		roleManager.init();
		// 处理房间的id
		MahjongManager mahjongManager = applicationContext.getBean(MahjongManager.class);
		mahjongManager.init();

		// 向后台注册当前服务器信息
		BackServerManager backServerManager = applicationContext.getBean(BackServerManager.class);
		backServerManager.init();
		GameServerContext.setBackServerManager(backServerManager);

		// 初始化系统邮件模块
		SystemMailManager systemMailManager = applicationContext.getBean(SystemMailManager.class);
		systemMailManager.init();

		// 事件分发系统
		EventManager eventManager = applicationContext.getBean(EventManager.class);
		eventManager.init();

		// 启动内存监控
		MemoryManager.getInstance().initialize();

		// 初始化http 后台用
		this.initHttpServer(appConfig);

		// 初始化tcp。就是socket端口
		this.initGameserver(appConfig);

		// 注册服务器
		// cacheManager.registerServer(serverInfo.getStatus(),
		// appConfig.getServerVersion(), appConfig.getDataVersion(),
		// appConfig.getServerList());
		LogoUtil.print(appConfig.getWorldName(), appConfig.getServerVersion());

	}

	@Override
	protected void onRun() throws ServiceException {
		// Date date = new Date();
		// ScheduleManager.getInstance().schedule(this, "test",
		// DateUtils.addSeconds(date, 20), null,
		// "0/10 * * * * ?");
	}

	// public void test() {
	// logger.info("test");
	//
	// }

	@Override
	protected void onStop() throws ServiceException {
		logger.warn("stopping {}", this.getServiceName());
		ServerConfig config = GameServerContext.getAppConfig();
		try {
			// 必要业务数据保存
			Set<Long> list = SessionManager.getInstance().getOnlineRoleIdList();
			for (long roleId : list) {
				try {
					// 有必要吗？？？？
					GameServerContext.getDataManager().flushDataContainer(roleId);
				} catch (Exception e) {
					logger.error(e.getMessage(), e);
				}
			}
		} finally {
			// 数据回写中心关闭
			GameServerContext.getDataManager().shutdown();
			// 服务器注销
			GameServerContext.getBackServerManager().stopServer(SystemConstant.SERVER_STATUS_STOPED,
			        config.getServerList());
			// 优雅退出，释放线程池资源
			bossGroup.shutdownGracefully();
			workerGroup.shutdownGracefully();
		}
	}

	@Override
	protected void onPause() throws ServiceException {

	}

	@Override
	protected void onResume() throws ServiceException {

	}

	private void initHttpServer(ServerConfig appConfig) {
		logger.info("初始化HTTP服务开始");
		GameHttpProcessorImpl gameHttpProcessorImpl = GameServerContext.getBean(GameHttpProcessorImpl.class);
		RpcBrokerService.getInstance().initialize(appConfig, gameHttpProcessorImpl);
		// Configure the server.
		ServerBootstrap bootstrap = new ServerBootstrap();
		bootstrap.group(new NioEventLoopGroup(), new NioEventLoopGroup(2));
		bootstrap.channel(NioServerSocketChannel.class);
		// Set up the event pipeline factory.
		bootstrap.childHandler(new HttpServerPipelineFactory());
		// Enable TCP_NODELAY to handle pipelined requests without latency.
		bootstrap.childOption(ChannelOption.TCP_NODELAY, true);
		bootstrap.childOption(ChannelOption.SO_KEEPALIVE, false);
		bootstrap.option(ChannelOption.SO_REUSEADDR, true);
		// Bind and start to accept incoming connections.
		try {
			logger.info("game http server start on {}", appConfig.getWebPort());
			bootstrap.bind(new InetSocketAddress(appConfig.getWebPort())).sync();
			logger.info("初始化HTTP服务完毕");
		} catch (Exception e) {
			logger.info("初始化HTTP服务失败");
			throw new ServiceException(e);
		}
	}

	private void initGameserver(ServerConfig appConfig) {
		logger.info("初始化TCP服务开始");
		// Configure the server.
		ServerBootstrap bootstrap = new ServerBootstrap();
		bootstrap.group(bossGroup, workerGroup);
		bootstrap.channel(NioServerSocketChannel.class);
		bootstrap.option(ChannelOption.TCP_NODELAY, true);
		bootstrap.option(ChannelOption.SO_REUSEADDR, true);
		bootstrap.option(ChannelOption.SO_RCVBUF, 43690); // 43690 为默认值
		bootstrap.option(ChannelOption.SO_SNDBUF, 32768);// 32k

		bootstrap.childOption(ChannelOption.TCP_NODELAY, true);
		bootstrap.childOption(ChannelOption.SO_RCVBUF, 43690); // 43690 为默认值
		bootstrap.childOption(ChannelOption.SO_SNDBUF, 32768);// 32k

		// 把boss线程给池化
		// bootstrap.option(ChannelOption.ALLOCATOR,
		// PooledByteBufAllocator.DEFAULT);
		// 把worker线程给池化
		bootstrap.childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
		// Set up the pipeline factory.

		if (appConfig.isDebug()) {
			final TrafficShapingHandler handler = new TrafficShapingHandler(GlobalEventExecutor.INSTANCE, "TCP");
			GlobalEventExecutor.INSTANCE.scheduleAtFixedRate(new Runnable() {
				@Override
				public void run() {
					logger.info(handler.trafficCounter().toString());
				}
			}, 30, 60, TimeUnit.SECONDS);
			bootstrap.childHandler(new GameServerChannelPipelineFactory(handler));
		} else {
			bootstrap.childHandler(new GameServerChannelPipelineFactory());
		}

		// Bind and start to accept incoming connections.
		try {
			bootstrap.bind(new InetSocketAddress(appConfig.getTcpPort())).sync();
			logger.info("game tcp server start on {}", appConfig.getTcpPort());
			logger.info("初始化TCP服务完毕");
		} catch (Exception e) {
			logger.info("初始化TCP服务失败");
			throw new ServiceException(e);
		}
	}

}
