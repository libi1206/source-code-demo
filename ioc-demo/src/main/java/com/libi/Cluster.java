package com.libi;

import com.libi.bean.UserService;
import com.libi.context.ExtAnnotationApplicationContext;
import com.libi.context.ExtClassPathXmlApplicationContext;
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
