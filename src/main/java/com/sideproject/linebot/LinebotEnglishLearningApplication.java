package com.sideproject.linebot;

import com.sideproject.linebot.config.AppRuntimeProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(AppRuntimeProperties.class)
public class LinebotEnglishLearningApplication {

    public static void main(String[] args) {
        SpringApplication.run(LinebotEnglishLearningApplication.class, args);
    }
}
