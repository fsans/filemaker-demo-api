package com.filemaker.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;

@SpringBootApplication
@OpenAPIDefinition(
    info = @Info(
        title = "FileMaker Demo API",
        version = "1.0",
        description = "REST API demo for FileMaker Hibernate Dialect - CRUD operations on Contacts database"
    )
)
public class FileMakerDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(FileMakerDemoApplication.class, args);
    }
}
