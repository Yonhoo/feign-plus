package com.example.feignplus.register;


import com.example.feignplus.factory.FeignPlusBeanFactory;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.context.annotation.ScannedGenericBeanDefinition;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.TypeFilter;


import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


public class FeignPlusClientScanner extends ClassPathBeanDefinitionScanner {
    public FeignPlusClientScanner(BeanDefinitionRegistry registry) {
        super(registry, true);
        registerFilters();
    }

    public void registerFilters() {
        // include all interfaces
        addIncludeFilter(new TypeFilter() {
            @Override
            public boolean match(MetadataReader metadataReader, MetadataReaderFactory metadataReaderFactory) throws IOException {
                return true;
            }
        });


        // exclude package-info.java
        addExcludeFilter(new TypeFilter() {
            @Override
            public boolean match(MetadataReader metadataReader, MetadataReaderFactory metadataReaderFactory) throws IOException {
                String className = metadataReader.getClassMetadata().getClassName();
                return className.endsWith("package-info");
            }
        });
    }

    /**
     * @param basePackages
     * @return
     */
    @SneakyThrows
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

//                String d =feignPlus.getSource().toString();
//                Class c = Class.forName(d);

            //    Method[] fd = c.getDeclaredMethods();



//                for (Method method: c.getDeclaredMethods()) {
//                    RequestLine annotation=method.getAnnotation(RequestLine.class);
//                    String vd = annotation.value();
//
//                }




                definition.setBeanClass(FeignPlusBeanFactory.class);

                definition.getPropertyValues().add("proxyInterface", beanClassName);
                definition.getPropertyValues().add("url", feignPlus.getValue("url").get());
            }

        return beanDefinitions;
    }


    @Override
    protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
        return beanDefinition.getMetadata().isInterface();
    }
}

