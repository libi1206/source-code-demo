# 手写Spring事务
Spring事务分为声明式事务（注解或包扫描）和编程式（在代码里提交或回滚）事务，声明式事务就是在编程式事务的基础上加上AOP计数进行包装<br>
这个工程为了实验事务的回滚，使用用了数据库，使用了jdbc模板连接数据库 ，数据库连接池配置再RootConfig里<br>
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
    /** 开启事务*/
    public TransactionStatus begin() {
        //使用默认的传播级别
        TransactionStatus transaction = dataSourceTransactionManager.getTransaction(new DefaultTransactionAttribute());
        return transaction;
    }
    
    /** 提交事务 需要传入这个事务状态*/
    public void commit(TransactionStatus transaction) {
        dataSourceTransactionManager.commit(transaction);
    }

    /**回滚事务 需要传入这个事务状态*/
    public void rollBack(TransactionStatus transaction) {
        dataSourceTransactionManager.rollback(transaction);
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
            transactionUtils.commit(begin);
        } catch (Exception e) {
            e.printStackTrace();
            transactionUtils.rollBack(begin);
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
        TransactionStatus status = transactionUtils.begin();
        proceedingJoinPoint.proceed();
        System.out.println("提交事务");
        transactionUtils.commit(status);
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
然后**注释掉aop我们上次的AOP注解**，使用`@Transactional(rollbackFor = Exception.class)`的注解开启这个方法的事务，`rollbackFor`标识需要回滚的异常类，整个方法如下
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
我们就以这种效果作为模板手写事务的框架（注解的知识见底部）

---
