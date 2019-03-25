package com.libi.ioc.bean;

import com.libi.ioc.annotation.ExtResource;
import com.libi.ioc.annotation.ExtService;

/**
 * @author libi
 */
@ExtService
public class UserService {
    @ExtResource
    private OrderService orderService;

    public void add() {
        orderService.add();
        System.out.println("User add...");
    }
}
