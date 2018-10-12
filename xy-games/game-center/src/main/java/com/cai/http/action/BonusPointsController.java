/**
 * 
 */
package com.cai.http.action;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.cai.common.domain.bonuspoints.AccountBonusPointsModel;
import com.cai.common.domain.bonuspoints.BonusPointsExchangeLog;
import com.cai.common.domain.bonuspoints.BonusPointsGoods;
import com.cai.common.domain.bonuspoints.BonusPointsGoodsType;
import com.cai.common.domain.bonuspoints.BonusPointsLog;
import com.cai.common.domain.bonuspoints.ExchangeRankModel;
import com.cai.common.domain.bonuspoints.PlayerAddressModel;
import com.cai.common.util.SpringService;
import com.cai.dao.PublicDAO;
import com.cai.http.security.SignUtil;
import com.cai.service.BonusPointsService;
import com.cai.service.MongoDBService;
import com.cai.service.PublicService;
import com.google.common.collect.Maps;

@Controller
@RequestMapping("/bonus")
public class BonusPointsController {

	// private static Logger logger =
	// LoggerFactory.getLogger(BonusPointsController.class);

	/**
	 * 成功
	 */
	public final static int SUCCESS = 0;
	public final static int FAIL = -1;

	public static final int TYPE_MY_SCORE = 1;
	// 商品列表+商品类型
	public static final int TYPE_GOODS_LIST = 2;
	// 商品详情
	public static final int TYPE_GOODS_DETAIL = 3;
	// 热门兑换
	public static final int TYPE_HOT_EXCHANGE = 4;
	// 兑换排行榜
	public static final int TYPE_EXCHANGE_RANK = 5;
	// 积分流水
	public static final int TYPE_BONUS_POINTS_STREAM = 6;
	// 获取收货地址
	public static final int TYPE_DELIVER_ADDRESS = 7;
	// 设置收货地址
	public static final int TYPE_SET_DELIVER_ADDRESS = 8;
	// 兑换商品
	public static final int TYPE_EXCHANGE_GOODS = 9;
	// 我的订单列表
	public static final int TYPE_ORDER_LIST = 10;
	// 兑换记录跑马灯
	public static final int TYPE_EXCHANGE_RECORD = 11;
	public static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");

	// 商品列表
	@RequestMapping("/mall")
	public void centerpay(HttpServletRequest request, HttpServletResponse response) throws IOException {
		Map<String, Object> resultMap = Maps.newHashMap();
		Map<String, String> params = SignUtil.getParametersHashMap(request);
		String queryType = params.get("queryType");
		response.setContentType("application/json;charset=utf-8");
		response.setCharacterEncoding("UTF-8");
		int type = 0;
		try {
			type = Integer.parseInt(queryType);
		} catch (NumberFormatException e) {
			resultMap.put("msg", "参数异常");
			resultMap.put("result", FAIL);
			response.getWriter().write(JSON.toJSONString(resultMap, SerializerFeature.DisableCircularReferenceDetect));
			return;
			// return new ModelAndView(new FastJsonJsonView(), resultMap);
		}
		if (type == TYPE_MY_SCORE) {
			myBonusPoints(params, resultMap);
		} else if (type == TYPE_GOODS_LIST) {
			goodsList(params, resultMap);
		} else if (type == TYPE_GOODS_DETAIL) {
			goodsDetail(params, resultMap);
		} else if (type == TYPE_BONUS_POINTS_STREAM) {
			bonusPointsStream(params, resultMap);
		} else if (type == TYPE_DELIVER_ADDRESS) {
			myAddress(params, resultMap);
		} else if (type == TYPE_SET_DELIVER_ADDRESS) {
			setAddress(params, resultMap);
		} else if (type == TYPE_EXCHANGE_GOODS) {
			exchangeGoods(params, resultMap);
		} else if (type == TYPE_ORDER_LIST) {
			orderList(params, resultMap);
		} else if (type == TYPE_EXCHANGE_RECORD) {
			exchangeRecord(params, resultMap);
		} else if (type == TYPE_HOT_EXCHANGE) {
			hot(params, resultMap);
		} else if (type == TYPE_EXCHANGE_RANK) {
			rank(params, resultMap);
		}
		response.getWriter().write(JSON.toJSONString(resultMap, SerializerFeature.DisableCircularReferenceDetect));
	}

	public void myBonusPoints(Map<String, String> params, Map<String, Object> resultMap) {
		String user_ID = params.get("userID");
		long userID;
		try {
			userID = Long.parseLong(user_ID);
		} catch (Exception e) {
			resultMap.put("result", FAIL);
			resultMap.put("msg", "参数异常");
			return;
		}
		resultMap.put("score", BonusPointsService.getInstance().getScore(userID));
		resultMap.put("result", SUCCESS);
	}

	public void bonusPointsStream(Map<String, String> params, Map<String, Object> resultMap) {
		String user_ID = params.get("userID");
		String pageIndex = params.get("pageIndex");
		String pageSize = params.get("pageSize");
		long userID;
		int index = 0;
		int size = 5;
		try {
			userID = Long.parseLong(user_ID);
			index = Integer.parseInt(pageIndex);
			size = Integer.parseInt(pageSize);
		} catch (Exception e) {
			resultMap.put("result", FAIL);
			resultMap.put("msg", "参数异常");
			return;
		}
		MongoDBService mongoDBService = SpringService.getBean(MongoDBService.class);
		Query query = new Query();
		query.addCriteria(Criteria.where("accountId").is(userID));
		query.with(new Sort(Direction.DESC, "create_time"));
		query.skip(index * size).limit(size);
		List<BonusPointsLog> list = mongoDBService.getMongoTemplate().find(query, BonusPointsLog.class);
		resultMap.put("data", list);
		AccountBonusPointsModel model = BonusPointsService.getInstance().getAccountBonusPointsModel(userID);
		if (model == null) {
			resultMap.put("historyScore", 0);
			resultMap.put("exchangeScore", 0);
		} else {
			resultMap.put("historyScore", model.getHistory_score());
			resultMap.put("exchangeScore", model.getHistory_score() - model.getScore());
		}

		resultMap.put("result", SUCCESS);
	}

	public void myAddress(Map<String, String> params, Map<String, Object> resultMap) {
		String user_ID = params.get("userID");
		long userID;
		try {
			userID = Long.parseLong(user_ID);
		} catch (Exception e) {
			resultMap.put("result", FAIL);
			resultMap.put("msg", "参数异常");
			return;
		}
		PlayerAddressModel model = BonusPointsService.getInstance().getAccountAddressMap().get(userID);
		if (model == null) {
			resultMap.put("result", FAIL);
			resultMap.put("msg", "无收货地址");
			return;
		}
		resultMap.put("data", model);
		resultMap.put("result", SUCCESS);
	}

	public void setAddress(Map<String, String> params, Map<String, Object> resultMap) {
		String user_ID = params.get("userID");
		String name = params.get("name");
		String mobile = params.get("mobile");
		String postCodeStr = params.get("postCode");
		String address = params.get("address");

		if (StringUtils.isBlank("name") || StringUtils.isBlank("mobile") || StringUtils.isBlank("postCode") || StringUtils.isBlank("address")) {
			resultMap.put("result", FAIL);
			resultMap.put("msg", "参数有误");
			return;
		}
		Pattern p = Pattern.compile("^1[1|3|4|5|6|7|8|9]\\d{9}$");
		Matcher m = p.matcher(mobile);
		if (!m.matches()) {
			resultMap.put("result", FAIL);
			resultMap.put("msg", "手机号有误");
			return;
		}
		long userID;
		try {
			userID = Long.parseLong(user_ID);
		} catch (Exception e) {
			resultMap.put("result", FAIL);
			resultMap.put("msg", "参数异常");
			return;
		}
		PlayerAddressModel model = BonusPointsService.getInstance().getAccountAddressMap().get(userID);
		PublicDAO dao = SpringService.getBean(PublicService.class).getPublicDAO();
		if (model == null) {
			model = new PlayerAddressModel();
			BonusPointsService.getInstance().getAccountAddressMap().put(userID, model);
			model.setAccount_id(userID);
			model.setAddress(address);
			model.setPhone(Long.parseLong(mobile));
			model.setName(name);
			model.setPostcode(Integer.parseInt(postCodeStr));
			dao.insertPlayerAddressModel(model);
		} else {
			model.setAccount_id(userID);
			model.setAddress(address);
			model.setPhone(Long.parseLong(mobile));
			model.setName(name);
			model.setPostcode(Integer.parseInt(postCodeStr));
			dao.updatePlayerAddressModel(model);
		}
		resultMap.put("msg", "保存地址成功");
		resultMap.put("result", SUCCESS);
	}

	public void exchangeGoods(Map<String, String> params, Map<String, Object> resultMap) {
		String user_ID = params.get("userID");
		String goodsIdStr = params.get("goodsId");
		String scoreStr = params.get("score");// 扣除的积分
		String countStr = params.get("count");// 兑换的数量
		String goodsFormat = params.get("goodsFormat");// 商品规格，下拉框选择的
		long userID;
		long score;
		int count;
		int goodsId;
		try {
			userID = Long.parseLong(user_ID);
			goodsId = Integer.parseInt(goodsIdStr);
			score = Long.parseLong(scoreStr);
			count = Integer.parseInt(countStr);
		} catch (Exception e) {
			resultMap.put("result", FAIL);
			resultMap.put("msg", "参数异常");
			return;
		}
		StringBuffer sb = new StringBuffer();
		int result = BonusPointsService.getInstance().exchangeGoods(goodsId, userID, score, count, goodsFormat, sb);
		if (result == 0) {
			resultMap.put("msg", "兑换成功，您的积分还剩" + BonusPointsService.getInstance().getScore(userID));
			resultMap.put("result", SUCCESS);
		} else {
			resultMap.put("result", FAIL);
			resultMap.put("msg", sb.toString());
		}

	}

	public void orderList(Map<String, String> params, Map<String, Object> resultMap) {
		String user_ID = params.get("userID");
		String pageIndex = params.get("pageIndex");
		String pageSize = params.get("pageSize");
		long userID;
		int index = 0;
		int size = 5;
		try {
			userID = Long.parseLong(user_ID);
			index = Integer.parseInt(pageIndex);
			size = Integer.parseInt(pageSize);
		} catch (Exception e) {
			resultMap.put("result", FAIL);
			resultMap.put("msg", "参数异常");
			return;
		}
		MongoDBService mongoDBService = SpringService.getBean(MongoDBService.class);
		Query query = new Query();
		query.addCriteria(Criteria.where("accountId").is(userID));
		query.with(new Sort(Direction.DESC, "create_time"));
		query.skip(index * size).limit(size);
		List<BonusPointsExchangeLog> list = mongoDBService.getMongoTemplate().find(query, BonusPointsExchangeLog.class);
		for (BonusPointsExchangeLog model : list) {
			BonusPointsGoods goods = BonusPointsService.getInstance().getGoodsById(model.getGoodsId());
			if (goods != null) {
				model.setGoods(goods);
			}

		}
		resultMap.put("data", list);
		resultMap.put("result", SUCCESS);
	}

	public void exchangeRecord(Map<String, String> params, Map<String, Object> resultMap) {
		String pageIndex = params.get("pageIndex");
		String pageSize = params.get("pageSize");
		int index = 0;
		int size = 5;
		try {
			index = Integer.parseInt(pageIndex);
			size = Integer.parseInt(pageSize);
		} catch (Exception e) {
			resultMap.put("result", FAIL);
			resultMap.put("msg", "参数异常");
			return;
		}
		MongoDBService mongoDBService = SpringService.getBean(MongoDBService.class);
		Query query = new Query();
		query.with(new Sort(Direction.DESC, "create_time"));
		query.skip(index * size).limit(size);
		List<BonusPointsExchangeLog> list = mongoDBService.getMongoTemplate().find(query, BonusPointsExchangeLog.class);
		List<String> recordList = new ArrayList<>();
		for (BonusPointsExchangeLog log : list) {
			String name = log.getDeliverName().length() > 1 ? log.getDeliverName().substring(0, 1) + "**" : log.getDeliverName() + "**";
			recordList.add(
					"恭喜用户<font color='red'>" + name + "</font>成功兑换了<font color='red'>" + log.getGoodsName() + "</font>*" + log.getCount() + "份!");
		}
		resultMap.put("data", recordList);
		resultMap.put("result", SUCCESS);
	}

	public void rank(Map<String, String> params, Map<String, Object> resultMap) {
		String typeStr = params.get("type");
		int type = Integer.parseInt(typeStr);
		List<ExchangeRankModel> list = new ArrayList<>();
		if (type == 0) {
			list = BonusPointsService.getInstance().getRankList();
		} else {
			List<ExchangeRankModel> allList = BonusPointsService.getInstance().getRankList();
			for (ExchangeRankModel model : allList) {
				if (model.getGoods().getGoods_type() == type) {
					list.add(model);
				}
			}
		}
		resultMap.put("data", list);
		resultMap.put("result", SUCCESS);
	}

	public void hot(Map<String, String> params, Map<String, Object> resultMap) {
		resultMap.put("data", BonusPointsService.getInstance().getHotGoodsList());
		resultMap.put("result", SUCCESS);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void goodsList(Map<String, String> params, Map<String, Object> resultMap) {
		String goodsType = params.get("goodsType");
		String sortType = params.get("sortType");
		List<BonusPointsGoodsType> goodsTypeList = new ArrayList(BonusPointsService.getInstance().getGoodsTypeMap().values());
		resultMap.put("goodsType", goodsTypeList);
		List<BonusPointsGoods> goodsList = new ArrayList<>();
		if (goodsType.equals("0")) {
			goodsList = new ArrayList(BonusPointsService.getInstance().getGoodsMap().values());

		} else {
			int type = Integer.parseInt(goodsType);
			goodsList = BonusPointsService.getInstance().getGoodsListByType(type);
			resultMap.put("goods", goodsList);
		}
		if (goodsList.size() > 0) {
			if (StringUtils.isNotBlank(sortType) && sortType.equals("1")) {
				Collections.sort(goodsList, new Comparator<BonusPointsGoods>() {
					@Override
					public int compare(BonusPointsGoods o1, BonusPointsGoods o2) {
						// TODO Auto-generated method stub
						return o1.getScore() - o2.getScore();
					}
				});
			}
			if (StringUtils.isNotBlank(sortType) && sortType.equals("2")) {
				Collections.sort(goodsList, new Comparator<BonusPointsGoods>() {
					@Override
					public int compare(BonusPointsGoods o1, BonusPointsGoods o2) {
						// TODO Auto-generated method stub
						return o2.getScore() - o1.getScore();
					}
				});
			}
		}
		resultMap.put("goods", goodsList);
		resultMap.put("result", SUCCESS);
	}

	public void goodsDetail(Map<String, String> params, Map<String, Object> resultMap) {
		String goodsId = params.get("goodsId");
		BonusPointsGoods model = BonusPointsService.getInstance().getGoodsMap().get(Integer.parseInt(goodsId));
		resultMap.put("goods", model);
		resultMap.put("result", SUCCESS);
	}
}
