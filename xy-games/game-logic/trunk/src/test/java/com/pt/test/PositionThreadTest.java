/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.pt.test;

import java.util.concurrent.TimeUnit;

import com.cai.core.Global;

/**
 * 
 *
 * @author XXY date: 2017年9月4日 上午10:52:42 <br/>
 */
public final class PositionThreadTest {

	public static void main(String[] args) {

		for (;;) {
			try {
				TimeUnit.MILLISECONDS.sleep(109L);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

	}

}
