/**
 * 
 */
package com.cai.http.action;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import com.cai.common.domain.Account;
import com.cai.common.domain.RedActivityModel;
import com.cai.common.domain.RedPackageRankModel;
import com.cai.common.util.SpringService;
import com.cai.http.FastJsonJsonView;
import com.cai.http.security.SignUtil;
import com.cai.rmi.impl.CenterRMIServerImpl;
import com.cai.service.MongoDBServiceImpl;
import com.cai.service.RankServiceImp;
import com.cai.service.RedPackageServiceImp;
import com.cai.service.RedPackageServiceImp.RedPackageActivity;
import com.google.common.collect.Maps;

@Controller
@RequestMapping("/active")
public class RedPackageController {

	/**
	 * 成功
	 */
	public final static int SUCCESS = 0;

	/**
	 * 失败
	 */
	public final static int FAIL = -1;
	//红包活动排行榜
	private static final int TYPE_RED_PACKAGE_RANK = 1;
	// 红包领取记录
	public static final int TYPE_RECOMMEND_OUT = 2;
	//发送红包
	public static final int TYPE_MY_WITHDRAWS = 3;
	//判断能否提现
	public static final int TYPE_JUDGE_WITHDRAWS = 4;
	//首页数据
	public static final int TYPE_INDEX = 5;
	//剩余奖品
	public static final int TYPE_REMAIN_PRIZE = 6;
	
	public static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
	// 下级钻石黄金推广员详情
	private static Logger logger = LoggerFactory.getLogger(RedPackageController.class);
	
	@RequestMapping("/detail")
	public ModelAndView centerpay(HttpServletRequest request) {
		Map<String, Object> resultMap = Maps.newHashMap();
		Map<String, String> params = SignUtil.getParametersHashMap(request);
		String queryType = params.get("queryType");
		int type;
		try {
			type = Integer.parseInt(queryType);
		} catch (NumberFormatException e) {
			resultMap.put("msg", "参数异常");
			resultMap.put("result", FAIL);
			return new ModelAndView(new FastJsonJsonView(), resultMap);
		}
	   if (type == TYPE_RED_PACKAGE_RANK) {
			rank(params, resultMap);
		}else if (type == TYPE_RECOMMEND_OUT) {
			record(params, resultMap);
		}else if (type == TYPE_MY_WITHDRAWS) {
			drawCash(params, resultMap);
		}else if (type == TYPE_JUDGE_WITHDRAWS) {
			judgeDrawCash(params, resultMap);
		}else if (type == TYPE_INDEX) {
			index(params, resultMap);
		}else if (type == TYPE_REMAIN_PRIZE) {
			remainPrize(params, resultMap);
		}
		return new ModelAndView(new FastJsonJsonView(), resultMap);
	}
	/**
	 * 排行榜
	 * 
	 * @param params
	 * @param resultMap
	 */
	private void rank(Map<String, String> params, Map<String, Object> resultMap) {
		List<RedPackageRankModel> rankList = RankServiceImp.getInstance().getRedPackageRankList();
		if(rankList!=null&&rankList.size()>0){
			resultMap.put("result", SUCCESS);
			resultMap.put("data", rankList);
		}else{
			resultMap.put("result", FAIL);
			resultMap.put("msg","活动尚未开始");
		}
		
	}
	
	private void judgeDrawCash(Map<String, String> params, Map<String, Object> resultMap){
		String user_ID = params.get("userID");
		String money = params.get("money");
		if(StringUtils.isBlank(user_ID)||StringUtils.isBlank(money)){
			resultMap.put("result", FAIL);
			resultMap.put("msg","参数有误");
			return;
		}
		long accountId = 0;
		int cash = 0;
		try{
			accountId = Long.parseLong(user_ID);
			cash = Double.valueOf(money).intValue();// Integer.valueOf(money);//Integer.parseInt(money);
		}catch(Exception e){
			resultMap.put("result", FAIL);
			resultMap.put("msg","参数有误");
			return;
		}
		
		if(cash<100){
			resultMap.put("result", FAIL);
			resultMap.put("msg","发送的红包金额最低不能低于1元");
			return;
		}
		CenterRMIServerImpl centerRMIServer = SpringService.getBean(CenterRMIServerImpl.class);
		Account account = centerRMIServer.getAccount(accountId);
		if(account==null){
			resultMap.put("result", FAIL);
			resultMap.put("msg","用户不存在");
			return;
		}
		long current=System.currentTimeMillis();//当前时间毫秒数
		long zero=current/(1000*3600*24)*(1000*3600*24)-TimeZone.getDefault().getRawOffset();
		if(account.getShareTime()<zero){
			resultMap.put("result", FAIL);
			resultMap.put("msg","请先到闲逸棋牌游戏app内分享活动到朋友圈再来领取红包");
			return;
		}
		RedActivityModel redActivityModel = null;
			redActivityModel = account.getRedActivityModel();
		if(redActivityModel == null){
			resultMap.put("result", FAIL);
			resultMap.put("msg","余额不足");
			return;
		}
		if((redActivityModel.getAll_money()-redActivityModel.getReceive_money())<cash){
			resultMap.put("result", FAIL);
			resultMap.put("msg","余额不足");
			return;
		}
		resultMap.put("result", SUCCESS);
		resultMap.put("msg","ok");
	}
	private void index(Map<String, String> params, Map<String, Object> resultMap){
		String user_ID = params.get("userID");
		if(StringUtils.isBlank(user_ID)){
			resultMap.put("result", FAIL);
			resultMap.put("msg","参数有误");
			return;
		}
		long accountId = Long.parseLong(user_ID);
		CenterRMIServerImpl centerRMIServer = SpringService.getBean(CenterRMIServerImpl.class);
		Account account = centerRMIServer.getAccount(accountId);
		resultMap.put("result", SUCCESS);
		resultMap.put("msg","ok");
		resultMap.put("userId", accountId);
		resultMap.put("icon", account.getAccountWeixinModel()!=null?account.getAccountWeixinModel().getHeadimgurl():"");
		resultMap.put("nickName", account.getAccountWeixinModel()!=null?account.getAccountWeixinModel().getNickname():"-");
		resultMap.put("total", account.getRedActivityModel().getAll_money());// 总计红包金额
		resultMap.put("remainMoney", account.getRedActivityModel().getAll_money()
				-account.getRedActivityModel().getReceive_money());//可提现金额
		
	}
	private void remainPrize(Map<String, String> params, Map<String, Object> resultMap){
		String active_type = params.get("type");
		int type = 1;
		try{
			type = Integer.parseInt(active_type);
		}catch(Exception e){
			type = 1;
		}
		ConcurrentHashMap<Integer, RedPackageActivity> map = RedPackageServiceImp.getInstance().getRedPackageTypeMap();
		resultMap.put("result", SUCCESS);
		resultMap.put("msg","ok");
		resultMap.put("remainCount", map.get(type).getRedPackageMap().size());
		resultMap.put("realTotal", map.get(type).total);
		
	}
	private void drawCash(Map<String, String> params, Map<String, Object> resultMap){
		String user_ID = params.get("userID");
		String money = params.get("money");
		if(StringUtils.isBlank(user_ID)||StringUtils.isBlank(money)){
			resultMap.put("result", FAIL);
			resultMap.put("msg","参数有误");
			return;
		}
		long accountId = Long.parseLong(user_ID);
		int cash =  Double.valueOf(money).intValue();
		if(cash<=0){
			resultMap.put("result", FAIL);
			resultMap.put("msg","参数有误");
			return;
		}
		CenterRMIServerImpl centerRMIServer = SpringService.getBean(CenterRMIServerImpl.class);
		Account account = centerRMIServer.getAccount(accountId);
//		if(account.getAccountModel().getIs_rebate()!=1){
//			resultMap.put("result", FAIL);
//			resultMap.put("msg","请先开通返利");
//			return;
//		}
		if(account == null){
			resultMap.put("result", FAIL);
			resultMap.put("msg","用户不存在");
			return;
		}
		RedActivityModel redActivityModel = account.getRedActivityModel();
		if(redActivityModel == null){
			resultMap.put("result", FAIL);
			resultMap.put("msg","用户没有红包数据");
			return;
		}
		if((redActivityModel.getAll_money()-redActivityModel.getReceive_money())<cash){
			resultMap.put("result", FAIL);
			resultMap.put("msg","余额不足");
			return;
		}
		int remainMoney = account.getRedActivityModel().getAll_money() - account.getRedActivityModel().getReceive_money()-cash;
		boolean operate = centerRMIServer.operateRedActivityModel(accountId, cash, 2,0);
		MongoDBServiceImpl.getInstance().red_package_record_log(accountId, cash,
				account.getAccountWeixinModel()!=null?account.getAccountWeixinModel().getNickname():"-",remainMoney);//领取记录入库
		if(operate){
			resultMap.put("result", SUCCESS);
			resultMap.put("msg","红包发送成功，请到闲逸互娱公众号查收");
			return;
		}else{
			logger.error(accountId +" 异常bug，扣款失败 "+cash);
			resultMap.put("result", FAIL);
			resultMap.put("msg","异常");
			return;
		}
		
	}
	private void record(Map<String, String> params, Map<String, Object> resultMap){
		String user_ID = params.get("userID");
		if(StringUtils.isBlank(user_ID)){
			resultMap.put("result", FAIL);
			resultMap.put("msg","参数有误");
			return;
		}
		long accountId = Long.parseLong(user_ID);
		CenterRMIServerImpl centerRMIServer = SpringService.getBean(CenterRMIServerImpl.class);
		Account account = centerRMIServer.getAccount(accountId);
		
	}
	
	
}
