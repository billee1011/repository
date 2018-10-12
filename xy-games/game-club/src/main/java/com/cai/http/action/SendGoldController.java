package com.cai.http.action;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import com.alibaba.fastjson.JSON;
import com.cai.common.define.EGoldOperateType;
import com.cai.common.domain.AddGoldResultModel;
import com.cai.common.domain.ClubDailyCostModel;
import com.cai.common.domain.SysParamModel;
import com.cai.common.domain.json.AreaLimitJsonModel;
import com.cai.common.rmi.ICenterRMIServer;
import com.cai.common.util.MyDateUtil;
import com.cai.common.util.SpringService;
import com.cai.dictionary.SysParamServerDict;
import com.cai.http.FastJsonJsonView;
import com.cai.service.ClubService;
import com.cai.service.MongoDBService;
import com.cai.service.MongoDBServiceImpl;

import protobuf.clazz.ClubMsgProto.ClubProto;

@Controller
@RequestMapping("/web")
public class SendGoldController {
	
	public Logger logger = LoggerFactory.getLogger(getClass());
	
	public static final int FAIL = -1;

	public static final int SUCCESS = 1;
	//192.168.1.64:7002/gameClub/oss/web/list
	@RequestMapping("/list")
	public ModelAndView list(HttpServletRequest request, long accountId,int receive,String startDate,String endDate) {
		Map<String, Object> map = new HashMap<String, Object>();
		SysParamModel sysParamModel2232 = SysParamServerDict.getInstance().getSysParamModelDictionary().get(6).get(2232);
		if(sysParamModel2232==null||sysParamModel2232.getVal1()==0){
			map.put("result", FAIL);
			map.put("msg", "活动还未开始");
			return new ModelAndView(new FastJsonJsonView(), map);
		}
		Date start = null;
		Date end = null;
		if(StringUtils.isNotBlank(startDate)&&StringUtils.isNotBlank(endDate)){
			start = MyDateUtil.getZeroDate(MyDateUtil.parse(startDate, "yyyyMMdd"));
			end = MyDateUtil.getTomorrowZeroDate(MyDateUtil.parse(endDate, "yyyyMMdd"));
			if(start.getTime()>end.getTime()){
				map.put("result", FAIL);
				map.put("msg", "筛选日期有误");
				return new ModelAndView(new FastJsonJsonView(), map);
			}
		}else{
			start = MyDateUtil.getZeroDate(new Date());
			end = MyDateUtil.getTomorrowZeroDate(start);
		}
		List<ClubProto> clubList = ClubService.getInstance().getMyClub(accountId, false);
		if(clubList ==null||clubList.size()==0){
			map.put("result", FAIL);
			map.put("hasclub", false);
			map.put("msg", "您还未创建亲友圈，赶紧去创建吧！");
			return new ModelAndView(new FastJsonJsonView(), map);
		}
		String dateLimit = sysParamModel2232.getStr1();
		String[] limits = dateLimit.split("\\|");
		Date startD = MyDateUtil.parse(limits[0], "yyyy-MM-dd");
		Date endD = MyDateUtil.parse(limits[1], "yyyy-MM-dd");
		//如果所选的时间要早于活动开始时间
		if(startD.getTime()>start.getTime()){
			start = startD;
		}
		//如果所选的时间要大于活动结束时间
		if(endD.getTime()<end.getTime()){
			end = endD;
		}
		List<AreaLimitJsonModel> areaList = JSON.parseArray(sysParamModel2232.getStr2(), AreaLimitJsonModel.class);
		List<ClubDailyCostModel> list = MongoDBServiceImpl.getInstance().getClubDailyCostModelList( 
				accountId, start, end,sysParamModel2232.getVal2(),receive);
		int totalSend = 0;
		for(ClubDailyCostModel model:list){
			if(model.getReceive()==0){
				model.setSendGold(getSendGold((int)model.getCost(), areaList));
				totalSend += model.getSendGold();
			}
			
		}
		map.put("result", SUCCESS);
		map.put("data", list);
		map.put("hasclub", true);
		map.put("receive", totalSend);
		map.put("totalSend", MongoDBServiceImpl.getInstance().getTotalReceiveClubDailyCount(accountId));
		return new ModelAndView(new FastJsonJsonView(), map);
	}
	
	public int getSendGold(int cost,List<AreaLimitJsonModel> areaList){
		for(AreaLimitJsonModel model:areaList){
			if(model.getMinArea()<=cost&&model.getMaxArea()>=cost){
				return model.getNum()*cost/1000;
			}
		}
		return 0;
	}
	@RequestMapping("/active")
	public ModelAndView bind(HttpServletRequest request) {
		Map<String, Object> map = new HashMap<String, Object>();
		SysParamModel sysParamModel2232 =SysParamServerDict.getInstance().getSysParamModelDictionary().get(6).get(2232);
		if(sysParamModel2232==null){
			map.put("result", FAIL);
			map.put("msg", "活动还未开始");
			return new ModelAndView(new FastJsonJsonView(), map);
		}
		map.put("result", SUCCESS);
		map.put("data", sysParamModel2232);
		return new ModelAndView(new FastJsonJsonView(), map);
	}
	@RequestMapping("/receive")
	public ModelAndView receiveSendGold(HttpServletRequest request, long accountId,String mid,String startDate,String endDate) {
		Map<String, Object> map = new HashMap<String, Object>();
		SysParamModel sysParamModel2232 =SysParamServerDict.getInstance().getSysParamModelDictionary().get(6).get(2232);
		if(sysParamModel2232==null||sysParamModel2232.getVal1()==0){
			map.put("result", FAIL);
			map.put("msg", "活动还未开始");
			return new ModelAndView(new FastJsonJsonView(), map);
		}
		ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
		boolean isExist = centerRMIServer.isExistAccount(accountId);
		if(!isExist){
			map.put("result", FAIL);
			map.put("msg", "用户不存在");
			return new ModelAndView(new FastJsonJsonView(), map);
		}
		Date start = null;
		Date end = null;
		if(StringUtils.isNotBlank(startDate)&&StringUtils.isNotBlank(endDate)){
			start = MyDateUtil.getZeroDate(MyDateUtil.parse(startDate, "yyyyMMdd"));
			end = MyDateUtil.getTomorrowZeroDate(MyDateUtil.parse(endDate, "yyyyMMdd"));
			if(start.getTime()>end.getTime()){
				map.put("result", FAIL);
				map.put("msg", "筛选日期有误");
				return new ModelAndView(new FastJsonJsonView(), map);
			}
		}else{
			start = MyDateUtil.getZeroDate(new Date());
			end = MyDateUtil.getTomorrowZeroDate(start);
		}
		String dateLimit = sysParamModel2232.getStr1();
		String[] limits = dateLimit.split("\\|");
		Date startD =  MyDateUtil.getZeroDate(MyDateUtil.parse(limits[0], "yyyy-MM-dd"));
		Date endD = MyDateUtil.getTomorrowZeroDate(MyDateUtil.parse(limits[1], "yyyy-MM-dd"));
		//如果所选的时间要早于活动开始时间
		if(startD.getTime()>start.getTime()){
			start = startD;
		}
		//如果所选的时间要大于活动结束时间
		if(endD.getTime()<end.getTime()){
			end = endD;
		}
		List<AreaLimitJsonModel> areaList = JSON.parseArray(sysParamModel2232.getStr2(), AreaLimitJsonModel.class);
		List<ClubDailyCostModel> list = MongoDBServiceImpl.getInstance().getClubDailyCostModelList( accountId, start, end,sysParamModel2232.getVal2(),0);
		int totalSend = 0;
		MongoDBService mongoDBService = SpringService.getBean(MongoDBService.class);
		MongoTemplate mongoTemplate = mongoDBService.getMongoTemplate();
		for(ClubDailyCostModel model:list){
			if(model.getReceive()==0){
				model.setSendGold(getSendGold((int)model.getCost(), areaList));
				totalSend += model.getSendGold();
				model.setReceive(1);
				MongoDBServiceImpl.getInstance().updateClubDailyCostModel(model, mongoTemplate);
			}
		}
		if(totalSend == 0){
			map.put("result", FAIL);
			map.put("msg", SysParamServerDict.getInstance().replaceGoldTipsWord("暂无可领取的闲逸豆"));
			return new ModelAndView(new FastJsonJsonView(), map);
		}
		try{
			AddGoldResultModel res = centerRMIServer.addAccountGold(accountId, totalSend, false, "俱乐部消耗送豆", EGoldOperateType.CLUB_CONSUME_SENDGOLD);
			if(!res.isSuccess()){
				logger.error("totalSend="+totalSend+"accountId="+accountId+SysParamServerDict.getInstance().replaceGoldTipsWord(" 领取闲逸豆失败，请联系客服处理"));
				map.put("result", FAIL);
				map.put("msg", SysParamServerDict.getInstance().replaceGoldTipsWord("领取闲逸豆失败，请联系客服处理"));
				return new ModelAndView(new FastJsonJsonView(), map);
			}
		}catch(Exception e){
			logger.error("totalSend="+totalSend+"accountId="+accountId,e);
			e.printStackTrace();
		}
		map.put("result", SUCCESS);
		map.put("msg", "恭喜,成功领取"+totalSend+"豆,闲逸愿您游戏愉快~");
		return new ModelAndView(new FastJsonJsonView(), map);
	}
	
}
