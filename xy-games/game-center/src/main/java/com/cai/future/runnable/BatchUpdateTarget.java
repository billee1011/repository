/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.future.runnable;

import java.util.List;

import com.cai.common.domain.AccountParamModel;
import com.google.common.collect.Lists;

/**
 * 
 *
 * @author DIY date: 2018年5月13日 下午3:13:10 <br/>
 */
public class BatchUpdateTarget implements Runnable {

	private List<AccountParamModel> accountModelList = Lists.newArrayListWithCapacity(10000);

	public BatchUpdateTarget(List<AccountParamModel> accountModelList) {
		this.accountModelList.addAll(accountModelList);
	}

	@Override
	public void run() {
		try {
			for (int i = 0; i < accountModelList.size(); i++) {
				accountModelList.get(i).setNeedDB(true);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
