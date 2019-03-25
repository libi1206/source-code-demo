#Java注解
注解式Java5后才产生的技术，为框架简化代码而存在的<br>
### 注解的分类
* 元注解（jdk自带注解）：如`@Override`、`@SuppessWarnings`等
* 自定义注解<br>
创建注解只需要通过`@interface`就可以使用了
```$xslt
public @interface AddAnnotation {
    
}
```
### 定义注解
* 使用`@Target`就可以指定你的注解只能放在哪里
    > 比如`@Target(ElementType.METHOD)`就规定这个注解只能放在方法上
* 使用`@Retention`用于描述注解的生命周期，
    > 比如`@Retention(RetentionPolicy.RUNTIME)`表示运行时有效
* 还可以直接在注解里定义成员变量，用default来定义默认值
<br><br>我写出的示例如下
```$xslt
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AddAnnotation {
    int userId() default 0;

    String userName() default "默认名字";

    String[] arrays();
}
```
调用时如下
```$xslt
public class User {
    @AddAnnotation(userId = 3,arrays = {"123","321"})
    public void add() {
        
    }
}
```

### 使用反射机制获取注解的值
直接上代码
```$xslt
    public static void main(String[] args) throws ClassNotFoundException {
        Class<?> targetClass = Class.forName("com.libi.annodemo.entity.User");
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
```