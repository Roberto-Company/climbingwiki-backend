package com.roberto.climbingwiki.project1.dtos;

import jakarta.validation.constraints.NotNull;

public record FollowRequest(@NotNull Long followerId, @NotNull Long followedId) {}
