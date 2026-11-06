package com.roberto.climbingwiki.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.roberto.climbingwiki.dtos.FollowRequest;
import com.roberto.climbingwiki.dtos.FollowResponse;
import com.roberto.climbingwiki.dtos.UpsertResult;
import com.roberto.climbingwiki.exceptions.ForbiddenException;
import com.roberto.climbingwiki.exceptions.FollowNotFoundException;
import com.roberto.climbingwiki.exceptions.InvalidFollowException;
import com.roberto.climbingwiki.exceptions.UserNotFoundException;
import com.roberto.climbingwiki.model.entities.Follow;
import com.roberto.climbingwiki.model.entities.FollowId;
import com.roberto.climbingwiki.model.entities.User;
import com.roberto.climbingwiki.repositories.FollowRepository;
import com.roberto.climbingwiki.repositories.UserRepository;
import com.roberto.climbingwiki.security.AuthenticatedUser;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FollowServiceTest {

    @Mock
    private FollowRepository followRepository;

    @Mock
    private UserRepository userRepository;

    private FollowService followService;

    @BeforeEach
    void setUp() {
        followService = new FollowService(followRepository, userRepository);
    }

    private User user(Long id) {
        User user = new User();
        user.setId(id);
        return user;
    }

    private Follow follow(Long followerId, Long followedId) {
        Follow follow = new Follow();
        follow.setFollower(user(followerId));
        follow.setFollowed(user(followedId));
        return follow;
    }

    private FollowRequest request(Long followerId, Long followedId) {
        return new FollowRequest(followerId, followedId);
    }

    private AuthenticatedUser self(Long id) {
        return new AuthenticatedUser(id, "jane", User.Role.USER);
    }

    private AuthenticatedUser admin(Long id) {
        return new AuthenticatedUser(id, "boss", User.Role.ADMIN);
    }

    @Test
    void findAll_returnsMappedResponses() {
        when(followRepository.findAll()).thenReturn(List.of(follow(1L, 2L), follow(3L, 2L)));

        List<FollowResponse> result = followService.findAll();

        assertThat(result).extracting(FollowResponse::followerId).containsExactly(1L, 3L);
    }

    @Test
    void findByFollower_returnsFollowsByThatUser() {
        when(followRepository.findByFollowerId(1L)).thenReturn(List.of(follow(1L, 2L)));

        List<FollowResponse> result = followService.findByFollower(1L);

        assertThat(result).extracting(FollowResponse::followerId).containsExactly(1L);
    }

    @Test
    void findByFollowed_returnsFollowersOfThatUser() {
        when(followRepository.findByFollowedId(2L)).thenReturn(List.of(follow(1L, 2L)));

        List<FollowResponse> result = followService.findByFollowed(2L);

        assertThat(result).extracting(FollowResponse::followedId).containsExactly(2L);
    }

    @Test
    void findOne_existingFollow_returnsResponse() {
        when(followRepository.findById(new FollowId(1L, 2L))).thenReturn(Optional.of(follow(1L, 2L)));

        FollowResponse result = followService.findOne(1L, 2L);

        assertThat(result.followedId()).isEqualTo(2L);
    }

    @Test
    void findOne_missingFollow_throws() {
        when(followRepository.findById(new FollowId(1L, 99L))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> followService.findOne(1L, 99L))
                .isInstanceOf(FollowNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void upsert_selfFollow_throws() {
        assertThatThrownBy(() -> followService.upsert(request(1L, 1L), self(1L)))
                .isInstanceOf(InvalidFollowException.class);

        verify(followRepository, never()).save(any());
    }

    @Test
    void upsert_missingFollower_throws() {
        when(followRepository.existsById(any())).thenReturn(false);
        when(followRepository.findById(any())).thenReturn(Optional.empty());
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> followService.upsert(request(1L, 2L), self(1L)))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void upsert_missingFollowed_throws() {
        when(followRepository.existsById(any())).thenReturn(false);
        when(followRepository.findById(any())).thenReturn(Optional.empty());
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L)));
        when(userRepository.findById(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> followService.upsert(request(1L, 2L), self(1L)))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void upsert_newFollow_createsAndReturnsCreatedTrue() {
        when(followRepository.existsById(new FollowId(1L, 2L))).thenReturn(false);
        when(followRepository.findById(new FollowId(1L, 2L))).thenReturn(Optional.empty());
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L)));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user(2L)));
        when(followRepository.save(any(Follow.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpsertResult<FollowResponse> result = followService.upsert(request(1L, 2L), self(1L));

        assertThat(result.created()).isTrue();
        assertThat(result.data().followedId()).isEqualTo(2L);
    }

    @Test
    void upsert_existingFollow_isNoOpAndReturnsCreatedFalse() {
        Follow existing = follow(1L, 2L);
        when(followRepository.existsById(new FollowId(1L, 2L))).thenReturn(true);
        when(followRepository.findById(new FollowId(1L, 2L))).thenReturn(Optional.of(existing));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L)));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user(2L)));
        when(followRepository.save(any(Follow.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpsertResult<FollowResponse> result = followService.upsert(request(1L, 2L), self(1L));

        assertThat(result.created()).isFalse();
    }

    @Test
    void upsert_byAnotherRegularUser_throwsForbidden() {
        assertThatThrownBy(() -> followService.upsert(request(1L, 2L), self(3L)))
                .isInstanceOf(ForbiddenException.class);

        verify(followRepository, never()).save(any());
    }

    @Test
    void upsert_byModerator_succeeds() {
        when(followRepository.existsById(new FollowId(1L, 2L))).thenReturn(false);
        when(followRepository.findById(new FollowId(1L, 2L))).thenReturn(Optional.empty());
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L)));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user(2L)));
        when(followRepository.save(any(Follow.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpsertResult<FollowResponse> result = followService.upsert(
                request(1L, 2L), new AuthenticatedUser(99L, "mod", User.Role.MODERATOR));

        assertThat(result.data().followedId()).isEqualTo(2L);
    }

    @Test
    void delete_existingFollow_deletes() {
        when(followRepository.existsById(new FollowId(1L, 2L))).thenReturn(true);

        followService.delete(1L, 2L, self(1L));

        verify(followRepository).deleteById(new FollowId(1L, 2L));
    }

    @Test
    void delete_missingFollow_isNoOpAndDoesNotThrow() {
        when(followRepository.existsById(new FollowId(1L, 99L))).thenReturn(false);

        followService.delete(1L, 99L, self(1L));

        verify(followRepository, never()).deleteById(any());
    }

    @Test
    void delete_byAnotherRegularUser_throwsForbidden() {
        assertThatThrownBy(() -> followService.delete(1L, 2L, self(3L))).isInstanceOf(ForbiddenException.class);

        verify(followRepository, never()).deleteById(any());
    }

    @Test
    void delete_byAdmin_deletesAnotherUsersFollow() {
        when(followRepository.existsById(new FollowId(1L, 2L))).thenReturn(true);

        followService.delete(1L, 2L, admin(99L));

        verify(followRepository).deleteById(new FollowId(1L, 2L));
    }
}
