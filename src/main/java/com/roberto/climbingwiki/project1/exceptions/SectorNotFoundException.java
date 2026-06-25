package com.roberto.climbingwiki.project1.exceptions;

public class SectorNotFoundException extends RuntimeException {

    public SectorNotFoundException(Long id) {
        super("Sector not found: id=" + id);
    }
}
