package com.lingyu.game.service.config;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.lingyu.common.core.ServiceException;
import com.lingyu.common.resource.IResourceLoader;
import com.lingyu.common.resource.ResourceManager;
import com.lingyu.common.template.ConfigTemplate;

@Service
public class ConfigDataTemplateManager implements IResourceLoader {
	private Map<Integer, ConfigTemplate> configTemplateMap = new HashMap<Integer, ConfigTemplate>();
	@Autowired
	private ResourceManager resourceManager;
	
	private Map<Integer, Integer> houseCarMap = null;


	@Override
	public String getResName() {
		return "全局配置数据模板";
	}

	@Override
	public void initialize() {
		resourceManager.register(this);
	}

	@Override
	public void load() {
		List<ConfigTemplate> configTemplateList = resourceManager.loadTemplate(ConfigTemplate.class);
		for (ConfigTemplate e : configTemplateList) {
			configTemplateMap.put(e.getType(), e);
		}
		ConstantUtil.build(configTemplateMap);
		configTemplateMap.clear();
		
		checkInitConfig();
	}

	/** 初始化及校验配置信息 */
	private void checkInitConfig() {
		initHouseCar();
	}
	
	/**
	 * 初始化消耗房卡数据
	 */
	private void initHouseCar(){
		Map<Integer, Integer> houseCarMap = new HashMap<>();
		String costCars = ConfigConstant.COST_CARS;
		if(StringUtils.isNotEmpty(costCars)){
			String strs[] = costCars.split(";");
			for(String str : strs){
				String s[] = str.split(":");
				int jushu = Integer.valueOf(s[0]);
				int costCar = Integer.valueOf(s[1]);
				houseCarMap.put(jushu, costCar);
			}
		}
		this.houseCarMap = houseCarMap;
	}
	
	/**
	 * 根据局数得到消耗房卡数据
	 * @param jushu
	 * @return
	 */
	public Integer getCostCarTemp(int jushu){
		return houseCarMap.get(jushu);
	}

	@Override
	public void checkValid() throws ServiceException {

	}
}
