package com.roberto.climbingwiki.project1.dtos;

public record UpsertResult<T>(T data, boolean created) {}
