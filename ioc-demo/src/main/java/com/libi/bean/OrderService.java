package com.libi.bean;

import com.libi.annotation.ExtService;

import javax.annotation.Resource;

/**
 * @author libi
 */
@ExtService
public class OrderService {
    public void add() {
        System.out.println("order add...");
    }
}
