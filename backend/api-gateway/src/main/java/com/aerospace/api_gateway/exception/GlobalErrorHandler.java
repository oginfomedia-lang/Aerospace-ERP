package com.aerospace.api_gateway.exception;

import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.reactive.error.DefaultErrorAttributes;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;

import java.util.Map;

@Component
public class GlobalErrorHandler extends DefaultErrorAttributes {

    @Override
    public Map<String, Object> getErrorAttributes(ServerRequest request, ErrorAttributeOptions options) {
        Map<String, Object> errorAttributes = super.getErrorAttributes(request, options);
        Throwable error = getError(request);

        errorAttributes.put("status", errorAttributes.get("status"));
        errorAttributes.put("message", errorAttributes.get("error"));
        errorAttributes.put("timestamp", errorAttributes.get("timestamp"));
        errorAttributes.put("path", errorAttributes.get("path"));

        // Remove internal fields
        errorAttributes.remove("error");
        errorAttributes.remove("requestId");

        return errorAttributes;
    }
}