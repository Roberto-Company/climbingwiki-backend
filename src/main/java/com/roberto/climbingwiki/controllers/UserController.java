package com.roberto.climbingwiki.controllers;

import com.roberto.climbingwiki.dtos.UserRequest;
import com.roberto.climbingwiki.dtos.UserResponse;
import com.roberto.climbingwiki.security.AuthenticatedUser;
import com.roberto.climbingwiki.services.UserService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public List<UserResponse> list(@AuthenticationPrincipal AuthenticatedUser principal) {
        return userService.findAll(principal);
    }

    @GetMapping("/{id}")
    public UserResponse get(@PathVariable Long id, @AuthenticationPrincipal AuthenticatedUser principal) {
        return userService.findById(id, principal);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse create(@Valid @RequestBody UserRequest request, @AuthenticationPrincipal AuthenticatedUser principal) {
        return userService.create(request, principal);
    }

    @PutMapping("/{id}")
    public UserResponse update(
            @PathVariable Long id,
            @Valid @RequestBody UserRequest request,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        return userService.update(id, request, principal);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, @AuthenticationPrincipal AuthenticatedUser principal) {
        userService.delete(id, principal);
    }
}
