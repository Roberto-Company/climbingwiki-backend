package com.roberto.climbingwiki.project1.dtos;

import com.roberto.climbingwiki.project1.model.entities.User;

public record LoginResponse(String token, Long userId, String username, User.Role role) {}
