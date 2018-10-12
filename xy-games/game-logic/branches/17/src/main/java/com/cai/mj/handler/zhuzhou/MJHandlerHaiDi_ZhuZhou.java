package com.cai.mj.handler.zhuzhou;

import org.apache.log4j.Logger;

import com.cai.common.constant.MJGameConstants;
import com.cai.common.domain.PlayerStatus;
import com.cai.mj.MJTable;
import com.cai.mj.handler.MJHandlerHaiDi;

public class MJHandlerHaiDi_ZhuZhou extends MJHandlerHaiDi {

	private static Logger logger = Logger.getLogger(MJHandlerHaiDi_ZhuZhou.class);

	@Override
	public void exe(MJTable table) {
		// TODO Auto-generated method stub
		PlayerStatus curPlayerStatus = table._playerStatus[_seat_index];
		curPlayerStatus.reset();

		for (int i = 0; i < MJGameConstants.GAME_PLAYER; i++) {
			table._playerStatus[i].chi_hu_round_valid();// 可以胡了
		}

		curPlayerStatus.add_action(MJGameConstants.WIK_YAO_HAI_DI);
		curPlayerStatus.add_yao_hai_di();

		table.operate_player_action(_seat_index, false);
	}

	/***
	 * //用户操作
	 * 
	 * @param seat_index
	 * @param operate_code
	 * @param operate_card
	 * @return
	 */
	@Override
	public boolean handler_operate_card(MJTable table, int seat_index, int operate_code, int operate_card) {
		if (seat_index != _seat_index) {
			logger.error("[海底],操作失败," + seat_index + "不是当前操作玩家");
			return false;
		}

		if (operate_code == MJGameConstants.WIK_NULL) {
			// 不要海底
			// 流局
			table._banker_select = seat_index;//如果轮到海底牌的玩家不要海底牌，则全部洗牌，其他玩家没有权利要海底牌，也不能胡，下轮由轮到海底牌的做庄
			table.handler_game_finish(table._banker_select, MJGameConstants.Game_End_DRAW);
			return true;
		} else {
			table.exe_yao_hai_di(_seat_index);
		}

		return true;
	}
}
