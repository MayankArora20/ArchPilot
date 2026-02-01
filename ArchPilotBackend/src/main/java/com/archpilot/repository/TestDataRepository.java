package com.archpilot.repository;

import com.archpilot.entity.TestData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TestDataRepository extends JpaRepository<TestData, Long> {
    
    List<TestData> findByNameContainingIgnoreCase(String name);
    
    @Query("SELECT t FROM TestData t ORDER BY t.createdAt DESC")
    List<TestData> findAllOrderByCreatedAtDesc();
    
    @Query(value = "SELECT COUNT(*) FROM test_data", nativeQuery = true)
    Long countAllRecords();
}