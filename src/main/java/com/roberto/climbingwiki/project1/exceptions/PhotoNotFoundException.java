package com.roberto.climbingwiki.project1.exceptions;

public class PhotoNotFoundException extends RuntimeException {

    public PhotoNotFoundException(Long id) {
        super("Photo not found: id=" + id);
    }
}
