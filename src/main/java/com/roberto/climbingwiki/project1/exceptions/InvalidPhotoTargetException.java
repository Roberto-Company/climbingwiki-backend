package com.roberto.climbingwiki.project1.exceptions;

public class InvalidPhotoTargetException extends RuntimeException {

    public InvalidPhotoTargetException() {
        super("Photo must be attached to at least one of routeId or cragId");
    }
}
