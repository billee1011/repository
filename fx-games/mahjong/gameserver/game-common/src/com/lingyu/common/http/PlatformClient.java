package com.lingyu.common.http;

import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ssl.AllowAllHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.lingyu.common.core.ServiceException;

public class PlatformClient {
	private static final Logger logger = LogManager.getLogger(PlatformClient.class);
	private String hostName;
	private int timeout;

	public PlatformClient(String hostName, int timeout) {
		this.hostName = hostName;
		this.timeout = timeout;
	}
	
	public PlatformClient() {
	}

	public JSONObject get4https(String url) throws ServiceException {
		if (url == null) {
			url = "";
		}
		url = "https://" + hostName + url;
		logger.info("url={}", url);
		CloseableHttpClient client = this.createSSLInsecureClient();
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
						return entity != null ? EntityUtils.toString(entity) : null;
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

	private CloseableHttpClient createSSLInsecureClient() {
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
	public JSONObject get(String url) throws ServiceException {
		return get(url, true);
	}
	
	/**
	 * A {@link PoolingHttpClientConnectionManager} with maximum 100 connections
	 * per route and a total maximum of 200 connections is used internally.
	 */
	public JSONObject get(String url, boolean hostNameAddFlag) throws ServiceException {
		if(hostNameAddFlag){
			url = "http://" + hostName + url;
		}
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
			logger.error(e.getMessage(), e);
		}
		return null;
	}
	
	public static void main(String[] args) {
		// PlatformClient client = new
		// PlatformClient("http://192.168.1.21:8080/platform/", 200000);
		// JSONObject object =
		// client.get("getuser?sessionid=041B9EBBE273DD63F36FEEB94C67661F");
		// http://127.0.0.1:8080/admin/user/create.do?name=bird&password=123456&email=41157121@qq.com&platformIdList=[1,2]
		//
		for(int i=0;i<1;i++){
			PlatformClient client = new
					 PlatformClient("127.0.0.1:9001", 10000);
					 client.get4https("");
					 try {
						Thread.sleep(5000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
		}
		
		//
		// JSONObject object =
		// client.get("user/create.do?name=bird&password=123456&email=41157121@qq.com&platformIdList=[1,2]");
		// System.out.println(object);
		
//		PlatformClient client = new PlatformClient("119.147.19.43", 2000);
//		client.get4https("/v3/pay/buy_goods?openkey=972CA2B3290B87120772FD229B3A63E5&pf=website&ts=1415090827&zoneid=0&payitem=9%3AD88888*100*100&appid=1102562958&openid=241F2CA66817C3AF340B1DF392853FC6&goodsurl=http%3A%2F%2F1251173991.cdn.myqcloud.com%2F1251173991%2Fgame%2Fr%2Fc%2Fi%2Ftask_bdyb.png&pfkey=e3739c16d6132de3b655d570d3975186&goodsmeta=%E9%92%BB%E7%9F%B3*%E8%BF%99%E6%98%AF%E4%B8%80%E9%A2%97%E9%92%BB%E7%9F%B3&sig=9Zkv43DPIouc4zSOgjQPdsCA9ZY%3D");
//	
	}
}
