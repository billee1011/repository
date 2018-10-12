/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 *
 */
package com.xianyi.framework.core.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author wu_hc
 */
public abstract class AbstractService implements Service {

	/**
	 * 日志
	 */
	protected final Logger logger = LoggerFactory.getLogger(getClass());

	@Override
	public boolean isRunning() {
		return true;
	}

	@Override
	public String name() {
		return null;
	}

	@Override
	public int order() {
		return 0;
	}
}
