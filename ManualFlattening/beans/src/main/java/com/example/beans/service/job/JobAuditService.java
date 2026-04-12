//package com.example.beans.service.job;
//
//import com.example.beans.constant.ExecutionStatus;
//import com.example.beans.model.JobExecutionAuditEntity;
//import com.example.beans.repository.JobExecutionAuditJpaRepository;
//import com.example.beans.repository.JobExecutionStatusRepository;
//import org.springframework.stereotype.Service;
//
//import java.time.LocalDateTime;
//import java.util.Optional;
//
//@Service
//public class JobAuditService {
//
//    private final JobExecutionAuditJpaRepository repository;
//
//private final JobExecutionStatusRepository statusRepository;
//    public JobAuditService(JobExecutionAuditJpaRepository repository, JobExecutionStatusRepository statusRepository) {
//        this.repository = repository;
//        this.statusRepository = statusRepository;
//    }
//
//    public Optional<JobExecutionAuditEntity> getAudit(Long id){
//        return repository.findById(id);
//    }
//
//    public void updateStatus(Long auditId, ExecutionStatus status) {
//        repository.findById(auditId).ifPresent(audit -> {
//            audit.setStatus(statusRepository.findByStatus(status));
//            audit.setEndTime(LocalDateTime.now());
//            repository.save(audit);
//        });
//
//    }
//}
