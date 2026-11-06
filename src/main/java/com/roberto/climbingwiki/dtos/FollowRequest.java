package com.roberto.climbingwiki.dtos;

import jakarta.validation.constraints.NotNull;

public record FollowRequest(@NotNull Long followerId, @NotNull Long followedId) {}
