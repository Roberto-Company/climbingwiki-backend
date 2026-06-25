package com.roberto.climbingwiki.project1.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.roberto.climbingwiki.project1.dtos.UserRequest;
import com.roberto.climbingwiki.project1.dtos.UserResponse;
import com.roberto.climbingwiki.project1.exceptions.ForbiddenException;
import com.roberto.climbingwiki.project1.exceptions.UserConflictException;
import com.roberto.climbingwiki.project1.exceptions.UserNotFoundException;
import com.roberto.climbingwiki.project1.model.entities.User;
import com.roberto.climbingwiki.project1.repositories.UserRepository;
import com.roberto.climbingwiki.project1.security.AuthenticatedUser;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, passwordEncoder);
    }

    private User user(Long id, String username, String email) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash("irrelevant-hash");
        user.setRole(User.Role.USER);
        return user;
    }

    private UserRequest request(String username, String email, String password, User.Role role) {
        return new UserRequest(username, email, password, "Jane Climber", "bio", "avatar.png", role);
    }

    private AuthenticatedUser self(Long id) {
        return new AuthenticatedUser(id, "jane", User.Role.USER);
    }

    private AuthenticatedUser admin(Long id) {
        return new AuthenticatedUser(id, "boss", User.Role.ADMIN);
    }

    @Test
    void findAll_returnsMappedResponses() {
        when(userRepository.findAll()).thenReturn(List.of(user(1L, "jane", "jane@example.com")));

        List<UserResponse> result = userService.findAll();

        assertThat(result).extracting(UserResponse::username).containsExactly("jane");
    }

    @Test
    void findById_existingUser_returnsResponse() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L, "jane", "jane@example.com")));

        UserResponse result = userService.findById(1L);

        assertThat(result.username()).isEqualTo("jane");
    }

    @Test
    void findById_missingUser_throws() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findById(99L))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void create_hashesPasswordBeforeSaving() {
        when(userRepository.findByUsername("jane")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("supersecret")).thenReturn("hashed-value");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        userService.create(request("jane", "jane@example.com", "supersecret", null));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getPasswordHash()).isEqualTo("hashed-value");
    }

    @Test
    void create_withoutRequester_defaultsToUserRegardlessOfRequestedRole() {
        when(userRepository.findByUsername("jane")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(any())).thenReturn("hashed-value");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse result = userService.create(request("jane", "jane@example.com", "supersecret", User.Role.ADMIN));

        assertThat(result.role()).isEqualTo(User.Role.USER);
    }

    @Test
    void create_withAdminRequester_honorsRequestedRole() {
        when(userRepository.findByUsername("admin")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(any())).thenReturn("hashed-value");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse result = userService.create(
                request("admin", "admin@example.com", "supersecret", User.Role.ADMIN), admin(99L));

        assertThat(result.role()).isEqualTo(User.Role.ADMIN);
    }

    @Test
    void create_duplicateUsername_throws() {
        when(userRepository.findByUsername("jane")).thenReturn(Optional.of(user(1L, "jane", "other@example.com")));

        assertThatThrownBy(() -> userService.create(request("jane", "jane@example.com", "supersecret", null)))
                .isInstanceOf(UserConflictException.class)
                .hasMessageContaining("jane");

        verify(userRepository, never()).save(any());
    }

    @Test
    void create_duplicateEmail_throws() {
        when(userRepository.findByUsername("jane")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("jane@example.com"))
                .thenReturn(Optional.of(user(1L, "other", "jane@example.com")));

        assertThatThrownBy(() -> userService.create(request("jane", "jane@example.com", "supersecret", null)))
                .isInstanceOf(UserConflictException.class)
                .hasMessageContaining("jane@example.com");

        verify(userRepository, never()).save(any());
    }

    @Test
    void update_self_keepingOwnUsernameAndEmail_doesNotConflictWithItself() {
        User existing = user(1L, "jane", "jane@example.com");
        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(userRepository.findByUsername("jane")).thenReturn(Optional.of(existing));
        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(existing));
        when(passwordEncoder.encode(any())).thenReturn("hashed-value");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse result =
                userService.update(1L, request("jane", "jane@example.com", "newpassword", null), self(1L));

        assertThat(result.username()).isEqualTo("jane");
    }

    @Test
    void update_self_attemptingRoleEscalation_roleIsIgnored() {
        User existing = user(1L, "jane", "jane@example.com");
        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(userRepository.findByUsername("jane")).thenReturn(Optional.of(existing));
        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(existing));
        when(passwordEncoder.encode(any())).thenReturn("hashed-value");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse result = userService.update(
                1L, request("jane", "jane@example.com", "newpassword", User.Role.ADMIN), self(1L));

        assertThat(result.role()).isEqualTo(User.Role.USER);
    }

    @Test
    void update_admin_canChangeAnotherUsersRole() {
        User existing = user(1L, "jane", "jane@example.com");
        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(userRepository.findByUsername("jane")).thenReturn(Optional.of(existing));
        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(existing));
        when(passwordEncoder.encode(any())).thenReturn("hashed-value");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse result = userService.update(
                1L, request("jane", "jane@example.com", "newpassword", User.Role.MODERATOR), admin(99L));

        assertThat(result.role()).isEqualTo(User.Role.MODERATOR);
    }

    @Test
    void update_byAnotherRegularUser_throwsForbidden() {
        assertThatThrownBy(() -> userService.update(
                        1L, request("jane", "jane@example.com", "newpassword", null), self(2L)))
                .isInstanceOf(ForbiddenException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    void update_toUsernameHeldByAnotherUser_throws() {
        User existing = user(1L, "jane", "jane@example.com");
        User other = user(2L, "bob", "bob@example.com");
        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> userService.update(
                        1L, request("bob", "jane@example.com", "newpassword", null), self(1L)))
                .isInstanceOf(UserConflictException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    void update_missingUser_throws() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.update(
                        99L, request("jane", "jane@example.com", "newpassword", null), self(99L)))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void delete_self_deletes() {
        when(userRepository.existsById(1L)).thenReturn(true);

        userService.delete(1L, self(1L));

        verify(userRepository).deleteById(1L);
    }

    @Test
    void delete_byAdmin_deletesAnotherUser() {
        when(userRepository.existsById(1L)).thenReturn(true);

        userService.delete(1L, admin(99L));

        verify(userRepository).deleteById(1L);
    }

    @Test
    void delete_byAnotherRegularUser_throwsForbiddenAndDoesNotDelete() {
        assertThatThrownBy(() -> userService.delete(1L, self(2L))).isInstanceOf(ForbiddenException.class);

        verify(userRepository, never()).deleteById(any());
    }

    @Test
    void delete_missingUser_throwsAndDoesNotDelete() {
        when(userRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> userService.delete(99L, self(99L))).isInstanceOf(UserNotFoundException.class);

        verify(userRepository, never()).deleteById(99L);
    }
}
