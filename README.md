# feign-plus
mock feign client without spring-cloud feign dependency 

study from crossoverJie/feign-plus repository



# Feign-Plus 是一个mock Spring-cloud Feign的应用

[feign-plus](https://github.com/Yonhoo/feign-plus)，是一个学习[crossover:feign-plus](https://github.com/crossoverJie/feign-plus)的clone，目的是为了学习如何从开启一个注解到注入一个FactoryBean，熟悉注入的流程。

---

## 如何使用

使用方法和原来的feign-client是一样的。

**1.** **使用**<font color=#0099ff size=3 > FeignPlusClient</font> **注解在定义的第三方client上**

```java
@FeignPlusClient(name = "client-service",url = "http://localhost:8080")
public interface HelloClient {

    @RequestLine("GET /hello-world")
    String hello();

    @RequestLine("POST /bug-maker")
    String bugMaker();

}

```

**2. 开启<font color=#0099ff size=3 >@EnableFeignPlusClients</font>注解**

```java
@RunWith(SpringRunner.class)
@ActiveProfiles("test")
@SpringBootTest
@EnableFeignPlusClients(basePackages = "com.example.feignplus.test")
@EnableAutoConfiguration
class FeignPlusApplicationTests {

    @Autowired
    private HelloClient helloClient;

    @Test
    void should() {
        System.out.println(helloClient.hello());
        System.out.println(helloClient.bugMaker());
    }

}
```

通过这种方式就可以实现构建http请求来调用第三方接口，避免自己写大量http-client代码。


## code analysis

**3. 首先是定义<font color=#0099ff size=3 > FeignPlusClient</font>注解**

```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface FeignPlusClient {

    String name() default "";

    /**
     * @return Target url
     */
    String url() default "";
}
```

**4. 定义<font color=#0099ff size=3 >@EnableFeignPlusClients</font>注解，使得spring能扫描到使用<font color=#0099ff size=3 > FeignPlusClient</font>的interface，然后构造其factory，注入到容器中。**

```java
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Import(FeignPlusClientsRegister.class)
public @interface EnableFeignPlusClients {

    String[] value() default {};

    /**
     * Base packages to scan for annotated components.
     *
     * @return
     */
    String[] basePackages() default {};
}
```

> <font color=#0099ff size=3 >@import</font>的作用是用来导入配置类或者一些需要前置加载的类，其支持三种方式。
>
> - 带有@Configuration的配置类
> - ImportSelector 的实现
> - ImportBeanDefinitionRegistrar 的实现
>
> 通过实现ImportBeanDefinitionRegistrar，就可以通过scan自己定义的client interface，然后定义`FactoryBean`注入到容器中

**5. 实现`FeignPlusBeanFactory`，注入到容器中**

```java
public class FeignPlusBeanFactory<T> implements FactoryBean<T>, ApplicationContextAware{

....

    @Override
    public T getObject() throws Exception {
        FeignPlusConfigurationProperties conf = applicationContext.getBean(FeignPlusConfigurationProperties.class);
        Client client;
        try {
            client = applicationContext.getBean("client", Client.class);
        } catch (NoSuchBeanDefinitionException e) {
            throw new NullPointerException("Without one of [okhttp3, Http2Client] client.");
        }
        T target = Feign.builder()
                .client(client)
                .retryer(new Retryer.Default(100, SECONDS.toMillis(1), 0))
                .options(new Request.Options(conf.getConnectTimeout(), conf.getReadTimeout(), true))
                .target(proxyInterface, url);

        return target;
    }

...
}
```

其实现内部就是返回一个FeignPlus FactoryBean object，也就是对第三方feign包装了一层，这样每次定义http-client就可以复用它，构建factory bean。通过调用client interface，到调用实现它的class ->factory Bean

**6. `ImportBeanDefinitionRegistrar` 的实现**

```java
public class FeignPlusClientsRegister implements ImportBeanDefinitionRegistrar, ResourceLoaderAware {

    ...


    @Override
    public void registerBeanDefinitions(AnnotationMetadata metadata, BeanDefinitionRegistry registry) {
        AnnotationAttributes attributes = AnnotationAttributes.fromMap(metadata.getAnnotationAttributes(EnableFeignPlusClients.class.getName()));


        List<String> basePackages = new ArrayList<>();
        for (String pkg : attributes.getStringArray("basePackages")) {
            if (StringUtils.hasText(pkg)) {
                basePackages.add(pkg);
            }
        }

        FeignPlusClientScanner scanner = new FeignPlusClientScanner(registry);

        scanner.doScan(StringUtils.toStringArray(basePackages));

    }


}
```



获取<font color=#0099ff size=3 >@EnableFeignPlusClients</font>注解里面定义的值，也就是获得使用注解<font color=#0099ff size=3 >@FeignPlusClient</font>的包地址，然后通过doScan注入到容器中。

**7. `FeignPlusClientScanner`的实现**

```java
public class FeignPlusClientScanner extends ClassPathBeanDefinitionScanner {
    public FeignPlusClientScanner(BeanDefinitionRegistry registry) {
        super(registry, true);
        registerFilters();
    }

  

    /**
     * @param basePackages
     * @return
     */
 
    @Override
    public Set<BeanDefinitionHolder> doScan(String... basePackages) {
        Set<BeanDefinitionHolder> beanDefinitions = super.doScan(basePackages);
        if (beanDefinitions.isEmpty()) {
            logger.warn("No feign plus client is found in package '" + Arrays.toString(basePackages) + "'.");
        }

        GenericBeanDefinition definition;
        Set<String> classNames = new HashSet<>();
        classNames.add("com.example.feignplus.register.FeignPlusClient");
        classNames.add("com.example.feignplus.register.EnableFeignPlusClients");

        List<BeanDefinitionHolder> holders = beanDefinitions.stream().filter((item) ->
                !classNames.contains(item.getBeanDefinition().getBeanClassName()))
                .collect(Collectors.toList());

        for (BeanDefinitionHolder holder : holders) {

            definition = (GenericBeanDefinition) holder.getBeanDefinition();

            String beanClassName = definition.getBeanClassName();

            MergedAnnotation<FeignPlusClient> feignPlus = ((ScannedGenericBeanDefinition) definition).getMetadata().getAnnotations().get(FeignPlusClient.class);
            
            definition.setBeanClass(FeignPlusBeanFactory.class);
            
            definition.getPropertyValues().add("proxyInterface", beanClassName);
            definition.getPropertyValues().add("url", feignPlus.getValue("url").get());
            }

        return beanDefinitions;
    }


    // 只有该注解使用在interface上，才能进行FactoryBean构建和注入的候选人
    @Override
    protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
        return beanDefinition.getMetadata().isInterface();
    }
}
```

Scanner的实现比较重要，它是获取使用@FeignPlusClient注解的client interface，然后构建FeignPlusBeanFactory，设置feign client需要的proxyInterface、url；这样当调用interface时，其实是调用该factory。

**8. `feign-client`的动态指定**

由于 `Feign` 支持多个客户端，所以这里的客户端可以通过配置文件动态指定。
```java
    @Bean(value = "client")
    @ConditionalOnExpression("'okhttp3'.equals('${feign.httpclient:okhttp3}')")
    public Client okHttpClient(ConnectionPool connectionPool) {
        OkHttpClient delegate = new OkHttpClient().newBuilder()
                .connectionPool(connectionPool)
                .connectTimeout(feignPlusConfigurationProperties.getConnectTimeout(), TimeUnit.MILLISECONDS)
                .readTimeout(feignPlusConfigurationProperties.getReadTimeout(), TimeUnit.MILLISECONDS)
                .writeTimeout(feignPlusConfigurationProperties.getWriteTimeout(), TimeUnit.MILLISECONDS)
                .build();
        return new feign.okhttp.OkHttpClient(delegate);
    }

    @Bean(value = "client")
    @ConditionalOnExpression("'http2Client'.equals('${feign.httpclient:okhttp3}')")
    public Client client() {
        HttpClient httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(Duration.ofMillis(feignPlusConfigurationProperties.getConnectTimeout()))
                .build();
        return new Http2Client(httpClient);
    }
```




