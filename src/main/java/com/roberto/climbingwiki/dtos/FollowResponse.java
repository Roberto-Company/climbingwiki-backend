package com.roberto.climbingwiki.dtos;

import com.roberto.climbingwiki.model.entities.Follow;
import java.time.LocalDateTime;

public record FollowResponse(Long followerId, Long followedId, LocalDateTime createdAt) {

    public static FollowResponse from(Follow follow) {
        return new FollowResponse(
                follow.getFollower().getId(), follow.getFollowed().getId(), follow.getCreatedAt());
    }
}
