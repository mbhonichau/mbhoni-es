package com.mbhoni_creative.admindto;

public class NavigationItem {

    private final String label;
    private final String url;
    private final String icon;
    private final String badge;
    private final boolean active;

    public NavigationItem(String label, String url, String icon, String badge, boolean active) {
        this.label = label;
        this.url = url;
        this.icon = icon;
        this.badge = badge;
        this.active = active;
    }

    public String getLabel() {
        return label;
    }

    public String getUrl() {
        return url;
    }

    public String getIcon() {
        return icon;
    }

    public String getBadge() {
        return badge;
    }

    public boolean isActive() {
        return active;
    }
}
