package com.minisocial;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class MiniSocialApplication {

    public static void main(String[] args) {
        SpringApplication.run(MiniSocialApplication.class, args);
    }
}
