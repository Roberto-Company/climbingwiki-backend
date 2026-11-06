package com.roberto.climbingwiki.exceptions;

public class CatalogItemInUseException extends RuntimeException {

    public CatalogItemInUseException() {
        super("This item still has data under it; delete that first or ask a moderator");
    }
}
