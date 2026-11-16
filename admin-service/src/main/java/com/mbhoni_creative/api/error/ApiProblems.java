package com.mbhoni_creative.api.error;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import org.springframework.http.*;

public final class ApiProblems {
    private ApiProblems() {}
    public static ProblemDetail detail(int status, String code, String message) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(status), message);
        problem.setType(URI.create("urn:mbhoni:api:problem:" + code.toLowerCase(java.util.Locale.ROOT)));
        problem.setProperty("code", code);
        return problem;
    }
    public static void write(ObjectMapper mapper, HttpServletResponse response, int status, String code, String message) throws IOException {
        response.setStatus(status); response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        mapper.writeValue(response.getOutputStream(), detail(status, code, message));
    }
}
