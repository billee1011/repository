/**
 * 
 */
package com.cai.game;

import org.apache.commons.lang.StringUtils;

import com.cai.common.domain.Room;
import com.cai.common.domain.SysParamModel;
import com.cai.dictionary.SysParamDict;
import com.cai.service.PlayerServiceImpl;

/**
 * @author xwy
 *
 */
public class RoomUtil {

	public static void realkou_dou(Room room) {
		if (room.is_sys()) {
			return;
		}
		// 判断房卡
		SysParamModel sysParamModel1010 = SysParamDict.getInstance()
				.getSysParamModelDictionaryByGameId(room.getGame_id()).get(1010);
		SysParamModel sysParamModel1011 = SysParamDict.getInstance()
				.getSysParamModelDictionaryByGameId(room.getGame_id()).get(1011);
		SysParamModel sysParamModel1012 = SysParamDict.getInstance()
				.getSysParamModelDictionaryByGameId(room.getGame_id()).get(1012);
		int check_gold = 0;
		boolean create_result = true;

		if (room._game_round == 4) {
			check_gold = sysParamModel1010.getVal2();
		} else if (room._game_round == 8) {
			check_gold = sysParamModel1011.getVal2();
		} else if (room._game_round == 16) {
			check_gold = sysParamModel1012.getVal2();
		}

		// 注意游戏ID不一样
		if (check_gold == 0) {
			create_result = false;
		} else {
			// 是否免费的
			SysParamModel sysParamModel = SysParamDict.getInstance()
					.getSysParamModelDictionaryByGameId(room.getGame_id()).get(room.game_index);

			if (sysParamModel != null && sysParamModel.getVal2() == 1) {
				// 收费
				StringBuilder buf = new StringBuilder();
				buf.append("真实扣豆 创建房间:" + room.getRoom_id()).append("game_id:" + room.getGame_id())
						.append(",game_type_index:" + room._game_type_index).append(",game_round:" + room._game_round);
				PlayerServiceImpl.getInstance().subRealGold(room.getRoom_owner_account_id(), check_gold, false,
						buf.toString());

				if (StringUtils.isNotEmpty(room.groupID)) {
					PlayerServiceImpl.getInstance().subRobotGold(room.getRoom_owner_account_id(), check_gold, false,
							buf.toString(), room.groupID, room.groupName, room._game_type_index, room._game_round,
							room._game_rule_index, room.getRoom_id());
				}

			}
		}
	}
	
}
