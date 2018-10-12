package com.cai.future.runnable;

import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.define.ELogType;
import com.cai.common.domain.Room;
import com.cai.common.util.ThreadUtil;
import com.cai.dictionary.SysGameTypeDict;
import com.cai.future.BaseFuture;
import com.cai.game.gdy.AbstractGDYTable;
import com.cai.service.MongoDBServiceImpl;
import com.cai.service.PlayerServiceImpl;


public class GDYAutoOutCardRunnable extends BaseFuture{
	
	private static Logger logger = LoggerFactory.getLogger(GDYAutoOutCardRunnable.class);


	private int _room_id;
	private int _seat_index;
	private AbstractGDYTable _table;
	private int _out_type;
	private int _cards[];
	private int _cahnge_cards[];
	private int _card_count;
	public GDYAutoOutCardRunnable(int room_id,int seat_index,AbstractGDYTable table,int cards[],int cahnge_cards[],int card_count,int out_type){
		super(room_id);
		_room_id = room_id;
		_seat_index = seat_index;
		_table=table;
		_out_type=out_type;
		_cards=new int[card_count];
		_cahnge_cards=new int[card_count];
		_card_count=card_count;
		for(int i=0;i<card_count;i++){
			_cards[i]=cards[i];
			_cahnge_cards[i]=cahnge_cards[i];
		}
	}
	
	
	
	@Override
	public void execute() {
		try {
			Room table = PlayerServiceImpl.getInstance().getRoomMap().get(_room_id);
			if(table==null){
				logger.info("调度发牌失败,房间["+_room_id+"]不存在");
				return ;
			}
			//logger.info("调度发牌,房间["+_room_id+"]");
			ReentrantLock roomLock = table.getRoomLock();
			try{
				roomLock.lock();
				_table.auto_out_card(_seat_index,_cards,_cahnge_cards,_card_count,_out_type);
			}finally{
				roomLock.unlock();
				
			}
			
		} catch (Exception e) {
			logger.error("error"+_room_id,e);
			Room room = PlayerServiceImpl.getInstance().getRoomMap().get(_room_id);
			if(room!=null) {
				MongoDBServiceImpl.getInstance().server_error_log(room.getRoom_id(), ELogType.roomLogicError, ThreadUtil.getStack(e),
						0L, SysGameTypeDict.getInstance().getGameDescByTypeIndex(room.getGameTypeIndex()), room.getGame_id());
			}
		}
		
	}
	
	
	

}
