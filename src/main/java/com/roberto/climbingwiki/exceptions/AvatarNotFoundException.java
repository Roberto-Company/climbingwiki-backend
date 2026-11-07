package com.roberto.climbingwiki.exceptions;

public class AvatarNotFoundException extends RuntimeException {

    public AvatarNotFoundException(Long userId) {
        super("User " + userId + " has no uploaded profile picture");
    }
}
