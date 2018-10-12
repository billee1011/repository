/**
 * Copyright: Copyright (c) 2007
 * <br>
 * Company: Digifun
 * <br>
 */
package com.cai.core;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import com.cai.common.domain.Event;


/**
 * Description:用于获取各Service监控状态的Event<br>
 * 其中Object为Map<String,String>
 * 
 * @Author Insunny
 * @CreateDate 2008-9-9
 */
public class MonitorEvent extends Event<SortedMap<String, String>>
{

	/**
	 * 该source表示当前待监控Service的名称
	 */
	public MonitorEvent(String _source)
	{
		super();
		this.source = _source;
		this.body = new TreeMap<String, String>();
	}

	/**
	 * 添加监控指数
	 * 
	 * @param _key
	 * @param _value
	 */
	public void put(String _key, String _value)
	{
		body.put(_key, _value);
	}

	/**
	 * 添加已经封装好的map
	 * 
	 * @param map
	 */
	public void putAll(Map<String, String> map)
	{
		body.putAll(map);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		Set<Entry<String, String>> set = body.entrySet();
		StringBuffer buffer = new StringBuffer();
		buffer.append("\n" + source + ":\n");
		for (Entry<String, String> entry : set)
		{
			buffer.append(entry.getKey());
			buffer.append(":");
			buffer.append(entry.getValue());
			buffer.append("\n");
		}
		return buffer.toString();
	}

}
