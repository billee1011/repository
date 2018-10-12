/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.future;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.constant.MsgConstants;
import com.cai.common.domain.Account;
import com.cai.common.domain.BrandLogModel;
import com.cai.common.domain.GameRoomRecord;
import com.cai.common.domain.Page;
import com.cai.common.domain.Player;
import com.cai.common.domain.PlayerResult;
import com.cai.dictionary.SysGameTypeDict;
import com.cai.handler.RoomHandler;
import com.cai.service.C2SSessionService;
import com.cai.service.MongoDBServiceImpl;
import com.cai.service.PlayerServiceImpl;
import com.cai.util.MessageResponse;
import com.google.common.collect.Maps;
import com.xianyi.framework.core.transport.netty.session.C2SSession;

import protobuf.clazz.ClubMsgProto.ClubRecordReqProto;
import protobuf.clazz.ClubMsgProto.ClubRequest;
import protobuf.clazz.Protocol;
import protobuf.clazz.Protocol.ClubRecordRepProto;
import protobuf.clazz.Protocol.ClubResponse;
import protobuf.clazz.Protocol.ClubResponse.ClubResponseType;
import protobuf.clazz.Protocol.PlayerResultFLSResponse;
import protobuf.clazz.Protocol.PlayerResultResponse;
import protobuf.clazz.Protocol.Response;
import protobuf.clazz.Protocol.Response.ResponseType;
import protobuf.clazz.Protocol.RoomPlayerResponse;
import protobuf.clazz.Protocol.RoomResponse;

/**
 * 
 *
 * @author DIY date: 2018年3月20日 上午1:42:26 <br/>
 */
public class ClubReqRecordRunnable implements Runnable {

	protected final Logger logger = LoggerFactory.getLogger(getClass());

	private ClubRequest request;
	private long accountId;

	private long createTime;

	public ClubReqRecordRunnable(ClubRequest request, long accountId) {
		this.request = request;
		this.accountId = accountId;

		this.createTime = System.currentTimeMillis();
	}

	@Override
	public void run() {

		long now = System.currentTimeMillis();
		long pass = now - createTime;

		C2SSession session = C2SSessionService.getInstance().getSession(accountId);
		if (null == session) {
			return;
		}

		if (pass > 10000) {
			PlayerServiceImpl.getInstance().sendAccountMsg(session, MessageResponse.getMsgAllResponse("查询超时,请稍后重试").build());
			logger.error("Slow ClubReqRecordRunnable 真正执行时间已经大于10秒........." + pass);
			return;
		}

		Account account = session.getAccount();
		if (account == null) {
			return;
		}

		ClubRecordReqProto req = request.getRecordReq();

		long targetAccountId = req.getTargetAccountId();
		int clubId = req.getClubId();

		int l = 0;
		List<BrandLogModel> room_record;

		Page page = new Page();
		page.setPageSize(10);
		page.setRealPage(Math.max(1, req.getPage()));

		if (targetAccountId > 0) {
			room_record = MongoDBServiceImpl.getInstance().getClubParentBrandList(page, clubId, targetAccountId, req.getBeginTime(), req.getEndTime(),
					req.getRuleId());
		} else {
			Map<String, Object> param = null;
			if (req.getRuleId() > 0) {
				param = Maps.newHashMapWithExpectedSize(1);
				param.put("ruleId", req.getRuleId());
			}
			room_record = MongoDBServiceImpl.getInstance().getClubParentBrandList(page, clubId, req.getBeginTime(), req.getEndTime(), param);
		}
		l = room_record.size();

		GameRoomRecord grr = null;
		RoomResponse.Builder game_room_record = RoomResponse.newBuilder();
		game_room_record.setType(MsgConstants.RESPONSE_GAME_ROOM_RECORD_LIST);

		long totalCost = 0;
		for (int k = 0; k < l; k++) {
			boolean error_check = false;
			BrandLogModel brandLogModel = room_record.get(k);
			grr = GameRoomRecord.to_Object(brandLogModel.getMsg());//
			if (grr == null)
				continue;
			int length = grr.getPlayers().length;
			for (int i = 0; i < length; i++) {
				if (i >= grr.getPlayers().length)
					continue;
				if (grr.getPlayers()[i] == null) {
					// error_check = true;
					continue;
				}
			}
			if (error_check)
				continue;

			PlayerResult _player_result = grr.get_player();

			PlayerResultResponse.Builder player_result = PlayerResultResponse.newBuilder();

			// 点赞
			player_result.setUpvote(brandLogModel.getUpvote());
			player_result.setCostGold(brandLogModel.getGold_count());
			totalCost += brandLogModel.getGold_count();
			// 1房卡 2专属豆 @see EWealthCategory
			player_result.setCostType(brandLogModel.isExclusiveGold() ? 3 : 1);
			String subGameName = SysGameTypeDict.getInstance().getMJname(_player_result.getGame_type_index());
			player_result.setSubName(subGameName);
			player_result.setClubRuleId(brandLogModel.getRuleId());

			PlayerResultFLSResponse.Builder playerResultFLSResponse = PlayerResultFLSResponse.newBuilder();

			Player create_player = grr.getCreate_player();
			if (create_player != null) {
				RoomPlayerResponse.Builder room_player = RoomHandler.setPlayerInfo(_player_result, player_result, create_player, length);
				player_result.setCreatePlayer(room_player);
			}
			player_result.setAppId(room_record.get(k).getGame_id());
			if (brandLogModel.getRandomNum() != null) {
				player_result.setRandomNum(brandLogModel.getRandomNum());
			}

			RoomHandler.recorde_common(grr, _player_result, player_result, playerResultFLSResponse, length);
			game_room_record.addGameRoomRecords(player_result);
		}

		if (page != null) {
			game_room_record.setCurPage(page.getRealPage());
			game_room_record.setPageSize(page.getPageSize());
			game_room_record.setTotalPage(page.getTotalPage());
			game_room_record.setTotalSize(page.getTotalSize());
		}

		//
		game_room_record.setTotalCost(totalCost);
		game_room_record.setPageType(1);// 分页--兼容老版本

		ClubResponse.Builder b = ClubResponse.newBuilder();
		b.setType(ClubResponseType.CLUB_RSP_RECORD);

		ClubRecordRepProto.Builder recordB = ClubRecordRepProto.newBuilder();
		recordB.setClubId(clubId);
		recordB.setTargetAccountId(targetAccountId);
		recordB.setRoomResponse(game_room_record);
		b.setRecord(recordB);

		// 返回消息
		Response.Builder responseBuilder = Response.newBuilder();
		responseBuilder.setResponseType(ResponseType.CLUB);
		responseBuilder.setExtension(Protocol.clubResponse, b.build());
		// session.send(responseBuilder.build());
		PlayerServiceImpl.getInstance().sendAccountMsg(session, responseBuilder.build());

	}

}
