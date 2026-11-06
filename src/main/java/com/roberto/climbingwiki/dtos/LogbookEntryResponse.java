package com.roberto.climbingwiki.dtos;

import com.roberto.climbingwiki.model.entities.LogbookEntry;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record LogbookEntryResponse(
        Long id,
        Long userId,
        Long routeId,
        LocalDate attemptDate,
        LogbookEntry.Result result,
        String notes,
        LocalDateTime createdAt) {

    public static LogbookEntryResponse from(LogbookEntry logbookEntry) {
        return new LogbookEntryResponse(
                logbookEntry.getId(),
                logbookEntry.getUser().getId(),
                logbookEntry.getRoute().getId(),
                logbookEntry.getAttemptDate(),
                logbookEntry.getResult(),
                logbookEntry.getNotes(),
                logbookEntry.getCreatedAt());
    }
}
