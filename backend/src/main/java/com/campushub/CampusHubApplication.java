package com.campushub;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * CampusHub 校园论坛社区 启动类
 */
@SpringBootApplication
@MapperScan("com.campushub.mapper")
public class CampusHubApplication {

    public static void main(String[] args) {
        SpringApplication.run(CampusHubApplication.class, args);
    }
}
