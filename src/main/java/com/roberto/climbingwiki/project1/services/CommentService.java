package com.roberto.climbingwiki.project1.services;

import com.roberto.climbingwiki.project1.dtos.CommentRequest;
import com.roberto.climbingwiki.project1.dtos.CommentResponse;
import com.roberto.climbingwiki.project1.exceptions.CommentNotFoundException;
import com.roberto.climbingwiki.project1.exceptions.CragNotFoundException;
import com.roberto.climbingwiki.project1.exceptions.ForbiddenException;
import com.roberto.climbingwiki.project1.exceptions.InvalidCommentParentException;
import com.roberto.climbingwiki.project1.exceptions.InvalidCommentTargetException;
import com.roberto.climbingwiki.project1.exceptions.RouteNotFoundException;
import com.roberto.climbingwiki.project1.exceptions.UserNotFoundException;
import com.roberto.climbingwiki.project1.model.entities.Comment;
import com.roberto.climbingwiki.project1.model.entities.Crag;
import com.roberto.climbingwiki.project1.model.entities.Route;
import com.roberto.climbingwiki.project1.model.entities.User;
import com.roberto.climbingwiki.project1.repositories.CommentRepository;
import com.roberto.climbingwiki.project1.repositories.CragRepository;
import com.roberto.climbingwiki.project1.repositories.RouteRepository;
import com.roberto.climbingwiki.project1.repositories.UserRepository;
import com.roberto.climbingwiki.project1.security.AuthenticatedUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommentService {

    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final RouteRepository routeRepository;
    private final CragRepository cragRepository;

    public CommentService(
            CommentRepository commentRepository,
            UserRepository userRepository,
            RouteRepository routeRepository,
            CragRepository cragRepository) {
        this.commentRepository = commentRepository;
        this.userRepository = userRepository;
        this.routeRepository = routeRepository;
        this.cragRepository = cragRepository;
    }

    public Page<CommentResponse> findAll(Pageable pageable) {
        return commentRepository.findAll(pageable).map(CommentResponse::from);
    }

    public Page<CommentResponse> findByRoute(Long routeId, Pageable pageable) {
        return commentRepository.findByRouteId(routeId, pageable).map(CommentResponse::from);
    }

    public Page<CommentResponse> findByCrag(Long cragId, Pageable pageable) {
        return commentRepository.findByCragId(cragId, pageable).map(CommentResponse::from);
    }

    public CommentResponse findById(Long id) {
        return commentRepository.findById(id)
                .map(CommentResponse::from)
                .orElseThrow(() -> new CommentNotFoundException(id));
    }

    @Transactional
    public CommentResponse create(CommentRequest request, AuthenticatedUser requester) {
        if (!requester.isSelfOrModeratorOrAdmin(request.userId())) {
            throw new ForbiddenException();
        }
        Comment comment = new Comment();
        applyRequest(comment, request, null);
        return CommentResponse.from(commentRepository.save(comment));
    }

    @Transactional
    public CommentResponse update(Long id, CommentRequest request, AuthenticatedUser requester) {
        Comment comment = commentRepository.findById(id).orElseThrow(() -> new CommentNotFoundException(id));
        if (!requester.isSelfOrModeratorOrAdmin(comment.getUser().getId())) {
            throw new ForbiddenException();
        }
        applyRequest(comment, request, id);
        return CommentResponse.from(commentRepository.save(comment));
    }

    @Transactional
    public void delete(Long id, AuthenticatedUser requester) {
        Comment comment = commentRepository.findById(id).orElseThrow(() -> new CommentNotFoundException(id));
        if (!requester.isSelfOrModeratorOrAdmin(comment.getUser().getId())) {
            throw new ForbiddenException();
        }
        commentRepository.deleteById(id);
    }

    private void applyRequest(Comment comment, CommentRequest request, Long selfId) {
        if (request.routeId() == null && request.cragId() == null) {
            throw new InvalidCommentTargetException();
        }

        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new UserNotFoundException(request.userId()));
        comment.setUser(user);

        if (request.routeId() != null) {
            Route route = routeRepository.findById(request.routeId())
                    .orElseThrow(() -> new RouteNotFoundException(request.routeId()));
            comment.setRoute(route);
        } else {
            comment.setRoute(null);
        }

        if (request.cragId() != null) {
            Crag crag = cragRepository.findById(request.cragId())
                    .orElseThrow(() -> new CragNotFoundException(request.cragId()));
            comment.setCrag(crag);
        } else {
            comment.setCrag(null);
        }

        if (request.parentCommentId() != null) {
            if (request.parentCommentId().equals(selfId)) {
                throw new InvalidCommentParentException(selfId);
            }
            Comment parentComment = commentRepository
                    .findById(request.parentCommentId())
                    .orElseThrow(() -> new CommentNotFoundException(request.parentCommentId()));
            comment.setParentComment(parentComment);
        } else {
            comment.setParentComment(null);
        }

        comment.setBody(request.body());
    }
}
