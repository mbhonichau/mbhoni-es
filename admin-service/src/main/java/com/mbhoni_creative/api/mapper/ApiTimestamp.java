package com.mbhoni_creative.api.mapper;

import java.time.*;
import java.time.format.DateTimeParseException;

public final class ApiTimestamp {
    private ApiTimestamp() {}
    public static LocalDateTime parse(String value) {
        if (value == null) return null;
        try { return OffsetDateTime.parse(value).atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime(); }
        catch (DateTimeParseException ignored) { return LocalDateTime.parse(value); }
    }
}
