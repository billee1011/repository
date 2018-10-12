package com.lingyu.admin;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Properties;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;
import org.springframework.scheduling.config.ContextLifecycleScheduledTaskRegistrar;

import com.lingyu.admin.config.JDBCConfigManager;
import com.lingyu.admin.config.TCPConfig;
import com.lingyu.admin.manager.AnnounceManager;
import com.lingyu.admin.manager.GameAreaManager;
import com.lingyu.admin.manager.PlatformManager;
import com.lingyu.admin.manager.UserManager;
import com.lingyu.admin.network.AsyncHttpClient;
import com.lingyu.admin.network.GameClientManager;
import com.lingyu.common.core.ServiceException;
import com.lingyu.common.manager.MemoryManager;
import com.lingyu.common.resource.ResourceManager;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

public class AdminServer implements ServletContextListener {
	private static final Logger logger = LogManager.getLogger(AdminServer.class);
	private EventLoopGroup bossGroup = new NioEventLoopGroup();
	private EventLoopGroup workerGroup = new NioEventLoopGroup();

	@Override
	public void contextInitialized(ServletContextEvent event) {
		long startTime = System.currentTimeMillis();
		logger.info("starting admin server service");
		logger.info("初始化Spring系统开始");
		// 获取jdbc配置文件
		String jdbcConfigFile = JDBCConfigManager.getConfigFile();
		// 初始化spring
		ApplicationContext context = new FileSystemXmlApplicationContext(new String[] { "classpath:spring-config.xml", "classpath:config/spring-task.xml",
				jdbcConfigFile });
		AdminServerContext.setApplicationContext(context);
		AdminServerContext.setServletContext(event.getServletContext());
		logger.info("初始化Spring系统完毕");
		
		PlatformManager platformManager = AdminServerContext.getBean(PlatformManager.class);
		platformManager.init();
		
		UserManager userManager = AdminServerContext.getBean(UserManager.class);
		userManager.init();
		
		GameAreaManager gameAreaManager = AdminServerContext.getBean(GameAreaManager.class);
		gameAreaManager.init();
		
		AnnounceManager announceManager = AdminServerContext.getBean(AnnounceManager.class);
		announceManager.init();

		// 初始化模板管理系统并加载模板
		ResourceManager resourceManager = AdminServerContext.getBean(ResourceManager.class);
		resourceManager.reloadAll();

		AsyncHttpClient.getInstance().init();
		
		// TCP
		TCPConfig tcpConfig = AdminServerContext.getBean(TCPConfig.class);
		this.initTcpserver(tcpConfig.getPort());
		
		//version
		String version=this.getVersion(event);
		AdminServerContext.setVersion(version);
		
		// 启动内存监控
		MemoryManager.getInstance().initialize();
		// logger.info("容器注入的对象列表  name={}",context.getBeanDefinitionNames());
		logger.info("admin server is running, delta={} ms", (System.currentTimeMillis() - startTime));
		System.out.println("admin server is running,delta=" + (System.currentTimeMillis() - startTime) + " ms");
	}
	private String getVersion(ServletContextEvent event){
		String version = this.getClass().getPackage().getImplementationVersion();
		if (version==null) {
		    Properties prop = new Properties();
		    try {
		        prop.load(event.getServletContext().getResourceAsStream("/META-INF/MANIFEST.MF"));
		        version = prop.getProperty("Implementation-Version");
		    } catch (IOException e) {
		        logger.error(e.getMessage(),e);
		    }
		}
		if(version==null){
			version="no version";
		}
		return version;
	}

	private void initTcpserver(int port) {
		logger.info("初始化TCP服务开始");
		// Configure the server.
		ServerBootstrap bootstrap = new ServerBootstrap();
		bootstrap.group(bossGroup, workerGroup);
		bootstrap.channel(NioServerSocketChannel.class);
		bootstrap.childOption(ChannelOption.TCP_NODELAY, true);
		bootstrap.option(ChannelOption.SO_REUSEADDR, true);
		// Set up the pipeline factory.
		bootstrap.childHandler(new AdminServerChannelPipelineFactory());
		// Bind and start to accept incoming connections.
		try {
			bootstrap.bind(new InetSocketAddress(port)).sync();
			logger.info("admin tcp server start on {}", port);
			logger.info("初始化TCP服务完毕");
		} catch (Exception e) {
			logger.info("初始化TCP服务失败");
			throw new ServiceException(e);
		}
	}

	@Override
	public void contextDestroyed(ServletContextEvent event) {
		logger.warn("stopping Admin server start");
	//	ChatMonitorManager chatMonitorManager = AdminServerContext.getBean(ChatMonitorManager.class);
		//chatMonitorManager.closeAllMonitors();
		ContextLifecycleScheduledTaskRegistrar executor = AdminServerContext.getBean(ContextLifecycleScheduledTaskRegistrar.class);
		executor.destroy();
		GameClientManager gameClientManager = AdminServerContext.getBean(GameClientManager.class);
		gameClientManager.destroyAllClient();
		// 优雅退出，释放线程池资源
		logger.warn("TCP 服务端停止开始");
		bossGroup.shutdownGracefully();
		workerGroup.shutdownGracefully();
		logger.warn("TCP 服务端停止完毕");
		logger.warn("stopping Admin server end");
	}
}