package com.cai.game.mj.ai.basic;

import com.cai.ai.AbstractAi;
import com.cai.ai.AiWrap;
import com.cai.ai.IRootAi;
import com.cai.ai.RobotPlayer;
import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.game.mj.AbstractMJTable;

import protobuf.clazz.Protocol.RoomResponse;

@IRootAi(gameIds = {},gameType = 1, desc = "麻将飘分或者类型飘分",msgIds = {MsgConstants.RESPONSE_PAO_QIANG_ACTION })
public class MJPaoBasic extends AbstractAi<AbstractMJTable> {

	public MJPaoBasic() {
	}

	@Override
	protected boolean isNeedExe(AbstractMJTable table, RobotPlayer player, RoomResponse rsp) {
		//int seat_index = player.get_seat_index();
		//table.log_error("自动跑 呛 判断开始");
		if (table._game_status != GameConstants.GS_MJ_PIAO &&table._game_status != GameConstants.GS_MJ_PAO){
			//table.log_error("自动跑 呛 状态不对");
			return false;
		}
		return true;
	}

	@Override
	public void onExe(AbstractMJTable table, RobotPlayer player, RoomResponse rsp) {
		//跑呛
		//WalkerGeek 测试，正式删除
		//table.log_error("自动跑 呛");
		table.handler_requst_pao_qiang(player, 0, 0);
	}

	@Override
	protected AiWrap needDelay(AbstractMJTable table, RobotPlayer player, RoomResponse rsp) {
		boolean flag = player.isIsTrusteeOver();
		//自建赛不需要一拖到底
		if(table.isClubMatch()){
			flag = false;
		}
		// 超时出牌
		return new AiWrap(flag, 5000);
	}

}
