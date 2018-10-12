/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.tasks;

import java.util.List;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.S2CCmd;
import com.cai.common.define.MilitaryExploitsType;
import com.cai.common.domain.BrandLogModel;
import com.cai.common.domain.GameRoomRecord;
import com.cai.common.domain.Page;
import com.cai.common.domain.Player;
import com.cai.common.domain.PlayerResult;
import com.cai.common.util.PBUtil;
import com.cai.handler.RoomHandler;
import com.cai.service.C2SSessionService;
import com.cai.service.MongoDBServiceImpl;
import com.cai.service.PlayerServiceImpl;
import com.cai.util.MessageResponse;
import com.xianyi.framework.core.transport.netty.session.C2SSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import protobuf.clazz.Protocol.PlayerResultFLSResponse;
import protobuf.clazz.Protocol.PlayerResultResponse;
import protobuf.clazz.Protocol.RoomPlayerResponse;
import protobuf.clazz.c2s.C2SProto.MilitaryExploitsReqProto;
import protobuf.clazz.c2s.C2SProto.MilitaryExploitsRspProto;
import protobuf.clazz.c2s.C2SProto.MilitaryExploitsRspProto.Builder;

/**
 * 请求战绩任务
 *
 * @author wu_hc date: 2017年11月30日 上午10:56:28 <br/>
 */
public final class GameRecordTask implements Runnable {

	protected final Logger logger = LoggerFactory.getLogger(getClass());

	private final long accountId; // 可能比较耗时，不传入session/account对象，防止不能正常释放
	private final MilitaryExploitsReqProto req;

	private long createTime;

	/**
	 * @param accountId
	 * @param req
	 */
	public GameRecordTask(long accountId, MilitaryExploitsReqProto req) {
		this.accountId = accountId;
		this.req = req;
		this.createTime = System.currentTimeMillis();
	}

	@Override
	public void run() {

		int reqType = req.getType();

		MilitaryExploitsRspProto.Builder builder = null;

		long now = System.currentTimeMillis();
		long pass = now - createTime;
		if (pass > 15000) {
			C2SSession session = C2SSessionService.getInstance().getSession(accountId);
			if (session != null) {
				PlayerServiceImpl.getInstance().sendAccountMsg(session, MessageResponse.getMsgAllResponse("查询请求超时,请稍后重试").build());
			}
			logger.error("Slow 战绩请求超时大于15秒.." + pass);
			return;
		}

		switch (reqType) {
		case MilitaryExploitsType.PERSONAL:
			builder = personal(req, accountId);
			break;
		case MilitaryExploitsType.CLUB:
			builder = club(req, accountId);
			break;
		case MilitaryExploitsType.PROXY:
			builder = proxy(req, accountId);
			break;
		case MilitaryExploitsType.ASSISTANT:
			builder = assistant(req, accountId);
			break;
		case MilitaryExploitsType.CLUB_MATCH:
			builder = clubMatchAll();
			break;
		case MilitaryExploitsType.CLUB_MATCH_PERSONAL:
			builder = clubMatchPersonal();
			break;
		default:
			break;
		}

		C2SSession session = C2SSessionService.getInstance().getSession(accountId);
		builder.setType(reqType);
		builder.setClubId(req.getClubId());
		if (null != session) {
			session.send(PBUtil.toS2CCommonRsp(S2CCmd.NEW_GAME_RECORDS, builder));
		}
	}

	private Builder clubMatchAll() {
		MilitaryExploitsRspProto.Builder builder;
		if (req.getClubMatchId() <= 0) {
			builder = MilitaryExploitsRspProto.newBuilder();
		} else {
			List<BrandLogModel> room_record = MongoDBServiceImpl.getInstance().getClubMatchParentBrandList(req.getClubMatchId(), 0, req.getClubId());
			builder = newRecordBuilder(room_record, null, 0, 0, 0);
		}
		builder.setClubMatchId(req.getClubMatchId());
		return builder;
	}

	private Builder clubMatchPersonal() {
		MilitaryExploitsRspProto.Builder builder;
		if (req.getClubMatchId() <= 0) {
			builder = MilitaryExploitsRspProto.newBuilder();
		} else {
			List<BrandLogModel> room_record = MongoDBServiceImpl.getInstance()
					.getClubMatchParentBrandList(req.getClubMatchId(), accountId, req.getClubId());
			builder = newRecordBuilder(room_record, null, 0, 0, 0);
		}
		builder.setClubMatchId(req.getClubMatchId());
		return builder;
	}

	private Builder assistant(MilitaryExploitsReqProto req, long accountId) {
		long b = req.getBeginTime();
		long e = req.getEndTime();
		// 分页
		int realPage = req.getPage();
		Page page = new Page();
		page.setPageSize(10);
		page.setRealPage(realPage);
		if (b > 0) {
			b *= 1000;
			e *= 1000;
		}
		List<BrandLogModel> room_record = MongoDBServiceImpl.getInstance().getAssistantParentBrandListByAccountId(page, accountId, 0, b, e);
		int count = MongoDBServiceImpl.getInstance().getOpenRoomConsume(GameConstants.CREATE_ROOM_ROBOT, accountId, 0, b, e);
		return newRecordBuilder(room_record, page, count, 0, 0);
	}

	/**
	 * @param req
	 * @param accountId
	 * @return
	 */
	private Builder proxy(MilitaryExploitsReqProto req, long accountId) {
		long b = req.getBeginTime();
		long e = req.getEndTime();
		// 分页
		int realPage = req.getPage();
		Page page = new Page();
		page.setPageSize(10);
		page.setRealPage(realPage);
		if (b > 0) {
			b *= 1000;
			e *= 1000;
		}
		List<BrandLogModel> room_record = MongoDBServiceImpl.getInstance().getProxyParentBrandListByAccountId(page, accountId, 0, b, e);
		int count = MongoDBServiceImpl.getInstance().getOpenRoomConsume(GameConstants.CREATE_ROOM_PROXY, accountId, 0, b, e);
		int eclusive_count = MongoDBServiceImpl.getInstance().getOpenRoomExclusiveConsume(GameConstants.CREATE_ROOM_PROXY, accountId, 0, b, e);
		return newRecordBuilder(room_record, page, count, eclusive_count, 0);
	}

	/**
	 *
	 */
	private MilitaryExploitsRspProto.Builder club(MilitaryExploitsReqProto req, long accountId) {
		MilitaryExploitsRspProto.Builder builder;
		if (req.getClubId() <= 0) {
			builder = MilitaryExploitsRspProto.newBuilder();
		} else {
			long b = req.getBeginTime();
			long e = req.getEndTime();
			// 分页
			int realPage = req.getPage();
			Page page = new Page();
			page.setPageSize(10);
			page.setRealPage(realPage);
			if (b > 0) {
				b *= 1000;
				e *= 1000;
			}
			List<BrandLogModel> room_record = MongoDBServiceImpl.getInstance().getClubParentBrandList(page, req.getClubId(), b, e);
			int count = MongoDBServiceImpl.getInstance().getOpenRoomConsumeClub(req.getClubId(), accountId, b, e);
			int exclusive_count = MongoDBServiceImpl.getInstance().getOpenRoomExclusiveConsumeClub(req.getClubId(), accountId, b, e);
			builder = newRecordBuilder(room_record, page, count, exclusive_count, 0);
		}
		return builder;
	}

	/**
	 *
	 */
	private MilitaryExploitsRspProto.Builder personal(MilitaryExploitsReqProto req, long accountId) {
		long b = req.getBeginTime();
		long e = req.getEndTime();
		// 分页
		int realPage = req.getPage();
		Page page = new Page();
		page.setPageSize(10);
		page.setRealPage(realPage);
		if (b > 0) {
			b *= 1000;
			e *= 1000;
		}
		List<BrandLogModel> room_record = MongoDBServiceImpl.getInstance().getParentBrandListByAccountIdNew(page, accountId, 0, b, e);
		;
		int count = MongoDBServiceImpl.getInstance().getOpenRoomConsume(-1, accountId, 0, b, e);
		int exclusive_count = MongoDBServiceImpl.getInstance().getOpenRoomExclusiveConsume(-1, accountId, 0, b, e);
		return newRecordBuilder(room_record, page, count, exclusive_count, 0);
	}

	/**
	 * @param brands
	 * @return
	 */
	private static MilitaryExploitsRspProto.Builder newRecordBuilder(final List<BrandLogModel> brands, Page page, int consume, int exclusiveConsume,
			int points) {

		MilitaryExploitsRspProto.Builder builder = MilitaryExploitsRspProto.newBuilder();
		GameRoomRecord grr = null;
		for (BrandLogModel logModel : brands) {
			grr = GameRoomRecord.to_Object(logModel.getMsg());//
			if (grr == null)
				continue;
			int length = grr.getPlayers().length;
			for (int i = 0; i < length; i++) {
				if (i >= grr.getPlayers().length)
					continue;
				if (grr.getPlayers()[i] == null) {
					continue;
				}
			}

			PlayerResult _player_result = grr.get_player();

			PlayerResultResponse.Builder player_result = PlayerResultResponse.newBuilder();

			PlayerResultFLSResponse.Builder playerResultFLSResponse = PlayerResultFLSResponse.newBuilder();

			Player create_player = grr.getCreate_player();
			if (create_player != null) {
				RoomPlayerResponse.Builder room_player = RoomHandler.setPlayerInfo(_player_result, player_result, create_player, length);
				player_result.setCreatePlayer(room_player);
			}
			player_result.setAppId(logModel.getGame_id());
			player_result.setCreatType(logModel.getCreateType());
			if (logModel.getName() != null) {
				player_result.setRecordName(logModel.getName());
			}

			RoomHandler.recorde_common(grr, _player_result, player_result, playerResultFLSResponse, length);
			if (logModel.isRealKouDou()) {
				player_result.setCostGold(logModel.getGold_count());
			}
			player_result.setRandomNum(logModel.getRandomNum());
			builder.addRecords(player_result);
		}
		if (page != null) {
			builder.setPage(page.getRealPage());
			builder.setTotle(page.getTotalSize());
			builder.setRows(page.getPageSize());
		}

		builder.setConsume(consume);
		builder.setExclusiveConsume(exclusiveConsume);
		builder.setGeneralRecord(points);
		return builder;
	}
}
