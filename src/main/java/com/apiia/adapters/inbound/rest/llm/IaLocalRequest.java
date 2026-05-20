package com.apiia.adapters.inbound.rest.llm;

import com.fasterxml.jackson.annotation.JsonInclude;

public record IaLocalRequest(
        String input,
        String model,
        Options options,
        String format
) {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Options(
            Double temperature,
            Double topP,
            Integer numCtx
    ) {
    }
}
