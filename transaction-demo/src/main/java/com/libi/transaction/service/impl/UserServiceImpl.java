package com.libi.transaction.service.impl;

import com.libi.transaction.annotation.ExtTransaction;
import com.libi.transaction.dao.UserDao;
import com.libi.transaction.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author libi
 */
@Service
public class UserServiceImpl implements UserService {
    @Autowired
    private UserDao userDao;

    @Override
//    @Transactional(rollbackFor = Exception.class)
    @ExtTransaction
    public void add() {
        userDao.add("test001", "1233321");
        System.out.println("中间的间隔，且出现异常");
        int i = 1 / 0;
        userDao.add("test002", "135365987");
    }
}
