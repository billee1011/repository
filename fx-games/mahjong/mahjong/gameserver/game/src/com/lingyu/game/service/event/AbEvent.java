package com.lingyu.game.service.event;

import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.esotericsoftware.reflectasm.MethodAccess;
import com.lingyu.common.core.ServiceException;
import com.lingyu.common.io.SessionManager;
import com.lingyu.game.GameServerContext;
import com.lingyu.msg.rpc.DispatchEventReq;

/**
 * 事件分发器.
 * 
 * @version 1.0
 * @author 江贵龙 <jiangguilong@lingyuwangluo.com>
 */
public abstract class AbEvent {
	private static final Logger logger = LogManager.getLogger(AbEvent.class);
	protected static EventManager eventManager = GameServerContext.getBean(EventManager.class);
	protected static SessionManager sessionManager = SessionManager.getInstance();
	protected long roleId;
	protected long id;
	private static AtomicLong seq = new AtomicLong(System.currentTimeMillis());

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id=id;
	}

	public long getRoleId() {
		return roleId;
	}
	
	public void setRoleId(long roleId) {
		this.roleId = roleId;
	}

	public String getName() {
		return this.getClass().getSimpleName();
	}

	public abstract void subscribe();

	protected abstract List<HandlerWrapper> getHandlerPipeline();

	protected void dispatch() {
		long start = System.nanoTime();
		id=seq.incrementAndGet();
		// 耗时日志
		List<HandlerWrapper> handlerList = this.getHandlerPipeline();
		// TODO by Allen 以下三行是否可以优化，一半情况下未必需要执行
		DispatchEventReq<AbEvent> req = new DispatchEventReq<>();
		req.setRoleId(roleId);
		req.setEntity(this);
		for (HandlerWrapper wrapper : handlerList) {
			byte group = wrapper.getGroup();
			if (group == 0) {
				// 同线程串行执行
				eventManager.syncCall(wrapper, this);
			}else {
				// 非同线程异步执行
				this.advancedAsyncCall(req, wrapper);
			}
		}
		float interval = (System.nanoTime() - start) / 1000000f;
		// 监控大于20毫秒的处理
		if (interval > 20) {
			logger.debug("event interval={} ms,event={},id={},roleId={}", interval, this.getName(), this.getId(), roleId);
		}
	}
	
	/** 异步事件分发 */
	public void advancedAsyncCall(DispatchEventReq<AbEvent> req, HandlerWrapper wrapper) {
		eventManager.asyncCall(roleId, wrapper, this);
	}

	/** 这些类必须是在spring容器里 */
	protected HandlerWrapper createHandler(byte group, Class<?> clazz) {
		IEventHandler handler = (IEventHandler) GameServerContext.getBean(clazz);
		HandlerWrapper ret = new HandlerWrapper();
		ret.setGroup(group);
		ret.setModule(handler.getModule());
		ret.setHandler(handler);
		
		MethodAccess access = MethodAccess.get(clazz);
		ret.setAccess(access);
		try {
			Method method = handler.getClass().getMethod("handle", this.getClass());
			int methodIndex = access.getIndex(method.getName(), method.getParameterTypes());
			ret.setMethodIndex(methodIndex);
//			Method method = handler.getClass().getMethod("handle", this.getClass());
//			method.setAccessible(true);
//			ret.setMethod(method);
		} catch (NoSuchMethodException | SecurityException e) {
			throw new ServiceException(e);
		}
//		eventManager.registerHandler(ret, this);
		return ret;
	}

	protected HandlerWrapper createHandler(Class<?> clazz) {
		return this.createHandler((byte) 0, clazz);
	}

	public String toString() {
		return ToStringBuilder.reflectionToString(this);
	}

}
