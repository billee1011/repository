package com.lingyu.common.resource;

import com.lingyu.common.core.ServiceException;

public interface IResourceManager {
	public void register(IResourceLoader loader);

	public void reloadAll() throws ServiceException;

	public void reload(String resName) throws ServiceException;
}
