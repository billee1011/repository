package com.cai.util;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.core.MediaType;

import org.json.JSONException;
import org.json.JSONObject;

import com.cai.common.define.ESmsSignType;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import com.sun.jersey.core.util.MultivaluedMapImpl;

/**
 *
 * @author Administrator
 */
public class TempSmsService {
	private static String SMS_URL = "http://sms-api.luosimao.com/v1/send.json";
	private static String SMS_API_KEY = "b1ce54dbe8cdf6b888f8a92bb0649762";
	private static WebResource webResource = null;

	public static WebResource getWebResource() {
		if (webResource == null) {
			Client client = Client.create();
			client.addFilter(new HTTPBasicAuthFilter("api", SMS_API_KEY));
			webResource = client.resource(SMS_URL);
		}
		return webResource;
	}

	// 发送短信验证码接口
	public static boolean sendCode(String mobile, String replaceContent, ESmsSignType eSmsSignType) {
		try {
			Pattern p = Pattern.compile("^1[1|3|4|5|6|7|8|9]\\d{9}$");
			Matcher m = p.matcher(mobile);
			if (!m.matches()) {
				return false;
			}
			MultivaluedMapImpl formData = new MultivaluedMapImpl();
			formData.add("mobile", mobile);
			formData.add("message", "您的验证码为" + replaceContent + "该验证码五分钟有效，请勿泄漏他人" + eSmsSignType.getDesc());
			ClientResponse response = getWebResource().type(MediaType.APPLICATION_FORM_URLENCODED).post(ClientResponse.class, formData);
			String textEntity = response.getEntity(String.class);
			System.out.println(getWebResource().toString() + formData.toString() + " Send message " + textEntity);
			int status = response.getStatus();
			if (status == 200) {
				return true;
			} else {
				return false;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	/**
	 * 发送自定义短信内容
	 * 
	 * @param mobile
	 * @param content
	 * @param eSmsSignType
	 *            短信签名 【玩一局】、【闲逸游戏】
	 */
	public static void sendDefineContent(String mobile, String content, ESmsSignType eSmsSignType) {
		try {
			Pattern p = Pattern.compile("^1[1|3|4|5|6|7|8|9]\\d{9}$");
			Matcher m = p.matcher(mobile);
			if (!m.matches()) {
				return;
			}
			MultivaluedMapImpl formData = new MultivaluedMapImpl();
			formData.add("mobile", mobile);
			formData.add("message", content + eSmsSignType.getDesc());
			ClientResponse response = getWebResource().type(MediaType.APPLICATION_FORM_URLENCODED).post(ClientResponse.class, formData);
			String textEntity = response.getEntity(String.class);
			System.out.println(getWebResource().toString() + formData.toString() + " Send message " + textEntity);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void batchSendMsg(Set<String> mobileList, String content) {
		try {
			for (String mobile : mobileList) {
				Pattern p = Pattern.compile("^1[1|3|4|5|6|7|8|9]\\d{9}$");
				Matcher m = p.matcher(mobile);
				if (!m.matches()) {
					continue;
				}
				MultivaluedMapImpl formData = new MultivaluedMapImpl();
				formData.add("mobile", mobile);
				formData.add("message", content + "【闲逸游戏】");
				ClientResponse response = getWebResource().type(MediaType.APPLICATION_FORM_URLENCODED).post(ClientResponse.class, formData);
				String textEntity = response.getEntity(String.class);
				System.out.println(getWebResource().toString() + formData.toString() + " Send message " + textEntity);
				int status = response.getStatus();
				if (status != 200) {
					System.out.println(mobile + " Send message error!");
				}
				break;
			}
		} catch (Exception e) {
		}
	}

	public static void main(String[] args) {
		TempSmsService SmsService = new TempSmsService();
		String httpResponse = SmsService.testSend();
		try {
			JSONObject jsonObj = new JSONObject(httpResponse);
			int error_code = jsonObj.getInt("error");
			String error_msg = jsonObj.getString("msg");
			if (error_code == 0) {
				System.out.println("Send message success.");
			} else {
				System.out.println("Send message failed,code is " + error_code + ",msg is " + error_msg);
			}
		} catch (JSONException ex) {
			ex.printStackTrace();
		}

		// httpResponse = SmsService.testStatus();
		// try {
		// JSONObject jsonObj = new JSONObject( httpResponse );
		// int error_code = jsonObj.getInt("error");
		// if( error_code == 0 ){
		// int deposit = jsonObj.getInt("deposit");
		// System.out.println("Fetch deposit success :"+deposit);
		// }else{
		// String error_msg = jsonObj.getString("msg");
		// System.out.println("Fetch deposit failed,code is "+error_code+",msg
		// is "+error_msg);
		// }
		// } catch (JSONException ex) {
		// Logger.getLogger(SmsService.class.getName()).log(Level.SEVERE, null,
		// ex);
		// }

	}

	private String testSend() {
		// just replace key here
		Client client = Client.create();
		client.addFilter(new HTTPBasicAuthFilter("api", SMS_API_KEY));
		WebResource webResource = client.resource(SMS_URL);
		MultivaluedMapImpl formData = new MultivaluedMapImpl();
		formData.add("mobile", "13026640938");
		formData.add("message", "尊敬的代理您好，您的兑换的闲逸豆已经到账【闲逸游戏】");
		ClientResponse response = webResource.type(MediaType.APPLICATION_FORM_URLENCODED).post(ClientResponse.class, formData);
		String textEntity = response.getEntity(String.class);
		int status = response.getStatus();
		// System.out.print(textEntity);
		System.out.println(webResource.toString() + formData.toString() + " Send message " + textEntity);
		return textEntity;
	}

	private static String testStatus() {
		Client client = Client.create();
		client.addFilter(new HTTPBasicAuthFilter("api", "b1ce54dbe8cdf6b888f8a92bb0649762"));
		WebResource webResource = client.resource("http://sms-api.luosimao.com/v1/status.json");
		MultivaluedMapImpl formData = new MultivaluedMapImpl();
		ClientResponse response = webResource.get(ClientResponse.class);
		String textEntity = response.getEntity(String.class);
		int status = response.getStatus();
		// System.out.print(status);
		// System.out.print(textEntity);
		return textEntity;
	}
}
