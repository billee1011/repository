package com.lingyu.common.config;

import java.io.File;
import java.util.List;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import com.lingyu.common.constant.SystemConstant;
import com.lingyu.common.core.ServiceException;
import com.lingyu.common.entity.Cache;
import com.lingyu.common.entity.Server;
import com.lingyu.common.util.IPUtil;
import com.lingyu.common.util.XMLUtil;

public class ServerConfigManager {
    private static final Logger logger = LogManager.getLogger(ServerConfigManager.class);
    // private static final String DEFAULT_CONFIG_NAME = "config.xml";
    private ServerConfig config = new ServerConfig();
    private Element root;

    public void load(int type, String fileName) throws ServiceException {
        String path = ServerConfigManager.class.getClassLoader().getResource("config/" + fileName).getPath();
        File file = new File(path);
        if (!file.exists()) {
            throw new ServiceException("config file not found");
        }
        try {
            load(type, file);
        } catch (Exception e) {
            throw new ServiceException("加载配置文件失败: " + file.getAbsolutePath(), e);
        }
    }

    public void load(int type, File file) throws ServiceException {
        logger.info("下载服务器配置文件开始: {}", file.getAbsolutePath());
        SAXReader reader = new SAXReader();
        Document doc;
        try {
            doc = reader.read(file);
            root = doc.getRootElement();
            config.setRoot(root);
            config.setContent(root.asXML());
        } catch (DocumentException e) {
            throw new ServiceException("failed to load config file", e);
        }
        if (type == SystemConstant.SERVER_TYPE_GAME) {
            this.parse4Game(type);
        }
        logger.info("下载服务器配置文件完成: {}", file.getAbsolutePath());
    }

    @SuppressWarnings("unchecked")
    private void parse4Game(int type) {
        // root属性
        // int id = XMLUtil.attributeValueInt(root, "id");
        // config.setId(id);
        // String serverName = XMLUtil.attributeValueString(root, "name");
        // config.setName(serverName);
        String platformId = XMLUtil.attributeValueString(root, "platformId");
        config.setPlatformId(platformId);
        int heartBeat = XMLUtil.attributeValueInt(root, "heartBeat");
        config.setHeartBeat(heartBeat);

        int maxConcurrentUser = XMLUtil.attributeValueInt(root, "maxConcurrentUser");
        config.setMaxConcurrentUser(maxConcurrentUser);
        int maxRegisterUser = XMLUtil.attributeValueInt(root, "maxRegisterUser");
        config.setMaxRegisterUser(maxRegisterUser);
        boolean compress = XMLUtil.attributeValueBoolean(root, "compress");
        config.setCompress(compress);
        int compressThreshold = XMLUtil.attributeValueInt(root, "compressThreshold");
        config.setCompressThreshold(compressThreshold);
        boolean crypto = XMLUtil.attributeValueBoolean(root, "crypto");
        config.setCrypto(crypto);
        boolean acceleratorValidate = XMLUtil.attributeValueBoolean(root, "enableAcceleratorValidate");
        config.setAcceleratorValidate(acceleratorValidate);
        int accIntervalUplimit = XMLUtil.attributeValueInt(root, "accIntervalUplimit");
        config.setAccIntervalUplimit(accIntervalUplimit);
        {
            Element server = XMLUtil.subElement(root, "server");
            // int type = XMLUtil.attributeValueInt(server, "type");
            // config.setType(type);
            int tcpPort = XMLUtil.attributeValueInt(server, "tcpPort");
            config.setTcpPort(tcpPort);
            int webPort = XMLUtil.attributeValueInt(server, "webPort");
            config.setWebPort(webPort);
            int innerPort = XMLUtil.attributeValueInt(server, "innerPort");
            config.setInnerPort(innerPort);
            boolean tgwMode = XMLUtil.attributeValueBoolean(server, "tgw");
            config.setTgwMode(tgwMode);
        }
        // server
        {
            String innerIp = IPUtil.getIP();
            List<Element> list = XMLUtil.subElement(root, "identity").elements("entry");
            int size = list.size();

            for (int i = 0; i < size; i++) {
                Element element = list.get(i);
                // <entry leader="true" wordId="9" wordName="G9" id="35"
                // name="游戏一区" externalIp="s35.app1101340201.qqopenapp.com"/>

                boolean leader = XMLUtil.attributeValueBoolean(element, "leader");
                if (i == 0) {
                    if (!leader) {
                        throw new ServiceException("首行配置 leader必须为true element=" + element.asXML());
                    }
                } else {
                    if (leader) {
                        throw new ServiceException("非首行配置 leader必须为false element=" + element.asXML());
                    }
                }
                int worldId = XMLUtil.attributeValueInt(element, "worldId");
                String worldName = XMLUtil.attributeValueString(element, "worldName");
                int id = XMLUtil.attributeValueInt(element, "id");
                String name = XMLUtil.attributeValueString(element, "name");
                String externalIp = XMLUtil.attributeValueString(element, "externalIp");
                Server server = new Server();
                server.setId(id);
                server.setExternalIp(externalIp);
                server.setInnerPort(config.getInnerPort());
                server.setInnerIp(innerIp);
                server.setName(name);
                server.setTcpPort(config.getTcpPort());
                server.setWebPort(config.getWebPort());
                server.setWorldId(worldId);
                server.setWorldName(worldName);
                server.setType(type);
                server.setPid(config.getPlatformId());
                config.add(leader, server);
                if (leader) {
                    config.setExternalIp(server.getExternalIp());
                }
            }

        }
        {
            Element game = XMLUtil.subElement(root, "game");
            String local = XMLUtil.attributeValueString(game, "local");
            config.setLocal(local);
            boolean debug = XMLUtil.attributeValueBoolean(game, "debug");
            config.setDebug(debug);
            int saveInterval = XMLUtil.attributeValueInt(game, "saveInterval");
            config.setSaveInterval(saveInterval);
            int offlineInterval = XMLUtil.attributeValueInt(game, "offlineInterval");
            config.setOfflineInterval(offlineInterval);
            boolean exchange = XMLUtil.attributeValueBoolean(game, "exchange");
            config.setExchange(exchange);
            float exchangeRate = XMLUtil.attributeValueInt(game, "exchangeRate");
            config.setExchangeRate(exchangeRate);
            String language = XMLUtil.attributeValueString(game, "language");
            config.setLanguage(language);
            // String version = XMLUtil.attributeValueString(game, "version");
            // config.setVersion(version);
            // boolean enableLuaJC = XMLUtil.attributeValueBoolean(game,
            // "enableLuaJC");
            // config.setEnableLuaJC(enableLuaJC);
            boolean antiAddiction = XMLUtil.attributeValueBoolean(game, "antiAddiction");
            config.setAntiAddiction(antiAddiction);
            boolean translate = XMLUtil.attributeValueBoolean(game, "translate");
            config.setTranslate(translate);
            boolean activate = XMLUtil.attributeValueBoolean(game, "activate");
            config.setActivate(activate);
            boolean mokylinLog = XMLUtil.attributeValueBoolean(game, "mokylinLog");
            config.setMokylinLog(mokylinLog);
            boolean location = XMLUtil.attributeValueBoolean(game, "location");
            config.setLocation(location);
        }
        List<Element> db = XMLUtil.subElement(root, "db").elements("property");
        Properties dbConfig = new Properties();
        for (Element each : db) {
            String name = XMLUtil.attributeValueString(each, "name");
            String value = XMLUtil.attributeValueString(each, "value");
            dbConfig.put(name, value);
        }
        config.setDbConfig(dbConfig);
        // redis
        {
            List<Element> list = XMLUtil.subElement(root, "cache").elements("entry");
            for (Element cache : list) {
                int redisType = XMLUtil.attributeValueInt(cache, "type");
                String ip = XMLUtil.attributeValueString(cache, "ip");
                int port = XMLUtil.attributeValueInt(cache, "port");
                int index = XMLUtil.attributeValueInt(cache, "index");
                config.add(new Cache(redisType, ip, port, index));
            }

        }
        // RPC
        {
            Element rpc = XMLUtil.subElement(root, "rpc");
            int rpcTimeout = XMLUtil.attributeValueInt(rpc, "timeout");
            config.setRpcTimeout(rpcTimeout);
        }
        // backURL
        {
            Element rpc = XMLUtil.subElement(root, "backserver");
            String backUrl = XMLUtil.attributeValueString(rpc, "url");
            config.setBackUrl(backUrl);
        }
        // playbacklocal 回放文件存放地址
        {
            Element playback = XMLUtil.subElement(root, "playback");
            String playbacklocal = XMLUtil.attributeValueString(playback, "playbacklocal");
            config.setPlaybacklocal(playbacklocal);
            createPlayBackLocalDir();
        }
        // 头像下载地址
        {
            Element downImg = XMLUtil.subElement(root, "downImg");
            String localurl = XMLUtil.attributeValueString(downImg, "localurl");
            config.setImgLocal(localurl);
        }
    }

    /**
     * 如果没有回放跟目录。就创建
     */
    private void createPlayBackLocalDir() {
        File dirFile = new File(config.getPlaybacklocal());
        if (!dirFile.exists()) {
            dirFile.mkdirs();
        }
    }

    public ServerConfig getServerConfig() {
        return config;
    }

    // public String getContent() {
    // return root.asXML();
    // }

    public Element getRootElement() {
        return root;
    }
}
