package com.roberto.climbingwiki.project1.exceptions;

public class InvalidCommentTargetException extends RuntimeException {

    public InvalidCommentTargetException() {
        super("Comment must be attached to at least one of routeId or cragId");
    }
}
