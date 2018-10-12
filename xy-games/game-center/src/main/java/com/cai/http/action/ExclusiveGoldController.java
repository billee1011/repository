/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.http.action;

import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import com.alibaba.fastjson.JSON;
import com.cai.common.constant.RMICmd;
import com.cai.common.define.EGameType;
import com.cai.common.define.ExclusiveSettingStatus;
import com.cai.common.define.XYCode;
import com.cai.common.domain.ExclusiveGoldLogModel;
import com.cai.common.domain.StatusModule;
import com.cai.common.rmi.ICenterRMIServer;
import com.cai.common.rmi.vo.ClubExlusiveTransferVo;
import com.cai.common.util.SpringService;
import com.cai.http.FastJsonJsonView;
import com.cai.http.security.SignUtil;
import com.cai.service.ClubExclusiveService;
import com.cai.service.MongoDBServiceImpl;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import protobuf.clazz.Common.CommonILI;
import protobuf.clazz.s2s.S2SProto.ExclusiveGoldPB;

/**
 * 
 *
 * @author wu_hc date: 2018年2月6日 上午10:07:08 <br/>
 */
@Controller
@RequestMapping("/exclusive")
public final class ExclusiveGoldController {
	private static Logger logger = LoggerFactory.getLogger(ExclusiveGoldController.class);

	@RequestMapping("/transfer")
	public ModelAndView transfer(HttpServletRequest request) {

		Map<String, String> params = SignUtil.getParametersHashMap(request);
		logger.info("来自web端的专属豆转让:{}", params);

		Map<String, Object> resultMap = Maps.newHashMap();
		try {
			long selfAccountId = Long.parseLong(params.get("selfAccountId"));
			long tagetAccountId = Long.parseLong(params.get("tagetAccountId"));
			long value = Long.parseLong(params.get("value"));
			int gameId = Integer.parseInt(params.get("gameId"));
			ClubExlusiveTransferVo vo = ClubExlusiveTransferVo.newVo(selfAccountId, tagetAccountId, value).setGameId(gameId);
			vo = SpringService.getBean(ICenterRMIServer.class).rmiInvoke(RMICmd.CLUB_EXCLUSIVE_TRANSFER, vo);
			resultMap.put("result", vo.getStatus());
			resultMap.put("msg", vo.getDesc());
		} catch (Exception e) {
			resultMap.put("result", XYCode.FAIL);
			resultMap.put("msg", "参数异常");
		}
		return new ModelAndView(new FastJsonJsonView(), resultMap);
	}

	@SuppressWarnings("unchecked")
	@RequestMapping("/sale")
	public ModelAndView saleExclusiveGold(HttpServletRequest request) {
		Map<String, String> params = SignUtil.getParametersHashMap(request);
		ExclusiveGoldLogModel model = new ExclusiveGoldLogModel();
		Map<String, Object> resultMap = Maps.newHashMap();
		try {
			model.setAccount_id(Long.parseLong(params.get("account_id")));
			model.setCreate_time(new Date());
			model.setExclusive_gold(Integer.parseInt(params.get("exclusive_gold")));
			model.setGame_type_index(Integer.parseInt(params.get("game_type_index")));
			model.setOrderSeq(params.get("orderSeq"));
			model.setSalePrice(Integer.parseInt(params.get("salePrice")));
			model.setTarget_account_id(Long.parseLong(params.get("target_account_id")));
			model.setExattr(params.get("exattr") == null ? "" : params.get("exattr"));
			MongoDBServiceImpl.getInstance().getLogQueue().add(model);
			resultMap.put("result", XYCode.SUCCESS);
		} catch (Exception e) {
			resultMap.put("result", XYCode.FAIL);
			resultMap.put("msg", "参数异常");
		}
		return new ModelAndView(new FastJsonJsonView(), resultMap);
	}

	/**
	 * CommonILI[k:gameId,v1:数量,v2:有效期,单位s]
	 * [{k:9,v1:4000,v2:1517885315},{k:11,v1:500,v2:1517885319}, ... ]
	 * 
	 * @see ExclusiveVO
	 * @param request
	 * @return [{gameId:9,num:4000,expire:1517885315,gameName:"斗牛",settings:3},{gameId:11,num:4050,expire:1517885777,gameName:"跑得快",settings:1},
	 *         ... ]
	 */
	@RequestMapping("/detail")
	public ModelAndView detail(HttpServletRequest request) {

		Map<String, String> params = SignUtil.getParametersHashMap(request);
		logger.info("来自web端的专属豆查询:{}", params);

		Map<String, Object> resultMap = Maps.newHashMap();
		try {
			long selfAccountId = Long.parseLong(params.get("accountId"));
			List<ExclusiveGoldPB> exclusiveCommonS = ClubExclusiveService.getInstance().accountExclusiveGold(selfAccountId);
			List<ExclusiveVO> vos = Lists.newArrayListWithCapacity(exclusiveCommonS.size());

			exclusiveCommonS.forEach(c -> {

				// 过期不显示
				if (c.getExpireE() < (System.currentTimeMillis() / 1000L)) {
					return;
				}
				// 不可以送，不可卖,不显示
				final int settings = c.getSettings();
				if (settings > 0) {
					StatusModule status = StatusModule.newWithStatus(settings);
					if (status.statusOR(ExclusiveSettingStatus.NOT_OFFER, ExclusiveSettingStatus.NOT_SELL)) {
						return;
					}
				}

				ExclusiveVO vo = new ExclusiveVO();
				vo.setGameId(c.getGameId());
				vo.setNum(c.getValue());
				vo.setExpire(c.getExpireE());
				vo.setSettings(settings);
				EGameType gameType = EGameType.getEGameType(vo.getGameId());
				if (null != gameType) {
					vo.setGameName(gameType.getName());
				} else {
					vo.setGameName("未知");
				}

				vos.add(vo);
			});
			resultMap.put("result", XYCode.SUCCESS);
			resultMap.put("msg", JSON.toJSONString(vos));
		} catch (Exception e) {
			resultMap.put("result", XYCode.FAIL);
			resultMap.put("msg", "参数异常");
		}
		return new ModelAndView(new FastJsonJsonView(), resultMap);
	}

	final static class ExclusiveVO {
		private int gameId;
		private long num;
		private int expire;
		private String gameName;
		private int settings;

		public int getGameId() {
			return gameId;
		}

		public void setGameId(int gameId) {
			this.gameId = gameId;
		}

		public long getNum() {
			return num;
		}

		public void setNum(long num) {
			this.num = num;
		}

		public int getExpire() {
			return expire;
		}

		public void setExpire(int expire) {
			this.expire = expire;
		}

		public String getGameName() {
			return gameName;
		}

		public void setGameName(String gameName) {
			this.gameName = gameName;
		}

		public int getSettings() {
			return settings;
		}

		public void setSettings(int settings) {
			this.settings = settings;
		}

	}

	// test
	public static void main(String[] args) {
		List<CommonILI> list = Lists.newArrayList();
		List<Integer> gameIds = Lists.newArrayList(9, 11);
		for (int i : gameIds) {
			list.add(CommonILI.newBuilder().setK(i).setV1(4).setV2(5).build());
		}
		List<ExclusiveVO> vos = Lists.newArrayListWithCapacity(list.size());
		//
		// StringBuilder sb = new StringBuilder();
		// sb.append('[');
		// list.forEach(c -> {
		// String jsonFormat = JsonFormat.printToString(c);
		// sb.append(jsonFormat);
		// sb.append(',');
		// });
		// if (sb.length() > 0) {
		// sb.deleteCharAt(sb.length() - 1);
		// }
		// sb.append(']');

		list.forEach(c -> {
			ExclusiveVO vo = new ExclusiveVO();
			vo.setGameId(c.getK());
			vo.setNum(c.getV1());
			vo.setExpire(c.getV2());
			vo.setGameName(EGameType.getEGameType(vo.getGameId()).getName());
			vos.add(vo);
		});
		System.out.println(JSON.toJSONString(vos));
	}
}
