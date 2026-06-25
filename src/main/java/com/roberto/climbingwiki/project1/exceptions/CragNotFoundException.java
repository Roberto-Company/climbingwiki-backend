package com.roberto.climbingwiki.project1.exceptions;

public class CragNotFoundException extends RuntimeException {

    public CragNotFoundException(Long id) {
        super("Crag not found: id=" + id);
    }
}
