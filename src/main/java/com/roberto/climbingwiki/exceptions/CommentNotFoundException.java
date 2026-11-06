package com.roberto.climbingwiki.exceptions;

public class CommentNotFoundException extends RuntimeException {

    public CommentNotFoundException(Long id) {
        super("Comment not found: id=" + id);
    }
}
