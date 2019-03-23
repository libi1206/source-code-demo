package com.libi.service.impl;

import com.libi.dao.UserDao;
import com.libi.service.UserService;
import com.libi.transaction.TransactionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author libi
 */
@Service
public class UserServiceImpl implements UserService {
    @Autowired
    private UserDao userDao;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void add() {
        userDao.add("test001", "1233321");
        System.out.println("中间的间隔，且出现异常");
        int i = 1 / 0;
        userDao.add("test002", "135365987");
    }
}
