package com.example.feignplus.test;

import com.example.feignplus.register.EnableFeignPlusClients;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;


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
        System.out.println(helloClient.hello1());
    }

}
