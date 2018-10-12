package com.lingyu.noark.data.accessor;

import java.io.UnsupportedEncodingException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.concurrent.atomic.AtomicInteger;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.lingyu.noark.data.FieldMapping;
import com.lingyu.noark.data.annotation.Json.JsonStyle;
import com.lingyu.noark.data.exception.DataAccessException;

/**
 * 属性适配器.
 * <p>
 * String Long long Integer int Date Double Float Json Enum
 * 
 * @author 小流氓<176543888@qq.com>
 */
public enum ValueAdaptor {

	AsString {
		@Override
		public void set(PreparedStatement pstmt, FieldMapping fm, Object entity, int index) throws IllegalArgumentException, IllegalAccessException,
				SQLException {
			pstmt.setString(index, (String) fm.getField().get(entity));
		}

		@Override
		public void get(ResultSet rs, FieldMapping fm, Object entity) throws SQLException, IllegalArgumentException, IllegalAccessException {
			fm.getField().set(entity, rs.getString(fm.getColumnName()));
		}

		@Override
		public String getString(FieldMapping fm, Object entity) throws IllegalArgumentException, IllegalAccessException {
			return (String) fm.getField().get(entity);
		}

		@Override
		public void get(String value, FieldMapping fm, Object entity) throws IllegalArgumentException, IllegalAccessException {
			fm.getField().set(entity, value);
		}
	},

	AsLong {
		@Override
		public void set(PreparedStatement pstmt, FieldMapping fm, Object entity, int index) throws IllegalArgumentException, IllegalAccessException,
				SQLException {
			pstmt.setLong(index, fm.getField().getLong(entity));
		}

		@Override
		public void get(ResultSet rs, FieldMapping fm, Object entity) throws SQLException, IllegalArgumentException, IllegalAccessException {
			fm.getField().setLong(entity, rs.getLong(fm.getColumnName()));
		}

		@Override
		public String getString(FieldMapping fm, Object entity) throws IllegalArgumentException, IllegalAccessException {
			return String.valueOf(fm.getField().getLong(entity));
		}

		@Override
		public void get(String value, FieldMapping fm, Object entity) throws IllegalArgumentException, IllegalAccessException {
			fm.getField().setLong(entity, Long.parseLong(value));
		}
	},

	AsInteger {
		@Override
		public void set(PreparedStatement pstmt, FieldMapping fm, Object entity, int index) throws IllegalArgumentException, IllegalAccessException,
				SQLException {
			pstmt.setInt(index, fm.getField().getInt(entity));
		}

		@Override
		public void get(ResultSet rs, FieldMapping fm, Object entity) throws SQLException, IllegalArgumentException, IllegalAccessException {
			fm.getField().setInt(entity, rs.getInt(fm.getColumnName()));
		}

		@Override
		public String getString(FieldMapping fm, Object entity) throws IllegalArgumentException, IllegalAccessException {
			return String.valueOf(fm.getField().getInt(entity));
		}

		@Override
		public void get(String value, FieldMapping fm, Object entity) throws IllegalArgumentException, IllegalAccessException {
			fm.getField().setInt(entity, Integer.parseInt(value));
		}
	},

	AsAtomicInteger {
		@Override
		public void set(PreparedStatement pstmt, FieldMapping fm, Object entity, int index) throws IllegalArgumentException, IllegalAccessException,
				SQLException {
			pstmt.setInt(index, ((AtomicInteger) fm.getField().get(entity)).intValue());
		}

		@Override
		public void get(ResultSet rs, FieldMapping fm, Object entity) throws SQLException, IllegalArgumentException, IllegalAccessException {
			fm.getField().set(entity, new AtomicInteger(rs.getInt(fm.getColumnName())));
		}

		@Override
		public String getString(FieldMapping fm, Object entity) throws IllegalArgumentException, IllegalAccessException {
			return String.valueOf(((AtomicInteger) fm.getField().get(entity)).intValue());
		}

		@Override
		public void get(String value, FieldMapping fm, Object entity) throws IllegalArgumentException, IllegalAccessException {
			fm.getField().set(entity, new AtomicInteger(Integer.parseInt(value)));
		}
	},

	AsBoolean {
		@Override
		public void set(PreparedStatement pstmt, FieldMapping fm, Object entity, int index) throws IllegalArgumentException, IllegalAccessException,
				SQLException {
			pstmt.setBoolean(index, fm.getField().getBoolean(entity));
		}

		@Override
		public void get(ResultSet rs, FieldMapping fm, Object entity) throws SQLException, IllegalArgumentException, IllegalAccessException {
			fm.getField().setBoolean(entity, rs.getBoolean(fm.getColumnName()));
		}

		@Override
		public String getString(FieldMapping fm, Object entity) throws IllegalArgumentException, IllegalAccessException {
			return String.valueOf(fm.getField().getBoolean(entity));
		}

		@Override
		public void get(String value, FieldMapping fm, Object entity) throws IllegalArgumentException, IllegalAccessException {
			fm.getField().setBoolean(entity, Boolean.parseBoolean(value));
		}
	},

	AsFloat {
		@Override
		public void set(PreparedStatement pstmt, FieldMapping fm, Object entity, int index) throws IllegalArgumentException, IllegalAccessException,
				SQLException {
			pstmt.setFloat(index, fm.getField().getFloat(entity));
		}

		@Override
		public void get(ResultSet rs, FieldMapping fm, Object entity) throws SQLException, IllegalArgumentException, IllegalAccessException {
			fm.getField().setFloat(entity, rs.getFloat(fm.getColumnName()));
		}

		@Override
		public String getString(FieldMapping fm, Object entity) throws IllegalArgumentException, IllegalAccessException {
			return String.valueOf(fm.getField().getFloat(entity));
		}

		@Override
		public void get(String value, FieldMapping fm, Object entity) throws IllegalArgumentException, IllegalAccessException {
			fm.getField().setFloat(entity, Float.parseFloat(value));
		}
	},

	AsDouble {
		@Override
		public void set(PreparedStatement pstmt, FieldMapping fm, Object entity, int index) throws IllegalArgumentException, IllegalAccessException,
				SQLException {
			pstmt.setDouble(index, fm.getField().getDouble(entity));
		}

		@Override
		public void get(ResultSet rs, FieldMapping fm, Object entity) throws SQLException, IllegalArgumentException, IllegalAccessException {
			fm.getField().setDouble(entity, rs.getDouble(fm.getColumnName()));
		}

		@Override
		public String getString(FieldMapping fm, Object entity) throws IllegalArgumentException, IllegalAccessException {
			return String.valueOf(fm.getField().getDouble(entity));
		}

		@Override
		public void get(String value, FieldMapping fm, Object entity) throws IllegalArgumentException, IllegalAccessException {
			fm.getField().setDouble(entity, Double.parseDouble(value));
		}
	},

	AsDate {
		@Override
		public void set(PreparedStatement pstmt, FieldMapping fm, Object entity, int index) throws IllegalArgumentException, IllegalAccessException,
				SQLException {
			Object args = fm.getField().get(entity);
			if (args == null) {
				pstmt.setNull(index, Types.TIMESTAMP);
			} else if (args instanceof java.util.Date) {
				pstmt.setTimestamp(index, new Timestamp(((java.util.Date) args).getTime()));
			} else if (args instanceof Long) {
				pstmt.setTimestamp(index, new Timestamp((long) args));
			} else {
				pstmt.setObject(index, args);
			}
		}

		@Override
		public void get(ResultSet rs, FieldMapping fm, Object entity) throws SQLException, IllegalArgumentException, IllegalAccessException {
			Timestamp ts = rs.getTimestamp(fm.getColumnName());
			if (fm.isLong()) {
				fm.getField().set(entity, null == ts ? 0L : ts.getTime());
			} else {
				fm.getField().set(entity, null == ts ? null : new java.util.Date(ts.getTime()));
			}
		}

		@Override
		public String getString(FieldMapping fm, Object entity) throws IllegalArgumentException, IllegalAccessException {
			Object args = fm.getField().get(entity);
			if (args == null) {
				return "2000-01-01 00:00:00";
			} else if (args instanceof java.util.Date) {
				// FIXME 这个不是线程安全的，建议修改成ThreadLocal<T>
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				return sdf.format((java.util.Date) args);
			} else {
				return "2000-01-01 00:00:00";
			}
		}

		@Override
		public void get(String value, FieldMapping fm, Object entity) throws IllegalArgumentException, IllegalAccessException {
			if (value != null && value.length() > 0) {
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				try {
					fm.getField().set(entity, sdf.parse(value));
				} catch (ParseException e) {
					throw new DataAccessException(e);
				}
			}
		}
	},

	AsJson {
		@Override
		public void set(PreparedStatement pstmt, FieldMapping fm, Object entity, int index) throws IllegalArgumentException, IllegalAccessException,
				SQLException {
			Object args = fm.getField().get(entity);
			if (args == null) {
				pstmt.setString(index, null);
			} else if (fm.getJson().style() == JsonStyle.WriteClassName) {
				pstmt.setString(index, JSON.toJSONString(args, SerializerFeature.WriteClassName, SerializerFeature.WriteMapNullValue));
			} else {
				pstmt.setString(index, JSON.toJSONString(args));
			}
		}

		@Override
		public void get(ResultSet rs, FieldMapping fm, Object entity) throws SQLException, IllegalArgumentException, IllegalAccessException {
			String str = rs.getString(fm.getColumnName());

			fm.getField().set(entity, JSON.parseObject(str, fm.getFieldClass()));
		}

		@Override
		public String getString(FieldMapping fm, Object entity) throws IllegalArgumentException, IllegalAccessException {
			Object args = fm.getField().get(entity);
			return args == null ? "" : JSON.toJSONString(args);
		}

		@Override
		public void get(String value, FieldMapping fm, Object entity) throws IllegalArgumentException, IllegalAccessException {
			fm.getField().set(entity, JSON.parseObject(value, fm.getFieldClass()));
		}
	},
	AsBlob {
		@Override
		public void set(PreparedStatement pstmt, FieldMapping fm, Object entity, int index) throws IllegalArgumentException, IllegalAccessException,
				SQLException {
			Object args = fm.getField().get(entity);
			if (args == null) {
				pstmt.setNull(index, Types.BLOB);
			} else {
				pstmt.setObject(index, args);
			}
		}

		@Override
		public void get(ResultSet rs, FieldMapping fm, Object entity) throws SQLException, IllegalArgumentException, IllegalAccessException {
			Object value = rs.getObject(fm.getColumnName());
			if (value != null) {
				fm.getField().set(entity, value);
			}
		}

		@Override
		public String getString(FieldMapping fm, Object entity) throws IllegalArgumentException, IllegalAccessException {
			Object args = fm.getField().get(entity);
			try {
				return args == null ? "null" : new String((byte[]) args, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
			return "null";
		}

		@Override
		public void get(String value, FieldMapping fm, Object entity) throws IllegalArgumentException, IllegalAccessException {
			fm.getField().set(entity, value.getBytes());
		}
	};

	/**
	 * 从实体对象中取出指定字段并设计到编译的 SQL语句对象中.
	 */
	public abstract void set(PreparedStatement pstmt, FieldMapping fm, Object entity, int index) throws IllegalArgumentException, IllegalAccessException,
			SQLException;

	/**
	 * 从结果集里取出指定字段并设计到实体对象中.
	 */
	public abstract void get(ResultSet rs, FieldMapping fm, Object entity) throws SQLException, IllegalArgumentException, IllegalAccessException;

	public abstract void get(String value, FieldMapping fm, Object entity) throws IllegalArgumentException, IllegalAccessException;

	public abstract String getString(FieldMapping fm, Object entity) throws IllegalArgumentException, IllegalAccessException;
}