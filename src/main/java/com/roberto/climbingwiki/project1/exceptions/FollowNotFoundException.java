package com.roberto.climbingwiki.project1.exceptions;

public class FollowNotFoundException extends RuntimeException {

    public FollowNotFoundException(Long followerId, Long followedId) {
        super("Follow not found: followerId=" + followerId + ", followedId=" + followedId);
    }
}
