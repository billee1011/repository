package com.cai.service;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

import com.alibaba.fastjson.JSONObject;
import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.dysmsapi.model.v20170525.QuerySendDetailsRequest;
import com.aliyuncs.dysmsapi.model.v20170525.QuerySendDetailsResponse;
import com.aliyuncs.dysmsapi.model.v20170525.SendSmsRequest;
import com.aliyuncs.dysmsapi.model.v20170525.SendSmsResponse;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.profile.DefaultProfile;
import com.aliyuncs.profile.IClientProfile;
import com.cai.common.define.XYCode;
import com.cai.common.domain.SysParamModel;
import com.cai.common.util.Pair;
import com.cai.dictionary.SysParamServerDict;

/**
 *
 * @author Administrator
 */
public class SmsService {
	// 产品名称:云通信短信API产品,开发者无需替换
	private static final String product = "Dysmsapi";
	// 产品域名,开发者无需替换
	private static final String domain = "dysmsapi.aliyuncs.com";

	// TODO 此处需要替换成开发者自己的AK(在阿里云访问控制台寻找)
	private static final String accessKeyId = "LTAIXoPySJiBUE8i";
	private static final String accessKeySecret = "3tC4pTwhsvsWpCZ7rKrXTsd9hIuu68";
	/**
	 * 单例
	 */
	private static SmsService instance;

	private static IAcsClient acsClient = null;

	/**
	 * 私有构造
	 * 
	 * @throws ClientException
	 */
	private SmsService() throws ClientException {
		System.setProperty("sun.net.client.defaultConnectTimeout", "5000");
		System.setProperty("sun.net.client.defaultReadTimeout", "5000");
		// 初始化acsClient,暂不支持region化
		IClientProfile profile = DefaultProfile.getProfile("cn-hangzhou", accessKeyId, accessKeySecret);
		DefaultProfile.addEndpoint("cn-hangzhou", "cn-hangzhou", product, domain);
		acsClient = new DefaultAcsClient(profile);
	}

	public static SmsService getInstance() {
		if (null == instance) {
			try {
				instance = new SmsService();
			} catch (ClientException e) {
				e.printStackTrace();
			}
		}
		return instance;
	}

	/**
	 * 
	 * @param mobile
	 * @param templateParam
	 * @return
	 * @throws ClientException
	 */
	public Pair<Integer, String> sendSms(String mobile, String templateParam) throws ClientException {
		SendSmsResponse r = sendSms(mobile, templateParam, null, null);
		if (r.getCode() != null && r.getCode().equals("OK")) {
			return Pair.of(XYCode.SUCCESS, "-");
		} else {
			return Pair.of(XYCode.FAIL, r.getMessage());
		}

	}

	/**
	 * 
	 * @param mobile
	 *            手机号
	 * @param templateParam
	 *            替换json 一般是{\"code\":\"123456\"}
	 * @param smsId
	 *            短信模板id 发送验证码可以不传 SMS_115265235
	 * @param signName
	 *            短信签名，默认闲逸游戏
	 * @return SendSmsResponse
	 * @throws ClientException
	 */
	public SendSmsResponse sendSms(String mobile, String templateParam, String smsId, String signName) throws ClientException {
		SysParamModel sysParamModel2237 = SysParamServerDict.getInstance().getSysParamModelDictionaryByGameId(6).get(2237);
		SendSmsResponse sendSmsResponse = new SendSmsResponse();
		if (sysParamModel2237 != null && sysParamModel2237.getVal1() == 1) {
			String replaceContent = JSONObject.parseObject(templateParam).getString("code");
			boolean isOk = TempSmsService.send(mobile, replaceContent);
			if (isOk) {
				sendSmsResponse.setCode("OK");
				sendSmsResponse.setMessage("发送成功");
				return sendSmsResponse;
			} else {
				sendSmsResponse.setCode("error");
				sendSmsResponse.setMessage("发送失败，请稍后重试");
				return sendSmsResponse;
			}
		}
		Pattern p = Pattern.compile("^1[1|3|4|5|6|7|8|9]\\d{9}$");
		Matcher m = p.matcher(mobile);

		if (!m.matches()) {
			sendSmsResponse.setCode("isv.MOBILE_NUMBER_ILLEGAL");
			sendSmsResponse.setMessage("手机号码有误");
			return sendSmsResponse;
		}
		SysParamModel sysParamModel2254 = SysParamServerDict.getInstance().getSysParamModelDictionaryByGameId(6).get(2254);
		if (sysParamModel2254 != null && sysParamModel2254.getVal1() == 1) {
			if (StringUtils.isBlank(smsId)) {
				smsId = "SMS_136385584";
			}
			if (StringUtils.isBlank(signName)) {
				signName = "玩一局";
			}
		} else {
			if (StringUtils.isBlank(smsId)) {
				smsId = "SMS_115760101";
			}
			if (StringUtils.isBlank(signName)) {
				signName = "闲逸互娱";
			}
		}

		// 组装请求对象-具体描述见控制台-文档部分内容
		SendSmsRequest request = new SendSmsRequest();
		// 必填:待发送手机号
		request.setPhoneNumbers(mobile);
		// 必填:短信签名-可在短信控制台中找到
		request.setSignName(signName);// "闲逸游戏");
		// 必填:短信模板-可在短信控制台中找到
		request.setTemplateCode(smsId);// "SMS_115255180");
		// 可选:模板中的变量替换JSON串,如模板内容为"亲爱的${name},您的验证码为${code}"时,此处的值为
		request.setTemplateParam(templateParam);// "{\"code\":\"123456\"}");

		// 选填-上行短信扩展码(无特殊需求用户请忽略此字段)
		// request.setSmsUpExtendCode("90997");

		// 可选:outId为提供给业务方扩展字段,最终在短信回执消息中将此值带回给调用者
		request.setOutId("id:" + mobile);

		// hint 此处可能会抛出异常，注意catch
		sendSmsResponse = acsClient.getAcsResponse(request);
		if (sendSmsResponse.getCode() != null && sendSmsResponse.getCode().equals("OK")) {

		}
		return sendSmsResponse;
	}

	/**
	 * 查询短信发送记录
	 * 
	 * @param bizId
	 * @param mobile
	 * @param date
	 * @return
	 * @throws ClientException
	 */
	public static QuerySendDetailsResponse querySendDetails(String bizId, String mobile, Date date) throws ClientException {

		// 组装请求对象
		QuerySendDetailsRequest request = new QuerySendDetailsRequest();
		// 必填-号码
		request.setPhoneNumber(mobile);
		// 可选-流水号
		if (StringUtils.isNotBlank(bizId)) {
			request.setBizId(bizId);
		}
		// 必填-发送日期 支持30天内记录查询，格式yyyyMMdd
		SimpleDateFormat ft = new SimpleDateFormat("yyyyMMdd");
		request.setSendDate(ft.format(date));
		// 必填-页大小
		request.setPageSize(10L);
		// 必填-当前页码从1开始计数
		request.setCurrentPage(1L);
		// hint 此处可能会抛出异常，注意catch
		QuerySendDetailsResponse querySendDetailsResponse = acsClient.getAcsResponse(request);
		return querySendDetailsResponse;
	}

}
