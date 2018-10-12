package com.cai.future.runnable;

import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.future.BaseFuture;
import com.cai.game.phu.PHTable;
import com.cai.service.PlayerServiceImpl;
/**
 * 调度发牌
 *
 */
public class EffectRunnable extends BaseFuture{
	
	private static Logger logger = LoggerFactory.getLogger(EffectRunnable.class);


	private int _room_id;
	private int _seat_index;
	private int  _peng_sao_count;
	private int _action;
	public EffectRunnable(int room_id,int seat_index,int peng_sao_count,int action){
		super(room_id);
		_room_id = room_id;
		_seat_index = seat_index;
		_peng_sao_count = peng_sao_count;
		_action = action;
		
	}
	
	
	
	@Override
	public void execute() {
		try {
			PHTable table = (PHTable) PlayerServiceImpl.getInstance().getRoomMap().get(_room_id);
			if(table==null){
				logger.info("调度发牌失败,房间["+_room_id+"]不存在");
				return ;
			}
			//logger.info("调度发牌,房间["+_room_id+"]");
			ReentrantLock roomLock = table.getRoomLock();
			try{
				roomLock.lock();
				table.effect_timer(_seat_index,_peng_sao_count, _action);
			}finally{
				roomLock.unlock();
				
			}
			
		} catch (Exception e) {
			logger.error("error"+_room_id,e);
		}
		
	}
	
	
	

}
