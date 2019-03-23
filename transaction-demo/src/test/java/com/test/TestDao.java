package com.test;

import com.libi.config.RootConfig;
import com.libi.dao.UserDao;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author libi
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = RootConfig.class)
public class TestDao {
    @Autowired
    private UserDao userDao;
    @Test
    public void TestUserDao() {
        userDao.add("libi5","123123");
    }
}
