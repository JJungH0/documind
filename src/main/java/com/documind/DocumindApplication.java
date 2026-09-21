package com.documind;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@ConfigurationPropertiesScan
@SpringBootApplication
public class DocumindApplication {

	public static void main(String[] args) {
		SpringApplication.run(DocumindApplication.class, args);
	}

}
