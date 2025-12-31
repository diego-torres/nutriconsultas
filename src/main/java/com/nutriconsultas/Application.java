package com.nutriconsultas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@SuppressWarnings({ "checkstyle:FinalClass", "checkstyle:HideUtilityClassConstructor" })
public class Application {

	public static void main(final String[] args) {
		SpringApplication.run(Application.class, args);
	}

}
