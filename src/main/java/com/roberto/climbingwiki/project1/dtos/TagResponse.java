package com.roberto.climbingwiki.project1.dtos;

import com.roberto.climbingwiki.project1.model.entities.Tag;

public record TagResponse(Long id, String name) {

    public static TagResponse from(Tag tag) {
        return new TagResponse(tag.getId(), tag.getName());
    }
}
