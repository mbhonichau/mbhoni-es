package com.mbhoni_creative.api.controller;

import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController @Hidden
public class ApiNotFoundController {
    @RequestMapping({"/api/v1/**", "/api/docs/**"})
    public void notFound() { throw new ResponseStatusException(HttpStatus.NOT_FOUND); }
}
