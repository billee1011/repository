package com.cai.future.runnable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.domain.Player;
import com.cai.common.domain.Room;
import com.cai.common.domain.SysParamModel;
import com.cai.common.util.RandomUtil;
import com.cai.dictionary.SysParamDict;
import com.cai.future.BaseFuture;
import com.cai.service.PlayerServiceImpl;
import com.cai.service.PtAPIServiceImpl;

import protobuf.clazz.Protocol.LocationInfor;
import protobuf.clazz.Protocol.LocationInfor.Builder;

public class PositionRunnable extends BaseFuture {

	private static Logger logger = LoggerFactory.getLogger(PositionRunnable.class);

	private int roomID;

	private double pos_x;

	private double pos_y;

	private long account_id;

	private long createTime;

	public PositionRunnable(int roomID, double x, double y, long account_id, long createTime) {
		super(roomID);
		this.roomID = roomID;
		this.pos_x = x;
		this.pos_y = y;
		this.createTime = createTime;
		this.account_id = account_id;
	}

	@Override
	public void execute() {

		Room room = PlayerServiceImpl.getInstance().getRoomMap().get(roomID);

		if (room == null) {
			logger.error("定位房间不存在" + roomID);
			return;
		}

		Player player = room.get_player(account_id);
		if (player == null) {
			logger.error("定位玩家不存在" + account_id);
			return;
		}
		String position = "";

		if ((System.currentTimeMillis() - createTime) > 1000 * 60) {
			logger.error("请求定位超时1分钟不请求百度用老地址" + roomID);

			LocationInfor oldlocationInfor = player.locationInfor;
			if (oldlocationInfor != null) {
				position = oldlocationInfor.getAddress();// 用老地址
			}

			Builder locationInfor = LocationInfor.newBuilder();
			locationInfor.setAddress(position);
			locationInfor.setPosX(pos_x);
			locationInfor.setPosY(pos_y);
			locationInfor.setTargetAccountId(player.getAccount_id());
			player.locationInfor = locationInfor.build();

			if (room != null) {
				boolean r = room.handler_requst_location(player, player.locationInfor);
			}
			return;
		}

		SysParamModel sysParamModel = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(1).get(5005);

		if (sysParamModel.getVal1() == 1) {
			int random = RandomUtil.generateRandomNumber(0, 100);
			if (random > 50 || org.apache.commons.lang.StringUtils.isEmpty(sysParamModel.getStr2()) || sysParamModel.getStr2().equals("0")) {
				position = PtAPIServiceImpl.getInstance().getbaiduPosition(1, pos_x, pos_y);
			} else {
				position = PtAPIServiceImpl.getInstance().getTengXunPosition(1, pos_x, pos_y);
			}
		}

		Builder locationInfor = LocationInfor.newBuilder();
		locationInfor.setAddress(position);
		locationInfor.setPosX(pos_x);
		locationInfor.setPosY(pos_y);
		locationInfor.setTargetAccountId(player.getAccount_id());
		player.locationInfor = locationInfor.build();

		if (room != null) {
			boolean r = room.handler_requst_location(player, player.locationInfor);
		}
	}

}
