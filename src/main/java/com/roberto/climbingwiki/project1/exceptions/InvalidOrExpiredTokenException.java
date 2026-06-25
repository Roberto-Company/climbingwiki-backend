package com.roberto.climbingwiki.project1.exceptions;

public class InvalidOrExpiredTokenException extends RuntimeException {

    public InvalidOrExpiredTokenException() {
        super("Invalid or expired token");
    }
}
