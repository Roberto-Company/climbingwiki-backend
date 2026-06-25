package com.roberto.climbingwiki.project1.services;

import com.roberto.climbingwiki.project1.dtos.UserRequest;
import com.roberto.climbingwiki.project1.dtos.UserResponse;
import com.roberto.climbingwiki.project1.exceptions.ForbiddenException;
import com.roberto.climbingwiki.project1.exceptions.UserConflictException;
import com.roberto.climbingwiki.project1.exceptions.UserNotFoundException;
import com.roberto.climbingwiki.project1.model.entities.User;
import com.roberto.climbingwiki.project1.repositories.UserRepository;
import com.roberto.climbingwiki.project1.security.AuthenticatedUser;
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

    public List<UserResponse> findAll() {
        return userRepository.findAll().stream().map(UserResponse::from).toList();
    }

    public UserResponse findById(Long id) {
        return userRepository.findById(id)
                .map(UserResponse::from)
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
        return UserResponse.from(userRepository.save(user));
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
