package com.example.beans.service.job;

import com.example.beans.model.JobExecutorBatchRequest;
import com.example.beans.model.JobExecutionEntity;
import com.example.beans.model.JobExecutorRequest;
import com.example.beans.repository.JobExecutionJpaRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class JobExecutionService {

    private final JobExecutionJpaRepository jobExecutionRepository;
    private final ParallelNinProcessorService parallelNinProcessorService;
    private final JobDetailsService jobDetailsService;

    public JobExecutionService(JobExecutionJpaRepository jobExecutionRepository,
                               ParallelNinProcessorService parallelNinProcessorService, JobDetailsService jobDetailsService) {
        this.jobExecutionRepository = jobExecutionRepository;
        this.parallelNinProcessorService = parallelNinProcessorService;
        this.jobDetailsService = jobDetailsService;
    }

    public void runJobsByExecutionIds(JobExecutorBatchRequest request) {
        List<JobExecutionEntity> executions = loadExecutions(request);
        executeJobs(executions);
    }

    private List<JobExecutionEntity> loadExecutions(JobExecutorBatchRequest request) {
        if (request == null || request.getJobs() == null || request.getJobs().isEmpty()) {
            throw new RuntimeException("Job request is empty");
        }

        List<JobExecutionEntity> executions = new ArrayList<>();

        for (JobExecutorRequest executionRequest : request.getJobs()) {
            Long executionId = executionRequest.getExecutionId();

            if (executionId == null) {
                throw new RuntimeException("Execution id must not be null");
            }

            log.info("Loading execution id={}", executionId);

            JobExecutionEntity execution = jobExecutionRepository.findById(executionId)
                    .orElseThrow(() -> new RuntimeException("Execution not found for id: " + executionId));

            executions.add(execution);
        }

        return executions;
    }

    private void executeJobs(List<JobExecutionEntity> executions) {
        for (JobExecutionEntity execution : executions) {
            Long executionId = execution.getExecutionId();

            try {
                String jobName = execution.getJobName();
                Long start = execution.getNinRangeStart();
                Long end = execution.getNinRangeEnd();

                if (jobName == null || jobName.trim().isEmpty()) {
                    throw new RuntimeException("Job name is missing for execution id: " + executionId);
                }

                if (start == null || end == null) {
                    throw new RuntimeException("NIN range start/end is missing for execution id: " + executionId);
                }

                log.info("Executing executionId={}, jobName={}, start={}, end={}",
                        executionId, jobName, start, end);

                parallelNinProcessorService.processInParallel(jobName, start, end);

            } catch (Exception e) {
                log.error("Failed executionId={}", executionId, e);
                throw e;
            }
        }
    }
    /**
     * New logic:
     * run one full job using jobId only
     */
    public void runFullJobByJobId(Long jobId) {
        if (jobId == null) {
            throw new RuntimeException("Job id must not be null");
        }

        String jobName = jobDetailsService.getJobNameRequired(jobId);

        log.info("Executing full job for jobId={}, jobName={}", jobId, jobName);

        parallelNinProcessorService.processInParallel(jobName);
    }
}