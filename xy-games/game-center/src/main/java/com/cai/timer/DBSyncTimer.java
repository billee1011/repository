//package com.cai.timer;
//
//import java.util.List;
//import java.util.TimerTask;
//
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import com.cai.common.define.DbOpType;
//import com.cai.common.define.DbStoreType;
//import com.cai.common.define.ELogType;
//import com.cai.common.define.ESysLogLevelType;
//import com.cai.common.domain.Account;
//import com.cai.common.domain.AccountModel;
//import com.cai.common.domain.AccountParamModel;
//import com.cai.common.domain.AccountWeixinModel;
//import com.cai.common.domain.DBUpdateDto;
//import com.cai.common.util.PerformanceTimer;
//import com.cai.core.DataThreadPool;
//import com.cai.service.MongoDBServiceImpl;
//import com.cai.service.PublicServiceImpl;
//import com.google.common.collect.Lists;
//
///**
// * 数据库同步
// * @author run
// *
// */
//public class DBSyncTimer extends TimerTask{
//
//	private static Logger logger = LoggerFactory.getLogger(DBSyncTimer.class);
//	
//	@Override
//	public void run() {
//		handle();
//	}
//	
//	
//	public void handle(){
//		try {
//			PerformanceTimer timer = new PerformanceTimer();
//			//检测当前内存
//			List<AccountModel> accountModelList = Lists.newArrayList();
//			List<AccountWeixinModel> accountWeiXinModelList = Lists.newArrayList();
//			List<AccountParamModel> accountParamModelList = Lists.newArrayList();
//			
//			for(Account account : PublicServiceImpl.getInstance().getAccountIdMap().values()){
//				if(account.getAccountModel().isNeedDB()){
//					account.getAccountModel().setNeedDB(false);
//					accountModelList.add(account.getAccountModel());
//				}
//				//微信相关
//				AccountWeixinModel accountWeixinModel = account.getAccountWeixinModel();
//				if(accountWeixinModel!=null && accountWeixinModel.isNeedDB()){
//					accountWeixinModel.setProvince("");
//					accountWeixinModel.setNeedDB(false);
//					accountWeiXinModelList.add(accountWeixinModel);
//				}
//				
//				//账号参数
//				for(AccountParamModel m : account.getAccountParamModelMap().values()){
//					//新值直接入库
//					if(m.isNewAddValue()){
//						m.setNewAddValue(false);
//						m.setNeedDB(false);
//						DataThreadPool.getInstance().addTask(new DBUpdateDto(DbStoreType.PUBLIC, DbOpType.INSERT, "insertAccountParamModel", m));
//					}
//					
//					if(m.isNeedDB()){
//						m.setNeedDB(false);
//						accountParamModelList.add(m);
//					}
//				}
//				
//				
//			}
//			
//			long count = 0;
//			StringBuilder buf = new StringBuilder();
//			//指量更新
//			if(accountModelList.size()>0){
//				DataThreadPool.getInstance().addTask(new DBUpdateDto(DbStoreType.PUBLIC, DbOpType.BATCH_UPDATE, "updateAccountModel", accountModelList));
//				count += accountModelList.size();
//				buf.append("accountModelList:"+accountModelList.size());
//			}
//			
//			if(accountWeiXinModelList.size()>0){
//				DataThreadPool.getInstance().addTask(new DBUpdateDto(DbStoreType.PUBLIC, DbOpType.BATCH_UPDATE, "updateAccountWeixinModel", accountWeiXinModelList));
//				count += accountWeiXinModelList.size();
//				buf.append(",accountWeiXinModelList:"+accountWeiXinModelList.size());
//			}
//			
//			if(accountParamModelList.size()>0){
//				DataThreadPool.getInstance().addTask(new DBUpdateDto(DbStoreType.PUBLIC, DbOpType.BATCH_UPDATE, "updateAccountParamModel", accountParamModelList));
//				count += accountParamModelList.size();
//				buf.append(",accountParamModelList:"+accountParamModelList.size());
//			}
//			
//			if(count>0){
//				String msg = "批量更新数据库:数量:"+accountModelList.size()+timer.getStr()+" INFO:"+buf.toString();
//				long time = timer.get();
//				ESysLogLevelType eSysLogLevelType;
//				if(time<5000L){
//					eSysLogLevelType  = ESysLogLevelType.NONE;
//				}else{
//					eSysLogLevelType  = ESysLogLevelType.WARN;
//				}
//				MongoDBServiceImpl.getInstance().systemLog(ELogType.dbBatch, msg, timer.get(), count, eSysLogLevelType);
//			}
//			
//		} catch (Exception e) {
//			logger.error("error",e);
//		}
//	}
//	
//	
//	
//}
