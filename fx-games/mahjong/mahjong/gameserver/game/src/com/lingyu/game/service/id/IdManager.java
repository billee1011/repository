package com.lingyu.game.service.id;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import com.lingyu.common.db.GameRepository;
import com.lingyu.game.GameServerContext;

@Service
public class IdManager {
	private static final Logger logger = LogManager.getLogger(IdManager.class);
	
	private final Map<String, Long> ids = new ConcurrentHashMap<>();
	
	
	public void init(){
		logger.info("id生成器开始");
		try {
			GameRepository repository = GameServerContext.getGameRepository();
			for (Field f : TableNameConstant.class.getDeclaredFields()) {
				String tableName = f.get(TableNameConstant.class).toString();
				long maxId = repository.getMaxId(tableName);
				if(maxId == 0){
					if(tableName.equals("role")){
						ids.put(tableName, IdMaxConstant.ROLE);
					}else if(tableName.equals("user")){
						ids.put(tableName, IdMaxConstant.USER);
					}else{
						ids.put(tableName, IdMaxConstant.COMMON);
					}
				}else{
					ids.put(tableName, maxId);
				}
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		logger.info("id生成器结束");
	}
	
	/**
	 * 获取新的id
	 * @param tableName
	 * @return
	 */
	public Long newId(String tableName){
		Long maxId = ids.get(tableName);
		if(maxId == null){
			logger.error("id生成规则出错,tableName={}",tableName);
			return null;
		}
		long newId = maxId + 1;
		ids.put(tableName, newId);
		return newId;
	}
}
