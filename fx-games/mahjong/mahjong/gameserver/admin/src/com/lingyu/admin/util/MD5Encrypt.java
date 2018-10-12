package com.lingyu.admin.util;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MD5Encrypt
{
	private static Logger logger = LoggerFactory.getLogger(MD5Encrypt.class);
	//
	public static char[] hexDigits =
	{ '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd',
			'e', 'f' };
	/**
	 * 
	 * @param arg
	 * @return
	 * @throws NoSuchAlgorithmException
	 */
	public static final String encrypt(String arg)
	{
		String encryptStr = "";
		try
		{
			if (arg == null)
				return null;
			byte[] source = arg.getBytes();
			MessageDigest messageDigest = MessageDigest.getInstance("MD5");
			messageDigest.update(source);
			byte[] tmp = messageDigest.digest();
			char str[] = new char[16 * 2];
			int k = 0;
			for (int i = 0; i < 16; i++)
			{
				byte byte0 = tmp[i];
				str[k++] = hexDigits[byte0 >>> 4 & 0xf];
				str[k++] = hexDigits[byte0 & 0xf];
			}
			encryptStr = new String(str);
		}
		catch (Exception e)
		{
			logger.error("error encrypt()", e);
			encryptStr = null;
		}
		return encryptStr;
	}
	/**
	 * 
	 * @param args
	 */
	public static void main(String[] args)
	{
		System.out.println(encrypt(""));
		System.out.println(encrypt("a"));
		System.out.println(encrypt("abc"));
	}
}
