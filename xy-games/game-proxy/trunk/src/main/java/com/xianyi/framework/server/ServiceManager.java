package com.xianyi.framework.server;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.math.NumberUtils;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.util.WRSystem;
import com.google.common.collect.Lists;

import javolution.util.FastMap;

/**
 * 
 * 服务管理
 */
public final class ServiceManager {
	/**
	 * 日志
	 */
	private static Logger log = LoggerFactory.getLogger(ServiceManager.class);

	/**
	 */
	private final Map<Integer, AbstractService> allServices;

	/**
	 * SessionCreate时按照数组正向顺序进行通知<br>
	 * SessionFree时按照数组逆向顺序进行通知
	 */
	private AbstractService[] ssnSequence = null;

	/**
	 * 该Service所提供的ResultMessage的列表<ServerHandlerName><ID>
	 */
	protected Map<String, Integer> msges = new FastMap<String, Integer>();

	/**
	 * 惰性加载
	 */
	private static final class LazzyHolder {
		public static ServiceManager INSTANCE = new ServiceManager();
	}

	private ServiceManager() {
		allServices = new FastMap<Integer, AbstractService>();
	}

	/**
	 * 获得实例
	 * 
	 * @return
	 */
	public static ServiceManager getInstance() {
		return LazzyHolder.INSTANCE;
	}

	/**
	 * 根据上行数据获取服务实例
	 * 
	 * @param _data
	 * @return
	 */
	public AbstractService getService(int serviceID) {
		return allServices.get(serviceID);
	}

	/**
	 * 加载service目录中的xml配置文件
	 * 
	 * @throws Exception
	 */
	public void load() throws Exception {
		allServices.clear();
		List<AbstractService> serviceList = Lists.newArrayList();
		String conf = WRSystem.HOME + "../common/services.xml";
		SAXReader reader = new SAXReader();
		Document document = reader.read(new File(conf));
		Element root = document.getRootElement();
		Iterator<Element> it = root.elementIterator("service");
		while (it.hasNext()) {
			Element e = it.next();
			int order = NumberUtils.createInteger(e.attributeValue("order"));
			AbstractService service = loadService(order, e.attributeValue("class"), e.attributeValue("name"));
			service.initService();
			log.info("Services[{}] init finish!!!", service.getName());
			allServices.put(order, service);
			serviceList.add(service);
		}
		Collections.sort(serviceList);

		ssnSequence = new AbstractService[serviceList.size()];
		serviceList.toArray(ssnSequence);
	}

	/**
	 * 根据Service的名称加载class
	 * 
	 * @param _className
	 * @return
	 * @throws ClassNotFoundException
	 * @throws NoSuchMethodException
	 * @throws SecurityException
	 */
	private AbstractService loadService(int order, String _className, String serviceName)
			throws ClassNotFoundException, NoSuchMethodException, Exception {
		Class<?> c = getClass().getClassLoader().loadClass(_className);
		// abstract的子类必须是通过getInstance来获取的单态类
		Method getInstance = c.getMethod("getInstance", null);
		AbstractService impl = (AbstractService) getInstance.invoke(null, null);
		impl.setOrder(order);
		impl.setName(serviceName);

		return impl;
	}
}
