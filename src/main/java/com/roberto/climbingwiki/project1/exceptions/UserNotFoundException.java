package com.roberto.climbingwiki.project1.exceptions;

public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(Long id) {
        super("User not found: id=" + id);
    }

    public UserNotFoundException(String email) {
        super("User not found: email=" + email);
    }
}
