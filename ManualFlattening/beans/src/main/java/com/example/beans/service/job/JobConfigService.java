package com.example.beans.service.job;

import com.example.beans.model.JobConfigEntity;
import com.example.beans.repository.JobConfigJpaRepository;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class JobConfigService {

    private final JobConfigJpaRepository jobConfigRepository;

    public JobConfigService(JobConfigJpaRepository jobConfigRepository) {
        this.jobConfigRepository = jobConfigRepository;
    }

    public Map<String, String> getConfigMap(Long jobId) {
        List<JobConfigEntity> rows = jobConfigRepository.findByIdJobId(jobId);

        if (rows.isEmpty()) {
            throw new RuntimeException("No job config found for jobId: " + jobId);
        }

        Map<String, String> config = new LinkedHashMap<>();

        for (JobConfigEntity row : rows) {
            String key = row.getConfigKey();

            if (key == null || key.trim().isEmpty()) {
                throw new RuntimeException("Found blank config key for jobId: " + jobId);
            }

            if (config.containsKey(key)) {
                throw new RuntimeException("Duplicate config key '" + key + "' for jobId: " + jobId);
            }

            config.put(key, row.getConfigValue());
        }

        return config;
    }
}