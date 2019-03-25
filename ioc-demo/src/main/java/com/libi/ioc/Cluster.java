package com.libi.ioc;

import com.libi.ioc.bean.UserService;
import com.libi.ioc.context.ExtAnnotationApplicationContext;
import org.dom4j.DocumentException;

/**
 * @author libi
 */
public class Cluster {
    public static void main(String[] args) throws ClassNotFoundException, InstantiationException, DocumentException, IllegalAccessException {
        ExtAnnotationApplicationContext context = new ExtAnnotationApplicationContext("com.libi");
        UserService user = (UserService) context.getBean("userService");
        user.add();
    }
}
