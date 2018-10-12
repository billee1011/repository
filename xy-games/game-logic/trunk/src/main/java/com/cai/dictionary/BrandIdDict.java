package com.cai.dictionary;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.core.SystemConfig;

/**
 * 牌局id字典
 * 
 * @author run
 *
 */
public class BrandIdDict {

	private Logger logger = LoggerFactory.getLogger(BrandIdDict.class);

	private SimpleDateFormat df = new SimpleDateFormat("yyMMddHHmmss");

	private String lastDateStr;

	private int index = 1;

	public synchronized long getId__() {

		try {
			String newDateStr = df.format(new Date());
			if (newDateStr.equals(lastDateStr)) {
				index++;
				if (index > 9999) {
					Thread.sleep(1000L);
					newDateStr = df.format(new Date());
					lastDateStr = newDateStr;
					index = 1;
				}

			} else {
				index = 1;
				lastDateStr = newDateStr;
			}

			int logic_id = SystemConfig.logic_index;

			// 日期 + logic_id + index

			StringBuffer buf = new StringBuffer();
			buf.append(newDateStr).append(String.format("%03d", logic_id)).append(String.format("%04d", index));

			return Long.parseLong(buf.toString());

		} catch (Exception e) {
			logger.error("error", e);
		}

		return 0L;
	}

	/**
	 * 
	 * @return
	 */
	public long getId() {
//		return SpringService.getBean(ICenterRMIServer.class).allocateId(EIDType.BRAND, SystemConfig.logic_index);
		return getId__();
	}

	/**
	 * 单例
	 */
	private static BrandIdDict instance;

	/**
	 * 私有构造
	 */
	private BrandIdDict() {
		lastDateStr = df.format(new Date());
	}

	/**
	 * 单例模式
	 * 
	 * @return 字典单例
	 */
	public static BrandIdDict getInstance() {
		if (null == instance) {
			instance = new BrandIdDict();
		}

		return instance;
	}

}
