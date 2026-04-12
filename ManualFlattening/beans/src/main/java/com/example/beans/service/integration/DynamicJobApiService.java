package com.example.beans.service.integration;

import com.example.beans.constant.DynamicJobApiConstants;
import com.example.beans.service.job.JobConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class DynamicJobApiService {

    private final Logger log = LoggerFactory.getLogger(this.getClass());
    private final RestTemplate restTemplate;
    private final JobConfigService jobConfigService;

    public DynamicJobApiService(RestTemplate restTemplate, JobConfigService jobConfigService) {
        this.restTemplate = restTemplate;
        this.jobConfigService = jobConfigService;
    }

    /**
     * Calls the external API for one NIN.
     *
     * Current supported config keys:
     * - <jobName>.url
     * - <jobName>.httpMethod
     * - <jobName>.mediaType
     *
     * Current request body:
     * {
     *   "nin": 1234567890
     * }
     *
     * ask JobConfigService for config of HRSD_DIS_ASS
     */
    public Map<String, Object> callApi(String jobName, Long nin) {
        //return clean map without prefix
        Map<String, String> config = jobConfigService.getConfigMap(jobName);

        String url = getRequiredValue(config, DynamicJobApiConstants.CONFIG_URL);
        String httpMethodValue = getRequiredValue(config, DynamicJobApiConstants.CONFIG_HTTP_METHOD);
        String mediaTypeValue = getRequiredValue(config, DynamicJobApiConstants.CONFIG_MEDIA_TYPE);

        HttpMethod httpMethod = HttpMethod.valueOf(httpMethodValue);
        MediaType mediaType = MediaType.parseMediaType(mediaTypeValue);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(mediaType);
        headers.setAccept(
                Collections.singletonList(
                        MediaType.parseMediaType(DynamicJobApiConstants.DEFAULT_ACCEPT_MEDIA_TYPE)
                )
        );
        //LinkedHashMap->preserves insertion order. Also, it is not strictly required use it here, but it gives stable ordering.
        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("nin", nin);

        HttpEntity<Map<String, Object>> requestEntity =
                new HttpEntity<>(requestBody, headers);

        log.info("Calling API for jobName={} url={} nin={}", jobName, url, nin);

        ResponseEntity<Map> response =
                restTemplate.exchange(url, httpMethod, requestEntity, Map.class);

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new RuntimeException(
                    "API call failed for jobName=" + jobName + " status=" + response.getStatusCode()
            );
        }

        return response.getBody();
    }

    /**
     * Returns a required config value.
     * Throws exception if the key is missing or blank.
     */
    private String getRequiredValue(Map<String, String> config, String key) {
        String value = config.get(key);

        if (value == null || value.trim().isEmpty()) {
            throw new RuntimeException("Missing required config key: " + key);
        }

        return value;
    }
}