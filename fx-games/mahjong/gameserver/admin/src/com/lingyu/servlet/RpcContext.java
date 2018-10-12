package com.lingyu.servlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class RpcContext
{
	/**对每个线程都会创建一个局部变量*/
	private static ThreadLocal<HttpServletRequest> request = new ThreadLocal<HttpServletRequest>();
	private static ThreadLocal<HttpServletResponse> response = new ThreadLocal<HttpServletResponse>();
	/**
	 * 
	 * @param request
	 */
	public static void setThreadLocalObjects(HttpServletRequest req,
			HttpServletResponse res)
	{
		request.set(req);
		response.set(res);
	}
	/**
	 * 
	 */
	public static void clearThreadLocalObjects()
	{
		request.remove();
		response.remove();
	}
	/**
	 * 
	 * @return
	 */
	public static HttpServletRequest getHttpRequest()
	{
		return request.get();
	}
	/**
	 * 
	 * @return
	 */
	public static HttpServletResponse getHttpResponse()
	{
		return response.get();
	}
}
