/**
 * 
 */
package com.cai.dictionary;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.cai.common.constant.RedisConstant;
import com.cai.common.domain.MatchCmdModel;
import com.cai.common.util.SpringService;
import com.cai.redis.service.RedisService;

/**
 * 扣豆描述
 *
 * @author tang
 */
public class MatchDict {

	private static MatchDict dict = new MatchDict();
	
	private Map<Integer, MatchCmdModel> cmdMap = new HashMap<>();

	private MatchDict() {}

	public static MatchDict getInstance() {
		return dict;
	}

	@SuppressWarnings("unchecked")
	public void load() {
		RedisService redisService = SpringService.getBean(RedisService.class);
		ArrayList<MatchCmdModel> cmdList = redisService.hGet(RedisConstant.DICT, 
				RedisConstant.DICT_MATCH_CMD, ArrayList.class);
		
		if(cmdList != null){
			Map<Integer, MatchCmdModel> cmdMapTemp = new HashMap<>();
			for(MatchCmdModel model : cmdList){
				cmdMapTemp.put(model.getCmd(), model);
			}
			this.cmdMap = cmdMapTemp;
		}
	}
	
	public MatchCmdModel getCmd(int cmd){
		MatchCmdModel model = cmdMap.get(cmd);
		return model;
	}
}
