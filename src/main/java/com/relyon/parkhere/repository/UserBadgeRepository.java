package com.relyon.parkhere.repository;

import com.relyon.parkhere.model.UserBadge;
import com.relyon.parkhere.model.enums.BadgeType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface UserBadgeRepository extends JpaRepository<UserBadge, UUID> {

    List<UserBadge> findByUserIdOrderByEarnedAtDesc(UUID userId);

    boolean existsByUserIdAndBadgeType(UUID userId, BadgeType badgeType);
}
