package com.roberto.climbingwiki.dtos;

/**
 * Activity counts shown on a user's profile.
 *
 * @param sends logbook entries that count as a send (redpoint, flash or onsight)
 * @param hardestSendGradeId grade of the hardest pitch on any sent route, or null if nothing graded was sent
 */
public record UserStatsResponse(
        long ascents,
        long sends,
        Long hardestSendGradeId,
        long favorites,
        long followers,
        long following) {}
