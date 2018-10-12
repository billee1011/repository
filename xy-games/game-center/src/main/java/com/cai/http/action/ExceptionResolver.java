package com.cai.http.action;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;

import com.cai.http.OssException;


public class ExceptionResolver implements HandlerExceptionResolver {

	private static Logger logger = LoggerFactory
			.getLogger(ExceptionResolver.class);

	@Override
	public ModelAndView resolveException(HttpServletRequest request,
			HttpServletResponse response, Object handler, Exception ex) {

		
	
		Map<String, Object> map = new HashMap<String, Object>();
		
		int code = -1;
		String errorStack = null;
		if(ex instanceof OssException){
			OssException ossEx = (OssException)ex;
			code = ossEx.code;
			errorStack = ossEx.getMessage();
		}else{
			StringPrintWriter strintPrintWriter = new StringPrintWriter();
			ex.printStackTrace(strintPrintWriter);
			errorStack = strintPrintWriter.getString();
			logger.error("SpringMVC异常: ", ex);// 把漏网的异常信息记入日志
		}
		map.put("errorStack", errorStack);// 将错误信息传递给view
		map.put("code", code);
		return new ModelAndView("error", map);
	}

}
