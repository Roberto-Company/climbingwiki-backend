package com.roberto.climbingwiki.repositories;

/** One pitch reduced to what a route summary needs. Grade fields are null for ungraded pitches. */
public record PitchSummaryRow(Long routeId, Long gradeId, Integer difficultyScore, Integer lengthM) {}
