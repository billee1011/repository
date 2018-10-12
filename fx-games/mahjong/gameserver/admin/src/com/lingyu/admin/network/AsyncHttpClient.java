package com.lingyu.admin.network;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.Consts;
import org.apache.http.NameValuePair;
import org.apache.http.client.fluent.Async;
import org.apache.http.client.fluent.Content;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.message.BasicNameValuePair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.alibaba.fastjson.JSON;
import com.esotericsoftware.reflectasm.MethodAccess;
import com.lingyu.admin.AdminServerContext;
import com.lingyu.common.core.ServiceException;
import com.lingyu.common.entity.IRpcOwner;
import com.lingyu.common.http.MethodWrapper;
import com.lingyu.common.manager.GameThreadFactory;
import com.lingyu.msg.http.ISerialaIdable;

/**
 * @author allen Jiang
 * 这是一个高性能的异步HTTP客户端，用于给多个HTTP server 发同样请求时用
 * */
public class AsyncHttpClient {
	private static final Logger logger = LogManager.getLogger(AsyncHttpClient.class);
	private Map<String, MethodWrapper> cachedMethod = new HashMap<String, MethodWrapper>();
	ExecutorService threadpool = new ThreadPoolExecutor(1, 20, 60L, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(500),new GameThreadFactory("async-http"),
			new ThreadPoolExecutor.CallerRunsPolicy());
	private Async async = Async.newInstance().use(threadpool);

	private AsyncHttpClient() {
	}

	public static AsyncHttpClient getInstance() {
		return InstanceHolder.INSTANCE;
	}

	private static class InstanceHolder {
		private static final AsyncHttpClient INSTANCE = new AsyncHttpClient();
	}

	
	public void init() {
		logger.info("异步HTTP客户端初始化开始");
		HttpClientHandler handler = AdminServerContext.getBean(HttpClientHandler.class);
		Method[] methods = handler.getClass().getDeclaredMethods();
		MethodAccess access = MethodAccess.get( handler.getClass());
		for (Method method : methods) {
			String methodName = method.getName();
			String lowerCaseMethodName = methodName.toLowerCase();
			int methodIndex = access.getIndex(method.getName(), method.getParameterTypes());
			cachedMethod.put(lowerCaseMethodName, new MethodWrapper(method, handler,access,methodIndex));
		}
		logger.info("异步HTTP客户端初始化完毕");
	}
	public void destory(){
		logger.info("异步HTTP客户端停止开始");
		threadpool.shutdown();
		logger.info("异步HTTP客户端停止完毕");
	}

	public void send(Collection<? extends IRpcOwner> list, Object msg) {
		String funcName = StringUtils.uncapitalize(StringUtils.substringBefore(msg.getClass().getSimpleName(), "_"));
		LinkedList<NameValuePair> paramlist = new LinkedList<NameValuePair>();
		paramlist.add(new BasicNameValuePair("funName", funcName));
		paramlist.add(new BasicNameValuePair("param", JSON.toJSONString(msg)));
		String url = URLEncodedUtils.format(paramlist, Consts.UTF_8);
		logger.info("url={}", url);
		int serialId = 0;
		if(msg instanceof ISerialaIdable){
			serialId = ((ISerialaIdable)msg).getSerialId();
		}
		final int sid = serialId;
		for (IRpcOwner area : list) {
			if (area.isValid() && area.getFollowerId() == 0) {
					String urlPrefix = "http://" + area.getIp() + ":" + area.getPort() + "/getdata?";
					Request request = Request.Post(urlPrefix + url).connectTimeout(60000).socketTimeout(60000);
					async.execute(request, new FutureCallback<Content>() {
						public void failed(final Exception ex) {
							logger.error(ex.getMessage(), ex);
						}

						public void completed(final Content content) {
							if (content != null) {
								Object msg = JSON.parse(content.asString());
								if(sid != 0 && msg instanceof ISerialaIdable){
									((ISerialaIdable)msg).setSerialId(sid);
								}
								String funcName = StringUtils.uncapitalize(StringUtils.substringBefore(msg.getClass().getSimpleName(), "_"));
								MethodWrapper wrapper = matcherMethod(funcName);
								call(wrapper, msg);
							}
						}

						public void cancelled() {

						}
					});
			}
		}

	}

	/**
	 * 
	 * @param functionName
	 * @param parameter null，表示是无参数的调用
	 * @return
	 */
	private void call(MethodWrapper wrapper, Object parameter) throws ServiceException {
		try {
			// 有参数调用
			wrapper.invoke(parameter);
			//

		} catch (Exception e) {
			throw new ServiceException(e);
		}
	}

	/**
	 * 
	 * @param functionName
	 * @param parameter
	 * @return
	 */
	private MethodWrapper matcherMethod(String functionName) {
		String lowerCaseFunctionName = functionName == null ? null : functionName.toLowerCase();
		return cachedMethod.get(lowerCaseFunctionName);
	}

}
