package com.lingyu.noark.data.accessor.mysql;

import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.util.Date;

import javax.sql.DataSource;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.lingyu.noark.data.FieldMapping;
import com.lingyu.noark.data.accessor.AccessType;
import com.lingyu.noark.data.accessor.ValueAdaptor;
import com.lingyu.noark.data.exception.DataAccessException;
import com.lingyu.noark.data.exception.DataException;
import com.lingyu.noark.data.kit.TypeKit;

public class Jdbcs {
	private static final Logger logger = LogManager.getLogger(Jdbcs.class);

	/**
	 * 针对一个数据源，返回此数据存储策略类型.
	 * 
	 * @param ds 数据源
	 * @return 数据存储策略类型.
	 */
	public static AccessType judgeAccessType(DataSource ds) {
		try (Connection conn = ds.getConnection()) {
			DatabaseMetaData meta = conn.getMetaData();
			String pnm = meta.getDatabaseProductName();
			String ver = meta.getDatabaseProductVersion();
			logger.info("数据库产品名称：{}，版本：{}", pnm, ver);
			// 目前不会实现其他SQL类型的数据方案.就输出个日志直接返回Mysql吧，是不是太装B了呢~~~~
			return AccessType.Mysql;
		} catch (Exception e) {
			throw new DataAccessException(e);
		}
	}

	/**
	 * 根据字段现有的信息，尽可能猜测一下字段的数据库类型
	 * 
	 * @param fm 映射字段
	 */
	public static void guessEntityFieldColumnType(FieldMapping fm) {
		Type type = fm.getField().getGenericType();
		// 明确标识为时间类型的属性
		if (fm.getTemporal() != null) {
			if (type == Date.class || type == Long.class || type == long.class) {
				fm.setAdaptor(ValueAdaptor.AsDate);
			} else {
				throw new DataException("Temporal 注解只能标识在Date或Long类型的属性.");
			}
		}
		// 明确标识为JSON类型的属性
		else if (fm.getJson() != null) {
			fm.setAdaptor(ValueAdaptor.AsJson);
			fm.setWidth(fm.getColumn() == null ? 1024 : fm.getColumn().length());
			return;
		}
		// 明确标识为Blob类型属性
		else if (fm.getBlob() != null) {
			fm.setAdaptor(ValueAdaptor.AsBlob);
			return;
		}
		// 整型
		else if (TypeKit.isInt(type)) {
			fm.setWidth(8);
			fm.setAdaptor(ValueAdaptor.AsInteger);
		}
		// 字符串
		else if (TypeKit.isString(type)) {
			fm.setAdaptor(ValueAdaptor.AsString);
			fm.setWidth(fm.getColumn() == null ? 255 : fm.getColumn().length());
		}
		// 长整型
		else if (TypeKit.isLong(type)) {
			fm.setWidth(16);
			fm.setAdaptor(ValueAdaptor.AsLong);
		}
		// 时间
		else if (Date.class == type) {
			fm.setAdaptor(ValueAdaptor.AsDate);
		}
		// 布尔
		else if (boolean.class == type || Boolean.class == type) {
			fm.setAdaptor(ValueAdaptor.AsBoolean);
		}
		// Float
		else if (float.class == type || Float.class == type) {
			fm.setAdaptor(ValueAdaptor.AsFloat);
		}
		// Double
		else if (double.class == type || Double.class == type) {
			fm.setAdaptor(ValueAdaptor.AsDouble);
		}
		// AtomicInteger
		else if (TypeKit.isAtomicInteger(type)) {
			fm.setWidth(8);
			fm.setAdaptor(ValueAdaptor.AsAtomicInteger);
		}
		// 其他就是Json类型的.
		else {
			fm.setAdaptor(ValueAdaptor.AsJson);
			fm.setWidth(fm.getColumn() == null ? 1024 : fm.getColumn().length());
		}
	}
}
