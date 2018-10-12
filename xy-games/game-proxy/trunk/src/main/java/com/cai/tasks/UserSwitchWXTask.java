package com.cai.tasks;

import java.util.Date;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSONObject;
import com.cai.common.constant.AccountConstant;
import com.cai.common.constant.AttributeKeyConstans;
import com.cai.common.constant.RMICmd;
import com.cai.common.constant.S2CCmd;
import com.cai.common.define.ELogType;
import com.cai.common.define.EPtType;
import com.cai.common.define.LoginType;
import com.cai.common.define.XYCode;
import com.cai.common.domain.Account;
import com.cai.common.domain.AccountModel;
import com.cai.common.domain.AccountWeixinModel;
import com.cai.common.rmi.ICenterRMIServer;
import com.cai.common.util.EmojiFilter;
import com.cai.common.util.MobileUtil;
import com.cai.common.util.MyStringUtil;
import com.cai.common.util.PBUtil;
import com.cai.common.util.Pair;
import com.cai.common.util.RandomUtil;
import com.cai.common.util.SessionUtil;
import com.cai.common.util.SpringService;
import com.cai.module.LoginModule;
import com.cai.service.C2SSessionService;
import com.cai.service.PlayerServiceImpl;
import com.cai.service.PtAPIServiceImpl;
import com.cai.util.MessageResponse;
import com.cai.util.MobileLogUtil;
import com.xianyi.framework.core.transport.netty.session.C2SSession;

import protobuf.clazz.c2s.C2SProto.PhoneSwitchWXRsp;

/**
 * 
 * 切换微信
 *
 * @author wu_hc date: 2017年12月6日 上午10:09:35 <br/>
 */
public final class UserSwitchWXTask implements Runnable {

	private final static Logger logger = LoggerFactory.getLogger(UserSwitchWXTask.class);

	private final String wxCode;
	private final int gameId;
	private final long accountId;

	/**
	 * 
	 * @param wxCode
	 * @param gameId
	 */
	public UserSwitchWXTask(final String wxCode, int gameId, long accountId) {
		this.wxCode = wxCode;
		this.gameId = gameId;
		this.accountId = accountId;
	}

	@Override
	public void run() {
		
		C2SSession session = C2SSessionService.getInstance().getSession(accountId);
		if (null == session) {
			return;
		}

		Account account = session.getAccount();
		if (null == account) {
			return;
		}

		final AccountModel accountModel = account.getAccountModel();
		if (null == accountModel) {
			return;
		}

		AccountWeixinModel wxModel = account.getAccountWeixinModel();
		if (null == wxModel) {
			return;
		}
		// 封号检测
		if (accountModel.getBanned() == 1) {
			session.send(MessageResponse.getMsgAllResponse("账号被封，请联系客服！").build());
			return;
		}

		final String oldUnid = wxModel.getUnionid();
		final String oldNickName = wxModel.getNickname();
		Integer loginType = SessionUtil.getAttr(session, AttributeKeyConstans.LOGIN_TYPE);
		if (null == loginType || LoginType.MOBILE != loginType.intValue()) {
			session.send(MessageResponse.getMsgAllResponse("登陆失败，需要手机登陆！").build());
			return;
		}

		if (MobileUtil.isMobileNull(accountModel.getMobile_phone())) {
			session.send(MessageResponse.getMsgAllResponse("你的帐号还没绑定手机，绑定手机后才能进行切换微信操作!!").build());
			return;
		}
		JSONObject jsonObject = PtAPIServiceImpl.getInstance().wxGetAccessTokenByCode(wxCode, gameId,account.getLastChannelId());
		if (jsonObject == null) {
			PlayerServiceImpl.getInstance().sendAccountMsg(session, MessageResponse.getMsgAllResponse("切换失败Error1!").build());
			return;
		}

		Integer errCode = 0;
		if (jsonObject.containsKey("errcode")) {
			errCode = (Integer) jsonObject.get("errcode");
		}

		if (errCode != 0) {
			logger.error("登录失败wx_code==" + wxCode + "errCode==" + errCode + "msg" + jsonObject);
			PlayerServiceImpl.getInstance().sendAccountMsg(session, MessageResponse.getMsgAllResponse("切换失败,code无效").build());
			return;
		}
		String unionid = jsonObject.getString("unionid");
		if (Objects.equals(wxModel.getUnionid(), unionid)) {
			logger.error("玩家[{}]切换微信息，但切换的相同微信[{}]，不支持!", account, unionid);
			session.send(MessageResponse.getMsgAllResponse("相同微信，不可以进行切换操作!!").build());
			return;
		}
		String access_token = jsonObject.getString("access_token");
		// int expires_in = jsonObject.getInteger("expires_in");
		String refresh_token = jsonObject.getString("refresh_token");
		String openid = jsonObject.getString("openid");
		String scope = jsonObject.getString("scope");

		// 用户详情
		jsonObject = PtAPIServiceImpl.getInstance().wxUserinfo(gameId, access_token, openid);
		if (jsonObject == null) {
			logger.error("登录失败wx_code==" + wxCode + "errCode==" + errCode);
			PlayerServiceImpl.getInstance().sendAccountMsg(session, MessageResponse.getMsgAllResponse("登录失败Error2!").build());
			return;
		}

		if (jsonObject.containsKey("errcode")) {
			errCode = (Integer) jsonObject.get("errcode");
		}
		if (errCode != 0) {
			PlayerServiceImpl.getInstance().sendAccountMsg(session, MessageResponse.getMsgAllResponse("登录失败,token无效").build());
			return;
		}

		openid = jsonObject.getString("openid");
		String nickname = jsonObject.getString("nickname");
		// nickname转码，过滤mysql识别不了的
		nickname = EmojiFilter.filterEmoji(nickname);
		// 长度控制
		nickname = MyStringUtil.substringByLength(nickname, AccountConstant.NICK_NAME_LEN);

		String sex = jsonObject.getString("sex");
		String province = jsonObject.getString("province");
		String city = jsonObject.getString("city");
		String country = jsonObject.getString("country");
		String headimgurl = jsonObject.getString("headimgurl");
		String privilege = jsonObject.getString("privilege");
		unionid = jsonObject.getString("unionid");// 全平台唯一id

		// 微信是否已经邦定过检测
		String accounName = EPtType.WX.getId() + "_" + unionid;
		Account tmp = SpringService.getBean(ICenterRMIServer.class).getAccount(accounName);
		if (null != tmp) {
			session.send(MessageResponse.getMsgAllResponse("绑定失败，此微信号已绑定闲逸棋牌账号！").build());
			return;
		}

		// 微信相关的
		AccountWeixinModel accountWeixinModel = account.getAccountWeixinModel();
		accountWeixinModel.setAccess_token(access_token);
		accountWeixinModel.setRefresh_token(refresh_token);
		accountWeixinModel.setOpenid(openid);
		accountWeixinModel.setScope(scope);
		accountWeixinModel.setUnionid(unionid);
		accountWeixinModel.setNickname(nickname);
		accountWeixinModel.setSex(sex);
		accountWeixinModel.setProvince(province);
		accountWeixinModel.setCity(city);
		accountWeixinModel.setCountry(country);
		accountWeixinModel.setHeadimgurl(headimgurl);
		accountWeixinModel.setPrivilege(privilege);
		accountWeixinModel.setLast_flush_time(new Date());
		accountWeixinModel.setSelf_token(LoginModule.encodeToken(accountWeixinModel.getAccount_id()));
		accountWeixinModel.setLast_false_self_token(new Date());
		Pair<Integer, String> r = SpringService.getBean(ICenterRMIServer.class).rmiInvoke(RMICmd.PHONE_SWITCH_WX, accountWeixinModel);
		logger.warn("玩家:[{}] 切换微信，结果:{}", account, r);
		session.send(MessageResponse.getMsgAllResponse(r.getSecond()).build());

		PhoneSwitchWXRsp.Builder builder = PhoneSwitchWXRsp.newBuilder();
		builder.setStatus(r.getFirst().intValue());
		if (r.getFirst().intValue() == XYCode.SUCCESS) {
			builder.setAccountRsp(MessageResponse.getAccountResponse(account));

		}
		session.send(PBUtil.toS2CCommonRsp(S2CCmd.PHONE_SWITCH_WX, builder));

		// 日志
		String msg = String.format("[%s,%s]->[%s,%s]", oldUnid, oldNickName, accountWeixinModel.getUnionid(), accountWeixinModel.getNickname());
		MobileLogUtil.log(account.getAccount_id(), ELogType.switchWx.getId(), null, r.getFirst().intValue(), msg, oldUnid,
				accountWeixinModel.getUnionid());
	}

}
