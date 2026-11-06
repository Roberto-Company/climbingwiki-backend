package com.roberto.climbingwiki.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CommentRequest(
        @NotNull Long userId, Long routeId, Long cragId, Long parentCommentId, @NotBlank String body) {}
