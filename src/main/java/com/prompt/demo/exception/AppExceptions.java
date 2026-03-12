package com.prompt.demo.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

public class AppExceptions {

    @ResponseStatus(HttpStatus.NOT_FOUND)
    public static class ResourceNotFoundException extends RuntimeException {
        public ResourceNotFoundException(String message) {
            super(message);
        }
        public ResourceNotFoundException(String resource, Object id) {
            super(String.format("%s not found with id: %s", resource, id));
        }
    }

    @ResponseStatus(HttpStatus.CONFLICT)
    public static class DuplicateResourceException extends RuntimeException {
        public DuplicateResourceException(String message) {
            super(message);
        }
    }

    @ResponseStatus(HttpStatus.FORBIDDEN)
    public static class AccessDeniedException extends RuntimeException {
        public AccessDeniedException(String message) {
            super(message);
        }
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public static class InvalidSqlException extends RuntimeException {
        public InvalidSqlException(String message) {
            super(message);
        }
    }

    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public static class AiServiceException extends RuntimeException {
        public AiServiceException(String message) {
            super(message);
        }
        public AiServiceException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public static class QueryExecutionException extends RuntimeException {
        public QueryExecutionException(String message) {
            super(message);
        }
        public QueryExecutionException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public static class AuthenticationException extends RuntimeException {
        public AuthenticationException(String message) {
            super(message);
        }
    }
}