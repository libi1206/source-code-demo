package com.libi.mvc.controller;

import com.libi.mvc.annotation.ExtController;
import com.libi.mvc.annotation.ExtRequestMapping;

/**
 * @author libi
 */
@ExtController
@ExtRequestMapping("/")
public class IndexController {

    @ExtRequestMapping
    public String getIndex() {
        System.out.println("自定义的MVC框架");
        return "index";
    }
}
