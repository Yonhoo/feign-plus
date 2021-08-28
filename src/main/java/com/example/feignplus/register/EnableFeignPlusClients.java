package com.example.feignplus.register;

import org.springframework.context.annotation.Import;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


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

