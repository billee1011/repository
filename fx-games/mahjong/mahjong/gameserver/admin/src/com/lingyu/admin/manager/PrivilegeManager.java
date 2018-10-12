package com.lingyu.admin.manager;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import com.lingyu.admin.privilege.PrivilegeConstant;
import com.lingyu.admin.privilege.Resource;
import com.lingyu.admin.privilege.ResourceType;
import com.lingyu.admin.vo.MenuVO;
import com.lingyu.admin.vo.ModuleVO;

@Service
public class PrivilegeManager {
	private static final Logger logger = LogManager.getLogger(PrivilegeManager.class);
	private Map<Integer, ModuleVO> modules = new HashMap<>();

	public void initialize() {
		Field[] fields = PrivilegeConstant.class.getFields();
		for (Field field : fields) {
			try {
				Resource resource = field.getAnnotation(Resource.class);
				int code = (Integer) field.get(PrivilegeConstant.class);
				String name = resource.name();
				if (resource.type().equals(ResourceType.MODULE)) {
					ModuleVO moduleDTO = new ModuleVO();
					moduleDTO.setCode(code);
					moduleDTO.setName(name);
					modules.put(code, moduleDTO);
				} else {
					MenuVO menuDTO = new MenuVO();
					menuDTO.setCode(code);
					menuDTO.setName(name);
					ModuleVO moduleVO = modules.get(resource.module());
					moduleVO.addMenuDTO(menuDTO);
				}
			} catch (Exception e) {
				logger.error(e.getMessage() + ":" + field.getName(), e);
			}
		}
		logger.info("{}", modules);
	}

	public Collection<ModuleVO> getModuleList() {
		return modules.values();
	}

	public ModuleVO getMenuList(int moduleId) {
		return modules.get(moduleId);
	}
}
