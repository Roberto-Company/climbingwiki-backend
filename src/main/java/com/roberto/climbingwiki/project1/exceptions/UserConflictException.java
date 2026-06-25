package com.roberto.climbingwiki.project1.exceptions;

public class UserConflictException extends RuntimeException {

    public UserConflictException(String field, String value) {
        super("User with " + field + "=" + value + " already exists");
    }
}
