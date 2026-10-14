package com.instakill;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class InstakillApplication {

    public static void main(String[] args) {
        SpringApplication.run(InstakillApplication.class, args);
    }
}
