package com.lingyu.common.util;

import java.text.MessageFormat;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LogoUtil {
	private static final Logger logger = LogManager.getLogger(LogoUtil.class);
	public static LogoUtil instance;

	
	public static void print(String area,String version) {
		if(version==null){
			version="";
		}
		String logo="\n"
				+"                                  . . . \n"
				+"                          . . . . . .   \n"
				+"                        ... . . . .     \n"
				+"  .                     ..... . .       \n"
				+"  . ..                  ..... .         	   - ★ ★  -       \n"
				+"  . . ..              .......                     every bird has its own sky\n"
				+"    . ......          ......            	  Area:{0}  Version:{1}\n"
				+"        ::::::      ::::::              \n"
				+"          ..:::     ::::                \n"
				+"              :::  ∷∷∷                               \n"
				+"      		∷  ∷ ∷∷            			\n"
				+"            ..:          ...                     http://www.lingyuwangluo.com \n"
				+"         .::::              ‥             \n"
				+"       ::::::                  .           \n"
				+"     .::::::                               ";
		logger.info(MessageFormat.format(logo, area,version));
		
	}
	
	public static void main(String[] args) {
		LogoUtil.print("Tencent001","S20140606R1 ♂♀");	
	}
}