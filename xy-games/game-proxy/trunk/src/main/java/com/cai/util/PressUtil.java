/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.util;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * 
 *
 * @author wu_hc date: 2017年10月26日 上午9:51:46 <br/>
 */
public final class PressUtil {

	public static final Executor EXECUTOR = Executors.newSingleThreadExecutor();

	private PressUtil() {
	}
}
