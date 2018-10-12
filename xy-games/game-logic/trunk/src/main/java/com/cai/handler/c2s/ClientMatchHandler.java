package com.cai.handler.c2s;

import com.cai.common.constant.MatchCmd;
import com.cai.common.constant.S2SCmd;
import com.cai.domain.Session;
import com.cai.manager.MatchTableManager;
import com.cai.match.MatchPlayer;
import com.cai.match.MatchTable;
import com.cai.service.MatchTableService;
import com.cai.util.MsgUtils;
import com.xianyi.framework.core.transport.IServerCmd;
import com.xianyi.framework.handler.IClientHandler;
import protobuf.clazz.match.MatchClientHeaderRsp.MatchClientRequest;
import protobuf.clazz.match.MatchRsp.MatchClientRequestWrap;
import protobuf.clazz.match.MatchRsp.MatchS2SRequest;

@IServerCmd(code = S2SCmd.MATCH_SERVER, desc = "代理服登录")
public class ClientMatchHandler extends IClientHandler<MatchS2SRequest> {

	@Override
	protected void execute(MatchS2SRequest req, Session session) throws Exception {
		switch (req.getCmd()) {
		case S2S_MATCH_CLIENT:
			clientRequest(req.getClientRequest(), session);
			break;
		default:
			break;
		}
	}

	private void clientRequest(MatchClientRequestWrap req, Session session) {
		long accountId = req.getAccountId();
		MatchTable table = MatchTableService.getInstance().getTable(req.getId());
		if (table == null) {
			logger.error("clientRequest -> 找不到玩家{}的比赛{} !!", req.getAccountId(), req.getId());
			return;
		}

		if (table != null) {
			table.addTask(new Runnable() {
				@Override
				public void run() {
					try{
						if (MatchTableService.getInstance().getTable(table.id) == null) {
							return;
						}
						if(table.isAdminId(accountId)){
							onRequest(accountId, table, req.getRequest(), session);
							return;
						}
						
						MatchPlayer matchPlayer = table.getPlayer(accountId);
						if (matchPlayer == null) {
							logger.error("clientRequest-> no player accountId:{} matchId:{} id:{} !!",
									accountId,table.matchId,table.id);
							return;
						}
						onRequest(table, req, matchPlayer, session);
					}catch (Exception e) {
						logger.error("clientRequest-> error accountId:{} matchId:{} id:{} exception",
								accountId,table.matchId,table.id,e);
					}
				}
			});
		}

	}

	private void onRequest(MatchTable table, MatchClientRequestWrap req, MatchPlayer player, Session session) {
		switch (req.getRequest().getCmd()) {
		case MatchCmd.C2S_MATCH_ENTER_MATCH:
			table.enter(req, player, session);
			break;
		case MatchCmd.C2S_MATCH_LEAVE:
			table.leave(player, session);
			break;
		case MatchCmd.MATCH_GMAE_RANK:
			table.rank(player, session);
			break;
		case MatchCmd.MATCH_WINNER_TABLE_LIST:
			table.requestWinnerTable(player.getAccount_id(), session);
			break;
		case MatchCmd.MATCH_TOP_ROUND_RECORD:
			table.requestTopRoundRecord(player, session);
			break;
		}
	}
	
	private void onRequest(long adminId, MatchTable table, MatchClientRequest request, Session session) {
		if(table.isOver()){
			MsgUtils.sendMsg(session, adminId, "该比赛已结束,不允许操作");
			return;
		}
		switch (request.getCmd()) {
		case MatchCmd.C2S_MATCH_ENTER_MATCH:
			table.enterByAdmin(adminId, session);
			break;
		case MatchCmd.MATCH_WINNER_LIST:
			MatchTableManager.INSTANCE().sendWinnerListResp(adminId, table, session);
			break;
		case MatchCmd.MATCH_WINNER_OPERATION:
			MatchTableManager.INSTANCE().handleTableManager(adminId, table, request, session);
			break;
		case MatchCmd.MATCH_WINNER_ALLOCATION:
			MatchTableManager.INSTANCE().handleTableAllocation(adminId, table, request, session);
			break;
		case MatchCmd.MATCH_WINNER_TABLE_LIST:
			table.requestWinnerTableByAdmin(adminId, session);
			break;
		}
	}

}
