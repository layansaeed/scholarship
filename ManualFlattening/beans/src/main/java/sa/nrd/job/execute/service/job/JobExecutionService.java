package sa.nrd.job.execute.service.job;

import sa.nrd.job.execute.constant.ExecutionStatus;
import sa.nrd.job.execute.job.BeanJob;
import sa.nrd.job.execute.model.JobDetailsEntity;
import sa.nrd.job.execute.model.JobExecutorBatchRequest;
import sa.nrd.job.execute.model.JobExecutionEntity;
import sa.nrd.job.execute.model.JobExecutorRequest;
import sa.nrd.job.execute.repository.JobExecutionJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class JobExecutionService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());

    private final JobExecutionJpaRepository jobExecutionRepository;
    private final BeneficiaryBatchProcessorService beneficiaryBatchProcessorService;
    private final JobDetailsService jobDetailsService;
    private final JobAuditService jobAuditService;
    private final BeanJob beanJob;

    /**
     * Creates the service with required dependencies.
     *
     * @param jobExecutionRepository repository for job executions
     * @param beneficiaryBatchProcessorService service for range processing
     * @param jobDetailsService service for job details
     * @param jobAuditService service for audit updates
     */
    public JobExecutionService(JobExecutionJpaRepository jobExecutionRepository,
                               BeneficiaryBatchProcessorService beneficiaryBatchProcessorService,
                               JobDetailsService jobDetailsService,
                               JobAuditService jobAuditService, BeanJob beanJob) {
        this.jobExecutionRepository = jobExecutionRepository;
        this.beneficiaryBatchProcessorService = beneficiaryBatchProcessorService;
        this.jobDetailsService = jobDetailsService;
        this.jobAuditService = jobAuditService;
        this.beanJob = beanJob;

    }

    /**
     * Runs multiple jobs based on execution ids.
     *
     * @param request batch request containing execution ids
     */
    public void runJobsByExecutionIds(JobExecutorBatchRequest request) {
        beanJob.markJobStarted();
        try {
            List<JobExecutionEntity> executions = loadExecutions(request);
            executeJobs(executions);

        } finally {
            beanJob.markJobFinished();
        }
    }

    /**
     * Loads execution entities from the request.
     *
     * @param request batch request
     * @return list of execution entities
     */
    private List<JobExecutionEntity> loadExecutions(JobExecutorBatchRequest request) {
        if (request == null || request.getJobs() == null || request.getJobs().isEmpty()) {
            throw new IllegalArgumentException("Job request must not be empty");
        }

        List<JobExecutionEntity> executions = new ArrayList<>();

        for (JobExecutorRequest executionRequest : request.getJobs()) {
            Long executionId = executionRequest.getExecutionId();

            if (executionId == null) {
                throw new IllegalArgumentException("Execution id must not be null");
            }

            logger.info("Loading execution id={}", executionId);

            JobExecutionEntity execution = jobExecutionRepository.findById(executionId)
                    .orElseThrow(() -> new IllegalArgumentException("Execution not found for id: " + executionId));

            executions.add(execution);
        }

        return executions;
    }

    /**
     * Executes each job execution.
     *
     * @param executions list of job execution entities
     */
    private void executeJobs(List<JobExecutionEntity> executions) {
        for (JobExecutionEntity execution : executions) {
            Long executionId = execution.getExecutionId();
            Long auditId = execution.getAuditId();

            try {
                String jobName = execution.getJobName();
                Long start = execution.getNinRangeStart();
                Long end = execution.getNinRangeEnd();

                jobAuditService.updateStatus(auditId, ExecutionStatus.PROCESSING);

                logger.info("Executing executionId={}, auditId={}, jobName={}, start={}, end={}",
                        executionId, auditId, jobName, start, end);

                beneficiaryBatchProcessorService.processByRange(jobName, start, end);

                jobAuditService.updateStatus(auditId, ExecutionStatus.SUCCEEDED);

            } catch (Exception exception) {
                if (auditId != null) {
                    jobAuditService.updateStatus(auditId, ExecutionStatus.FAILED);
                }

                logger.error("Failed executionId={}, auditId={}", executionId, auditId, exception);
                throw exception;
            }
        }
    }

    /**
     * Runs a full job using only the job id.
     *
     * @param jobId job identifier
     */
    public void runFullJobByJobId(Long jobId) {

        beanJob.markJobStarted();

        try {
            if (jobId == null) {
                throw new IllegalArgumentException("Job id must not be null");
            }

            JobDetailsEntity jobDetails = jobDetailsService.getJobRequired(jobId);
            String jobName = jobDetails.getJobName();

            if (jobName == null || jobName.trim().isEmpty()) {
                throw new IllegalArgumentException("Job name is missing for job id: " + jobId);
            }

            logger.info("Executing full job for jobId={}, jobName={}", jobId, jobName);

            beneficiaryBatchProcessorService.processAllBeneficiaries(jobName);

        } catch (Exception exception) {
            logger.error("Failed full job for jobId={}", jobId, exception);
            throw exception;

        } finally {
            beanJob.markJobFinished();
        }
    }
}