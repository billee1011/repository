/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.dictionary;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.cai.common.domain.CityCodeModel;
import com.cai.common.util.SpringService;
import com.cai.dao.ClubDao;

/**
 * 城市处理
 * 
 * @author tang
 */
public class CityDict {
	/**
	 * 
	 * /** 网关服
	 */
	private static Map<Integer, String> cityDictionary;

	/**
	 * 单例
	 */
	private final static CityDict instance = new CityDict();

	/**
	 * 私有构造
	 */
	private CityDict() {
		cityDictionary = new HashMap<>();
	}

	/**
	 * 单例模式
	 * 
	 * @return 字典单例
	 */
	public static CityDict getInstance() {
		return instance;
	}

	/**
	 * 
	 */
	public void load() {
		List<CityCodeModel> list = SpringService.getBean(ClubDao.class).getCityCodeModelList();
		for(CityCodeModel model:list){
			cityDictionary.put(model.getCode(), model.getCity_name());
		}
		
	}

	/**
	 * 
	 * @return
	 */
	public Map<Integer, String> getCityDictionary() {
		return cityDictionary;
	}
	
	public String getCityNameByCode(int code){
		return cityDictionary.get(code)==null?"未知":cityDictionary.get(code);
		
	}
}
