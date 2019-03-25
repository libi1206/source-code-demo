package com.libi.bean;

import com.libi.annotation.ExtResource;
import com.libi.annotation.ExtService;

import javax.annotation.Resource;

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
