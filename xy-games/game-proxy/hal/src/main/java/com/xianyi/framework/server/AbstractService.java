package com.xianyi.framework.server;

import java.util.SortedMap;

import com.cai.common.domain.Event;
import com.cai.core.MonitorEvent;
import com.cai.domain.Session;



/**
 * Description:所有Service的基类<br>
 * 采用泛型T来定义config的目的在于在AbstractManager的子类中使用config时不需要转型为具体的 AbstractConfig的子类了。<br>
 * 例如：EchoServiceImpl中的EchoConfig不再使用 <code>
 *  ((EchoConfig)config).getChClass();
 * </code> 的调用方式
 */
public abstract class AbstractService implements Comparable<AbstractService>
{
	public int order;
	
	public String name;
	
	
	public int getOrder()
	{
		return order;
	}

	public void setOrder(int order)
	{
		this.order = order;
	}

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	/**
	 * 初始化
	 * 
	 * @param _root
	 * @throws Exception
	 */
	public void initService() throws Exception
	{
		startService();
	}

	/**
	 * 服务类被加载完成之后，会调用这个方法<br>
	 * 具体Service的初始化工作在此处调用
	 */
	protected abstract void startService();

	/**
	 * 返回服务状态
	 * 
	 * @return 服务状态
	 */
	public abstract MonitorEvent montior();
	/**
	 * 执行事件处理
	 * 
	 * @param event 事件对象
	 */
	public abstract void onEvent(Event<SortedMap<String, String>> event);

	/**
	 * 根据_userID初始化玩家相关数据
	 */
	public abstract void sessionCreate(Session session);

	/**
	 * 根据_userID清理玩家相关数据
	 */
	public abstract void sessionFree(Session session);

	/**
	 * 根据_userID更新玩家数据库
	 * 
	 * @param _userID
	 */
	public abstract void dbUpdate(int _userID);

    public int compareTo (AbstractService otherService)
    {
        if (order < otherService.order)
            return -1;
        else if (order > otherService.order)
            return 1;
        return 0;
    }
    /**
     * HANDLER 处理后执行协议
     * @param _userID
     */
    public Object afterHandlerProcces(int _userID)
    {
    	return null;
    }
}
