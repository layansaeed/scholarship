package com.example.beans.service.job;

import com.example.beans.config.Config;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class JobConfigService {

    private final Config.PropertySourceConfig propertySourceConfig;

    public JobConfigService(Config.PropertySourceConfig propertySourceConfig) {
        this.propertySourceConfig = propertySourceConfig;
    }

    /**
     * Returns one normalized config map for a job name.
     *
     * Input properties in Environment:
     * - HRSD_DIS_ASS.url
     * - HRSD_DIS_ASS.httpMethod
     * - HRSD_DIS_ASS.auth.url
     *
     * Output map:
     * - url -> ...
     * - httpMethod -> ...
     * - auth.url -> ...
     */
    public Map<String, String> getConfigMap(String jobName) {
        if (jobName == null || jobName.trim().isEmpty()) {
            throw new RuntimeException("Job name must not be null or blank");
        }

        String prefix = jobName + ".";
        Map<String, String> rawProperties = propertySourceConfig.getPropertiesStartingWith(prefix);

        if (rawProperties.isEmpty()) {
            throw new RuntimeException("No configuration found for jobName: " + jobName);
        }

        Map<String, String> normalizedConfig = new LinkedHashMap<String, String>();

        for (Map.Entry<String, String> entry : rawProperties.entrySet()) {
            String fullKey = entry.getKey();
            String normalizedKey = fullKey.substring(prefix.length());

            if (normalizedKey.trim().isEmpty()) {
                continue;
            }

            if (normalizedConfig.containsKey(normalizedKey)) {
                throw new RuntimeException("Duplicate normalized config key '" + normalizedKey
                        + "' for jobName: " + jobName);
            }

            normalizedConfig.put(normalizedKey, entry.getValue());
        }

        return normalizedConfig;
    }
}