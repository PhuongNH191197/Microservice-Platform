package com.platform.crbtcampaign.repository;

import com.platform.crbtcampaign.entity.UserSubscription;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserSubscriptionRepository extends JpaRepository<UserSubscription, Long> {
    List<UserSubscription> findByUserIdAndStatus(Long userId, UserSubscription.Status status);
}
