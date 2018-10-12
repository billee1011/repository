package com.cai.game.mj.ai.honzhong;

import com.cai.ai.AbstractAi;
import com.cai.ai.AiWrap;
import com.cai.ai.IRootAi;
import com.cai.ai.RobotPlayer;
import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.game.mj.AbstractMJTable;

import protobuf.clazz.Protocol.RoomResponse;

@IRootAi(gameIds = { GameConstants.GAME_TYPE_HALL_HONG_ZHONG_MJ,GameConstants.GAME_TYPE_HALL_ZHUAN_ZHUAN_MJ  }, desc = "红中麻将飘分",msgIds = {MsgConstants.RESPONSE_PAO_QIANG_ACTION },roomType = 7)
public class MJPao extends AbstractAi<AbstractMJTable> {

	public MJPao() {
	}

	@Override
	protected boolean isNeedExe(AbstractMJTable table, RobotPlayer player, RoomResponse rsp) {
		if (table._game_status != GameConstants.GS_MJ_PIAO){
			return false;
		}
		return true;
	}

	@Override
	public void onExe(AbstractMJTable table, RobotPlayer player, RoomResponse rsp) {
		//跑呛
		table.handler_requst_pao_qiang(player, 0, 0);
	}

	@Override
	protected AiWrap needDelay(AbstractMJTable table, RobotPlayer player, RoomResponse rsp) {
		// 超时出牌
		return new AiWrap(player.isIsTrusteeOver(), 5000);
	}

}
