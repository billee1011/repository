package com.cai.service;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.cai.common.domain.AccountGroupModel;
import com.cai.common.domain.AccountModel;
import com.cai.common.domain.AccountSimple;
import com.cai.common.domain.ClubGroupModel;
import com.cai.common.domain.SysParamModel;
import com.cai.common.rmi.ICenterRMIServer;
import com.cai.common.util.HttpClientUtils;
import com.cai.common.util.SpringService;
import com.cai.constant.Club;
import com.cai.constant.ServiceOrder;
import com.cai.dao.ClubDao;
import com.cai.dictionary.SysParamServerDict;
import com.xianyi.framework.core.service.AbstractService;
import com.xianyi.framework.core.service.IService;

import protobuf.clazz.ClubMsgProto.GroupMembers;
import protobuf.clazz.ClubMsgProto.WxGroups;

@IService(order = ServiceOrder.MEMBER_TURN, desc = "俱乐部与微信群交互")
public class GroupClubMemberService extends AbstractService{
	protected final Logger logger = LoggerFactory.getLogger(getClass());

	private GroupClubMemberService(){
	}
	private static GroupClubMemberService instance;
	
	public static GroupClubMemberService getInstance(){
		if(null == instance){
			instance = new GroupClubMemberService();
		}
		return instance;
	}
	// public int ClubToGroup(int clubId,String groupId){
	// ClubDao clubDao = SpringService.getBean(ClubDaoService.class).getDao();
	// List<Long> list = clubDao.getClubAccountIdNotInGroup(groupId, clubId);
	// if(list.size()==0){
	// return 0;
	// }
	// List<AccountGroupModel> groupList = new
	// ArrayList<AccountGroupModel>(list.size());
	// Date date = new Date();
	// for(Long id:list){
	// AccountGroupModel model = new AccountGroupModel();
	// model.setAccount_id(id);
	// model.setGroupId(groupId);
	// model.setVal(0);
	// model.setDate(date);
	// groupList.add(model);
	// }
	// try{
	// clubDao.batchInsert("insertAccountGroupModel", list);
	// }catch(Exception e){
	// logger.error(clubId+"同步俱乐部数据到微信群出错"+groupId, e);
	// }
	// return 0;
	// }

	@SuppressWarnings("unchecked")
	public int GroupToClub(int clubId, String groupId) {
		ClubDao clubDao = SpringService.getBean(ClubDaoService.class).getDao();
		List<Long> list = clubDao.getGroupAccountIdNotInClub(groupId, clubId);
		if (list.size() == 0) {
			return 0;
		}
		try {
			ClubService service = ClubService.getInstance();
			for (Long id : list) {
				service.bindClubAccount(clubId, id);
			}
		} catch (Exception e) {
			logger.error(clubId + "同步微信群数据到俱乐部出错" + groupId, e);
			return -2;
		}
		return 0;
	}
	public int GroupMemberToClub(int clubId, String groupId,long accountId) {
		try {
			ClubService service = ClubService.getInstance();
			Club club = service.getClub(clubId);
			if(club == null){
				return -1;
			}
//			if(!club.groupSet.contains(groupId)){
//				return -1;
//			}
			service.bindClubAccount(clubId, accountId);
		} catch (Exception e) {
			logger.error(clubId + "同步微信群数据到俱乐部出错" + groupId, e);
			return -1;
		}
		return 0;
	}
	public List<WxGroups> getWxGroupsList(long accountId,StringBuffer code,String groupId) {
		ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
		AccountModel accountModel = centerRMIServer.getAccountModel(accountId);
		List<WxGroups> list = new ArrayList<>();
		if(accountModel.getProxy_level()==0&&accountModel.getIs_agent()==0){
			return list;
		}
		SysParamModel sysParamModel2233 = SysParamServerDict.getInstance().getSysParamModelDictionaryByGameId(6).get(2233);
		String path = "";
		int hallId = 0;
		if(sysParamModel2233==null){
			path = "http://39.108.11.126/web/group/list";
			hallId = 80008;
		}else{
			hallId = sysParamModel2233.getVal1();
			path = sysParamModel2233.getStr1();
		}
		path = path+"?hallId="+hallId+"&userId="+accountId;
		try {
			logger.info(path);
			String res = HttpClientUtils.get(path);
			if(StringUtils.isBlank(res)){
				return list;
			}
			JSONObject json = JSONObject.parseObject(res);
			if(json.getIntValue("status")!=0){
				if(json.getIntValue("status")==10000){
					code.append("10000");
				}
				return list;
			}
//			json.getString("data")
			if(StringUtils.isNotBlank(groupId)){
				List<com.cai.common.domain.json.AccountGroupModel> resList = JSON.parseArray(json.getString("data"), com.cai.common.domain.json.AccountGroupModel.class);
				for(com.cai.common.domain.json.AccountGroupModel model:resList){
					if(model.getGroupId().equals(groupId)){
						WxGroups.Builder builder = WxGroups.newBuilder();
						builder.setGroupId(model.getGroupId());
						builder.setGroupMemberNum(model.getGroupMemberNum());
						builder.setGroupName(model.getGroupName());
						list.add(builder.build());
						break;
					}
					
				};
			}else{
				List<com.cai.common.domain.json.AccountGroupModel> resList = JSON.parseArray(json.getString("data"), com.cai.common.domain.json.AccountGroupModel.class);
				resList.forEach(model->{
					WxGroups.Builder builder = WxGroups.newBuilder();
					builder.setGroupId(model.getGroupId());
					builder.setGroupMemberNum(model.getGroupMemberNum());
					builder.setGroupName(model.getGroupName());
					list.add(builder.build());
				});
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		return list;
	}

	public List<GroupMembers> getGroupMembersList(int clubId, String groupId) {
		List<GroupMembers> list = new ArrayList<>();
		ClubDao clubDao = SpringService.getBean(ClubDaoService.class).getDao();
		List<AccountGroupModel> groupList = clubDao.getAccountGroupModelListByGroupId(groupId);
		if (groupList.size() == 0) {
			return list;
		}
		Club club = ClubService.getInstance().getClub(clubId);
		if (club == null) {
			return list;
		}
		ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
		groupList.forEach(accountGroupModel -> {
			AccountSimple accountSimple = centerRMIServer.getSimpleAccount(accountGroupModel.getAccount_id());
			if (accountSimple == null) {
				return;
			}
			GroupMembers.Builder builder = GroupMembers.newBuilder();
			builder.setAccountId(accountGroupModel.getAccount_id());
			builder.setNick(accountSimple.getNick_name());
			builder.setHeadPic(accountSimple.getIcon());
			builder.setIsJoinInClub(club.members.containsKey(accountGroupModel.getAccount_id()) ? 1 : 0);
			list.add(builder.build());
		});
		return list;
	}
	
	public int bindGroup(String groupId,int clubId,long userId){
		Club club = ClubService.getInstance().getClub(clubId);
		if(club == null){
		 	return Club.CLUB_NOT_FIND;
		}
		if(club.clubModel.getAccount_id() != userId){
		 	return Club.PERM_DENIED;
		}
		if(ClubService.getInstance().groupClubMaps.containsKey(groupId)){
			return Club.GROUP_ISBIND;
		}
		ClubGroupModel clubGroup  = new ClubGroupModel();
		clubGroup.setGroup_id(groupId);
		clubGroup.setClub_id(clubId);
		SpringService.getBean(ClubDaoService.class).getDao().insertClubGroup(clubGroup);
		ClubService.getInstance().groupClubMaps.put(groupId, clubId);
		club.groupSet.add(groupId);
		return Club.SUCCESS;
	}
	public int unbindGroup(String groupId,int clubId,long userId){
		Club club = ClubService.getInstance().getClub(clubId);
		if(club == null){
			return Club.CLUB_NOT_FIND;
		}
		if(SpringService.getBean(ClubDaoService.class).getDao().deleteClubGroup(clubId, groupId) <= 0){
			return Club.SUCCESS;
		}
		ClubService.getInstance().groupClubMaps.remove(groupId);
		club.groupSet.remove(groupId);
		return Club.SUCCESS;
	}

	@Override
	public void start() throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void stop() throws Exception {
		// TODO Auto-generated method stub
		
	}
}
