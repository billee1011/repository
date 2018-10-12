package com.cai.http.interceptor;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.ui.ModelMap;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import com.cai.core.SystemConfig;
import com.cai.http.security.SignUtil;

/**
 * 后台管理系统拦截器
 * 
 * @author run
 * @date 2013年11月26日
 * 
 *       <pre>
 * 说明：
 * 	1.控制返回的json的一致性
 * 
 *       </pre>
 */
public class OssInterceptor implements HandlerInterceptor {

	private static ThreadLocal<Map<String, Object>> myhreadLocal = new ThreadLocal<Map<String, Object>>();

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

		Map<String, Object> myMap = new HashMap<String, Object>();
		myMap.put("t1", System.currentTimeMillis());
		myhreadLocal.set(myMap);

		String currentURL = request.getRequestURI(); // 取得根目录所对应的绝对路径
		if (currentURL.contains("index/pay") || currentURL.contains("henan/pay") || currentURL.contains("hall/recommend")
				|| currentURL.contains("exclusive/") || currentURL.contains("hall/detail") || currentURL.contains("active/pay")
				|| currentURL.contains("recommend/detail") || currentURL.contains("phone/operate")) {
			// 自动验证，失败抛出异常
			SignUtil.verifyCenterSign(request, SystemConfig.webSecret);
		}
		// if (currentURL.contains("room/detail")) {
		// String ip = getIpAddress(request);
		// if (SystemConfig.gameDebug == 1 || IpUtil.isWhiteIp(ip)) {
		// return true;
		// }
		// return false;
		// }

		return true;
	}

	@Override
	public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {

		Map<String, Object> myMap = myhreadLocal.get();
		long t1 = (Long) myMap.get("t1");

		if (modelAndView != null) {
			ModelMap map = modelAndView.getModelMap();
			if (!map.containsKey("code")) {
				map.put("code", 0);
			}
			map.put("remoteTime", (System.currentTimeMillis() - t1));
		}

	}

	@Override
	public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {

	}

	public final static String getIpAddress(HttpServletRequest request) throws IOException {
		// 获取请求主机IP地址,如果通过代理进来，则透过防火墙获取真实IP地址

		String ip = request.getHeader("X-Forwarded-For");

		if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
			if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
				ip = request.getHeader("Proxy-Client-IP");

			}
			if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
				ip = request.getHeader("WL-Proxy-Client-IP");

			}
			if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
				ip = request.getHeader("HTTP_CLIENT_IP");

			}
			if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
				ip = request.getHeader("HTTP_X_FORWARDED_FOR");

			}
			if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
				ip = request.getRemoteAddr();

			}
		} else if (ip.length() > 15) {
			String[] ips = ip.split(",");
			for (int index = 0; index < ips.length; index++) {
				String strIp = (String) ips[index];
				if (!("unknown".equalsIgnoreCase(strIp))) {
					ip = strIp;
					break;
				}
			}
		}
		return ip;
	}

}
