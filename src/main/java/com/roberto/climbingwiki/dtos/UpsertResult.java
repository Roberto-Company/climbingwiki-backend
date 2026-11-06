package com.roberto.climbingwiki.dtos;

public record UpsertResult<T>(T data, boolean created) {}
