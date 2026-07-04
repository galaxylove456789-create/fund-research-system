package com.fund.research;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.fund.research.module.*.mapper")
public class FundResearchApplication {

    public static void main(String[] args) {
        SpringApplication.run(FundResearchApplication.class, args);
    }
}
