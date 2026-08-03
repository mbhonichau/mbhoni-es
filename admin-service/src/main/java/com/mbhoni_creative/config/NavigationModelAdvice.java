package com.mbhoni_creative.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.mbhoni_creative.adminservice.NavigationService;

import jakarta.servlet.http.HttpServletRequest;

@ControllerAdvice(annotations = Controller.class)
public class NavigationModelAdvice {

    private final NavigationService navigationService;

    public NavigationModelAdvice(NavigationService navigationService) {
        this.navigationService = navigationService;
    }

    @ModelAttribute("sidebarSections")
    public Object sidebarSections(HttpServletRequest request) {
        return navigationService.getSidebarSections(request.getRequestURI());
    }
}
