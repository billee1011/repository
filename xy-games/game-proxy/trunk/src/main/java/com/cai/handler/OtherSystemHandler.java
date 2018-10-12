/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 *
 */
package com.cai.handler;

import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.time.DateUtils;

import com.cai.common.constant.ActivityMissionTypeEnum;
import com.cai.common.constant.AttributeKeyConstans;
import com.cai.common.constant.S2SCmd;
import com.cai.common.define.EAccountParamType;
import com.cai.common.define.EGoldOperateType;
import com.cai.common.define.EPlayerStatus;
import com.cai.common.define.ERedisTopicType;
import com.cai.common.define.EShareType;
import com.cai.common.domain.Account;
import com.cai.common.domain.AccountParamModel;
import com.cai.common.domain.AccountRecommendModel;
import com.cai.common.domain.CustomerSerNoticeModel;
import com.cai.common.domain.GameDescModel;
import com.cai.common.domain.GoodsModel;
import com.cai.common.domain.MainUiNoticeModel;
import com.cai.common.domain.Page;
import com.cai.common.domain.SysNoticeModel;
import com.cai.common.domain.SysParamModel;
import com.cai.common.rmi.ICenterRMIServer;
import com.cai.common.util.GlobalExecutor;
import com.cai.common.util.MyDateUtil;
import com.cai.common.util.PBUtil;
import com.cai.common.util.SessionUtil;
import com.cai.common.util.SpringService;
import com.cai.dictionary.CustomerSerNoticeDict;
import com.cai.dictionary.GameDescDict;
import com.cai.dictionary.GoodsDict;
import com.cai.dictionary.MainUiNoticeDict;
import com.cai.dictionary.SysNoticeDict;
import com.cai.dictionary.SysParamDict;
import com.cai.service.ClientServiceImpl;
import com.cai.service.FoundationService;
import com.cai.service.PublicServiceImpl;
import com.cai.service.RedisServiceImpl;
import com.cai.util.MessageResponse;
import com.google.common.collect.Lists;
import com.xianyi.framework.core.transport.ICmd;
import com.xianyi.framework.core.transport.netty.session.C2SSession;
import com.xianyi.framework.handler.IClientHandler;

import javolution.util.FastMap;
import protobuf.clazz.Protocol;
import protobuf.clazz.Protocol.GaemDescItemResponse;
import protobuf.clazz.Protocol.GoodsResponse;
import protobuf.clazz.Protocol.LocationInfor;
import protobuf.clazz.Protocol.MainUiNoticeItemResponse;
import protobuf.clazz.Protocol.OtherSystemRequest;
import protobuf.clazz.Protocol.OtherSystemResponse;
import protobuf.clazz.Protocol.Request;
import protobuf.clazz.Protocol.Request.RequestType;
import protobuf.clazz.Protocol.Response;
import protobuf.clazz.Protocol.Response.ResponseType;
import protobuf.clazz.Protocol.ShareActivityViewResponse;
import protobuf.clazz.Protocol.ShareInviteInfoResponse;
import protobuf.clazz.Protocol.ShareInviteViewResponse;
import protobuf.clazz.s2s.S2SProto.PlayerStatusProto;
import protobuf.redis.ProtoRedis.RedisResponse;
import protobuf.redis.ProtoRedis.RedisResponse.RsResponseType;
import protobuf.redis.ProtoRedis.RsAccountParamModelResponse;
import protobuf.redis.ProtoRedis.RsAccountResponse;

/**
 *
 * @author wu_hc
 */
@ICmd(code = RequestType.OTHER_SYS_VALUE, exName = "otherSystemRequest")
public class OtherSystemHandler extends IClientHandler<OtherSystemRequest> {

	/**
	 * 系统公告
	 */
	private static final int SYS_NOTICE = 1;

	/**
	 * 玩法说明
	 */
	private static final int GAME_DESC = 2;

	/**
	 * 主界面公告
	 */
	private static final int MAIN_UI_NOTICE = 3;

	/**
	 * 定位
	 */
	private static final int POSITION = 5;

	/**
	 * 分享活动面板
	 */
	private static final int SHARE_ACTIVITY_VIEW = 6;

	/**
	 * 成功分享每日每分享 //微信好友，微信朋友圈，支付宝，易信，QQ，钉钉 分享回调;
	 */
	private static final int SUCCESS_EVERYDAY_SHARE = 7;

	/**
	 * 邀请详情列表
	 */
	private static final int INVITE_INFO_VIEW = 8;

	/**
	 * 道具列表
	 */
	private static final int GOODS = 9;

	/**
	 * 客服界面公告
	 */
	private static final int CUSTOMER_SERVICE_NOTICE = 10;

	/**
	 * 比赛场公告
	 */
	private static final int MATCH_BROAD_NOTICE = 11;
		private static final int SUB_MATCH_BROAD_NOTICE = -100; //配置下的gameId值

	private static final int SHARE_TASK = 12;

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.xianyi.framework.cmd.IClientHandler#execute(com.google.protobuf.
	 * GeneratedMessage, com.cai.domain.Session)
	 */
	@Override
	protected void execute(OtherSystemRequest request, Request topRequest, C2SSession session) throws Exception {
		int type = request.getType();

		if (session.getAccount() == null)
			return;

		Account account = session.getAccount();

		int game_id = account.getGame_id();

		if (request.getAppId() > 0) {
			game_id = request.getAppId();
		}
		if (type == SYS_NOTICE) {

			SysNoticeModel sysNoticeModel = SysNoticeDict.getInstance().getSysNoticeModelDictionary().get(game_id).get(1);

			OtherSystemResponse.Builder otherSystemResponseBuilder = OtherSystemResponse.newBuilder();
			otherSystemResponseBuilder.setType(1);
			otherSystemResponseBuilder.setContent(sysNoticeModel.getContent());

			Response.Builder responseBuilder = Response.newBuilder();
			responseBuilder.setResponseType(ResponseType.OTHER_SYS);
			responseBuilder.setExtension(Protocol.otherSystemResponse, otherSystemResponseBuilder.build());
			session.send(responseBuilder.build());

			// List<RsAccountParamModelResponse> rsAccountParamModelResponseList
			// = Lists.newArrayList();
			// //标记为看过的
			// AccountParamModel accountParamModel =
			// account.getAccountParamModelMap().get(EPropertyType.RED_SYS_NOTIC.getId());
			// if(accountParamModel==null){
			// accountParamModel = new AccountParamModel();
			// accountParamModel.setAccount_id(account.getAccount_id());
			// accountParamModel.setType(EPropertyType.RED_SYS_NOTIC.getId());
			// accountParamModel.setLong1(sysNoticeModel.getCreate_time().getTime());
			// accountParamModel.setNeedDB(true);
			// rsAccountParamModelResponseList.add(MessageResponse.getRsAccountParamModelResponse(accountParamModel).build());
			// }else{
			// if(accountParamModel.getLong1()==null ||
			// accountParamModel.getLong1()!=sysNoticeModel.getCreate_time().getTime()){
			// accountParamModel.setLong1(sysNoticeModel.getCreate_time().getTime());
			// accountParamModel.setNeedDB(true);
			// rsAccountParamModelResponseList.add(MessageResponse.getRsAccountParamModelResponse(accountParamModel).build());
			// }
			// }
			//
			// if(rsAccountParamModelResponseList.size()>0){
			// //========同步到中心========
			// RedisResponse.Builder redisResponseBuilder =
			// RedisResponse.newBuilder();
			// redisResponseBuilder.setRsResponseType(RsResponseType.ACCOUNT_UP);
			// RsAccountResponse.Builder rsAccountResponseBuilder =
			// RsAccountResponse.newBuilder();
			// rsAccountResponseBuilder.setAccountId(account.getAccount_id());
			// rsAccountResponseBuilder.addAllRsAccountParamModelResponseList(rsAccountParamModelResponseList);
			// RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(),
			// ERedisTopicType.topicCenter);
			// }

		}

		else if (type == GAME_DESC) {
			FastMap<Integer, GameDescModel> des = GameDescDict.getInstance().getGameDescModelDictionary().get(game_id);
			if (null == des || des.isEmpty()) {
				logger.error("没有找到游戏描述，请确认，gameid:{}", game_id);
				return;
			} else {
				OtherSystemResponse.Builder otherSystemResponseBuilder = OtherSystemResponse.newBuilder();
				otherSystemResponseBuilder.setType(2);
				List<GaemDescItemResponse> gaemDescItemResponseList = Lists.newArrayList();
				otherSystemResponseBuilder.setAppId(game_id);
				for (GameDescModel m : des.values()) {
					gaemDescItemResponseList.add(MessageResponse.getGaemDescItemResponse(m).build());
				}
				otherSystemResponseBuilder.addAllGaemDescItemResponseList(gaemDescItemResponseList);
				Response.Builder responseBuilder = Response.newBuilder();
				responseBuilder.setResponseType(ResponseType.OTHER_SYS);
				responseBuilder.setExtension(Protocol.otherSystemResponse, otherSystemResponseBuilder.build());
				session.send(responseBuilder.build());
			}
		}

		else if (type == GOODS) {
			OtherSystemResponse.Builder otherSystemResponseBuilder = OtherSystemResponse.newBuilder();
			otherSystemResponseBuilder.setType(GOODS);
			List<GoodsResponse> goodsList = Lists.newArrayList();

			for (GoodsModel m : GoodsDict.getInstance().getGoodsModelByGameIdAndShopType(game_id)) {
				goodsList.add(MessageResponse.getGoodResponse(m).build());
			}
			otherSystemResponseBuilder.addAllGoodList(goodsList);
			Response.Builder responseBuilder = Response.newBuilder();
			responseBuilder.setResponseType(ResponseType.OTHER_SYS);
			otherSystemResponseBuilder.setAppId(game_id);
			responseBuilder.setExtension(Protocol.otherSystemResponse, otherSystemResponseBuilder.build());
			session.send(responseBuilder.build());
		} else if (type == CUSTOMER_SERVICE_NOTICE) {

			List<MainUiNoticeItemResponse> customerSerNoticeModelList = Lists.newArrayList();
			// 是否开放
			// SysParamModel sysParamModel1016 =
			// SysParamDict.getInstance().getSysParamModelDictionaryByGameId(game_id).get(1016);
			// if (null == sysParamModel1016) {
			// logger.error("玩家[{}]请求系统配置gameid:{}，但找不到!!", account, game_id);
			// return;
			// }
			// if (sysParamModel1016.getVal1() == 1) {
			FastMap<Integer, CustomerSerNoticeModel> map = CustomerSerNoticeDict.getInstance().getCustomerSerNoticeDictionary().get(game_id);
			if (map == null) {
				map = CustomerSerNoticeDict.getInstance().getCustomerSerNoticeDictionary().get(0);
			}
			if (map != null) {
				Date now = MyDateUtil.getNow();
				for (CustomerSerNoticeModel m : map.values()) {
					if (m.getEnd_time().after(now)) {
						customerSerNoticeModelList.add(MessageResponse.getCustomerSerNoticeItemResponse(m).build());
					}
				}
			}
			// }

			OtherSystemResponse.Builder otherSystemResponseBuilder = OtherSystemResponse.newBuilder();
			otherSystemResponseBuilder.setType(10);
			otherSystemResponseBuilder.setAppId(game_id);
			otherSystemResponseBuilder.addAllMainUiNoticeItemResponseList(customerSerNoticeModelList);
			Response.Builder responseBuilder = Response.newBuilder();
			responseBuilder.setResponseType(ResponseType.OTHER_SYS);
			responseBuilder.setExtension(Protocol.otherSystemResponse, otherSystemResponseBuilder.build());
			session.send(responseBuilder.build());
			// session.send(PBUtil.toS2CCommonRsp(S2CCmd.xxxx,
			// otherSystemResponseBuilder));

		} else if (type == MATCH_BROAD_NOTICE) {
			List<MainUiNoticeItemResponse> mainUiNoticeItemResponseList = Lists.newArrayList();
			FastMap<Integer, MainUiNoticeModel> commonMap = MainUiNoticeDict.getInstance().getMainUiNoticeDictionary().get(SUB_MATCH_BROAD_NOTICE);
			if (commonMap != null) {
				Date now = MyDateUtil.getNow();
				for (MainUiNoticeModel m : commonMap.values()) {
					if (m.getEnd_time().after(now)) {
						mainUiNoticeItemResponseList.add(MessageResponse.getMainUiNoticeItemResponse(m).build());
					}
				}
			}

			OtherSystemResponse.Builder otherSystemResponseBuilder = OtherSystemResponse.newBuilder();
			otherSystemResponseBuilder.setType(type);
			otherSystemResponseBuilder.setAppId(SUB_MATCH_BROAD_NOTICE);
			otherSystemResponseBuilder.addAllMainUiNoticeItemResponseList(mainUiNoticeItemResponseList);
			Response.Builder responseBuilder = Response.newBuilder();
			responseBuilder.setResponseType(ResponseType.OTHER_SYS);
			responseBuilder.setExtension(Protocol.otherSystemResponse, otherSystemResponseBuilder.build());
			session.send(responseBuilder.build());
		 
		 }else if (type == MAIN_UI_NOTICE) {
			List<MainUiNoticeItemResponse> mainUiNoticeItemResponseList = Lists.newArrayList();
			// 是否开放
			SysParamModel sysParamModel1016 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(game_id).get(1016);
			if (null == sysParamModel1016) {
				logger.error("玩家[{}]请求系统配置gameid:{}，但找不到!!", account, game_id);
				return;
			}
			if (sysParamModel1016.getVal1() == 1) {
				FastMap<Integer, MainUiNoticeModel> map = MainUiNoticeDict.getInstance().getMainUiNoticeDictionary().get(game_id);
				if (map != null) {
					Date now = MyDateUtil.getNow();
					for (MainUiNoticeModel m : map.values()) {
						if (m.getEnd_time().after(now)) {
							mainUiNoticeItemResponseList.add(MessageResponse.getMainUiNoticeItemResponse(m).build());
						}
					}
				}
				FastMap<Integer, MainUiNoticeModel> commonMap = MainUiNoticeDict.getInstance().getMainUiNoticeDictionary().get(0);
				if (commonMap != null) {
					Date now = MyDateUtil.getNow();
					for (MainUiNoticeModel m : commonMap.values()) {
						if (m.getEnd_time().after(now)) {
							// 过滤大厅主界面的公告
							if (m.getGame_id() == 6 && game_id == 6) {
								continue;
							}
							mainUiNoticeItemResponseList.add(MessageResponse.getMainUiNoticeItemResponse(m).build());
						}
					}
				}
			}

			OtherSystemResponse.Builder otherSystemResponseBuilder = OtherSystemResponse.newBuilder();
			otherSystemResponseBuilder.setType(3);
			otherSystemResponseBuilder.setAppId(game_id);
			otherSystemResponseBuilder.addAllMainUiNoticeItemResponseList(mainUiNoticeItemResponseList);
			Response.Builder responseBuilder = Response.newBuilder();
			responseBuilder.setResponseType(ResponseType.OTHER_SYS);
			responseBuilder.setExtension(Protocol.otherSystemResponse, otherSystemResponseBuilder.build());
			session.send(responseBuilder.build());
		}

		else if (type == POSITION) {
			LocationInfor locationInfor = request.getLocationInfor();
			if (locationInfor == null) {
				logger.error("locationInfor is null");
				return;
			}
			OtherSystemResponse.Builder otherSystemResponseBuilder = OtherSystemResponse.newBuilder();
			otherSystemResponseBuilder.setType(POSITION);
			otherSystemResponseBuilder.setLocationInfor(locationInfor);
			Response.Builder responseBuilder = Response.newBuilder();
			responseBuilder.setResponseType(ResponseType.OTHER_SYS);
			responseBuilder.setExtension(Protocol.otherSystemResponse, otherSystemResponseBuilder.build());
			session.send(responseBuilder.build());

			SessionUtil.setAttr(session, AttributeKeyConstans.ACCOUNT_LOCATION, locationInfor);
		}

		// else if(type == DAILY_SHARE) {
		// SysParamModel sysParamModel2005 =
		// SysParamDict.getInstance().getSysParamModelDictionaryByGameId(account.getGame_id()).get(2005);
		// if (sysParamModel2005.getVal1() == 0) {
		// day_share(0);
		// return;
		// }
		// int gold = sysParamModel2005.getVal1();
		// Date finishDate = sysParamModel2005.getFinish_time();
		// if(finishDate==null) {
		// day_share(0);
		// return;
		// }
		// Date now = MyDateUtil.getNow();
		// if(now.getTime()> finishDate.getTime()) {
		// day_share(0);
		// return ;
		// }
		// AccountParamModel accountParamModel =
		// PublicServiceImpl.getInstance().getAccountParamModel(account,
		// EAccountParamType.DAY_SHARE_TIME);
		// if(accountParamModel.getDate1()!=null) {
		// boolean isSameday =
		// DateUtils.isSameDay(accountParamModel.getDate1(),now);
		// if(isSameday) {
		// day_share(0);
		// return;
		// }
		// }
		//
		// day_share(gold);
		//
		// ICenterRMIServer centerRMIServer =
		// SpringService.getBean(ICenterRMIServer.class);
		// centerRMIServer.addAccountGold(account.getAccount_id(), gold, false,
		// "每日分享得卡:" + gold, EGoldOperateType.DAY_SHARE);
		//
		// accountParamModel.setDate1(now);
		//
		//
		// // ========同步到中心========
		// RedisResponse.Builder redisResponseBuilder =
		// RedisResponse.newBuilder();
		// redisResponseBuilder.setRsResponseType(RsResponseType.ACCOUNT_UP);
		// //
		// RsAccountResponse.Builder rsAccountResponseBuilder =
		// RsAccountResponse.newBuilder();
		// rsAccountResponseBuilder.setAccountId(account.getAccount_id());
		// //
		// RsAccountParamModelResponse.Builder rsAccountParamModelResponse =
		// RsAccountParamModelResponse.newBuilder();
		// rsAccountParamModelResponse.setAccountId(account.getAccount_id());
		// rsAccountParamModelResponse.setType(EAccountParamType.DAY_SHARE_TIME.getId());
		// rsAccountParamModelResponse.setData1(accountParamModel.getDate1().getTime());
		// rsAccountParamModelResponse.setNeedDb(true);
		// rsAccountResponseBuilder.addRsAccountParamModelResponseList(rsAccountParamModelResponse);
		// //
		// redisResponseBuilder.setRsAccountResponse(rsAccountResponseBuilder);
		// RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(),
		// ERedisTopicType.topicCenter);
		//
		// }

		else if (type == SHARE_ACTIVITY_VIEW) {
			// 活动1，每日分享可获的金币数
			// 活动2，推荐好友下载可获得的金币数，好友获得的金币数
			// 已邀请的人数
			// 总获得金币数
			SysParamModel sysParamModel2004 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(account.getGame_id()).get(2004);
			ShareActivityViewResponse.Builder shareActivityViewResponseBuilder = ShareActivityViewResponse.newBuilder();
			shareActivityViewResponseBuilder.setEveryShareGold(sysParamModel2004.getVal1());
			shareActivityViewResponseBuilder.setFriendDownSelfGold(sysParamModel2004.getVal2());
			shareActivityViewResponseBuilder.setFriendDownFriendGold(sysParamModel2004.getVal3());
			//
			int goldCount = 0;
			int peopleNum = 0;
			for (AccountRecommendModel m : account.getAccountRecommendModelMap().values()) {
				goldCount += m.getGold_num();
				peopleNum++;
			}
			//
			boolean flag = false;
			Date now = MyDateUtil.getNow();
			AccountParamModel accountParamModel = PublicServiceImpl.getInstance().getAccountParamModel(account, EAccountParamType.DAY_SHARE_TIME);
			if (accountParamModel.getDate1() != null) {
				boolean isSameday = DateUtils.isSameDay(accountParamModel.getDate1(), now);
				if (!isSameday) {
					flag = true;
				}
			} else {
				flag = true;
			}

			shareActivityViewResponseBuilder.setInvitePeopleCount(peopleNum);
			shareActivityViewResponseBuilder.setInviteGoldNum(goldCount);
			if (flag) {
				shareActivityViewResponseBuilder.setTodayShareStatus(0);//
			} else {
				shareActivityViewResponseBuilder.setTodayShareStatus(1);//
			}

			OtherSystemResponse.Builder otherSystemResponseBuilder = OtherSystemResponse.newBuilder();
			otherSystemResponseBuilder.setType(6);
			otherSystemResponseBuilder.setShareActivityViewResponse(shareActivityViewResponseBuilder);
			Response.Builder responseBuilder = Response.newBuilder();
			responseBuilder.setResponseType(ResponseType.OTHER_SYS);
			responseBuilder.setExtension(Protocol.otherSystemResponse, otherSystemResponseBuilder.build());
			session.send(responseBuilder.build());

		}

		else if (type == SUCCESS_EVERYDAY_SHARE) {

			GlobalExecutor.asyn_execute(new Runnable() {

				@Override
				public void run() {
					if (request.getShareType() == EShareType.TYPE_SHARE_WXHY.getId()) {
						// 分享微信好友
						FoundationService.getInstance().sendActivityMissionProcess(account.getAccount_id(), ActivityMissionTypeEnum.SHARE_WX_FRIENDS,
								1, 1);
					} else if (request.getShareType() == EShareType.TYPE_SHARE_WXPYQ.getId()) {
						// 分享微信朋友圈
						FoundationService.getInstance().sendActivityMissionProcess(account.getAccount_id(), ActivityMissionTypeEnum.SHARE_WX_PYQ, 1,
								1);
						// 连续分享朋友圈
						FoundationService.getInstance().sendActivityMissionProcess(account.getAccount_id(),
								ActivityMissionTypeEnum.SHARE_WX_PYQ_SEQUENCE, 1, 1);
					} else if (request.getShareType() == EShareType.TYPE_SHARE_QQ.getId()) {
						// 分享QQ好友
						FoundationService.getInstance().sendActivityMissionProcess(account.getAccount_id(), ActivityMissionTypeEnum.SHARE_QQ_FRIENDS,
								1, 1);
					}
				}
			});

			SysParamModel sysParamModel2004 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(account.getGame_id()).get(2004);
			int every_share_gold = sysParamModel2004.getVal1();
			// session.send(MessageResponse.getMsgAllResponse("今日已领取了").build());
			if (every_share_gold == 0)
				return;
			boolean flag = false;
			Date now = MyDateUtil.getNow();
			AccountParamModel accountParamModel = PublicServiceImpl.getInstance().getAccountParamModel(account, EAccountParamType.DAY_SHARE_TIME);
			if (accountParamModel.getDate1() != null) {
				boolean isSameday = DateUtils.isSameDay(accountParamModel.getDate1(), now);
				if (!isSameday) {
					flag = true;
				}
			} else {
				flag = true;
			}

			if (!flag)
				return;

			accountParamModel.setDate1(now);
			ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
			centerRMIServer.addAccountGold(account.getAccount_id(), every_share_gold, false, "每日分享得卡:" + every_share_gold,
					EGoldOperateType.DAY_SHARE);

			// ========同步到中心========
			RedisResponse.Builder redisResponseBuilder = RedisResponse.newBuilder();
			redisResponseBuilder.setRsResponseType(RsResponseType.ACCOUNT_UP);
			//
			RsAccountResponse.Builder rsAccountResponseBuilder = RsAccountResponse.newBuilder();
			rsAccountResponseBuilder.setAccountId(account.getAccount_id());
			//
			RsAccountParamModelResponse.Builder rsAccountParamModelResponse = RsAccountParamModelResponse.newBuilder();
			rsAccountParamModelResponse.setAccountId(account.getAccount_id());
			rsAccountParamModelResponse.setType(EAccountParamType.DAY_SHARE_TIME.getId());
			rsAccountParamModelResponse.setData1(accountParamModel.getDate1().getTime());
			rsAccountParamModelResponse.setNeedDb(true);
			rsAccountResponseBuilder.addRsAccountParamModelResponseList(rsAccountParamModelResponse);
			//
			redisResponseBuilder.setRsAccountResponse(rsAccountResponseBuilder);
			RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(), ERedisTopicType.topicCenter);

			// 返回消息
			OtherSystemResponse.Builder otherSystemResponseBuilder = OtherSystemResponse.newBuilder();
			otherSystemResponseBuilder.setType(7);
			otherSystemResponseBuilder.setGoldNumber(every_share_gold);
			Response.Builder responseBuilder = Response.newBuilder();
			responseBuilder.setResponseType(ResponseType.OTHER_SYS);
			responseBuilder.setExtension(Protocol.otherSystemResponse, otherSystemResponseBuilder.build());
			session.send(responseBuilder.build());

			PlayerStatusProto.Builder builder = PlayerStatusProto.newBuilder();
			builder.setAccountId(account.getAccount_id());
			builder.setStatus(EPlayerStatus.NONE.status());
			ClientServiceImpl.getInstance().sendMatch(PBUtil.toS2SRequet(S2SCmd.EVERYDAY_SHARE, builder).build());

		}

		else if (type == INVITE_INFO_VIEW) {

			int cur_page = request.getCurPage();
			if (cur_page <= 0)
				cur_page = 1;
			List<AccountRecommendModel> accountRecommendModelList = Lists.newArrayList(account.getAccountRecommendModelMap().values());
			// 排序
			Collections.sort(accountRecommendModelList, new Comparator<AccountRecommendModel>() {
				public int compare(AccountRecommendModel p1, AccountRecommendModel p2) {
					return ((Long) p2.getCreate_time().getTime()).compareTo((Long) p1.getCreate_time().getTime());// id
																													// 从大到小
				}
			});

			int beginIndex = (cur_page - 1) * 10;
			List<ShareInviteInfoResponse> shareInviteInfoResponseBuilderList = Lists.newArrayList();

			/*
			 * for(int i=0;i<101;i++) { AccountRecommendModel model = new
			 * AccountRecommendModel(); model.setAccount_id(1L);
			 * model.setCreate_time(new Date()); model.setGold_num(8);
			 * model.setTarget_account_id(40); model.setTarget_name("test");
			 * model.setTarget_icon(
			 * "http://wx.qlogo.cn/mmopen/vUJicqzME8aB3Olnt0l620cBJqwXfUAecgvLEiaEyxicRJ3ZHr7LuJhYYPgxPyoQozqeQk5NAlMvODUOfVrHFs7EjJoVyicGZ4R5/46"
			 * ); accountRecommendModelList.add(model); }
			 */

			for (int i = 0; i < 10; i++) {
				if (beginIndex >= accountRecommendModelList.size())
					break;
				AccountRecommendModel model = accountRecommendModelList.get(beginIndex);
				ShareInviteInfoResponse.Builder shareInviteInfoResponseBuilder = MessageResponse.getShareInviteInfoResponse(model);
				shareInviteInfoResponseBuilderList.add(shareInviteInfoResponseBuilder.build());
				beginIndex++;
			}

			int totalSize = accountRecommendModelList.size();

			Page page = new Page(cur_page, 10, totalSize);

			ShareInviteViewResponse.Builder shareInviteViewResponseBuilder = ShareInviteViewResponse.newBuilder();
			shareInviteViewResponseBuilder.addAllShareInviteInfoList(shareInviteInfoResponseBuilderList);
			shareInviteViewResponseBuilder.setTotalPage(page.getTotalPage());
			shareInviteViewResponseBuilder.setCurPage(page.getRealPage());
			shareInviteViewResponseBuilder.setPageSize(page.getPageSize());

			OtherSystemResponse.Builder otherSystemResponseBuilder = OtherSystemResponse.newBuilder();
			otherSystemResponseBuilder.setType(8);
			otherSystemResponseBuilder.setShareInviteViewResponse(shareInviteViewResponseBuilder);

			Response.Builder responseBuilder = Response.newBuilder();
			responseBuilder.setResponseType(ResponseType.OTHER_SYS);
			responseBuilder.setExtension(Protocol.otherSystemResponse, otherSystemResponseBuilder.build());
			session.send(responseBuilder.build());

		} else if (type == SHARE_TASK) {

			GlobalExecutor.asyn_execute(new Runnable() {

				@Override
				public void run() {
					if (request.getShareType() == EShareType.TYPE_SHARE_WXHY.getId()) {
						// 分享微信好友
						FoundationService.getInstance().sendActivityMissionProcess(account.getAccount_id(), ActivityMissionTypeEnum.SHARE_WX_FRIENDS,
								1, 1);
					} else if (request.getShareType() == EShareType.TYPE_SHARE_WXPYQ.getId()) {
						// 分享微信朋友圈
						FoundationService.getInstance().sendActivityMissionProcess(account.getAccount_id(), ActivityMissionTypeEnum.SHARE_WX_PYQ, 1,
								1);
						// 连续分享朋友圈
						FoundationService.getInstance().sendActivityMissionProcess(account.getAccount_id(),
								ActivityMissionTypeEnum.SHARE_WX_PYQ_SEQUENCE, 1, 1);
					} else if (request.getShareType() == EShareType.TYPE_SHARE_QQ.getId()) {
						// 分享QQ好友
						FoundationService.getInstance().sendActivityMissionProcess(account.getAccount_id(), ActivityMissionTypeEnum.SHARE_QQ_FRIENDS,
								1, 1);
					}
				}
			});
		}
	}

}
