/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.game.manager;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import com.game.Room;
import com.google.common.collect.Maps;

/**
 * 
 *
 * @author wu_hc date: 2017年10月11日 下午4:24:41 <br/>
 */
public final class RoomMananger {

	private Map<Integer, Room> rooms = Maps.newConcurrentMap();

	/**
	 * 
	 */
	private final static ScheduledExecutorService misAction = Executors.newSingleThreadScheduledExecutor();

	private static RoomMananger M = new RoomMananger();

	/**
	 * @param rooms
	 */
	private RoomMananger() {
		startMatchThread();
	}

	public static RoomMananger getInstance() {
		return M;
	}

	public void add(final Room room) {
		rooms.put(room.getRoomId(), room);
	}

	public void remove(int roomId) {
		rooms.remove(roomId);
	}

	public Room get(final int roomId) {
		return rooms.get(roomId);
	}

	public Map<Integer, Room> rooms() {
		return rooms;
	}

	/**
	 * 
	 * @return
	 */
	public Room getUnFullRoom() {
		for (final Room room : rooms.values()) {
			if (!room.isRoomFull()) {
				return room;
			}
		}

		return null;
	}

	public void startMatchThread() {
		misAction.scheduleAtFixedRate(() -> {
			for (final Room room : rooms.values()) {
				if (!room.isRoomFull()) {
					continue;
				}
				int weight = ThreadLocalRandom.current().nextInt(10);
				if (weight > 4) {
					room.randomChat();
				}
			}
		}, 100, 100, TimeUnit.MILLISECONDS);
	}
}
