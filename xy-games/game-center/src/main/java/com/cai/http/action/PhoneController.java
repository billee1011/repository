package com.cai.http.action;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import com.cai.common.constant.RMICmd;
import com.cai.common.define.IPhoneOperateType;
import com.cai.common.domain.Account;
import com.cai.common.domain.AccountMobileModel;
import com.cai.common.rmi.vo.UserPhoneRMIVo;
import com.cai.common.util.Pair;
import com.cai.common.util.SpringService;
import com.cai.http.FastJsonJsonView;
import com.cai.http.model.ErrorCode;
import com.cai.http.security.SignUtil;
import com.cai.rmi.impl.CenterRMIServerImpl;
import com.cai.service.PhoneService;
import com.cai.service.PublicServiceImpl;

@Controller
@RequestMapping("/phone")
public class PhoneController {
	public static final int FAIL = 0;

	public static final int SUCCESS = 1;

	// 绑定手机到账号上
	public static final int TYPE_BIND = 1;
	// 解绑
	public static final int TYPE_UNBIND = 2;
	// 查看绑定状态
	public static final int TYPE_BIND_STATE = 3;
	// 手机号登录
	public static final int TYPE_PHONE_LOGIN = 4;
	// 通过手机号绑定公众号
	public static final int TYPE_BAND_WX_OFFICAL = 5;
	// 通过手机号查看是否已经绑定公众号
	public static final int TYPE_BAND_WX_OFFICAL_STATE = 6;
	//查看公众号openId是否绑定过账号
	public static final int TYPE_WX_BAND_STATE = 7;
	//解绑公众号绑定
	public static final int TYPE_UNBIND_OPENID = 8;
	

	// 钻石黄金推广员----我的会员代理

	@RequestMapping("/operate")
	public ModelAndView handle(HttpServletRequest request, HttpServletResponse response) {
		Map<String, String> params = SignUtil.getParametersHashMap(request);
		String queryType = params.get("queryType");
		Map<String, Object> resultMap = new HashMap<String, Object>();
		String accountId = params.get("account_id");
		String mobile = params.get("mobile");
		String openId = params.get("openId");
		int type;
		long account_id = 0;
		try {
			if (StringUtils.isNotBlank(accountId)) {
				account_id = Long.parseLong(accountId);
			}
			type = Integer.parseInt(queryType);
		} catch (NumberFormatException e) {
			resultMap.put("msg", "参数异常");
			resultMap.put("result", FAIL);
			return new ModelAndView(new FastJsonJsonView(), resultMap);
		}

		if (type == TYPE_BIND) {
			bind(mobile, account_id, resultMap);
		} else if (type == TYPE_UNBIND) {
			unbind(mobile, account_id, resultMap);
		} else if (type == TYPE_BIND_STATE) {
			bindState(mobile, account_id, resultMap);
		} else if (type == TYPE_PHONE_LOGIN) {
			phoneLogin(mobile, resultMap);
		} else if (type == TYPE_BAND_WX_OFFICAL) {
			bindWx(mobile, openId, resultMap);
		}else if (type == TYPE_BAND_WX_OFFICAL_STATE) {
			bindWxState(mobile, openId, resultMap);
		}else if (type == TYPE_WX_BAND_STATE) {
			bindOpenIdState(openId, resultMap);
		}else if (type == TYPE_UNBIND_OPENID) {
			unBindOpenIdState(openId,mobile, resultMap);
		}

		return new ModelAndView(new FastJsonJsonView(), resultMap);
	}

	private void bind(String mobile, long account_id, Map<String, Object> resultMap) {
		CenterRMIServerImpl centerRMIServer = SpringService.getBean(CenterRMIServerImpl.class);
		UserPhoneRMIVo vo = UserPhoneRMIVo.newVo(IPhoneOperateType.BIND, account_id, mobile);
		Pair<Integer, String> r = centerRMIServer.rmiInvoke(RMICmd.ACCOUNT_PHONE, vo);
		if (r.getFirst().intValue() == SUCCESS) {
			resultMap.put("result", SUCCESS);
		} else {
			resultMap.put("result", FAIL);
			resultMap.put("msg", r.getSecond());
		}
	}

	private void unbind(String mobile, long account_id, Map<String, Object> resultMap) {
		CenterRMIServerImpl centerRMIServer = SpringService.getBean(CenterRMIServerImpl.class);
		UserPhoneRMIVo vo = UserPhoneRMIVo.newVo(IPhoneOperateType.UN_BIND, account_id, mobile);
		Pair<Integer, String> r = centerRMIServer.rmiInvoke(RMICmd.ACCOUNT_PHONE, vo);
		if (r.getFirst().intValue() == SUCCESS) {
			resultMap.put("result", SUCCESS);
		} else {
			resultMap.put("result", FAIL);
			resultMap.put("msg", r.getSecond());
		}
	}

	private void bindState(String mobile, long account_id, Map<String, Object> resultMap) {
		CenterRMIServerImpl centerRMIServer = SpringService.getBean(CenterRMIServerImpl.class);
		UserPhoneRMIVo vo = UserPhoneRMIVo.newVo(IPhoneOperateType.BIND_INFO, account_id, mobile);
		Pair<Integer, String> r = centerRMIServer.rmiInvoke(RMICmd.ACCOUNT_PHONE, vo);
		if (r.getFirst().intValue() == SUCCESS) {
			resultMap.put("result", SUCCESS);
		} else {
			resultMap.put("result", FAIL);
			resultMap.put("msg", r.getSecond());
		}
	}

	private void phoneLogin(String mobile, Map<String, Object> resultMap) {
		Optional<AccountMobileModel> phoneOpt = PhoneService.getInstance().getPhoneModelByMobileNum(mobile);
		if (phoneOpt.isPresent()) {
			Account account = PublicServiceImpl.getInstance().getAccount(phoneOpt.get().getAccount_id());
			if (account == null || account.getAccountWeixinModel() == null) {
				resultMap.put("result", FAIL);
				resultMap.put("msg", ErrorCode.ROLE_FIND_FAIL);
				return;
			}
			resultMap.put("userId", account.getAccountWeixinModel().getAccount_id());
			resultMap.put("result", SUCCESS);
			resultMap.put("nickName", account.getAccountWeixinModel().getNickname());
			resultMap.put("isAgent", account.getAccountModel().getIs_agent());
			resultMap.put("headUrl", account.getAccountWeixinModel().getHeadimgurl());
			resultMap.put("gold", account.getAccountModel().getGold());
			resultMap.put("upAgent", account.getAccountModel().getUp_proxy());
			resultMap.put("unionid", account.getAccountWeixinModel().getUnionid());
			resultMap.put("totalConsum", account.getAccountModel().getConsum_total());
			resultMap.put("is_rebate", account.getAccountModel().getIs_rebate());
			resultMap.put("banned", account.getAccountModel().getBanned());
			resultMap.put("mobile", account.getAccountModel().getMobile_phone());
			if (account.getHallRecommendModel() != null) {
				resultMap.put("hall_recommend_level", account.getHallRecommendModel().getRecommend_level());
			} else {
				resultMap.put("hall_recommend_level", 0);
			}
		}
	}
	private void bindWx(String mobile, String openId, Map<String, Object> resultMap) {
		UserPhoneRMIVo vo = UserPhoneRMIVo.newVo(IPhoneOperateType.BIND_INFO, 0, mobile);
		CenterRMIServerImpl centerRMIServer = SpringService.getBean(CenterRMIServerImpl.class);
		Pair<Integer, String> r = centerRMIServer.rmiInvoke(RMICmd.ACCOUNT_PHONE, vo);
		if (r.getFirst() == SUCCESS) {
			Map<String, String> map = new HashMap<>();
			map.put("mobile", mobile);
			map.put("openId", openId);
			map.put("accountId", r.getSecond());
			int res = centerRMIServer.rmiInvoke(RMICmd.BIND_WX_OFFICAL, map);
			if (res == SUCCESS) {
				resultMap.put("result", SUCCESS);
			} else {
				resultMap.put("result", FAIL);
				resultMap.put("msg", r.getSecond());
			}
		}else{
			resultMap.put("result", FAIL);
			resultMap.put("msg", "请先在游戏内绑定您的手机号再来使用此功能！");
		}
	}
	private void bindWxState(String mobile, String openId, Map<String, Object> resultMap) {
		UserPhoneRMIVo vo = UserPhoneRMIVo.newVo(IPhoneOperateType.BIND_INFO, 0, mobile);
		CenterRMIServerImpl centerRMIServer = SpringService.getBean(CenterRMIServerImpl.class);
		Pair<Integer, String> r = centerRMIServer.rmiInvoke(RMICmd.ACCOUNT_PHONE, vo);
		if (r.getFirst() == SUCCESS) {
			Map<String, String> map = new HashMap<>();
			map.put("mobile", mobile);
			map.put("openId", openId);
			map.put("accountId", r.getSecond());
			map.put("type", "1");
			int res = centerRMIServer.rmiInvoke(RMICmd.BIND_WX_OFFICAL, map);
			if (res == SUCCESS) {
				resultMap.put("result", SUCCESS);
			} else {
				resultMap.put("result", FAIL);
				resultMap.put("msg", r.getSecond());
			}
		}else{
			resultMap.put("result", FAIL);
			resultMap.put("msg", "请先在游戏内绑定您的手机号再来使用此功能！");
		}
	}
	private void bindOpenIdState(String openId, Map<String, Object> resultMap) {
		CenterRMIServerImpl centerRMIServer = SpringService.getBean(CenterRMIServerImpl.class);
		Map<String, String> map = new HashMap<>();
		map.put("openId", openId);
		map.put("type", "2");
		int res = centerRMIServer.rmiInvoke(RMICmd.BIND_WX_OFFICAL, map);
		if (res == SUCCESS) {
			resultMap.put("result", SUCCESS);
		} else {
			resultMap.put("result", FAIL);
		}
	}
	private void unBindOpenIdState(String openId,String mobile, Map<String, Object> resultMap) {
		CenterRMIServerImpl centerRMIServer = SpringService.getBean(CenterRMIServerImpl.class);
		Map<String, String> map = new HashMap<>();
		map.put("openId", openId);
		map.put("type", "3");
		map.put("mobile", mobile);
		int res = centerRMIServer.rmiInvoke(RMICmd.BIND_WX_OFFICAL, map);
		if (res == SUCCESS) {
			resultMap.put("result", SUCCESS);
		} else {
			resultMap.put("result", FAIL);
		}
	}
}
