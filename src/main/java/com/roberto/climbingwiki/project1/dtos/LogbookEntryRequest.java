package com.roberto.climbingwiki.project1.dtos;

import com.roberto.climbingwiki.project1.model.entities.LogbookEntry;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record LogbookEntryRequest(
        @NotNull Long userId,
        @NotNull Long routeId,
        @NotNull LocalDate attemptDate,
        @NotNull LogbookEntry.Result result,
        String notes) {}
