package com.gourmet;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 美食推荐系统启动类。
 *
 * <p>启动后访问：
 * <ul>
 *     <li>前端页面：http://localhost:8080/</li>
 *     <li>Swagger 文档：http://localhost:8080/swagger-ui.html</li>
 *     <li>H2 控制台（dev）：http://localhost:8080/h2-console</li>
 * </ul>
 */
@SpringBootApplication
@MapperScan("com.gourmet.mapper")
@EnableScheduling
public class GourmetApplication {

    public static void main(String[] args) {
        SpringApplication.run(GourmetApplication.class, args);
    }
}
