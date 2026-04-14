package com.example.beans.repository;

import com.example.beans.model.JobExecutionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JobExecutionJpaRepository extends JpaRepository<JobExecutionEntity, Long> {
    Optional<JobExecutionEntity> findFirstByJobId(Long jobId);
}