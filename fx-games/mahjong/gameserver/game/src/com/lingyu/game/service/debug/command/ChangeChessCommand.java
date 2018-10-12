package com.lingyu.game.service.debug.command;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.lingyu.game.service.debug.Command;
import com.lingyu.game.service.mahjong.MahjongManager;

/**
 * 改变当前牌的列表
 */
@Scope("prototype")
@Component("@ change")
public class ChangeChessCommand extends Command {
	@Autowired
	private MahjongManager mahjongManager;

	private Map<Integer, List<Integer>> map = new HashMap<>();
	
	@Override
	public void analysis(String... args) {
		String strs[] = args[2].split(";");
		for(String s : strs){
			String str[] = s.split("\\|");
			Integer type = Integer.valueOf(str[0]);
			List<Integer> list = map.get(type);
			if(list == null){
				list= new ArrayList<>();
				map.put(type, list);
			}
			String ss[] = str[1].split(",");
			for(String sss: ss){
				list.add(Integer.valueOf(sss));
			}
		}
		if(!map.containsKey(1) && !map.containsKey(2) && !map.containsKey(3) && !map.containsKey(4)){
			this.send("牌的花色有问题");
			return ;
		}
		
		int i = 0;
		for(Integer key : map.keySet()){
			List<Integer> list= map.get(key);
			i += list.size();
			for(Integer n : list){
				if(key == 1 || key == 2 || key ==3){
					if(n > 9){
						this.send("筒条万最大是9");
						return ;
					}
				}else if(key ==4){
					if(n != 10 && n != 11 && n != 12 && n != 13 && n != 14 && n != 15 && n != 16){
						this.send("风牌数值范围为10-16");
						return ;
					}
				}
			}
		}
		if(i > 13){
			this.send("一次只能改变13张牌");
			return ;
		}
	}

	@Override
	public void exec() {
		String str = mahjongManager.GMChangeChess(roleId, map);
		if(str != null){
			this.send(str);
		}
	}

	@Override
	public String help() {
		return "@ change";
	}
}