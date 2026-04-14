package com.example.beans.service.job;

import com.example.beans.model.JobDetailsEntity;
import com.example.beans.repository.JobDetailsJpaRepository;
import org.springframework.stereotype.Service;

@Service
public class JobDetailsService {

    private final JobDetailsJpaRepository jobDetailsJpaRepository;

    public JobDetailsService(JobDetailsJpaRepository jobDetailsJpaRepository) {
        this.jobDetailsJpaRepository = jobDetailsJpaRepository;
    }

    public JobDetailsEntity getJobRequired(Long jobId) {
        return jobDetailsJpaRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found for id: " + jobId));
    }

    public String getJobNameRequired(Long jobId) {
        JobDetailsEntity job = getJobRequired(jobId);

        if (job.getJobName() == null || job.getJobName().trim().isEmpty()) {
            throw new RuntimeException("Job name is missing for job id: " + jobId);
        }

        return job.getJobName();
    }
}