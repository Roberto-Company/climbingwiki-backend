package com.roberto.climbingwiki.repositories;

import com.roberto.climbingwiki.model.entities.UserAvatar;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAvatarRepository extends JpaRepository<UserAvatar, Long> {}
