package com.libi.ioc.utils;

import org.apache.commons.lang.StringUtils;
import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.io.InputStream;
import java.util.Iterator;
import java.util.List;

/**
 * @author libi
 */
public class XmlUtils {
    public static void main(String[] args) throws DocumentException {
        XmlUtils xmlUtils = new XmlUtils();
        xmlUtils.readXml("student.xml");
    }

    public void readXml(String xmlPath) throws DocumentException {
        SAXReader saxReader = new SAXReader();
        Document document = saxReader.read(getResourceAsSteam(xmlPath));
        Element rootElement = document.getRootElement();
        getNodes(rootElement);
    }

    private static void getNodes(Element rootElement) {
        //获取节点名称
        System.out.print("节点名称：" + rootElement.getName()+"\t\t");
        //获取节点属性
        List<Attribute> attributes = rootElement.attributes();
        for (Attribute attribute : attributes) {
            System.out.print("属性："+attribute.getName()+"---"+attribute.getText()+"\t\t");
        }
        //获取属性值
        String value = rootElement.getTextTrim();
        if (!StringUtils.isEmpty(value)) {
            System.out.print("节点值：" + value+"\t\t");
        }
        System.out.println();
        //遍历子节点
        Iterator<Element> elementIterator = rootElement.elementIterator();
        while (elementIterator.hasNext()) {
            Element next = elementIterator.next();
            getNodes(next);
        }
    }


    private InputStream getResourceAsSteam(String xmlPath) {
        return this.getClass().getClassLoader().getResourceAsStream(xmlPath);
    }
}
