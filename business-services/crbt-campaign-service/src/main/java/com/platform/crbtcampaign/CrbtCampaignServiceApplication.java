package com.platform.crbtcampaign;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.platform.crbtcampaign", "com.platform.common"})
public class CrbtCampaignServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CrbtCampaignServiceApplication.class, args);
    }
}
