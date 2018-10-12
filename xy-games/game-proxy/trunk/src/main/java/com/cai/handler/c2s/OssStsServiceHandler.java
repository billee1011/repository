/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 *
 */
package com.cai.handler.c2s;

import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.exceptions.ServerException;
import com.aliyuncs.http.MethodType;
import com.aliyuncs.http.ProtocolType;
import com.aliyuncs.profile.DefaultProfile;
import com.aliyuncs.profile.IClientProfile;
import com.aliyuncs.sts.model.v20150401.AssumeRoleRequest;
import com.aliyuncs.sts.model.v20150401.AssumeRoleResponse;
import com.cai.common.constant.C2SCmd;
import com.cai.common.constant.S2CCmd;
import com.cai.common.domain.Account;
import com.cai.common.domain.SysParamModel;
import com.cai.common.util.PBUtil;
import com.cai.dictionary.SysParamServerDict;
import com.xianyi.framework.core.transport.ICmd;
import com.xianyi.framework.core.transport.netty.session.C2SSession;
import com.xianyi.framework.handler.IClientHandler;

import protobuf.clazz.Protocol.Request;
import protobuf.clazz.ossSts.OssStsRsp.OssStsClientRequest;
import protobuf.clazz.ossSts.OssStsRsp.OssStsClientResponse;
import protobuf.clazz.ossSts.OssStsRsp.OssStsClientResponse.Builder;

/**
 * 
 */
@ICmd(code = C2SCmd.OSS_STS_AUTH)
public class OssStsServiceHandler extends IClientHandler<OssStsClientRequest> {
	@Override
	protected void execute(OssStsClientRequest req, Request topRequest, C2SSession session) throws Exception {
		// TODO Auto-generated method stub
		Account account = session.getAccount();
		if (account == null)
			return;
		String accessKeyId="LTAI6plAhAari5zq";
		String accessKeySecret="RUIDpRnd6lWTD6r7cAziqWN4YPBBdY";
        //需要在RAM控制台获取，此时要给子账号权限，并建立一个角色，把这个角色赋给子账户，这个角色会有一串值，就是rolearn要填的　　　　　　　　　　//记得角色的权限，子账户的权限要分配好，不然会报错
        String roleArn = "acs:ram::1617076849828548:role/aliyunosstokengeneratorrole";//临时Token的会话名称，自己指定用于标识你的用户，主要用于审计，或者用于区分Token颁发给谁
        SysParamModel SysParamModel2230 = SysParamServerDict.getInstance().getSysParamModelDictionaryByGameId(6).get(2230);
        if(SysParamModel2230!=null){
        	accessKeyId=SysParamModel2230.getStr1();
        	accessKeySecret = SysParamModel2230.getStr2();
        	roleArn = SysParamModel2230.getParam_desc();
        }
        String roleSessionName = "external-username";//这个可以为空，不好写，格式要对，无要求建议为空
        String policy = "{\n" +
                "    \"Version\": \"1\", \n" +
                "    \"Statement\": [\n" +
                "        {\n" +
                "            \"Action\": [\n" +
                "                \"oss:GetBucket\", \n" +
                "                \"oss:GetObject\", \n" +
                "                \"oss:AbortMultipartUpload\", \n" +
                "                \"oss:PutObject\" \n" +
                "            ], \n" +
                "            \"Resource\": [\n" +
                "                \"acs:oss:*:*:*\"\n" +
                "            ], \n" +
                "            \"Effect\": \"Allow\"\n" +
                "        }\n" +
                "    ]\n" +
                "}";
        ProtocolType protocolType = ProtocolType.HTTPS;
        try {
            AssumeRoleResponse response = assumeRole(accessKeyId, accessKeySecret, roleArn,
                    roleSessionName, policy, protocolType);
            String accesskeyid = response.getCredentials().getAccessKeyId();
            String accesskeysecret = response.getCredentials().getAccessKeySecret();
            String securityToken = response.getCredentials().getSecurityToken();
            Builder res = OssStsClientResponse.newBuilder();
            res.setAccessKeyId(accesskeyid);
            res.setAccesKeySecret(accesskeysecret);
            res.setSecurityToken(securityToken);
            session.send(PBUtil.toS2CCommonRsp(S2CCmd.OSS_STS_AUTH, res));
        } catch (ClientException e) {
            e.printStackTrace();
        }
	}
	// 目前只有"cn-hangzhou"这个region可用, 不要使用填写其他region的值
    public static final String REGION_CN_HANGZHOU = "cn-hangzhou";
    // 当前 STS API 版本
    public static final String STS_API_VERSION = "2015-04-01";
    //静态方法，方便调用
    private static AssumeRoleResponse assumeRole(String accessKeyId, String accessKeySecret,
                                         String roleArn, String roleSessionName, String policy,
                                         ProtocolType protocolType) throws ClientException, ServerException, com.aliyuncs.exceptions.ClientException {
      try {
        // 创建一个 Aliyun Acs Client, 用于发起 OpenAPI 请求
        IClientProfile profile = DefaultProfile.getProfile(REGION_CN_HANGZHOU, accessKeyId, accessKeySecret);
        DefaultAcsClient client = new DefaultAcsClient(profile);
        // 创建一个 AssumeRoleRequest 并设置请求参数
        final AssumeRoleRequest request = new AssumeRoleRequest();
        request.setVersion(STS_API_VERSION);
        request.setMethod(MethodType.POST);
        request.setProtocol(protocolType);
        request.setRoleArn(roleArn);
        request.setRoleSessionName(roleSessionName);
        request.setPolicy(policy);
        // 发起请求，并得到response
        final AssumeRoleResponse response = client.getAcsResponse(request);
        return response;
      } catch (ClientException e) {
        throw e;
      }
    }

}
