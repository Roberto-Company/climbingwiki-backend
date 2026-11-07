package com.roberto.climbingwiki.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.roberto.climbingwiki.dtos.UserResponse;
import com.roberto.climbingwiki.exceptions.AvatarNotFoundException;
import com.roberto.climbingwiki.exceptions.ForbiddenException;
import com.roberto.climbingwiki.exceptions.InvalidImageException;
import com.roberto.climbingwiki.model.entities.User;
import com.roberto.climbingwiki.model.entities.UserAvatar;
import com.roberto.climbingwiki.repositories.UserAvatarRepository;
import com.roberto.climbingwiki.repositories.UserRepository;
import com.roberto.climbingwiki.security.AuthenticatedUser;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AvatarServiceTest {

    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0x10};
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0};
    private static final byte[] WEBP = {'R', 'I', 'F', 'F', 0x24, 0, 0, 0, 'W', 'E', 'B', 'P', 'V', 'P'};
    private static final AuthenticatedUser SELF = new AuthenticatedUser(7L, "jane", User.Role.USER);

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserAvatarRepository avatarRepository;

    private AvatarService avatarService;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.ofEpochMilli(1_700_000_000_000L), ZoneOffset.UTC);
        avatarService = new AvatarService(userRepository, avatarRepository, clock);
    }

    private User user(Long id) {
        User user = new User();
        user.setId(id);
        user.setUsername("jane");
        user.setRole(User.Role.USER);
        return user;
    }

    @Test
    void detectContentType_readsTheFileSignature() {
        assertThat(AvatarService.detectContentType(JPEG)).isEqualTo("image/jpeg");
        assertThat(AvatarService.detectContentType(PNG)).isEqualTo("image/png");
        assertThat(AvatarService.detectContentType(WEBP)).isEqualTo("image/webp");
    }

    @Test
    void detectContentType_rejectsOtherFiles() {
        byte[] svg = "<svg xmlns=\"http://www.w3.org/2000/svg\"/>".getBytes();
        byte[] gif = "GIF89a....".getBytes();

        assertThatThrownBy(() -> AvatarService.detectContentType(svg)).isInstanceOf(InvalidImageException.class);
        assertThatThrownBy(() -> AvatarService.detectContentType(gif)).isInstanceOf(InvalidImageException.class);
        assertThatThrownBy(() -> AvatarService.detectContentType(new byte[] {(byte) 0xFF}))
                .isInstanceOf(InvalidImageException.class);
    }

    @Test
    void upload_bySelf_storesThePictureAndPointsAvatarUrlAtAVersionedPath() {
        User jane = user(7L);
        when(userRepository.findById(7L)).thenReturn(Optional.of(jane));
        when(avatarRepository.findById(7L)).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = avatarService.upload(7L, PNG, SELF);

        ArgumentCaptor<UserAvatar> saved = ArgumentCaptor.forClass(UserAvatar.class);
        verify(avatarRepository).save(saved.capture());
        assertThat(saved.getValue().getUserId()).isEqualTo(7L);
        assertThat(saved.getValue().getContentType()).isEqualTo("image/png");
        assertThat(saved.getValue().getData()).isEqualTo(PNG);
        assertThat(response.avatarUrl()).isEqualTo("/api/users/7/avatar?v=1700000000000");
    }

    @Test
    void upload_forSomeoneElse_isForbidden_evenForAModerator() {
        AuthenticatedUser moderator = new AuthenticatedUser(1L, "mod", User.Role.MODERATOR);

        assertThatThrownBy(() -> avatarService.upload(7L, PNG, moderator)).isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> avatarService.upload(7L, PNG, null)).isInstanceOf(ForbiddenException.class);
        verify(avatarRepository, never()).save(any());
    }

    @Test
    void upload_byAdmin_isAllowed() {
        AuthenticatedUser admin = new AuthenticatedUser(1L, "admin", User.Role.ADMIN);
        when(userRepository.findById(7L)).thenReturn(Optional.of(user(7L)));
        when(avatarRepository.findById(7L)).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(avatarService.upload(7L, JPEG, admin).avatarUrl()).startsWith("/api/users/7/avatar");
    }

    @Test
    void upload_emptyOrOversizedOrNotAnImage_isRejected() {
        when(userRepository.findById(7L)).thenReturn(Optional.of(user(7L)));

        assertThatThrownBy(() -> avatarService.upload(7L, new byte[0], SELF))
                .isInstanceOf(InvalidImageException.class);
        assertThatThrownBy(() -> avatarService.upload(7L, new byte[AvatarService.MAX_BYTES + 1], SELF))
                .isInstanceOf(InvalidImageException.class);
        assertThatThrownBy(() -> avatarService.upload(7L, "hello".getBytes(), SELF))
                .isInstanceOf(InvalidImageException.class);
        verify(avatarRepository, never()).save(any());
    }

    @Test
    void find_missingPicture_throwsNotFound() {
        when(avatarRepository.findById(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> avatarService.find(7L)).isInstanceOf(AvatarNotFoundException.class);
    }

    @Test
    void delete_byModerator_removesThePictureAndClearsAvatarUrl() {
        AuthenticatedUser moderator = new AuthenticatedUser(1L, "mod", User.Role.MODERATOR);
        User jane = user(7L);
        jane.setAvatarUrl("/api/users/7/avatar?v=1");
        when(userRepository.findById(7L)).thenReturn(Optional.of(jane));
        when(avatarRepository.existsById(7L)).thenReturn(true);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = avatarService.delete(7L, moderator);

        verify(avatarRepository).deleteById(7L);
        assertThat(response.avatarUrl()).isNull();
    }

    @Test
    void delete_bySomeoneElse_isForbidden() {
        AuthenticatedUser stranger = new AuthenticatedUser(9L, "bob", User.Role.USER);

        assertThatThrownBy(() -> avatarService.delete(7L, stranger)).isInstanceOf(ForbiddenException.class);
        verify(avatarRepository, never()).deleteById(any());
    }
}
