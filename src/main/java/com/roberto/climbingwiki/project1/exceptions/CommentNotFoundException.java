package com.roberto.climbingwiki.project1.exceptions;

public class CommentNotFoundException extends RuntimeException {

    public CommentNotFoundException(Long id) {
        super("Comment not found: id=" + id);
    }
}
