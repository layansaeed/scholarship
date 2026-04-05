package com.example.beans.service.job;

import com.example.beans.model.JobExecutionAuditEntity;
import com.example.beans.repository.JobExecutionAuditJpaRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class JobAuditService {

    private final JobExecutionAuditJpaRepository repository;

    public JobAuditService(JobExecutionAuditJpaRepository repository) {
        this.repository = repository;
    }

    public Optional<JobExecutionAuditEntity> getAudit(Long id){
        return repository.findById(id);
    }
}
