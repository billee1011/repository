/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.test.platservice;

import com.cai.common.domain.json.BaiduLBSJsonModel;
import com.cai.service.PtAPIServiceImpl;

/**
 * 
 *
 * @author wu_hc date: 2017年8月2日 上午10:35:16 <br/>
 */
public final class PtAPIServiceImpTest {

	public static void main(String[] args) {
		BaiduLBSJsonModel xxx = PtAPIServiceImpl.getInstance().getLBSModelFromIP(1, "14.215.177.37");
		System.out.println(xxx);

	}
}
