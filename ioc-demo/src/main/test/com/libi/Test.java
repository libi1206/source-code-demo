package com.libi;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.AbstractApplicationContext;

/**
 * @author :Libi
 * @version :1.0
 * @date :2020-05-22 15:43
 */
@Configuration
public class Test {
    public static void main(String[] args) {
        AnnotationConfigApplicationContext ac =
                new AnnotationConfigApplicationContext(Test.class);

        Test bean = ac.getBean("test1", Test.class);
        System.out.println(bean.hashCode());
    }

    @Bean(name = "test1")
    Test getTest() {
        System.out.println(123);
        return new Test();
    }
}
