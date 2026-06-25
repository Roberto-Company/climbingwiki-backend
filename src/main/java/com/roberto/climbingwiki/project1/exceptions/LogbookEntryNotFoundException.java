package com.roberto.climbingwiki.project1.exceptions;

public class LogbookEntryNotFoundException extends RuntimeException {

    public LogbookEntryNotFoundException(Long id) {
        super("Logbook entry not found: id=" + id);
    }
}
