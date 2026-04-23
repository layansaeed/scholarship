package sa.nrd.job.execute.service.integration;

import sa.nrd.job.execute.constant.DynamicCallConstants;
import sa.nrd.job.execute.service.job.JobConfigService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class DynamicCallService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());

    private final RestTemplate restTemplate;
    private final JobConfigService jobConfigService;

    public DynamicCallService(RestTemplate restTemplate, JobConfigService jobConfigService) {
        this.restTemplate = restTemplate;
        this.jobConfigService = jobConfigService;
    }

    /**
     * Calls the external API for the given job and NIN.
     * Always returns one map ready for storing.
     */
    public Map<String, Object> callApi(String jobName, Long nin) {
        Map<String, String> config = jobConfigService.getConfigMap(jobName);

        try {
            String url = buildUrl(config);
            HttpMethod httpMethod = buildHttpMethod(config);
            HttpHeaders headers = buildHeaders(config);
            Map<String, Object> requestBody = buildRequestBody(nin);
            HttpEntity<Map<String, Object>> requestEntity = buildRequestEntity(headers, requestBody);

            ResponseEntity<Map> restResponse =
                    executeRequest(jobName, nin, url, httpMethod, requestEntity);

            logger.debug("Successfully retrieved response for jobName={} nin={}", jobName, nin);

            return prepareSuccessResponse(restResponse);

        } catch (Exception e) {
            logger.debug("Inside Exception for jobName [{}] nin [{}] Exception is [{}]",
                    jobName, nin, e.getMessage());

            return prepareErrorResponse(e);
        }
    }

    /**
     * Builds the request URL from configuration.
     */
    private String buildUrl(Map<String, String> config) {
        return getRequiredConfigValue(config, DynamicCallConstants.CONFIG_URL);
    }

    /**
     * Builds the HTTP method from configuration.
     */
    private HttpMethod buildHttpMethod(Map<String, String> config) {
        String httpMethodValue = getRequiredConfigValue(config, DynamicCallConstants.CONFIG_HTTP_METHOD);
        return HttpMethod.valueOf(httpMethodValue);
    }

    /**
     * Builds the request headers from configuration.
     */
    private HttpHeaders buildHeaders(Map<String, String> config) {
        String requiredConfigValue = getRequiredConfigValue(config, DynamicCallConstants.CONFIG_MEDIA_TYPE);
        //MediaType mediaType = MediaType.parseMediaType(requiredConfigValue);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        addConfiguredHeaders(headers, config);
        return headers;
    }

    /**
     * Adds all configured headers that start with the headers prefix.
     */
    private void addConfiguredHeaders(HttpHeaders headers, Map<String, String> config) {
        String headerPrefix = DynamicCallConstants.CONFIG_HEADERS_PREFIX;

        //loop all clean map
        for (Map.Entry<String, String> entry : config.entrySet()) {
            String configKey = entry.getKey();

            if (!configKey.startsWith(headerPrefix)) {
                continue;
            }

            String headerName = configKey.substring(headerPrefix.length());
            String headerValue = entry.getValue();

            if (headerName.trim().isEmpty()) {
                throw new IllegalArgumentException("Header name must not be blank for config key: " + configKey);
            }

            if (headerValue == null || headerValue.trim().isEmpty()) {
                throw new IllegalArgumentException("Header value must not be blank for config key: " + configKey);
            }

            headers.set(headerName, headerValue);
        }
    }

    /**
     * Builds the request body.
     */
    private Map<String, Object> buildRequestBody(Long nin) {
        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("nin", nin);
        return requestBody;
    }

    /**
     * Builds the HTTP entity from headers and body.
     */
    private HttpEntity<Map<String, Object>> buildRequestEntity(HttpHeaders headers,
                                                               Map<String, Object> requestBody) {
        return new HttpEntity<>(requestBody, headers);
    }

    /**
     * Executes the external API request.
     * Does not swallow the exception.
     */
    private ResponseEntity<Map> executeRequest(String jobName,
                                               Long nin,
                                               String url,
                                               HttpMethod httpMethod,
                                               HttpEntity<Map<String, Object>> requestEntity) {
        logger.info("Calling API for jobName={} url={} nin={}", jobName, url, nin);
        //Thread.sleep(500);
        return restTemplate.exchange(url, httpMethod, requestEntity, Map.class);
    }

    /**
     * Prepares final map for success case.
     */
    private Map<String, Object> prepareSuccessResponse(ResponseEntity<Map> restResponse) {
        //real data
        Map<String, Object> responseBody = restResponse.getBody();

        if (responseBody == null) {
            responseBody = new LinkedHashMap<>();
        } else {
            responseBody = new LinkedHashMap<>(responseBody);
        }

        responseBody.put("failure", false);
        responseBody.put("message", null);
        responseBody.put("errorCode", null);
        responseBody.put("statusCode", String.valueOf(restResponse.getStatusCode().value()));

        return responseBody;
    }

    /**
     * Prepares final map for error case in standard company style.
     */
    private Map<String, Object> prepareErrorResponse(Exception exception) {
        Map<String, Object> responseBody = new LinkedHashMap<>();

        if (exception instanceof HttpClientErrorException || exception instanceof HttpServerErrorException) {
            HttpStatusCodeException httpEx = (HttpStatusCodeException) exception;

            String statusCode = String.valueOf(httpEx.getRawStatusCode());
            String responseText = httpEx.getResponseBodyAsString();
//            String errorCode = extractErrorCode(responseText, statusCode);

            responseBody.put("failure", true);
            responseBody.put("message",  responseText);
          //  responseBody.put("errorCode", errorCode);
            responseBody.put("statusCode", String.valueOf(httpEx.getRawStatusCode()));

            return responseBody;
        }

        responseBody.put("failure", true);
        responseBody.put("message", exception.getMessage());
       // responseBody.put("errorCode", "");
        responseBody.put("statusCode", null);

        return responseBody;
    }

//    /**
//     * Extracts error code from integration error response body.
//     * If not found, returns the provided default code.
//     */
//    private String extractErrorCode(String responseBody, String defaultCode) {
//        try {
//            ObjectMapper mapper = new ObjectMapper();
//            JsonNode root = mapper.readTree(responseBody);
//
//            if (root.has("ErrorCode")) {
//                return root.get("ErrorCode").asText();
//            }
//        } catch (Exception ex) {
//            logger.warn("Failed to parse error code from response: {}", ex.getMessage());
//        }
////
//        return String.valueOf(defaultCode);
//       // return defaultCode;
//    }

    /**
     * Returns a required configuration value.
     */
    private String getRequiredConfigValue(Map<String, String> config, String key) {
        String value = config.get(key);

        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Missing required config key: " + key);
        }

        return value;
    }
}