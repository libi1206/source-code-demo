package com.libi.dao.impl;

import com.libi.dao.UserDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * @author libi
 */
@Component
public class UserDaoImpl implements UserDao {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    public void add(String userName, String password) {
        String sql = "INSERT INTO user (username,password) VALUES(?,?);";
        int result = jdbcTemplate.update(sql,userName,password);
        System.out.println("插入结果："+result);
    }
}
