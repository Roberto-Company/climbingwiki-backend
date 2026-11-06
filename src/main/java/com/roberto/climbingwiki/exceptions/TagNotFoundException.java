package com.roberto.climbingwiki.exceptions;

import java.util.Set;

public class TagNotFoundException extends RuntimeException {

    public TagNotFoundException(Set<Long> tagIds) {
        super("One or more tags not found: ids=" + tagIds);
    }
}
