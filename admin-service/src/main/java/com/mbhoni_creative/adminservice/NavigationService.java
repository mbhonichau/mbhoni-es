package com.mbhoni_creative.adminservice;

import java.util.List;

import com.mbhoni_creative.admindto.NavigationSection;

public interface NavigationService {

    List<NavigationSection> getSidebarSections(String requestPath);
}
