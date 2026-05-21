package com.platform.crbtcampaign.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.platform.common.core.exception.BaseException;
import com.platform.common.rmq.RmqExchanges;
import com.platform.common.rmq.RmqRoutingKeys;
import com.platform.crbtcampaign.dto.request.CreateCampaignRequest;
import com.platform.crbtcampaign.dto.request.CreatePackageRequest;
import com.platform.crbtcampaign.dto.response.CampaignResponse;
import com.platform.crbtcampaign.entity.Campaign;
import com.platform.crbtcampaign.entity.CampaignPackage;
import com.platform.crbtcampaign.repository.CampaignPackageRepository;
import com.platform.crbtcampaign.repository.CampaignRepository;
import com.platform.crbtcampaign.repository.UserSubscriptionRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

@ExtendWith(MockitoExtension.class)
class CampaignServiceTest {

    @Mock CampaignRepository campaignRepository;
    @Mock CampaignPackageRepository packageRepository;
    @Mock UserSubscriptionRepository subscriptionRepository;
    @Mock RabbitTemplate rabbitTemplate;

    @InjectMocks CampaignService campaignService;

    @Test
    void createCampaign_shouldPersistAndReturn() {
        Instant start = Instant.now();
        Instant end = start.plus(30, ChronoUnit.DAYS);
        CreateCampaignRequest request = new CreateCampaignRequest(
            "Tet 2026", "Tet promo", start, end,
            List.of(new CreatePackageRequest("VIP", new BigDecimal("50000"), 100, 30))
        );

        when(campaignRepository.save(any(Campaign.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CampaignResponse response = campaignService.createCampaign(request);

        assertEquals("Tet 2026", response.name());
        assertEquals(1, response.packages().size());
    }

    @Test
    void subscribe_shouldThrowWhenPackageNotFound() {
        when(packageRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(BaseException.class, () -> campaignService.subscribe(1L, 99L));
    }

    @Test
    void subscribe_shouldPublishCreditChangedEvent() {
        Instant start = Instant.now().minus(1, ChronoUnit.DAYS);
        Instant end = Instant.now().plus(30, ChronoUnit.DAYS);
        Campaign campaign = new Campaign("Tet", "desc", Campaign.Status.ACTIVE, start, end);
        CampaignPackage pkg = new CampaignPackage(campaign, "VIP", new BigDecimal("50000"), 100, 30);

        when(packageRepository.findById(1L)).thenReturn(Optional.of(pkg));

        campaignService.subscribe(42L, 1L);

        verify(rabbitTemplate).convertAndSend(eq(RmqExchanges.CREDIT_EVENTS), eq(RmqRoutingKeys.CREDIT_CHANGED), any(Object.class));
    }
}
