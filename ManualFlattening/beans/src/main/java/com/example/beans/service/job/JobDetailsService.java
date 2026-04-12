//package com.example.beans.service.job;
//
//import com.example.beans.constant.ExecutionStatus;
//import com.example.beans.model.JobDetailsEntity;
//import com.example.beans.model.JobExecutionAuditEntity;
//import com.example.beans.model.JobExecutorBatchRequest;
//import com.example.beans.model.JobExecutorRequest;
//import com.example.beans.repository.JobDetailsJpaRepository;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Service;
//
//import java.util.ArrayList;
//import java.util.Comparator;
//import java.util.List;
//
//@Slf4j
//@Service
//public class JobDetailsService {
//
//    private final JobAuditService jobAuditService;
//    private final JobDetailsJpaRepository jobRepository;
//    private final ParallelNinProcessorService parallelNinProcessorService;
//
//    public JobDetailsService(JobAuditService jobAuditService,
//                             JobDetailsJpaRepository jobRepository,
//                             ParallelNinProcessorService parallelNinProcessorService) {
//        this.jobAuditService = jobAuditService;
//        this.jobRepository = jobRepository;
//        this.parallelNinProcessorService = parallelNinProcessorService;
//    }
//
//    public JobExecutionAuditEntity getAuditRequired(Long id) {
//        return jobAuditService.getAudit(id)
//                .orElseThrow(() -> new RuntimeException("Audit not found for id: " + id));
//    }
//
//    public JobDetailsEntity getJobRequired(Long id) {
//        return jobRepository.findById(id)
//                .orElseThrow(() -> new RuntimeException("Job not found for id: " + id));
//    }
//
//    /**
//     * Loads audits, sorts them by job priority, then executes each audit.
//     */
//    public void runJobsByAuditIds(JobExecutorBatchRequest request) {
//        List<JobExecutionAuditEntity> audits = loadAudits(request);
//        sortAuditsByJobPriority(audits);
//        executeAudits(audits);
//    }
//
//    private List<JobExecutionAuditEntity> loadAudits(JobExecutorBatchRequest request) {
//        if (request == null || request.getJobs() == null || request.getJobs().isEmpty()) {
//            throw new RuntimeException("Job request is empty");
//        }
//
//        List<JobExecutionAuditEntity> audits = new ArrayList<JobExecutionAuditEntity>();
//
//        for (JobExecutorRequest auditRequest : request.getJobs()) {
//            Long auditId = auditRequest.getAuditId();
//
//            if (auditId == null) {
//                throw new RuntimeException("Audit id must not be null");
//            }
//
//            log.info("Loading audit id={}", auditId);
//            audits.add(getAuditRequired(auditId));
//        }
//
//        return audits;
//    }
//
//    private void sortAuditsByJobPriority(List<JobExecutionAuditEntity> audits) {
//        audits.sort(Comparator.comparing(
//                audit -> getJobRequired(audit.getJob().getJobId()).getPriority(),
//                Comparator.nullsLast(Integer::compareTo)
//        ));
//    }
//
//    /**
//     * Executes each audit using:
//     * - jobName from JOB_DETAILS
//     * - range start/end from AUDIT table
//     */
//    private void executeAudits(List<JobExecutionAuditEntity> audits) {
//        for (JobExecutionAuditEntity audit : audits) {
//            Long auditId = audit.getAuditId();
//
//            try {
//                JobDetailsEntity job = getJobRequired(audit.getJob().getJobId());
//
//                String jobName = job.getJobName();
//                Long start = audit.getNinRangeStart();
//                Long end = audit.getNinRangeEnd();
//
//                if (jobName == null || jobName.trim().isEmpty()) {
//                    throw new RuntimeException("Job name is missing for audit id: " + auditId);
//                }
//
//                if (start == null || end == null) {
//                    throw new RuntimeException("NIN range start/end is missing for audit id: " + auditId);
//                }
//
//                jobAuditService.updateStatus(auditId, ExecutionStatus.PROCESSING);
//
//                parallelNinProcessorService.processInParallel(jobName, start, end);
//
//                jobAuditService.updateStatus(auditId, ExecutionStatus.SUCCEEDED);
//
//            } catch (Exception e) {
//                jobAuditService.updateStatus(auditId, ExecutionStatus.FAILED);
//                log.error("Failed auditId={}", auditId, e);
//                throw e;
//            }
//        }
//    }
//}