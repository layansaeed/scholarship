package com.example.beans.service.job;

import com.example.beans.model.JobDetailsEntity;
import com.example.beans.repository.JobDetailsJpaRepository;
import org.springframework.stereotype.Service;

@Service
public class JobDetailsService {

    private final JobDetailsJpaRepository jobDetailsRepository;

    /**
     * Creates the service with the required repository dependency.
     *
     * @param jobDetailsRepository repository for job details
     */
    public JobDetailsService(JobDetailsJpaRepository jobDetailsRepository) {
        this.jobDetailsRepository = jobDetailsRepository;
    }

    /**
     * Returns the job details for the given job id.
     *
     * @param jobId job identifier
     * @return job details entity
     */
    public JobDetailsEntity getJobRequired(Long jobId) {
        return jobDetailsRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job not found for id: " + jobId));
    }

}