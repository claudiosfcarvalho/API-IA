package com.apiia.common.error;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

public record ApiErrorResponse(
        String correlationId,
        ErrorBody error
) {

    public record ErrorBody(
            String code,
            String message,
            @JsonInclude(JsonInclude.Include.NON_NULL)
            Map<String, Object> details
    ) {
    }
}
