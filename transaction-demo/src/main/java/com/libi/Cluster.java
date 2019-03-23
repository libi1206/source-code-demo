package com.libi;

import com.libi.config.RootConfig;
import com.libi.service.UserService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author libi
 * 模拟用户调用
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = RootConfig.class)
public class Cluster {
    @Autowired
    private  UserService userService;
    @Test
    public void cluster() {
        userService.add();
    }
}
