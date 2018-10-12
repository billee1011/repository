package com.cai.util;

import java.util.List;

import org.apache.commons.lang.StringUtils;

import com.cai.common.constant.GameConstants;
import com.cai.common.define.EPropertyType;
import com.cai.common.define.EPtType;
import com.cai.common.define.ESysMsgType;
import com.cai.common.define.EWxHeadimgurlType;
import com.cai.common.domain.Account;
import com.cai.common.domain.AccountModel;
import com.cai.common.domain.AccountParamModel;
import com.cai.common.domain.AccountRecommendModel;
import com.cai.common.domain.AccountWeixinModel;
import com.cai.common.domain.AppShopModel;
import com.cai.common.domain.GameDescModel;
import com.cai.common.domain.GoodsModel;
import com.cai.common.domain.LoginNoticeModel;
import com.cai.common.domain.MainUiNoticeModel;
import com.cai.common.domain.MoneyShopModel;
import com.cai.common.domain.ProxyGoldLogModel;
import com.cai.common.domain.PrxoyPlayerRoomModel;
import com.cai.common.domain.RoomRedisModel;
import com.cai.common.domain.ShopModel;
import com.cai.common.domain.SysNoticeModel;
import com.cai.common.domain.SysParamModel;
import com.cai.common.util.IpUtil;
import com.cai.common.util.WxUtil;
import com.cai.core.SystemConfig;
import com.cai.dictionary.SysNoticeDict;
import com.cai.dictionary.SysParamDict;
import com.cai.domain.Session;
import com.google.common.collect.Lists;

import javolution.util.FastMap;
import protobuf.clazz.Protocol;
import protobuf.clazz.Protocol.AccountPropertyResponse;
import protobuf.clazz.Protocol.AccountResponse;
import protobuf.clazz.Protocol.AddGoldCardHistoryResponse;
import protobuf.clazz.Protocol.GaemDescItemResponse;
import protobuf.clazz.Protocol.GoodsResponse;
import protobuf.clazz.Protocol.LogicRoomAccountItemRequest;
import protobuf.clazz.Protocol.LoginItemResponse;
import protobuf.clazz.Protocol.LoginNoticeResponse;
import protobuf.clazz.Protocol.MainUiNoticeItemResponse;
import protobuf.clazz.Protocol.MsgAllResponse;
import protobuf.clazz.Protocol.ProxyRoomItemResponse;
import protobuf.clazz.Protocol.Request;
import protobuf.clazz.Protocol.Request.RequestType;
import protobuf.clazz.Protocol.Response;
import protobuf.clazz.Protocol.Response.ResponseType;
import protobuf.clazz.Protocol.ShareInviteInfoResponse;
import protobuf.clazz.Protocol.StoreGoodMoneyResponse;
import protobuf.clazz.Protocol.StoreGoodResponse;
import protobuf.redis.ProtoRedis.RsAccountParamModelResponse;

public class MessageResponse {
	
	
	public static Request.Builder getLogicRequest(Request.Builder requestBuilder,Session session){
		requestBuilder.setProxId(SystemConfig.proxy_index);
		requestBuilder.setProxSeesionId(session.getSessionId());
		requestBuilder.setGameId(session.getAccount().getGame_id());
		if(session.getAccount()!=null){
			requestBuilder.setProxyAccountId(session.getAccount().getAccount_id());
		}
		return requestBuilder;
	}
	
	public static Request.Builder getLogicRequest(RequestType requestType,Session session){
		Request.Builder requestBuilder = Request.newBuilder();
		requestBuilder.setRequestType(requestType);
		requestBuilder.setProxId(SystemConfig.proxy_index);
		requestBuilder.setProxSeesionId(session.getSessionId());
		requestBuilder.setGameId(session.getAccount().getGame_id());
		if(session.getAccount()!=null){
			requestBuilder.setProxyAccountId(session.getAccount().getAccount_id());
		}
		return requestBuilder;
	}
	
	public static Request.Builder getLogicRequest(RequestType requestType){
		Request.Builder requestBuilder = Request.newBuilder();
		requestBuilder.setRequestType(requestType);
		requestBuilder.setProxId(SystemConfig.proxy_index);
		return requestBuilder;
	}
	
	
	public static AccountResponse.Builder getAccountResponse(Account account){
		AccountModel accountModel = account.getAccountModel();
		AccountResponse.Builder accountResponseBuilder = AccountResponse.newBuilder();
		accountResponseBuilder.setAccountId(accountModel.getAccount_id());
		accountResponseBuilder.setAccountName(account.getNickName());
		accountResponseBuilder.setNickName(account.getNickName());
		accountResponseBuilder.setSex(1);
		accountResponseBuilder.setAccountCreateTime((int)(accountModel.getCreate_time().getTime()/1000L));
		accountResponseBuilder.setGold(accountModel.getGold());
		accountResponseBuilder.setVipLvl(accountModel.getIs_agent());
		accountResponseBuilder.setIsInner(accountModel.getIs_inner());
		accountResponseBuilder.setMoney(accountModel.getMoney());
		String ip = accountModel.getClient_ip2();
		if(IpUtil.isWhiteIp(ip)){
			ip = "";
		}
		accountResponseBuilder.setIp(ip);
		accountResponseBuilder.setIpAddr("");
		if(accountModel.getPt().equals(EPtType.WX.getId())){
			AccountWeixinModel accountWeixinModel = account.getAccountWeixinModel();
			//头像
			accountResponseBuilder.setHeadImgUrl(WxUtil.changHeadimgurl(accountWeixinModel.getHeadimgurl(), EWxHeadimgurlType.S132));
			//性别
			if("1".equals(accountWeixinModel.getSex())){
				accountResponseBuilder.setSex(1);
			}else{
				accountResponseBuilder.setSex(2);
			}
			//昵称
		}else{
			accountResponseBuilder.setHeadImgUrl("");//其它平台暂时没有头像
		}
		
		if(accountModel.getPassword()==null || "".equals(accountModel.getPassword().trim())){
			accountResponseBuilder.setIsNullAgentPw(true);
		}else{
			accountResponseBuilder.setIsNullAgentPw(false);
		}
		
		accountResponseBuilder.setRecommendId(accountModel.getRecommend_id());
		
		
		
		return accountResponseBuilder;
	}
	
	
	public static LoginItemResponse.Builder getLoginItemResponse(Account account,int gameIndex){
		AccountModel accountModel = account.getAccountModel();
		LoginItemResponse.Builder loginItemResponseBuilder = LoginItemResponse.newBuilder();
		loginItemResponseBuilder.setAccountResponse(getAccountResponse(account));
		loginItemResponseBuilder.setGameIndex(gameIndex);
		loginItemResponseBuilder.setGameName("tmp");
		return loginItemResponseBuilder;
	}
	
	public static LogicRoomAccountItemRequest.Builder getLogicRoomAccountItemRequest(Session session){
		Account account = session.getAccount();
		LogicRoomAccountItemRequest.Builder buider = LogicRoomAccountItemRequest.newBuilder();
		buider.setAccountId(account.getAccount_id());
		buider.setGold(account.getAccountModel().getGold());
		buider.setMoney(account.getAccountModel().getMoney());
		buider.setProxyIndex(SystemConfig.proxy_index);
		buider.setProxySessionId(session.getSessionId());
		buider.setNickName(account.getNickName());
		String ip = account.getAccountModel().getClient_ip2();
		if(IpUtil.isWhiteIp(ip)){
			ip = "";
		}
		buider.setAccountIp(ip);
		buider.setIpAddr("");
		buider.setAccountIcon("1.png");
		//微信用户
		if(account.getAccountWeixinModel()!=null){
			buider.setAccountIcon(WxUtil.changHeadimgurl(account.getAccountWeixinModel().getHeadimgurl(), EWxHeadimgurlType.S132));
			if("1".equals(account.getAccountWeixinModel().getSex())){
				buider.setSex(1);
			}else{
				buider.setSex(2);
			}
		}else{
			buider.setSex(1);
		}
		
		return buider;
	}
	
	/**
	 * 普通提示信息
	 * @param type
	 * @param msg
	 * @return
	 */
	public static Response.Builder getMsgAllResponse(String msg){
		Response.Builder responseBuilder = Response.newBuilder();
		responseBuilder.setResponseType(ResponseType.MSG);
		MsgAllResponse.Builder msgBuilder = MsgAllResponse.newBuilder();
		msgBuilder.setType(ESysMsgType.NONE.getId());
		msgBuilder.setMsg(msg);
		responseBuilder.setExtension(Protocol.msgAllResponse, msgBuilder.build());
		return responseBuilder;
	}
	
	/**
	 * 有错误码的提示信息
	 * @param type
	 * @param error_id
	 * @param msg
	 * @return
	 */
	public static Response.Builder getMsgAllResponse(int error_id,String msg){
		Response.Builder responseBuilder = Response.newBuilder();
		responseBuilder.setResponseType(ResponseType.MSG);
		MsgAllResponse.Builder msgBuilder = MsgAllResponse.newBuilder();
		msgBuilder.setType(ESysMsgType.INCLUDE_ERROR.getId());
		msgBuilder.setMsg(msg);
		msgBuilder.setErrorId(error_id);
		responseBuilder.setExtension(Protocol.msgAllResponse, msgBuilder.build());
		return responseBuilder;
	}
	
	/**
	 * 通用属性
	 * @param prop_id
	 * @param val1
	 * @param val2
	 * @param val3
	 * @param str1
	 * @param str2
	 * @return
	 */
	public static AccountPropertyResponse.Builder getAccountPropertyResponse(int prop_id,Integer val1,Integer val2,Integer val3,Integer val4,Integer val5,String str1,String str2,Long vallong1){
		AccountPropertyResponse.Builder builder = AccountPropertyResponse.newBuilder();
		builder.setPropId(prop_id);
		if(val1!=null)
			builder.setVal1(val1);
		if(val2!=null)
			builder.setVal2(val2);
		if(val3!=null)
			builder.setVal3(val3);
		if(val4!=null)
			builder.setVal4(val4);
		if(val5!=null)
			builder.setVal5(val5);
		if(str1!=null)
			builder.setStr1(str1);
		if(str2!=null)
			builder.setStr2(str2);
		if(vallong1!=null)
			builder.setVallong1(vallong1);
		
		return builder;
	}
	
	/**
	 * 系统属性
	 * @param game_id
	 * @return
	 */
	public static List<AccountPropertyResponse> getSysAccountPropertyResponseList(int game_id,Account account){
		//系统参数的
		FastMap<Integer, SysParamModel> map = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(game_id);
		List<AccountPropertyResponse> list = Lists.newArrayList();
		for(SysParamModel model : map.values()){
			if(model.getSend_client()!=1)
				continue;
			AccountPropertyResponse.Builder accountPropertyResponseBuilder = getAccountPropertyResponse(model.getId(), model.getVal1(), model.getVal2(), model.getVal3(),model.getVal4(),model.getVal5(), model.getStr1(), model.getStr2(),null);
			list.add(accountPropertyResponseBuilder.build());
		}

		
		
		//玩家本身的数据
		//红点功能
		//系统公告
		AccountParamModel accountParamModel = account.getAccountParamModelMap().get(EPropertyType.RED_SYS_NOTIC.getId());
		if(accountParamModel!=null){
			SysNoticeModel sysNoticeModel = SysNoticeDict.getInstance().getSysNoticeModelDictionary().get(1).get(1);
			if(sysNoticeModel!=null){
				int flag = 0;
				if(accountParamModel.getLong1()==null || accountParamModel.getLong1() !=sysNoticeModel.getCreate_time().getTime()){
					flag = 1;
				}else{
					flag = 0;
				}
				AccountPropertyResponse.Builder accountPropertyResponseBuilder =getAccountPropertyResponse(EPropertyType.RED_SYS_NOTIC.getId(), flag, null,null,null, null, null, null, null);
				list.add(accountPropertyResponseBuilder.build());
			}
		}
		
		
		
		
		return list;
	}
	
	
	public static RsAccountParamModelResponse.Builder getRsAccountParamModelResponse(AccountParamModel accountParamModel){
		RsAccountParamModelResponse.Builder rsAccountParamModelResponseBuilder = RsAccountParamModelResponse.newBuilder();
		rsAccountParamModelResponseBuilder.setAccountId(accountParamModel.getAccount_id());
		rsAccountParamModelResponseBuilder.setType(accountParamModel.getType());
		rsAccountParamModelResponseBuilder.setVal1(accountParamModel.getVal1());
		rsAccountParamModelResponseBuilder.setLong1(accountParamModel.getLong1());
		rsAccountParamModelResponseBuilder.setNeedDb(accountParamModel.isNeedDB());
		return rsAccountParamModelResponseBuilder;
	}
	
	/**
	 * 游戏玩法说明
	 * @param model
	 * @return
	 */
	public static GaemDescItemResponse.Builder getGaemDescItemResponse(GameDescModel model){
		GaemDescItemResponse.Builder gaemDescItemResponseBuilder = GaemDescItemResponse.newBuilder();
		gaemDescItemResponseBuilder.setId(model.getId());
		gaemDescItemResponseBuilder.setTitle(model.getTitle());
		gaemDescItemResponseBuilder.setGameDesc(model.getGame_desc());
		gaemDescItemResponseBuilder.setSort(model.getSort());
		return gaemDescItemResponseBuilder;
	}
	
	/**
	 * 道具
	 * @param model
	 * @return
	 */
	public static GoodsResponse.Builder getGoodResponse(GoodsModel model){
		GoodsResponse.Builder goodResponse = GoodsResponse.newBuilder();
		goodResponse.setGoodID(model.getGoodID());
		goodResponse.setSort(model.getDisplay_order());
		goodResponse.setIcon(model.getIcon());
		goodResponse.setMoney(model.getMoney());
		return goodResponse;
	}
	
	/**
	 * 代理转卡记录
	 * @param model
	 * @return
	 */
	public static AddGoldCardHistoryResponse.Builder getAddGoldCardHistoryResponse(ProxyGoldLogModel model){
		AddGoldCardHistoryResponse.Builder builder = AddGoldCardHistoryResponse.newBuilder();
		builder.setId("");
		builder.setCreateTime(model.getCreate_time().getTime()/1000L);
		builder.setTargetAccountId(model.getTarget_account_id());
		builder.setGiveNum(model.getGive_num());
		//builder.setAccountIp(model.getAccount_ip());
		//builder.setCode(model.getCode());
		builder.setTargetNickName(model.getTarget_nick_name());
		return builder;
	}
	
	/**
	 * 商城商品列表
	 * @return
	 */
	public static StoreGoodResponse.Builder getStoreGoodResponse(ShopModel model){
		StoreGoodResponse.Builder builder = StoreGoodResponse.newBuilder();
		builder.setId(model.getId());
		builder.setName(model.getName());
		builder.setGold(model.getGold());
		builder.setSendGold(model.getSend_gold());
		builder.setPrice(model.getPrice());
		if(!StringUtils.isEmpty(model.getIcon())){
			builder.setIcon(model.getIcon());
		}
		return builder;
	}
	
	
	/**
	 * 商城商品列表
	 * @return
	 */
	public static StoreGoodMoneyResponse.Builder getStoreMoneyResponse(MoneyShopModel model){
		StoreGoodMoneyResponse.Builder builder = StoreGoodMoneyResponse.newBuilder();
		builder.setId(model.getId());
		builder.setName(model.getName());
		builder.setMoney(model.getMoney());
		builder.setSendMoney(model.getSendMoney());
		builder.setPrice(model.getPrice());
		if(!StringUtils.isEmpty(model.getIcon())){
			builder.setIcon(model.getIcon());
		}
		return builder;
	}
	
	
	/**
	 * 商城商品列表
	 * @return
	 */
	public static StoreGoodResponse.Builder getStoreGoodResponse(AppShopModel model){
		StoreGoodResponse.Builder builder = StoreGoodResponse.newBuilder();
		builder.setId(model.getShop_id());
		builder.setName(model.getName());
		builder.setGold(model.getGold());
		builder.setSendGold(model.getSend_gold());
		builder.setPrice(model.getPrice());
		if(!StringUtils.isEmpty(model.getIcon())){
			builder.setIcon(model.getIcon());
		}
		return builder;
	}
	
	

	/**
	 * 主界面公告
	 * @param model
	 * @return
	 */
	public static MainUiNoticeItemResponse.Builder getMainUiNoticeItemResponse(MainUiNoticeModel model){
		MainUiNoticeItemResponse.Builder builder = MainUiNoticeItemResponse.newBuilder();
		builder.setId(model.getId());
		builder.setType(model.getType());
		builder.setContent(model.getContent());
		if(StringUtils.isNotEmpty(model.getHref())){
			builder.setHref(model.getHref());
		}
		return builder;
	}
	
	/**
	 * 登录公告
	 * @param model
	 * @return
	 */
	public static LoginNoticeResponse.Builder getLoginNoticeResponse(LoginNoticeModel model){
		LoginNoticeResponse.Builder builder = LoginNoticeResponse.newBuilder();
		builder.setTitle(model.getTitle());
		builder.setContent(model.getContent());
		return builder;
	}
	
	/**
	 * 邀请列表
	 * @param model
	 * @return
	 */
	public static ShareInviteInfoResponse.Builder getShareInviteInfoResponse(AccountRecommendModel model){
		ShareInviteInfoResponse.Builder builder = ShareInviteInfoResponse.newBuilder();
		builder.setAccountId(model.getTarget_account_id());
		builder.setCreateTime(model.getCreate_time().getTime());
		builder.setGoldNum(model.getGold_num());
		if(model.getTarget_name()==null){
			builder.setName("--");
		}else{
			builder.setName(model.getTarget_name());
		}
		if(model.getTarget_icon()==null){
			builder.setIcon("1.png");
		}else{
			builder.setIcon(model.getTarget_icon());
		}
		
		return builder;
	}
	
	/**
	 * 代理房间子项
	 * @param model
	 * @return
	 */
	public static ProxyRoomItemResponse.Builder getProxyRoomItemResponse(PrxoyPlayerRoomModel model,RoomRedisModel roomRedisModel){
		ProxyRoomItemResponse.Builder builder = ProxyRoomItemResponse.newBuilder();
		builder.setRoomId(model.getRoom_id());
		long k = GameConstants.CREATE_ROOM_PROXY_TIME_GAP*60*1000L;
		long k2 = model.getCreate_time() + k - System.currentTimeMillis();
		if(k2<0){
			builder.setSurplusTime(0);//
		}else{
			builder.setSurplusTime((int)(k2/1000L));
		}
		builder.setRoomMaxPeople(roomRedisModel.getPlayer_max());
		builder.setRoomCurPeople(roomRedisModel.getPlayersIdSet().size());
		builder.setRoomStatus(roomRedisModel.getRoomStatus());
		builder.setGameTypeIndex(roomRedisModel.getGame_type_index());
		builder.setGameRuleDes(roomRedisModel.getGameRuleDes()==null?"":roomRedisModel.getGameRuleDes());
		builder.setGameRound(roomRedisModel.getGame_round());
		return builder;
	}
	

}
