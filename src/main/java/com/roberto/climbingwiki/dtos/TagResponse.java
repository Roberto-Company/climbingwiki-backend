package com.roberto.climbingwiki.dtos;

import com.roberto.climbingwiki.model.entities.Tag;

public record TagResponse(Long id, String name) {

    public static TagResponse from(Tag tag) {
        return new TagResponse(tag.getId(), tag.getName());
    }
}
