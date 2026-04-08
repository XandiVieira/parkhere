package com.relyon.parkhere;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ParkhereApplication {

	public static void main(String[] args) {
		SpringApplication.run(ParkhereApplication.class, args);
	}

}
