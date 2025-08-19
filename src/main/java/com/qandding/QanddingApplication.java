package com.qandding;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class QanddingApplication {
	public static void main(String[] args) {
		SpringApplication.run(QanddingApplication.class, args);
	}
}
