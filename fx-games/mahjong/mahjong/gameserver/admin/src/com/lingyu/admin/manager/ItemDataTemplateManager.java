package com.lingyu.admin.manager;

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.lingyu.admin.vo.ItemVo;
import com.lingyu.common.core.ServiceException;
import com.lingyu.common.resource.IResourceLoader;
import com.lingyu.common.resource.ResourceManager;

@Service
public class ItemDataTemplateManager implements IResourceLoader {
	private static final Logger logger = LogManager.getLogger(ItemDataTemplateManager.class);
	private Map<String,ItemVo> itemMap = new HashMap<String,ItemVo>();
	@Autowired
	private ResourceManager resourceManager;
	
	public void initialize() {
		resourceManager.register(this);
	}

	@Override
	public String getResName() {
		return "道具数据模板";
	}

	

	@Override
	public void load() {
		
	}

	@Override
	public void checkValid() throws ServiceException {

	}
}