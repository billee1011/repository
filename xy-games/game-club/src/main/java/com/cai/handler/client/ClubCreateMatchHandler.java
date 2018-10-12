/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.handler.client;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import com.cai.common.config.ExclusiveGoldCfg;
import com.cai.common.constant.C2SCmd;
import com.cai.common.constant.RMICmd;
import com.cai.common.constant.S2CCmd;
import com.cai.common.constant.S2SCmd;
import com.cai.common.constant.Symbol;
import com.cai.common.define.EGoldOperateType;
import com.cai.common.define.ESysMsgType;
import com.cai.common.domain.AddGoldResultModel;
import com.cai.common.domain.ClubMatchModel;
import com.cai.common.domain.ClubMemberModel;
import com.cai.common.domain.ClubRuleModel;
import com.cai.common.rmi.ICenterRMIServer;
import com.cai.common.rmi.vo.ClubExclusiveRMIVo;
import com.cai.common.util.PBUtil;
import com.cai.common.util.RoomComonUtil;
import com.cai.common.util.SpringService;
import com.cai.common.util.StringUtil;
import com.cai.common.util.TimeUtil;
import com.cai.config.ClubCfg;
import com.cai.constant.Club;
import com.cai.constant.ClubMatchFactory;
import com.cai.constant.ClubMatchOpType;
import com.cai.constant.ClubMatchOpenType;
import com.cai.constant.ClubMatchType;
import com.cai.constant.ClubMatchWrap;
import com.cai.constant.ClubMatchWrap.ClubMatchStatus;
import com.cai.constant.EClubIdentity;
import com.cai.dictionary.SysGameTypeDict;
import com.cai.service.ClubDaoService;
import com.cai.service.ClubService;
import com.cai.service.MongoDBServiceImpl;
import com.cai.service.SessionService;
import com.cai.utils.ClubMatchCostUtil;
import com.cai.utils.ClubRoomUtil;
import com.cai.utils.Utils;
import com.google.common.base.Strings;
import com.googlecode.protobuf.format.JsonFormat;
import com.xianyi.framework.core.transport.ICmd;
import com.xianyi.framework.core.transport.netty.session.C2SSession;
import com.xianyi.framework.handler.IClientExHandler;

import protobuf.clazz.ClubMsgProto.ClubMatchInfoProto;
import protobuf.clazz.ClubMsgProto.ClubMatchOperateResultResponse;
import protobuf.clazz.ClubMsgProto.ClubRuleProto;
import protobuf.clazz.Common.CommonII;
import protobuf.clazz.Common.CommonILI;
import protobuf.clazz.s2s.S2SProto.ClubMatchStartFailSendMailProto;
import protobuf.clazz.s2s.S2SProto.S2STransmitProto;
import protobuf.clazz.s2s.S2SProto.TransmitProto;

/**
 * @author wu_hc date: 2018年6月21日 下午8:54:34 <br/>
 */
@ICmd(code = C2SCmd.CLUB_CREATE_OR_DEL_MATCH, desc = "创建or删除or修改亲友圈比赛")
public final class ClubCreateMatchHandler extends IClientExHandler<ClubMatchInfoProto> {

	@Override
	protected void execute(ClubMatchInfoProto req, TransmitProto topReq, C2SSession session) throws Exception {

		Club club = ClubService.getInstance().getClub(req.getClubId());
		if (null == club) {
			return;
		}
		club.runInReqLoop(() -> {
			ClubMemberModel member = club.members.get(topReq.getAccountId());
			if (null == member) {
				return;
			}
			if (!EClubIdentity.isManager(member.getIdentity())) {
				Utils.sendTip(topReq.getAccountId(), "权限不够，操作无效！", ESysMsgType.INCLUDE_ERROR, session);
				return;
			}
			ClubMatchOperateResultResponse.Builder resultBuilder = ClubMatchOperateResultResponse.newBuilder();
			resultBuilder.setClubId(req.getClubId());
			resultBuilder.setOpType(req.getOpType());
			boolean result = false;
			String msg = "";
			ClubMatchWrap wrap = null;
			if (req.getOpType() == ClubMatchOpType.CREATE) {
				msg = checkCanCreateMatch(req, club);
				if (!msg.equals("")) {
					resultBuilder.setIsSuccess(result);
					resultBuilder.setMsg(msg);
					session.send(PBUtil.toS_S2CRequet(topReq.getAccountId(), S2CCmd.CLUB_CREATE_OR_DEL_MATCH_RESULT_RSP, resultBuilder));
					return;
				}
				CostResult costResult = costGold(req, club);
				if (!costResult.isSuccess) {
					// 扣豆失败
					resultBuilder.setIsSuccess(result);
					resultBuilder.setMsg("亲友圈创始人豆不足，扣豆失败！");
					resultBuilder.setRet(1);
					session.send(PBUtil.toS_S2CRequet(topReq.getAccountId(), S2CCmd.CLUB_CREATE_OR_DEL_MATCH_RESULT_RSP, resultBuilder));
					return;
				}
				ClubMatchModel model = createClubMatchModel(topReq.getAccountId(), req);
				StringBuilder costBuffer = new StringBuilder();
				costBuffer.append(costResult.isExclusive ? 1 : 0);
				costBuffer.append(",").append(costResult.costNum);
				costBuffer.append(",").append(costResult.singleCostNum);
				model.setCostGold(costBuffer.toString());
				model.setStatus(ClubMatchStatus.PRE.status());

				// save to db
				SpringService.getBean(ClubDaoService.class).getDao().insertClubMatchModel(model);

				wrap = ClubMatchFactory.createClubMatchWrap(req.getOpenMatchType(), model, club);
//				if (req.getOpenMatchType() == ClubMatchOpenType.TIME_MODE) {
//					wrap = new ClubMatchTimeWrap(model, club);
//				} else if (req.getOpenMatchType() == ClubMatchOpenType.COUNT_MODE) {
//					wrap = new ClubMatchCountWrap(model, club);
//				}

				club.matchs.put(wrap.id(), wrap);
				result = true;
				msg = "创建成功";
				resultBuilder.setMatchId(wrap.id());
				resultBuilder.setMatchType(req.getMatchType());

				club.addMatchCreateNum();
				MongoDBServiceImpl.getInstance().logClubCreateMatch(wrap);

			} else if (req.getOpType() == ClubMatchOpType.DEL) {
				wrap = club.matchs.get(req.getId());
				if (wrap == null) {
					Utils.sendTip(topReq.getAccountId(), "该比赛不存在！", ESysMsgType.INCLUDE_ERROR, session);
					return;
				}
				// 判断比赛状态
				ClubMatchModel model = wrap.getModel();
				if (model.getStatus() == ClubMatchStatus.ING.status()) {
					Utils.sendTip(topReq.getAccountId(), "该比赛已开始！", ESysMsgType.INCLUDE_ERROR, session);
					return;
				}
				int oldStatus = model.getStatus();
				wrap.cancelSchule(true);

				model.setStatus(ClubMatchStatus.CANCEL.status());
				club.matchs.remove(req.getId());
				club.delMatchs.put(wrap.id(), wrap);

				if (oldStatus == ClubMatchStatus.PRE.status()) { // 还未开始的比赛删除时需要还豆
					wrap.sendBackGold();
					// 向参赛成员发邮件通知
					ClubMatchStartFailSendMailProto.Builder b = ClubMatchStartFailSendMailProto.newBuilder();
					b.setType(2);
					b.addAllPlayerIds(wrap.enrollAccountIds());
					b.setMatchName(model.getMatchName());
					b.setClubName(club.getClubName());
					SessionService.getInstance().sendGate(1, PBUtil.toS2SRequet(S2SCmd.S_2_M, S2STransmitProto.newBuilder().setAccountId(0)
							.setRequest(PBUtil.toS2SResponse(S2SCmd.CLUB_MATCH_START_FAIL_TO_MATCH_SERVER, b))).build());
				}

				result = true;
				msg = "删除成功";
				resultBuilder.setMatchId(req.getId());

			} else if (req.getOpType() == ClubMatchOpType.UPDATE) {
				wrap = club.matchs.get(req.getId());
				msg = checkCanUpdateMatch(req, wrap);
				if (!msg.equals("")) {
					resultBuilder.setIsSuccess(result);
					resultBuilder.setMsg(msg);
					session.send(PBUtil.toS_S2CRequet(topReq.getAccountId(), S2CCmd.CLUB_CREATE_OR_DEL_MATCH_RESULT_RSP, resultBuilder));
					return;
				}
				// 扣豆还豆检查
				if (req.hasMaxPlayerCount()) {
					int addPlayerNum = req.getMaxPlayerCount() - wrap.getModel().getMaxPlayerCount();
					int tablePlayerNum = RoomComonUtil.getMaxNumber(wrap.ruleModel().getRuleParams());
					int addTableNum = addPlayerNum / tablePlayerNum;
					CostResult replenishResult = replenishGold(addTableNum, wrap, req, club);
					if (!replenishResult.isSuccess) {
						String douMsg = replenishResult.isExclusive ? "专属豆" : "豆";
						if (addTableNum > 0) {
							msg = douMsg + "不足（修改与创建需同一类型豆），无法进行补扣，修改失败";
						} else {
							msg = douMsg + "补还" + douMsg + "失败";
						}
						resultBuilder.setIsSuccess(result);
						resultBuilder.setMsg(msg);
						session.send(PBUtil.toS_S2CRequet(topReq.getAccountId(), S2CCmd.CLUB_CREATE_OR_DEL_MATCH_RESULT_RSP, resultBuilder));
						return;
					}
					String costGold = wrap.getModel().getCostGold();
					List<Integer> list = StringUtil.toIntList(costGold, Symbol.COMMA);
					if (list != null && list.size() >= 2) {
						StringBuilder costBuffer = new StringBuilder();
						costBuffer.append(list.get(0));
						costBuffer.append("," + (list.get(1) + replenishResult.costNum));
						if (list.size() > 2) {
							costBuffer.append("," + list.get(2));
						}

						wrap.getModel().setCostGold(costBuffer.toString());
					}
					String douMsg = replenishResult.isExclusive ? "专属豆" : "豆";
					if (addTableNum == 0) {
						msg = "修改成功";
					} else if (replenishResult.costNum > 0) {
						msg = "成功修改比赛，补充扣除" + replenishResult.costNum + douMsg;
					} else {
						msg = "成功修改比赛，退还" + (-replenishResult.costNum) + douMsg;
					}
				}
				updateClubMatchModel(wrap, req);
				wrap.updateMatch();
				result = true;
				resultBuilder.setMatchId(req.getId());

				if (wrap.getModel().getOpenType() == ClubMatchOpenType.COUNT_MODE) {
					if (wrap.getEnrollAccountIds().size() == wrap.getModel().getMaxPlayerCount()) { //修改了最大开赛人数满人赛需要检查是否可开赛
						wrap.start();
					}
				}
			}

			// send to client
			resultBuilder.setIsSuccess(result);
			resultBuilder.setMsg(msg);
			session.send(PBUtil.toS_S2CRequet(topReq.getAccountId(), S2CCmd.CLUB_CREATE_OR_DEL_MATCH_RESULT_RSP, resultBuilder));

			Utils.notifyClubMatchEvent(topReq.getAccountId(), club, wrap.id(), req.getOpType());
		});
	}

	private String checkCanCreateMatch(ClubMatchInfoProto req, Club club) {
		String msg = "";
		if (req.getOpenMatchType() != ClubMatchOpenType.TIME_MODE && req.getOpenMatchType() != ClubMatchOpenType.COUNT_MODE) {
			msg = "开赛类型非法！";
			return msg;
		}
		if (req.getMatchType() == ClubMatchType.MANAGER_SET && req.getOpenMatchType() == ClubMatchOpenType.COUNT_MODE) {
			msg = "参赛方式为成员自主报名才可选择满人开赛！";
			return msg;
		}
		if (club.matchs.size() >= 5) {
			msg = "比赛最多同时配置5个！";
			return msg;
		}
		msg = checkMatchPlayerCount(req);
		return msg;
	}

	private String checkMatchPlayerCount(ClubMatchInfoProto req) {
		String msg = "";
		if (req.getMaxPlayerCount() <= 0) {
			msg = "开赛人数不符合！";
			return msg;
		}
		if (req.getMaxPlayerCount() > ClubCfg.get().getClubMemberMax()) {
			msg = "开赛人数超过亲友圈人数上限！";
			return msg;
		}

		ClubRuleModel clubRuleModel = new ClubRuleModel();
		ClubRuleProto ruleProto = req.getRule();
		clubRuleModel.setGame_type_index(ruleProto.getGameTypeIndex());
		clubRuleModel.setRules(ruleProto.getRules());
		clubRuleModel.setGame_round(ruleProto.getGameRound());
		clubRuleModel.init();
		int tablePlayerNum = RoomComonUtil.getMaxNumber(clubRuleModel.getRuleParams());
		if (req.getMaxPlayerCount() % tablePlayerNum != 0) {
			msg = "开赛人数不是单桌人数的整数倍！";
			return msg;
		}
		if (req.getOpenMatchType() == ClubMatchOpenType.TIME_MODE) {
			if (req.getStartDate() * 1000L < System.currentTimeMillis() + (ClubCfg.get().getClubMatchMinStartMinute() - 1) * TimeUtil.MINUTE) {
				msg = "开赛时间要大于当前时间" + ClubCfg.get().getClubMatchMinStartMinute() + "分钟！";
				return msg;
			}
			if (req.getMinPlayerCount() % tablePlayerNum != 0) {
				msg = "最小开赛人数不是单桌人数的整数倍！";
				return msg;
			}
			if (req.getMinPlayerCount() > ClubCfg.get().getClubMemberMax()) {
				msg = "最小开赛人数超过亲友圈人数上限！";
				return msg;
			}
			if (req.hasMinPlayerCount() && req.getMinPlayerCount() <= 0) {
				msg = "最小开赛人数不符合！";
				return msg;
			}
		}
		return msg;
	}

	private String checkCanUpdateMatch(ClubMatchInfoProto req, ClubMatchWrap wrap) {
		String msg = "";
		if (wrap == null) {
			msg = "该比赛不存在！";
			return msg;
		}
		// 判断比赛状态
		ClubMatchModel model = wrap.getModel();
		if (model.getStatus() != ClubMatchStatus.PRE.status()) {
			msg = "该比赛已开始,不能修改！";
			return msg;
		}
		if (req.getMaxPlayerCount() < wrap.getEnrollAccountIds().size()) {
			msg = "比赛人数不能小于当前已报名的玩家数！";
			return msg;
		}
		int rewardSize = 0;
		List<CommonII> sets = req.getRewardsList();
		if (sets != null && sets.size() > 0) {
			rewardSize = sets.size();
		} else {
			rewardSize = wrap.reward.size();
		}
		if (req.getMaxPlayerCount() < rewardSize) {
			msg = "奖励设置人数必须小于或等于比赛人数！";
			return msg;
		}
		msg = checkMatchPlayerCount(req);
		return msg;
	}

	private ClubMatchModel createClubMatchModel(long accountId, ClubMatchInfoProto req) {
		ClubMatchModel model = new ClubMatchModel();
		model.setClubId(req.getClubId());
		model.setCreatorId(accountId);
		model.setMatchName(req.getMatchName());
		model.setMatchType(req.getMatchType());
		model.setStartDate(new Date(req.getStartDate() * 1000L));
		model.setMaxPlayerCount(req.getMaxPlayerCount());
		model.setOpenType(req.getOpenMatchType());
		if (req.getMatchType() == ClubMatchType.MEMBER_ATTEND) {
			model.setAttendCondition(req.getAttendCondition());
			model.setConditionValue(req.getConditionValue());
		}
		List<CommonII> sets = req.getRewardsList();
		if (null != sets && !sets.isEmpty()) {
			StringBuilder rewardBuffer = new StringBuilder();
			for (CommonII proto : sets) {
				rewardBuffer.append(proto.getV()).append(",");
			}
			if (rewardBuffer.length() > 0) {
				rewardBuffer.deleteCharAt(rewardBuffer.length() - 1);
			}
			model.setReward(rewardBuffer.toString());
		}
		model.setGameRuleJson(JsonFormat.printToString(req.getRule()));
		if (req.getOpenMatchType() == ClubMatchOpenType.TIME_MODE) {
			model.setMinPlayerCount(req.getMinPlayerCount());
		}
		if (req.getOpenMatchType() == ClubMatchOpenType.COUNT_MODE) {
			model.setStartDate(new Date());
		}
		return model;
	}

	private void updateClubMatchModel(ClubMatchWrap wrap, ClubMatchInfoProto req) {
		ClubMatchModel model = wrap.getModel();
		model.setMatchName(req.getMatchName());
		model.setMaxPlayerCount(req.getMaxPlayerCount());
		model.setMinPlayerCount(req.getMinPlayerCount());
		model.setStartDate(new Date(req.getStartDate() * 1000L));
		List<CommonII> sets = req.getRewardsList();
		if (null != sets && !sets.isEmpty()) {
			StringBuilder rewardBuffer = new StringBuilder();
			for (CommonII proto : sets) {
				rewardBuffer.append(proto.getV()).append(",");
			}
			if (rewardBuffer.length() > 0) {
				rewardBuffer.deleteCharAt(rewardBuffer.length() - 1);
			}
			model.setReward(rewardBuffer.toString());
		}
		if (req.getOpenMatchType() == ClubMatchOpenType.TIME_MODE) {
			model.setMinPlayerCount(req.getMinPlayerCount());
		}
	}

	/**
	 * 修改比赛后补充扣还豆
	 */
	private CostResult replenishGold(int addTableNum, ClubMatchWrap wrap, ClubMatchInfoProto req, Club club) {
		CostResult costResult = new CostResult();
		costResult.isSuccess = false;
		if (addTableNum == 0) {
			costResult.isSuccess = true;
			return costResult;
		}
		String costGold = wrap.getModel().getCostGold();
		if (Strings.isNullOrEmpty(costGold)) {
			logger.error("亲友圈修改自建赛失败,没有扣豆数据,clubId={},matchId={}", club.getClubId(), wrap.id());
			return costResult;
		}

		List<Integer> list = StringUtil.toIntList(costGold, Symbol.COMMA);
		if (list == null || list.size() < 2) {
			logger.error("亲友圈修改自建赛失败,扣豆数据不全,clubId={},matchId={}", club.getClubId(), wrap.id());
			return costResult;
		}
		boolean isExclusive = list.get(0) == 1;
		costResult.isExclusive = isExclusive;
		ClubRuleProto ruleProto = req.getRule();
		int singleCost = 0;
		if (list.size() > 2) {
			singleCost = list.get(2);
		} else {
			singleCost = ClubMatchCostUtil.finalCost(ruleProto.getGameTypeIndex(), ruleProto.getGameRound(), club.getMemberCount(),
					wrap.ruleModel().getRuleParams().getMap());
		}
		int changeGold = addTableNum * singleCost;
		int gameId = SysGameTypeDict.getInstance().getGameIDByTypeIndex(ruleProto.getGameTypeIndex());
		int game_type_index = ruleProto.getGameTypeIndex();
		long createAccountId = club.getOwnerId();
		StringBuilder buf = new StringBuilder();
		buf.append("修改亲友圈比赛:clubId:").append(club.getClubId()).append("game_id:").append(gameId).append(",game_type_index:")
				.append(ruleProto.getGameTypeIndex()).append(",game_round:").append(ruleProto.getGameRound());
		if (isExclusive) {
			ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
			if (changeGold > 0) { //补充扣豆
				if (ExclusiveGoldCfg.get().isUseExclusiveGold()) {
					ClubExclusiveRMIVo vo = ClubExclusiveRMIVo.newVo(createAccountId, gameId, changeGold, EGoldOperateType.OPEN_CLUB_MATCH)
							.setDesc(buf.toString()).setGameTypeIndex(game_type_index).setClubId(club.getClubId());
					AddGoldResultModel addGoldResult = centerRMIServer.rmiInvoke(RMICmd.CLUB_EXCLUSIVE_COST, vo);
					if (null != addGoldResult && addGoldResult.isSuccess()) {
						Object attament = addGoldResult.getAttament();
						if (attament instanceof CommonILI) {
							Utils.sendExclusiveGoldUpdate(createAccountId, Arrays.asList((CommonILI) attament));
						}
						logger.warn("自建赛修改比赛后专属豆补扣豆成功,clubId={},matchId={},gameId={},gameTypeIndex={},cost={}", club.getClubId(), wrap.id(), gameId,
								game_type_index, changeGold);
						costResult.isSuccess = true;
						costResult.costNum = changeGold;
						return costResult;
					}
				}
				logger.warn("自建赛修改比赛后专属豆补扣豆失败,clubId={},matchId={},gameId={},gameTypeIndex={},cost={}", club.getClubId(), wrap.id(), gameId,
						game_type_index, changeGold);
				costResult.costNum = changeGold;
				return costResult;
			} else { // 补还豆
				ClubExclusiveRMIVo vo = ClubExclusiveRMIVo.newVo(club.getOwnerId(), gameId, -changeGold, EGoldOperateType.CLUB_MATCH_FAILED)
						.setGameTypeIndex(ruleProto.getGameTypeIndex()).setClubId(club.getClubId());
				vo.setDesc(buf.toString());
				AddGoldResultModel addresult = centerRMIServer.rmiInvoke(RMICmd.CLUB_EXCLUSIVE_REPAY, vo);
				if (null != addresult && addresult.isSuccess()) {
					Object attament = addresult.getAttament();
					if (attament instanceof CommonILI) {
						Utils.sendExclusiveGoldUpdate(club.getOwnerId(), Arrays.asList((CommonILI) attament));
					}
					logger.warn("自建赛修改比赛后补还专属豆成功,clubId={},matchId={},gameId={},gameTypeIndex={},cost={}", club.getClubId(), wrap.id(), gameId,
							game_type_index, changeGold);
					costResult.isSuccess = true;
					costResult.costNum = changeGold;
					return costResult;
				}
				logger.warn("自建赛修改比赛后补还专属豆失败,clubId={},matchId={},gameId={},gameTypeIndex={},cost={}", club.getClubId(), wrap.id(), gameId,
						game_type_index, changeGold);
				costResult.costNum = changeGold;
				return costResult;
			}

		} else {
			if (changeGold > 0) {
				AddGoldResultModel addGoldResult = ClubRoomUtil
						.subGold(createAccountId, changeGold, false, buf.toString(), EGoldOperateType.OPEN_CLUB_MATCH);
				if (addGoldResult != null && addGoldResult.isSuccess()) {
					logger.warn("修改自建赛闲逸豆补扣豆成功,clubId={},gameId={},gameTypeIndex={},cost={}", club.getClubId(), gameId, ruleProto.getGameTypeIndex(),
							changeGold);
					costResult.isSuccess = true;
					costResult.costNum = changeGold;
					return costResult;
				}
				logger.warn("修改自建赛闲逸豆补扣豆失败,clubId={},gameId={},gameTypeIndex={},cost={}", club.getClubId(), gameId, ruleProto.getGameTypeIndex(),
						changeGold);
				costResult.costNum = changeGold;
				return costResult;
			} else {
				AddGoldResultModel addresult = ClubRoomUtil
						.addGold(club.getOwnerId(), -changeGold, false, buf.toString(), EGoldOperateType.CLUB_MATCH_FAILED);
				if (addresult.isSuccess()) {
					logger.warn("修改自建赛补还闲逸豆成功,clubId={},matchId={},gameId={},gameTypeIndex={},cost={}", club.getClubId(), wrap.id(), gameId,
							game_type_index, -changeGold);
					costResult.isSuccess = true;
					costResult.costNum = changeGold;
					return costResult;
				}
				logger.warn("修改自建赛补还闲逸豆失败,clubId={},matchId={},gameId={},gameTypeIndex={},cost={}", club.getClubId(), wrap.id(), gameId,
						game_type_index, -changeGold);
				costResult.costNum = changeGold;
				return costResult;
			}
		}
	}

	private CostResult costGold(ClubMatchInfoProto req, Club club) {
		CostResult costResult = new CostResult();
		costResult.isSuccess = false;
		costResult.isExclusive = false;

		ClubRuleModel clubRuleModel = new ClubRuleModel();
		ClubRuleProto ruleProto = req.getRule();
		clubRuleModel.setGame_type_index(ruleProto.getGameTypeIndex());
		clubRuleModel.setRules(ruleProto.getRules());
		clubRuleModel.setGame_round(ruleProto.getGameRound());
		clubRuleModel.init();
		int gameId = SysGameTypeDict.getInstance().getGameIDByTypeIndex(ruleProto.getGameTypeIndex());
		int tablePlayerNum = RoomComonUtil.getMaxNumber(clubRuleModel.getRuleParams());
		int singleCost = ClubMatchCostUtil
				.finalCost(ruleProto.getGameTypeIndex(), ruleProto.getGameRound(), club.getMemberCount(), clubRuleModel.getRuleParams().getMap());
		int costMoney = singleCost * (req.getMaxPlayerCount() / tablePlayerNum);
		if (costMoney == 0) {
			costResult.isSuccess = true;
			return costResult;
		}

		long createAccountId = club.getOwnerId();
		StringBuilder buf = new StringBuilder();
		buf.append("创建亲友圈比赛:clubId:").append(club.getClubId()).append("game_id:").append(gameId).append(",game_type_index:")
				.append(ruleProto.getGameTypeIndex()).append(",game_round:").append(ruleProto.getGameRound());
		// 1 如果是俱乐部开的房间,优先判断俱乐部专属豆
		if (ExclusiveGoldCfg.get().isUseExclusiveGold()) {
			ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
			ClubExclusiveRMIVo vo = ClubExclusiveRMIVo.newVo(createAccountId, gameId, costMoney, EGoldOperateType.OPEN_CLUB_MATCH)
					.setDesc(buf.toString()).setGameTypeIndex(ruleProto.getGameTypeIndex()).setClubId(club.getClubId());

			AddGoldResultModel addGoldResult = centerRMIServer.rmiInvoke(RMICmd.CLUB_EXCLUSIVE_COST, vo);
			if (null != addGoldResult && addGoldResult.isSuccess()) {
				Object attament = addGoldResult.getAttament();
				if (attament instanceof CommonILI) {
					Utils.sendExclusiveGoldUpdate(createAccountId, Arrays.asList((CommonILI) attament));
				}
				costResult.isSuccess = true;
				costResult.isExclusive = true;
				costResult.costNum = costMoney;
				costResult.singleCostNum = singleCost;
				logger.warn("新建自建赛专属豆扣豆成功,clubId={},gameId={},gameTypeIndex={},cost={}", club.getClubId(), gameId, ruleProto.getGameTypeIndex(),
						costMoney);
				return costResult;
			}
		}
		// 2俱乐部专属豆不够，用房卡支付
		AddGoldResultModel addGoldResult = ClubRoomUtil.subGold(createAccountId, costMoney, false, buf.toString(), EGoldOperateType.OPEN_CLUB_MATCH);
		if (addGoldResult != null && addGoldResult.isSuccess()) {
			costResult.isSuccess = true;
			costResult.costNum = costMoney;
			costResult.singleCostNum = singleCost;
			logger.warn("新建自建赛闲逸豆扣豆成功,clubId={},gameId={},gameTypeIndex={},cost={}", club.getClubId(), gameId, ruleProto.getGameTypeIndex(),
					costMoney);
			return costResult;
		}
		logger.warn("新建自建赛扣豆失败,clubId={},gameId={},gameTypeIndex={},cost={}", club.getClubId(), gameId, ruleProto.getGameTypeIndex(), costMoney);
		return costResult;
	}

	class CostResult {
		boolean isSuccess;
		boolean isExclusive;
		int costNum;
		int singleCostNum;
	}
}
