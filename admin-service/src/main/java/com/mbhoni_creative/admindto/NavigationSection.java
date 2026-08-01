package com.mbhoni_creative.admindto;

import java.util.List;

public class NavigationSection {

    private final String title;
    private final String icon;
    private final List<NavigationItem> items;

    public NavigationSection(String title, String icon, List<NavigationItem> items) {
        this.title = title;
        this.icon = icon;
        this.items = items;
    }

    public String getTitle() {
        return title;
    }

    public String getIcon() {
        return icon;
    }

    public List<NavigationItem> getItems() {
        return items;
    }

    public boolean isActive() {
        return items != null && items.stream().anyMatch(NavigationItem::isActive);
    }
}
