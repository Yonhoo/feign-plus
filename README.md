# feign-plus
mock feign client without spring-cloud feign dependency 

study from crossoverJie/feign-plus repository



# Feign-Plus is a mock Spring-cloud Feign application

[feign-plus](https://github.com/Yonhoo/feign-plus)，It is a clone for learning from [crossover:feign-plus](https://github.com/crossoverJie/feign-plus). The purpose is to learn how to start an annotation to inject a factorybean and get familiar with the injection process.


---

## How To Use

The use method is the same as the original feign client.

**1.** **using**<font color=#0099ff size=3 > FeignPlusClient</font> **annotation on the defined third-party client**

```java
@FeignPlusClient(name = "client-service",url = "http://localhost:8080")
public interface HelloClient {

    @RequestLine("GET /hello-world")
    String hello();

    @RequestLine("POST /bug-maker")
    String bugMaker();

}

```

**2. Enable<font color=#0099ff size=3 >@EnableFeignPlusClients</font>annotation**

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

In this way, you can build HTTP requests to call third-party interfaces, and avoid writing a lot of HTTP client code yourself.


## code analysis

**3. Definition of<font color=#0099ff size=3 > FeignPlusClient</font> annotation**

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

**4. Definition of<font color=#0099ff size=3 >@EnableFeignPlusClients</font>annotation，Enables spring to scan the interface <font color=#0099ff size=3 > FeignPlusClient</font> used by client，then construct client factory，inject into the container。**

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

> <font color=#0099ff size=3 >@import</font>is used to import configuration classes or some classes that need to be preloaded. It supports three methods。
>
> - Configuration class with @ configuration.
> - Implementation of ImportSelector.
> - Implementation of ImportBeanDefinitionRegistrar.
>
>By implementing importbeandefinitionregistrar, it can scan the client interface defined by yourself, and then define 'factorybean' to inject into the container

**5. Implementation of `FeignPlusBeanFactory`，inject into the container**

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

The internal implementation is to return a feignplus factorybean object, that is, wrap a layer of third-party feign, so that each time you define HTTP client, you can reuse it and build factory beans. By calling the client interface, you can call the factoryBean class 

**6. Implementation of `ImportBeanDefinitionRegistrar`**

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

Get the value defined in the <font color=#0099ff size=3 >@EnableFeignPlusClients</font> annotation, that is, get the package address using the annotation <font color=#0099ff size=3 >@FeignPlusClient</font>, and then inject it into the container through doscan function.



**7. Implementation of `FeignPlusClientScanner`**

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

The implementation of Scanner is very important. It is to obtain the client interface annotated with <font color=#0099ff size=3 >@FeignPlusClient</font>, build `FeignPlusBeanFactory`, and set the proxyInterface and url required by feign client; In this way, when the interface is called, the factory is actually called.


**8. `feign-client`的动态指定**

`Feign` can supports multiple clients, the clients here can be dynamically specified through the configuration file.

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




