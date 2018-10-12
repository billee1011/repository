package com.lingyu.admin.util;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.support.RequestContext;

import com.lingyu.common.entity.User;
import com.lingyu.servlet.RpcContext;

public abstract class SessionUtil {
	final static Logger logger = LoggerFactory.getLogger(SessionUtil.class);
	public final static String USER_KEY = "USER";
	public final static String AREA_ID_KEY = "AREAR_ID";
	public final static String PLATFORM_ID_KEY = "PLATFORM_ID";
	public final static String I18N_KEY = "I18N";
	public final static String BATTLE_AREA_TYPE_KEY = "battle_area_type";

	public static RequestContext getRequestContext() {
		return (RequestContext) SessionUtil.getSessionValue(SessionUtil.I18N_KEY);
	}

	public static int getBattleAreaType() {
		int ret=0;
		Object obj= SessionUtil.getSessionValue(SessionUtil.BATTLE_AREA_TYPE_KEY);
		if(obj!=null){
			ret=(int)obj;
		}
		return ret;
	}

	public static void setBattleAreaType(int type) {
		setSessionValue(SessionUtil.BATTLE_AREA_TYPE_KEY, type);
	}

	public static User getCurrentUser() {
		return (User) SessionUtil.getSessionValue(SessionUtil.USER_KEY);
	}

	public static int getCurrentAreaId() {
		return (int) SessionUtil.getSessionValue(SessionUtil.AREA_ID_KEY);
	}

	public static void removeCurrentSession() {
		SessionUtil.removeSessionAttribute(SessionUtil.USER_KEY);
		SessionUtil.removeSessionAttribute(SessionUtil.AREA_ID_KEY);
	}

	public static Object getSessionValue(String name) {
		Object value = null;
		HttpServletRequest hsr = RpcContext.getHttpRequest();
		if (hsr != null) {
			HttpSession hs = hsr.getSession();
			if (hs != null) {
				value = hs.getAttribute(name);
			} else {
				logger.warn("Can't get HttpSession.");
			}
		} else {
			logger.warn("Can't get HttpServletRequest.");
		}
		return value;
	}

	public static void setSessionValue(String key, Object value) {
		HttpServletRequest hsr = RpcContext.getHttpRequest();
		if (hsr != null) {
			HttpSession hs = hsr.getSession();
			if (hs != null) {
				hs.setAttribute(key, value);
			} else {
				logger.warn("Can't get HttpSession.");
			}
		} else {
			logger.warn("Can't get HttpServletRequest.");
		}
	}

	public static void removeSessionAttribute(String key) {
		HttpServletRequest hsr = RpcContext.getHttpRequest();
		if (hsr != null) {
			HttpSession hs = hsr.getSession();
			if (hs != null) {
				hs.removeAttribute(key);
			} else {
				logger.warn("Can't get HttpSession.");
			}
		} else {
			logger.warn("Can't get HttpServletRequest.");
		}
	}

	public static String getI18nMessage(String code) {
		RequestContext requestContext = SessionUtil.getRequestContext();
		if (requestContext == null) {
			HttpServletRequest request = RpcContext.getHttpRequest();
			requestContext = new RequestContext(request);
			// Locale myLocale = requestContext.getLocale();//获取locale对象
			SessionUtil.setSessionValue(SessionUtil.I18N_KEY, requestContext);
		}

		return requestContext.getMessage(code);
	}
}
