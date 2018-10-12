package com.lingyu.common.http;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

public class PostClient {
	private static final Logger logger = LogManager.getLogger(PostClient.class);

	private int timeout;

	public PostClient(int timeout) {
		this.timeout = timeout;
	}

	/**
	 * 发送post请求到
	 * 
	 * @param url
	 * @param params
	 * @return
	 */
	public JSONObject post(String url, Map<String, String> params) {
		JSONObject object = null;
		CloseableHttpClient httpclient = HttpClients.createDefault();
		try {
			HttpPost httpPost = new HttpPost(url);
			httpPost.setConfig(RequestConfig.custom().setConnectTimeout(timeout).setSocketTimeout(timeout).setConnectionRequestTimeout(timeout).build());
			List<NameValuePair> nvps = new ArrayList<NameValuePair>();
			if (MapUtils.isNotEmpty(params)) {
				for (Entry<String, String> entry : params.entrySet()) {
					nvps.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
				}
			}
			httpPost.setEntity(new UrlEncodedFormEntity(nvps, "utf-8"));
			CloseableHttpResponse response = httpclient.execute(httpPost);

			try {
				HttpEntity entity = response.getEntity();
				String retStr = EntityUtils.toString(entity);
				if (StringUtils.isNotEmpty(retStr)) {
					object = JSON.parseObject(retStr);
				}
				EntityUtils.consume(entity);
			} finally {
				response.close();
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		} finally {
			try {
				httpclient.close();
			} catch (IOException e) {
				logger.error(e.getMessage(), e);
			}
		}

		return object;
	}

	public static void main(String[] args) throws UnsupportedEncodingException {
		// appid=1102562958&openid=5A852537538FA98F368B5977862EE13A&openkey=192574DFFFF068E79E68082E9319B4CC&uri=/game6/checkin?gameId=baofeng

		PostClient pc = new PostClient(5000);

		String url = "http://cgi.tiantian.qq.com/tiantian/get_qiqi_info";
		Map<String, String> params = new HashMap<>();
		params.put("appid", "1102562958");
		params.put("openid", "86F0046D489A7F7DE3D9F028F1DFF216");
		params.put("openkey", "411F89FF8F2CD4CC18D957462782D41A");
		params.put("uri", "/game6/checkin?gameId=baofeng");
		JSONObject jo = pc.post(url, params);
		System.out.println(jo);
		System.out.println(JSON.parseObject(jo.getJSONObject("result").getString("result")).getString("msg"));
	}
}
