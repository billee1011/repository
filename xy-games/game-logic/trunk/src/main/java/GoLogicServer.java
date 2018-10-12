import org.apache.log4j.PropertyConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import com.cai.common.define.ELogType;
import com.cai.common.define.EServerStatus;
import com.cai.common.define.EServerType;
import com.cai.common.define.ESysLogLevelType;
import com.cai.common.domain.LogicGameServerModel;
import com.cai.common.rmi.ICenterRMIServer;
import com.cai.common.util.PerformanceTimer;
import com.cai.common.util.PropertiesUtil;
import com.cai.common.util.ServerInfo;
import com.cai.common.util.ServerListenUtil;
import com.cai.common.util.ServerRegisterUtil;
import com.cai.common.util.SpringService;
import com.cai.common.util.WRSystem;
import com.cai.core.SystemConfig;
import com.cai.dictionary.CardCategoryDict;
import com.cai.dictionary.CoinDict;
import com.cai.dictionary.CoinExciteDict;
import com.cai.dictionary.GameGroupRuleDict;
import com.cai.dictionary.GoodsDict;
import com.cai.dictionary.MatchDict;
import com.cai.dictionary.RedPackageRuleDict;
import com.cai.dictionary.ServerDict;
import com.cai.dictionary.SysGameTypeDict;
import com.cai.dictionary.SysMatchBroadDict;
import com.cai.dictionary.SysParamDict;
import com.cai.dictionary.SysParamServerDict;
import com.cai.dictionary.TurntableDict;
import com.cai.game.AbstractRoom;
import com.cai.net.server.GameSocketServer;
import com.cai.service.MongoDBServiceImpl;
import com.cai.service.PlayerServiceImpl;
import com.cai.service.ServiceManager;
import com.google.common.base.Strings;

public class GoLogicServer {

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

		// 加载字典
		GoLogicServer.loadDict();

		// 开启服务类
		ServiceManager.getInstance().load();

		final LogicGameServerModel serverModel = ServerDict.getInstance().getLogicGameServerModel();
		// 中转与客户端socket
		new GameSocketServer(serverModel.getSocket_port()).start();

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

				clearRoom();
				
				MongoDBServiceImpl.getInstance().systemLog(ELogType.stopJvm, "关闭服务器", null, null, ESysLogLevelType.NONE);

				// 通知中心服
				LogicGameServerModel serverMode = ServerDict.getInstance().getLogicGameServerModel();
				if (null != serverMode && ServerRegisterUtil.doVaildServerMsg(ServerInfo.of(serverMode.getInner_ip(), serverMode.getLogic_game_id()),
						SystemConfig.logic_index)) {
					SpringService.getBean(ICenterRMIServer.class).serverStatusUpdate(EServerType.LOGIC, EServerStatus.CLOSE,
							SystemConfig.logic_index);
				} else {
					logger.error("###### 逻辑服[{}]取消注册失败.{} ######", serverMode, SystemConfig.logic_index);
				}

				
				try {

					// mongodb入库
					MongoDBServiceImpl.getInstance().getMogoDBTimer().handle();

				} catch (Exception e) {
					e.printStackTrace();
				}

				System.out.println("关闭服务器耗时:" + timerShudown.getStr());

			}
		});

	}
	
	
	public static void clearRoom() {
		logger.warn("------------系统开始清理残余房间数量--------------"+PlayerServiceImpl.getInstance().getRoomMap().size());
		for(AbstractRoom room:PlayerServiceImpl.getInstance().getRoomMap().values()) {
			if (room != null) {
				try {
//					room.getRoomLock().lock();
					if (PlayerServiceImpl.getInstance().getRoomMap().containsKey(room.getRoom_id())) {
						if (room.matchId > 0) {
							// 比赛场不清
							return;
						}

						if (Strings.isNullOrEmpty("房间已经被系统解散")) {
							room.force_account();
						} else {
							room.force_account("房间已经被系统解散");
						}

					}
				} finally {
//					room.getRoomLock().unlock();
				}

			}
		}
		logger.warn("------------系统开始清理残余房间完成------------");
	}

	public static void loadDict() {
		SysGameTypeDict.getInstance().load();
		SysParamDict.getInstance().load();// 系统参数
		GoodsDict.getInstance().load();
		ServerDict.getInstance().load();
		TurntableDict.getInstance().load();
		RedPackageRuleDict.getInstance().load();
		SysParamServerDict.getInstance().load();// 服务端系统参数
		MatchDict.getInstance().load();
		SysMatchBroadDict.getInstance().load();// 服务端系统参数
		GameGroupRuleDict.getInstance().load();// 服务端系统参数
		CoinDict.getInstance().load();
		CardCategoryDict.getInstance().load();
		CoinExciteDict.getInstance().load();
	}

}
