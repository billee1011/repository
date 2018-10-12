package com.lingyu.servlet;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

public class RequestEncoderFilter implements Filter {
	public static String DEFULT_ENCODE = "UTF-8";
	private String encode;

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		encode = filterConfig.getInitParameter("encode");
		if (encode == null) {
			encode = DEFULT_ENCODE;
		}
	}

	@Override
	public void doFilter(ServletRequest servletRequest,
			ServletResponse servletResponse, FilterChain filterChain)
			throws IOException, ServletException {
		if (servletRequest.getCharacterEncoding() == null) {
			servletRequest.setCharacterEncoding(encode);
		}
		filterChain.doFilter(servletRequest, servletResponse);
	}

	@Override
	public void destroy() {
		encode = null;
	}
}
