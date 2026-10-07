package com.bese.tesouraria;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@EnableCaching
@SpringBootApplication
public class TesourariaApplication {
	public static void main(String[] args) {
		SpringApplication.run(TesourariaApplication.class, args);
	}

}
