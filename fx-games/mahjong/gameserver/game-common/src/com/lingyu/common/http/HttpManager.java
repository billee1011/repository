package com.lingyu.common.http;

import java.io.IOException;
import java.net.URLEncoder;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.net.ssl.SSLContext;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.AllowAllHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.lingyu.common.core.ServiceException;

public class HttpManager {
	private static final Logger logger = LogManager.getLogger(HttpManager.class);

	private static int timeout = 4000;
	
	public static HttpManager getInstance() {
		return InstanceHolder.INSTANCE;
	}

	private static class InstanceHolder {
		private static final HttpManager INSTANCE = new HttpManager();
	}


	public static JSONObject get4https(String url) throws ServiceException {
		if (url == null) {
			url = "";
		}
		logger.info("url={}", url);
		CloseableHttpClient client = createSSLInsecureClient();
		try {
			HttpGet httpGet = new HttpGet(url);
			// 设置建立连接超时时间
			 // 设置读数据超时时间
			RequestConfig config = RequestConfig.custom().setConnectionRequestTimeout(timeout).setSocketTimeout(timeout).setConnectTimeout(timeout).build();
			httpGet.setConfig(config);
	// get responce
			// Create a custom response handler
			ResponseHandler<String> responseHandler = new ResponseHandler<String>() {
				public String handleResponse(final HttpResponse response) throws ClientProtocolException, IOException {
					int status = response.getStatusLine().getStatusCode();
					if (status >= 200 && status < 300) {
						HttpEntity entity = response.getEntity();
						if(entity == null){
							return null;
						}
						return new String(EntityUtils.toString(entity).getBytes("ISO-8859-1"),"UTF-8"); 
					} else {
						throw new ClientProtocolException("Unexpected response status: " + status);
					}
				}

			};
			long start = System.nanoTime();
			String ret = client.execute(httpGet, responseHandler);
			logger.info("recv delay={} ms", (System.nanoTime() - start) / 1000000f);
			if (ret != null) {
				JSONObject object = JSON.parseObject(ret);
				return object;
			}
		//	System.out.println(responseBody);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		} finally {
			try {
				client.close();
			} catch (IOException e) {
				logger.error(e.getMessage(), e);
			}
		}

		return null;

	}

	private static CloseableHttpClient createSSLInsecureClient() {
		try {
			SSLContext sslContext = new SSLContextBuilder().loadTrustMaterial(null, new TrustStrategy() {
				// 信任所有
				@Override
				public boolean isTrusted(X509Certificate[] chain, String authType) throws CertificateException {
					return true;
				}
			}).build();
			SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslContext, new AllowAllHostnameVerifier());
			return HttpClients.custom().setSSLSocketFactory(sslsf).build();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		return HttpClients.createDefault();
	}

	/**
	 * A {@link PoolingHttpClientConnectionManager} with maximum 100 connections
	 * per route and a total maximum of 200 connections is used internally.
	 */
	public static JSONObject get(String url) throws ServiceException {
		logger.info("url={}", url);
		Request request = Request.Get(url);
		long start = System.nanoTime();
		try {
			String ret = request.connectTimeout(timeout).socketTimeout(timeout).execute().returnContent().asString();
			logger.info("recv delay={} ms", (System.nanoTime() - start) / 1000000f);
			if (ret != null) {
				JSONObject object = JSON.parseObject(ret);
				return object;
			}

		} catch (IOException e) {
			logger.error("connection error,url={}", url);
			JSONObject fail = new JSONObject();
			fail.put("errorCode", "0");
			return fail;
		}
		return null;
	}
	
	/**
	 * 发送get请求。不要返回值
	 * @param url
	 * @throws ServiceException
	 */
	public static void getNoReturn(String url) throws ServiceException {
		Request request = Request.Get(url);
		try {
			request.connectTimeout(timeout).socketTimeout(timeout).execute();
		} catch (IOException e) {
			logger.error("connection error,url={}", url);
		}
	}
	
	/**
	 * 发送post请求到
	 * 
	 * @param url
	 * @param params
	 * @return
	 */
	public static JSONObject post(String url, Map<String, String> params) {
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
	
	/**
	 * 组装http链接
	 * @param sendParams
	 * @return
	 */
	public static String getParamString(String url, Map<String, Object> sendParams){
		StringBuilder sb = new StringBuilder();
		sb.append(url);
		int i = 0;
		for(String key : sendParams.keySet()){
			if(i == 0){
				sb.append("?");
			}else{
				sb.append("&");
			}
			i++;
			try {
				sb.append(key)
				  .append("=")
				  .append(URLEncoder.encode(sendParams.get(key).toString(), "utf-8"));
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}
		}
		return sb.toString();
	}

}
