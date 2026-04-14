package com.example.beans.service.integration;

import com.example.beans.constant.DynamicCallConstants;
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
public class DynamicCallService {

    private final Logger log = LoggerFactory.getLogger(this.getClass());
    private final RestTemplate restTemplate;
    private final JobConfigService jobConfigService;

    public DynamicCallService(RestTemplate restTemplate, JobConfigService jobConfigService) {
        this.restTemplate = restTemplate;
        this.jobConfigService = jobConfigService;
    }

    /**
     * Main entry point for calling external API for one job and one NIN.
     * Loads config, prepares request parts, executes call, and returns response body.
     */
    public Map<String, Object> callApi(String jobName, Long nin) {
        Map<String, String> config = jobConfigService.getConfigMap(jobName);

        String url = buildUrl(config, jobName, nin);
        HttpMethod httpMethod = buildHttpMethod(config);
        HttpHeaders headers = buildHeaders(config);
        Map<String, Object> requestBody = buildRequestBody(jobName, nin);
        HttpEntity<Map<String, Object>> requestEntity = buildRequestEntity(headers, requestBody);

        ResponseEntity<Map> response = executeRequest(jobName, nin, url, httpMethod, requestEntity);

        return handleResponse(jobName, nin, response);
    }

    /**
     * Builds request URL.
     * Future changes for path variables or query params should be done here.
     */
    private String buildUrl(Map<String, String> config, String jobName, Long nin) {
        String baseUrl = getRequiredConfigValue(config, DynamicCallConstants.CONFIG_URL);

        // Future example:
        // return baseUrl + "/" + nin;
        // or append query params here if needed

        return baseUrl;
    }

    /**
     * Builds HTTP method from config.
     */
    private HttpMethod buildHttpMethod(Map<String, String> config) {
        String httpMethodValue = getRequiredConfigValue(config, DynamicCallConstants.CONFIG_HTTP_METHOD);
        return HttpMethod.valueOf(httpMethodValue);
    }

    /**
     * Builds request headers.
     * Future header changes should be done here.
     */
    private HttpHeaders buildHeaders(Map<String, String> config) {
        String mediaTypeValue = getRequiredConfigValue(config, DynamicCallConstants.CONFIG_MEDIA_TYPE);
        String clientId = getOptionalConfigValue(config, DynamicCallConstants.CONFIG_CLIENT_ID);
        String clientSecret = getOptionalConfigValue(config, DynamicCallConstants.CONFIG_CLIENT_SECRET);

        MediaType mediaType = MediaType.parseMediaType(mediaTypeValue);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(mediaType);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        if (clientId != null && !clientId.isEmpty()) {
            headers.set("X-HRSD-Client-Id", clientId);
        }

        if (clientSecret != null && !clientSecret.isEmpty()) {
            headers.set("X-HRSD-Client-Secret", clientSecret);
        }
//
//        log.info("Prepared headers for clientId={} and clientSecret exists={}",
//                clientId, clientSecret != null);


        log.info("Prepared headers for clientId={} and clientSecret exists={}",
                clientId, clientSecret);

        return headers;
    }

   // private HttpHeaders buildHeaders(Map<String, String> config) {
//        String mediaTypeValue = getRequiredConfigValue(config, DynamicCallConstants.CONFIG_MEDIA_TYPE);
//        String clientId = getRequiredConfigValue(config, DynamicCallConstants.CONFIG_CLIENT_ID);
//        String clientSecret = getRequiredConfigValue(config, DynamicCallConstants.CONFIG_CLIENT_SECRET);
//
//        MediaType mediaType = MediaType.parseMediaType(mediaTypeValue);
//
//        HttpHeaders headers = new HttpHeaders();
//        headers.setContentType(mediaType);
//        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
//        headers.set("X-HRSD-Client-Id", clientId);
//        headers.set("X-HRSD-Client-Secret", clientSecret);
//
//        return headers;
//    }

    /**
     * Builds request body.
     * Future body changes for different services should be done here.
     */
    private Map<String, Object> buildRequestBody(String jobName, Long nin) {
        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("nin", nin);

        // Future examples:
//        if ("HRSD_DIS_ASS".equals(jobName)) {
//            requestBody.put("nin", nin);
//        } else if ("ANOTHER_SERVICE".equals(jobName)) {
//            requestBody.put("nationalId", nin);
//            requestBody.put("sourceSystem", "NRD");
//        }
        return requestBody;
    }

    /**
     * Builds HTTP entity from headers and body.
     */
    private HttpEntity<Map<String, Object>> buildRequestEntity(HttpHeaders headers,
                                                               Map<String, Object> requestBody) {
        return new HttpEntity<>(requestBody, headers);
    }

    /**
     * Executes the external API request.
     */
    private ResponseEntity<Map> executeRequest(String jobName,
                                               Long nin,
                                               String url,
                                               HttpMethod httpMethod,
                                               HttpEntity<Map<String, Object>> requestEntity) {
        try {
            log.info("Calling API for jobName={} url={} nin={}", jobName, url, nin);
            return restTemplate.exchange(url, httpMethod, requestEntity, Map.class);
        } catch (Exception e) {
            log.warn("External API call failed for jobName={} nin={} error={}",
                    jobName, nin, e.getMessage());
            return ResponseEntity.internalServerError().body(new LinkedHashMap<String, Object>());
        }
    }

    /**
     * Handles API response.
     * If response is not successful or body is null, logs warning and returns empty map.
     * This keeps the full processing flow running.
     */
    private Map<String, Object> handleResponse(String jobName, Long nin, ResponseEntity<Map> response) {
        if (!response.getStatusCode().is2xxSuccessful()) {
            log.warn("API call returned non-success status for jobName={} nin={} status={}",
                    jobName, nin, response.getStatusCode());
            return new LinkedHashMap<>();
        }

        if (response.getBody() == null) {
            log.warn("API call returned null body for jobName={} nin={}", jobName, nin);
            return new LinkedHashMap<>();
        }

        return response.getBody();
    }

    /**
     * Returns required config value.
     * Throws exception if value is missing or blank.
     */
    private String getRequiredConfigValue(Map<String, String> config, String key) {
        String value = config.get(key);

        if (value == null || value.trim().isEmpty()) {
            throw new RuntimeException("Missing required config key: " + key);
        }

        return value;
    }

    private String getOptionalConfigValue(Map<String, String> config, String key) {
        String value = config.get(key);
        return value == null ? null : value.trim();
    }
}