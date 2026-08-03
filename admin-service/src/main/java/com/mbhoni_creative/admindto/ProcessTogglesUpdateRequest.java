package com.mbhoni_creative.admindto;

public class ProcessTogglesUpdateRequest {

    private Boolean mfaEnforced;
    private Boolean ssoEnforced;
    private Boolean apiAccessAllowed;
    private Boolean auditExtended;
    private Boolean passwordChangeRequired;

    public Boolean getMfaEnforced() {
        return mfaEnforced;
    }

    public void setMfaEnforced(Boolean mfaEnforced) {
        this.mfaEnforced = mfaEnforced;
    }

    public Boolean getSsoEnforced() {
        return ssoEnforced;
    }

    public void setSsoEnforced(Boolean ssoEnforced) {
        this.ssoEnforced = ssoEnforced;
    }

    public Boolean getApiAccessAllowed() {
        return apiAccessAllowed;
    }

    public void setApiAccessAllowed(Boolean apiAccessAllowed) {
        this.apiAccessAllowed = apiAccessAllowed;
    }

    public Boolean getAuditExtended() {
        return auditExtended;
    }

    public void setAuditExtended(Boolean auditExtended) {
        this.auditExtended = auditExtended;
    }

    public Boolean getPasswordChangeRequired() {
        return passwordChangeRequired;
    }

    public void setPasswordChangeRequired(Boolean passwordChangeRequired) {
        this.passwordChangeRequired = passwordChangeRequired;
    }
}
