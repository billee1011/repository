package com.lingyu.game.service.mahjong;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.lingyu.common.constant.ModuleConstant;
import com.lingyu.game.service.event.ChessJifenEvent;
import com.lingyu.game.service.event.IEventHandler;

/**
 * 清算每局积分
 * @author wangning
 * @date 2017年1月6日 下午3:06:26
 */
@Service
public class MahjongEventHandler implements IEventHandler {

	@Autowired
	private MahjongManager mahjongManager;
	
	@Override
	public String getModule() {
		return ModuleConstant.MODULE_PLAY_MAHJONG;
	}

	public void handle(ChessJifenEvent event) {
		long roleId = event.getRoleId();
		int roomNum = event.getRoomNum();
		int signType = event.getSignType();
		int loseIndex = event.getLoseIndex();
		mahjongManager.jifen(roleId, roomNum, signType, loseIndex);
	}
	
}