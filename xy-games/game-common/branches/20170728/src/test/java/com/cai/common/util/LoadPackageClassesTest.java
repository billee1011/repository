/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 *
 */
package com.cai.common.util;

import java.io.IOException;
import java.util.Set;

import com.xianyi.framework.core.transport.ICmd;

/**
 *
 * @author wu_hc
 */
public final class LoadPackageClassesTest {

	public static void main(String[] args) {
		LoadPackageClasses classes = new LoadPackageClasses(new String[]{"com.cai.common.util"},ICmd.class);
		try {
			Set<Class<?>> x = classes.getClassSet();
			System.out.println(x);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
