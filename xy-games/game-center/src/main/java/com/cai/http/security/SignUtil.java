package com.cai.http.security;


import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.codec.digest.DigestUtils;

import com.cai.http.ErrorCode;
import com.cai.http.OssException;
import com.cai.http.SnsSigCheck;

public class SignUtil {
	
	/**
	 * 获取请求参数
	 * @param request
	 * @return
	 */
	public static Map<String,String> getParametersHashMap(HttpServletRequest request){
		
		Map<String,String> params = new HashMap<String,String>();
		Enumeration paramNames = request.getParameterNames();
		while ((paramNames != null) && (paramNames.hasMoreElements())) {
			String paramName = (String) paramNames.nextElement();
			params.put(paramName,request.getParameter(paramName));
		}
		return params;
	}
	
	
	/**
	 * 验证中心充值加密(web请求)
	 * @param request
	 * @param key
	 * @return
	 * @throws Exception
	 */
	public static void verifyCenterSign(HttpServletRequest request,String appKey) throws Exception{
		
		HashMap<String,String> params = new HashMap<String,String>();
		params.putAll(getParametersHashMap(request));
		String requestSig = params.get("sign");
		//必要参数sign
		if(!params.containsKey("sign")) {
			throw new OssException(ErrorCode.PARAMETER_EMPTY,"sign is null");
		}
		
		//一定要有time参数
		if(!params.containsKey("time")) {
			throw new OssException(ErrorCode.PARAMETER_EMPTY,"time is null");
		}
			
		Long time = null;
		try {
			 time = Long.valueOf(params.get("time"));
		} catch (Exception e) {
			throw new OssException(ErrorCode.PARAMETER_INVALID,"time is long");
		}
		//是否过期
		long nowTime = System.currentTimeMillis();
		
		if(time > (nowTime+1000*60*5L)){
			throw new OssException(ErrorCode.TIME_INVALID,"time is long");
		}
			
		
		//删除不参与加密的参数
		params.remove("sign");
		
		String sig =generateSign(params, appKey);
		if(!requestSig.equalsIgnoreCase(sig)){
			throw new OssException(ErrorCode.MAKE_SIGNATURE_ERROR,"sign is error");
		}
	}
	
	/**
	 * 生成加密参数(web请求)
	 * @param args
	 * @param key
	 * @param validTime
	 * @return
	 * @throws Exception
	 */
	public static String generateSign(Map<String,String> args,String appKey) throws Exception{
		
		//不改变请求数据，复制副本
		HashMap<String, String> params = new HashMap<String, String>();
		if (args != null)
			params.putAll(args);
		
		// 无需传sig,会自动生成
		params.remove("sign");
		if(!params.containsKey("time"))
			throw new Exception("时间截不能为空");
		
		//排序求参数
		Object[] keys = params.keySet().toArray();
		Arrays.sort(keys);
		StringBuilder buf = new StringBuilder();
		for(int i=0;i<keys.length;i++){
			String pv = params.get(keys[i]);
			//排除
			if(pv==null)
				continue;
			buf.append(pv+"&");
		}
		buf.append(appKey);
		String mySign = md5Check(buf.toString());
		return mySign;
	}
	
	/**
	 * md5加密
	 * 
	 * @param str
	 * @return
	 */
	public static String md5Check(String str) {
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			md.update(str.getBytes());
			byte[] byteDigest = md.digest();
			int i;
			StringBuffer buf = new StringBuffer("");
			for (int offset = 0; offset < byteDigest.length; offset++) {
				i = byteDigest[offset];
				if (i < 0)
					i += 256;
				if (i < 16)
					buf.append("0");
				buf.append(Integer.toHexString(i));
			}
			// 32位加密
			return buf.toString();
			// 16位的加密
			// return buf.toString().substring(8, 24);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * 验证加密(web请求)
	 * @param request
	 * @param key
	 * @return
	 * @throws Exception
	 */
	public static void verifySign(HttpServletRequest request,String secret) throws Exception{
		
		
		//统一请求加密验证
		String scriptName = request.getServletPath() + request.getPathInfo();
		String method = request.getMethod().toUpperCase();
		
		//copy副本，不改本原来的
		HashMap<String,String> params = new HashMap<String,String>();
		params.putAll(SignUtil.getParametersHashMap(request));
		String requestSig = params.get("sig");
		//必要参数sign
		if(!params.containsKey("sig"))
			throw new OssException(-2,"sig is null");
		
		//一定要有time参数
		if(!params.containsKey("time"))
			throw new OssException(-2,"time is null");
		
		Long time = null;
		try {
			 time = Long.valueOf(params.get("time"));
		} catch (Exception e) {
			throw new OssException(-2,"time is not number");
		}
		//是否过期
		long nowTime = System.currentTimeMillis();
		
		if(time<nowTime)
			throw new OssException(-2,"time is expired");
		
		//有效时长不能超过1h
		if(time > (nowTime+1000*60*60L))
			throw new OssException(-2,"time is illegal");
		
		//删除不参与加密的参数
		params.remove("sig");
		
		String sig = SnsSigCheck.makeSig(method, scriptName, params, secret);
		if(!requestSig.equalsIgnoreCase(sig))
			throw new OssException(-2,"sig is fail");
		
	}
	
	/**
	 * 生成加密参数(web请求)
	 * @param args
	 * @param key
	 * @param validTime
	 * @return
	 * @throws Exception
	 */
	public static String generateSign(String scriptName,Map<String,String> args,String key) throws Exception{
		
		//不改变请求数据，复制副本
		HashMap<String, String> params = new HashMap<String, String>();
		if (args != null)
			params.putAll(args);
		
		// 无需传sig,会自动生成
		params.remove("sign");
		if(!params.containsKey("time"))
			throw new OssException(-1,"时间截不能为空");
		
		//排序求参数
		Object[] keys = params.keySet().toArray();
		Arrays.sort(keys);
		StringBuilder buf = new StringBuilder();
		buf.append(scriptName+"&");
		for(int i=0;i<keys.length;i++){
			String pv = params.get(keys[i]);
			//排除
			if(pv==null)
				continue;
			buf.append(pv+"&");
		}
		buf.append(key);
		System.out.println("测：原加密串="+buf.toString());
		String mySign = DigestUtils.md5Hex(buf.toString());
		return mySign;
	}
	
	
	public static void main(String[] args) throws Exception {
		
		String script = "/oss/myt.json";
		Map<String,String> map = new HashMap<String,String>();
		map.put("time", (System.currentTimeMillis()+1000*60*30) + "");
		String sign = generateSign(script,map,"TTTTTTT");
		System.out.println(script+"?time="+map.get("time")+"&sign="+sign);
//		
	}
	
	

}
