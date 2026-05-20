package com.example.newsapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/*
"NewsAppApplication.java is the entry point. The @SpringBootApplication annotation auto-configures the Spring framework and starts an embedded Tomcat server. SpringApplication.run() boots the entire application."
*/
@SpringBootApplication
public class NewsAppApplication {
    public static void main(String[] args) {
        SpringApplication.run(NewsAppApplication.class, args);
    }
}