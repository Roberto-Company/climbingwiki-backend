package com.roberto.climbingwiki.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.roberto.climbingwiki.dtos.CommentRequest;
import com.roberto.climbingwiki.dtos.CommentResponse;
import com.roberto.climbingwiki.exceptions.CommentNotFoundException;
import com.roberto.climbingwiki.exceptions.CragNotFoundException;
import com.roberto.climbingwiki.exceptions.ForbiddenException;
import com.roberto.climbingwiki.exceptions.InvalidCommentParentException;
import com.roberto.climbingwiki.exceptions.InvalidCommentTargetException;
import com.roberto.climbingwiki.exceptions.RouteNotFoundException;
import com.roberto.climbingwiki.exceptions.UserNotFoundException;
import com.roberto.climbingwiki.model.entities.Comment;
import com.roberto.climbingwiki.model.entities.Crag;
import com.roberto.climbingwiki.model.entities.Route;
import com.roberto.climbingwiki.model.entities.User;
import com.roberto.climbingwiki.repositories.CommentRepository;
import com.roberto.climbingwiki.repositories.CragRepository;
import com.roberto.climbingwiki.repositories.RouteRepository;
import com.roberto.climbingwiki.repositories.UserRepository;
import com.roberto.climbingwiki.security.AuthenticatedUser;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RouteRepository routeRepository;

    @Mock
    private CragRepository cragRepository;

    private CommentService commentService;

    @BeforeEach
    void setUp() {
        commentService = new CommentService(commentRepository, userRepository, routeRepository, cragRepository);
    }

    private User user(Long id) {
        User user = new User();
        user.setId(id);
        return user;
    }

    private Route route(Long id) {
        Route route = new Route();
        route.setId(id);
        return route;
    }

    private Crag crag(Long id) {
        Crag crag = new Crag();
        crag.setId(id);
        return crag;
    }

    private Comment comment(Long id, User user, Route route, Crag crag, String body) {
        Comment comment = new Comment();
        comment.setId(id);
        comment.setUser(user);
        comment.setRoute(route);
        comment.setCrag(crag);
        comment.setBody(body);
        return comment;
    }

    private CommentRequest request(Long userId, Long routeId, Long cragId, Long parentCommentId, String body) {
        return new CommentRequest(userId, routeId, cragId, parentCommentId, body);
    }

    private AuthenticatedUser self(Long id) {
        return new AuthenticatedUser(id, "jane", User.Role.USER);
    }

    private AuthenticatedUser admin(Long id) {
        return new AuthenticatedUser(id, "boss", User.Role.ADMIN);
    }

    @Test
    void findAll_returnsMappedResponses() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Comment> commentPage =
                new PageImpl<>(List.of(comment(1L, user(1L), route(1L), null, "nice route")), pageable, 1);
        when(commentRepository.findAll(pageable)).thenReturn(commentPage);

        Page<CommentResponse> result = commentService.findAll(pageable);

        assertThat(result.getContent()).extracting(CommentResponse::body).containsExactly("nice route");
    }

    @Test
    void findByRoute_returnsCommentsForThatRoute() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Comment> commentPage =
                new PageImpl<>(List.of(comment(1L, user(1L), route(1L), null, "nice route")), pageable, 1);
        when(commentRepository.findByRouteId(1L, pageable)).thenReturn(commentPage);

        Page<CommentResponse> result = commentService.findByRoute(1L, pageable);

        assertThat(result.getContent()).extracting(CommentResponse::routeId).containsExactly(1L);
    }

    @Test
    void findByCrag_returnsCommentsForThatCrag() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Comment> commentPage =
                new PageImpl<>(List.of(comment(1L, user(1L), null, crag(1L), "nice crag")), pageable, 1);
        when(commentRepository.findByCragId(1L, pageable)).thenReturn(commentPage);

        Page<CommentResponse> result = commentService.findByCrag(1L, pageable);

        assertThat(result.getContent()).extracting(CommentResponse::cragId).containsExactly(1L);
    }

    @Test
    void findById_existingComment_returnsResponse() {
        when(commentRepository.findById(1L))
                .thenReturn(Optional.of(comment(1L, user(1L), route(1L), null, "nice route")));

        CommentResponse result = commentService.findById(1L);

        assertThat(result.body()).isEqualTo("nice route");
    }

    @Test
    void findById_missingComment_throws() {
        when(commentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.findById(99L))
                .isInstanceOf(CommentNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void create_neitherRouteNorCrag_throws() {
        assertThatThrownBy(() -> commentService.create(request(1L, null, null, null, "body"), self(1L)))
                .isInstanceOf(InvalidCommentTargetException.class);

        verify(commentRepository, never()).save(any());
    }

    @Test
    void create_missingUser_throws() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.create(request(1L, 1L, null, null, "body"), self(1L)))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void create_missingRoute_throws() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L)));
        when(routeRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.create(request(1L, 1L, null, null, "body"), self(1L)))
                .isInstanceOf(RouteNotFoundException.class);
    }

    @Test
    void create_missingCrag_throws() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L)));
        when(cragRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.create(request(1L, null, 1L, null, "body"), self(1L)))
                .isInstanceOf(CragNotFoundException.class);
    }

    @Test
    void create_withOnlyRoute_savesComment() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L)));
        when(routeRepository.findById(1L)).thenReturn(Optional.of(route(1L)));
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CommentResponse result = commentService.create(request(1L, 1L, null, null, "body"), self(1L));

        assertThat(result.routeId()).isEqualTo(1L);
        assertThat(result.cragId()).isNull();
    }

    @Test
    void create_withParentComment_resolvesParent() {
        Comment parent = comment(7L, user(1L), route(1L), null, "parent body");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L)));
        when(routeRepository.findById(1L)).thenReturn(Optional.of(route(1L)));
        when(commentRepository.findById(7L)).thenReturn(Optional.of(parent));
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CommentResponse result = commentService.create(request(1L, 1L, null, 7L, "reply"), self(1L));

        assertThat(result.parentCommentId()).isEqualTo(7L);
    }

    @Test
    void create_withMissingParentComment_throws() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L)));
        when(routeRepository.findById(1L)).thenReturn(Optional.of(route(1L)));
        when(commentRepository.findById(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.create(request(1L, 1L, null, 7L, "reply"), self(1L)))
                .isInstanceOf(CommentNotFoundException.class)
                .hasMessageContaining("7");
    }

    @Test
    void create_byAnotherRegularUser_throwsForbidden() {
        assertThatThrownBy(() -> commentService.create(request(1L, 1L, null, null, "body"), self(2L)))
                .isInstanceOf(ForbiddenException.class);

        verify(commentRepository, never()).save(any());
    }

    @Test
    void create_byModeratorOnBehalfOfAnotherUser_succeeds() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L)));
        when(routeRepository.findById(1L)).thenReturn(Optional.of(route(1L)));
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CommentResponse result = commentService.create(
                request(1L, 1L, null, null, "body"), new AuthenticatedUser(99L, "mod", User.Role.MODERATOR));

        assertThat(result.body()).isEqualTo("body");
    }

    @Test
    void update_existingComment_appliesChanges() {
        Comment existing = comment(1L, user(1L), route(1L), null, "old body");
        when(commentRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L)));
        when(routeRepository.findById(1L)).thenReturn(Optional.of(route(1L)));
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CommentResponse result = commentService.update(1L, request(1L, 1L, null, null, "new body"), self(1L));

        assertThat(result.body()).isEqualTo("new body");
    }

    @Test
    void update_missingComment_throws() {
        when(commentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.update(99L, request(1L, 1L, null, null, "body"), self(1L)))
                .isInstanceOf(CommentNotFoundException.class);
    }

    @Test
    void update_selfAsParent_throwsInvalidCommentParent() {
        Comment existing = comment(1L, user(1L), route(1L), null, "body");
        when(commentRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L)));
        when(routeRepository.findById(1L)).thenReturn(Optional.of(route(1L)));

        assertThatThrownBy(() -> commentService.update(1L, request(1L, 1L, null, 1L, "body"), self(1L)))
                .isInstanceOf(InvalidCommentParentException.class)
                .hasMessageContaining("1");

        verify(commentRepository, never()).save(any());
    }

    @Test
    void update_byAnotherRegularUser_throwsForbidden() {
        Comment existing = comment(1L, user(1L), route(1L), null, "body");
        when(commentRepository.findById(1L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> commentService.update(1L, request(1L, 1L, null, null, "body"), self(2L)))
                .isInstanceOf(ForbiddenException.class);

        verify(commentRepository, never()).save(any());
    }

    @Test
    void update_byModerator_succeeds() {
        Comment existing = comment(1L, user(1L), route(1L), null, "old body");
        when(commentRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L)));
        when(routeRepository.findById(1L)).thenReturn(Optional.of(route(1L)));
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CommentResponse result = commentService.update(
                1L, request(1L, 1L, null, null, "new body"), new AuthenticatedUser(99L, "mod", User.Role.MODERATOR));

        assertThat(result.body()).isEqualTo("new body");
    }

    @Test
    void delete_existingComment_deletes() {
        when(commentRepository.findById(1L)).thenReturn(Optional.of(comment(1L, user(1L), route(1L), null, "body")));

        commentService.delete(1L, self(1L));

        verify(commentRepository).deleteById(1L);
    }

    @Test
    void delete_missingComment_throwsAndDoesNotDelete() {
        when(commentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.delete(99L, self(99L))).isInstanceOf(CommentNotFoundException.class);

        verify(commentRepository, never()).deleteById(99L);
    }

    @Test
    void delete_byAnotherRegularUser_throwsForbiddenAndDoesNotDelete() {
        when(commentRepository.findById(1L)).thenReturn(Optional.of(comment(1L, user(1L), route(1L), null, "body")));

        assertThatThrownBy(() -> commentService.delete(1L, self(2L))).isInstanceOf(ForbiddenException.class);

        verify(commentRepository, never()).deleteById(any());
    }

    @Test
    void delete_byAdmin_deletesAnotherUsersComment() {
        when(commentRepository.findById(1L)).thenReturn(Optional.of(comment(1L, user(1L), route(1L), null, "body")));

        commentService.delete(1L, admin(99L));

        verify(commentRepository).deleteById(1L);
    }
}
