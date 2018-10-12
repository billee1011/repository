package com.cai.constant;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class CityReportModel {
	
	private final Map<Integer,Set<Long>> cityMap = new HashMap<>();

	public Map<Integer, Set<Long>> getCityMap() {
		return cityMap;
	}
	
	public void addCityReport(long accountId,Integer cityCode){
		Set<Long> set = this.cityMap.get(cityCode);
		if(set == null){
			set = new HashSet<Long>();
			set.add(accountId);
			this.cityMap.put(cityCode, set);
		}else{
			set.add(accountId);
		}
	}
	
	public void clearMap(){
		this.cityMap.clear();
	}
}
