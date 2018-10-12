package com.cai.common.util;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class WRSystem {
	
	private static Logger logger = LoggerFactory.getLogger(WRSystem.class);
	
	/**
	 * 获取环境变量ME2_HOME的目录 部署在服务器中时，当前执行目录为./bin
	 */
	public static String HOME;

	static {
		
		
		//读取config.txt里的面文件
		try {
			File file = new File("./config.txt");
			String str = FileUtils.readFileToString(file, "UTF-8").trim();			
			HOME = "./global_config/"+str+"/";
			System.out.println("确定配置文件位置:" + HOME);
			
		} catch (Exception e) {
			logger.error("error",e);
		}
		
		
/*		
		HOME = System.getenv("WR_HOME");
		if (HOME == null || HOME.equals("")) {
			HOME = "./";

			// 自动识别我家的,用不同配置文件
			try {
				InetAddress ia = InetAddress.getLocalHost();
				String localip = ia.getHostAddress();
				if (localip.indexOf("192.168.199") != -1) {
					System.out.println("本机的ip是 ：" + localip+",自动切换到run家里配置");
					HOME = "./path_crz/";
				}

			} catch (Exception e) {
				e.printStackTrace();
			}

		}*/
	}
	
}
