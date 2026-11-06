package com.roberto.climbingwiki.services;

import com.roberto.climbingwiki.dtos.UserRequest;
import com.roberto.climbingwiki.dtos.UserResponse;
import com.roberto.climbingwiki.exceptions.ForbiddenException;
import com.roberto.climbingwiki.exceptions.UserConflictException;
import com.roberto.climbingwiki.exceptions.UserNotFoundException;
import com.roberto.climbingwiki.model.entities.User;
import com.roberto.climbingwiki.repositories.UserRepository;
import com.roberto.climbingwiki.security.AuthenticatedUser;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<UserResponse> findAll(AuthenticatedUser viewer) {
        return userRepository.findAll().stream()
                .map(user -> UserResponse.forViewer(user, viewer))
                .toList();
    }

    public UserResponse findById(Long id, AuthenticatedUser viewer) {
        return userRepository.findById(id)
                .map(user -> UserResponse.forViewer(user, viewer))
                .orElseThrow(() -> new UserNotFoundException(id));
    }

    @Transactional
    public UserResponse create(UserRequest request) {
        return create(request, null);
    }

    @Transactional
    public UserResponse create(UserRequest request, AuthenticatedUser requester) {
        User user = new User();
        applyRequest(user, request, null, requester);
        return UserResponse.from(userRepository.save(user));
    }

    @Transactional
    public UserResponse update(Long id, UserRequest request, AuthenticatedUser requester) {
        if (!requester.isSelfOrModeratorOrAdmin(id)) {
            throw new ForbiddenException();
        }
        User user = userRepository.findById(id).orElseThrow(() -> new UserNotFoundException(id));
        applyRequest(user, request, id, requester);
        return UserResponse.forViewer(userRepository.save(user), requester);
    }

    @Transactional
    public void delete(Long id, AuthenticatedUser requester) {
        if (!requester.isSelfOrModeratorOrAdmin(id)) {
            throw new ForbiddenException();
        }
        if (!userRepository.existsById(id)) {
            throw new UserNotFoundException(id);
        }
        userRepository.deleteById(id);
    }

    private void applyRequest(User user, UserRequest request, Long selfId, AuthenticatedUser requester) {
        userRepository.findByUsername(request.username())
                .filter(existing -> !existing.getId().equals(selfId))
                .ifPresent(existing -> {
                    throw new UserConflictException("username", request.username());
                });

        userRepository.findByEmail(request.email())
                .filter(existing -> !existing.getId().equals(selfId))
                .ifPresent(existing -> {
                    throw new UserConflictException("email", request.email());
                });

        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFullName(request.fullName());
        user.setBio(request.bio());
        user.setAvatarUrl(request.avatarUrl());

        boolean callerIsAdmin = requester != null && requester.isAdmin();
        if (callerIsAdmin && request.role() != null) {
            user.setRole(request.role());
        } else if (user.getRole() == null) {
            user.setRole(User.Role.USER);
        }
    }
}
