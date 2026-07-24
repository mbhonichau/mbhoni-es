package com.mbhoni_creative.admindto;

import java.time.LocalDateTime;

public class TenantDto {

    private Long id;

    private String name;

    private boolean active;

    // Derived UI fields
    private String latestUsage;
    private LocalDateTime lastUpdated;
    
    private Long industryProfileId;
    private String industryProfileName;

    // ================= GETTERS & SETTERS =================

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getLatestUsage() {
        return latestUsage;
    }

    public void setLatestUsage(String latestUsage) {
        this.latestUsage = latestUsage;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

	public Object getTenantId() {
		// TODO Auto-generated method stub
		return null;
	}
	
	public Long getIndustryProfileId() {
	    return industryProfileId;
	}

	public void setIndustryProfileId(Long industryProfileId) {
	    this.industryProfileId = industryProfileId;
	}

	public String getIndustryProfileName() {
	    return industryProfileName;
	}

	public void setIndustryProfileName(String industryProfileName) {
	    this.industryProfileName = industryProfileName;
	}
}