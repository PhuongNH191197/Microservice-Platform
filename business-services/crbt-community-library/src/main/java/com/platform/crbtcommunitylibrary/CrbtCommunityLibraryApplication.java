package com.platform.crbtcommunitylibrary;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.platform.crbtcommunitylibrary", "com.platform.common"})
public class CrbtCommunityLibraryApplication {

    public static void main(String[] args) {
        SpringApplication.run(CrbtCommunityLibraryApplication.class, args);
    }
}
