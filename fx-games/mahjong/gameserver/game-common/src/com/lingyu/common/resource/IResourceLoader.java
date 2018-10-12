package com.lingyu.common.resource;

import com.lingyu.common.core.ServiceException;

public interface IResourceLoader {
	public void initialize();

	/** 数据加载 */
	public void load() throws ServiceException;

	/** 数据的合法性检测 */
	public void checkValid() throws ServiceException;

	public String getResName();
}
