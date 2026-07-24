package com.mbhoni_creative.adminexception;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AccessDeniedException.class)
    public String handleAccessDenied(
            AccessDeniedException exception,
            Model model,
            Authentication authentication) {

        model.addAttribute(
                "username",
                authentication != null ? authentication.getName() : "User"
        );

        return "access-denied";
    }
    
    @ExceptionHandler(RuntimeException.class)
    public String handleRuntimeException(
            RuntimeException exception,
            Model model,
            Authentication authentication) {

        model.addAttribute(
                "username",
                authentication != null ? authentication.getName() : "User"
        );

        model.addAttribute("errorMessage", exception.getMessage());

        return "error";
    }
}