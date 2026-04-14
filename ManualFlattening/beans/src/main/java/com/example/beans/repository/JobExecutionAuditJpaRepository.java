package com.example.beans.repository;

import com.example.beans.model.JobExecutionAuditEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JobExecutionAuditJpaRepository extends JpaRepository<JobExecutionAuditEntity, Long> {
}