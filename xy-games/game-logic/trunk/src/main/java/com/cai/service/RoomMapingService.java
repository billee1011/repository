/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.service;

import java.util.Map;
import java.util.Set;
import java.util.SortedMap;

import com.cai.common.domain.Event;
import com.cai.common.util.LoadPackageClasses;
import com.cai.common.util.XYGameException;
import com.cai.core.MonitorEvent;
import com.cai.domain.Session;
import com.cai.game.AbstractGameTypeTable;
import com.cai.game.AbstractRoom;
import com.cai.util.IRoom;
import com.google.common.collect.Maps;

/**
 * @author wu_hc date: 2018年4月17日 下午7:53:10 <br/>
 */
public class RoomMapingService extends AbstractService {

	private final static RoomMapingService instance = new RoomMapingService();

	protected final Map<Integer, Class<? extends AbstractRoom>> map = Maps.newHashMap();

	private RoomMapingService() {
	}

	public static RoomMapingService getInstance() {
		return instance;
	}

	@Override
	protected void startService() {
		try {
			LoadPackageClasses loader = new LoadPackageClasses(new String[] { "com.cai.game" }, IRoom.class);
			try {

				Set<Class<?>> handlerClassz = loader.getClassSet();
				for (final Class<?> cls : handlerClassz) {
					IRoom cmdAnnotation = cls.getAnnotation(IRoom.class);
					if (null == cmdAnnotation)
						throw new RuntimeException(String.format("解析处理器[%s]出错，请检查注解是否正确!!", cls.getName()));

					AbstractGameTypeTable obj = (AbstractGameTypeTable) cls.newInstance();
					obj.doMaping();
					for (Map.Entry<Integer, Set<Class<? extends AbstractRoom>>> entry : obj.getMaping().entrySet()) {
						entry.getValue().forEach((tableClz) -> {
							Object o = map.put(entry.getKey(), tableClz);
							if (null != o) {
								throw new XYGameException(String.format("重复定义id[%d] ,[%s]  [%s] ：", entry.getKey(), tableClz, o));
							}
						});
					}
				}
			} catch (Exception e) {
				logger.error("解析处理器出错!", e);
				e.printStackTrace();
				System.exit(0);
			}

			logger.info("========LOGIC ROOM MAPPING 注册完成========");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public MonitorEvent montior() {
		return null;
	}

	@Override
	public void onEvent(Event<SortedMap<String, String>> event) {

	}

	@Override
	public void sessionCreate(Session session) {

	}

	@Override
	public void sessionFree(Session session) {

	}

	@Override
	public void dbUpdate(int _userID) {
	}

	/**
	 * @param gameTypeIndex
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public final <T extends AbstractRoom> T createRoom(int gameTypeIndex) {
		Class<? extends AbstractRoom> classz = map.get(gameTypeIndex);
		if (null != classz) {
			try {
				return (T) classz.newInstance();
			} catch (InstantiationException | IllegalAccessException e) {
				e.printStackTrace();
			}
		}
		return null;
	}
}
