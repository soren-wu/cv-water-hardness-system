package com.example.titration;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.example.titration.module")
public class TitrationApplication {

    public static void main(String[] args) {
        SpringApplication.run(TitrationApplication.class, args);
    }

}
