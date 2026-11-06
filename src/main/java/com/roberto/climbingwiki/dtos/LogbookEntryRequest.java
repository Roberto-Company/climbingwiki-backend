package com.roberto.climbingwiki.dtos;

import com.roberto.climbingwiki.model.entities.LogbookEntry;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record LogbookEntryRequest(
        @NotNull Long userId,
        @NotNull Long routeId,
        @NotNull LocalDate attemptDate,
        @NotNull LogbookEntry.Result result,
        String notes) {}
