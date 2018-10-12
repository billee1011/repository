/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.common.define;

/**
 * IO流向
 *
 * @author wu_hc date: 2017年7月13日 上午11:52:50 <br/>
 */
public enum EIODire {
	NONE("--"), C2S("客户端<->代理服"), S2S("代理服<->逻辑服");

	private final String description;

	EIODire(String description) {
		this.description = description;
	}

	/**
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}
}
