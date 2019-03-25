package com.libi.context;


import com.libi.annotation.ExtResource;
import com.libi.annotation.ExtService;
import com.libi.utils.ClassUtils;
import org.apache.commons.lang.StringUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author libi
 * 拿到扫包范围然后扫描包拿到Bean
 */
public class ExtAnnotationApplicationContext {
    private String packageName;
    /**保存有Service注解的类*/
    private ConcurrentHashMap<String, Object> beans = null;

    public ExtAnnotationApplicationContext(String packageName) throws InstantiationException, IllegalAccessException {
        this.packageName = packageName;
        initBeans();
        //在所有Bean容器里所有bean自动注入所有的Bean
        for (Map.Entry<String, Object> entry : beans.entrySet()) {
            System.out.println("beanId:"+entry.getKey());
            Object bean = entry.getValue();
            attrAssign(bean);
        }
    }

    /**初始化Bean容器*/
    private void initBeans() throws IllegalAccessException, InstantiationException {
        beans = new ConcurrentHashMap<String, Object>();
        //使用扫包工具获得包下所有的类
        List<Class<?>> classes = ClassUtils.getClasses(packageName);
        //判断所有的类上面是否有注解,有的话就会加入到Bean容器里面去
        findClassExistAnnotation(classes);
        if (beans == null || beans.isEmpty()) {
            throw new RuntimeException("没有类加上了注解");
        }
    }

    /**扫包，把有注解的类加入到bean容器里*/
    private void findClassExistAnnotation(List<Class<?>> classes) throws InstantiationException, IllegalAccessException {
        for (Class classInfo : classes) {
            //判断是否有注解
            Annotation annotation = classInfo.getAnnotation(ExtService.class);
            if (annotation != null) {
                //到这里表示有这个注解
                String className = classInfo.getName();
                //默认Id是首字母小写
                beans.put(toLowerCaseFirestOne(classInfo.getSimpleName()), newInstance(classInfo));
            }
        }
    }

    /**类名的首字母小写*/
    private String toLowerCaseFirestOne(String className) {
        return new StringBuilder().append(Character.toLowerCase(className.charAt(0))).append(className.substring(1)).toString();
    }

    /**获取Bean的方法*/
    public Object getBean(String beanId) throws IllegalAccessException, InstantiationException {
        if (StringUtils.isEmpty(beanId)) {
            throw new RuntimeException("BeanID为空");
        }
        return beans.get(beanId);
    }

    /**利用反射机制创建Bean*/
    private Object newInstance(Class classInfo) throws IllegalAccessException, InstantiationException {
        if (classInfo == null) {
            throw new RuntimeException("没有这个ID的bean");
        }
        return classInfo.newInstance();
    }

    /**自动注入注入这个对象的属性*/
    private void attrAssign(Object object) throws IllegalAccessException {
        //获取这个类所有的属性
        Field[] fields = object.getClass().getDeclaredFields();
        //判断当前属性是否有注解
        for (Field field : fields) {
            ExtResource extResource = field.getAnnotation(ExtResource.class);
            if (extResource != null) {
                //允许访问私有属性
                field.setAccessible(true);
                //到这里说明这个属性里有这个注解,在从容器里获取对象然后给这个属性赋值
                String fieldName = field.getName();
                Object target = beans.get(fieldName);
                if (target == null) {
                    throw new RuntimeException("注入\"" + fieldName + "\"属性失败，bean容器里没有这个对象");
                }
                //第一个参数是这个属性所在的对象
                field.set(object,target);
            }
        }

    }
}
