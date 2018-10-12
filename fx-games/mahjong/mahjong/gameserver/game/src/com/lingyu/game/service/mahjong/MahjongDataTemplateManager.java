package com.lingyu.game.service.mahjong;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.lingyu.common.core.ServiceException;
import com.lingyu.common.resource.IResourceLoader;
import com.lingyu.common.resource.ResourceManager;
import com.lingyu.common.template.Fan2JifenTemplate;
import com.lingyu.common.template.FanTemplate;
import com.lingyu.common.template.JiFenTemplate;

/**
 * 麻将模板表
 * 
 * @author wangning
 * @date 2017年1月2日 下午4:31:33
 */
@Service
public class MahjongDataTemplateManager implements IResourceLoader {

	@Autowired
	private ResourceManager resourceManager;

	// 积分表
	private Map<Integer, JiFenTemplate> jifenTemplateMap;

	// 翻的倍数模板表
	private Map<Integer, FanTemplate> fanTemplateMap;

	// 番对应的积分
	private Map<Integer, Fan2JifenTemplate> fan2jifenTemplate;

	@Override
	public void initialize() {
		resourceManager.register(this);
	}

	@Override
	public String getResName() {
		return "麻将模板表";
	}

	@Override
	public void load() {
		// 积分模板表
		{
			List<JiFenTemplate> jifenTemplateList = resourceManager.loadTemplate(JiFenTemplate.class);
			Map<Integer, JiFenTemplate> jifenTemplateMap = new HashMap<>();
			for (JiFenTemplate t : jifenTemplateList) {
				t.deserialize();
				jifenTemplateMap.put(t.getSignType(), t);
			}

			this.jifenTemplateMap = jifenTemplateMap;
		}
		{// 翻的倍数模板表
			List<FanTemplate> fanTemplateList = resourceManager.loadTemplate(FanTemplate.class);
			Map<Integer, FanTemplate> fanTemplateMap = new HashMap<>();
			for (FanTemplate t : fanTemplateList) {
				t.deserialize();
				fanTemplateMap.put(t.getId(), t);
			}
			this.fanTemplateMap = fanTemplateMap;
		}

		{// 翻对应积分模板表
			List<Fan2JifenTemplate> fan2jifenTemplateList = resourceManager.loadTemplate(Fan2JifenTemplate.class);
			Map<Integer, Fan2JifenTemplate> fan2jifenTemplateMap = new HashMap<>();
			for (Fan2JifenTemplate t : fan2jifenTemplateList) {
				t.deserialize();
				fan2jifenTemplateMap.put(t.getFan(), t);
			}
			this.fan2jifenTemplate = fan2jifenTemplateMap;
		}
	}

	@Override
	public void checkValid() throws ServiceException {

	}

	/** 获取积分表 */
	public JiFenTemplate getJiFenTemplate(int signType) {
		return resourceManager.getValueFromMap(jifenTemplateMap, JiFenTemplate.class, signType);
	}

	/** 获取翻的倍数模板 */
	public FanTemplate getFanTemplate(int id) {
		return resourceManager.getValueFromMap(fanTemplateMap, FanTemplate.class, id);
	}

	/** 获取翻对应积分模板 */
	public Fan2JifenTemplate getFan2JifenTemplate(int fan) {
		return resourceManager.getValueFromMap(fan2jifenTemplate, Fan2JifenTemplate.class, fan);
	}
}