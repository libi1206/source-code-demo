package com.libi.ioc.bean;

import com.libi.ioc.annotation.ExtService;

/**
 * @author libi
 */
@ExtService
public class OrderService {
    public void add() {
        System.out.println("order add...");
    }
}
