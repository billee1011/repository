/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.game.test;

import org.apache.log4j.PropertyConfigurator;

/**
 * 
 *
 * @author wu_hc date: 2017年10月24日 上午10:24:08 <br/>
 */
public abstract class AbstractTest {

	static {
		PropertyConfigurator.configureAndWatch("log4j.properties", 5000);
	}
}
