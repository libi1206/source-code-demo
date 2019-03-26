# 手写SpringMVC

## SpringMVC原理
* SpringMVC基于Servlet实现的（单例的，Servlet会产生线程安全问题）
![SpringMVC的请求流程](https://upload-images.jianshu.io/upload_images/16643404-321fa20f9ea1388c.jpg?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

## 实现步骤
* 创建DispatcherServlet拦截所有的请求
* 初始化，重写Servlet的init方法
    * 扫包，获取需要注入到SpringMVC容器中的类（如`@Controller`注解`）
    * 把URL和方法经行关联：利用反射机制找到Controller类上的方法是否存在`@RequestMapping`注解，存在就把方法名称存起来，把URL和方法对应起来
* 处理请求 重写GET和POST方法
    * 获取请求URL，去URLBean里获取实例，再去URLMethod里获取方法实例，使用反射执行方法

## Maven依赖
```xml
<dependencies>
        <!--servlet依赖-->
        <dependency>
            <groupId>javax.servlet</groupId>
            <artifactId>javax.servlet-api</artifactId>
            <version>3.1.0</version>
            <scope>provided</scope>
        </dependency>
        <!--各种工具-->
        <dependency>
            <groupId>commons-lang</groupId>
            <artifactId>commons-lang</artifactId>
            <version>2.5</version>
        </dependency>
    </dependencies>
```
## 定义自己的注解`@ExtController`和`@ExtRequestMapping`，定义一个自己的Controller
注解如下
```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ExtController {
}
```
```java
@Target({ElementType.METHOD,ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ExtRequestMapping {
    String value() default "";
}
```
自己定义的controller如下
```java
@ExtController
@ExtRequestMapping("/")
public class IndexController {

    @ExtRequestMapping
    public String getIndex() {
        System.out.println("自定义的MVC框架");
        return "index";
    }
}
```
## 重点！构建DispatcherServlet
由于是个Web项目，我们在Web.xml里面配置好自己的DispatcherServlet的映射
```xml
<web-app xmlns="http://xmlns.jcp.org/xml/ns/javaee"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://xmlns.jcp.org/xml/ns/javaee http://xmlns.jcp.org/xml/ns/javaee/web-app_4_0.xsd"
         version="4.0">
    <servlet>
        <servlet-name>disaptcher</servlet-name>
        <servlet-class>com.libi.mvc.servlet.ExtDispatcherServlet</servlet-class>
        <load-on-startup>1</load-on-startup>
    </servlet>
    <servlet-mapping>
        <servlet-name>disaptcher</servlet-name>
        <url-pattern>/</url-pattern>
    </servlet-mapping>
</web-app>
```
### 第一步、在init方法里扫包，判断是否有Controller注解，再把URL和映射经行关联
* 在DispatcherServlet里定义三个Bean容器
```
    /**mvc的Bean容器,存入Controller对象*/
    private ConcurrentHashMap<String, Object> controllerBeans = new ConcurrentHashMap<String, Object>();
    /**mvc的请求地址,把Url和Controller建立映射*/
    private ConcurrentHashMap<String, Object> urlBeans = new ConcurrentHashMap<String, Object>();
    /**mvc的请求方法名称，把Url和方法名建立映射*/
    private ConcurrentHashMap<String, Object> urlMethods = new ConcurrentHashMap<String, Object>();
```
* 在重写了Servlet的init方法里扫描指定包下面所有带有`@ExtController`注解的类，实例化后放在mvc的bean容器里
```
    public void init() throws ServletException {
        //扫描Controller包下面的类，并且找到所有带@Controller的注解倒到Bean容器里
        List<Class<?>> classes = ClassUtils.getClasses("com.controller");
        try {
            findClassMvcAnnoation(classes);
        } catch (Exception e) {
            e.printStackTrace();
        } 
    }
    
    /**找到带有Controller注解的类然后初始化放入bean容器里*/
     private void findClassMvcAnnoation(List<Class<?>> classes) throws IllegalAccessException, InstantiationException {
         for (Class<?> classInfo : classes) {
             ExtController extController = classInfo.getAnnotation(ExtController.class);
             if (extController != null) {
                 String beanId = ClassUtils.toLowerCaseFirstOne(classInfo.getSimpleName());
                 mvcBeans.put(beanId, classInfo.newInstance());
             }
         }
     }
```

### 第二步 把method和url进行映射
* 利用反射机制获取有@RequestMapping的类和方法，把URL和方法经行关联，放在Map里
```
    /**将URL映射和方法关联起来*/
    private void handleMapping() {
        //遍历Controller，判断类上是否有RequestMapping注解
        for (Map.Entry<String, Object> entry : controllerBeans.entrySet()) {
            Object controller = entry.getValue();
            //判断类是否加上了@RequestMapping注解
            Class<?> classInfo = controller.getClass();
            ExtRequestMapping classRequestMapping = classInfo.getDeclaredAnnotation(ExtRequestMapping.class);
            String baseUrl = "";
            if (classRequestMapping != null) {
                //获取Url映射的地址
                baseUrl = classRequestMapping.value();
            }
            //判断方法上是否加上了RequestMapping
            Method[] methods = classInfo.getDeclaredMethods();
            for (Method method : methods) {
                //获取方法上的映射
                ExtRequestMapping methodRequestMapping = method.getDeclaredAnnotation(ExtRequestMapping.class);
                if (methodRequestMapping != null) {
                    String methodUrl = baseUrl + methodRequestMapping.value();
                    //装入映射表
                    System.out.println("put url:"+methodUrl);
                    urlBeans.put(methodUrl, controller);
                    urlMethods.put(methodUrl, method);
                }
            }
        }
    }
```
上面这一步在init()方法里，创建完controller实例之后执行

### 第三步 重写doPost方法
* 利用反射机制获取到需要的方法，利用反射机制执行存在Map里的方法
```
    /**处理请求*/
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        //获取请求url地址
        String url = req.getRequestURI();
        System.out.println("url:"+url);
        //从map集合中获取controller
        Object controller = urlBeans.get(url);
        if (controller == null) {
            //到这里说明页面不存在
            resp.getWriter().println("404 not find url");
            return;
        }
        //从map中获取方法,调用并且获取返回结果
        Method method = urlMethods.get(url);
        if (method == null) {
            //到这里说明页面不存在
            resp.getWriter().println("404 not find method");
            return;
        }
        String result = (String) methodInvoke(method, controller);
        resp.getWriter().println(result);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doPost(req,resp);
    }

    private Object methodInvoke(Method method, Object controller) {
        Object result = null;
        try {
            result = method.invoke(controller);
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
        return result;
    }
```
写到这里的效果如下
![访问正常的效果](https://upload-images.jianshu.io/upload_images/16643404-05e012ea0ec84e7e.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

![访问404的效果](https://upload-images.jianshu.io/upload_images/16643404-9c2e7bf6cce435f2.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

### 第四步 配置视图解析器
```gwt j
    /**视图解析器*/
    private void extResourceViewResolver(String pageName, HttpServletRequest request,HttpServletResponse response) throws ServletException, IOException {
        String prefix = "/WEB-INF/view/";
        String suffix = ".jsp";
        request.getRequestDispatcher(prefix+pageName+suffix).forward(request,response);
    }
```
以上代码在doGet里执行
```gwt j
/**处理请求*/
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        //获取请求url地址
        String url = req.getRequestURI();
        System.out.println("url:"+url);
        //从map集合中获取controller
        Object controller = urlBeans.get(url);
        if (controller == null) {
            //到这里说明页面不存在
            resp.getWriter().println("404 not find url");
            return;
        }
        //从map中获取方法,调用并且获取返回结果
        Method method = urlMethods.get(url);
        if (method == null) {
            //到这里说明页面不存在
            resp.getWriter().println("404 not find method");
            return;
        }
        String result = (String) methodInvoke(method, controller);
        //-----------------使用视图转换器渲染页面
        extResourceViewResolver(result,req,resp);
        //------------------
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doPost(req,resp);
    }
```

/WEB-INF/view/index.jsp内容如下
```jsp
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <title>index</title>
</head>
<body>
<h1>自己手写mvc——index.jsp</h1>
</body>
</html>
``` 
执行后效果如下
![使用视图解析器的效果](https://upload-images.jianshu.io/upload_images/16643404-9a5bf92e33a7134e.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

