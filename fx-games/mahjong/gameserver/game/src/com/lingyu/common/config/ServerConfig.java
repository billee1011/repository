package com.lingyu.common.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.dom4j.Element;

import com.lingyu.common.constant.SystemConstant;
import com.lingyu.common.entity.Cache;
import com.lingyu.common.entity.Server;

public class ServerConfig implements IConfig {

	private String content;
	/** 游戏区worldName (全局唯一，所有平台共用 ) */
	private int worldId;
	/** worldName:游戏区名 (全局唯一,所有平台共用 ) */
	private String worldName;
	// /**
	// * 游戏区id
	// */
	// private int id;
	// /** 游戏区名 */
	// private String name;
	/** 平台联运商 */
	private String platformId;
	/**
	 * 最大同时在线
	 */
	private int maxConcurrentUser;
	/**
	 * 最大注册用户
	 */
	private int maxRegisterUser;
	/**
	 * 网关的心跳时间(0取消心跳检测)
	 */
	private int heartBeat;
	/**
	 * 网络数据包压缩
	 */
	private boolean compress;
	/**
	 * 网络数据包压缩阀值
	 */
	private int compressThreshold;
	/** 是否允许处理加速器 */
	private boolean acceleratorValidate;
	/** 加速检测的间隔上限，到达这个上限就弹框警告，毫秒单位 */
	private int accIntervalUplimit;
	/**
	 * 网络数据报加密
	 */
	private boolean crypto;

	/** 服务器类型 */
	private int type;
	private int subType;
	private int tcpPort;
	private int webPort;
	private int innerPort;
	/** 外网IP */
	private String externalIp;
	// /** 远程下载地址 */
	// private String remote;
	/** 本地策划数据存放地址 */
	private String local;
	private boolean debug;
	private int saveInterval;
	private int offlineInterval;
	private boolean exchange;
	/** 语言 */
	private String language;
	/** 兑换率 */
	private float exchangeRate;
	// /** 启动LUAJC模式 */
	// private boolean enableLuaJC;
	/** 版本 */
	private String serverVersion = "";
	/** 策划数据版本 */
	private String dataVersion = "";
	/**
	 * rpc client: 超时
	 */
	private int rpcTimeout;
	private boolean antiAddiction;
	/** 是否支持翻译 */
	private boolean translate;
	private boolean tgwMode;

	/** 是否需要账号激活 */
	private boolean activate;
	/** 墨麟的日志 */
	private boolean mokylinLog;
	/***/
	private boolean location;
	private Element root;
	
	/** 后台的url*/
	private String backUrl;
	/** 回放文件存放地址*/
	private String playbacklocal;
	/** 头像图片存放地址*/
	private String imgLocal;
	// /**
	// * redis的IP
	// */
	// private String cacheIp;
	// /**
	// * redis的Port
	// */
	// private int cachePort;
	// /**
	// * redis的db index
	// */
	// private int cacheIndex;
	private List<Cache> cacheList = new ArrayList<>();
	private Map<Integer, Server> serverStore = new HashMap<>();
	private Map<Integer, Server> worldStore = new HashMap<>();
	// private List<Server> serverList = new ArrayList<>();
	private Properties dbConfig = new Properties();

	public void add(boolean leader, Server server) {
		if (leader) {
			worldId = server.getWorldId();
			worldName = server.getWorldName();
			// id = server.getId();
			// name = server.getName();
		} else {
			server.setFollowerId(worldId);
		}
		serverStore.put(server.getId(), server);
		worldStore.put(server.getWorldId(), server);
	}

	public Collection<Server> getServerList() {
		return serverStore.values();
	}

	public Server getServer(int areaId) {
		return serverStore.get(areaId);
	}

	/**
	 * 获取当前服务器Id
	 */
	public int getServerId() {
		return worldId;
	}

	/** 用平台 server ID来判断是否在本区 */
	public boolean isLocal4Area(int areaId) {
		return serverStore.containsKey(areaId);
	}

	/** 用世界ID来判断是否在本进程 */
	public boolean isLocal4World(int worldId) {
		return worldStore.containsKey(worldId);
	}


	public Server getLeaderServer() {
		return worldStore.get(worldId);
	}

	public void add(Cache cache) {
		cacheList.add(cache);
	}

	public List<Cache> getCacheList() {
		return cacheList;
	}

	public String getExternalIp() {
		return externalIp;
	}

	public void setExternalIp(String externalIp) {
		this.externalIp = externalIp;
	}

	public String getDataVersion() {
		return dataVersion;
	}

	public void setDataVersion(String dataVersion) {
		this.dataVersion = dataVersion;
	}

	public boolean isTgwMode() {
		return tgwMode;
	}

	public void setTgwMode(boolean tgwMode) {
		this.tgwMode = tgwMode;
	}

	public boolean isAntiAddiction() {
		return antiAddiction;
	}

	public void setAntiAddiction(boolean antiAddiction) {
		this.antiAddiction = antiAddiction;
	}

	public void setWorldId(int worldId) {
		this.worldId = worldId;
	}

	@Override
	public int getWorldId() {
		return worldId;
	}

	public String getWorldName() {
		return worldName;
	}

	public void setWorldName(String worldName) {
		this.worldName = worldName;
	}

	public void setSubType(int subType) {
		this.subType = subType;
	}

	public boolean isMokylinLog() {
		return mokylinLog;
	}

	public void setMokylinLog(boolean mokylinLog) {
		this.mokylinLog = mokylinLog;
	}

	@Override
	public int getSubType() {
		return subType;
	}

	// public int getId() {
	// return id;
	// }
	//
	// public void setId(int id) {
	// this.id = id;
	// }

	public String getServerVersion() {
		return serverVersion;
	}

	public void setServerVersion(String serverVersion) {
		if (serverVersion != null) {
			this.serverVersion = serverVersion;
		}

	}

	//
	// public boolean isEnableLuaJC() {
	// return enableLuaJC;
	// }
	//
	// public void setEnableLuaJC(boolean enableLuaJC) {
	// this.enableLuaJC = enableLuaJC;
	// }

	public boolean isLocation() {
		return location;
	}

	public void setLocation(boolean location) {
		this.location = location;
	}

	public String getLanguage() {
		return language;
	}

	public void setLanguage(String language) {
		this.language = language;
	}

	public int getInnerPort() {
		return innerPort;
	}

	public void setInnerPort(int innerPort) {
		this.innerPort = innerPort;
	}

	public float getExchangeRate() {
		return exchangeRate;
	}

	// public int getCacheIndex() {
	// return cacheIndex;
	// }
	//
	// public void setCacheIndex(int cacheIndex) {
	// this.cacheIndex = cacheIndex;
	// }

	public void setExchangeRate(float exchangeRate) {
		this.exchangeRate = exchangeRate;
	}

	public int getMaxConcurrentUser() {
		return maxConcurrentUser;
	}

	public void setMaxConcurrentUser(int maxConcurrentUser) {
		this.maxConcurrentUser = maxConcurrentUser;
	}

	public int getRpcTimeout() {
		return rpcTimeout;
	}

	public void setRpcTimeout(int rpcTimeout) {
		this.rpcTimeout = rpcTimeout;
	}
	
	public String getBackUrl() {
		return backUrl;
	}

	public void setBackUrl(String backUrl) {
		this.backUrl = backUrl;
	}
	
	public String getPlaybacklocal() {
		return playbacklocal;
	}

	public void setPlaybacklocal(String playbacklocal) {
		this.playbacklocal = playbacklocal;
	}
	
	public String getImgLocal() {
		return imgLocal;
	}

	public void setImgLocal(String imgLocal) {
		this.imgLocal = imgLocal;
	}

	public int getMaxRegisterUser() {
		return maxRegisterUser;
	}

	public void setMaxRegisterUser(int maxRegisterUser) {
		this.maxRegisterUser = maxRegisterUser;
	}

	public int getHeartBeat() {
		return heartBeat;
	}

	public void setHeartBeat(int heartBeat) {
		this.heartBeat = heartBeat;
	}

	public boolean isCompress() {
		return compress;
	}

	public void setCompress(boolean compress) {
		this.compress = compress;
	}

	public int getCompressThreshold() {
		return compressThreshold;
	}

	public void setCompressThreshold(int compressThreshold) {
		this.compressThreshold = compressThreshold;
	}

	public boolean isAcceleratorValidate() {
		return acceleratorValidate;
	}

	public void setAcceleratorValidate(boolean acceleratorValidate) {
		this.acceleratorValidate = acceleratorValidate;
	}

	public int getAccIntervalUplimit() {
		return accIntervalUplimit;
	}

	public void setAccIntervalUplimit(int accIntervalUplimit) {
		this.accIntervalUplimit = accIntervalUplimit;
	}

	public boolean isCrypto() {
		return crypto;
	}

	public void setCrypto(boolean crypto) {
		this.crypto = crypto;
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
		SystemConstant.serverType = type;
	}

	// public String getName() {
	// return name;
	// }
	//
	// public void setName(String name) {
	// this.name = name;
	// }

	public int getTcpPort() {
		return tcpPort;
	}

	public void setTcpPort(int tcpPort) {
		this.tcpPort = tcpPort;
	}

	public int getWebPort() {
		return webPort;
	}

	public void setWebPort(int webPort) {
		this.webPort = webPort;
	}

	public String getPlatformId() {
		return platformId;
	}

	public void setPlatformId(String platformId) {
		this.platformId = platformId;
	}

	// public String getRemote() {
	// return remote;
	// }
	//
	// public String getSysRemote() {
	// return remote + "java/zhcn";
	// }
	//
	// public String getMapRemote() {
	// return remote + "zhcn/r/m/";
	// }
	//
	// public void setRemote(String remote) {
	// this.remote = remote;
	// }

	public String getLocal() {
		return local;
	}

	public String getMapLocal() {
		return local + "/map";
	}

	public void setLocal(String local) {
		this.local = local;
	}

	public boolean isDebug() {
		return debug;
	}

	public void setDebug(boolean debug) {
		this.debug = debug;
	}

	public int getSaveInterval() {
		return saveInterval;
	}

	public void setSaveInterval(int saveInterval) {
		this.saveInterval = saveInterval;
	}

	public int getOfflineInterval() {
		return offlineInterval;
	}

	public void setOfflineInterval(int offlineInterval) {
		this.offlineInterval = offlineInterval;
	}

	public boolean isExchange() {
		return exchange;
	}

	public void setExchange(boolean exchange) {
		this.exchange = exchange;
	}

	public Properties getDbConfig() {
		return dbConfig;
	}

	public void setDbConfig(Properties dbConfig) {
		this.dbConfig = dbConfig;
	}

	public boolean isTranslate() {
		return translate;
	}

	public void setTranslate(boolean translate) {
		this.translate = translate;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public Element getRoot() {
		return root;
	}

	public void setRoot(Element root) {
		this.root = root;
	}

	/**
	 * 是否需要账号激活
	 * 
	 * @return
	 */
	public boolean isActivate() {
		return activate;
	}

	public void setActivate(boolean activate) {
		this.activate = activate;
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this);
	}

	public boolean isGameServer() {
		return type == SystemConstant.SERVER_TYPE_GAME;
	}
}