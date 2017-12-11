package com.bdqn.dao.impl;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import com.bdqn.dao.IBaseDao;

public class BaseDaoImpl<T> implements IBaseDao<T> {

	// 实际开发的时候，这些相关的信息不适合硬编码，我们适合放在一个配置文件里面
	private static String driver;
	private static String url;
	private static String username;
	private static String password;
	private static Connection conn;

	static {
		init();
	}

	public Connection getConnection() {
		return conn;
	}

	private static void init() {
		// 加载这个配置 利用输入流 记得加上这个斜杠，不能去写我D盘jt27,,, 你这个路径是跟随这个工程的路径跑的，
		// 相对这个工程的路径，而不能写一个具体的file路径 可移植性差。
		InputStream in = BaseDaoImpl.class.getResourceAsStream("/jdbc.properties");
		Properties properties = new Properties();
		try {
			// 加载这个配置文件进来
			properties.load(in);
			// 使用properties对象取出对应的值
			driver = properties.getProperty("driver");
			url = properties.getProperty("url");
			username = properties.getProperty("username");
			password = properties.getProperty("password");
			// 注册
			Class.forName(driver);
			conn = DriverManager.getConnection(url, username, password);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	// Object ... objects 属于不定长的参数列表。
	public int update(String sql, Object... objects) {
		PreparedStatement ps = null;
		try {
			ps = conn.prepareStatement(sql);
			// 绑定参数
			for (int i = 0; i < objects.length; i++) {
				ps.setObject(i + 1, objects[i]);
			}
			return ps.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
			return 0;
		} finally {
			try {
				close(conn, ps);
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

	// 第一次封装使用原名封装
	public List<Map<String, Object>> getOriginalName(String sql, Object... objects) {
		PreparedStatement ps = null;
		ResultSet rs = null;
		ResultSetMetaData rsmd = null;
		List<Map<String, Object>> list = null;
		try {
			ps = conn.prepareStatement(sql);

			for (int i = 0; i < objects.length; i++) {
				ps.setObject(i + 1, objects[i]);
			}
			rs = ps.executeQuery();
			rsmd = rs.getMetaData();
			// 取出总共有多少列
			int columnCount = rsmd.getColumnCount();
			// 取列名
			list = new ArrayList<>();

			while (rs.next()) {
				Map<String, Object> map = new HashMap<>();
				for (int i = 0; i < columnCount; i++) {
					// 先拿列名
					String columnName = rsmd.getColumnName(i + 1);
					map.put(columnName, rs.getObject(i + 1));
				}
				list.add(map);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				close(conn, ps, rs);
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		return list;
	}

	/**
	 * 别名方式取出来
	 * 
	 * @param sql
	 * @return
	 */
	public List<Map<String, Object>> getAliasName(String sql, Object... objects) {

		PreparedStatement ps = null;
		ResultSet rs = null;
		ResultSetMetaData rsmd = null;
		List<Map<String, Object>> list = new ArrayList<>();
		try {
			ps = conn.prepareStatement(sql);
			for (int i = 0; i < objects.length; i++) {
				ps.setObject(i + 1, objects[i]);
			}
			rs = ps.executeQuery();
			rsmd = rs.getMetaData();
			int columnCount = rsmd.getColumnCount();

			while (rs.next()) {
				// 整体添加完再添加到list里面
				Map<String, Object> map = new HashMap<>();
				for (int i = 0; i < columnCount; i++) {
					map.put(rsmd.getColumnLabel(i + 1), rs.getObject(i + 1));

				}
				list.add(map);
			}

		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				close(conn, ps, rs);
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}

		return list;
	}

	public void close(Connection conn) throws SQLException {
		conn.close();
	}

	public void close(PreparedStatement ps) throws SQLException {
		ps.close();
	}

	public void close(ResultSet rs) throws SQLException {
		rs.close();
	}

	public void close(Connection conn, PreparedStatement ps, ResultSet rs) throws SQLException {
		close(rs);
		close(ps);
		close(conn);
	}

	public void close(Connection conn, PreparedStatement ps) throws SQLException {
		close(ps);
		close(conn);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getUnique(Class<T> clazz, String sql, Object id) {
		List<Map<String, Object>> list = getAliasName(sql, id);
		T obj = null;
		if (null != list && list.size() > 0) {
			Map<String, Object> map = list.get(0);
			Iterator<Entry<String, Object>> it = map.entrySet().iterator();
			
			try {
				obj = clazz.newInstance();
			} catch (InstantiationException e1) {
				e1.printStackTrace();
			} catch (IllegalAccessException e1) {
				e1.printStackTrace();
			}
			while (it.hasNext()) {
				Entry<String, Object> entry = it.next();
				String key = entry.getKey();
				Object value = entry.getValue();
				try {
					Method[] methods = clazz.getDeclaredMethods();
					for (int i = 0; i < methods.length; i++) {
						Method m = methods[i];
						if (m.getName().contains("set")) {
							String name = m.getName().substring(3);
							if (name.equalsIgnoreCase(key)) {
								m.setAccessible(true);
								Class<?>[] types = m.getParameterTypes();
								for (int j = 0; j < types.length; j++) {
									if(types[j].getName().equals(value.getClass().getName())) {
										m.invoke(obj, value);
									}
								}
							}
						}
					}
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				} catch (InvocationTargetException e) {
					e.printStackTrace();
				} finally {
					try {
						close(conn);
					} catch (SQLException e) {
						e.printStackTrace();
					}
				}
			}
		}
		return obj;
	}
}
