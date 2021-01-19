package com.h2t.study;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class SpringbootLogbackApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpringbootLogbackApplication.class, args);
	}

}
