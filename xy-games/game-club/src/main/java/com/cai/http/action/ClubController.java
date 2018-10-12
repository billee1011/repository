package com.cai.http.action;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.alibaba.fastjson.JSONObject;
import com.cai.common.domain.AccountSimple;
import com.cai.common.domain.ClubGroupModel;
import com.cai.common.domain.ClubMemberModel;
import com.cai.common.domain.ClubMemberRecordModel;
import com.cai.common.domain.json.ClubRuleBaseModel;
import com.cai.common.rmi.ICenterRMIServer;
import com.cai.common.util.GameDescUtil;
import com.cai.common.util.SpringService;
import com.cai.constant.Club;
import com.cai.dictionary.GameGroupRuleDict;
import com.cai.http.FastJsonJsonView;
import com.cai.service.ClubDaoService;
import com.cai.service.ClubService;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequestMapping("/club")
public class ClubController {
	
	public Logger logger = LoggerFactory.getLogger(getClass());
	
	public static final int FAIL = -1;

	public static final int SUCCESS = 1;
	
	@RequestMapping("/list")
	public ModelAndView list(HttpServletRequest request, long userId, HttpServletResponse response) {
		Map<String, Object> map = new HashMap<String, Object>();
		
		List<ClubGroupModel> models =  SpringService.getBean(ClubDaoService.class).getDao().getClubGroupByUserId(userId);
		
		for (ClubGroupModel clubGroupModel : models) {
			List<ClubRuleBaseModel> ruleBase = new ArrayList<>();
			clubGroupModel.setRules(ruleBase);
			Club club = ClubService.getInstance().getClub(clubGroupModel.getClub_id());
			if(club != null){
				clubGroupModel.setClub_name(club.clubModel.getClub_name());
				club.clubModel.getRules().forEach((id,rule)->{
					ClubRuleBaseModel clubRuleBase = new ClubRuleBaseModel();
					clubRuleBase.setId(rule.getId());
					clubRuleBase.setGame_type_index(rule.getGame_type_index());
					clubRuleBase.setGame_round(rule.getGame_round());
					clubRuleBase.setGame_desc(GameDescUtil.getGameDesc(rule, GameGroupRuleDict.getInstance().get(rule.getGame_type_index())));
					ruleBase.add(clubRuleBase);
				});
			}
		}
		
		map.put("result", SUCCESS);
		map.put("data", models);
		
		return new ModelAndView(new FastJsonJsonView(), map);
	}
	
	@RequestMapping("/bind")
	public ModelAndView bind(HttpServletRequest request, String groupId, int clubId, HttpServletResponse response,long userId) {
		
		Map<String, Object> resultMap = new HashMap<String, Object>();
		
		if(StringUtils.isEmpty(groupId)){
			resultMap.put("result", FAIL);
			resultMap.put("msg", "参数错误");
			return new ModelAndView(new FastJsonJsonView(), resultMap);
		}
		
		Club club = ClubService.getInstance().getClub(clubId);
		if(club == null){
			resultMap.put("result", FAIL);
			resultMap.put("msg", "找不到该亲友圈");
		 	return new ModelAndView(new FastJsonJsonView(), resultMap);
		}
		
		if(club.clubModel.getAccount_id() != userId){
			resultMap.put("result", FAIL);
			resultMap.put("msg", "不是你的亲友圈");
		 	return new ModelAndView(new FastJsonJsonView(), resultMap);
		}

		ClubGroupModel clubGroup = SpringService.getBean(ClubDaoService.class).getDao().getClubGroup(groupId);
		if (clubGroup != null) {
			resultMap.put("result", FAIL);
			resultMap.put("msg", "该微信群已被绑定");
			return new ModelAndView(new FastJsonJsonView(), resultMap);
		}
	
		clubGroup = new ClubGroupModel();
		clubGroup.setGroup_id(groupId);
		clubGroup.setClub_id(clubId);

		SpringService.getBean(ClubDaoService.class).getDao().insertClubGroup(clubGroup);
		ClubService.getInstance().groupClubMaps.put(groupId, clubId);
		club.groupSet.add(groupId);
		resultMap.put("result", SUCCESS);
		resultMap.put("msg", "绑定成功");
		
		return new ModelAndView(new FastJsonJsonView(), resultMap);
	}
	
	@RequestMapping("/unbind")
	public ModelAndView unbind(HttpServletRequest request, String group_id, int club_id, HttpServletResponse response) {
		
		Map<String, Object> resultMap = new HashMap<String, Object>();
		
		if(StringUtils.isEmpty(group_id)){
			resultMap.put("result", FAIL);
			resultMap.put("msg", "参数错误");
			return new ModelAndView(new FastJsonJsonView(), resultMap);
		}
		
		Club club = ClubService.getInstance().getClub(club_id);
		if(club == null){
			resultMap.put("result", FAIL);
			resultMap.put("msg", "找不到该亲友圈");
		 	return new ModelAndView(new FastJsonJsonView(), resultMap);
		}
		
		if(SpringService.getBean(ClubDaoService.class).getDao().deleteClubGroup(club_id, group_id) <= 0){
			resultMap.put("result", FAIL);
			resultMap.put("msg", "该群没有绑定信息");
			return new ModelAndView(new FastJsonJsonView(), resultMap);
		}
		ClubService.getInstance().groupClubMaps.remove(group_id);
		club.groupSet.remove(group_id);
		resultMap.put("result", SUCCESS);
		resultMap.put("msg", "解绑成功");
		
		return new ModelAndView(new FastJsonJsonView(), resultMap);
	}
	
	@RequestMapping("/lucky")
	public ModelAndView lucky(HttpServletRequest request, int new_id, int club_id, HttpServletResponse response) {
		
		Map<String, Object> resultMap = new HashMap<String, Object>();
		
		if(new_id <= 0 || club_id <= 0){
			resultMap.put("result", FAIL);
			resultMap.put("msg", "参数错误");
			return new ModelAndView(new FastJsonJsonView(), resultMap);
		}
		
		Club club = ClubService.getInstance().getClub(club_id);
		if(club == null){
			resultMap.put("result", FAIL);
			resultMap.put("msg", "找不到该亲友圈");
		 	return new ModelAndView(new FastJsonJsonView(), resultMap);
		}
		
		if(ClubService.getInstance().getClub(new_id) != null){
			resultMap.put("result", FAIL);
			resultMap.put("msg", "该亲友圈号码已经存在");
		 	return new ModelAndView(new FastJsonJsonView(), resultMap);
		}
		
//		if(!ClubService.getInstance().changeClubId(club, new_id) ){
//			resultMap.put("result", FAIL);
//			resultMap.put("msg", "修改亲友圈靓号异常");
//			return new ModelAndView(new FastJsonJsonView(), resultMap);
//		}
		resultMap.put("result", SUCCESS);
		resultMap.put("msg", "待开发");
		return new ModelAndView(new FastJsonJsonView(), resultMap);
	}
	

	@RequestMapping("/updateAccountId")
	public ModelAndView updateAccountId(HttpServletRequest request, int new_id, int account_id, HttpServletResponse response) {
		
		Map<String, Object> resultMap = new HashMap<String, Object>();
		
		if(new_id <= 0 || account_id <= 0){
			resultMap.put("result", FAIL);
			resultMap.put("msg", "参数错误");
			return new ModelAndView(new FastJsonJsonView(), resultMap);
		}
		
		logger.info("玩家修改靓号 new_id:{},old_id:{}", new_id, account_id);
		
		ClubService.getInstance().updateAccountId(new_id, account_id);
	
		resultMap.put("result", SUCCESS);
		resultMap.put("msg", "修改成功");
		return new ModelAndView(new FastJsonJsonView(), resultMap);
	}
	
	
	@RequestMapping("/bindAccountId")
	public ModelAndView bindAccountId(HttpServletRequest request, long userID, String nickName, 
			HttpServletResponse response, String groupID, long ownerId) {
		
		Map<String, Object> resultMap = new HashMap<String, Object>();
		
		ClubGroupModel clubGroup = SpringService.getBean(ClubDaoService.class).getDao().getClubGroup(groupID);
		
		if(clubGroup == null){
			resultMap.put("result", FAIL);
			resultMap.put("msg", "群没有绑定亲友圈");
			return new ModelAndView(new FastJsonJsonView(), resultMap);
		}
		
		Club club = ClubService.getInstance().getClub(clubGroup.getClub_id());
		if(club == null){
			resultMap.put("result", FAIL);
			resultMap.put("msg", "找不到该亲友圈");
		 	return new ModelAndView(new FastJsonJsonView(), resultMap);
		}
		
		if(club.clubModel.getAccount_id() != ownerId){
			resultMap.put("result", FAIL);
			resultMap.put("msg", "亲友圈创建人与群创建人对不上");
		 	return new ModelAndView(new FastJsonJsonView(), resultMap);
		}
		
		ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
		AccountSimple simple = centerRMIServer.getSimpleAccount(userID);
		if(simple == null){
			resultMap.put("result", FAIL);
			resultMap.put("msg", "找不到该用户");
		 	return new ModelAndView(new FastJsonJsonView(), resultMap);
		}
		
		if(nickName != null && !nickName.equals(simple.getNick_name())){
			resultMap.put("result", FAIL);
			resultMap.put("msg", "绑定的Id错误，不是本人");
		 	return new ModelAndView(new FastJsonJsonView(), resultMap);
		}
		
		if(club.members.containsKey(userID)){
			resultMap.put("result", FAIL);
			resultMap.put("msg", "你已是该亲友圈成员");
		 	return new ModelAndView(new FastJsonJsonView(), resultMap);
		}
		ClubService.getInstance().requestClub(clubGroup.getClub_id(), userID, simple.getIcon(), "", nickName);
		resultMap.put("result", SUCCESS);
		resultMap.put("msg", "申请加入亲友圈成功");
		return new ModelAndView(new FastJsonJsonView(), resultMap);
	}
	@RequestMapping("/bindId")
	public ModelAndView bindId(HttpServletRequest request, long userId,  int clubId, long partnerId,
			HttpServletResponse response) {
		
		Map<String, Object> resultMap = new HashMap<String, Object>();
		Club club = ClubService.getInstance().getClub(clubId);
		if(club == null){
			resultMap.put("result", FAIL);
			resultMap.put("msg", "找不到该亲友圈");
		 	return new ModelAndView(new FastJsonJsonView(), resultMap);
		}
	
		ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
		AccountSimple bindAccount = centerRMIServer.getSimpleAccount(userId);
		if(bindAccount == null){
			resultMap.put("result", FAIL);
			resultMap.put("msg", "找不到该用户");
		 	return new ModelAndView(new FastJsonJsonView(), resultMap);
		}
		if(club.members.containsKey(userId)){
			resultMap.put("result", 10000);
			resultMap.put("msg", "你已是该亲友圈成员");
		 	return new ModelAndView(new FastJsonJsonView(), resultMap);
		}
		ClubService.getInstance().requestClub(clubId, userId, bindAccount.getIcon(), "", bindAccount.getNick_name(), partnerId);
		resultMap.put("result", SUCCESS);
		resultMap.put("msg", "申请加入亲友圈成功");
		return new ModelAndView(new FastJsonJsonView(), resultMap);
	}

	@RequestMapping("/club/detail")
	public ModelAndView existId(HttpServletRequest request,int clubId, 
			HttpServletResponse response) {
		Map<String, Object> resultMap = new HashMap<String, Object>();
		if(clubId<=0){
			resultMap.put("result", FAIL);
			resultMap.put("msg", "亲友圈不存在");
		 	return new ModelAndView(new FastJsonJsonView(), resultMap);
		}
		Club club = ClubService.getInstance().getClub(clubId);
		if(club == null){
			resultMap.put("result", FAIL);
			resultMap.put("msg", "亲友圈不存在");
		 	return new ModelAndView(new FastJsonJsonView(), resultMap);
		}
		JSONObject json = new JSONObject();
		json.put("clubName", club.clubModel.getClub_name());
		json.put("gameDesc", club.getGameDescs());
		json.put("accountId", club.getOwnerId());
		json.put("nickName", club.getOwnerName());
		json.put("headPic", club.clubModel.getAvatar());
		resultMap.put("result", SUCCESS);
		resultMap.put("data", json);
		return new ModelAndView(new FastJsonJsonView(), resultMap);
	}
	
	@RequestMapping("/exist/userId")
	public ModelAndView existId(HttpServletRequest request, long userId,  int clubId, 
			HttpServletResponse response) {
		
		Map<String, Object> resultMap = new HashMap<String, Object>();
		Club club = ClubService.getInstance().getClub(clubId);
		if(club == null){
			resultMap.put("result", FAIL);
			resultMap.put("msg", "找不到该亲友圈");
		 	return new ModelAndView(new FastJsonJsonView(), resultMap);
		}
	
		ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
		boolean isExsit = centerRMIServer.isExistAccount(userId);
		if(!isExsit){
			resultMap.put("result", FAIL);
			resultMap.put("msg", "找不到该用户");
		 	return new ModelAndView(new FastJsonJsonView(), resultMap);
		}
		if(club.members.containsKey(userId)){
			resultMap.put("result", SUCCESS);
			resultMap.put("msg", "你已是该亲友圈成员");
		 	return new ModelAndView(new FastJsonJsonView(), resultMap);
		}else{
			resultMap.put("result", FAIL);
			resultMap.put("msg", "你还未加入亲友圈");
		 	return new ModelAndView(new FastJsonJsonView(), resultMap);
		}
	}

	@RequestMapping("/club/memberRecord")
	public ModelAndView existId(HttpServletRequest request, int clubId, long accountId, int day, int addValue, HttpServletResponse response) {
		Map<String, Object> resultMap = new HashMap<String, Object>();
		if (clubId <= 0) {
			resultMap.put("result", FAIL);
			resultMap.put("msg", "亲友圈不存在");
			return new ModelAndView(new FastJsonJsonView(), resultMap);
		}
		Club club = ClubService.getInstance().getClub(clubId);
		if (club == null) {
			resultMap.put("result", FAIL);
			resultMap.put("msg", "亲友圈不存在");
			return new ModelAndView(new FastJsonJsonView(), resultMap);
		}
		if (day < 1 || day > 8) {
			resultMap.put("result", FAIL);
			resultMap.put("msg", "修改的天数不合法(1-8)");
			return new ModelAndView(new FastJsonJsonView(), resultMap);
		}
		club.runInReqLoop(() -> {
			ClubMemberModel memberModel = club.members.get(accountId);
			if (memberModel == null) {
				return;
			}
			ClubMemberRecordModel dayRecord = club.getMemberRecordModelByDay(day, memberModel);
			dayRecord.setTireValue(dayRecord.getTireValue() + addValue);
			dayRecord.setAccuTireValue(dayRecord.getAccuTireValue() + addValue);
		});
		resultMap.put("result", SUCCESS);
		return new ModelAndView(new FastJsonJsonView(), resultMap);
	}
}
