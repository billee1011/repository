package com.lingyu.noark.data.accessor.mysql;

public class SqlRunner {
	// public void run(DataSource dataSource, ConnectionCallback callback) {
	// try (Connection conn = dataSource.getConnection()) {
	// boolean old = conn.getAutoCommit();
	// conn.setAutoCommit(false);
	// // 开始循环运行
	// callback.invoke(conn);
	// // 完成提交
	// if (!conn.getAutoCommit()) {
	// conn.commit();
	// conn.setAutoCommit(old);
	// }
	// } catch (SQLException e) {
	// // 要回滚吗？ // conn.rollback();
	// throw new DataAccessException(e);
	// }
	// }
}
