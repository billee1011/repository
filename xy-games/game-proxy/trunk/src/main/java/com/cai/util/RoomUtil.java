package com.cai.util;

import com.cai.module.RoomModule;

public class RoomUtil {
	public static int getRoomId(long accountId){
		return RoomModule.getRoomId(accountId);
	}
}
