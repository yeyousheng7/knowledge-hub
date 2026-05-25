package com.yousheng.knowledgehub;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.yousheng.knowledgehub.user.mapper")
public class KnowledgehubApplication {

    public static void main(String[] args) {
        SpringApplication.run(KnowledgehubApplication.class, args);
    }

}
