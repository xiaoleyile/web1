package com.bdqn.service.impl;

import com.bdqn.dao.IBaseDao;
import com.bdqn.dao.impl.BaseDaoImpl;
import com.bdqn.service.IUserSerivce;

public class UserServiceImpl implements IUserSerivce{

	@Override
	public int register(String username, String password, String gender) {
		String sql = "";
		IBaseDao baseDao = new BaseDaoImpl();
		int sex = 0;
		if(username == null || password == null) {
			return -1;
		}
		if(gender == null) {
			sql =  "insert into users (username,password) values(?,?)";
			return baseDao.update(sql, username,password);
		} else {
			if(gender.equals("男")) {
				sex = 1;
			}
			sql =  "insert into users (username,password,gender) values(?,?,?)";
			return baseDao.update(sql, username,password,sex);
		}
	}

}
