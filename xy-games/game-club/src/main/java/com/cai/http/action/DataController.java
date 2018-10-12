package com.cai.http.action;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import com.cai.common.constant.AccountConstant;
import com.cai.common.define.EGoldOperateType;
import com.cai.common.define.EMoneyOperateType;
import com.cai.common.define.EPtType;
import com.cai.common.define.ERedisTopicType;
import com.cai.common.domain.Account;
import com.cai.common.domain.AccountModel;
import com.cai.common.domain.AccountWeixinModel;
import com.cai.common.domain.OldUserModel;
import com.cai.common.rmi.ICenterRMIServer;
import com.cai.common.util.EmojiFilter;
import com.cai.common.util.ModelToRedisUtil;
import com.cai.common.util.MyStringUtil;
import com.cai.common.util.SpringService;
import com.cai.http.FastJsonJsonView;
import com.cai.redis.service.RedisService;
import com.cai.service.ClubDaoService;

import protobuf.redis.ProtoRedis.RedisResponse;
import protobuf.redis.ProtoRedis.RedisResponse.RsResponseType;
import protobuf.redis.ProtoRedis.RsAccountModelResponse;
import protobuf.redis.ProtoRedis.RsAccountResponse;
import protobuf.redis.ProtoRedis.RsAccountWeixinModelResponse;

@Controller
@RequestMapping("/data")
public class DataController {

	private final static Logger logger = LoggerFactory.getLogger(DataController.class);

	public static final int FAIL = -1;
	public static final int SUCCESS = 1;

	public static int updateKey = 123456;

	@RequestMapping("/key")
	public ModelAndView list(HttpServletRequest request, HttpServletResponse response) {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("result", SUCCESS);
		map.put("updateKey", updateKey);

		return new ModelAndView(new FastJsonJsonView(), map);
	}

	@RequestMapping("/hunan")
	public void data(HttpServletRequest request, HttpServletResponse response, int updateKey) {

		if (updateKey != DataController.updateKey) {
			return;
		}

		if (!isUpdate.compareAndSet(false, true)) {
			logger.error("数据正在导入中");
			return;
		}

		updateData(6,7,"getHuNanUser","updateHunanStatus");
	}
	
	@RequestMapping("/lhq")
	public void lhq(HttpServletRequest request, HttpServletResponse response, int updateKey) {

		if (updateKey != DataController.updateKey) {
			return;
		}

		if (!isUpdate.compareAndSet(false, true)) {
			logger.error("数据正在导入中");
			return;
		}

		updateData(6,7,"getLiuHuQiangUser","updateLiuHuQiangStatus");
	}
	
	@RequestMapping("/fls")
	public void fls(HttpServletRequest request, HttpServletResponse response, int updateKey) {

		if (updateKey != DataController.updateKey) {
			return;
		}

		if (!isUpdate.compareAndSet(false, true)) {
			logger.error("数据正在导入中");
			return;
		}

		updateData(10,10,"getFlsUser","updateFlsStatus");
	}

	private static ExecutorService services = null;

	public static final int THREAD_COUNT = 16;

	public static final Map<String, OldUserModel> accounts = new ConcurrentHashMap<>();

	public static AtomicBoolean isUpdate = new AtomicBoolean(false);

	public static AtomicInteger timer = new AtomicInteger(0);

	public static class OldDataRunable implements Runnable {
		public final List<OldUserModel> temps;

		public final float cardProxyRate;

		public final float cardRate;
		
		public final String updateName;

		public OldDataRunable(List<OldUserModel> pdks, float cardProxyRate, float cardNormalRate,String updateName) {
			temps = pdks;
			this.cardProxyRate = cardProxyRate;
			this.cardRate = cardNormalRate;
			this.updateName = updateName;
		}

		@Override
		public void run() {
			RedisService redisService = SpringService.getBean(RedisService.class);

			final ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
			temps.forEach((oldUserModel) -> {
				try {

					String accounName = EPtType.WX.getId() + "_" + oldUserModel.getUnionid();
					Account account = centerRMIServer.getAndCreateAccount(EPtType.WX.getId(), accounName, "127.0.0.1", "0", "1.0.0", -1);

					String nickname = oldUserModel.getNickname();
					// nickname转码，过滤mysql识别不了的
					nickname = EmojiFilter.filterEmoji(nickname);
					// 长度控制
					nickname = MyStringUtil.substringByLength(nickname, AccountConstant.NICK_NAME_LEN);

					String sex = String.valueOf(oldUserModel.getSex());
					String headimgurl = oldUserModel.getHeadImg();
					String unionid = oldUserModel.getUnionid();// 全平台唯一id

					if (account == null) {
						return;
					}

					if (oldUserModel.getMoney() > 1000) {
						int money = (oldUserModel.getMoney() - 1000) * 40;
						centerRMIServer.addAccountMoney(account.getAccount_id(), money, true, "合并旧版数据", EMoneyOperateType.ADD_MONEY);
					}

					if (oldUserModel.getCard() >= 5 || oldUserModel.getIsagent() > 0) {
						if (oldUserModel.getIsagent() > 0) {
							centerRMIServer.addAccountGold(account.getAccount_id(), (int) Math.ceil(oldUserModel.getCard() * cardProxyRate), true,
									"合并旧版数据", EGoldOperateType.OSS_OPERATE);
						} else {
							centerRMIServer.addAccountGold(account.getAccount_id(), (int) Math.ceil(oldUserModel.getCard() * cardRate), true,
									"合并旧版数据", EGoldOperateType.OSS_OPERATE);
						}
					}

					AccountModel accountModel = account.getAccountModel();
					
					//在旧平台是代理，新平台不是代理的，设置为代理
					if (oldUserModel.getIsagent() > 0 && accountModel.getIs_agent() <= 0) {
						RsAccountModelResponse.Builder rsAccountModelResponseBuilder = RsAccountModelResponse.newBuilder();
						rsAccountModelResponseBuilder.setIsAgent(1);
						rsAccountModelResponseBuilder.setAccountId(account.getAccount_id());
						rsAccountModelResponseBuilder.setHallRecommentId(5848);
						rsAccountModelResponseBuilder.setHallRecommentLevel(2);
						centerRMIServer.ossModifyAccountModel(rsAccountModelResponseBuilder.build());
					}
					//
					// // 微信相关的
					AccountWeixinModel accountWeixinModel = account.getAccountWeixinModel();
					if (StringUtils.isEmpty(accountWeixinModel.getUnionid())) {

						accountWeixinModel.setAccess_token(
								StringUtils.isEmpty(accountWeixinModel.getAccess_token()) ? "" : accountWeixinModel.getAccess_token());
						accountWeixinModel.setRefresh_token(
								StringUtils.isEmpty(accountWeixinModel.getRefresh_token()) ? "" : accountWeixinModel.getRefresh_token());
						accountWeixinModel.setOpenid(StringUtils.isEmpty(accountWeixinModel.getOpenid()) ? "" : accountWeixinModel.getOpenid());
						accountWeixinModel
								.setScope(StringUtils.isEmpty(accountWeixinModel.getScope()) ? "snsapi_userinfo" : accountWeixinModel.getScope());
						accountWeixinModel.setUnionid(unionid);
						accountWeixinModel
								.setNickname(StringUtils.isEmpty(accountWeixinModel.getNickname()) ? nickname : accountWeixinModel.getNickname());
						accountWeixinModel.setSex(StringUtils.isEmpty(accountWeixinModel.getSex()) ? sex : accountWeixinModel.getSex());
						accountWeixinModel.setProvince(StringUtils.isEmpty(accountWeixinModel.getProvince()) ? "" : accountWeixinModel.getProvince());
						accountWeixinModel.setCity(StringUtils.isEmpty(accountWeixinModel.getCity()) ? "" : accountWeixinModel.getCity());

						accountWeixinModel.setCountry(StringUtils.isEmpty(accountWeixinModel.getCountry()) ? "" : accountWeixinModel.getCountry());
						accountWeixinModel.setHeadimgurl(
								StringUtils.isEmpty(accountWeixinModel.getHeadimgurl()) ? headimgurl : accountWeixinModel.getHeadimgurl());
						accountWeixinModel
								.setPrivilege(StringUtils.isEmpty(accountWeixinModel.getPrivilege()) ? "" : accountWeixinModel.getPrivilege());
						accountWeixinModel.setLast_flush_time(
								accountWeixinModel.getLast_flush_time() == null ? new Date() : accountWeixinModel.getLast_flush_time());
						accountWeixinModel
								.setSelf_token(StringUtils.isEmpty(accountWeixinModel.getSelf_token()) ? "" : accountWeixinModel.getSelf_token());
						accountWeixinModel.setLast_false_self_token(
								accountWeixinModel.getLast_false_self_token() == null ? new Date() : accountWeixinModel.getLast_false_self_token());

						RsAccountResponse.Builder rsAccountResponseBuilder = RsAccountResponse.newBuilder();
						rsAccountResponseBuilder.setAccountId(account.getAccount_id());
						RedisResponse.Builder redisResponseBuilder = RedisResponse.newBuilder();
						redisResponseBuilder.setRsResponseType(RsResponseType.ACCOUNT_UP);
						RsAccountWeixinModelResponse.Builder rsAccountWeixinModelResponseBuilder = ModelToRedisUtil
								.getRsAccountWeixinModelResponse(accountWeixinModel);
						rsAccountWeixinModelResponseBuilder.setNeedDb(true);
						rsAccountResponseBuilder.setRsAccountWeixinModelResponse(rsAccountWeixinModelResponseBuilder);
						redisResponseBuilder.setRsAccountResponse(rsAccountResponseBuilder);

						redisService.convertAndSendRsResponse(redisResponseBuilder.build(), ERedisTopicType.topicCenter);
					}

					accounts.put(unionid, oldUserModel);
				} catch (Exception e) {
					logger.error("修改用户数据异常:" + oldUserModel.getUnionid(), e);
					e.printStackTrace();
				}
			});
			if (timer.incrementAndGet() >= THREAD_COUNT) {

				logger.error("录入完成，数据长度" + accounts.size());
				 handle(updateName);
				isUpdate.compareAndSet(true, false);
				services.shutdown();
				
			}
		}
	}
	
	public static void handle(String updateName) {
		try {
				SpringService.getBean(ClubDaoService.class).updateObject(updateName);
			logger.error("修改旧数据状态完成");

		} catch (Exception e) {
			logger.error("修改旧数据状态失败", e);
		}
	}
	
	
	private static void  updateData(float cardProxyRate, float cardNormalRate,String ibatisName,String updateName){
		DataController.updateKey++;
		logger.error("数据导入====数据开始导入");

		List<OldUserModel> list = SpringService.getBean(ClubDaoService.class).getDao().queryForList(ibatisName);

		logger.error("数据导入====数据长度" + list.size());

		accounts.clear();
		timer.set(0);

		services = Executors.newFixedThreadPool(THREAD_COUNT);
		if (list.size() < THREAD_COUNT) {

			OldDataRunable task = new OldDataRunable(new ArrayList<>(list), cardProxyRate, cardNormalRate,updateName);
			services.execute(task);
			timer.set(THREAD_COUNT - 1);

		} else {
			int mark_count = list.size() / THREAD_COUNT;

			if ((list.size() % THREAD_COUNT) > 0) {
				mark_count++;
			}

			for (int i = 0; i < THREAD_COUNT; i++) {
				OldDataRunable task = new OldDataRunable(new ArrayList<>(list.subList(i * mark_count, Math.min((i + 1) * mark_count, list.size()))),
						cardProxyRate, cardNormalRate, updateName);
				services.execute(task);
			}
		}

		list.clear();
	}
}
