package com.lingyu.common.resource;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.lingyu.common.core.ServiceException;
import com.lingyu.common.manager.GameThreadFactory;
import com.lingyu.common.template.Attribute;
import com.lingyu.common.template.CSVAnalysis;
import com.lingyu.common.util.FileUtil;

public class ResourceManager implements IResourceManager {
    private static final Logger logger = LogManager.getLogger(ResourceManager.class);
    private ExecutorService pool = Executors.newCachedThreadPool(new GameThreadFactory("async-reload"));
    private Map<String, IResourceLoader> loaders = new HashMap<String, IResourceLoader>();
    private SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private String gameDataDir;

    public ResourceManager() {

    }

    public ResourceManager(String path) {
        this.gameDataDir = path;
    }

    /***/
    public String init(String path) throws ServiceException {
        String version = "";
        logger.info("初始化 GameData:{}", path);// config.getLocal()
        gameDataDir = path;
        this.reloadAll();
        String versionFile = "version.properties";
        File file = new File(gameDataDir + File.separator + versionFile);
        if (file != null) {
            try {
                version = FileUtils.readFileToString(file);
            } catch (IOException e) {
                logger.warn("策划版本文件不存在，不处理 file={}", versionFile);
            }
        }
        return version;
    }

    @Override
    public void register(IResourceLoader loader) {
        loaders.put(loader.getResName(), loader);
    }

    /** 用户后台异步加载 */
    public void asyncReloadAll() {
        pool.execute(new Runnable() {
            @Override
            public void run() {
                reloadAll();
            }
        });
    }

    @Override
    public void reloadAll() throws ServiceException {
        if (MapUtils.isNotEmpty(loaders)) {
            logger.info("策划模板数据开始加载...");
            logger.info("模板数量为 {}", loaders.size());
            Collection<IResourceLoader> list = loaders.values();
            for (IResourceLoader loader : list) {
                logger.info("加载 {} 数据开始", loader.getResName());
                loader.load();
                logger.info("加载 {} 数据完成", loader.getResName());
            }
            logger.info("策划模板数据加载完成.");
            logger.info("策划数据合法性校验开始");
            for (IResourceLoader loader : list) {
                loader.checkValid();
            }
            logger.info("策划数据合法性校验完毕");
        }

    }

    @Override
    public void reload(String name) throws ServiceException {
        IResourceLoader loader = loaders.get(name);
        if (null != loader) {
            logger.info("加载 {} 数据开始", loader.getResName());
            loader.load();
            logger.info("加载 {} 数据完成", loader.getResName());
            logger.info("策划数据合法性校验开始");
            loader.checkValid();
            logger.info("策划数据合法性校验完毕");
        } else {
            logger.warn("{} 不存在.", name);
        }
    }

    public <T> T getValueFromMap(Map<String, T> map, Class<T> clazz, String id) {
        if (StringUtils.isEmpty(id) || StringUtils.equals(id, "0")) {
            return null;
        }
        T result = map.get(id);
        if (result == null) {
            logger.error("no {}: {}", clazz.getSimpleName(), id);
        }
        return result;
    }

    public <T> T getValueFromMap(Map<Integer, T> map, Class<T> clazz, int id) {
        // if (id == 0) { //FIXME @龙哥 有的地方会有0为key的情况
        // logger.error("no {}: {}", clazz.getSimpleName(), id);
        // return null;
        // }
        T result = map.get(id);
        if (result == null) {
            logger.error("no {}: {}", clazz.getSimpleName(), id);
        }
        return result;
    }

    public <T> List<T> loadTemplate(Class<T> clazz) throws ServiceException {
        CSVAnalysis cSVAnalysis = clazz.getSuperclass().getAnnotation(CSVAnalysis.class);
        if (cSVAnalysis == null) {
            throw new ServiceException("loadTemplate class " + clazz.getSimpleName() + " is not existed.");
        }
        Object[] array = null;
        try {
            array = FileUtil.parseCSV(this.gameDataDir + "/" + cSVAnalysis.res());

        } catch (Exception ex) {
            throw new ServiceException(ex);
        }
        List<T> ret = new ArrayList<T>();
        for (Object e : array) {

            try {
                ret.add(this.make(clazz, e));
            } catch (Exception ex) {
                throw new ServiceException("有问题的策划数据:" + e.toString(), ex);
            }
        }
        return ret;
    }

    /*
     * File file = new File("e:\\test.csv"); // FileReader fReader = new
     * FileReader(file); BufferedReader fReader = new BufferedReader(new
     * InputStreamReader(new FileInputStream(file),"utf-8")); CSVReader
     * csvReader = new CSVReader(fReader); String[] strs = csvReader.readNext();
     * if(strs != null && strs.length > 0){ for(String str : strs) if(null !=
     * str && !str.equals("")) System.out.print(str + " , "); 1
     * System.out.println("\n---------------"); } List<String[]> list =
     * csvReader.readAll(); for(String[] ss : list){ for(String s : ss) if(null
     * != s && !s.equals("")) System.out.print(s + " , "); System.out.println();
     * } csvReader.close(); }
     */

    @SuppressWarnings("unchecked")
    private <T> T make(Class<T> clazz, Object obj)
                    throws InstantiationException, IllegalAccessException, IllegalArgumentException, ParseException {
        Map<String, Object> map = (Map<String, Object>) obj;
        T template = clazz.newInstance();
        Field[] fields = clazz.getSuperclass().getDeclaredFields();
        for (Field f : fields) {
            // 没有配置的属性直接跳过
            Attribute a = f.getAnnotation(Attribute.class);
            if (a == null)
                continue;
            String value = String.valueOf(map.get(a.value()));
            if (value == null) {
                throw new IllegalArgumentException("amf3对应字段找不到 :" + a.value());
            }
            if ("null".equals(value) || "".equals(value))
                continue;
            f.setAccessible(true);
            try {
                if ("int".equals(f.getType().getSimpleName())) {
                    f.set(template, new Double(value).intValue());
                    // f.set(template, Integer.parseInt(value));
                    // 不这样写的原因是：有可能导出的数据时科学级数法格式
                } else if ("long".equals(f.getType().getSimpleName())) {
                    f.setLong(template, new Double(value).longValue());
                } else if ("float".equals(f.getType().getSimpleName())) {
                    f.set(template, Float.parseFloat(value));
                } else if ("double".equals(f.getType().getSimpleName())) {
                    f.set(template, Double.parseDouble(value));
                } else if ("boolean".equals(f.getType().getSimpleName())) {
                    f.setBoolean(template, BooleanUtils.toBoolean(Integer.parseInt(value)));
                } else if ("Date".equals(f.getType().getSimpleName())) {
                    f.set(template, format.parse(value));
                } else {
                    // CSV会把有特殊字符的字符串加上双引号
                    if (StringUtils.startsWith(value, "\"") && StringUtils.endsWith(value, "\"")) {
                        value = StringUtils.substring(value, 1, value.length() - 1);
                    }
                    value = value.trim();
                    // if(value.indexOf(" ")>=0){
                    // throw new ServiceException("字符串出现了空格
                    // ，需要逗逼策划修正."+f.getName() + ":" + value);
                    // }
                    f.set(template, value);
                }
            } catch (Exception e) {
                throw new ServiceException(f.getName() + ":" + value, e);
            }
            f.setAccessible(false);
        }
        return template;
    }

    public String getGameDataDir() {
        return gameDataDir;
    }

    public void setGameDataDir(String gameDataDir) {
        this.gameDataDir = gameDataDir;
    }
}
