package com.mbhoni_creative.contentmanagementservice;

    import feign.Response;
    import feign.codec.ErrorDecoder;
    import org.springframework.stereotype.Component;

    @Component
    public class FeignErrorDecoder implements ErrorDecoder {
        @Override
        public Exception decode(String methodKey, Response response) {
            if (response.status() == 404) {
                return new RuntimeException("Admin service not found for method: " + methodKey);
            }
            return new RuntimeException("Feign client error: " + response.status());
        }
    }