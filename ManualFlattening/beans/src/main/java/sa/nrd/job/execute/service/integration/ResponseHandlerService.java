//package sa.nrd.job.execute.service.integration;
//
//import org.springframework.http.ResponseEntity;
//import org.springframework.stereotype.Service;
//import org.springframework.web.client.HttpClientErrorException;
//import org.springframework.web.client.HttpServerErrorException;
//import org.springframework.web.client.HttpStatusCodeException;
//
//import java.util.LinkedHashMap;
//import java.util.Map;
//
//
//@Service
//public class ResponseHandlerService {
//
//    /**
//     * Prepares final map for success case.
//     *
//     * @param restResponse integration response
//     * @return success response map
//     */
//    public Map<String, Object> prepareSuccessResponse(ResponseEntity<Map> restResponse) {
//        //real data
//        Map<String, Object> responseBody = restResponse.getBody();
//
//        if (responseBody == null) {
//            responseBody = new LinkedHashMap<>();
//        } else {
//            responseBody = new LinkedHashMap<>(responseBody);
//        }
//
//        responseBody.put("failure", false);
//        responseBody.put("message", null);
//        responseBody.put("errorCode", null);
//        responseBody.put("statusCode", String.valueOf(restResponse.getStatusCode().value()));
//
//        return responseBody;
//    }
//
//
//    /**
//     * Prepares final map for error case.
//     *
//     * @param exception current exception
//     * @return error response map
//     */
//    public Map<String, Object> prepareErrorResponse(Exception exception) {
//        Map<String, Object> responseBody = new LinkedHashMap<>();
//
//        Throwable actualException = exception;
//
//        if (exception instanceof RetryableIntegrationException && exception.getCause() != null) {
//            actualException = exception.getCause();
//        }
//
//        //RetryableIntegrationException -> cause = HttpServerErrorException
//        //unwraps exception and sees the real cause
//
//        //if (actualException instanceof HttpStatusCodeException httpEx) {
//        if (actualException instanceof HttpClientErrorException
//                || actualException instanceof HttpServerErrorException) {
//
//            HttpStatusCodeException httpEx = (HttpStatusCodeException) actualException;
//            responseBody.put("failure", true);
//            responseBody.put("message", httpEx.getResponseBodyAsString());
//            responseBody.put("statusCode", String.valueOf(httpEx.getRawStatusCode()));
//            responseBody.put("errorCode", null);
//            return responseBody;
//        }
//
//        responseBody.put("failure", true);
//        responseBody.put("message", actualException.getMessage());
//        responseBody.put("statusCode", null);
//        responseBody.put("errorCode", null);
//
//        return responseBody;
//    }
//
//
//
////    /**
////     * Extracts error code from integration error response body.
////     * If not found, returns the provided default code.
////     */
////    private String extractErrorCode(String responseBody, String defaultCode) {
////        try {
////            ObjectMapper mapper = new ObjectMapper();
////            JsonNode root = mapper.readTree(responseBody);
////
////            if (root.has("ErrorCode")) {
////                return root.get("ErrorCode").asText();
////            }
////        } catch (Exception ex) {
////            logger.warn("Failed to parse error code from response: {}", ex.getMessage());
////        }
//////
////        return String.valueOf(defaultCode);
////       // return defaultCode;
////    }
//}
