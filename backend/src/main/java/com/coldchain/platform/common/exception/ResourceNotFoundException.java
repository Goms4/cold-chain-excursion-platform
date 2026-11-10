package com.coldchain.platform.common.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends ApiException {
    public ResourceNotFoundException(String resourceName, String id) {
        super(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", resourceName + " with ID '" + id + "' was not found.");
    }
}
