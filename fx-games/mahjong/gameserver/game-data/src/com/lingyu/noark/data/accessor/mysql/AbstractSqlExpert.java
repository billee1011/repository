package com.lingyu.noark.data.accessor.mysql;

import com.lingyu.noark.data.FieldMapping;
import com.lingyu.noark.data.exception.UnrealizedException;

public abstract class AbstractSqlExpert implements SqlExpert {

	/**
	 * 将来有其他SQL时，如果不合适，就把此层改进为通用型，具体实现放入底层重写.
	 * <p>
	 * MySql参考：
	 * http://dev.mysql.com/doc/refman/5.0/es/connector-j-reference-type-
	 * conversions.html
	 * 
	 * @param fm 属性描述对象.
	 * @return 返回当前属性对应SQL中的类型.
	 */
	protected String evalFieldType(FieldMapping fm) {
		switch (fm.getAdaptor()) {
		case AsBoolean:// Boolean直接写死啦，不可能为其他值的.
			return "BIT(1)";

		case AsString:// 字符串类型的，过长需要换类型
		case AsJson:
			if (fm.getWidth() >= 65535) {
				return "TEXT";
			}
			return "VARCHAR(" + fm.getWidth() + ")";

		case AsDate:// 日期类型的，三种，其他用不着就不实现啦.
			if (fm.getTemporal() != null) {
				switch (fm.getTemporal().value()) {
				case TIMESTAMP:// 第一个判定这个类型是为了那些一点点的性能.
					return "TIMESTAMP";
				case DATE:
					return "DATE";
				case TIME:
					return "TIME";
				default:
					return "TIMESTAMP";
				}
			}
			return "TIMESTAMP";

		case AsInteger:// 数字类型的就写成通用的，Mysql的由子类重写
		case AsLong:
		case AsAtomicInteger:
			// 用户自定义了宽度
			if (fm.getWidth() > 0)
				return "INT(" + fm.getWidth() + ")";
			// 用数据库的默认宽度
			return "INT";

		case AsDouble:
		case AsFloat:
			// 用户自定义了精度
			if (fm.getWidth() > 0 && fm.getPrecision() > 0) {
				return "NUMERIC(" + fm.getWidth() + "," + fm.getPrecision() + ")";
			}
			// 用默认精度
			if (fm.isDouble())
				return "NUMERIC(15,10)";
			return "FLOAT";
		case AsBlob:
			return "BLOB";
		default:
			throw new UnrealizedException("未实现的Java属性转Mysql类型：" + fm.getAdaptor());
		}
	}
}
