# 手写MyBatis
如题，这次我又来作死试试编写类似Mybatis的持久层框架了

## MyBatis的难点
* 如何在没有实例的情况下创建Mapping接口的实现类并且调用接口中的方法
    * 使用字节技术创建子类
    * 使用匿名内部类
    * 使用动态代理创建对象(我们使用这个)
---
创建一个接口UserMapper,再创建一个实体类User<br>
使用JDK的动态代理，创建一个代理处理器
```java
public class InvocationHandlerMybatis implements InvocationHandler {
    /**
     *
     * @param proxy     代理对象
     * @param method    拦截的方法
     * @param args      方法上的参数
     * @return
     * @throws Throwable
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        System.out.println("开始代理");
        return 1;
    }
}
```
包装上面的代理
```java
public class SqlSession {
    /**加载Mapper接口*/
    public static <T> T getMapper(Class clazz) {
        return (T) Proxy.newProxyInstance(clazz.getClassLoader(), new Class[]{clazz}, new InvocationHandlerMybatis());
    }
}
```
使用测试类测试一下
```
    public static void main(String[] args) {
        UserMapper userMapper = SqlSession.getMapper(UserMapper.class);
        int i = userMapper.insertUser("", "");
        System.out.println(i);
    }
```
测试结果如下
![动态代理的测试结果](https://upload-images.jianshu.io/upload_images/16643404-2884417bf2195b4c.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)
这样就可以实现拿到接口的方法参数并且自行控制对象的返回值
## 一、`@Insert`的实现步骤
* 判断方法上是否存在`@Insert`注解，存在的话，获取上面的SQL语句
* 获取方法的参数，和SQL参数经行匹配，并且替换SQL的参数（变成问号）
* 调用JDBC代码执行语句并获取返回值

## 具体实现如下
```java
/**
 * @author libi
 * 用于动态代理，获取方法的参数并且给返回值
 */
public class InvocationHandlerMybatis implements InvocationHandler {
    /**
     * @param proxy     代理对象
     * @param method    拦截的方法
     * @param args      方法上的参数
     * @return          方法的返回值
     * @throws Throwable
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        System.out.println("开始代理");
        //判断方法上是否存在Insert注解
        ExtInsert extInsert = method.getDeclaredAnnotation(ExtInsert.class);
        if (extInsert != null) {
            //执行插入的操作，返回影响行数
            return doInsert(method, args, extInsert);
        }
        return null;
    }

    /**
     * 执行插入的操作
     * @param method
     * @param args
     * @param extInsert
     * @return
     */
    private int doInsert(Method method, Object[] args, ExtInsert extInsert) {
        //获取Sql语句
        String sql = extInsert.value();
        System.out.println("insert sql:" + sql);
        //获取方法参数和Sql语句进行匹配
        //定义一个Map，Key是参数名，Value是参数值
        ConcurrentHashMap<String, Object> map = new ConcurrentHashMap<>();
        //获取方法上的参数
        Parameter[] parameters = method.getParameters();
        for (int i = 0; i < parameters.length; i++) {
            //获取参数名称和参数的值
            ExtParam param = parameters[i].getDeclaredAnnotation(ExtParam.class);
            if (param != null) {
                String name = param.value();
                Object value = args[i];
                System.out.println("paramName:"+name+",paramValue:"+value);
                map.put(name, value);
            }
        }
        //怕打乱顺序而把sql语句的参数放在一个有序的数组里
        List<Object> sqlParam = new ArrayList<>();
        String[] sqlInsertParameter = SQLUtils.sqlInsertParameter(sql);
        for (String paramName : sqlInsertParameter) {
            Object paramValue = map.get(paramName);
            sqlParam.add(paramValue);
        }
        System.out.println();
        //把参数替换成？
        sql = SQLUtils.parameQuestion(sql, sqlInsertParameter);
        System.out.println("new sql:"+sql);
        //执行JDBC
        return JDBCUtils.insert(sql, false, sqlParam);
    }
}
```
## 测试这个方法
我们定义一个Mapper
```java
public interface UserMapper {
    @ExtInsert("insert into user(username,password) values (#{userName},#{password})")
    int insertUser(@ExtParam("userName") String userName, @ExtParam("password") String password);
}
```
然后再主函数里使用代理调用这个方法
```java
public class Cluster {
    public static void main(String[] args) {
        UserMapper userMapper = SqlSession.getMapper(UserMapper.class);
        int i = userMapper.insertUser("name", "123");
        System.out.println(i);
    }
}
```
然后运行结果如下
![插入的执行结果](https://upload-images.jianshu.io/upload_images/16643404-f98cd9c6330fbc21.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

## 二、`@Select`的实现思路
* 找到方法里带有`@Select`注解的方法，拿到Spl语句
* 获取方法上的参数，绑定，然后把参数替换成？
* 调用JDBC调用底层
* **使用反射机制实例化实体类对象（获取方法返回的类型，使用反射实例化对象）**

## 核心代码如下
和上面不同的是，我重构了InvocationHandleMybatis类的代码，复用了一些代码
```
/**
     * 执行查询的操作
     * @param method
     * @param args
     * @param extSelect
     * @return 查询结果，可能是实体类对象，List或者基础类型
     */
    private Object doSelect(Method method, Object[] args, ExtSelect extSelect) throws SQLException, IllegalAccessException, InstantiationException, NoSuchFieldException {
        //获取Sql语句
        String sql = extSelect.value();
        System.out.println("select sql:" + sql);
        //获取方法参数和Sql语句进行匹配
        ConcurrentHashMap<String, Object> paramMap = getParamMap(method, args);

        //怕Sql参数顺序和@Param参数顺序不一致而把sql语句的参数放在一个有序的数组里
        List<Object> sqlParamValue = new ArrayList<>();
        List<String> sqlSelectParameter = SQLUtils.sqlSelectParameter(sql);
        for (String paramName : sqlSelectParameter) {
            Object paramValue = paramMap.get(paramName);
            sqlParamValue.add(paramValue);
        }
        //把参数替换成？
        sql = SQLUtils.parameQuestion(sql, sqlSelectParameter);
        System.out.println("new sql:"+sql);
        //执行JDBC
        ResultSet resultSet = JDBCUtils.query(sql, sqlParamValue);
        //判断是否有结果集
        if (!resultSet.next()) {
            return null;
        }
        resultSet.previous();
        //使用反射获取方法类型
        Class<?> returnType = method.getReturnType();
        //使用反射机制实例化对象
        Object result = returnType.newInstance();
        //遍历这个结果集
        while (resultSet.next()) {
            for (String paramName : sqlSelectParameter) {
                //获取参数的值
                Object resultValue = resultSet.getObject(paramName);
                //使用反射机制赋值
                Field field = returnType.getDeclaredField(paramName);
                field.setAccessible(true);
                field.set(result, resultValue);
            }
        }
        return result;
    }
```
在使用上面的代码时，我会检测方法上是否有`@Select`注解，有的话说明这个方法是用于查询语句的，我们就把这个注解传进来<br>
我们改写UserMapper类，增加Select方法，如下
```java
public interface UserMapper {
    @ExtInsert("insert into user(username,password) values (#{userName},#{password})")
    int insertUser(@ExtParam("userName") String userName, @ExtParam("password") String password);

    @ExtSelect("select * from user where username=#{userName} and password=#{password}")
    User selectUser(@ExtParam("userName") String userName, @ExtParam("password") String password);
}
```
改写测试用的代码，如下
```java
public class Cluster {
    public static void main(String[] args) {
        UserMapper userMapper = SqlSession.getMapper(UserMapper.class);
        User user = userMapper.selectUser("name", "123");
        System.out.println(user.getUserName());
    }
}
```
执行后我的运行结果如下
![执行查询的结果](https://upload-images.jianshu.io/upload_images/16643404-b23bba0c92ad0ce7.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

## 附：整个核心的代理类如下
```java
/**
 * @author libi
 * 用于动态代理，获取方法的参数并且给返回值
 */
public class InvocationHandlerMybatis implements InvocationHandler {
    /**
     * @param proxy     代理对象
     * @param method    拦截的方法
     * @param args      方法上的参数
     * @return          方法的返回值
     * @throws Throwable
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        System.out.println("开始代理");
        //判断方法上是否存在Insert注解
        ExtInsert extInsert = method.getDeclaredAnnotation(ExtInsert.class);
        if (extInsert != null) {
            //执行插入的操作，返回影响行数
            return doInsert(method, args, extInsert);
        }
        //判断方法上是否有Select注解
        ExtSelect extSelect = method.getDeclaredAnnotation(ExtSelect.class);
        if (extSelect != null) {
            //执行查询的操作，返回实际实体类或者List
            return doSelect(method, args, extSelect);
        }
        return null;
    }

    /**
     * 执行插入的操作
     * @param method
     * @param args
     * @param extInsert
     * @return 影响行数
     */
    private int doInsert(Method method, Object[] args, ExtInsert extInsert) {
        //获取Sql语句
        String sql = extInsert.value();
        System.out.println("insert sql:" + sql);
        //获取方法参数和Sql语句进行匹配
        ConcurrentHashMap<String, Object> paramMap = getParamMap(method, args);

        //怕Sql参数顺序和@Param参数顺序不一致而把sql语句的参数放在一个有序的数组里
        List<Object> sqlParamValue = new ArrayList<>();
        String[] sqlInsertParameter = SQLUtils.sqlInsertParameter(sql);
        for (String paramName : sqlInsertParameter) {
            Object paramValue = paramMap.get(paramName);
            sqlParamValue.add(paramValue);
        }
        //把参数替换成？
        sql = SQLUtils.parameQuestion(sql, sqlInsertParameter);
        System.out.println("new sql:"+sql);
        //执行JDBC
        return JDBCUtils.insert(sql, false, sqlParamValue);
    }

    /**
     * 执行查询的操作
     * @param method
     * @param args
     * @param extSelect
     * @return 查询结果，可能是实体类对象，List或者基础类型
     */
    private Object doSelect(Method method, Object[] args, ExtSelect extSelect) throws SQLException, IllegalAccessException, InstantiationException, NoSuchFieldException {
        //获取Sql语句
        String sql = extSelect.value();
        System.out.println("select sql:" + sql);
        //获取方法参数和Sql语句进行匹配
        ConcurrentHashMap<String, Object> paramMap = getParamMap(method, args);

        //怕Sql参数顺序和@Param参数顺序不一致而把sql语句的参数放在一个有序的数组里
        List<Object> sqlParamValue = new ArrayList<>();
        List<String> sqlSelectParameter = SQLUtils.sqlSelectParameter(sql);
        for (String paramName : sqlSelectParameter) {
            Object paramValue = paramMap.get(paramName);
            sqlParamValue.add(paramValue);
        }
        //把参数替换成？
        sql = SQLUtils.parameQuestion(sql, sqlSelectParameter);
        System.out.println("new sql:"+sql);
        //执行JDBC
        ResultSet resultSet = JDBCUtils.query(sql, sqlParamValue);
        //判断是否有结果集
        if (!resultSet.next()) {
            return null;
        }
        resultSet.previous();
        //使用反射获取方法类型
        Class<?> returnType = method.getReturnType();
        //使用反射机制实例化对象
        Object result = returnType.newInstance();
        //遍历这个结果集
        while (resultSet.next()) {
            for (String paramName : sqlSelectParameter) {
                //获取参数的值
                Object resultValue = resultSet.getObject(paramName);
                //使用反射机制赋值
                Field field = returnType.getDeclaredField(paramName);
                field.setAccessible(true);
                field.set(result, resultValue);
            }
        }
        return result;
    }

    /**
     * 建立方法上的参数和值@Param参数名的映射
     * @param method
     * @param args
     * @return
     */
    private ConcurrentHashMap<String, Object> getParamMap(Method method, Object[] args) {
        //定义一个Map，Key是参数名，Value是参数值
        ConcurrentHashMap<String, Object> map = new ConcurrentHashMap<>();
        //获取方法上的参数
        Parameter[] parameters = method.getParameters();
        for (int i = 0; i < parameters.length; i++) {
            //获取参数名称和参数的值
            ExtParam param = parameters[i].getDeclaredAnnotation(ExtParam.class);
            if (param != null) {
                String name = param.value();
                Object value = args[i];
                System.out.println("paramName:"+name+",paramValue:"+value);
                map.put(name, value);
            }
        }
        return map;
    }
}
```
