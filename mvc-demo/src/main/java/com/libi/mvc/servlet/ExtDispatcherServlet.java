package com.libi.mvc.servlet;

import com.libi.mvc.annotation.ExtController;
import com.libi.mvc.annotation.ExtRequestMapping;
import com.libi.mvc.utils.ClassUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author libi
 * 自定义的DispatcherServlet
 */
public class ExtDispatcherServlet extends HttpServlet {
    /**mvc的Bean容器,存入Controller对象*/
    private ConcurrentHashMap<String, Object> controllerBeans = new ConcurrentHashMap<String, Object>();
    /**mvc的请求地址,把Url和Controller建立映射*/
    private ConcurrentHashMap<String, Object> urlBeans = new ConcurrentHashMap<String, Object>();
    /**mvc的请求方法名称，把Url和方法名建立映射*/
    private ConcurrentHashMap<String, Method> urlMethods = new ConcurrentHashMap<String, Method>();

    @Override
    public void init() throws ServletException {
        //扫描Controller包下面的类，并且找到所有带@Controller的注解倒到Bean容器里
        List<Class<?>> classes = ClassUtils.getClasses("com.libi.mvc.controller");
        try {
            findClassMvcAnnoation(classes);
        } catch (Exception e) {
            e.printStackTrace();
        }
        handleMapping();
    }

    /**找到带有Controller注解的类然后初始化放入bean容器里*/
    private void findClassMvcAnnoation(List<Class<?>> classes) throws IllegalAccessException, InstantiationException {
        for (Class<?> classInfo : classes) {
            ExtController extController = classInfo.getAnnotation(ExtController.class);
            if (extController != null) {
                String beanId = ClassUtils.toLowerCaseFirstOne(classInfo.getSimpleName());
                System.out.println("put controller:"+beanId);
                controllerBeans.put(beanId, classInfo.newInstance());
            }
        }
    }

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
        //使用视图转换器渲染页面
        extResourceViewResolver(result,req,resp);
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

    /**视图解析器*/
    private void extResourceViewResolver(String pageName, HttpServletRequest request,HttpServletResponse response) throws ServletException, IOException {
        String prefix = "/WEB-INF/view/";
        String suffix = ".jsp";
        request.getRequestDispatcher(prefix+pageName+suffix).forward(request,response);
    }
}
