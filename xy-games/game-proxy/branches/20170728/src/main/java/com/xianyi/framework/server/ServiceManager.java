package com.xianyi.framework.server;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import org.apache.commons.lang.math.NumberUtils;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import com.cai.common.domain.Event;
import com.cai.common.util.WRSystem;
import com.cai.core.MonitorEvent;
import com.cai.domain.Session;
import com.google.common.collect.Lists;
import javolution.util.FastMap;

/**
 * 
 * 服务管理
 */
public final class ServiceManager {

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
	 * 获取指定名称的ResultMessage的ID 该ID是填写在Service的配置文件中
	 * 
	 * @param _name
	 *            具体message的类名
	 * @return
	 */
	public int getRMessageId(String _name) throws Exception {
		Integer i = msges.get(_name);
		if (i == null) {
			throw new Exception("Can`t find ResultMessage by name " + _name);
		}
		return i;
	}

	/**
	 * 加载service目录中的xml配置文件
	 * 
	 * @throws Exception
	 */
	public void load() throws Exception {
		allServices.clear();
		List<AbstractService> serviceList = Lists.newArrayList();
		try {
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
				allServices.put(order, service);
				serviceList.add(service);
			}
		} catch (Exception e1) {
			e1.printStackTrace();
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

	/**
	 * 执行事件处理
	 * 
	 * @param event
	 *            事件对象
	 * @throws Exception
	 */
	public void onEvent(Event<SortedMap<String, String>> event) throws Exception {
	}

	public List<MonitorEvent> monitor() {
		return null;
	}

	/**
	 * 通知所有服务增加了一个sid
	 * 
	 * @param _userID
	 */
	public void sessionCreate(Session session) {
		if (null != session) {
			if (ssnSequence == null) {
				System.out.println("ssnSequence is not initialized");
				return;
			}
			for (int i = 0; i < ssnSequence.length; i++) {
				ssnSequence[i].sessionCreate(session);
			}
		}
	}

	/**
	 * 通知所有服务
	 * 
	 * @param _userID
	 */
	public void sessionFree(Session session) {
		if (ssnSequence == null) {
			System.out.println("ssnSequence is not initialized");

			return;
		}

		if (null != session) {
			for (int i = ssnSequence.length - 1; i >= 0; i--) {
				try {
					ssnSequence[i].sessionFree(session);
				} catch (Exception e) {
					e.printStackTrace();
					System.out.println("服务释放会话出错：" + ssnSequence[i].getName());
				}
			}
		} else {
			System.out.println("释放了空的会话");
		}
	}

	/**
	 * 更新数据库
	 * 
	 * @param sid
	 */
	public void dbUpdate(int _userID) {
		for (AbstractService srvc : allServices.values()) {
			srvc.dbUpdate(_userID);
		}
	}

	public AbstractService getServicesByteOrder(int order) {
		return allServices.get(order);
	}

}
