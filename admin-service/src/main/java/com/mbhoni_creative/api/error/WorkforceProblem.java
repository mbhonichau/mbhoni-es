package com.mbhoni_creative.api.error;

public class WorkforceProblem extends RuntimeException {
    private final int status;
    private final String code;
    public WorkforceProblem(int status, String code) { super(code); this.status = status; this.code = code; }
    public int status() { return status; }
    public String code() { return code; }
}
