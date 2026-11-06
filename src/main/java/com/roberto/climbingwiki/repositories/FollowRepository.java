package com.roberto.climbingwiki.repositories;

import com.roberto.climbingwiki.model.entities.Follow;
import com.roberto.climbingwiki.model.entities.FollowId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FollowRepository extends JpaRepository<Follow, FollowId> {

    List<Follow> findByFollowerId(Long followerId);

    List<Follow> findByFollowedId(Long followedId);
}
