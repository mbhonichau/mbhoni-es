package com.mbhoni_creative.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiEnvelope<T>(T data, Page page) {
    public static <T> ApiEnvelope<T> single(T data) { return new ApiEnvelope<>(data, null); }
    public record Page(int number, int size, long totalElements) {}
}
