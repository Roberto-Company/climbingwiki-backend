package com.roberto.climbingwiki.project1.repositories;

import com.roberto.climbingwiki.project1.model.entities.Follow;
import com.roberto.climbingwiki.project1.model.entities.FollowId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FollowRepository extends JpaRepository<Follow, FollowId> {

    List<Follow> findByFollowerId(Long followerId);

    List<Follow> findByFollowedId(Long followedId);
}
