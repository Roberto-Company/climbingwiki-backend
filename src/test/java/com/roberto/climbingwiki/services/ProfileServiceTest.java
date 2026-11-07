package com.roberto.climbingwiki.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.roberto.climbingwiki.dtos.ProfileRequest;
import com.roberto.climbingwiki.dtos.UserResponse;
import com.roberto.climbingwiki.dtos.UserStatsResponse;
import com.roberto.climbingwiki.exceptions.ForbiddenException;
import com.roberto.climbingwiki.exceptions.UserNotFoundException;
import com.roberto.climbingwiki.model.entities.User;
import com.roberto.climbingwiki.repositories.FavoriteRepository;
import com.roberto.climbingwiki.repositories.FollowRepository;
import com.roberto.climbingwiki.repositories.LogbookEntryRepository;
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
class ProfileServiceTest {

    private static final AuthenticatedUser SELF = new AuthenticatedUser(7L, "jane", User.Role.USER);

    @Mock
    private UserRepository userRepository;

    @Mock
    private LogbookEntryRepository logbookEntryRepository;

    @Mock
    private FavoriteRepository favoriteRepository;

    @Mock
    private FollowRepository followRepository;

    private ProfileService profileService;

    @BeforeEach
    void setUp() {
        profileService = new ProfileService(userRepository, logbookEntryRepository, favoriteRepository, followRepository);
    }

    private User user(Long id) {
        User user = new User();
        user.setId(id);
        user.setUsername("jane");
        user.setEmail("jane@example.com");
        user.setPasswordHash("hash");
        user.setRole(User.Role.USER);
        return user;
    }

    @Test
    void updateProfile_bySelf_updatesNameAndBio_andLeavesCredentialsAlone() {
        User jane = user(7L);
        when(userRepository.findById(7L)).thenReturn(Optional.of(jane));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = profileService.updateProfile(7L, new ProfileRequest("  Jane Climber ", "Limestone fan"), SELF);

        assertThat(response.fullName()).isEqualTo("Jane Climber");
        assertThat(response.bio()).isEqualTo("Limestone fan");
        assertThat(jane.getPasswordHash()).isEqualTo("hash");
        assertThat(jane.getUsername()).isEqualTo("jane");
    }

    @Test
    void updateProfile_blankValues_clearTheFields() {
        User jane = user(7L);
        jane.setFullName("Old");
        jane.setBio("Old bio");
        when(userRepository.findById(7L)).thenReturn(Optional.of(jane));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = profileService.updateProfile(7L, new ProfileRequest(" ", null), SELF);

        assertThat(response.fullName()).isNull();
        assertThat(response.bio()).isNull();
    }

    @Test
    void updateProfile_bySomeoneElse_isForbidden() {
        AuthenticatedUser stranger = new AuthenticatedUser(9L, "bob", User.Role.USER);

        assertThatThrownBy(() -> profileService.updateProfile(7L, new ProfileRequest("x", "y"), stranger))
                .isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> profileService.updateProfile(7L, new ProfileRequest("x", "y"), null))
                .isInstanceOf(ForbiddenException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void stats_collectsTheCounts_andTheHardestSend() {
        when(userRepository.existsById(7L)).thenReturn(true);
        when(logbookEntryRepository.findGradeIdsByDifficulty(eq(7L), eq(ProfileService.SEND_RESULTS), any()))
                .thenReturn(List.of(42L));
        when(logbookEntryRepository.countByUserId(7L)).thenReturn(12L);
        when(logbookEntryRepository.countByUserIdAndResultIn(7L, ProfileService.SEND_RESULTS)).thenReturn(9L);
        when(favoriteRepository.countByUserId(7L)).thenReturn(4L);
        when(followRepository.countByFollowedId(7L)).thenReturn(3L);
        when(followRepository.countByFollowerId(7L)).thenReturn(5L);

        UserStatsResponse stats = profileService.stats(7L);

        assertThat(stats).isEqualTo(new UserStatsResponse(12, 9, 42L, 4, 3, 5));
    }

    @Test
    void stats_withoutSends_hasNoHardestGrade() {
        when(userRepository.existsById(7L)).thenReturn(true);
        when(logbookEntryRepository.findGradeIdsByDifficulty(eq(7L), any(), any())).thenReturn(List.of());

        assertThat(profileService.stats(7L).hardestSendGradeId()).isNull();
    }

    @Test
    void stats_unknownUser_throwsNotFound() {
        when(userRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> profileService.stats(99L)).isInstanceOf(UserNotFoundException.class);
    }
}
