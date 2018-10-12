/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.handler.c2s;

import java.util.HashMap;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.constant.C2SCmd;
import com.cai.common.constant.RMICmd;
import com.cai.common.constant.S2CCmd;
import com.cai.common.domain.AccountModel;
import com.cai.common.domain.ClientUploadErWeiMaModel;
import com.cai.common.domain.SysParamModel;
import com.cai.common.rmi.ICenterRMIServer;
import com.cai.common.util.PBUtil;
import com.cai.common.util.SpringService;
import com.cai.dictionary.SysParamServerDict;
import com.xianyi.framework.core.transport.ICmd;
import com.xianyi.framework.core.transport.netty.session.C2SSession;
import com.xianyi.framework.handler.IClientHandler;

import protobuf.clazz.Protocol.Request;
import protobuf.clazz.basic.HallGuideProto.ClientPlayerUploadErWeiMaRequest;
import protobuf.clazz.basic.HallGuideProto.PlayerErWeiMaResponse;
import protobuf.clazz.basic.HallGuideProto.SingleErWeiMaData;
import protobuf.clazz.basic.HallGuideProto.UploadResultResponse;
import protobuf.clazz.basic.HallGuideProto.UploadRoleAndTypeResponse;


@ICmd(code = C2SCmd.CLIENT_PLAYER_UPLOAD_ERWEIMA_REQUEST, desc = "玩家在客户端上传二维码")
public final class ClientPlayerUploadErWeiMaHandler extends IClientHandler<ClientPlayerUploadErWeiMaRequest> {

	private static final Logger logger = LoggerFactory.getLogger(ClientPlayerUploadErWeiMaHandler.class);
	
	private static final int UPLOAD_CONTROL_AND_TYPE  = 1;				//玩家上传二维码的开关和上传类型
	private static final int PLAYER_ERWEIMA_DATA = 2;					//玩家二维码数据
	private static final int UPLOAD_ERWEIMA = 3;						//接收客户端上传的数据
	
	@Override
	protected void execute(ClientPlayerUploadErWeiMaRequest req, Request topRequest, C2SSession session) throws Exception {
		int paramType = req.getParamType();
		int primaryKey = req.getId();
		String imageUrl = req.getImageUrl();
		long accountId = session.getAccountID();
		switch (paramType) {
			case UPLOAD_CONTROL_AND_TYPE:
				session.send(PBUtil.toS2CCommonRsp(S2CCmd.UPLOAD_ROLE_AND_TYPE_RESPONSE, getUploadRoleAndType()));
				break;
			case PLAYER_ERWEIMA_DATA:
				session.send(PBUtil.toS2CCommonRsp(S2CCmd.PLAYER_ERWEIMA_RESPONSE, getPlayerErWeiMaImage(accountId)));
				break;
			case UPLOAD_ERWEIMA:
				session.send(PBUtil.toS2CCommonRsp(S2CCmd.CLIENT_PLAYER_UPLOAD_ERWEIMA_RESPONSE, uploadErWeiMa(session, imageUrl, primaryKey)));
				break;
		}
	}
	
	/**
	 * 获取玩家是否能上传,上传者的身份和上传方式
	 * @return
	 */
	public UploadRoleAndTypeResponse.Builder getUploadRoleAndType() {
		UploadRoleAndTypeResponse.Builder uploadRoleAndType = UploadRoleAndTypeResponse.newBuilder();
		SysParamModel paramServer = SysParamServerDict.getInstance().getSysParamModelDictionaryByGameId(6).get(2269);
		if (paramServer != null) {
			uploadRoleAndType.setRole(Integer.valueOf(paramServer.getStr1()));
		} else {
			uploadRoleAndType.setRole(0);	//若服务器参数未配置上传开关,则返回玩家不能上传的标识
		}
		return uploadRoleAndType;
	}
	
	/**
	 * 获取玩家的二维码图片
	 * @return
	 */
	public PlayerErWeiMaResponse.Builder getPlayerErWeiMaImage(long accountId) {
		PlayerErWeiMaResponse.Builder erweimaBuilder = PlayerErWeiMaResponse.newBuilder();
		try {
			HashMap<String, String> map = new HashMap<>();
			map.put("accountId", accountId + "");
			ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
			List<ClientUploadErWeiMaModel> dataList = centerRMIServer.rmiInvoke(RMICmd.FIND_PLAYER_CLIENT_UPLOAD_ERWEIMA, map);
			for (ClientUploadErWeiMaModel model : dataList) {
				SingleErWeiMaData.Builder single = SingleErWeiMaData.newBuilder();
				single.setId(model.getId());
				single.setAccountId(model.getAccountId());
				single.setImageUrl(model.getImage());
				single.setUploadStatus(model.getUploadStatus());
				erweimaBuilder.addSingle(single);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return erweimaBuilder;
	}
	
	/**
	 * 记录上传的玩家二维码
	 * @param accountId
	 * @param imageUrl
	 * @return
	 */
	public UploadResultResponse.Builder uploadErWeiMa(C2SSession session, String imageUrl, int primaryKey) {
		UploadResultResponse.Builder resultBuilder = UploadResultResponse.newBuilder();
		String uploadType = "1";			//上传方式:1及时上传2审核上传
		try {
			SysParamModel paramServer = SysParamServerDict.getInstance().getSysParamModelDictionaryByGameId(6).get(2269);
			if (paramServer != null) {
				String isAbleUpload = paramServer.getStr1();
				if ("0".equals(isAbleUpload)) {
					resultBuilder.setResult(0);
					resultBuilder.setMsg("上传二维码图片功能暂未开启");
					return resultBuilder;
				} else if ("2".equals(isAbleUpload)) {
					AccountModel account = session.getAccount().getAccountModel();
					if (account == null || account.getIs_agent() != 1) {
						resultBuilder.setResult(0);
						resultBuilder.setMsg("上传二维码图片暂时仅限代理用户");
						return resultBuilder;
					}
				}
				uploadType = paramServer.getStr2();
				HashMap<String, String> map = new HashMap<>();
				map.put("id", primaryKey + "");
				map.put("accountId", session.getAccountID() + "");
				map.put("imageUrl", imageUrl);
				map.put("uploadType", uploadType);
				ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
				HashMap<String, Object> resultMap = centerRMIServer.rmiInvoke(RMICmd.CLIENT_UPLOAD_ERWEIMA, map);
				if (Integer.valueOf(resultMap.get("result").toString()) == 1) {
					resultBuilder.setResult(Integer.valueOf(resultMap.get("result").toString()));
					@SuppressWarnings("unchecked")
					List<ClientUploadErWeiMaModel> dataList = (List<ClientUploadErWeiMaModel>) resultMap.get("data");
					for (ClientUploadErWeiMaModel model : dataList) {
						SingleErWeiMaData.Builder single = SingleErWeiMaData.newBuilder();
						single.setId(model.getId());
						single.setAccountId(model.getAccountId());
						single.setImageUrl(model.getImage());
						single.setUploadStatus(model.getUploadStatus());
						resultBuilder.addSingle(single);
					}
				} else {
					resultBuilder.setResult(0);
					resultBuilder.setMsg(resultMap.get("msg") == null ? "上传失败" : resultMap.get("msg").toString());
				}
			} else {
				resultBuilder.setResult(0);
				resultBuilder.setMsg("上传二维码图片功能暂未开启");
			}
		} catch (Exception e) {
			logger.error("CLIENT_UPLOAD_ERWEIMA rmi error", e);
		}
		return resultBuilder;
	}
	
}
