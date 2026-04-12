//package com.example.beans.service.integration;
//
//import com.example.beans.constant.DynamicJobApiConstants;
//import com.example.beans.service.job.JobConfigService;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.http.HttpEntity;
//import org.springframework.http.HttpHeaders;
//import org.springframework.http.HttpMethod;
//import org.springframework.http.MediaType;
//import org.springframework.http.ResponseEntity;
//import org.springframework.stereotype.Service;
//import org.springframework.web.client.RestTemplate;
//
//import java.util.Collections;
//import java.util.LinkedHashMap;
//import java.util.Map;
//
//@Service
//public class DynamicApiService {
//
//    private final Logger log = LoggerFactory.getLogger(this.getClass());
//    private final RestTemplate restTemplate;
//    private final JobConfigService jobConfigService;
//
//    public DynamicApiService(RestTemplate restTemplate, JobConfigService jobConfigService) {
//        this.restTemplate = restTemplate;
//        this.jobConfigService = jobConfigService;
//    }
//
//    /**
//     * Calls the target API dynamically using config loaded by job name prefix.
//     *
//     * Example:
//     * jobName = HRSD_DIS_ASS
//     * nin = 1234567890
//     *
//     * Config source keys in DB:
//     * - HRSD_DIS_ASS.url
//     * - HRSD_DIS_ASS.httpMethod
//     * - HRSD_DIS_ASS.mediaType
//     * - HRSD_DIS_ASS.auth.url
//     */
//    public Map<String, Object> callApi(String jobName, Long nin) {
//        Map<String, String> config = jobConfigService.getConfigMap(jobName);
//
//        String bearerToken = resolveBearerTokenIfRequired(config);
//
//        String requestUrl = getRequiredConfigValue(config, DynamicJobApiConstants.CONFIG_URL);
//        HttpMethod requestMethod = getConfiguredHttpMethod(
//                config,
//                DynamicJobApiConstants.CONFIG_HTTP_METHOD,
//                DynamicJobApiConstants.DEFAULT_HTTP_METHOD
//        );
//        MediaType requestMediaType = getConfiguredMediaType(
//                config,
//                DynamicJobApiConstants.CONFIG_MEDIA_TYPE,
//                DynamicJobApiConstants.DEFAULT_ACCEPT_MEDIA_TYPE
//        );
//
//        HttpHeaders requestHeaders = buildHeadersFromConfig(
//                config,
//                DynamicJobApiConstants.PREFIX_HEADER
//        );
//        requestHeaders.setContentType(requestMediaType);
//        requestHeaders.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
//
//        if (isBearerAuthorizationEnabled(config) && bearerToken != null && !bearerToken.trim().isEmpty()) {
//            requestHeaders.setBearerAuth(bearerToken);
//        }
//
//        Map<String, Object> requestBody = buildRequestBody(nin, config);
//
//        HttpEntity<Map<String, Object>> requestEntity =
//                new HttpEntity<Map<String, Object>>(requestBody, requestHeaders);
//
//        log.info("Calling dynamic API for jobName={} url={} nin={}", jobName, requestUrl, nin);
//
//        ResponseEntity<Map> response =
//                restTemplate.exchange(requestUrl, requestMethod, requestEntity, Map.class);
//
//        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
//            throw new RuntimeException(
//                    "Dynamic API failed for jobName=" + jobName + " status=" + response.getStatusCode()
//            );
//        }
//
//        return response.getBody();
//    }
//
//    /**
//     * Calls auth API only when auth.url exists.
//     * If auth.url is missing, returns null.
//     */
//    private String resolveBearerTokenIfRequired(Map<String, String> config) {
//        String authUrl = config.get(DynamicJobApiConstants.CONFIG_AUTH_URL);
//
//        if (authUrl == null || authUrl.trim().isEmpty()) {
//            return null;
//        }
//
//        HttpMethod authMethod = getConfiguredHttpMethod(
//                config,
//                DynamicJobApiConstants.CONFIG_AUTH_HTTP_METHOD,
//                DynamicJobApiConstants.DEFAULT_HTTP_METHOD
//        );
//        MediaType authMediaType = getConfiguredMediaType(
//                config,
//                DynamicJobApiConstants.CONFIG_AUTH_MEDIA_TYPE,
//                DynamicJobApiConstants.DEFAULT_ACCEPT_MEDIA_TYPE
//        );
//
//        HttpHeaders authHeaders = buildHeadersFromConfig(
//                config,
//                DynamicJobApiConstants.PREFIX_AUTH_HEADER
//        );
//        authHeaders.setContentType(authMediaType);
//        authHeaders.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
//
//        if (!authHeaders.containsKey(HttpHeaders.AUTHORIZATION)) {
//            String basicAuthorization = getOptionalConfigValue(
//                    config,
//                    DynamicJobApiConstants.PREFIX_AUTH_HEADER + HttpHeaders.AUTHORIZATION,
//                    DynamicJobApiConstants.PREFIX_AUTH_HEADER + HttpHeaders.AUTHORIZATION.toLowerCase(),
//                    DynamicJobApiConstants.PREFIX_HEADER + HttpHeaders.AUTHORIZATION,
//                    DynamicJobApiConstants.PREFIX_HEADER + HttpHeaders.AUTHORIZATION.toLowerCase()
//            );
//
//            if (basicAuthorization != null) {
//                authHeaders.set(HttpHeaders.AUTHORIZATION, basicAuthorization);
//            }
//        }
//
//        Map<String, Object> authRequestBody = buildMapFromConfigPrefix(
//                config,
//                DynamicJobApiConstants.PREFIX_AUTH_BODY
//        );
//
//        HttpEntity<Map<String, Object>> authRequestEntity =
//                new HttpEntity<Map<String, Object>>(authRequestBody, authHeaders);
//
//        log.info("Calling auth API url={}", authUrl);
//
//        ResponseEntity<Map> authResponse =
//                restTemplate.exchange(authUrl, authMethod, authRequestEntity, Map.class);
//
//        if (!authResponse.getStatusCode().is2xxSuccessful() || authResponse.getBody() == null) {
//            throw new RuntimeException("Auth API failed. status=" + authResponse.getStatusCode());
//        }
//
//        return extractAccessToken(authResponse.getBody());
//    }
//
//    /**
//     * Builds headers from config keys like:
//     * header.X-Test = 123
//     */
//    private HttpHeaders buildHeadersFromConfig(Map<String, String> config, String prefix) {
//        HttpHeaders headers = new HttpHeaders();
//
//        for (Map.Entry<String, String> entry : config.entrySet()) {
//            String key = entry.getKey();
//
//            if (key.startsWith(prefix)) {
//                String headerName = key.substring(prefix.length());
//                headers.set(headerName, entry.getValue());
//            }
//        }
//
//        return headers;
//    }
//
//    /**
//     * Builds the main request body.
//     * Always includes NIN, then adds any body.* values.
//     */
//    private Map<String, Object> buildRequestBody(Long nin, Map<String, String> config) {
//        Map<String, Object> requestBody = new LinkedHashMap<String, Object>();
//        requestBody.put("nin", nin);
//
//        requestBody.putAll(buildMapFromConfigPrefix(config, DynamicJobApiConstants.PREFIX_BODY));
//
//        return requestBody;
//    }
//
//    /**
//     * Converts config entries with a certain prefix into a map.
//     *
//     * Example:
//     * auth.body.username = admin
//     * auth.body.password = 1234
//     *
//     * Result:
//     * username -> admin
//     * password -> 1234
//     */
//    private Map<String, Object> buildMapFromConfigPrefix(Map<String, String> config, String prefix) {
//        Map<String, Object> values = new LinkedHashMap<String, Object>();
//
//        for (Map.Entry<String, String> entry : config.entrySet()) {
//            String key = entry.getKey();
//
//            if (key.startsWith(prefix)) {
//                String fieldName = key.substring(prefix.length());
//                values.put(fieldName, entry.getValue());
//            }
//        }
//
//        return values;
//    }
//
//    /**
//     * Tries common token field names.
//     */
//    private String extractAccessToken(Map responseBody) {
//        Object token = responseBody.get(DynamicJobApiConstants.TOKEN_ACCESS);
//
//        if (token == null) {
//            token = responseBody.get(DynamicJobApiConstants.TOKEN_SIMPLE);
//        }
//
//        if (token == null) {
//            token = responseBody.get(DynamicJobApiConstants.TOKEN_JWT);
//        }
//
//        if (token == null) {
//            throw new RuntimeException("Could not find token in auth response");
//        }
//
//        return String.valueOf(token);
//    }
//
//    private HttpMethod getConfiguredHttpMethod(
//            Map<String, String> config,
//            String configKey,
//            String defaultValue
//    ) {
//        String methodValue = config.get(configKey);
//
//        if (methodValue == null || methodValue.trim().isEmpty()) {
//            methodValue = defaultValue;
//        }
//
//        return HttpMethod.valueOf(methodValue);
//    }
//
//    private MediaType getConfiguredMediaType(
//            Map<String, String> config,
//            String configKey,
//            String defaultValue
//    ) {
//        String mediaTypeValue = config.get(configKey);
//
//        if (mediaTypeValue == null || mediaTypeValue.trim().isEmpty()) {
//            mediaTypeValue = defaultValue;
//        }
//
//        return MediaType.parseMediaType(mediaTypeValue);
//    }
//
//    private boolean isBearerAuthorizationEnabled(Map<String, String> config) {
//        String authorizationType = config.get(DynamicJobApiConstants.CONFIG_AUTHORIZATION);
//
//        return DynamicJobApiConstants.AUTH_TYPE_BEARER.equalsIgnoreCase(authorizationType);
//    }
//
//    private String getRequiredConfigValue(Map<String, String> config, String configKey) {
//        String value = config.get(configKey);
//
//        if (value == null || value.trim().isEmpty()) {
//            throw new RuntimeException("Missing required config key: " + configKey);
//        }
//
//        return value;
//    }
//
//    private String getOptionalConfigValue(Map<String, String> config, String... keys) {
//        for (String key : keys) {
//            String value = config.get(key);
//            if (value != null && !value.trim().isEmpty()) {
//                return value;
//            }
//        }
//        return null;
//    }
//}