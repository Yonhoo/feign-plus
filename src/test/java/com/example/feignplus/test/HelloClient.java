package com.example.feignplus.test;

import com.example.feignplus.register.FeignPlusClient;
import feign.RequestLine;

@FeignPlusClient(name = "client-service",url = "http://localhost:8080")
public interface HelloClient {

    @RequestLine("GET /hello/world")
    String hello();

    @RequestLine("GET /hello/world1")
    String hello1();
}
