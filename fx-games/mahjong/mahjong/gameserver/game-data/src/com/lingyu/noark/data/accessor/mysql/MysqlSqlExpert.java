package com.lingyu.noark.data.accessor.mysql;

import com.lingyu.noark.data.EntityMapping;
import com.lingyu.noark.data.FieldMapping;
import com.lingyu.noark.data.accessor.AccessorEntityMapping;
import com.lingyu.noark.data.accessor.Sort;
import com.lingyu.noark.data.accessor.ValueAdaptor;
import com.lingyu.noark.data.exception.DataException;
import com.lingyu.noark.data.kit.StringKit;

public class MysqlSqlExpert extends AbstractSqlExpert {
	@Override
	public <T> String genCreateTableSql(EntityMapping<T> em) {
		StringBuilder sb = new StringBuilder(512);
		sb.append("CREATE TABLE `" + em.getTableName() + "` (");
		// 创建字段
		for (FieldMapping fm : em.getFieldMapping()) {
			sb.append('\n').append('`').append(fm.getColumnName()).append('`');
			sb.append(' ').append(evalFieldType(fm));
			// 主键的 @Id，应该加入唯一性约束
			if (fm.isPrimaryId()) {
				sb.append(" UNIQUE NOT NULL");
				if (fm.isAutoIncrement()) {
					sb.append(" AUTO_INCREMENT");
				}
			}
			// 普通字段
			else {
				// 下面的关于Timestamp处理，是因为MySql中第一出现Timestamp的话，如果没有设定default，数据库默认会设置为CURRENT_TIMESTAMP
				if (fm.isUnsigned())
					sb.append(" UNSIGNED");

				if (fm.isNotNull()) {
					sb.append(" NOT NULL");
				} else if (fm.getAdaptor() == ValueAdaptor.AsDate) {
					sb.append(" NULL");
				}

				if (fm.hasDefaultValue()) {
					switch (fm.getAdaptor()) {
					case AsBoolean:
					case AsInteger:
					case AsLong:
					case AsDouble:
					case AsFloat:
					case AsAtomicInteger:
						sb.append(" DEFAULT ").append(fm.getDefaultValue()).append("");
						break;
					default:
						if (fm.getWidth() < 65535) {// 超过这个值当Text啦，Text是不可以有默认值的.
							sb.append(" DEFAULT '").append(fm.getDefaultValue()).append("'");
						}
						break;
					}
				}
			}

			if (fm.hasColumnComment()) {
				sb.append(" COMMENT '").append(fm.getColumnComment()).append("'");
			}

			sb.append(',');
		}
		// 创建主键
		FieldMapping pk = em.getPrimaryId();
		if (pk != null) {
			sb.append('\n');
			sb.append("PRIMARY KEY (");
			sb.append('`').append(pk.getColumnName()).append('`').append(',');
			sb.setCharAt(sb.length() - 1, ')');
			sb.append("\n ");
		}

		// 结束表字段设置
		sb.setCharAt(sb.length() - 1, ')');
		// 设置特殊引擎
		sb.append(" ENGINE=").append(em.getTableEngine().name());
		sb.append(" DEFAULT CHARSET=utf8");
		// 表名注释
		if (!StringKit.isEmpty(em.getTableComment())) {
			sb.append(" COMMENT='").append(em.getTableComment()).append("'");
		}
		return sb.append(";").toString();
	}

	@Override
	protected String evalFieldType(FieldMapping fm) {
		switch (fm.getAdaptor()) {

		case AsInteger:// 游戏嘛，数字就是int(11)不要想多啦，简单直接明了
		case AsAtomicInteger:
			return "INT(11)";

		case AsLong:// 龙哥说20就20吧~~~
			return "BIGINT(20)";

		case AsDouble:// 有小数的就直接写上他写的参数
			return "DOUBLE(" + fm.getPrecision() + "," + fm.getScale() + ")";

		case AsFloat:
			return "FLOAT(" + fm.getPrecision() + "," + fm.getScale() + ")";

		default:// 其它的参照默认字段规则 ...
			return super.evalFieldType(fm);
		}
	}

	@Override
	public <T> String genInsertSql(EntityMapping<T> em) {
		// INSERT [LOW_PRIORITY | DELAYED] [IGNORE]
		// [INTO] tbl_name [(col_name,...)]
		// VALUES (expression,...),(...),...
		StringBuilder sb = new StringBuilder(128);
		sb.append("INSERT INTO ").append(em.getTableName()).append(" (");

		int count = 0;
		for (FieldMapping fm : em.getFieldMapping()) {
			sb.append(fm.getColumnName()).append(',');
			count++;
		}
		sb.setCharAt(sb.length() - 1, ')');

		sb.append(" VALUES (");
		for (int i = 0; i < count; i++) {
			sb.append("?,");
		}
		sb.setCharAt(sb.length() - 1, ')');
		return sb.toString();
	}

	@Override
	public <T> String genDeleteSql(AccessorEntityMapping<T> sem) {
		// delete from item where id=?
		StringBuilder sb = new StringBuilder(128);
		sb.append("DELETE FROM ").append(sem.getTableName());
		sb.append(" WHERE ").append(sem.getPrimaryId().getColumnName()).append("=?");
		return sb.toString();
	}

	@Override
	public <T> String genUpdateSql(EntityMapping<T> em) {
		StringBuilder sb = new StringBuilder(128);
		sb.append("UPDATE ").append(em.getTableName()).append(" SET ");
		for (FieldMapping fm : em.getFieldMapping()) {
			if (!fm.isPrimaryId()) {
				sb.append(fm.getColumnName()).append("=?,");
			}
		}
		sb.setCharAt(sb.length() - 1, ' ');

		sb.append("WHERE ").append(em.getPrimaryId().getColumnName()).append("=?");
		return sb.toString();
	}

	@Override
	public <T> String genSeleteByRoleId(EntityMapping<T> em) {
		// Selete id from item where role_id = ?
		StringBuilder sb = new StringBuilder(128);
		sb.append("SELECT ");
		for (FieldMapping fm : em.getFieldMapping()) {
			sb.append(fm.getColumnName()).append(',');
		}
		sb.setCharAt(sb.length() - 1, ' ');
		sb.append("FROM ").append(em.getTableName());
		if (em.getRoleId() != null) {
			sb.append(" WHERE ").append(em.getRoleId().getColumnName()).append("=?");
		}
		return sb.toString();
	}

	@Override
	public <T> String genSeleteSql(AccessorEntityMapping<T> sem) {
		// Selete id from item where role_id = ?
		StringBuilder sb = new StringBuilder(128);
		sb.append("SELECT ");
		for (FieldMapping fm : sem.getFieldMapping()) {
			sb.append(fm.getColumnName()).append(',');
		}
		sb.setCharAt(sb.length() - 1, ' ');
		sb.append("FROM ").append(sem.getTableName());
		sb.append(" WHERE ").append(sem.getPrimaryId().getColumnName()).append("=?");
		return sb.toString();
	}

	@Override
	public <T> String genSeleteAllSql(AccessorEntityMapping<T> sem) {
		StringBuilder sb = new StringBuilder(128);
		sb.append("SELECT ");
		for (FieldMapping fm : sem.getFieldMapping()) {
			sb.append(fm.getColumnName()).append(',');
		}
		sb.setCharAt(sb.length() - 1, ' ');
		sb.append("FROM ").append(sem.getTableName());
		return sb.toString();
	}

	@Override
	public <T> String genSeleteByGroup(AccessorEntityMapping<T> em) {
		// Selete id from item where role_id = ?
		StringBuilder sb = new StringBuilder(128);
		sb.append("SELECT ");
		for (FieldMapping fm : em.getFieldMapping()) {
			sb.append(fm.getColumnName()).append(',');
		}
		sb.setCharAt(sb.length() - 1, ' ');
		sb.append("FROM ").append(em.getTableName());
		if (em.getGroupBy() != null) {
			sb.append(" WHERE ").append(em.getGroupBy().getColumnName()).append("=?");
		}
		return sb.toString();
	}

	private <T> void handleSingleQuotationMarks(StringBuilder sb, FieldMapping fm, T entity) {
		try {
			switch (fm.getAdaptor()) {
			case AsBoolean:
			case AsInteger:
			case AsLong:
			case AsFloat:
			case AsDouble:
			case AsAtomicInteger:
				sb.append(fm.getAdaptor().getString(fm, entity));
				break;
			default:
				sb.append("'").append(fm.getAdaptor().getString(fm, entity)).append("'");
				break;
			}
		} catch (IllegalArgumentException | IllegalAccessException e) {
			throw new DataException("" + e);
		}
	}

	@Override
	public <T> String genNInsertSql(EntityMapping<T> em, T entity) {
		StringBuilder sb = new StringBuilder(256);
		sb.append(em.getTableName(entity)).append("|");

		for (FieldMapping fm : em.getFieldMapping()) {
			try {
				sb.append(fm.getAdaptor().getString(fm, entity)).append("|");
			} catch (IllegalArgumentException | IllegalAccessException e) {
				throw new DataException("" + e);
			}
		}
		sb.deleteCharAt(sb.length() - 1);
		return sb.toString();
	}

	@Override
	public <T> String genInsertSql(EntityMapping<T> em, T entity) {
		StringBuilder sb = new StringBuilder(256);
		sb.append("INSERT DELAYED INTO ").append(em.getTableName(entity)).append(" (");
		for (FieldMapping fm : em.getFieldMapping()) {
			sb.append(fm.getColumnName()).append(',');
		}
		sb.setCharAt(sb.length() - 1, ')');

		sb.append(" VALUES (");
		for (FieldMapping fm : em.getFieldMapping()) {
			this.handleSingleQuotationMarks(sb, fm, entity);
			sb.append(",");
		}
		sb.setCharAt(sb.length() - 1, ')');
		return sb.toString();
	}

	@Override
	public <T> String genUpdateSql(EntityMapping<T> em, T entity) {
		StringBuilder sb = new StringBuilder(256);
		sb.append("UPDATE ").append(em.getTableName(entity)).append(" SET ");
		for (FieldMapping fm : em.getFieldMapping()) {
			if (!fm.isPrimaryId()) {
				sb.append(fm.getColumnName()).append("=");
				this.handleSingleQuotationMarks(sb, fm, entity);
				sb.append(",");
			}
		}
		sb.setCharAt(sb.length() - 1, ' ');

		sb.append("WHERE ").append(em.getPrimaryId().getColumnName()).append("=");
		this.handleSingleQuotationMarks(sb, em.getPrimaryId(), entity);
		return sb.toString();
	}

	@Override
	public <T> String genDeleteSql(EntityMapping<T> em, T entity) {
		StringBuilder sb = new StringBuilder(128);
		sb.append("DELETE FROM ").append(em.getTableName(entity));
		sb.append(" WHERE ").append(em.getPrimaryId().getColumnName()).append("=");
		this.handleSingleQuotationMarks(sb, em.getPrimaryId(), entity);
		return sb.toString();
	}

	@Override
	public <T> String genAddTableColumnSql(EntityMapping<T> em, FieldMapping fm) {
		// alter table `user_movement_log` Add column GatewayId int not null
		// default 0 AFTER `Regionid` (在哪个字段后面添加)
		StringBuilder sb = new StringBuilder(128);
		sb.append("ALTER TABLE `").append(em.getTableName()).append("` ADD COLUMN `").append(fm.getColumnName());
		sb.append("` ").append(evalFieldType(fm));
		if (fm.isNotNull()) {
			sb.append(" NOT NULL");
		} else if (fm.getAdaptor() == ValueAdaptor.AsDate) {
			sb.append(" NULL");
		}

		if (fm.hasDefaultValue()) {
			switch (fm.getAdaptor()) {
			case AsBoolean:
			case AsInteger:
			case AsLong:
			case AsDouble:
			case AsFloat:
			case AsAtomicInteger:
				sb.append(" DEFAULT ").append(fm.getDefaultValue()).append("");
				break;
			default:
				if (fm.getWidth() < 65535) {// 超过这个值当Text啦，Text是不可以有默认值的.
					sb.append(" DEFAULT '").append(fm.getDefaultValue()).append("'");
				}
				break;
			}
		}
		if (fm.hasColumnComment()) {
			sb.append(" COMMENT '").append(fm.getColumnComment()).append("'");
		}
		return sb.toString();
	}

	@Override
	public <T> String genSeleteByRoleIdAndPage(EntityMapping<T> em) {
		StringBuilder sb = new StringBuilder(128);
		sb.append("SELECT ");
		for (FieldMapping fm : em.getFieldMapping()) {
			sb.append(fm.getColumnName()).append(',');
		}
		sb.setCharAt(sb.length() - 1, ' ');
		sb.append("FROM ").append(em.getTableName());
		if (em.getRoleId() != null) {
			sb.append(" WHERE ").append(em.getRoleId().getColumnName()).append("=?");
		}
		sb.append(" limit ?,?");
		return sb.toString();
	}

	@Override
	public <T> String genSeleteByCount(EntityMapping<T> em) {
		StringBuilder sb = new StringBuilder(128);
		sb.append("SELECT count(1) FROM ").append(em.getTableName());
		return sb.toString();
	}

	@Override
	public <T> String genSeleteByRoleIdAndCount(EntityMapping<T> em) {
		StringBuilder sb = new StringBuilder(128);
		sb.append("SELECT count(1) FROM ").append(em.getTableName());
		if (em.getRoleId() != null) {
			sb.append(" WHERE ").append(em.getRoleId().getColumnName()).append("=?");
		}
		return sb.toString();
	}

	@Override
	public <T> String genSeleteByPageSql(EntityMapping<T> em) {
		StringBuilder sb = new StringBuilder(128);
		sb.append("SELECT ");
		for (FieldMapping fm : em.getFieldMapping()) {
			sb.append(fm.getColumnName()).append(',');
		}
		sb.setCharAt(sb.length() - 1, ' ');
		sb.append("FROM ").append(em.getTableName());
		sb.append(" limit ?,?");
		return sb.toString();
	}

	@Override
	public <T> String genSeleteByRoleIdAndSortAndPageSql(EntityMapping<T> em, Sort sort) {
		StringBuilder sb = new StringBuilder(128);
		sb.append("SELECT ");
		for (FieldMapping fm : em.getFieldMapping()) {
			sb.append(fm.getColumnName()).append(',');
		}
		sb.setCharAt(sb.length() - 1, ' ');
		sb.append("FROM ").append(em.getTableName());
		if (em.getRoleId() != null) {
			sb.append(" WHERE ").append(em.getRoleId().getColumnName()).append("=?");
		}
		sb.append(" ORDER BY ");
		for (String field : sort.getFields()) {
			sb.append(field).append(',');
		}
		sb.setCharAt(sb.length() - 1, ' ');
		sb.append(sort.getDirection().toString());
		sb.append(" limit ?,?");
		return sb.toString();
	}

	@Override
	public <T> String genSeleteByAndSortPageSql(EntityMapping<T> em, Sort sort) {
		StringBuilder sb = new StringBuilder(128);
		sb.append("SELECT ");
		for (FieldMapping fm : em.getFieldMapping()) {
			sb.append(fm.getColumnName()).append(',');
		}
		sb.setCharAt(sb.length() - 1, ' ');
		sb.append("FROM ").append(em.getTableName());
		sb.append(" ORDER BY ");
		for (String field : sort.getFields()) {
			sb.append(field).append(',');
		}
		sb.setCharAt(sb.length() - 1, ' ');
		sb.append(sort.getDirection().toString());
		sb.append(" limit ?,?");
		return sb.toString();
	}

	@Override
	public <T> String genUpdateDefaultValueSql(EntityMapping<T> em, FieldMapping fm) {
		StringBuilder sb = new StringBuilder(64);
		sb.append("UPDATE ").append(em.getTableName()).append(" SET ").append(fm.getColumnName()).append("='").append(fm.getDefaultValue()).append("'");
		// sb.append("WHERE ").append(em.getPrimaryId().getColumnName()).append("=?");
		return sb.toString();
	}
}