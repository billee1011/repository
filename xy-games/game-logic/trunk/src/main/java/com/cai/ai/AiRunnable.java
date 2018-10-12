package com.cai.ai;

import com.cai.common.base.BaseTask;
import com.cai.game.AbstractRoom;
import com.cai.service.AiService;
import com.cai.service.PlayerServiceImpl;

import protobuf.clazz.Protocol.RoomResponse;

public class AiRunnable<T extends AbstractRoom> extends BaseTask{
	
	private final AbstractAi<T> handler;
	
	private final RobotPlayer player;
	
	private final RoomResponse rsp;
	
	private final T table;
	
	private final int aiFlag;
	
	private final AiWrap aiWrap;
	
	private boolean lastIsTrustee;
	
	public AiRunnable(AbstractAi<T> handler, RobotPlayer player,RoomResponse rsp, T t, int aiFlag, AiWrap aiWrap){
		this.handler  =handler;
		this.player = player;
		this.rsp = rsp;
		this.table = t;
		this.aiFlag = aiFlag;
		this.aiWrap = aiWrap;
		
		this.lastIsTrustee = getLastIsTrustee();
	}
	
	private boolean getLastIsTrustee(){
		boolean status = false;
		try {
			table.getRoomLock().lock();
			status = table.istrustee[player.get_seat_index()];
		}catch (Exception e) {
		}finally {
			table.getRoomLock().unlock();
		}
		return status;
	}

	@Override
	public void execute() {
		if(table == null){
			player.cancel();
			return;
		}
	
		try {
			table.getRoomLock().lock();
			
			if(player.get_seat_index() < 0){
				return;
			}
			
			if(!player.isRobot()){
				boolean isNowTrustee = table.istrustee[player.get_seat_index()];
				boolean isValid = handler.isValidMaxTrusteeTime(table);
				/*LOGGER.info("AiRunnable->>>accountId:{} lastIsTrustee:{} isNowTrustee:{} isValid:{} !!",
						player.getAccount_id(),lastIsTrustee,isNowTrustee,isValid);*/
				if(lastIsTrustee && !isNowTrustee && isValid){
					player.cancel();
					AiWrap nextAiWrap = aiWrap.getNextAiWrap();
					if (nextAiWrap.getDelayTime() > 0) {
						AiService.getInstance().schedule(player.getAccount_id(),
								handler, player, rsp, table, aiFlag, nextAiWrap);
					}
					return;
				}
			}
			//操作标记不对
			if(!table.isUseAi(player.get_seat_index(), aiFlag)){
				return;
			}
			
			if(PlayerServiceImpl.getInstance().getRoomMap().containsKey(table.getRoom_id())){
				if(handler.doExe(table, player, rsp) == false)
					return ;
				if(aiWrap.isNeedTrustee() && table.istrustee[player.get_seat_index()] == false){
					table.handler_request_trustee(player.get_seat_index(), true, 0);
				}
			}
		} finally {
			table.getRoomLock().unlock();
		}
	}

	@Override
	public String getTaskName() {
		return "AiRunnable";
	}

}
