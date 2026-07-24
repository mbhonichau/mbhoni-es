package com.mbhoni_creative.admindto;

public class GeneratedApiKey {

    private final ApiKeyDto apiKey;
    private final String rawKey;

    public GeneratedApiKey(ApiKeyDto apiKey, String rawKey) {
        this.apiKey = apiKey;
        this.rawKey = rawKey;
    }

    public ApiKeyDto getApiKey() {
        return apiKey;
    }

    public String getRawKey() {
        return rawKey;
    }
}
