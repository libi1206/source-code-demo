# 手写SpringMVC

## SpringMVC原理
* SpringMVC基于Servlet实现的（单例的，Servlet会产生线程安全问题）
![SpringMVC的请求流程](https://upload-images.jianshu.io/upload_images/16643404-321fa20f9ea1388c.jpg?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

## 实现步骤
* 创建DispatcherServlet拦截所有的请求
* 初始化，重写Servlet的init方法
    * 扫包，获取需要注入到SpringMVC容器中的类（如`@Controller`注解`）
    * 把URL和方法经行关联
* 处理请求  

## 

