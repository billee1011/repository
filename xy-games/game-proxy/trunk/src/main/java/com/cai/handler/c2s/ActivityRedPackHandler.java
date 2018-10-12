/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.handler.c2s;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.remoting.rmi.RmiBasedExporter;

import com.cai.common.config.struct.CenterRmiStruct;
import com.cai.common.constant.C2SCmd;
import com.cai.common.constant.S2CCmd;
import com.cai.common.constant.SysParamEnum;
import com.cai.common.define.EGameType;
import com.cai.common.domain.Account;
import com.cai.common.domain.AppItem;
import com.cai.common.domain.Player;
import com.cai.common.domain.RedActivityModel;
import com.cai.common.domain.RedPackageModel;
import com.cai.common.domain.RedPackageRankModel;
import com.cai.common.domain.RedPackageRecordModel;
import com.cai.common.domain.SysParamModel;
import com.cai.common.rmi.ICenterRMIServer;
import com.cai.common.util.PBUtil;
import com.cai.common.util.SpringService;
import com.cai.core.GbCdCtrl;
import com.cai.core.GbCdCtrl.Opt;
import com.cai.dictionary.AppItemDict;
import com.cai.dictionary.SysParamDict;
import com.cai.dictionary.SysParamServerDict;
import com.cai.module.LoginModule;
import com.cai.service.MongoDBServiceImpl;
import com.cai.service.PlayerServiceImpl;
import com.cai.service.RankService;
import com.xianyi.framework.core.transport.ICmd;
import com.xianyi.framework.core.transport.netty.session.C2SSession;
import com.xianyi.framework.handler.IClientHandler;

import javolution.util.FastMap;
import protobuf.clazz.Protocol.Request;
import protobuf.clazz.Protocol.UpdateSubAppItemResponse;
import protobuf.clazz.activity.ActivityRedPackProto.ActivityRedPackReq;
import protobuf.clazz.activity.ActivityRedPackProto.ActivityRuleResp;
import protobuf.clazz.activity.ActivityRedPackProto.ActivityShareResp;
import protobuf.clazz.activity.ActivityRedPackProto.GetRedPackData;
import protobuf.clazz.activity.ActivityRedPackProto.GetRedPackResp;
import protobuf.clazz.activity.ActivityRedPackProto.RankData;
import protobuf.clazz.activity.ActivityRedPackProto.RankListRedPackResp;
import protobuf.clazz.activity.ActivityRedPackProto.ReceiveRedPackData;
import protobuf.clazz.activity.ActivityRedPackProto.ReceiveRedPackResp;
import protobuf.clazz.activity.ActivityRedPackProto.RedPackRuleData;
import protobuf.clazz.c2s.C2SProto.AppItemsReq;


@ICmd(code = C2SCmd.ACTIVITY_RED_PACK, desc = "红包雨活动")
@SuppressWarnings("unused")
public final class ActivityRedPackHandler extends IClientHandler<ActivityRedPackReq> {

	private static final Logger logger = LoggerFactory.getLogger(ActivityRedPackHandler.class);
	
	private static final int RANK_DATA  = 1;			// 排行榜数据
	private static final int RED_PACK_GET_DATA = 2;		// 获得记录
	private static final int RED_PACK_RECEIVE_DATA  = 3; // 领取记录
	private static final int RED_PACK_RULE  = 4;  //活动规则
	private static final int RED_PACK_SHARE = 5;  // 分享链接
	private static final int RED_PACK_SHARE_SUCCESS = 6;  // 分享链接成功
	
	@Override
	protected void execute(ActivityRedPackReq req, Request topRequest, C2SSession session) throws Exception {
		int paramType = req.getParamType();
		long accountId = session.getAccountID();
		switch (paramType) {
		case RANK_DATA:
			if(!GbCdCtrl.canHandleMust(session, Opt.RANK_DATA)) return;
			session.send(PBUtil.toS2CCommonRsp(S2CCmd.ACTIVITY_RED_PACK_RANK, processRankData(accountId)));
			break;
		case RED_PACK_GET_DATA:
			if(!GbCdCtrl.canHandleMust(session, Opt.RED_PACK_GET_DATA)) return;
			session.send(PBUtil.toS2CCommonRsp(S2CCmd.ACTIVITY_RED_PACK_GET, processRedPackGet(accountId)));
			break;
		case RED_PACK_RECEIVE_DATA:
			if(!GbCdCtrl.canHandleMust(session, Opt.RED_PACK_RECEIVE_DATA)) return;
			session.send(PBUtil.toS2CCommonRsp(S2CCmd.ACTIVITY_RED_PACK_RECEIVE, processRedPackReceive(accountId)));
			break;
		case RED_PACK_RULE:
			session.send(PBUtil.toS2CCommonRsp(S2CCmd.ACTIVITY_RED_PACK_RULE, processRedPackRule()));
			break;
		case RED_PACK_SHARE:
			session.send(PBUtil.toS2CCommonRsp(S2CCmd.ACTIVITY_RED_PACK_SHARE, processRedPackShare()));
			break;
		case RED_PACK_SHARE_SUCCESS: //只记录分享成功的分享
			processRedPackShareSuccess(accountId);
			break;
		default:
			break;
		}
		
	}

	
	/**
	 * 获取红包雨排行榜
	 * @param builder
	 * @param req
	 */
	private RankListRedPackResp.Builder processRankData(final long accountId) {
		
		RankListRedPackResp.Builder rankListResp = RankListRedPackResp.newBuilder();
		List<RedPackageRankModel> rankList = RankService.getInstance().getRedPackageRankList();
		if( rankList == null ){
			logger.error("红包雨排行榜NUll AccountID="+accountId);
		}else{
			for (RedPackageRankModel redPackageRankModel : rankList) {
				RankData.Builder rankData = RankData.newBuilder();
				rankData.setRanking(redPackageRankModel.getRank());
				rankData.setHeadImgUrl(redPackageRankModel.getHead());
				rankData.setNickName(redPackageRankModel.getNickName());
				rankData.setRedPackMoney( changeF2Y(redPackageRankModel.getValue()));
				rankListResp.addRankData(rankData);
			}
		}
		
		ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
		RedActivityModel model = centerRMIServer.getAccount(accountId).getRedActivityModel();
		long allMoney = 0;
		long notReceiveMoney = 0;
		if(model != null){
			allMoney  = model.getAll_money();
			notReceiveMoney = model.getAll_money() - model.getReceive_money();
		}
		rankListResp.setAllMoney(changeF2Y(allMoney));
		rankListResp.setNotReceiveMoney(changeF2Y(notReceiveMoney));
		return rankListResp;
	}
	
	
	
	/**
	 * 获取红包雨我的红包获得记录
	 */
	private GetRedPackResp.Builder processRedPackGet(final long accountId) {
		GetRedPackResp.Builder gets = GetRedPackResp.newBuilder();
		List<RedPackageModel> list = MongoDBServiceImpl.getInstance().getRedPackageModelList(accountId);
		if( list == null ){
			logger.error("获取个人红包获得记录为NUll AccountID="+accountId);
			return gets;
		}
		
		for (RedPackageModel model : list) {
			
			GetRedPackData.Builder getData = GetRedPackData.newBuilder();
			getData.setGetTime(model.getCreate_time().getTime());
			getData.setActivityName(model.getActive_type() ==1 ? "红包雨" : "运财童子");
			getData.setRedPackMoney(changeF2Y( model.getMoney()+0l ));
			gets.addGetData(getData);
		}
		return gets;
	}
	
	

	/**
	 * 获取红包雨个人红包领取数据
	 */
	private ReceiveRedPackResp.Builder processRedPackReceive(final long accountId) {
		ReceiveRedPackResp.Builder receives = ReceiveRedPackResp.newBuilder();
		List<RedPackageRecordModel> list = MongoDBServiceImpl.getInstance().queryRedPackageRecordList(accountId);
		if( list == null ){
			logger.error("获取个人红包领取数据为NUll AccountID="+accountId);
			return receives;
		}

		for (RedPackageRecordModel model : list) {
			ReceiveRedPackData.Builder receiveData = ReceiveRedPackData.newBuilder();
			receiveData.setReceiveTime(model.getCreate_time().getTime());
			receiveData.setRedPackMoney(changeF2Y( model.getMoney()+0l ));
			receives.addReceiveData(receiveData);
		}
		return receives;
	}
	
	
	
	/**
	 * 获取红包雨规则
	 */
	private ActivityRuleResp.Builder processRedPackRule() {
		ActivityRuleResp.Builder rules = ActivityRuleResp.newBuilder();
		//DB库
		FastMap<Integer, SysParamModel> fastMap = SysParamServerDict.getInstance().getSysParamModelDictionaryByGameId(EGameType.DT.getId());
		String ruleString ="";
		if(fastMap != null && fastMap.get(SysParamEnum.ID_1198.getId()) != null){
			ruleString = fastMap.get(SysParamEnum.ID_1198.getId()).getStr2();
		}
		if(ruleString.length() > 0 ){
			String[] str1 = ruleString.split("%");
			for (String string : str1) {
				RedPackRuleData.Builder builder = RedPackRuleData.newBuilder(); 
				if(string.contains("*")){
					String[] str2  = string.split("\\*");
					builder.setContent(str2[1]);
					builder.setTitle(str2[0]);
				}else{
					builder.setTitle(string);
				}
				rules.addRules(builder);
			}
		}
		return rules;
	}
	
	
	
	/**
	 * 获取分享链接
	 */
	private ActivityShareResp.Builder processRedPackShare() {
		ActivityShareResp.Builder share = ActivityShareResp.newBuilder();
		//DB库
		FastMap<Integer, SysParamModel> fastMap = SysParamServerDict.getInstance().getSysParamModelDictionaryByGameId(EGameType.DT.getId());
		String shareString ="";
		if(fastMap != null && fastMap.get(SysParamEnum.ID_1198.getId()) != null){
			shareString = fastMap.get(SysParamEnum.ID_1198.getId()).getStr1();
		}
		share.setShare(shareString);   
		return share;
	}
	
	/**
	 * 分享链接成功通知
	 */
	public void processRedPackShareSuccess(final long accountId){
		ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
		//通知中心服更新分享时间
		centerRMIServer.setShareTime(accountId);
	}
	
	
    /**   
     * 将分为单位的转换为元并返回金额格式的字符串 （除100）  
     *   
     * @param amount  
     * @return  
     */    
    public Double changeF2Y(Long amount) {    
       /* if(!amount.toString().matches("\\?[0-9]+")) {    
        	logger.error("金额格式有误"+amount);
        	return 0.00;
        }   */ 
        String amString = amount.toString();    
        StringBuffer result = new StringBuffer();    
        if(amString.length()==1){    
            result.append("0.0").append(amString);    
        }else if(amString.length() == 2){    
            result.append("0.").append(amString);    
        }else{    
            String intString = amString.substring(0,amString.length()-2);    
            for(int i=1; i<=intString.length();i++){    
                if( (i-1)%3 == 0 && i !=1){    
                
                }    
                result.append(intString.substring(intString.length()-i,intString.length()-i+1));    
            }    
            result.reverse().append(".").append(amString.substring(amString.length()-2));    
        }    
        return Double.parseDouble(result.toString());    
    }
	
}
