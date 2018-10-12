package com.lingyu.game.service.event;

import java.lang.reflect.Method;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Service;

import com.lingyu.common.constant.SystemConstant;
import com.lingyu.common.core.ServiceException;
import com.lingyu.common.message.BalanceBusinessExecutor;
import com.lingyu.common.message.IRunnable;

@Service
public class EventManager {
	private static final Logger logger = LogManager.getLogger(EventManager.class);
	@Autowired
	@Qualifier("busExecutor")
	private BalanceBusinessExecutor busExecutor;
	@Autowired
	@Qualifier("publicExecutor")
	private BalanceBusinessExecutor publicExecutor;

	public void init() {
		logger.info("事件分发系统初始化开始");
		ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false);
		provider.addIncludeFilter(new AnnotationTypeFilter(Event.class));
		Set<BeanDefinition> candidates = provider.findCandidateComponents("com.lingyu.game.service.event");
		for (BeanDefinition candidate : candidates) {
			try {
				String clazzName = candidate.getBeanClassName();
				Class<?> clazz = Class.forName(clazzName);
				Method method = clazz.getMethod("subscribe");
				method.invoke(clazz.newInstance());
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
				throw new ServiceException(e);
			}
		}
		logger.info("事件分发系统初始化完毕");
		// AnnotationConfigApplicationContext context=new
		// AnnotationConfigApplicationContext(new
		// AnnotationTypeFilter(Event.class));
		// context.scan("com.lingyu.game.service.event");
		// context.getBeanDefinitionCount();
		// Map map=context.getBeansWithAnnotation(Event.class);
	}

	/**handler存储*//*
	public <T> void registerHandler(HandlerWrapper wrapper,AbEvent event){
		handleStore.put(wrapper.getHandler().getClass().getSimpleName()+":"+event.getName(), wrapper);
	}
	*//**获取处理类*//*
	public <T> HandlerWrapper getHandlerWrapper(String handlerName,T event){
		return handleStore.get(handlerName+":"+event.getClass().getSimpleName());
	}*/
	
	/** 非同线程异步执行 */
	public <T> void asyncCall(long roleId, HandlerWrapper wrapper, T event) {
		// if
		byte groupName = wrapper.getGroup();
		EventRunnable runnable = new EventRunnable(roleId,wrapper, event);
		switch (groupName) {
		case SystemConstant.GROUP_BUS_CACHE: {
			busExecutor.getExecutorService(groupName, roleId).execute(runnable);
			break;
		}
//		case SystemConstant.GROUP_STAGE: {
//			long stageId = stageManager.getStageId(roleId);
//			if (stageId != 0) {
//				stageExecutor.getExecutorService(groupName, stageId).execute(runnable);
//			}
//			break;
//		}
		case SystemConstant.GROUP_PUBLIC: {
			publicExecutor.getExecutorService(groupName, wrapper.getModule()).execute(runnable);
			break;
		}
		}
	}

	/** 同线程串行执行 */
	public Object syncCall(HandlerWrapper wrapper, AbEvent event) throws ServiceException {
		return this.call(wrapper, event);
	}

	/** 同线程串行执行 */
	private <T> Object call(HandlerWrapper wrapper, T event) throws ServiceException {
		Object ret = null;
		try {
			wrapper.invoke(event);
		}catch (Exception e) {
			logger.error(event.getClass().getSimpleName() + ":" + e.getMessage(), e);
		}
//		Method method = wrapper.getMethod();
//		try {
//			if (event != null) {
//				// 有参数调用
//				ret = method.invoke(wrapper.getHandler(), event);
//			}
//		} catch (Exception e) {
//			logger.error(event.getClass().getSimpleName() + ":" + e.getMessage(), e);
//			// throw new ServiceException(e);
//		}
		return ret;
	}
//	private <T> Object call(HandlerWrapper wrapper, T event) throws ServiceException {
//		Object ret = null;
//		Method method = wrapper.getMethod();
//		try {
//			if (event != null) {
//				// 有参数调用
//				ret = method.invoke(wrapper.getHandler(), event);
//			}
//		} catch (Exception e) {
//			logger.error(event.getClass().getSimpleName() + ":" + e.getMessage(), e);
//			// throw new ServiceException(e);
//		}
//		return ret;
//	}

	private class EventRunnable implements IRunnable {
		private HandlerWrapper wrapper;
		private  Object event;
		private long roleId;

		public <T> EventRunnable(long roleId,HandlerWrapper wrapper, T event) {
			this.roleId=roleId;
			this.event = event;
			this.wrapper = wrapper;
		}

		@Override
		public void run() {
			call(wrapper, event);
		}

		@Override
		public int getCommand() {
			return 0;
		}

		@Override
		public long getRoleId() {
			return roleId;
		}

	}
}
