package com.cai.http.interceptor;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.ui.ModelMap;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import com.cai.config.SystemConfig;
import com.cai.http.security.SignUtil;

/**
 * 后台管理系统拦截器
 * @author run
 * @date 2013年11月26日
 * <pre>
 * 说明：
 * 	1.控制返回的json的一致性
 * 	
 * </pre>
 */
public class OssInterceptor implements HandlerInterceptor {

	private static ThreadLocal<Map<String,Object>> myhreadLocal = new ThreadLocal<Map<String,Object>>(); 
	

	@Override
	public boolean preHandle(HttpServletRequest request,
			HttpServletResponse response, Object handler) throws Exception {

		Map<String,Object> myMap = new HashMap<String,Object>();
		myMap.put("t1", System.currentTimeMillis());
		myhreadLocal.set(myMap);
		
		String currentURL = request.getRequestURI(); // 取得根目录所对应的绝对路径
		if( currentURL.contains("club")||currentURL.contains("web")) {
			//自动验证，失败抛出异常
			SignUtil.verifyCenterSign(request,SystemConfig.webSecret);
		}
		return true;
	}

	@Override
	public void postHandle(HttpServletRequest request,
			HttpServletResponse response, Object handler,
			ModelAndView modelAndView) throws Exception {
		
		Map<String,Object> myMap =myhreadLocal.get();
		long t1 = (Long)myMap.get("t1");
		
		if(modelAndView!=null) {
			ModelMap map = modelAndView.getModelMap();
			if(!map.containsKey("code")){
				map.put("code", 0);
			}
			map.put("remoteTime", (System.currentTimeMillis() -t1));
		}

	} 

	@Override
	public void afterCompletion(HttpServletRequest request,
			HttpServletResponse response, Object handler, Exception ex)
			throws Exception {

	}

}
