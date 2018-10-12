package com.cai.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.SortedMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cai.common.define.ERedisTopicType;
import com.cai.common.domain.Account;
import com.cai.common.domain.AccountGroupModel;
import com.cai.common.domain.Event;
import com.cai.common.util.SpringService;
import com.cai.core.MonitorEvent;
import com.cai.domain.Session;

import protobuf.redis.ProtoRedis.RedisResponse;
import protobuf.redis.ProtoRedis.RedisResponse.RsResponseType;
import protobuf.redis.ProtoRedis.RsAccountGroup;
import protobuf.redis.ProtoRedis.RsAccountResponse;
@Service
public class GroupClubMemberService extends AbstractService{
	private static final Logger logger = LoggerFactory.getLogger(GroupClubMemberService.class);
	
	private static final class LazzyHolder {
		private static final GroupClubMemberService INSTANCE = new GroupClubMemberService();
	}

	public static GroupClubMemberService getInstance() {
		return LazzyHolder.INSTANCE;
	}
	public int ClubToGroup(int clubId,String groupId){
		PublicService publicService = SpringService.getBean(PublicService.class);
		List<Long> list = publicService.getPublicDAO().getClubAccountIdNotInGroup(groupId, clubId);
		if(list.size()==0){//没有需要同步的用户
			return 0;
		}
		List<AccountGroupModel> groupList = new ArrayList<AccountGroupModel>(list.size());
		Date date = new Date();
		for(Long id:list){
			Account account = PublicServiceImpl.getInstance().getAccount(id);
			if (account == null) {
				continue;
			}
			AccountGroupModel model = new AccountGroupModel();
			model.setAccount_id(id);
			model.setGroupId(groupId);
			model.setVal(0);
			model.setDate(date);
			account.getAccountGroupModelMap().put(groupId, model);
			groupList.add(model);
		}
		try{
			if(groupList.size()>0){
				publicService.getPublicDAO().batchInsert("insertAccountGroupModel", groupList);
				RedisResponse.Builder redisResponseBuilder = RedisResponse.newBuilder();
				redisResponseBuilder.setRsResponseType(RsResponseType.ACCOUNT_GROUP);
				groupList.forEach((accountGroupModel)->{
					RsAccountGroup.Builder rsAccountGroup = RsAccountGroup.newBuilder();
					rsAccountGroup.setAccountId(accountGroupModel.getAccount_id());
					rsAccountGroup.setGroupId(accountGroupModel.getGroupId());
					redisResponseBuilder.addRsAccountGroupList(rsAccountGroup);
				});
				RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(), ERedisTopicType.topicProxy);

			}
		}catch(Exception e){
			logger.error(clubId+"同步俱乐部数据到微信群出错"+groupId, e);
			return -3;
		}
		return 0;
	}
	public int kickGroup(long accountId ,String groupId){
		PublicService publicService = SpringService.getBean(PublicService.class);
		Account account = PublicServiceImpl.getInstance().getAccount(accountId);
		if (account == null) {
			return -1;
		}
		AccountGroupModel accountGroupModel = account.getAccountGroupModelMap().remove(groupId);
		if (accountGroupModel == null) {
			return -2;
		}
		publicService.getPublicDAO().deleteAccountGroupModel(accountGroupModel);
		RsAccountResponse.Builder rsAccountResponseBuilder = RsAccountResponse.newBuilder();
		RedisResponse.Builder redisResponseBuilder = RedisResponse.newBuilder();
		redisResponseBuilder.setRsResponseType(RsResponseType.ACCOUNT_UP);
		rsAccountResponseBuilder.setAccountId(accountId);
		rsAccountResponseBuilder.setGroupID(groupId);
		rsAccountResponseBuilder.setDeleteGroupID(true);
		redisResponseBuilder.setRsAccountResponse(rsAccountResponseBuilder);
		RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(), ERedisTopicType.topicProxy);
		return 0;
	}

	@Override
	protected void startService() {
		// TODO Auto-generated method stub
		
	}


	@Override
	public MonitorEvent montior() {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public void onEvent(Event<SortedMap<String, String>> event) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void sessionCreate(Session session) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void sessionFree(Session session) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void dbUpdate(int _userID) {
		// TODO Auto-generated method stub
		
	}
}
