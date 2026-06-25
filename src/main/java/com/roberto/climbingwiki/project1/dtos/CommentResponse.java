package com.roberto.climbingwiki.project1.dtos;

import com.roberto.climbingwiki.project1.model.entities.Comment;
import java.time.LocalDateTime;

public record CommentResponse(
        Long id,
        Long userId,
        Long routeId,
        Long cragId,
        Long parentCommentId,
        String body,
        LocalDateTime createdAt) {

    public static CommentResponse from(Comment comment) {
        return new CommentResponse(
                comment.getId(),
                comment.getUser().getId(),
                comment.getRoute() != null ? comment.getRoute().getId() : null,
                comment.getCrag() != null ? comment.getCrag().getId() : null,
                comment.getParentComment() != null ? comment.getParentComment().getId() : null,
                comment.getBody(),
                comment.getCreatedAt());
    }
}
