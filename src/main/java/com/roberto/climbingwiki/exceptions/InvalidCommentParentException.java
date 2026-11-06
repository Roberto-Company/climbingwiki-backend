package com.roberto.climbingwiki.exceptions;

public class InvalidCommentParentException extends RuntimeException {

    public InvalidCommentParentException(Long id) {
        super("Comment cannot be its own parent: id=" + id);
    }
}
