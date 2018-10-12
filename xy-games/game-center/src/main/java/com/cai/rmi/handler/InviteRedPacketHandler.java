package com.cai.rmi.handler;

import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import com.cai.common.constant.RMICmd;
import com.cai.common.define.EAccountParamType;
import com.cai.common.domain.Account;
import com.cai.common.domain.AccountParamModel;
import com.cai.common.domain.InviteActiveModel;
import com.cai.common.domain.InviteMoneyModel;
import com.cai.common.domain.InviteRedPacketModel;
import com.cai.common.rmi.IRMIHandler;
import com.cai.common.rmi.IRmi;
import com.cai.common.util.SpringService;
import com.cai.dictionary.InviteActiveDict;
import com.cai.service.MongoDBService;
import com.cai.service.MongoDBServiceImpl;
import com.cai.service.PublicServiceImpl;
import com.mongodb.WriteResult;

/**
 * 
 * 
 *
 * @author tang date: 2018年01月24日 上午10:11:20 <br/>
 */
@IRmi(cmd = RMICmd.INVITE_REDPACKET_HANDLER, desc = "邀请红包真实发放处理")
public final class InviteRedPacketHandler extends IRMIHandler<Map<String, String>, Integer> {
	private int ERROR = -1;
	private int SUCCESS = 1;

	@Override
	public Integer execute(Map<String, String> map) {
		String accountId = map.get("account_id");
		if (StringUtils.isBlank(accountId)) {
			return ERROR;
		}
		InviteActiveModel inviteActiveModel = InviteActiveDict.getInstance().getInviteActiveModel();
		if (inviteActiveModel == null || inviteActiveModel.getId() == 0) {
			return ERROR;
		}
		long nowTime = System.currentTimeMillis();
		long startTime = inviteActiveModel.getBegin_time().getTime();
		long endTime = inviteActiveModel.getEnd_time().getTime();
		// 活动未开始
		if (nowTime < startTime || nowTime > endTime) {
			return ERROR;
		}
		Long account_id = Long.parseLong(accountId);
		Account account = PublicServiceImpl.getInstance().getAccount(account_id);
		if (account == null) {
			return ERROR;
		}
		// 没有推荐人，不处理
		if (account.getAccountModel().getRecommend_id() == 0) {
			return ERROR;
		}
		// 用户注册时间也要在这个范围内
		long registerTime = account.getAccountModel().getCreate_time().getTime();
		if (registerTime < startTime || registerTime > endTime) {
			return ERROR;
		}
		// 注册时间需要大于24小时的倍数
		if ((registerTime + inviteActiveModel.getTime_limit() * 24 * 60 * 60000) > nowTime) {
			return ERROR;
		}
		// paramModel16 是否享受过推荐人获豆
		AccountParamModel model16 = account.getAccountParamModelMap().get(EAccountParamType.ADD_RECOMMEND_GOLD.getId());
		// long1可以用来标识自己是否领取过被推荐的奖励
		if (model16 == null || model16.getVal1() != 1 || (StringUtils.isNotBlank(model16.getStr1()) && model16.getStr1().equals("1"))) {
			return ERROR;
		}
		// paramModel15 局数判断局数是否达到要求
		AccountParamModel model15 = account.getAccountParamModelMap().get(EAccountParamType.TOTAL_ROUND.getId());
		 if(model15==null||model15.getVal1()<inviteActiveModel.getBrand_limit()){
		 return ERROR;
		 }
		// 先给推荐人发送红包，如果推荐人也是最新被推荐的，并且没领过自身的福利包，则推荐人再发一个福利包
		InviteMoneyModel moneyModel = new InviteMoneyModel(account.getAccountModel().getRecommend_id(), account.getAccount_id(), 0,
				inviteActiveModel.getInvite_pay(), inviteActiveModel.getId(), 0);
		// 标识已经领取过推荐奖励
		model16.setStr1("1");
		model16.setNeedDB(true);
		long effectiveInviteCounts = MongoDBServiceImpl.getInstance().getEffectiveInvitePersonsCount(account.getAccountModel().getRecommend_id());
		int total = InviteActiveDict.getInstance().getTotalInviteCounts().intValue();
		//判断有效邀请人数跟红包总数有没有超过限制
		if(total<=inviteActiveModel.getTotal_redpacket()&&effectiveInviteCounts<inviteActiveModel.getMax_limit()){
			MongoDBServiceImpl.getInstance().getLogQueue().add(moneyModel);
			InviteActiveDict.getInstance().getTotalInviteCounts().getAndIncrement();
			total += 1;
		}
		updateInviteRedPacketModel(account.getAccount_id());
		Account recomAccount = PublicServiceImpl.getInstance().getAccount(account.getAccountModel().getRecommend_id());
		if (recomAccount == null) {
			return SUCCESS;
		}
		AccountParamModel recomModel16 = PublicServiceImpl.getInstance().getAccountParamModel(account.getAccountModel().getRecommend_id(),EAccountParamType.ADD_RECOMMEND_GOLD);
		// long1可以用来标识自己是否领取过被推荐的奖励,只在活动内的才送红包
		if (recomModel16!=null&&recomModel16.getLong1() == 0&& recomAccount.getAccountModel().getRecommend_id()>0) {
			long recomCreateTime = recomAccount.getAccountModel().getCreate_time().getTime();
			if (recomCreateTime >= startTime && recomCreateTime <= endTime) {
				//最大邀请送红包人数不能超过限制,并且红包总个数不能超过设定
				if(total<inviteActiveModel.getTotal_redpacket()&&effectiveInviteCounts<inviteActiveModel.getMax_limit()){
					InviteMoneyModel moneyModel2 = new InviteMoneyModel(account.getAccountModel().getRecommend_id(), 0, 0,
							inviteActiveModel.getPay(), inviteActiveModel.getId(), 1);
					MongoDBServiceImpl.getInstance().getLogQueue().add(moneyModel2);
					InviteActiveDict.getInstance().getTotalInviteCounts().getAndIncrement();
				}
			}
			recomModel16.setLong1(1L);
			recomModel16.setNeedDB(true);
		}
		// 如果这些条件满足，则给玩家推荐人送红包
		return SUCCESS;
	}

	// 修改推荐用户成功领取红包状态
	public void updateInviteRedPacketModel(long accountId) {
		Query query = new Query();
		query.addCriteria(Criteria.where("target_account_id").is(accountId));
		Update update = Update.update("state", 1);
		MongoDBService mongoDBService = SpringService.getBean(MongoDBService.class);
		try{
			mongoDBService.getMongoTemplate().updateFirst(query, update, InviteRedPacketModel.class);
		}catch(Exception e){
			logger.error("邀请的用户不存在"+accountId,e);
		}
	}
}
