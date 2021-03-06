package com.example.feignplus.factory;



import feign.Client;
import feign.Feign;
import feign.Request;
import feign.Retryer;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import com.example.feignplus.configuration.FeignPlusConfigurationProperties;

import static java.util.concurrent.TimeUnit.SECONDS;


public class FeignPlusBeanFactory<T> implements FactoryBean<T>, ApplicationContextAware {


    private ApplicationContext applicationContext;

    private Class<T> proxyInterface;

    private String url;


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

    @Override
    public Class<?> getObjectType() {
        return proxyInterface;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    public Class<T> getProxyInterface() {
        return proxyInterface;
    }

    public void setProxyInterface(Class<T> proxyInterface) {
        this.proxyInterface = proxyInterface;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
