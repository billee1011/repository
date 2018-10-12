package com.lingyu.common.util;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.lingyu.common.core.ServiceException;

public class IPUtil {
	private static final Logger logger = LogManager.getLogger(IPUtil.class);

	// SiteLocalAddress=false,LoopbackAddress=false,address.getHostAddress()=115.29.176.102
	// 广域网IP
	// SiteLocalAddress=true,LoopbackAddress=false,address.getHostAddress()=10.161.175.91
	// 局域网IP
	// SiteLocalAddress=false,LoopbackAddress=true,address.getHostAddress()=127.0.0.1
	// 回环IP
	// 获取局域网IP
	public static String getIP() throws ServiceException {
		String ret = "";
		try {
			if (SystemUtils.IS_OS_LINUX) {
				Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();
				while (en.hasMoreElements()) {
					NetworkInterface ni = en.nextElement();
					Enumeration<InetAddress> enIp = ni.getInetAddresses();
					while (enIp.hasMoreElements()) {
						InetAddress inet = enIp.nextElement();
						if (inet.isSiteLocalAddress() && !inet.isLoopbackAddress() && (inet instanceof Inet4Address)) {
							ret = inet.getHostAddress().toString();
							// logger.info("SiteLocalAddress={},host={}",inet.isSiteLocalAddress(),
							// inet.getHostAddress().toString());
						}
					}
				}
			} else {
				ret = InetAddress.getLocalHost().getHostAddress();
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		if (StringUtils.isEmpty(ret)) {
			throw new ServiceException("获取本地IP失败");
		}
		return ret;
	}

	// 将127.0.0.1形式的IP地址转换成十进制整数，这里没有进行任何错误处理
	//将IP地址转换为主机字节序
	public static long ipToLong(String strIp) {
		long[] ip = new long[4];
		// 先找到IP地址字符串中.的位置
		int position1 = strIp.indexOf(".");
		int position2 = strIp.indexOf(".", position1 + 1);
		int position3 = strIp.indexOf(".", position2 + 1);
		// 将每个.之间的字符串转换成整型
		ip[0] = Long.parseLong(strIp.substring(0, position1));
		ip[1] = Long.parseLong(strIp.substring(position1 + 1, position2));
		ip[2] = Long.parseLong(strIp.substring(position2 + 1, position3));
		ip[3] = Long.parseLong(strIp.substring(position3 + 1));
		return (ip[0] << 24) + (ip[1] << 16) + (ip[2] << 8) + ip[3];
	}

	// 将十进制整数形式转换成127.0.0.1形式的ip地址
	//主机字节序转换为将IP地址
	public static String longToIP(long longIp) {
		StringBuffer sb = new StringBuffer("");
		// 直接右移24位
		sb.append(String.valueOf((longIp >>> 24)));
		sb.append(".");
		// 将高8位置0，然后右移16位
		sb.append(String.valueOf((longIp & 0x00FFFFFF) >>> 16));
		sb.append(".");
		// 将高16位置0，然后右移8位
		sb.append(String.valueOf((longIp & 0x0000FFFF) >>> 8));
		sb.append(".");
		// 将高24位置0
		sb.append(String.valueOf((longIp & 0x000000FF)));
		return sb.toString();
	}

	public static void main(String[] args) {
		String ipStr = "127.0.0.1";
		long longIp = IPUtil.ipToLong(ipStr);
		System.out.println("整数形式为：" + longIp);
		System.out.println("整数" + longIp + "转化成字符串IP地址：" + IPUtil.longToIP(longIp));
		// ip地址转化成二进制形式输出
		System.out.println("二进制形式为：" + Long.toBinaryString(longIp));
	}

}
