package com.lingyu.common.util;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.lingyu.noark.amf3.Amf3;
import com.opencsv.CSVReader;

public class FileUtil {
	private static final Logger logger = LogManager.getLogger(FileUtil.class);

	public static Object[] parse(String fileName) {
		byte[] bytes = load(fileName);
		Object[] ret = decode(bytes);
		return ret;
	}

	public static byte[] load(String fileName) {
		try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(new File(fileName))); ByteArrayOutputStream out = new ByteArrayOutputStream();) {
			int tmp;
			while ((tmp = in.read()) != -1) {
				out.write(tmp);
			}
			return out.toByteArray();
		} catch (Exception e) {
			logger.error(e.getMessage(), fileName);
			return null;
		}
	}

	public static Object[] decode(byte[] fileData) {
		if (fileData == null) {
			return null;
		}
		return (Object[]) Amf3.parse(fileData);
	}
	
	public static Object[] parseCSV(String fileName){
		try(BufferedReader fReader = new BufferedReader(new InputStreamReader(new FileInputStream(fileName),"UTF-8"));) {
			CSVReader csvReader = new CSVReader(fReader); 
			csvReader.readNext();
			String names[] = csvReader.readNext(); 
			Map<Integer, String> attributeName = new HashMap<>();
			int i = 1;
			for(String name : names){
				if(StringUtils.isNotEmpty(name)){
					attributeName.put(i++, name);
				}
			}
			List<Map<String, Object>> result = new ArrayList<>();
			
			List<String[]> list = csvReader.readAll(); 
			Map<String, Object> map = null; 
			for(String objs[] : list){
				map = new HashMap<>();
				i = 1;
				for(String obj : objs){
					String attName = attributeName.get(i);
					if(StringUtils.isEmpty(attName)){
						continue;
					}
					map.put(attributeName.get(i), obj);
					i++;
				}
				result.add(map);
			}
			csvReader.close();
			return result.toArray();
		} catch (Exception e) {
			logger.error(e.getMessage(), fileName);
			return null;
		}
	}
	
	/**
	 * 写入文件
	 * @param filePath 文件路径
	 * @param bytes 具体内容
	 */
	public static void write(String filePath, byte bytes[]){
		try {
			File file = new File(filePath);
			if(!file.exists()){
				file.createNewFile();
			}
			FileOutputStream out = new FileOutputStream(file);
			out.write(bytes);
			out.close();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}
	
	/**
	 * 读取文件
	 * @param filePath文件路径 
	 * @return 目前存在文件里的是以json格式的
	 */
	public static String read(String filePath){
		try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(filePath)); ByteArrayOutputStream out = new ByteArrayOutputStream();) {
			int tmp;
			while ((tmp = in.read()) != -1) {
				out.write(tmp);
			}
			return out.toString();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		return null;
	}
	
	/**
	 * 下载图片
	 * @param imageUrl
	 * @param localPath
	 * @param imgName 需要加后缀
	 */
	public static boolean downImg(String imageUrl, String localPath, String imgName) {
		boolean flag = false;
		try {
			// 构造URL
			URL url = new URL(imageUrl);
			// 打开连接
			URLConnection con = url.openConnection();
			// 输入流
			InputStream is = con.getInputStream();
			// 1K的数据缓冲
			byte[] bs = new byte[1024];
			// 读取到的数据长度
			int len;
			File file = new File(localPath);
			if (!file.exists()) {
				file.mkdirs();
			}
			// 输出的文件流
			OutputStream os = new FileOutputStream(localPath + imgName);
			// 开始读取
			while ((len = is.read(bs)) != -1) {
				os.write(bs, 0, len);
			}
			// 完毕，关闭所有链接
			os.close();
			is.close();
			flag = true;
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		return flag;
	}
	
//  FileReader fReader = new FileReader(file);  
 /* BufferedReader fReader = new BufferedReader(new InputStreamReader(new FileInputStream(file),"utf-8"));
  CSVReader csvReader = new CSVReader(fReader);  
  String[] strs = csvReader.readNext();  
  if(strs != null && strs.length > 0){  
      for(String str : strs)  
          if(null != str && !str.equals(""))  
              System.out.print(str + " , ");  1
      System.out.println("\n---------------");  
  }  
  List<String[]> list = csvReader.readAll();  
  for(String[] ss : list){  
      for(String s : ss)  
          if(null != s && !s.equals(""))  
              System.out.print(s + " , ");  
      System.out.println();  
  }  
  csvReader.close();  */
	// public static byte[] load(String fileName) {
	// BufferedInputStream in = null;
	// try {
	// File file = new File(fileName);
	// in = new BufferedInputStream(new FileInputStream(file));
	// ByteArrayOutputStream out = new ByteArrayOutputStream();
	// int tmp;
	// while ((tmp = in.read()) != -1) {
	// out.write(tmp);
	// }
	// byte[] data = out.toByteArray();
	// return data;
	// } catch (Exception e) {
	// logger.error(e.getMessage(), fileName);
	// } finally {
	// if (null != in) {
	// try {
	// in.close();
	// } catch (IOException e) {
	// logger.error(e.getMessage(), fileName);
	// }
	// }
	// }
	// return null;
	// }
	//
	// public static Object[] decode(byte[] fileData) {
	// try {
	// if (fileData != null) {
	// ByteArrayInputStream in = new ByteArrayInputStream(fileData);
	// Amf3Input input = new Amf3Input(new SerializationContext());
	// input.setInputStream(in);
	// Object[] os = (Object[]) input.readObject();
	// return os;
	// } else {
	// return null;
	// }
	//
	// } catch (Exception e) {
	// logger.error(e.getMessage(), e);
	// return null;
	// }
	// }
}
