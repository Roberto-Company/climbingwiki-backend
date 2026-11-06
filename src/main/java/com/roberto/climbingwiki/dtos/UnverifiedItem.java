package com.roberto.climbingwiki.dtos;

/**
 * One entry in the moderation queue.
 *
 * @param kind zone, crag, sector, route or pitch
 * @param name the item's name; for a pitch, its route's name
 * @param pitchNumber only for pitches
 * @param parentId the zone/crag/sector/route the item belongs to (a pitch's route), or null
 */
public record UnverifiedItem(
        String kind,
        Long id,
        String name,
        Integer pitchNumber,
        Long parentId,
        String parentName,
        Long createdById,
        String createdByUsername) {}
