package com.lingyu.common.util;

import java.io.BufferedInputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.zip.CRC32;
import java.util.zip.CheckedOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.jpountz.lz4.LZ4Compressor;
import net.jpountz.lz4.LZ4Factory;
import net.jpountz.lz4.LZ4SafeDecompressor;

/**
 * 压缩文件的通用工具类-采用org.apache.tools.zip.ZipOutputStream实现，较复杂。
 * 
 * @author wangning
 * @date 2017年3月9日 下午4:22:25
 */
public class ZipCompressor {
	private static final Logger logger = LogManager.getLogger(ZipCompressor.class);
	static final int BUFFER = 8192;
	private File zipFile;
	public static LZ4Factory factory;
	public static LZ4Compressor compressor;
	public static boolean inited = false;

	public static void checkInit() {
		if (!inited) {
			factory = LZ4Factory.fastestInstance();
			compressor = factory.fastCompressor();
			inited = true;
		}
	}

	public static byte[] unZip(byte[] data) {
		byte[] b = null;
		try {
			ByteArrayInputStream bis = new ByteArrayInputStream(data);
			ZipInputStream zip = new ZipInputStream(bis);
			while (zip.getNextEntry() != null) {
				byte[] buf = new byte[1024];
				int num = -1;
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				while ((num = zip.read(buf, 0, buf.length)) != -1) {
					baos.write(buf, 0, num);
				}
				b = baos.toByteArray();
				baos.flush();
				baos.close();
			}
			zip.close();
			bis.close();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return b;
	}

	public static byte[] unGZip(byte[] data) {
		byte[] b = null;
		try {
			ByteArrayInputStream bis = new ByteArrayInputStream(data);
			GZIPInputStream gzip = new GZIPInputStream(bis);
			byte[] buf = new byte[1024];
			int num = -1;
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			while ((num = gzip.read(buf, 0, buf.length)) != -1) {
				baos.write(buf, 0, num);
			}
			b = baos.toByteArray();
			baos.flush();
			baos.close();
			gzip.close();
			bis.close();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return b;
	}

	/**
	 * 使用zip进行压缩
	 * 
	 * @param str 压缩前的文本
	 * @return 返回压缩后的文本
	 */
	public static final byte[] zip(byte[] source) {
		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			GZIPOutputStream gzip = new GZIPOutputStream(out);
			gzip.write(source);
			gzip.close();
			// PrintUtil.print("old zip2:"+out.toByteArray().length);
			return out.toByteArray();
		} catch (UnsupportedEncodingException e) {
			logger.error(e.getMessage(), e);
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		}
		return null;
	}

	public static void main(String[] args) {
		String val = "hello world";
		byte[] v1 = val.getBytes();
		byte[] compressed = newZip(v1);
		byte[] deCompressed = newUnGZip(compressed);
		System.out.println(new String(deCompressed));
	}

	public static byte[] newUnGZip(byte[] data) {
		// TODO 【业务标记】如果已知长度可优化
		checkInit();
		final int decompressedLength = data.length;
		// - method 2: when the compressed length is known (a little slower)
		// the destination buffer needs to be over-sized
		LZ4SafeDecompressor decompressor2 = factory.safeDecompressor();
		byte[] temp = new byte[5000];

		int decompressedLength2 = decompressor2.decompress(data, 0, data.length, temp, 0);
		byte[] result = new byte[decompressedLength2];
		for (int i = 0; i < result.length; i++) {
			result[i] = temp[i];
		}

		// decompressedLength == decompressedLength2
		return result;
	}

	/**
	 * lz4压缩方法
	 * @param source
	 * @return
	 */
	public static final byte[] newZip(byte[] source) {
		checkInit();
		final int decompressedLength = source.length;
		int maxCompressedLength = compressor.maxCompressedLength(decompressedLength);
		byte[] compressed = new byte[maxCompressedLength];
		int compressedLength = compressor.compress(source, 0, decompressedLength, compressed, 0, maxCompressedLength);
		byte[] results = new byte[compressedLength];
		for (int i = 0; i < results.length; i++) {
			results[i] = compressed[i];
		}
		return results;
	}

	/**
	 * 压缩文件构造函数
	 * @param pathName 压缩的文件存放目录
	 */
	public ZipCompressor(String pathName) {
		zipFile = new File(pathName);
	}

	/**
	 * 执行压缩操作
	 * 
	 * @param srcPathName 被压缩的文件/文件夹
	 */
	public void compressExe(String srcPathName) {
		File file = new File(srcPathName);
		if (!file.exists()) {
			throw new RuntimeException(srcPathName + "不存在！");
		}
		try {
			FileOutputStream fileOutputStream = new FileOutputStream(zipFile);
			CheckedOutputStream cos = new CheckedOutputStream(fileOutputStream, new CRC32());
			ZipOutputStream out = new ZipOutputStream(cos);
			String basedir = "";
			compressByType(file, out, basedir);
			out.close();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

	/**
	 * 判断是目录还是文件，根据类型（文件/文件夹）执行不同的压缩方法
	 * 
	 * @param file
	 * @param out
	 * @param basedir
	 */
	private void compressByType(File file, ZipOutputStream out, String basedir) {
		/* 判断是目录还是文件 */
		if (file.isDirectory()) {
			logger.info("压缩：" + basedir + file.getName());
			this.compressDirectory(file, out, basedir);
		} else {
			logger.info("压缩：" + basedir + file.getName());
			this.compressFile(file, out, basedir);
		}
	}

	/**
	 * 压缩一个目录
	 * 
	 * @param dir
	 * @param out
	 * @param basedir
	 */
	private void compressDirectory(File dir, ZipOutputStream out, String basedir) {
		if (!dir.exists()) {
			return;
		}

		File[] files = dir.listFiles();
		for (int i = 0; i < files.length; i++) {
			/* 递归 */
			compressByType(files[i], out, basedir + dir.getName() + "/");
		}
	}

	/**
	 * 压缩一个文件
	 * 
	 * @param file
	 * @param out
	 * @param basedir
	 */
	private void compressFile(File file, ZipOutputStream out, String basedir) {
		if (!file.exists()) {
			return;
		}
		try {
			BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
			ZipEntry entry = new ZipEntry(basedir + file.getName());
			out.putNextEntry(entry);
			int count;
			byte data[] = new byte[BUFFER];
			while ((count = bis.read(data, 0, BUFFER)) != -1) {
				out.write(data, 0, count);
			}
			bis.close();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static short[] toBiggerShort(byte[] b2) {
		short[] vals = new short[b2.length];
		for (int i = 0; i < b2.length; i++) {
			vals[i] = b2[i] >= 0 ? (short) b2[i] : (short) (b2[i] + 256);
		}
		return vals;
	}

	public static byte[] toSmallByte(byte[] b2) {
		byte[] vals = new byte[b2.length / 2];
		for (int i = 0; i < vals.length; i++) {
			vals[i] = b2[i * 2 + 1];
		}
		return vals;
	}

	/**
	 * 将字符串压缩为gzip流
	 * 
	 * @param content
	 * @return
	 */
	public static byte[] gzip(String content) {
		ByteArrayOutputStream baos = null;
		GZIPOutputStream out = null;
		byte[] ret = null;
		try {
			baos = new ByteArrayOutputStream();
			out = new GZIPOutputStream(baos);
			out.write(content.getBytes());
			out.close();
			baos.close();
			ret = baos.toByteArray();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (out != null) {
				try {
					baos.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (out != null) {
				try {
					out.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return ret;
	}

	/**
	 * 将gzip流解压为字符串
	 * 
	 * @param inputStream
	 * @return
	 */
	public static String gzipStream2Str(InputStream inputStream) {
		try {
			GZIPInputStream gzipinputStream = new GZIPInputStream(inputStream);
			byte[] buf = new byte[1024];
			int num = -1;
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			while ((num = gzipinputStream.read(buf, 0, buf.length)) != -1) {
				baos.write(buf, 0, num);
			}
			return new String(baos.toByteArray(), "utf-8");
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
}
