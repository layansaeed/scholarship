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
     * Calls the main API for one NIN using configuration loaded from JOB_CONFIG.
     *
     * Flow:
     * 1. Load config by job id.
     * 2. Get bearer token first if auth config exists.
     * 3. Read main URL, method, and media type from config.
     * 4. Build headers and request body.
     * 5. Call the external API.
     * 6. Return the response body as a map.
     *
     * Example:
     * jobId = 4
     * nin = 1234567890
     *
     * Config may contain:
     * - url = http://localhost:3000/hrsd-dis-serv
     * - httpMethod = POST
     * - mediaType = application/json
     * - authorization = Bearer
     * - auth.url = http://localhost:3000/auth/hrsd-dis-serv
     */
    public Map<String, Object> callApi(Long jobId, Long nin) {
        Map<String, String> config = jobConfigService.getConfigMap(jobId);

        String bearerToken = resolveBearerTokenIfRequired(config);

        String requestUrl = getRequiredConfigValue(config, DynamicJobApiConstants.CONFIG_URL);
        HttpMethod requestMethod = getConfiguredHttpMethod(
                config,
                DynamicJobApiConstants.CONFIG_HTTP_METHOD,
                DynamicJobApiConstants.DEFAULT_HTTP_METHOD
        );
        MediaType requestMediaType = getConfiguredMediaType(
                config,
                DynamicJobApiConstants.CONFIG_MEDIA_TYPE,
                DynamicJobApiConstants.DEFAULT_MEDIA_TYPE
        );

        HttpHeaders requestHeaders = buildHeadersFromConfig(
                config,
                DynamicJobApiConstants.PREFIX_HEADER
        );
        requestHeaders.setContentType(requestMediaType);
        requestHeaders.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        if (isBearerAuthorizationEnabled(config) && bearerToken != null && !bearerToken.trim().isEmpty()) {
            requestHeaders.setBearerAuth(bearerToken);
        }

        Map<String, Object> requestBody = buildRequestBody(nin, config);

        HttpEntity<Map<String, Object>> requestEntity =
                new HttpEntity<Map<String, Object>>(requestBody, requestHeaders);

        log.info("Calling dynamic API for jobId={} url={} nin={}", jobId, requestUrl, nin);

        ResponseEntity<Map> response =
                restTemplate.exchange(requestUrl, requestMethod, requestEntity, Map.class);

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new RuntimeException(
                    "Dynamic API failed for jobId=" + jobId + " status=" + response.getStatusCode()
            );
        }

        return response.getBody();
    }

    /**
     * Calls the auth API only if auth.url exists in config.
     *
     * If auth.url is missing or blank, this method returns null,
     * which means the main API does not need an auth call.
     *
     * Example:
     * Config contains:
     * - auth.url = http://localhost:3000/auth/hrsd-dis-serv
     * - auth.httpMethod = POST
     * - auth.mediaType = application/json
     * - auth.body.username = myUser
     * - auth.body.password = myPass
     *
     * Result:
     * - calls auth API
     * - extracts token from auth response
     * - returns token string
     */
    private String resolveBearerTokenIfRequired(Map<String, String> config) {
        String authUrl = config.get(DynamicJobApiConstants.CONFIG_AUTH_URL);

        if (authUrl == null || authUrl.trim().isEmpty()) {
            return null;
        }

        HttpMethod authMethod = getConfiguredHttpMethod(
                config,
                DynamicJobApiConstants.CONFIG_AUTH_HTTP_METHOD,
                DynamicJobApiConstants.DEFAULT_HTTP_METHOD
        );
        MediaType authMediaType = getConfiguredMediaType(
                config,
                DynamicJobApiConstants.CONFIG_AUTH_MEDIA_TYPE,
                DynamicJobApiConstants.DEFAULT_MEDIA_TYPE
        );

        HttpHeaders authHeaders = buildHeadersFromConfig(
                config,
                DynamicJobApiConstants.PREFIX_AUTH_HEADER
        );
        authHeaders.setContentType(authMediaType);
        authHeaders.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        if (!authHeaders.containsKey(HttpHeaders.AUTHORIZATION)) {
            String basicAuthorization = config.get(
                    DynamicJobApiConstants.PREFIX_HEADER + HttpHeaders.AUTHORIZATION
            );

            if (basicAuthorization != null && !basicAuthorization.trim().isEmpty()) {
                authHeaders.set(HttpHeaders.AUTHORIZATION, basicAuthorization);
            }
        }

        Map<String, Object> authRequestBody = buildMapFromConfigPrefix(
                config,
                DynamicJobApiConstants.PREFIX_AUTH_BODY
        );

        HttpEntity<Map<String, Object>> authRequestEntity =
                new HttpEntity<Map<String, Object>>(authRequestBody, authHeaders);

        log.info("Calling auth API url={}", authUrl);

        ResponseEntity<Map> authResponse =
                restTemplate.exchange(authUrl, authMethod, authRequestEntity, Map.class);

        if (!authResponse.getStatusCode().is2xxSuccessful() || authResponse.getBody() == null) {
            throw new RuntimeException("Auth API failed. status=" + authResponse.getStatusCode());
        }

        return extractAccessToken(authResponse.getBody());
    }

    /**
     * Builds HttpHeaders from config entries that start with a prefix.
     *
     * Example:
     * prefix = "header."
     * config contains:
     * - header.X-HRSD-Client-Id = 123
     * - header.X-HRSD-Client-Secret = abc
     *
     * Result headers:
     * - X-HRSD-Client-Id: 123
     * - X-HRSD-Client-Secret: abc
     */
    private HttpHeaders buildHeadersFromConfig(Map<String, String> config, String prefix) {
        HttpHeaders headers = new HttpHeaders();

        for (Map.Entry<String, String> entry : config.entrySet()) {
            String key = entry.getKey();

            if (key.startsWith(prefix)) {
                String headerName = key.substring(prefix.length()); //remove header
                headers.set(headerName, entry.getValue());
            }
        }

        return headers;
    }

    /**
     * Builds the main request body for the target API.
     * This method always adds the current NIN,
     * then adds all config entries that start with "body.".
     * Example:
     * nin = 1234567890
     * config contains:
     * - body.sourceSystem = NRD
     * - body.requestType = full
     *
     * Result:
     * {
     *   "nin": 1234567890,
     *   "sourceSystem": "NRD",
     *   "requestType": "full"
     * }
     */
    private Map<String, Object> buildRequestBody(Long nin, Map<String, String> config) {
        Map<String, Object> requestBody = new LinkedHashMap<String, Object>();
        requestBody.put("nin", nin);

        requestBody.putAll(buildMapFromConfigPrefix(config, DynamicJobApiConstants.PREFIX_BODY));

        return requestBody;
    }

    /**
     * Creates a map from config entries that start with a given prefix.
     *
     * Example:
     * prefix = "auth.body."
     * config contains:
     * - auth.body.username = admin
     * - auth.body.password = 1234
     *
     * Result:
     * {
     *   "username": "admin",
     *   "password": "1234"
     * }
     */
    private Map<String, Object> buildMapFromConfigPrefix(Map<String, String> config, String prefix) {
        Map<String, Object> values = new LinkedHashMap<String, Object>();

        for (Map.Entry<String, String> entry : config.entrySet()) {
            String key = entry.getKey();

            if (key.startsWith(prefix)) {
                String fieldName = key.substring(prefix.length());
                values.put(fieldName, entry.getValue());
            }
        }

        return values;
    }

    /**
     * Extracts the access token from the auth API response.
     *
     * It tries common token field names in this order:
     * 1. access_token
     * 2. token
     * 3. jwt
     *
     * Example auth response:
     * {
     *   "access_token": "abc123"
     * }
     *
     * Result:
     * "abc123"
     */
    private String extractAccessToken(Map responseBody) {
        Object token = responseBody.get(DynamicJobApiConstants.TOKEN_ACCESS);

        if (token == null) {
            token = responseBody.get(DynamicJobApiConstants.TOKEN_SIMPLE);
        }

        if (token == null) {
            token = responseBody.get(DynamicJobApiConstants.TOKEN_JWT);
        }

        if (token == null) {
            throw new RuntimeException("Could not find token in auth response");
        }

        return String.valueOf(token);
    }

    /**
     * Reads the HTTP method from config.
     * If the value is missing or blank, it returns the default value.
     *
     * Example:
     * configKey = "httpMethod"
     * config value = "POST"
     *
     * Result:
     * HttpMethod.POST
     */
    private HttpMethod getConfiguredHttpMethod(
            Map<String, String> config,
            String configKey,
            String defaultValue
    ) {
        String methodValue = config.get(configKey);

        if (methodValue == null || methodValue.trim().isEmpty()) {
            methodValue = defaultValue;
        }

        return HttpMethod.valueOf(methodValue);
    }

    /**
     * Reads the media type from config.
     * If the value is missing or blank, it returns the default value.
     *
     * Example:
     * configKey = "mediaType"
     * config value = "application/json"
     *
     * Result:
     * MediaType.APPLICATION_JSON
     */
    private MediaType getConfiguredMediaType(
            Map<String, String> config,
            String configKey,
            String defaultValue
    ) {
        String mediaTypeValue = config.get(configKey);

        if (mediaTypeValue == null || mediaTypeValue.trim().isEmpty()) {
            mediaTypeValue = defaultValue;
        }

        return MediaType.parseMediaType(mediaTypeValue);
    }

    /**
     * Checks whether the main API should use Bearer authorization.
     *
     * Example:
     * config contains:
     * - authorization = Bearer
     *
     * Result:
     * true
     */
    private boolean isBearerAuthorizationEnabled(Map<String, String> config) {
        String authorizationType = config.get(DynamicJobApiConstants.CONFIG_AUTHORIZATION);

        return DynamicJobApiConstants.AUTH_TYPE_BEARER.equalsIgnoreCase(authorizationType);
    }

    /**
     * Returns a required config value.
     * If the key is missing or blank, it throws an exception.
     * Example:
     * configKey = "url"
     * If config contains:
     * - url = http://localhost:3000/hrsd-dis-serv
     * Result:
     * returns the URL string
     */
    private String getRequiredConfigValue(Map<String, String> config, String configKey) {
        String value = config.get(configKey);
        //instead of entry level in DB-> config as map (key(bean name).value(url))
        //each field should have before beanName.field -> //bean name in config file
        //no entity jpa so no id so used PropertySourcesPlaceholderConfigurer
        if (value == null || value.trim().isEmpty()) {
            throw new RuntimeException("Missing required config key: " + configKey);
        }

        return value;
    }
}