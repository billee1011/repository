package com.cai.common.domain;

import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 用于分配房间号的
 * @author run
 *
 */
public class RandomGameRoomModel {
	
	private static Logger logger = LoggerFactory.getLogger(RandomGameRoomModel.class);
	
	private Lock lock = new ReentrantLock();

	private int game_id;
	
	private List<Integer> roomIdList;
	
	/**
	 * 当前下标
	 */
	private int curIndex;
	
	
	public RandomGameRoomModel(int game_id,List<Integer> roomIdList){
		this.game_id = game_id;
		this.roomIdList = roomIdList;
	}
	
	
	
	public int randomRoomId(){
		lock.lock();
		try {
			if(curIndex>=roomIdList.size())
				curIndex = 0;
			
			int room_id = roomIdList.get(curIndex);
			curIndex++;
			return room_id;
			
		} catch (Exception e) {
			logger.error("error", e);
		}finally{
			lock.unlock();
		}
		
		return -1;
	}
	

	public Lock getLock() {
		return lock;
	}

	public void setLock(Lock lock) {
		this.lock = lock;
	}

	public List<Integer> getRoomIdList() {
		return roomIdList;
	}

	public void setRoomIdList(List<Integer> roomIdList) {
		this.roomIdList = roomIdList;
	}

	public int getGame_id() {
		return game_id;
	}

	public void setGame_id(int game_id) {
		this.game_id = game_id;
	}
	
	
	
	
}
