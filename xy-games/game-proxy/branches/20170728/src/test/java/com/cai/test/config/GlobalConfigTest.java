/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 *
 */
package com.cai.test.config;

import com.cai.common.config.GlobalParseDom4j;

/**
 *
 * @author wu_hc
 */
public final class GlobalConfigTest {

	public static void main(String[] args) {
		GlobalParseDom4j.getInstance().parse();
	}

}
