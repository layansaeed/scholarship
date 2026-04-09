package com.example.beans.service.job;

import com.example.beans.constant.ExecutionStatus;
import com.example.beans.model.JobDetailsEntity;
import com.example.beans.model.JobExecutionAuditEntity;
import com.example.beans.model.JobExecutorBatchRequest;
import com.example.beans.model.JobExecutorRequest;
import com.example.beans.repository.JobDetailsJpaRepository;
import com.example.beans.service.pattern.IntegrationStrategy;
import com.example.beans.service.pattern.IntegrationStrategyRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
public class JobDetailsService {

    private final JobAuditService jobAuditService;
    private final JobDetailsJpaRepository jobRepository;
    private final IntegrationStrategyRegistry strategyRegistry;

    public JobDetailsService(JobAuditService jobAuditService,
                             JobDetailsJpaRepository jobRepository,
                              IntegrationStrategyRegistry strategyRegistry) {
        this.jobAuditService = jobAuditService;
        this.jobRepository = jobRepository;
        this.strategyRegistry = strategyRegistry;

    }

    /**
     * Returns the required audit row by audit id.
     */
    public JobExecutionAuditEntity getAuditRequired(Long id) {
        return jobAuditService.getAudit(id)
                .orElseThrow(() -> new RuntimeException("Audit not found for id: " + id));
    }

    /**
     * Returns the required job row by job id.
     */
    public JobDetailsEntity getJobRequired(Long id) {
        return jobRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Job not found for id: " + id));
    }

    /**
     * Loads audits, sorts them by job priority, then executes each audit
     * using its own NIN range from the audit table.
     */
    public void runJobsByAuditIds(JobExecutorBatchRequest auditIds) {

        List<JobExecutionAuditEntity> audits = loadAudits(auditIds);
        sortAuditsByJobPriority(audits);
        executeAudits(audits);
    }

    /**
     * Loads all audit rows for the given audit id list.
     */
    private List<JobExecutionAuditEntity> loadAudits(JobExecutorBatchRequest request) {
        List<JobExecutionAuditEntity> audits = new ArrayList<>();

        List<JobExecutorRequest> auditRequests = request.getJobs();

        for (JobExecutorRequest auditRequest : auditRequests) {
            Long auditId = auditRequest.getAuditId();
            log.info("############ id={}", auditId);
            audits.add(getAuditRequired(auditId));
        }

        return audits;
    }
    /**
     * Sorts audit rows by the priority of their related job.
     */
    private void sortAuditsByJobPriority(List<JobExecutionAuditEntity> audits) {
        audits.sort(Comparator.comparing(
                audit -> getJobRequired(audit.getJob().getJobId()).getPriority(),
                Comparator.nullsLast(Integer::compareTo)
        ));
    }

    /**
     * Executes each audit separately using its own start/end NIN range.
     */
    private void executeAudits(List<JobExecutionAuditEntity> audits) {
        for (JobExecutionAuditEntity audit : audits) {
            Long auditId = audit.getAuditId();

            try {
                JobDetailsEntity job = getJobRequired(audit.getJob().getJobId());

                String jobName = String.valueOf(job.getJobName());
                Long start = audit.getNinRangeStart();
                Long end = audit.getNinRangeEnd();

                if (jobName == null || jobName.isBlank()) {
                    throw new RuntimeException("Job name is missing for audit id: " + auditId);
                }

                if (start == null || end == null) {
                    throw new RuntimeException("NIN range start/end is missing for audit id: " + auditId);
                }

                log.info("Starting auditId={} jobName={} start={} end={}", auditId, jobName, start, end);

                jobAuditService.updateStatus(auditId, ExecutionStatus.PROCESSING);

                //registry object depend on jobName ->how registry knows that?
                //when app starts, Spring gives registry all strategy beans objects
                //using constructor-> strategies.put(key, strategy bean object); key:job name
                //we can't remove getKey ->registry no longer knows what string belongs to each strategy object.
                IntegrationStrategy strategy = strategyRegistry.getStrategy(jobName);
                strategy.insert(start, end);

                jobAuditService.updateStatus(auditId, ExecutionStatus.SUCCEEDED);

                log.info("Completed auditId={} jobName={}", auditId, jobName);

            } catch (Exception e) {
                jobAuditService.updateStatus(auditId, ExecutionStatus.FAILED);
                log.error("Failed auditId={}", auditId, e);
                throw e;
            }
        }
    }
    }
