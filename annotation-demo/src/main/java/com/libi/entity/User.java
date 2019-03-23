package com.libi.entity;

import com.libi.annotation.AddAnnotation;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;

/**
 * @author libi
 */
public class User {
    @AddAnnotation(userId = 3,arrays = {"123","321"})
    public void add() {

    }

    public static void main(String[] args) throws ClassNotFoundException {
        Class<?> targetClass = Class.forName("com.libi.entity.User");
        //获取当前类所有的方法（不包括父类的方法）
        Method[] declaredMethods = targetClass.getDeclaredMethods();
        for (Method declaredMethod : declaredMethods) {
            //拿到这个方法上的这个注解对象
            AddAnnotation addAnnotation = declaredMethod.getDeclaredAnnotation(AddAnnotation.class);
            if (addAnnotation == null) {
                //如果为空表示这个方法没有这个注解
                continue;
            }
            //这里表示拿到了这个注解
            System.out.println("userId:"+ addAnnotation.userId());
            System.out.println("userName:"+ addAnnotation.userName());
            System.out.println("arrays:"+ addAnnotation.arrays()[0]);
        }
    }
}
