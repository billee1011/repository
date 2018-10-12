package com.cai.http.interceptor;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import com.cai.common.define.ELogType;
import com.cai.common.define.ESysLogLevelType;
import com.cai.common.util.PerformanceTimer;
import com.cai.core.SystemConfig;
import com.cai.service.MongoDBServiceImpl;



/**
 * 调试拦截器
 * @author run
 * @date 2013年11月26日
 */
public class DebugInterceptor implements HandlerInterceptor {

	private static Logger logger = LoggerFactory.getLogger(DebugInterceptor.class);

	public static ThreadLocal<Map<String,Object>> myhreadLocal = new ThreadLocal<Map<String,Object>>(); 
	
	private String tipPrefix = "##";

	@Override
	public boolean preHandle(HttpServletRequest request,
			HttpServletResponse response, Object handler) throws Exception {
		
		Map<String,Object> map = new HashMap<String,Object>();
		PerformanceTimer timer = new PerformanceTimer();
		map.put("timer", timer);
		map.put("buf", tipPrefix + "in控制器："+ handler + "\n");
		myhreadLocal.set(map);


		return true;
	}

	@Override
	public void postHandle(HttpServletRequest request,
			HttpServletResponse response, Object handler,
			ModelAndView modelAndView) throws Exception {
		
		
		Map<String,Object> map = myhreadLocal.get();
		PerformanceTimer timer = (PerformanceTimer)map.get("timer");
		
		StringBuffer buf = new StringBuffer();
		buf.append(map.get("buf"));
		
		buf.append(tipPrefix+"post==>\n")
		.append(tipPrefix+"modelAndView=" + modelAndView+ "\n")
		.append("view 渲染前方法执行时间：" + timer.getStr() + "\n");
		
		
		
		
		map.put("buf", buf.toString());
		

	}

	@Override
	public void afterCompletion(HttpServletRequest request,
			HttpServletResponse response, Object handler, Exception ex)
			throws Exception {
		
//		if(SystemConfig.gameDebug==0)
//			return;
		
		Map<String,Object> map = myhreadLocal.get();
		PerformanceTimer timer = (PerformanceTimer)map.get("timer");
		String proString = (String)map.get("buf");
		
		long executionTime = timer.get();
		
		StringBuffer buf = new StringBuffer();
		
		String originalAccessUrl = request.getRequestURL().toString();
		String accessMethod = request.getMethod();
		String parameters = "";
		String debugUrl = "";

		if ("get".equalsIgnoreCase(accessMethod)) {
			parameters = request.getQueryString();
			debugUrl = originalAccessUrl+ ((parameters == null)|| ("".equals(parameters.trim())) ? "": new StringBuilder("?").append(parameters).toString());
		} else {
			parameters = getParameters(request);
			debugUrl = originalAccessUrl+ ((parameters == null)|| ("".equals(parameters.trim())) ? "": new StringBuilder("?").append(parameters).toString());
		}

		
		
		buf.append(generateTip(tipPrefix, 20)+ "debugInterceptor info begin" + generateTip(tipPrefix, 20) + "\n");
		
		buf.append(proString);
		
		
		int i = 1;
		buf.append(tipPrefix + i++ + ".accessMethod："+ accessMethod+"\n");
		buf.append(tipPrefix + i++ + ".debugUrl："+ debugUrl+"\n");
		if ((parameters != null) && (!"".equals(parameters.trim()))) {
			buf.append(tipPrefix
					+ i++
					+ ".参数："
					+ "\n"
					+ tipPrefix
					+ "  "
					+ parameters.replaceAll("&", new StringBuilder("\n").append(tipPrefix).append("  ").toString()) + "\n");
		}
		
		
		buf.append(tipPrefix + i++ + ".报头==>"+"\n");
		Enumeration en = request.getHeaderNames();
		for(;en.hasMoreElements();){
			String h = (String)en.nextElement();
			buf.append("  " + h + ":" + request.getHeader(h) + "\n");
		}
		
		
		buf.append(tipPrefix + i++ + ".执行时间:"+ executionTime + "ms\n");
		
		buf.append(tipPrefix + i++ + ".返回结果:"+map.get("jsonResult") + "\n");
		
		
		buf.append(generateTip(tipPrefix, 20)+ "debugInterceptor info end" + generateTip(tipPrefix, 21)+"\n");
		//logger.info(buf.toString());//不要写到log4j中
		if(SystemConfig.gameDebug==1){
			System.out.println(buf.toString());
		}
		if(executionTime>200) {
			MongoDBServiceImpl.getInstance().systemLog(ELogType.webRequest, buf.toString(),executionTime,null, ESysLogLevelType.NONE);
		}
		
		
		
		myhreadLocal.remove();
		

	}

	
	//内部方法
	private String getParameters(ServletRequest request) {
		Enumeration paramNames = request.getParameterNames();
		StringBuffer params = new StringBuffer();
		while ((paramNames != null) && (paramNames.hasMoreElements())) {
			String paramName = (String) paramNames.nextElement();
			String[] values = request.getParameterValues(paramName);
			if ((values == null) || (values.length == 0))
				continue;
			if (values.length == 1)
				params.append(paramName + "=" + values[0] + "&");
			else {
				for (int i = 0; i < values.length; i++) {
					params.append(paramName + "=" + values[i] + "&");
				}
			}
		}
		if (params.length() > 0) {
			return params.substring(0, params.length() - 1);
		}
		return params.toString();
	}

	private String generateTip(String tipPrefix, int count) {
		StringBuffer tip = new StringBuffer();
		for (int i = 0; i < count; i++) {
			tip.append(tipPrefix.trim());
		}
		return tip.toString();
	}

}
