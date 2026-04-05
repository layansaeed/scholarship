package com.example.beans.service.job;

import com.example.beans.constant.IntegrationType;
import com.example.beans.model.JobDetailsEntity;
import com.example.beans.model.JobExecutionAuditEntity;
import com.example.beans.repository.JobDetailsJpaRepository;
import com.example.beans.repository.JobExecutionAuditJpaRepository;
import com.example.beans.service.pattern.IntegrationStrategyFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
@Slf4j
@Service
public class JobDetailsService {

    private final JobAuditService jobAuditService;
    private final JobDetailsJpaRepository jobRepository;
    private final IntegrationStrategyFactory strategyFactory;

    public JobDetailsService(JobAuditService jobAuditService, JobDetailsJpaRepository jobRepository,
                             IntegrationStrategyFactory strategyFactory) {
        this.jobAuditService = jobAuditService;
        this.jobRepository = jobRepository;
        this.strategyFactory = strategyFactory;
    }

    public JobExecutionAuditEntity getAuditRequired(Long id) {
        return jobAuditService.getAudit(id)
                .orElseThrow(() -> new RuntimeException("Audit not found for id: " + id));
    }

    public JobDetailsEntity getJobRequired(Long id) {
        return jobRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Job not found for id: " + id));
    }

    public void runJobByAuditId(Long auditId,Long nin) {
        JobExecutionAuditEntity audit = getAuditRequired(auditId);
        JobDetailsEntity job = getJobRequired(audit.getJobId());

        IntegrationType type = job.getJobName();
        strategyFactory.getStrategy(type).insertForAllNins();
    }

    public void runJobsByAuditIds(List<Long> auditIds) {
        if (auditIds == null || auditIds.isEmpty()) {
            throw new RuntimeException("Audit id list is empty");
        }

        List<JobDetailsEntity> jobs = loadJobsFromAuditIds(auditIds);
        log.info("000000000000000000000[Before sorting]0000000000000000000000");
        jobs.forEach(System.out::println);
        List<JobDetailsEntity> sortedJobs = sortJobsByPriority(jobs);
        log.info("000000000000000000000[After sorting]0000000000000000000000");
        sortedJobs.forEach(System.out::println);
        executeJobs(sortedJobs);
    }

    private List<JobDetailsEntity> loadJobsFromAuditIds(List<Long> auditIds) {
        List<JobDetailsEntity> jobs = new ArrayList<>();

        for (Long auditId : auditIds) {
            JobExecutionAuditEntity audit = getAuditRequired(auditId);
            JobDetailsEntity job = getJobRequired(audit.getJobId());
            jobs.add(job);
        }

        return jobs;
    }

    /**
     * Job A → priority 3
     * Job B → priority 1
     * Job C → priority 2
     *
     * The list may come in this order:
     * [Job A, Job B, Job C]
     *
     *It changes the same list object.
     *It does not create a brand new list.
     * So after sorting, jobs itself becomes ordered.
     */
    private List<JobDetailsEntity> sortJobsByPriority(List<JobDetailsEntity> jobs) {
        //3, null, 1, 2 ->  1, 2, 3, null ; null.compareTo(2) it crashes with NullPointerException.
        //So nullsLast protects the sort when some priorities are missing.
        jobs.sort(Comparator.comparing( //Java extracts priority using: JobDetailsEntity::getPriority -> job1.getPriority()  vs  job2.getPriority() -> 3 compare null
                JobDetailsEntity::getPriority, Comparator.nullsLast(Integer::compareTo) //compare numbers
        ));
        return jobs;
    }

    //This version runs each strategy once only even if the same job appears multiple times in the audit list.
    private void executeJobs(List<JobDetailsEntity> jobs) {
        Set<IntegrationType> executedTypes = new LinkedHashSet<>();

        for (JobDetailsEntity job : jobs) {
            IntegrationType type = job.getJobName();

            if (executedTypes.add(type)) {
                strategyFactory.getStrategy(type).insertForAllNins();
                //strategyFactory.getStrategy(type).insertForNin(nin);
            }
        }
    }
}