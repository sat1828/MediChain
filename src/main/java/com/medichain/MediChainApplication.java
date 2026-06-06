package com.medichain;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
public class MediChainApplication {

    public static void main(String[] args) {
        SpringApplication.run(MediChainApplication.class, args);
    }
}
