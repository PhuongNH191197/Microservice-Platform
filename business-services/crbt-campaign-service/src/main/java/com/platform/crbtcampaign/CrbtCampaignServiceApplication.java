package com.platform.crbtcampaign;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {"com.platform.crbtcampaign", "com.platform.common"})
@EnableScheduling
public class CrbtCampaignServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CrbtCampaignServiceApplication.class, args);
    }
}
