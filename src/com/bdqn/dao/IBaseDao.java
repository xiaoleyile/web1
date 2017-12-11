package com.bdqn.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public interface IBaseDao<T> {

	int update(String sql, Object... objects);

	List<Map<String, Object>> getOriginalName(String sql, Object... objects);

	List<Map<String, Object>> getAliasName(String sql, Object... objects);

	void close(Connection conn) throws SQLException;

	void close(PreparedStatement ps) throws SQLException;

	void close(ResultSet rs) throws SQLException;

	void close(Connection conn, PreparedStatement ps, ResultSet rs) throws SQLException;

	void close(Connection conn, PreparedStatement ps) throws SQLException;

	<T>T getUnique(Class<T> c,String sql,Object id);
}
