package com.archpilot.repository;

import com.archpilot.entity.RateLimitBucket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;

/**
 * Repository for RateLimitBucket entity with atomic operations
 */
@Repository
public interface RateLimitBucketRepository extends JpaRepository<RateLimitBucket, Long> {

    /**
     * Find rate limit bucket by user ID with pessimistic lock for atomic operations
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM RateLimitBucket r WHERE r.userId = :userId")
    Optional<RateLimitBucket> findByUserIdWithLock(@Param("userId") String userId);

    /**
     * Find rate limit bucket by user ID without lock for read operations
     */
    Optional<RateLimitBucket> findByUserId(String userId);
}