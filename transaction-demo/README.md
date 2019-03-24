# 手写Spring事务
Spring事务分为声明式事务（注解或包扫描）和编程式（在代码里提交或回滚）事务，声明式事务就是在编程式事务的基础上加上AOP计数进行包装<br>
这个工程为了实验事务的回滚，使用用了数据库，使用了jdbc模板连接数据库 ，数据库连接池配置再RootConfig里<br>
我导入的Maven依赖如下
```$xslt
    <dependencies>
        <!-- 引入Spring-AOP等相关Jar -->
        <dependency>
            <groupId>org.springframework</groupId>
            <artifactId>spring-test</artifactId>
            <version>4.3.20.RELEASE</version>
        </dependency>
        <dependency>
            <groupId>org.springframework</groupId>
            <artifactId>spring-core</artifactId>
            <version>4.3.20.RELEASE</version>
        </dependency>
        <dependency>
            <groupId>org.springframework</groupId>
            <artifactId>spring-context</artifactId>
            <version>4.3.20.RELEASE</version>
        </dependency>
        <dependency>
            <groupId>org.springframework</groupId>
            <artifactId>spring-aop</artifactId>
            <version>4.3.20.RELEASE</version>
        </dependency>
        <dependency>
            <groupId>org.springframework</groupId>
            <artifactId>spring-orm</artifactId>
            <version>4.3.20.RELEASE</version>
        </dependency>
        <dependency>
            <groupId>org.aspectj</groupId>
            <artifactId>aspectjrt</artifactId>
            <version>1.6.1</version>
        </dependency>
        <dependency>
            <groupId>aspectj</groupId>
            <artifactId>aspectjweaver</artifactId>
            <version>1.5.3</version>
        </dependency>
        <dependency>
            <groupId>cglib</groupId>
            <artifactId>cglib</artifactId>
            <version>2.1_2</version>
        </dependency>
        <!--mysql连接驱动-->
        <dependency>
            <groupId>mysql</groupId>
            <artifactId>mysql-connector-java</artifactId>
            <version>8.0.13</version>
        </dependency>
        <!--连接池-->
        <dependency>
            <groupId>com.alibaba</groupId>
            <artifactId>druid</artifactId>
            <version>1.1.10</version>
        </dependency>
        <!--测试-->
        <dependency>
            <groupId>junit</groupId>
            <artifactId>junit</artifactId>
            <version>4.12</version>
        </dependency>
    </dependencies>

```
配置类如下，用于代替有些过时的XML配置Spring<br>
```$xslt
@Configuration
@ComponentScan(basePackages = {"com.libi"})
@EnableAspectJAutoProxy
public class RootConfig {
    @Bean
    public DataSource dataSource() {
        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setUrl("jdbc:mysql://localhost:3306/sms?userSSL=true&useUnicode=true&characterEncoding=UTF8&serverTimezone=GMT");
        dataSource.setUsername("root");
        dataSource.setPassword("root");
        dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
        return dataSource;
    }

    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate();
        jdbcTemplate.setDataSource(dataSource);
        return jdbcTemplate;
    }

    @Bean
    public DataSourceTransactionManager dataSourceTransactionManager(DataSource dataSource) {
        DataSourceTransactionManager dataSourceTransactionManager = new DataSourceTransactionManager();
        dataSourceTransactionManager.setDataSource(dataSource);
        return dataSourceTransactionManager;
    }
}
```
需要加入事务的方法如下userDao是会操作数据的，在中间的间隔会抛出异常
```
 @Service
 public class UserServiceImpl implements UserService {
     @Autowired
     private UserDao userDao;
     public void add() {
         userDao.add("test001","1233321");
         System.out.println("中间的间隔，且出现异常");
         int i = 1 / 0;
         userDao.add("test002","135365987");
     }
 }
```
这时只会插入test001的语句，test002不会插入成功。<br>
### 编程式事务

这时我们封装一个事务工具
```
@Component
@Scope("prototype")
public class TransactionUtils {
    @Autowired
    private DataSourceTransactionManager dataSourceTransactionManager;

    private TransactionStatus status;
    
    /** 开启事务*/
    public TransactionStatus begin() {
        //使用默认的传播级别
        TransactionStatus transaction = dataSourceTransactionManager.getTransaction(new DefaultTransactionAttribute());
        return transaction;
    }

    /** 提交事务 需要传入这个事务状态*/
    public void commit() {
        dataSourceTransactionManager.commit(status);
    }

    /**回滚事务 需要传入这个事务状态*/
    public void rollBack() {
        //获取当前事务，如果有，就回滚
        if (status != null) {
            dataSourceTransactionManager.rollback(status);
        }
    }
}
```
再这样使用,修改add方法
```
    public void add() {
        TransactionStatus begin = null;
        try {
            begin = transactionUtils.begin();
            userDao.add("test001", "1233321");
            System.out.println("中间的间隔，且出现异常");
            int i = 1 / 0;
            userDao.add("test002", "135365987");
            transactionUtils.commit();
        } catch (Exception e) {
            e.printStackTrace();
            transactionUtils.rollBack();
        }
    }
```

---

### 声明式事务
我们使用AOP编程把刚刚的事务工具封装一下
```$xslt
@Component
@Aspect
public class AopTransaction {
    @Autowired
    private TransactionUtils transactionUtils;

    @Around("execution(* com.libi.service.UserService.add(..))")
    public void around(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        System.out.println("开启事务");
        proceedingJoinPoint.proceed();
        System.out.println("提交事务");
        transactionUtils.commit();
    }

    @AfterThrowing("execution(* com.libi.service.UserService.add(..))")
    public void afterThrowing() {
        System.out.println("回滚事务");
        //获取当前事务，直接回滚
        TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
    }
    
}
```
然后清空原来的方法里所有的try代码，让他回到最初的状态（**不能捕获异常，否者出现异常后不能被异常通知捕获到，导致事务不生效**）

### 注解式事务

在Spring里已经帮我们实现类注解事务，需要在配置类里添加下面的注解来开启注解事务的支持
```$xslt
@EnableTransactionManagement
```
然后**注释掉我们上次的AOP注解**，使用`@Transactional(rollbackFor = Exception.class)`的注解开启这个方法的事务，`rollbackFor`标识需要回滚的异常类，整个方法如下
```$xslt
    @Transactional(rollbackFor = Exception.class)
    public void add() {
        userDao.add("test001", "1233321");
        System.out.println("中间的间隔，且出现异常");
        int i = 1 / 0;
        userDao.add("test002", "135365987");
    }
```
这样也可以实现这个方法的事务。**当然，这个方法里也不能捕获异常，这样仍然会导致无法触发异常通知而导致事务无效**<br>
我们就以这种效果作为模板手写事务的框架

---
#### 具体步骤
* 定义注解
```$xslt
/**
 * @author libi
 * 自己实现的事务注解
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ExtTransaction {
    
}
```
* 封装手动事务（使用原来的TransactionUtils类）
* 使用AOP扫描规定包下的注解
    * 在AOP上封装找到注解并且加上注解的操作
```$xslt
@Component
@Aspect
public class AopAnnotationTransaction {
    @Autowired
    private TransactionUtils transactionUtils;
    /**这边规定扫描service下的所有方法*/
    @Around("execution(* com.libi.service.*.*(..))")
        //获取方法上的注解,这里把获取注解的方法单独提出来了
        ExtTransaction extTransaction = getExtTransaction(proceedingJoinPoint);

        TransactionStatus status = null;
        if (extTransaction != null) {
            //若果有事务，开启事务
            System.out.println("开启事务");
            status = transactionUtils.begin();
        }
        //调用代理目标方法
        proceedingJoinPoint.proceed();
        if (status != null) {
            //提交事务
            System.out.println("提交事务");
            transactionUtils.commit();
        }
    }

    /**事务的异常通知*/
    @AfterThrowing("execution(* com.libi.service.*.*.*(..))")
    public void afterThrowing() {
        System.out.println("回滚事务");
       transactionUtils.rollBack();
    }

    /**获取方法上的注解*/
    private ExtTransaction getExtTransaction(ProceedingJoinPoint proceedingJoinPoint) throws NoSuchMethodException {
        //获取代理对象的方法
        String methodName = proceedingJoinPoint.getSignature().getName();
        Class<?> targetClass = proceedingJoinPoint.getTarget().getClass();
        Class[] parameterTypes = ((MethodSignature) (proceedingJoinPoint.getSignature())).getParameterTypes();
        Method targetMethod = targetClass.getMethod(methodName, parameterTypes);
        //获取方法上的注解
        return targetMethod.getAnnotation(ExtTransaction.class);
    }
}
```
**还要注意的是，TransactionUtils类仍然需要时多例的，不然会出现线程安全问题**

### 事务传播行为
* **什么是传播行为（Propagation）** ：事务的传播行为产生在调用事务中，也就是说当小个事务嵌套在大事务里时，会发生怎样的行为
* **传播行为的种类**
    * PROPAGATION_REQUIRED—如果当前有事务，就用当前事务，如果当前没有事务，就新建一个事务。这是最常见的选择。（**如果大的方法有事务，那么需要事务的小方法就加入到这个事务里去，如果大方法没有事务，就创建事务**）
    * PROPAGATION_SUPPORTS--支持当前事务，如果当前没有事务，就以非事务方式执行。//（**如果外层方法没有事务，就会以非事务进行执行。这样相当于默认没有事务**）
    * PROPAGATION_MANDATORY--支持当前事务，如果当前没有事务，就抛出异常。 
    * PROPAGATION_REQUIRES_NEW--新建事务，如果当前存在事务，把当前事务挂起（**互不影响，运行到小事务时暂停大事务**）。 
    * PROPAGATION_NOT_SUPPORTED--以非事务方式执行操作，如果当前存在事务，就把当前事务挂起。
    * --- 如果当前有事务，就是以非事务进行执行
    * PROPAGATION_NEVER--以非事务方式执行，如果当前存在事务，则抛出异常。
