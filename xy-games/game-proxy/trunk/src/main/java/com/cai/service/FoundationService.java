package com.cai.service;

import org.apache.commons.lang.math.RandomUtils;
import org.apache.log4j.Logger;

import com.cai.common.constant.ActivityMissionTypeEnum;
import com.cai.common.constant.S2SCmd;
import com.cai.common.util.GlobalExecutor;
import com.cai.common.util.PBUtil;

import protobuf.clazz.Protocol.Request;
import protobuf.clazz.s2s.S2SProto.S2SActivityMissionUpdateProto;

/**
 * 调用游戏基础服类
 * 
 * @author chansonyan 2018年4月20日
 */
public class FoundationService {

	private static Logger logger = Logger.getLogger(FoundationService.class);

	private static final FoundationService service = new FoundationService();

	/**
	 * 完成一次活动任务 避免基础服的压力，异步处理
	 * 
	 * @param accountId
	 * @param typeEnum
	 * @param targetCondition
	 *            目标指标
	 * @param count
	 */
	public void sendActivityMissionProcess(long accountId, ActivityMissionTypeEnum typeEnum, int targetCondition, int count) {
		final S2SActivityMissionUpdateProto.Builder s2sActivityMissionUpdateBuild = S2SActivityMissionUpdateProto.newBuilder();
		s2sActivityMissionUpdateBuild.setAccountId(accountId);
		s2sActivityMissionUpdateBuild.setCondition(targetCondition);
		s2sActivityMissionUpdateBuild.setCurrentCount(count);
		s2sActivityMissionUpdateBuild.setMissionTypeEnum(typeEnum.getId());
		Request.Builder builder = PBUtil.toS2SRequet(S2SCmd.FOUNDATION_ACTIVITYMISSION_UPDATE, s2sActivityMissionUpdateBuild);
		GlobalExecutor.schedule(() -> {
			ClientServiceImpl.getInstance().sendToFoundation(builder.build());
		}, RandomUtils.nextInt(2000));
		logger.debug("FoundationService-->sendActivityMissionProcess:" + typeEnum);
		;
	}

	public void sendInviteFriendActivityMissionProcess(long accountId, long recommendId) {
		final S2SActivityMissionUpdateProto.Builder s2sActivityMissionUpdateBuild = S2SActivityMissionUpdateProto.newBuilder();
		s2sActivityMissionUpdateBuild.setAccountId(recommendId);
		s2sActivityMissionUpdateBuild.setBeRecommendId(accountId);
		s2sActivityMissionUpdateBuild.setMissionTypeEnum(ActivityMissionTypeEnum.INVITE_RECOMMEND.getId());
		Request.Builder builder = PBUtil.toS2SRequet(S2SCmd.FOUNDATION_ACTIVITYMISSION_UPDATE, s2sActivityMissionUpdateBuild);
		GlobalExecutor.schedule(() -> {
			ClientServiceImpl.getInstance().sendToFoundation(builder.build());
		}, RandomUtils.nextInt(2000));
		logger.debug("FoundationService-->sendActivityMissionProcess:" + ActivityMissionTypeEnum.INVITE_RECOMMEND.getId());
		;
	}

	public static final FoundationService getInstance() {
		return service;
	}

}