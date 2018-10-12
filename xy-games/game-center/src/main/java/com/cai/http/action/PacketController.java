package com.cai.http.action;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import com.cai.common.constant.RedisConstant;
import com.cai.common.define.EGameType;
import com.cai.common.define.ERedisTopicType;
import com.cai.common.domain.SysParamModel;
import com.cai.common.domain.log.UseRedPacketLogModel;
import com.cai.common.util.SpringService;
import com.cai.dictionary.SysParamServerDict;
import com.cai.http.FastJsonJsonView;
import com.cai.http.security.SignUtil;
import com.cai.redis.service.RedisService;
import com.cai.service.MongoDBServiceImpl;
import com.cai.service.RedisServiceImpl;

import javolution.util.FastMap;
import protobuf.redis.ProtoRedis.RedisResponse;
import protobuf.redis.ProtoRedis.RedisResponse.RsResponseType;
import protobuf.redis.ProtoRedis.RsDictUpdateResponse;
import protobuf.redis.ProtoRedis.RsDictUpdateResponse.RsDictType;

/**
 * 
 *
 * @author zhanglong date: 2018年4月20日 上午11:29:22
 */
@Controller
@RequestMapping("/packet")
public class PacketController {

	public static final int FAIL = 0;

	public static final int SUCCESS = 1;

	@RequestMapping("/verify")
	public ModelAndView handle(HttpServletRequest request, HttpServletResponse response) {
		Map<String, String> params = SignUtil.getParametersHashMap(request);
		String order_id = params.get("order_id");
		String account_id = params.get("account_id");
		String money = params.get("money");
		Map<String, Object> resultMap = new HashMap<String, Object>();
		String orderId = "";
		long accountId = 0;
		int moneyValue = 0;
		try {
			if (StringUtils.isNotBlank(account_id)) {
				accountId = Long.parseLong(account_id);
			}
			if (StringUtils.isNotBlank(order_id)) {
				orderId = order_id;
			}
			if (StringUtils.isNotBlank(money)) {
				moneyValue = Integer.parseInt(money);
			}
		} catch (NumberFormatException e) {
			resultMap.put("msg", "参数异常");
			resultMap.put("result", FAIL);
			return new ModelAndView(new FastJsonJsonView(), resultMap);
		}

		UseRedPacketLogModel model = MongoDBServiceImpl.getInstance().getUseRedPacketLog(orderId);
		if (model == null) {
			resultMap.put("msg", "没有订单");
			resultMap.put("result", FAIL);
			return new ModelAndView(new FastJsonJsonView(), resultMap);
		}
		if (accountId != model.getAccountId()) {
			resultMap.put("msg", "玩家帐号不正确");
			resultMap.put("result", FAIL);
			return new ModelAndView(new FastJsonJsonView(), resultMap);
		}

		if (model.getState() != 1) {
			resultMap.put("msg", "订单已完成");
			resultMap.put("result", FAIL);
			return new ModelAndView(new FastJsonJsonView(), resultMap);
		}

		if (moneyValue != model.getMoney()) {
			resultMap.put("msg", "红包金额不正确");
			resultMap.put("result", FAIL);
			return new ModelAndView(new FastJsonJsonView(), resultMap);
		}

		resultMap.put("result", SUCCESS);
		return new ModelAndView(new FastJsonJsonView(), resultMap);
	}

	@RequestMapping("/useswitch")
	public ModelAndView useSwitch(HttpServletRequest request, HttpServletResponse response) {
		Map<String, String> params = SignUtil.getParametersHashMap(request);
		String state = params.get("state");
		int switchState = 0;
		Map<String, Object> resultMap = new HashMap<String, Object>();
		try {
			if (StringUtils.isNotBlank(state)) {
				switchState = Integer.parseInt(state);
			}
		} catch (NumberFormatException e) {
			resultMap.put("msg", "参数异常");
			resultMap.put("result", FAIL);
			return new ModelAndView(new FastJsonJsonView(), resultMap);
		}
		if (switchState != 0 && switchState != 1) {
			resultMap.put("msg", "参数错误");
			resultMap.put("result", FAIL);
			return new ModelAndView(new FastJsonJsonView(), resultMap);
		}

		SysParamModel sysParamModel = SysParamServerDict.getInstance().getSysParamModelDictionaryByGameId(EGameType.DT.getId()).get(2302);
		if (sysParamModel == null) {
			resultMap.put("msg", "后台没有添加该开关:6-2302");
			resultMap.put("result", FAIL);
			return new ModelAndView(new FastJsonJsonView(), resultMap);
		}
		int oldState = sysParamModel.getVal2();
		if (switchState != oldState) {
			try {
				// 放入redis缓存
				FastMap<Integer, FastMap<Integer, SysParamModel>> sysParamModelDictionary = SysParamServerDict.getInstance()
						.getSysParamModelDictionary();
				sysParamModel.setVal2(switchState);
				RedisService redisService = SpringService.getBean(RedisService.class);
				redisService.hSet(RedisConstant.DICT, RedisConstant.DICT_SYSPARAM_SERVER, sysParamModelDictionary);
				RedisResponse.Builder redisResponseBuilder = RedisResponse.newBuilder();
				redisResponseBuilder.setRsResponseType(RsResponseType.DICT_UPDATE);
				//
				RsDictUpdateResponse.Builder rsDictUpdateResponseBuilder = RsDictUpdateResponse.newBuilder();
				rsDictUpdateResponseBuilder.setRsDictType(RsDictType.SYS_PARAM_SERVER);
				//
				redisResponseBuilder.setRsDictUpdateResponse(rsDictUpdateResponseBuilder);
				RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(), ERedisTopicType.topProxAndLogic);

				resultMap.put("result", SUCCESS);
				resultMap.put("state", switchState);
			} catch (Exception e) {
				resultMap.put("msg", "设置异常");
				resultMap.put("result", FAIL);
				return new ModelAndView(new FastJsonJsonView(), resultMap);
			}
		} else {
			resultMap.put("msg", "设置的新状态和旧状态相同");
			resultMap.put("result", FAIL);
			return new ModelAndView(new FastJsonJsonView(), resultMap);
		}

		return new ModelAndView(new FastJsonJsonView(), resultMap);
	}
}
