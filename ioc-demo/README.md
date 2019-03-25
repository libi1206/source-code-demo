# 手写IOC
IOC控制反转就是通过反射机制帮我们托管了所有的类。<br>
我想要自己实现的就是使用XML注入Bean和使用注解（`@Service`之类的）注入Bean
### Spring的Xml版本IOC原理
SpringIOC的XML版本使用Dom4j和反射技术解析XML和注入类<br>
所有的Bean在ApplicationContext创建的时候就会初始化



## XML版本注入

### 自行解析XML
一个自己解析XML的小Demo，使用Dom4j解析XML，如下
```$xslt
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

```
### 自己实现XML获取Bean的ApplicationContext
* **实现步骤**
    * 读取配置XML
    * 查看传入的BeanId和Xml中的BeanId是否一致
    * 使用反射创建对象并且返回<br>
* **按照上面的步骤实现自己的ApplicationContext**
<br>核心方法代码如下（这个getBean方法就是按照上面的步骤实现的，我把步骤的具体实现都抽取出去了）
```$xslt
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
```
全文如下
```$xslt
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
}
```
然后在主方法里创建上面的Context，使用getBean方法，就可以拿到想要的Bean了（和Spring的ClassPathApplicationContext一样）

## 使用注解注入Bean

### 一些需要注意的性质
* 需要把已知带有注解的类装入一个集合里，便于随时取用
* 加载时才初始化上面的集合，还需要注意线程安全问题
* 使用懒加载模式加载Bean
* 只实现了单例，所有getBean都是创建了一个实例
* 只实现了使用默认的beanId进行注入（类名第一个字母小写），不能自定义ID


### 实现注解装配Bean并且通过getBean方法获取Bean
* **实现步骤**
    * 使用反射机制，扫包，获取所有的类(使用了一个开源的扫包的工具类。。没有自己实现)
    * 判断每个类上是否有注入bean的注解
    * 使用反射机制进行初始化类
* **按照上面的步骤实现自己的ApplicationContext**
<br>核心代码如下
```$xslt
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
```
全文如下
```$xslt
public class ExtAnnotationApplicationContext {
    private String packageName;
    /**保存有Service注解的类*/
    private ConcurrentHashMap<String, Object> beans = null;

    public ExtAnnotationApplicationContext(String packageName) throws InstantiationException, IllegalAccessException {
        this.packageName = packageName;
        initBeans();
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

    /**依赖注入传入类的属性*/
    private void attrAssign(Class<?> classInfo) {
        //获取这个类所有的属性
        Field[] fields = classInfo.getFields();
        //判断当前属性是否有注解
        for (Field field : fields) {
            ExtService extService = field.getAnnotation(ExtService.class);
            if (extService != null) {
                //到这里说明这个属性里有这个注解
                String fieldName = field.getName();
            }
        }

    }
}
```
现在获取到Bean只用在类上加上自己的Service注解然后使用getBean方法传入类名的首字母小写就可以了
### 实现自动装配（依赖注入）
* **自动装配/依赖注入原理（实现步骤）** 
    * 使用反射机制获取当前类的所有属性
    * 判断当前类是否存在注解
    * 使用默认名称在Bean容器里查找对象，然后赋值
* **核心代码**
```$xslt
    /**自动注入注入这个对象的属性*/
    private void attrAssign(Object object) throws IllegalAccessException {
        //获取这个类所有的属性
        Field[] fields = object.getClass().getDeclaredFields();
        //判断当前属性是否有注解
        for (Field field : fields) {
            ExtService extService = field.getAnnotation(ExtService.class);
            if (extService != null) {
                //到这里说明这个属性里有这个注解,在从容器里获取对象然后给这个属性赋值
                String fieldName = field.getName();
                Object target = beans.get(fieldName);
                if (target == null) {
                    throw new RuntimeException("注入\"" + fieldName + "\"属性失败，bean容器里没有这个对象");
                }
                //允许访问私有属性
                field.setAccessible(true);
                //第一个参数是这个属性所在的对象
                field.set(object,target);
            }
        }
    }
```
这个方法我们需要在Bean容器初始化完成之后，把所有的bean容器的Object里做一遍，达到依赖注入的效果，如下（如果给所有的类都实现注入Bean容器里的bean的话，就是依赖注入`@Autowired`了）
```
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
```
增加了依赖注入的Context全文如下
```
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

```