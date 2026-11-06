package com.roberto.climbingwiki.dtos;

import com.roberto.climbingwiki.model.entities.User;

public record LoginResponse(String token, Long userId, String username, User.Role role) {}
