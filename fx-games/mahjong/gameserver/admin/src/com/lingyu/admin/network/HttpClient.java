package com.lingyu.admin.network;

import java.io.IOException;
import java.util.LinkedList;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.Consts;
import org.apache.http.NameValuePair;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.alibaba.fastjson.JSON;
import com.lingyu.common.core.ServiceException;

/**
 * @author allen Jiang 用于给单个HTTP server 发同样请求时用
 * */
public class HttpClient {
	private static final Logger logger = LogManager.getLogger(HttpClient.class);
	protected String ip;
	protected int port;
	protected int timeout;
	private String urlPrefix;

	// private CloseableHttpClient httpclient = HttpClients.createDefault();

	public HttpClient(String ip, int port, int timeout) {
		this.ip = ip;
		this.port = port;
		this.timeout = timeout;
		urlPrefix = "http://" + ip + ":" + port + "/getdata?";
	}

	public String sendWithReturn(Object msg) throws ServiceException {
		return this.sendWithReturn(msg, timeout);
	}

	public String sendWithReturn(Object msg, int timeout) throws ServiceException {
		String funcName = StringUtils.uncapitalize(StringUtils.substringBefore(msg.getClass().getSimpleName(), "_"));
		LinkedList<NameValuePair> list = new LinkedList<NameValuePair>();
		list.add(new BasicNameValuePair("funName", funcName));
		list.add(new BasicNameValuePair("param", JSON.toJSONString(msg)));
		String url = urlPrefix + URLEncodedUtils.format(list, Consts.UTF_8);
		logger.info("url={}", url);
		// GET
		// http://192.168.2.181:10086/getdata?funName=test&param=%7B%22name%22%3A%22bird%22%7D
		// http://192.168.1.21:10086/getdata?funName=test&param={%22name%22:%22bird%22}

		// String
		// url="http://192.168.2.181:10086/getdata?"+URLEncodedUtils.format(list,
		// Consts.UTF_8);

		long start = System.nanoTime();
		try {
			Request request = Request.Post(url);

			String ret = request.connectTimeout(timeout).socketTimeout(timeout).execute().returnContent().asString();
			logger.info("recv type={},exec={} ms,len={} byte", funcName, (System.nanoTime() - start) / 1000000f, ret.length());
			return ret;
			// Test_S2C_Msg ret=JSON.parseObject(content.asString(),
			// Test_S2C_Msg.class);

		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		}
		return null;
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public int getTimeout() {
		return timeout;
	}

	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

	public void destory() {

	}
}
