/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.dictionary;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;

import com.cai.common.domain.GaoDeModel;
import com.cai.common.util.SpringService;
import com.cai.dao.PublicDAO;
import com.cai.service.PublicService;

/**
 * 城市处理
 * 
 * @author tang
 */
public class CityDict {
	private  Map<Integer, Integer> cityCodeToAdcodeDictionary;
	private Map<Integer,Integer> sfMap = new HashMap<>();
	private Map<Integer,Integer> csMap = new HashMap<>();
	/**
	 * 单例
	 */
	private final static CityDict instance = new CityDict();

	/**
	 * 私有构造
	 */
	private CityDict() {
		cityCodeToAdcodeDictionary = new HashMap<>();
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
		PublicService publicService = SpringService.getBean(PublicService.class);
		PublicDAO dao = publicService.getPublicDAO();
		List<GaoDeModel> list = dao.getGaoDeCityCodeModelList();
		
		for(GaoDeModel model:list){
			if(model.getCityCode()>0){
				if(model.getType()==1){
					sfMap.put(model.getCityCode(), 1);
				}
				if(model.getType()==2){
					csMap.put(model.getCityCode(), model.getCityParent());
				}
				cityCodeToAdcodeDictionary.put(model.getCityCode(), model.getAdcode());
			}
		}
		
	}
	public String cityCodeToAdcode(String cityCodes){
		if(StringUtils.isBlank(cityCodes)){
			return "";
		}
		if(cityCodes.equals("1")){
			return "100000";
		}
		StringBuffer adcodeSb = new StringBuffer();
		String[] ccs = cityCodes.split(",");
		Set<Integer> sfSet = new HashSet<>();
		for(String c:ccs){
			int code = Integer.parseInt(c);
			if(sfMap.containsKey(code)){
				sfSet.add(code);
			}
		}
		for(String c:ccs){
			int code = Integer.parseInt(c);
			if(csMap.containsKey(code)){
				if(sfSet.contains(csMap.get(code))){
					continue;
				}
			}
			adcodeSb.append(getAdcodeByCityCode(Integer.parseInt(c))).append(",");
		}
		String res = adcodeSb.toString();
		if(StringUtils.isNotBlank(res)){
			return res.substring(0, res.length()-1);
		}else{
			return "";
		}
	}
	public Integer getAdcodeByCityCode(int cityCode){
		Integer adcode = cityCodeToAdcodeDictionary.get(cityCode);
		if(adcode == null){
			return cityCode;
		}else{
			return adcode;
		}
	}
	public  Map<Integer, Integer> getCityCodeToAdcodeDictionary() {
		return cityCodeToAdcodeDictionary;
	}
	

	
}
