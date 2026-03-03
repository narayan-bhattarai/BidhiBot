package com.ragdesk;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
@ComponentScan(basePackages = "com.ragdesk")
@EnableJpaRepositories(basePackages = "com.ragdesk.repositories")
@EntityScan(basePackages = "com.ragdesk.models")
public class RagDeskApplication {
    public static void main(String[] args) {
        SpringApplication.run(RagDeskApplication.class, args);
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
