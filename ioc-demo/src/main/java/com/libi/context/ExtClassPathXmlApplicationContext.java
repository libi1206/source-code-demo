package com.libi.context;

import org.apache.commons.lang.StringUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.io.InputStream;
import java.util.List;

/**
 * @author libi
 * 读取XML拿到所有的Bean
 */
public class ExtClassPathXmlApplicationContext {
    private String xmlPath;

    public ExtClassPathXmlApplicationContext(String xmlPath) {
        this.xmlPath = xmlPath;
    }

    /** 用于GetBean的方法*/
    public Object getBean(String beanId) throws DocumentException, IllegalAccessException, InstantiationException, ClassNotFoundException {
        if (StringUtils.isEmpty(beanId)) {
            throw new RuntimeException("BeanId为空");
        }
        //解析Xml,获取所有节点
        List<Element> elements = readXml();
        if (elements == null||elements.isEmpty()) {
            throw new RuntimeException("没有任何Bean信息");
        }
        //查找对应的ClassName
        String className = getClassName(beanId, elements);
        if (StringUtils.isEmpty(className)) {
            throw new RuntimeException("没有配置类信息");
        }
        //利用反射机制创建Bean
        return newInstance(className);
    }

    /**解析Xml文件,获取所有节点*/
    private List<Element> readXml() throws DocumentException {
        SAXReader saxReader = new SAXReader();
        Document document = saxReader.read(getResourceAsSteam());
        Element rootElement = document.getRootElement();
        List<Element> elements = rootElement.elements();
        return elements;
    }

    /**在节点里查找对应的ClassName*/
    private String  getClassName(String beanId, List<Element> elements) {
        for (Element element : elements) {
            //找到Id属性，和传入的beanId做对比
            String xmlBeanId = element.attributeValue("id");
            if (StringUtils.isEmpty(xmlBeanId)) {
                continue;
            }
            if (beanId.equals(xmlBeanId)) {
                //到这里说明找到了对应的BeanId，返回Class全名
                return element.attributeValue("class");
            }
        }
        return null;
    }

    /**利用反射机制创建Bean*/
    private Object newInstance(String xmlClass) throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        Class<?> target = Class.forName(xmlClass);
        return target.newInstance();
    }

    /** 获取当前上下文路径*/
    private InputStream getResourceAsSteam() {
        return this.getClass().getClassLoader().getResourceAsStream(xmlPath);
    }
}
