package com.maintenancemate.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when a maintenance request id does not exist.
 * Spring MVC translates it into an HTTP 404 response.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class RequestNotFoundException extends RuntimeException {

    public RequestNotFoundException(Long id) {
        super("Maintenance request " + id + " not found");
    }
}
