package com.nutriconsultas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Application {

	private Application() {
		// Utility class - prevent instantiation
	}

	public static void main(final String[] args) {
		SpringApplication.run(Application.class, args);
	}

}
